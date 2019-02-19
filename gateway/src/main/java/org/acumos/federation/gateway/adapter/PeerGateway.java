/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
 * ===================================================================================
 * This Acumos software file is distributed by AT&T and Tech Mahindra
 * under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ===============LICENSE_END=========================================================
 */

package org.acumos.federation.gateway.adapter;

import java.io.Closeable;
import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.acumos.cds.AccessTypeCode;
import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPPeerSubscription;
import org.acumos.cds.domain.MLPRevisionDescription;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.cds.domain.MLPTag;
import org.acumos.federation.gateway.cds.Artifact;
import org.acumos.federation.gateway.cds.Document;
import org.acumos.federation.gateway.cds.PeerSubscription;
import org.acumos.federation.gateway.cds.Solution;
import org.acumos.federation.gateway.cds.SolutionRevision;
import org.acumos.federation.gateway.cds.SubscriptionScope;
import org.acumos.federation.gateway.cds.TimestampedEntity;
import org.acumos.federation.gateway.common.Clients;
import org.acumos.federation.gateway.common.FederationClient;
import org.acumos.federation.gateway.common.FederationException;
import org.acumos.federation.gateway.common.JsonResponse;
import org.acumos.federation.gateway.config.GatewayCondition;
import org.acumos.federation.gateway.event.PeerSubscriptionEvent;
import org.acumos.federation.gateway.service.CatalogService;
import org.acumos.federation.gateway.service.ContentService;
import org.acumos.federation.gateway.service.PeerSubscriptionService;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.service.ServiceException;
import org.acumos.federation.gateway.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;


@Component("peergateway")
@Scope("singleton")
@Conditional({GatewayCondition.class})
public class PeerGateway {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	@Autowired
	@Qualifier("acumos")
	private TaskExecutor taskExecutor;
	@Autowired
	private Environment env;
	@Autowired
	private Clients clients;
	@Autowired
	private ContentService content;
	@Autowired
	private CatalogService catalog;
	@Autowired
	private PeerSubscriptionService peerSubscriptionService;

	private static final String federationDotOperator = "federation.operator";

	public PeerGateway() {
		log.trace("PeerGateway::new");
	}

	@PostConstruct
	public void initGateway() {
		log.trace("initPeerGateway");

		/* make sure an operator was specified and that it is a declared user */
		if (null == this.env.getProperty(federationDotOperator)) {
			throw new BeanInitializationException("Missing configuration key " + federationDotOperator);
		} 
		else {
			try {
				if (null == this.clients.getCDSClient().getUser(this.env.getProperty(federationDotOperator))) {
					log.warn(federationDotOperator + 
							" does not point to an existing user");
				}
			}
			catch (/* HttpStatusCode */Exception dx) {
				log.warn("failed to verify value " + federationDotOperator, dx);
			}
		}

		// Done
		log.debug("PeerGateway available");
	}

	@PreDestroy
	public void cleanupGateway() {
		log.debug("PeerGateway destroyed");
	}

	protected String getUserId(MLPPeerSubscription theSubscription/*
																	 * , MLPSolution theSolution
																	 */) {
		String userId = theSubscription.getUserId();
		return userId != null ? userId : this.env.getProperty(federationDotOperator);
	}

	@EventListener
	public void handlePeerSubscriptionUpdate(PeerSubscriptionEvent theEvent) {
		log.info("received peer subscription update event {}", theEvent);
		taskExecutor.execute(
				new PeerGatewayUpdateTask(theEvent.getPeer(), theEvent.getSubscription()));
	}

	/**
	 * The list of solutions processed here represents the solutions (with respect
	 * to the subscription filter definition) that were reported by the peer as
	 * being updated since the last check.
	 */
	public class PeerGatewayUpdateTask implements Runnable {

		private MLPPeer peer;
		private PeerSubscription sub;

		public PeerGatewayUpdateTask(MLPPeer thePeer, MLPPeerSubscription theSub) {
			this.peer = thePeer;
			this.sub = new PeerSubscription(theSub);
		}

		public void run() {

			Map selector = null;
			try {
				selector = Utils.jsonStringToMap(this.sub.getSelector());
			}
			catch(Exception x) {
				log.error("Failed to parse selector for subscription {}", this.sub);
				return;
			}
			Instant lastProcessed = this.sub.getProcessed();
			if (lastProcessed != null) {
				selector.put("modified", lastProcessed);
			}
			lastProcessed = Instant.now();
			
			FederationClient peerClient = clients.getFederationClient(this.peer.getApiUrl());
			if (peerClient == null) {
				log.error("Failed to get client for peer {}", this.peer);
				return;
			}

			JsonResponse<List<MLPSolution>> peerSolutionsResponse = null;
			try {
				peerSolutionsResponse = peerClient.getSolutions(selector);
			}
			catch (FederationException fx) {
				log.info("Processing peer " + this.peer + " subscription " + this.sub.getSubId() + " error.", fx);
				return;
			}

			List<MLPSolution> peerSolutions = peerSolutionsResponse.getContent();
			log.info("Processing peer {} subscription {}, {} yielded solutions {}", this.peer, this.sub.getSubId(), selector, peerSolutions);
			if (peerSolutions == null) {
				log.warn("No solutions available for peer {} subscription {} in {}", this.peer, this.sub.getSubId(), peerSolutionsResponse);
				peerSolutions = Collections.EMPTY_LIST;
				//and let it proceed so we end up marking it as processed
			}

			ServiceContext ctx = catalog.selfService();
			boolean isComplete = true;

			for (MLPSolution peerSolution : peerSolutions) {
				log.info("Processing peer solution {}", peerSolution);

				try {
					isComplete &= mapSolution(peerSolution, peerClient, ctx);
				}
				catch (Throwable t) {
					log.error("Mapping of acumos solution failed for " + peerSolution, t);
				}
			}
					
			log.info("Processing of subscription {} completed succesfully: {}", this.sub, isComplete);
			//only commit the last processed date if we completed succesfully
			if (isComplete) {
				try {
					this.sub.setProcessed(lastProcessed);
					peerSubscriptionService.updatePeerSubscription(this.sub);
				}
				catch (ServiceException sx) {
					log.error("Failed to update subscription information", sx);
				}
			}
		}

		//this should go away once the move to service interface based operations is complete
		//as ugly as they come
		private ICommonDataServiceRestClient getCDSClient(ServiceContext theContext) {
			return PeerGateway.this.clients.getCDSClient();
		}

		private Artifact copyArtifact(Artifact peerArtifact) {
			return Artifact.buildFrom(peerArtifact)
								.withUser(getUserId(this.sub))
								.withCreated(TimestampedEntity.ORIGIN)
								.withModified(TimestampedEntity.ORIGIN)
								.build();
		}

		/* we create a new one as nothing is preserved. assumes matching ids. */
		private Artifact copyArtifact(Artifact peerArtifact, Artifact localArtifact) {
			return Artifact.buildFrom(peerArtifact)
								.withId(localArtifact.getArtifactId())
								.withUser(getUserId(this.sub))
								.build();
		}

		private void putArtifact(String theSolutionId, String theRevisionId, Artifact theArtifact,
				ServiceContext theContext) throws ServiceException {

			assert(getCDSClient(theContext) != null);

			try {
				if (theArtifact.getCreated() == Instant.MIN) {
					getCDSClient(theContext).createArtifact(theArtifact);
					getCDSClient(theContext).addSolutionRevisionArtifact(theSolutionId, theRevisionId, theArtifact.getArtifactId());
					log.info("Local artifact created: {}", theArtifact);
				}
				else {
					getCDSClient(theContext).updateArtifact(theArtifact);
					log.info("Local artifact updated: {}", theArtifact);
				}
	
			}
			catch (HttpStatusCodeException restx) {
				log.error("Artifact CDS call failed. CDS message is " + restx.getResponseBodyAsString(), restx);
				throw new ServiceException("Artifact CDS call failed.", restx);
			}
			catch (Exception x) {
				log.error("Artifact unexpected failure", x);
				throw new ServiceException("Artifact CDS call failed.", x);
			}
		}

		private Document copyDocument(Document peerDocument) {
			return Document.buildFrom(peerDocument)
								.withUser(getUserId(this.sub))
								.withCreated(TimestampedEntity.ORIGIN)
								.withModified(TimestampedEntity.ORIGIN)
								.build();
		}

		private Document copyDocument(Document peerDocument, Document localDocument) {
			return Document.buildFrom(peerDocument)
								.withId(localDocument.getDocumentId())
								.withUser(getUserId(this.sub))
								.build();
		}

		private void putDocument(String theSolutionId, String theRevisionId, Document theDocument,
				ServiceContext theContext) throws ServiceException {

			try {
				if (theDocument.getCreated() == Instant.MIN) {
					getCDSClient(theContext).createDocument(theDocument);
					getCDSClient(theContext).addSolutionRevisionDocument(theRevisionId, AccessTypeCode.PB.name(), theDocument.getDocumentId());
					log.info("Local document created: {}", theDocument);
				}
				else {
					getCDSClient(theContext).updateDocument(theDocument);
					log.info("Local document updated: {}", theDocument);
				}
			}
			catch (HttpStatusCodeException restx) {
				log.error("Document CDS call failed. CDS message is " + restx.getResponseBodyAsString(), restx);
				throw new ServiceException("Document CDS call failed.", restx);
			}
			catch (Exception x) {
				log.error("Document handling unexpected failure", x);
				throw new ServiceException("Document handling unexpected failure", x);
			}
		}
	
		private MLPRevisionDescription copyRevisionDescription(MLPRevisionDescription peerDescription) {
			MLPRevisionDescription localDescription = new MLPRevisionDescription(peerDescription);
			localDescription.setCreated(TimestampedEntity.ORIGIN);
			localDescription.setModified(TimestampedEntity.ORIGIN);
			return localDescription;
		}

		private MLPRevisionDescription copyRevisionDescription(MLPRevisionDescription peerDescription, MLPRevisionDescription localDescription) {
			localDescription.setDescription(peerDescription.getDescription());
			return localDescription;
		}

		private void putRevisionDescription(MLPRevisionDescription theDescription,ServiceContext theContext) throws ServiceException {
			
			try {
				if (theDescription.getCreated() == Instant.MIN) {
					getCDSClient(theContext).createRevisionDescription(theDescription);
					log.info("Local description created: {}", theDescription);
				}
				else {
					getCDSClient(theContext).updateRevisionDescription(theDescription);
				}
			}
			catch (HttpStatusCodeException restx) {
				log.error("Revision description CDS call failed. CDS message is " + restx.getResponseBodyAsString(), restx);
				throw new ServiceException("Revision description CDS call failed.", restx);
			}
			catch (Exception x) {
				log.error("Revision description handling unexpected failure", x);
				throw new ServiceException("Revision description handling unexpected failure", x);
			}
		}

		private boolean hasChanged(Artifact thePeerArtifact, Artifact theLocalArtifact) {
			if (thePeerArtifact.getVersion() != null && theLocalArtifact.getVersion() != null) { 
				return !thePeerArtifact.getVersion().equals(theLocalArtifact.getVersion());
			}

			if (thePeerArtifact.getSize() != null && theLocalArtifact.getSize() != null) { 
				return !thePeerArtifact.getSize().equals(theLocalArtifact.getSize());
			}

			return true;
		}	

		private boolean hasChanged(Document thePeerDoc, Document theLocalDoc) {
			if (thePeerDoc.getVersion() != null && theLocalDoc.getVersion() != null) { 
				return !thePeerDoc.getVersion().equals(theLocalDoc.getVersion());
			}

			if (thePeerDoc.getSize() != null && theLocalDoc.getSize() != null) { 
				return !thePeerDoc.getSize().equals(theLocalDoc.getSize());
			}

			return true;
		}

		private Solution copySolution(Solution thePeerSolution) {
			return Solution.buildFrom(thePeerSolution)
								.withCreated(TimestampedEntity.ORIGIN)
								.withModified(TimestampedEntity.ORIGIN)
								.withUser(getUserId(this.sub))
								.withSource(this.peer.getPeerId())
								.resetStats()
								.build();
		}
	
		private Solution copySolution(Solution thePeerSolution, Solution theLocalSolution) {	
			String newUserId = getUserId(this.sub),
						 newSourceId = this.peer.getPeerId();

			//some basic warnings
			if (!theLocalSolution.getUserId().equals(newUserId)) {
				// is this solution being updated as part of different/new subscription?
				log.warn("Updating solution {} triggers a user change", theLocalSolution.getSolutionId());
				//but make the change anyway
				theLocalSolution.setUserId(newUserId);
			}

			if (theLocalSolution.getSourceId() == null) {
				//this is a local solution that made its way back
				log.info("Solution {} was originally provisioned locally, avoiding user update", theLocalSolution.getSolutionId());
			}
			else {
				if (!theLocalSolution.getSourceId().equals(newSourceId)) {
					// we will see this if a solution is available in more than one peer
					log.warn("Solution {} triggers a source change", theLocalSolution.getSolutionId());
					//but make the change anyway
					theLocalSolution.setSourceId(newSourceId);
				}
			}

			//tags, keep only the delta
			Set<MLPTag> tags = thePeerSolution.getTags();
			tags.removeAll(theLocalSolution.getTags());
			theLocalSolution.setTags(tags);			

			return theLocalSolution;
		}

		private boolean hasChanged(Solution thePeerSolution, Solution theLocalSolution) {
			if (!theLocalSolution.getTags().containsAll(thePeerSolution.getTags()))
				return false;

			return true;
		}

		/**
		 * Here comes the core process of updating a local solution's related
		 * information with what is available from a peer.
		 * 
		 * @param theSolution
		 *            the local solution who's related information (revisions and
		 *            artifacts) we are trying to sync
		 * @param thePeerClient
		 *            client
		 * @param theContext
		 *            the context in which we perform the catalog operations
		 * @return true if mapping was succesful, false otherwise
		 * @throws Exception
		 *             any error related to CDS and peer interaction
		 */
		protected boolean mapSolution(MLPSolution theSolution, FederationClient thePeerClient, ServiceContext theContext) throws Exception {

			boolean isComplete = true,
							isSolutionNew = false,
							hasSolutionChanged = false;

			Solution localSolution = null,
							 peerSolution = null;

			//retrieve the full representation from the peer
			JsonResponse<MLPSolution> peerSolutionResponse = null;
			try {
				peerSolutionResponse = thePeerClient.getSolution(theSolution.getSolutionId());
			}
			catch (FederationException fx) {
				log.warn("Failed to retrieve peer solution details for " + theSolution, fx);
				return false;
			}

			peerSolution = (Solution)peerSolutionResponse.getContent();
			if (peerSolution == null) {
				log.warn("No solution details available for {} in {}", theSolution, peerSolutionResponse);
				return false;
			}

			localSolution = catalog.getSolution(peerSolution.getSolutionId());
			if (localSolution == null) {
				localSolution = catalog.putSolution(copySolution(peerSolution), theContext);
			isSolutionNew = true;
			}
			else {
				hasSolutionChanged = hasChanged(peerSolution, localSolution);
			}
			
			List<MLPSolutionRevision> peerRevisions = (List)peerSolution.getRevisions();
			Collections.sort(peerRevisions, (arev, brev) -> arev.getModified().compareTo(brev.getModified()));

			// this should not happen as any solution should have at least one
			// revision (but that's an assumption on how on-boarding works)
			if (peerRevisions == null || peerRevisions.size() == 0) {
				log.warn("No peer revisions were retrieved");
				return true;
			}

			// check if we have locally the latest revision available on the peer
			List<MLPSolutionRevision> catalogRevisions = (List)localSolution.getRevisions();
			final List<MLPSolutionRevision> localRevisions = catalogRevisions == null ? Collections.EMPTY_LIST : catalogRevisions;

			// map peer revisions to local ones; new peer revisions have a null mapping
			Map<MLPSolutionRevision, MLPSolutionRevision> peerToLocalRevisions =
					new LinkedHashMap<MLPSolutionRevision, MLPSolutionRevision>();
			peerRevisions.forEach(peerRevision -> peerToLocalRevisions.put(peerRevision,
					localRevisions.stream()
							.filter(localRevision -> localRevision.getRevisionId().equals(peerRevision.getRevisionId()))
							.findFirst().orElse(null)));

			for (Map.Entry<MLPSolutionRevision, MLPSolutionRevision> revisionEntry : peerToLocalRevisions.entrySet()) {
				MLPSolutionRevision peerRevision = revisionEntry.getKey(), localRevision = revisionEntry.getValue();

				boolean isRevisionNew = false,
								hasRevisionChanged = false;

				//revision related information (artifacts/documents/description/..) is now embedded in the revision details
				//federation api call so one call is all is needed
				JsonResponse<MLPSolutionRevision> peerRevisionResponse = null;
				try {
					peerRevisionResponse = thePeerClient.getSolutionRevision(peerSolution.getSolutionId(), peerRevision.getRevisionId());
				}
				catch (FederationException fx) {
					isComplete = false; //try the next revision but mark the overall processing as incomplete
					continue;
				}

				peerRevision = peerRevisionResponse.getContent();
				if (peerRevision == null) {
					isComplete = false; //try the next revision but mark the overall processing as incomplete
					continue;
				}

				if (localRevision == null) {
					try {
						localRevision = catalog.putSolutionRevision(
																			SolutionRevision.buildFrom(peerRevision)
																				.withCreated(TimestampedEntity.ORIGIN)
																				.withModified(TimestampedEntity.ORIGIN)
																				.withUser(getUserId(this.sub))
																				.withSource(this.peer.getPeerId())
																				.withAccessTypeCode(this.sub.getAccessType())
																				.build(), theContext);
					}
					catch (ServiceException sx) {
						log.error("Failed to put revision " + theSolution.getSolutionId() + "/" + peerRevision.getRevisionId() + " into catalog", sx);
						isComplete = false; //try procecessing the next revision but mark the processing as incomplete
						continue;
					}
					isRevisionNew = true;
				}

				List<Artifact> peerArtifacts = (List)((SolutionRevision)peerRevision).getArtifacts();
				List<Document> peerDocuments = (List)((SolutionRevision)peerRevision).getDocuments();

				List<Artifact> catalogArtifacts = (List)((SolutionRevision)localRevision).getArtifacts();
				List<Document> catalogDocuments = (List)((SolutionRevision)localRevision).getDocuments();

				final List<Artifact> localArtifacts = catalogArtifacts;
				// map the artifacts
				// TODO: track deleted artifacts
				Map<Artifact, Artifact> peerToLocalArtifacts = new HashMap<Artifact, Artifact>();
				peerArtifacts.forEach(peerArtifact -> peerToLocalArtifacts.put(peerArtifact, localArtifacts.stream()
						.filter(localArtifact -> localArtifact.getArtifactId().equals(peerArtifact.getArtifactId()))
						.findFirst().orElse(null)));

				for (Map.Entry<Artifact, Artifact> artifactEntry : peerToLocalArtifacts.entrySet()) {
					Artifact peerArtifact = artifactEntry.getKey(),
									 localArtifact = artifactEntry.getValue();
					boolean doCatalog = false;
					
					log.info("Processing peer artifact {} against local artifact {}", peerArtifact, localArtifact);

					if (localArtifact == null) {
						localArtifact = copyArtifact(peerArtifact);
						doCatalog = true;
					}
					else {
						if (hasChanged(peerArtifact, localArtifact)) {
							// update local artifact
							localArtifact = copyArtifact(peerArtifact, localArtifact);
							doCatalog = true;
						}
					}

					boolean doContent = doCatalog &&
															(peerArtifact.getUri() != null) &&
															(SubscriptionScope.Full == SubscriptionScope.forCode(this.sub.getScopeType()));
					if (doContent) {
						log.info("Processing content for artifact {}", peerArtifact); 
						// TODO: we are trying to access the artifact by its identifier which
						// is fine in the common case but the uri specified in the artifact
						// data is the right approach (as it does not rely on the E5 definition).
						Resource artifactContent = null;
						try {
							artifactContent = thePeerClient.getArtifactContent(
								peerSolution.getSolutionId(), peerRevision.getRevisionId(), peerArtifact.getArtifactId());
							log.info("Received {} bytes of artifact content", artifactContent.contentLength()); 
						}
						catch (FederationException x) {
							log.error("Failed to retrieve acumos artifact content", x);
							doCatalog = this.sub.getSubscriptionOptions().alwaysUpdateCatalog();
							isComplete = false;
						}

						if (artifactContent != null) {
							try {
								content.putArtifactContent(
									localSolution.getSolutionId(), localRevision.getRevisionId(), localArtifact, artifactContent);
								doCatalog = true;
							}
							catch (ServiceException sx) {
								log.error("Failed to store artifact content to local repo", sx);
								doCatalog = this.sub.getSubscriptionOptions().alwaysUpdateCatalog();
								isComplete = false;
							}
							finally {
								if (artifactContent instanceof Closeable) {
									((Closeable)artifactContent).close();
								}
							}
						}
					}

					if (doCatalog) {
						try {
							putArtifact(localSolution.getSolutionId(), localRevision.getRevisionId(), localArtifact, theContext);
						}
						catch (ServiceException sx) {
							log.error("Artifact processing failed.", sx);
							isComplete = false;
						}
						hasRevisionChanged = true;
					}
				} //end map artifacts loop


				final List<Document> localDocuments = catalogDocuments;
				// map the documents
				// TODO: track deleted documents
				Map<Document, Document> peerToLocalDocuments = new HashMap<Document, Document>();
				peerDocuments.forEach(peerDocument -> peerToLocalDocuments.put(peerDocument, localDocuments.stream()
						.filter(localDocument -> localDocument.getDocumentId().equals(peerDocument.getDocumentId()))
						.findFirst().orElse(null)));

				for (Map.Entry<Document, Document> documentEntry : peerToLocalDocuments.entrySet()) {
					Document peerDocument = documentEntry.getKey(),
									 localDocument = documentEntry.getValue();
					boolean doCatalog = false;

					log.info("Processing peer document {} against local version {}", peerDocument, localDocument);
					if (localDocument == null) {
						localDocument = copyDocument(peerDocument);
						doCatalog = true;
					}
					else {
						//version strings are not standard so comparing them is not necessarly safe
						if (hasChanged(peerDocument, localDocument)) {
							// update local doc
							localDocument = copyDocument(peerDocument, localDocument);
							doCatalog = true;
						}
					}

					boolean doContent = doCatalog &&
															(peerDocument.getUri() != null) &&
															(SubscriptionScope.Full == SubscriptionScope.forCode(this.sub.getScopeType()));
					if (doContent) {
						log.info("Processing content for document {}", peerDocument); 
						// TODO: we are trying to access the document by its identifier which
						// is fine in the common case but the uri specified in the document
						// data is a more flexible approach.
						Resource documentContent = null;
						try {
							documentContent = thePeerClient.getDocumentContent(
								peerSolution.getSolutionId(), localRevision.getRevisionId(), peerDocument.getDocumentId());
							log.info("Received {} bytes of document content", documentContent.contentLength()); 
						}
						catch (FederationException x) {
							log.error("Failed to retrieve acumos document content", x);
							doCatalog = this.sub.getSubscriptionOptions().alwaysUpdateCatalog();
							isComplete = false;
						}

						if (documentContent != null) {
							try {
								content.putDocumentContent(
									localSolution.getSolutionId(), localRevision.getRevisionId(), localDocument, documentContent);
								doCatalog = true;
							}
							catch (ServiceException sx) {
								log.error("Failed to store document content to local repo", sx);
								doCatalog = this.sub.getSubscriptionOptions().alwaysUpdateCatalog();
								isComplete = false;
							}
						}
					}

					if (doCatalog) {
						try {
							putDocument(localSolution.getSolutionId(), localRevision.getRevisionId(), localDocument, theContext);
						}
						catch (ServiceException sx) {
							log.error("Document processing failed",	sx);
							isComplete = false;
						}
						hasRevisionChanged = true;
					}
	
				} // end map documents loop
				
				MLPRevisionDescription localDescription = ((SolutionRevision)localRevision).getRevisionDescription();
				MLPRevisionDescription peerDescription = ((SolutionRevision)peerRevision).getRevisionDescription();

				if (peerDescription != null) {
					boolean doCatalog = false;

					if (localDescription == null) {
						localDescription = copyRevisionDescription(peerDescription);
						doCatalog = true;
					}
					else {
						//is this a good enough test ?? it implies time sync ..
						if (peerDescription.getModified().isAfter(localDescription.getModified())) {
							localDescription = copyRevisionDescription(peerDescription, localDescription);
							doCatalog = true;
						}
					}

					if (doCatalog) {
						try {
							putRevisionDescription(localDescription, theContext);
						}
						catch (ServiceException sx) {
							log.error("Description processing failed",	sx);
							isComplete = false;
						}
						hasRevisionChanged = true;
					}
				} //end revision processing

				if (!isRevisionNew && hasRevisionChanged) {
					try {
						//we do not actually update any properties, just give CDS a chance to update the timestamps as to mark it as updated.
						catalog.putSolutionRevision(SolutionRevision.buildFrom(localRevision).build(),
																				theContext);
					}
					catch (ServiceException sx) {
						log.error("Failed to update local revision",	sx);
						isComplete = false;
					}
				}	

				hasSolutionChanged |= (isRevisionNew || hasRevisionChanged);
			} //end revisions processing

			if (!isSolutionNew && hasSolutionChanged) {
				try {
					catalog.putSolution(copySolution(peerSolution, localSolution), theContext);
				}
				catch (ServiceException sx) {
					log.error("Failed to update local solution",	sx);
					isComplete = false;
				}
			}

			return isComplete;
		} // mapSolution
	}
}

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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.acumos.cds.AccessTypeCode;
import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPPeerSubscription;
import org.acumos.cds.domain.MLPRevisionDescription;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.federation.gateway.cds.Artifact;
import org.acumos.federation.gateway.cds.Document;
import org.acumos.federation.gateway.cds.PeerSubscription;
import org.acumos.federation.gateway.cds.Solution;
import org.acumos.federation.gateway.cds.SolutionRevision;
import org.acumos.federation.gateway.cds.SubscriptionScope;
import org.acumos.federation.gateway.cds.TimestampedEntity;
import org.acumos.federation.gateway.util.Utils;
import org.acumos.federation.gateway.common.Clients;
import org.acumos.federation.gateway.common.FederationClient;
import org.acumos.federation.gateway.common.FederationException;
import org.acumos.federation.gateway.common.JsonResponse;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.config.GatewayCondition;
import org.acumos.federation.gateway.event.PeerSubscriptionEvent;
import org.acumos.federation.gateway.service.CatalogService;
import org.acumos.federation.gateway.service.ContentService;
import org.acumos.federation.gateway.service.PeerSubscriptionService;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.service.ServiceException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
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

	private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(MethodHandles.lookup().lookupClass());
	@Autowired
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
		log.trace(EELFLoggerDelegate.debugLogger, "PeerGateway::new");
	}

	@PostConstruct
	public void initGateway() {
		log.trace(EELFLoggerDelegate.debugLogger, "initPeerGateway");

		/* make sure an operator was specified and that it is a declared user */
		if (null == this.env.getProperty(federationDotOperator)) {
			throw new BeanInitializationException("Missing configuration key " + federationDotOperator);
		} 
		else {
			try {
				if (null == this.clients.getCDSClient().getUser(this.env.getProperty(federationDotOperator))) {
					log.warn(EELFLoggerDelegate.errorLogger, federationDotOperator + 
							" does not point to an existing user");
				}
			}
			catch (/* HttpStatusCode */Exception dx) {
				log.warn(EELFLoggerDelegate.errorLogger, "failed to verify value " + federationDotOperator, dx);
			}
		}

		// Done
		log.debug(EELFLoggerDelegate.debugLogger, "PeerGateway available");
	}

	@PreDestroy
	public void cleanupGateway() {
		log.debug(EELFLoggerDelegate.debugLogger, "PeerGateway destroyed");
	}

	protected String getUserId(MLPPeerSubscription theSubscription/*
																	 * , MLPSolution theSolution
																	 */) {
		String userId = theSubscription.getUserId();
		return userId != null ? userId : this.env.getProperty(federationDotOperator);
	}

	@EventListener
	public void handlePeerSubscriptionUpdate(PeerSubscriptionEvent theEvent) {
		log.info(EELFLoggerDelegate.debugLogger, "received peer subscription update event {}", theEvent);
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
				log.error(EELFLoggerDelegate.errorLogger, "Failed to parse selector for subscription {}", this.sub);
				return;
			}
			Date lastProcessed = this.sub.getProcessed();
			if (lastProcessed != null) {
				selector.put("modified", lastProcessed);
			}
			lastProcessed = new Date();
			
			FederationClient peerClient = clients.getFederationClient(this.peer.getApiUrl());
			if (peerClient == null) {
				log.error(EELFLoggerDelegate.errorLogger, "Failed to get client for peer {}", this.peer);
				return;
			}

			JsonResponse<List<MLPSolution>> peerSolutionsResponse = null;
			try {
				peerSolutionsResponse = peerClient.getSolutions(selector);
			}
			catch (FederationException fx) {
				log.info(EELFLoggerDelegate.errorLogger, "Processing peer " + this.peer + " subscription " + this.sub.getSubId() + " error.", fx);
				return;
			}

			List<MLPSolution> peerSolutions = peerSolutionsResponse.getContent();
			log.info(EELFLoggerDelegate.debugLogger, "Processing peer {} subscription {}, {} yielded solutions {}", this.peer, this.sub.getSubId(), selector, peerSolutions);
			if (peerSolutions == null) {
				log.warn(EELFLoggerDelegate.debugLogger, "No solutions available for peer {} subscription {} in {}", this.peer, this.sub.getSubId(), peerSolutionsResponse);
				peerSolutions = Collections.EMPTY_LIST;
				//and let it proceed so we end up marking it as processed
			}

			ServiceContext ctx = catalog.selfService();
			boolean isComplete = true;

			for (MLPSolution peerSolution : peerSolutions) {
				log.info(EELFLoggerDelegate.debugLogger, "Processing peer solution {}", peerSolution);

				try {
					isComplete &= mapSolution(peerSolution, peerClient, ctx);
				}
				catch (Throwable t) {
					log.error(EELFLoggerDelegate.errorLogger,
							"Mapping of acumos solution failed for " + peerSolution, t);
				}
			}
					
			log.info(EELFLoggerDelegate.debugLogger, "Processing of subscription {} completed succesfully: {}", this.sub, isComplete);
			//only commit the last processed date if we completed succesfully
			if (isComplete) {
				try {
					this.sub.setProcessed(lastProcessed);
					peerSubscriptionService.updatePeerSubscription(this.sub);
				}
				catch (ServiceException sx) {
					log.error(EELFLoggerDelegate.errorLogger,
							"Failed to update subscription information", sx);
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
								.withCreatedDate(TimestampedEntity.ORIGIN)
								.withModifiedDate(TimestampedEntity.ORIGIN)
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

			try {
				if (theArtifact.getCreated().getTime() == 0) {
					getCDSClient(theContext).createArtifact(theArtifact);
					getCDSClient(theContext).addSolutionRevisionArtifact(theSolutionId, theRevisionId, theArtifact.getArtifactId());
					log.info(EELFLoggerDelegate.debugLogger, "Local artifact created: {}", theArtifact);
				}
				else {
					getCDSClient(theContext).updateArtifact(theArtifact);
					log.info(EELFLoggerDelegate.debugLogger, "Local artifact updated: {}", theArtifact);
				}
	
			}
			catch (HttpStatusCodeException restx) {
				log.error(EELFLoggerDelegate.errorLogger,
						"Artifact CDS call failed. CDS message is " + restx.getResponseBodyAsString(), restx);
				throw new ServiceException("Artifact CDS call failed.", restx);
			}
			catch (Exception x) {
				log.error(EELFLoggerDelegate.errorLogger, "Artifact unexpected failure", x);
				throw new ServiceException("Artifact CDS call failed.", x);
			}
		}

		private Document copyDocument(Document peerDocument) {
			return Document.buildFrom(peerDocument)
								.withUser(getUserId(this.sub))
								.withCreatedDate(TimestampedEntity.ORIGIN)
								.withModifiedDate(TimestampedEntity.ORIGIN)
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
				if (theDocument.getCreated().getTime() == 0) {
					getCDSClient(theContext).createDocument(theDocument);
					getCDSClient(theContext).addSolutionRevisionDocument(theRevisionId, AccessTypeCode.PB.name(), theDocument.getDocumentId());
					log.info(EELFLoggerDelegate.debugLogger, "Local document created: {}", theDocument);
				}
				else {
					getCDSClient(theContext).updateDocument(theDocument);
					log.info(EELFLoggerDelegate.debugLogger, "Local document updated: {}", theDocument);
				}
			}
			catch (HttpStatusCodeException restx) {
				log.error(EELFLoggerDelegate.errorLogger,
						"Document CDS call failed. CDS message is " + restx.getResponseBodyAsString(), restx);
				throw new ServiceException("Document CDS call failed.", restx);
			}
			catch (Exception x) {
				log.error(EELFLoggerDelegate.errorLogger, "Document handling unexpected failure", x);
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
				if (theDescription.getCreated().getTime() == 0) {
					getCDSClient(theContext).createRevisionDescription(theDescription);
					log.info(EELFLoggerDelegate.debugLogger, "Local description created: {}", theDescription);
				}
				else {
					getCDSClient(theContext).updateRevisionDescription(theDescription);
				}
			}
			catch (HttpStatusCodeException restx) {
				log.error(EELFLoggerDelegate.errorLogger,
					"Revision description CDS call failed. CDS message is " + restx.getResponseBodyAsString(), restx);
				throw new ServiceException("Revision description CDS call failed.", restx);
			}
			catch (Exception x) {
				log.error(EELFLoggerDelegate.errorLogger, "Revision description handling unexpected failure", x);
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
	
		/**
		 * Here comes the core process of updating a local solution's related
		 * information with what is available from a peer.
		 * 
		 * @param theSolution
		 *            the local solution who's related information (revisions and
		 *            artifacts) we are trying to sync
		 * @param theContext
		 *            the context in which we perform the catalog operations
		 * @return true if mapping was succesful, false otherwise
		 * @throws Exception
		 *             any error related to CDS and peer interaction
		 */
		protected boolean mapSolution(MLPSolution theSolution, FederationClient thePeerClient, ServiceContext theContext) throws Exception {

			boolean isComplete = true;

			Solution localSolution = null,
							 peerSolution = null;

			//retrieve the full representation from the peer
			JsonResponse<MLPSolution> peerSolutionResponse = null;
			try {
				peerSolutionResponse = thePeerClient.getSolution(theSolution.getSolutionId());
			}
			catch (FederationException fx) {
				log.warn(EELFLoggerDelegate.errorLogger, "Failed to retrieve peer solution details for " + theSolution, fx);
				return false;
			}

			peerSolution = (Solution)peerSolutionResponse.getContent();
			if (peerSolution == null) {
				log.warn(EELFLoggerDelegate.debugLogger, "No solution details available for {} in {}", theSolution, peerSolutionResponse);
				return false;
			}

			localSolution = catalog.putSolution(
																	Solution.buildFrom(peerSolution)
																		.withUser(getUserId(this.sub))
																		.withSource(this.peer.getPeerId())
																		.build(), theContext);

			List<MLPSolutionRevision> peerRevisions = (List)peerSolution.getRevisions();

			// this should not happen as any solution should have at least one
			// revision (but that's an assumption on how on-boarding works)
			if (peerRevisions == null || peerRevisions.size() == 0) {
				log.warn(EELFLoggerDelegate.debugLogger, "No peer revisions were retrieved");
				return true;
			}

			// check if we have locally the latest revision available on the peer
			List<MLPSolutionRevision> catalogRevisions = Collections.EMPTY_LIST;
			try {
				catalogRevisions = catalog.getSolutionRevisions(localSolution.getSolutionId(), theContext);
			}
			catch (ServiceException sx) {
				log.error(EELFLoggerDelegate.errorLogger,
							"Failed to retrieve catalog revisions for solution " + theSolution.getSolutionId(), sx);
				throw sx;
			}
			final List<MLPSolutionRevision> localRevisions = catalogRevisions;

			// map peer revisions to local ones; new peer revisions have a null mapping
			Map<MLPSolutionRevision, MLPSolutionRevision> peerToLocalRevisions =
					/*
					 * Elegant but toMap uses map merging which does not allow null values
					 * peerRevisions .stream() .collect( Collectors.toMap(...)
					 */
					new HashMap<MLPSolutionRevision, MLPSolutionRevision>();
			peerRevisions.forEach(peerRevision -> peerToLocalRevisions.put(peerRevision,
					localRevisions.stream()
							.filter(localRevision -> localRevision.getRevisionId().equals(peerRevision.getRevisionId()))
							.findFirst().orElse(null)));

			for (Map.Entry<MLPSolutionRevision, MLPSolutionRevision> revisionEntry : peerToLocalRevisions.entrySet()) {
				MLPSolutionRevision peerRevision = revisionEntry.getKey(), localRevision = revisionEntry.getValue();

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

				try {
					localRevision = catalog.putSolutionRevision(
																		SolutionRevision.buildFrom(peerRevision)
																					.withUser(getUserId(this.sub))
																					.withSource(this.peer.getPeerId())
																					.withAccessTypeCode(this.sub.getAccessType())
																					.withValidationStatusCode(this.peer.getValidationStatusCode())
																					.build(), theContext);
				}
				catch (ServiceException sx) {
					log.error(EELFLoggerDelegate.errorLogger,
							"Failed to put revision " + theSolution.getSolutionId() + "/" + peerRevision.getRevisionId() + " into catalog", sx);
					isComplete = false; //try procecessing the next revision but mark the processing as incomplete
					continue;
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
					
					log.info(EELFLoggerDelegate.debugLogger, "Processing peer artifact {} against local artifact {}", peerArtifact, localArtifact);

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
						log.info(EELFLoggerDelegate.debugLogger, "Processing content for artifact {}", peerArtifact); 
						// TODO: we are trying to access the artifact by its identifier which
						// is fine in the common case but the uri specified in the artifact
						// data is the right approach (as it does not rely on the E5 definition).
						Resource artifactContent = null;
						try {
							artifactContent = thePeerClient.getArtifactContent(
								peerSolution.getSolutionId(), peerRevision.getRevisionId(), peerArtifact.getArtifactId());
							log.info(EELFLoggerDelegate.debugLogger, "Received {} bytes of artifact content", artifactContent.contentLength()); 
						}
						catch (FederationException x) {
							log.error(EELFLoggerDelegate.errorLogger, "Failed to retrieve acumos artifact content", x);
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
								log.error(EELFLoggerDelegate.errorLogger,
											"Failed to store artifact content to local repo", sx);
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
							log.error(EELFLoggerDelegate.errorLogger, "Artifact processing failed.", sx);
							isComplete = false;
						}
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

					log.info(EELFLoggerDelegate.debugLogger, "Processing peer document {} against local version {}", peerDocument, localDocument);
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
						log.info(EELFLoggerDelegate.debugLogger, "Processing content for document {}", peerDocument); 
						// TODO: we are trying to access the document by its identifier which
						// is fine in the common case but the uri specified in the document
						// data is a more flexible approach.
						Resource documentContent = null;
						try {
							documentContent = thePeerClient.getDocumentContent(
								peerSolution.getSolutionId(), localRevision.getRevisionId(), peerDocument.getDocumentId());
							log.info(EELFLoggerDelegate.debugLogger, "Received {} bytes of document content", documentContent.contentLength()); 
						}
						catch (FederationException x) {
							log.error(EELFLoggerDelegate.errorLogger, "Failed to retrieve acumos document content", x);
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
								log.error(EELFLoggerDelegate.errorLogger,
											"Failed to store document content to local repo", sx);
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
							log.error(EELFLoggerDelegate.errorLogger,	"Document processing failed",	sx);
							isComplete = false;
						}
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
						if (peerDescription.getModified().after(localDescription.getModified())) {
							localDescription = copyRevisionDescription(peerDescription, localDescription);
							doCatalog = true;
						}
					}

					if (doCatalog) {
						try {
							putRevisionDescription(localDescription, theContext);
						}
						catch (ServiceException sx) {
							log.error(EELFLoggerDelegate.errorLogger,	"Description processing failed",	sx);
							isComplete = false;
						}
					}
				}

			}

			return isComplete;
		} // mapSolution
	}
}

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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.acumos.cds.AccessTypeCode;
//to go away 
import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPDocument;
import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPPeerSubscription;
import org.acumos.cds.domain.MLPRevisionDescription;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.federation.gateway.cds.Artifact;
import org.acumos.federation.gateway.cds.Document;
import org.acumos.federation.gateway.cds.Solution;
import org.acumos.federation.gateway.cds.SolutionRevision;
import org.acumos.federation.gateway.cds.SubscriptionScope;
import org.acumos.federation.gateway.common.Clients;
import org.acumos.federation.gateway.common.FederationClient;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.config.GatewayCondition;
import org.acumos.federation.gateway.event.PeerSubscriptionEvent;
import org.acumos.federation.gateway.service.CatalogService;
import org.acumos.federation.gateway.service.ContentService;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.service.ServiceException;
import org.acumos.federation.gateway.service.impl.AbstractServiceImpl;
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

	private final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(PeerGateway.class);
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


	public PeerGateway() {
		log.trace(EELFLoggerDelegate.debugLogger, "PeerGateway::new");
	}

	@PostConstruct
	public void initGateway() {
		log.trace(EELFLoggerDelegate.debugLogger, "initPeerGateway");

		/* make sure an operator was specified and that it is a declared user */
		if (null == this.env.getProperty("federation.operator")) {
			throw new BeanInitializationException("Missing 'federation.operator' configuration");
		} 
		else {
			try {
				if (null == this.clients.getCDSClient().getUser(this.env.getProperty("federation.operator"))) {
					log.warn(EELFLoggerDelegate.errorLogger,
							"'federation.operator' does not point to an existing user");
				}
			}
			catch (/* HttpStatusCode */Exception dx) {
				log.warn(EELFLoggerDelegate.errorLogger, "failed to verify 'federation.operator' value", dx);
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
		return userId != null ? userId : this.env.getProperty("federation.operator");
	}

	@EventListener
	public void handlePeerSubscriptionUpdate(PeerSubscriptionEvent theEvent) {
		log.info(EELFLoggerDelegate.debugLogger, "received peer subscription update event {}", theEvent);
		taskExecutor.execute(
				new PeerGatewayUpdateTask(theEvent.getPeer(), theEvent.getSubscription(), theEvent.getSolutions()));
	}

	/**
	 * The list of solutions processed here represents the solutions (with respect
	 * to the subscription filter definition) that were reported by the peer as
	 * being updated since the last check.
	 */
	public class PeerGatewayUpdateTask implements Runnable {

		private MLPPeer peer;
		private MLPPeerSubscription sub;
		private List<MLPSolution> solutions;

		public PeerGatewayUpdateTask(MLPPeer thePeer, MLPPeerSubscription theSub, List<MLPSolution> theSolutions) {
			this.peer = thePeer;
			this.sub = theSub;
			this.solutions = theSolutions;
		}

		public void run() {

			log.info(EELFLoggerDelegate.debugLogger, "Received peer " + this.peer + " solutions: " + this.solutions);
			ServiceContext ctx = catalog.selfService();

			for (MLPSolution peerSolution : this.solutions) {
				log.info(EELFLoggerDelegate.debugLogger, "Processing peer solution {}", peerSolution);

				try {
					mapSolution(peerSolution, ctx);
				}
				catch (Throwable t) {
					log.error(EELFLoggerDelegate.errorLogger,
							"Mapping of acumos solution failed for " + peerSolution, t);
				}
			}
		}

		//this should go away once the move to service interface based operations is complete
		//as ugly as they come
		private ICommonDataServiceRestClient getCDSClient(ServiceContext theContext) {
			return PeerGateway.this.clients.getCDSClient();
			//return (ICommonDataServiceRestClient)theContext.getAttribute(AbstractServiceImpl.Attributes.cdsClient);
		}

		private Artifact createArtifact(String theSolutionId, String theRevisionId, Artifact peerArtifact,
				ServiceContext theContext) throws Exception {

			Artifact artifact = Artifact.buildFrom(peerArtifact)
														.withUser(getUserId(this.sub))
														.build();
			try {
				getCDSClient(theContext).createArtifact(artifact);
				getCDSClient(theContext).addSolutionRevisionArtifact(theSolutionId, theRevisionId, artifact.getArtifactId());
			}
			catch (HttpStatusCodeException restx) {
				log.error(EELFLoggerDelegate.errorLogger,
						"createArtifact CDS call failed. CDS message is " + restx.getResponseBodyAsString(), restx);
				return null;
			}
			catch (Exception x) {
				log.error(EELFLoggerDelegate.errorLogger, "createArtifact unexpected failure", x);
				throw x;
			}
			return artifact;
		}

		/* we create a new one as nothing is preserved. assumes matching ids. */
		private Artifact copyArtifact(Artifact peerArtifact, Artifact localArtifact) {

			return Artifact.buildFrom(peerArtifact)
								.withId(localArtifact.getArtifactId())
								.withUser(getUserId(this.sub))
								.build();
		}

		private Document createDocument(String theSolutionId, String theRevisionId, Document peerDocument,
				ServiceContext theContext) {

			Document document = Document.buildFrom(peerDocument)
														.withUser(getUserId(this.sub))
														.build();
			try {
				getCDSClient(theContext).createDocument(document);
				getCDSClient(theContext).addSolutionRevisionDocument(theRevisionId, AccessTypeCode.PB.name(), document.getDocumentId());
				return document;
			}
			catch (HttpStatusCodeException restx) {
				log.error(EELFLoggerDelegate.errorLogger,
						"createDocument CDS call failed. CDS message is " + restx.getResponseBodyAsString(), restx);
				return null;
			}
			catch (Exception x) {
				log.error(EELFLoggerDelegate.errorLogger, "createDocument unexpected failure", x);
				return null;
			}
		}

		private Document copyDocument(Document peerDocument, Document localDocument) {

			return Document.buildFrom(peerDocument)
								.withId(localDocument.getDocumentId())
								.withUser(getUserId(this.sub))
								.build();
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
		 * @throws Exception
		 *             any error related to CDS and peer interaction
		 */
		protected void mapSolution(MLPSolution theSolution, ServiceContext theContext) throws Exception {

			FederationClient fedClient = clients.getFederationClient(this.peer.getApiUrl());

			Solution localSolution,
							 peerSolution;

			//retrieve the full representation from the peer
			peerSolution = (Solution)fedClient.getSolution(theSolution.getSolutionId()).getContent();
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
				return;
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
				try {
					peerRevision = fedClient.getSolutionRevision(peerSolution.getSolutionId(), peerRevision.getRevisionId())
																		.getContent();
				}
				catch (Exception x) {
					log.error(EELFLoggerDelegate.errorLogger, "Failed to retrieve peer acumos artifact details", x);
					continue; //try procecssing the next revision
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
					continue; //try procecssing the next revision
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
					boolean doUpdate = false;
					boolean doContent = (peerArtifact.getUri() != null) &&
															(SubscriptionScope.Full == SubscriptionScope.forCode(this.sub.getScopeType()));

					if (localArtifact == null) {
						localArtifact = createArtifact(localSolution.getSolutionId(), localRevision.getRevisionId(),
								peerArtifact, theContext);
					}
					else {
						if (!peerArtifact.getVersion().equals(localArtifact.getVersion())) {
							// update local artifact
							localArtifact = copyArtifact(peerArtifact, localArtifact);
							doUpdate = true;
						}
						else {
							//if no changes, do not go after the content
							doContent = false;
						}
					}

					if (doContent) {
						log.info(EELFLoggerDelegate.debugLogger, "Processing content for artifact {}", peerArtifact); 
						// TODO: we are trying to access the artifact by its identifier which
						// is fine in the common case but the uri specified in the artifact
						// data is the right approach (as it does not rely on the E5 definition).
						Resource artifactContent = null;
						try {
							artifactContent = fedClient.getArtifactContent(
								peerSolution.getSolutionId(), peerRevision.getRevisionId(), peerArtifact.getArtifactId());
							log.info(EELFLoggerDelegate.debugLogger, "Received {} bytes of artifact content", artifactContent.contentLength()); 
						}
						catch (Exception x) {
							log.error(EELFLoggerDelegate.errorLogger, "Failed to retrieve acumos artifact content", x);
						}

						if (artifactContent != null) {
							try {
								content.putArtifactContent(
									localSolution.getSolutionId(), localRevision.getRevisionId(), localArtifact, artifactContent);
								doUpdate = true;
							}
							catch (ServiceException sx) {
								log.error(EELFLoggerDelegate.errorLogger,
											"Failed to store artifact content to local repo", sx);
							}
							finally {
								if (artifactContent instanceof Closeable) {
									((Closeable)artifactContent).close();
								}
							}
						}
					}

					if (doUpdate) {
						try {
							getCDSClient(theContext).updateArtifact(localArtifact);
							log.info(EELFLoggerDelegate.debugLogger, "Local artifact updated with local content reference: {}", localArtifact); 
						}
						catch (HttpStatusCodeException restx) {
							log.error(EELFLoggerDelegate.errorLogger,
									"updateArtifact CDS call failed. CDS message is " + restx.getResponseBodyAsString(),
									restx);
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
					boolean doUpdate = false;
					boolean doContent = (peerDocument.getUri() != null) &&
															(SubscriptionScope.Full == SubscriptionScope.forCode(this.sub.getScopeType()));

					if (localDocument == null) {
						localDocument = createDocument(localSolution.getSolutionId(), localRevision.getRevisionId(),
								peerDocument, theContext);
					}
					else {
						//version strings are not standard so comparing them is not necessarly safe
						if (peerDocument.getVersion() != null && localDocument.getVersion() != null &&
								!peerDocument.getVersion().equals(localDocument.getVersion())) {
							// update local doc
							localDocument = copyDocument(peerDocument, localDocument);
							doUpdate = true;
						}
						else {
							//if no changes, do not go after the content
							doContent = false;
						}
					}

					if (doContent) {
						log.info(EELFLoggerDelegate.debugLogger, "Processing content for document {}", peerDocument); 
						// TODO: we are trying to access the document by its identifier which
						// is fine in the common case but the uri specified in the document
						// data is a more flexible approach.
						Resource documentContent = null;
						try {
							documentContent = fedClient.getDocumentContent(
								peerSolution.getSolutionId(), localRevision.getRevisionId(), peerDocument.getDocumentId());
							log.info(EELFLoggerDelegate.debugLogger, "Received {} bytes of document content", documentContent.contentLength()); 
						}
						catch (Exception x) {
							log.error(EELFLoggerDelegate.errorLogger, "Failed to retrieve acumos document content", x);
						}

						if (documentContent != null) {
							try {
								content.putDocumentContent(
									localSolution.getSolutionId(), localRevision.getRevisionId(), localDocument, documentContent);
								doUpdate = true;
							}
							catch (ServiceException sx) {
								log.error(EELFLoggerDelegate.errorLogger,
											"Failed to store document content to local repo", sx);
							}
						}
					}

					if (doUpdate) {
						try {
							getCDSClient(theContext).updateDocument(localDocument);
							log.info(EELFLoggerDelegate.debugLogger, "Local document updated with local content reference: {}", localDocument); 
						}
						catch (HttpStatusCodeException restx) {
							log.error(EELFLoggerDelegate.errorLogger,
									"updateDocument CDS call failed. CDS message is " + restx.getResponseBodyAsString(),
									restx);
						}
					}
	
				} // end map documents loop
				
				MLPRevisionDescription catalogDescription = ((SolutionRevision)localRevision).getRevisionDescription();
				MLPRevisionDescription peerDescription = ((SolutionRevision)peerRevision).getRevisionDescription();

				if (peerDescription != null) {
					try {
						if (catalogDescription == null) {
							getCDSClient(theContext).createRevisionDescription(peerDescription);
						}
						else {
							//is this a good enough test ??
							if (peerDescription.getModified().after(catalogDescription.getModified())) {
								getCDSClient(theContext).updateRevisionDescription(peerDescription);
							}
						}
					}
					catch (HttpStatusCodeException restx) {
						log.error(EELFLoggerDelegate.errorLogger,
									"Failed to store revision description. CDS message is " + restx.getResponseBodyAsString(),
						restx);
					}
				}
			}
		} // mapSolution
	}
}

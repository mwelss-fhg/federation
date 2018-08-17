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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.AccessTypeCode;
import org.acumos.cds.domain.MLPTag;
import org.acumos.cds.domain.MLPDocument;
import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPPeerSubscription;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.cds.domain.MLPRevisionDescription;
import org.acumos.federation.gateway.cds.Document;
import org.acumos.federation.gateway.cds.Artifact;
import org.acumos.federation.gateway.cds.Solution;
import org.acumos.federation.gateway.cds.SolutionRevision;
import org.acumos.federation.gateway.cds.SubscriptionScope;
import org.acumos.federation.gateway.common.Clients;
import org.acumos.federation.gateway.common.FederationClient;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.config.GatewayCondition;
import org.acumos.federation.gateway.event.PeerSubscriptionEvent;
import org.acumos.federation.gateway.service.ContentService;
import org.acumos.federation.gateway.service.CatalogService;
import org.acumos.federation.gateway.service.ServiceException;
import org.acumos.federation.gateway.util.Errors;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

@Component("peergateway")
@Scope("singleton")
@Conditional({GatewayCondition.class})
public class PeerGateway {

	private final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(PeerGateway.class);
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

		this.taskExecutor = new ThreadPoolTaskExecutor();
		((ThreadPoolTaskExecutor) this.taskExecutor).setCorePoolSize(1);
		((ThreadPoolTaskExecutor) this.taskExecutor).setMaxPoolSize(1);
		((ThreadPoolTaskExecutor) this.taskExecutor).setQueueCapacity(25);
		((ThreadPoolTaskExecutor) this.taskExecutor).initialize();

		// Done
		log.trace(EELFLoggerDelegate.debugLogger, "PeerGateway available");
	}

	@PreDestroy
	public void cleanupGateway() {
		log.trace(EELFLoggerDelegate.debugLogger, "PeerGateway destroyed");
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
			ICommonDataServiceRestClient cdsClient = PeerGateway.this.clients.getCDSClient();
			for (MLPSolution peerSolution : this.solutions) {
				// Check if the Model already exists in the Local Acumos
				MLPSolution localSolution = null;
				log.info(EELFLoggerDelegate.debugLogger, "Processing peer solution {}", peerSolution);
				try {
					try {
						localSolution = cdsClient.getSolution(peerSolution.getSolutionId());
					} 
					catch (HttpStatusCodeException scx) {
						if (!Errors.isCDSNotFound(scx)) {
							log.error(EELFLoggerDelegate.errorLogger, "Failed to check if solution with id "
								+ peerSolution.getSolutionId() + " exists locally, skipping for now. Response says " + scx.getResponseBodyAsString(), scx);
							continue;
						}
					}

					if (localSolution == null) {
						log.info(EELFLoggerDelegate.debugLogger, "Solution Id : " + peerSolution.getSolutionId()
								+ " does not exists locally, adding it to local catalog ");
						localSolution = createMLPSolution(peerSolution, cdsClient);
					}
					else {
						log.info(EELFLoggerDelegate.debugLogger, "Solution Id : " + peerSolution.getSolutionId()
								+ " exists locally, updating local catalog ");
						localSolution = updateMLPSolution(peerSolution, localSolution, cdsClient);
					}

					addTags(peerSolution, localSolution, cdsClient);
					mapSolution(localSolution, cdsClient);
				}
				catch (Throwable t) {
					log.error(EELFLoggerDelegate.errorLogger,
							"Mapping of acumos solution failed for " + peerSolution, t);
				}
			}
		}

		private MLPSolution createMLPSolution(MLPSolution peerSolution, ICommonDataServiceRestClient cdsClient) {
			log.info(EELFLoggerDelegate.debugLogger,
					"Creating Local MLP Solution for peer solution " + peerSolution);

			Solution localSolution = Solution.buildFrom(peerSolution)
			//should the creted/modified reflect this information or the information we got from the peer ?
																	.withUser(getUserId(this.sub))
																	.withSource(this.peer.getPeerId())
																	//clear the tags and web info for now.
																	.withTags(null)
																	.withWebStats(null)
																	.build();
			try {
				cdsClient.createSolution(localSolution);	
				return localSolution;
			}
			catch (HttpStatusCodeException restx) {
				log.error(EELFLoggerDelegate.errorLogger,
						"createSolution CDS call failed. CDS message is " + restx.getResponseBodyAsString(), restx);
				return null;
			}
			catch (Exception x) {
				log.error(EELFLoggerDelegate.errorLogger, "createMLPSolution unexpected failure", x);
				return null;
			}
		}

		private MLPSolutionRevision createMLPSolutionRevision(MLPSolutionRevision peerRevision,
				ICommonDataServiceRestClient cdsClient) {

			SolutionRevision localRevision = SolutionRevision.buildFrom(peerRevision)
																					.withUser(getUserId(this.sub))
																					.withSource(this.peer.getPeerId())
																					.withAccessTypeCode(this.sub.getAccessType())
																					.withValidationStatusCode(this.peer.getValidationStatusCode())
																					.build();
			try {
				cdsClient.createSolutionRevision(localRevision);
				return localRevision;
			}
			catch (HttpStatusCodeException restx) {
				log.error(EELFLoggerDelegate.errorLogger,
						"createSolutionRevision CDS call failed. CDS message is " + restx.getResponseBodyAsString(),
						restx);
				return null;
			}
			catch (Exception x) {
				log.error(EELFLoggerDelegate.errorLogger, "createSolutionRevision unexpected failure", x);
				return null;
			}
		}

		private MLPArtifact createMLPArtifact(String theSolutionId, String theRevisionId, MLPArtifact peerArtifact,
				ICommonDataServiceRestClient cdsClient) {

			Artifact artifact = Artifact.buildFrom(peerArtifact)
														.withUser(getUserId(this.sub))
														.build();
			try {
				cdsClient.createArtifact(artifact);
				cdsClient.addSolutionRevisionArtifact(theSolutionId, theRevisionId, artifact.getArtifactId());
				return artifact;
			}
			catch (HttpStatusCodeException restx) {
				log.error(EELFLoggerDelegate.errorLogger,
						"createArtifact CDS call failed. CDS message is " + restx.getResponseBodyAsString(), restx);
				return null;
			}
			catch (Exception x) {
				log.error(EELFLoggerDelegate.errorLogger, "createArtifact unexpected failure", x);
				return null;
			}
		}

		/* we create a new one as nothing is preserved. assumes matching ids. */
		private MLPArtifact copyMLPArtifact(MLPArtifact peerArtifact, MLPArtifact localArtifact) {

			return Artifact.buildFrom(peerArtifact)
								.withId(localArtifact.getArtifactId())
								.withUser(getUserId(this.sub))
								.build();
		}

		private MLPDocument createMLPDocument(String theSolutionId, String theRevisionId, MLPDocument peerDocument,
				ICommonDataServiceRestClient cdsClient) {

			Document document = Document.buildFrom(peerDocument)
														.withUser(getUserId(this.sub))
														.build();
			try {
				cdsClient.createDocument(document);
				cdsClient.addSolutionRevisionDocument(theRevisionId, AccessTypeCode.PB.name(), document.getDocumentId());
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

		private MLPDocument copyMLPDocument(MLPDocument peerDocument, MLPDocument localDocument) {

			return Document.buildFrom(peerDocument)
								.withId(localDocument.getDocumentId())
								.withUser(getUserId(this.sub))
								.build();
		}

		private MLPSolution updateMLPSolution(final MLPSolution peerSolution, final MLPSolution localSolution,
				ICommonDataServiceRestClient cdsClient) {
			log.info(EELFLoggerDelegate.debugLogger,
					"Updating Local MLP Solution for peer solution " + peerSolution);

			if (!peerSolution.getSolutionId().equals(localSolution.getSolutionId()))
				throw new IllegalArgumentException("Local and Peer identifier mismatch");

			//start with the peer solution and pick the few local values we ought to preserve or impose
			Solution solution = Solution.buildFrom(peerSolution)
															.withUser((Object... args) -> {
																	String newUserId = getUserId(this.sub);
																		if (!newUserId.equals(localSolution.getUserId())) {
																			// is this solution being updated as part of different/new subscription?
																			log.warn(EELFLoggerDelegate.errorLogger, "updating solution " +localSolution.getSolutionId()
																			+ " as part of subscription " + this.sub.getSubId() + " triggers a user change");
																		}
																		return newUserId;
																})
															.withSource((Object... args) -> {
																	if (localSolution.getSourceId() == null) {
																		//this is a local solution that made its way back
																		log.info(EELFLoggerDelegate.debugLogger, "Solution " + localSolution.getSolutionId()
																		+ " as part of subscription " + this.sub.getSubId() + " was originally provisioned locally");
																		return null;
																	}
																	else {
																		String newSourceId = this.peer.getPeerId();
																		if (!newSourceId.equals(localSolution.getSourceId())) {
																			// we will see this if a solution is available in more than one peer
																			log.warn(EELFLoggerDelegate.errorLogger, "updating solution " +localSolution.getSolutionId()
																			+ " as part of subscription " + this.sub.getSubId() + " triggers a source change");
																		}
																		return newSourceId;
																	}
																})
															.withTags(null)
															.withWebStats(null)
															.build();

			try {
				cdsClient.updateSolution(solution);
				return solution;
			}
			catch (HttpStatusCodeException restx) {
				log.error(EELFLoggerDelegate.errorLogger,
						"updateSolution CDS call failed. CDS message is " + restx.getResponseBodyAsString(), restx);
				return null;
			}
			catch (Exception x) {
				log.error(EELFLoggerDelegate.errorLogger, "updateSolution unexpected failure", x);
				return null;
			}
		}

		private void addTags(MLPSolution peerSolution, MLPSolution localSolution, ICommonDataServiceRestClient cdsClient) {
			for (MLPTag tag: peerSolution.getTags()) {
				try {
					cdsClient.addSolutionTag(localSolution.getSolutionId(), tag.getTag());
				}
				catch (HttpStatusCodeException restx) {
					//we ignore and keep trying
				}
			}
		}
	
		/**
		 * Here comes the core process of updating a local solution's related
		 * information with what is available from a peer.
		 * 
		 * @param theSolution
		 *            the local solution who's related information (revisions and
		 *            artifacts) we are trying to sync
		 * @param cdsClient
		 *            CDS client to use in the process
		 * @throws Exception
		 *             any error related to CDS and peer interaction
		 */
		protected void mapSolution(MLPSolution theSolution, ICommonDataServiceRestClient cdsClient) throws Exception {

			FederationClient fedClient = clients.getFederationClient(this.peer.getApiUrl());

			// get revisions
			List<MLPSolutionRevision> peerRevisions = null;
			try {
				peerRevisions = (List<MLPSolutionRevision>) fedClient.getSolutionRevisions(theSolution.getSolutionId())
						.getContent();
			}
			catch (Exception x) {
				log.warn(EELFLoggerDelegate.errorLogger, "Failed to retrieve acumos revisions for solution "
						+ theSolution.getSolutionId() + " from peer " + this.peer, x);
				throw x;
			}

			// this should not happen as any solution should have at least one
			// revision (but that's an assumption on how on-boarding works)
			if (peerRevisions == null || peerRevisions.size() == 0) {
				log.warn(EELFLoggerDelegate.debugLogger, "No peer revisions were retrieved");
				return;
			}

			// check if we have locally the latest revision available on the peer
			List<MLPSolutionRevision> catalogRevisions = Collections.EMPTY_LIST;
			try {
				catalogRevisions = catalog.getSolutionRevisions(theSolution.getSolutionId());
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
					peerRevision = fedClient.getSolutionRevision(theSolution.getSolutionId(), peerRevision.getRevisionId())
																		.getContent();
				}
				catch (Exception x) {
					log.warn(EELFLoggerDelegate.errorLogger, "Failed to retrieve peer acumos artifact details", x);
					throw x;
				}

				List<MLPArtifact> peerArtifacts = (List)((SolutionRevision)peerRevision).getArtifacts();
				List<MLPDocument> peerDocuments = (List)((SolutionRevision)peerRevision).getDocuments();

				List<MLPArtifact> catalogArtifacts = Collections.EMPTY_LIST;
				List<MLPDocument>	catalogDocuments = Collections.EMPTY_LIST;
				
				if (localRevision == null) {
					localRevision = createMLPSolutionRevision(peerRevision, cdsClient);
					if (localRevision == null) {
						//cannot map this revision, move onto the next one
						continue;
					}
				}
				else {
					try {
						localRevision = catalog.getSolutionRevision(
																theSolution.getSolutionId(), localRevision.getRevisionId());
					}
					catch (ServiceException sx) {
						log.error(EELFLoggerDelegate.errorLogger,
								"Failed to retrieve catalog solution revision details for  " + theSolution.getSolutionId() + "/" + localRevision.getRevisionId(), sx);
						throw sx;
					}

					catalogArtifacts = (List)((SolutionRevision)localRevision).getArtifacts();
					catalogDocuments = (List)((SolutionRevision)localRevision).getDocuments();
				}

				final List<MLPArtifact> localArtifacts = catalogArtifacts;
				// map the artifacts
				// TODO: track deleted artifacts
				Map<MLPArtifact, MLPArtifact> peerToLocalArtifacts = new HashMap<MLPArtifact, MLPArtifact>();
				peerArtifacts.forEach(peerArtifact -> peerToLocalArtifacts.put(peerArtifact, localArtifacts.stream()
						.filter(localArtifact -> localArtifact.getArtifactId().equals(peerArtifact.getArtifactId()))
						.findFirst().orElse(null)));

				for (Map.Entry<MLPArtifact, MLPArtifact> artifactEntry : peerToLocalArtifacts.entrySet()) {
					MLPArtifact peerArtifact = artifactEntry.getKey(), localArtifact = artifactEntry.getValue();
					boolean doUpdate = false;

					if (localArtifact == null) {
						localArtifact = createMLPArtifact(theSolution.getSolutionId(), localRevision.getRevisionId(),
								peerArtifact, cdsClient);
					}
					else {
						if (!peerArtifact.getVersion().equals(localArtifact.getVersion())) {
							// update local artifact
							localArtifact = copyMLPArtifact(peerArtifact, localArtifact);
							doUpdate = true;
						}
					}

					boolean doContent = (peerArtifact.getUri() != null) &&
															(SubscriptionScope.Full == SubscriptionScope.forCode(this.sub.getScopeType()));
					if (doContent) {
						log.info(EELFLoggerDelegate.debugLogger, "Processing content for artifact {}", peerArtifact); 
						// TODO: we are trying to access the artifact by its identifier which
						// is fine in the common case but the uri specified in the artifact
						// data is a more flexible approach.
						Resource artifactContent = null;
						try {
							artifactContent = fedClient.getArtifactContent(
								theSolution.getSolutionId(), localRevision.getRevisionId(), peerArtifact.getArtifactId());
							log.info(EELFLoggerDelegate.debugLogger, "Received {} bytes of artifact content", artifactContent.contentLength()); 
						}
						catch (Exception x) {
							log.error(EELFLoggerDelegate.errorLogger, "Failed to retrieve acumos artifact content", x);
						}

						if (artifactContent != null) {
							try {
								content.putArtifactContent(
									theSolution.getSolutionId(), localRevision.getRevisionId(), localArtifact, artifactContent);
								doUpdate = true;
							}
							catch (ServiceException sx) {
								log.error(EELFLoggerDelegate.errorLogger,
											"Failed to store artifact content to local repo", sx);
							}
						}
					}

					if (doUpdate) {
						try {
							cdsClient.updateArtifact(localArtifact);
							log.info(EELFLoggerDelegate.debugLogger, "Local artifact updated with local content reference: {}", localArtifact); 
						}
						catch (HttpStatusCodeException restx) {
							log.error(EELFLoggerDelegate.errorLogger,
									"updateArtifact CDS call failed. CDS message is " + restx.getResponseBodyAsString(),
									restx);
						}
					}
				} //end map artifacts loop


				final List<MLPDocument> localDocuments = catalogDocuments;
				// map the documents
				// TODO: track deleted documents
				Map<MLPDocument, MLPDocument> peerToLocalDocuments = new HashMap<MLPDocument, MLPDocument>();
				peerDocuments.forEach(peerDocument -> peerToLocalDocuments.put(peerDocument, localDocuments.stream()
						.filter(localDocument -> localDocument.getDocumentId().equals(peerDocument.getDocumentId()))
						.findFirst().orElse(null)));

				for (Map.Entry<MLPDocument, MLPDocument> documentEntry : peerToLocalDocuments.entrySet()) {
					MLPDocument peerDocument = documentEntry.getKey(),
											localDocument = documentEntry.getValue();
					boolean doUpdate = false;

					if (localDocument == null) {
						localDocument = createMLPDocument(theSolution.getSolutionId(), localRevision.getRevisionId(),
								peerDocument, cdsClient);
					}
					else {
						//what if the local document has been modified past the last fetch ??
						if (!peerDocument.getVersion().equals(localDocument.getVersion())) {
							// update local doc
							localDocument = copyMLPDocument(peerDocument, localDocument);
							doUpdate = true;
						}
					}

					boolean doContent = (peerDocument.getUri() != null) &&
															(SubscriptionScope.Full == SubscriptionScope.forCode(this.sub.getScopeType()));
					if (doContent) {
						log.info(EELFLoggerDelegate.debugLogger, "Processing content for document {}", peerDocument); 
						// TODO: we are trying to access the document by its identifier which
						// is fine in the common case but the uri specified in the document
						// data is a more flexible approach.
						Resource documentContent = null;
						try {
							documentContent = fedClient.getDocumentContent(
								theSolution.getSolutionId(), localRevision.getRevisionId(), peerDocument.getDocumentId());
							log.info(EELFLoggerDelegate.debugLogger, "Received {} bytes of document content", documentContent.contentLength()); 
						}
						catch (Exception x) {
							log.error(EELFLoggerDelegate.errorLogger, "Failed to retrieve acumos document content", x);
						}

						if (documentContent != null) {
							try {
								content.putDocumentContent(
									theSolution.getSolutionId(), localRevision.getRevisionId(), localDocument, documentContent);
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
							cdsClient.updateDocument(localDocument);
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
							cdsClient.createRevisionDescription(peerDescription);
						}
						else {
							//is this a good enough test ??
							if (peerDescription.getModified().after(catalogDescription.getModified())) {
								cdsClient.updateRevisionDescription(peerDescription);
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

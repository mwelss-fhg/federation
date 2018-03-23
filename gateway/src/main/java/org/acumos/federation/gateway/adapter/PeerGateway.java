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

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.acumos.cds.AccessTypeCode;
import org.acumos.cds.ValidationStatusCode;
import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPPeerSubscription;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;

import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.config.GatewayCondition;
import org.acumos.federation.gateway.event.PeerSubscriptionEvent;
import org.acumos.federation.gateway.common.Clients;
import org.acumos.federation.gateway.common.FederationClient;
import org.acumos.federation.gateway.util.Errors;
import org.acumos.federation.gateway.util.Utils;
import org.acumos.federation.gateway.cds.SubscriptionScope;

import org.acumos.nexus.client.data.UploadArtifactInfo;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Conditional;
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

	protected String getOwnerId(MLPPeerSubscription theSubscription/*
																	 * , MLPSolution theSolution
																	 */) {
		String ownerId = theSubscription.getOwnerId();
		return ownerId != null ? ownerId : this.env.getProperty("federation.operator");
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
			MLPSolution localSolution = new MLPSolution();
			localSolution.setSolutionId(peerSolution.getSolutionId());
			localSolution.setName(peerSolution.getName());
			localSolution.setDescription(peerSolution.getDescription());
			localSolution.setAccessTypeCode(this.sub.getAccessType());
			localSolution.setMetadata(peerSolution.getMetadata());
			localSolution.setModelTypeCode(peerSolution.getModelTypeCode());
			localSolution.setProvider(this.peer.getName());
			localSolution.setActive(peerSolution.isActive());
			localSolution.setToolkitTypeCode(peerSolution.getToolkitTypeCode());
			localSolution.setValidationStatusCode(this.peer.getValidationStatusCode());
			//should the creted/modified reflect this information or the information we got from the peer ?
			localSolution.setCreated(peerSolution.getCreated());
			localSolution.setModified(peerSolution.getModified());
			localSolution.setOwnerId(getOwnerId(this.sub));
			localSolution.setSourceId(this.peer.getPeerId());
			localSolution.setOrigin(peerSolution.getOrigin());
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

		private MLPSolutionRevision createMLPSolutionRevision(MLPSolutionRevision peerSolutionRevision,
				ICommonDataServiceRestClient cdsClient) {
			MLPSolutionRevision localSolutionRevision = new MLPSolutionRevision();
			localSolutionRevision.setSolutionId(peerSolutionRevision.getSolutionId());
			localSolutionRevision.setRevisionId(peerSolutionRevision.getRevisionId());
			localSolutionRevision.setVersion(peerSolutionRevision.getVersion());
			localSolutionRevision.setDescription(peerSolutionRevision.getDescription());
			localSolutionRevision.setOwnerId(getOwnerId(this.sub));
			localSolutionRevision.setMetadata(peerSolutionRevision.getMetadata());
			localSolutionRevision.setCreated(peerSolutionRevision.getCreated());
			localSolutionRevision.setModified(peerSolutionRevision.getModified());
			localSolutionRevision.setSourceId(this.peer.getPeerId());
			try {
				cdsClient.createSolutionRevision(localSolutionRevision);
				return localSolutionRevision;
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

		private MLPArtifact createMLPArtifact(String theSolutionId, String theRevisionId, MLPArtifact mlpArtifact,
				ICommonDataServiceRestClient cdsClient) {
			MLPArtifact artifact = new MLPArtifact();
			artifact.setArtifactId(mlpArtifact.getArtifactId());
			artifact.setArtifactTypeCode(mlpArtifact.getArtifactTypeCode());
			artifact.setCreated(mlpArtifact.getCreated());
			artifact.setDescription(mlpArtifact.getDescription());
			artifact.setMetadata(mlpArtifact.getMetadata());
			artifact.setModified(mlpArtifact.getModified());
			artifact.setName(mlpArtifact.getName());
			artifact.setOwnerId(getOwnerId(this.sub));
			artifact.setSize(mlpArtifact.getSize());
			;
			artifact.setUri(mlpArtifact.getUri());
			artifact.setVersion(mlpArtifact.getVersion());
			try {
				cdsClient.createArtifact(artifact);
				cdsClient.addSolutionRevisionArtifact(theSolutionId, theRevisionId, mlpArtifact.getArtifactId());
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

		private MLPArtifact copyMLPArtifact(MLPArtifact peerMLPArtifact, MLPArtifact localMLPArtifact) {

			localMLPArtifact.setArtifactId(peerMLPArtifact.getArtifactId());
			localMLPArtifact.setArtifactTypeCode(peerMLPArtifact.getArtifactTypeCode());
			localMLPArtifact.setCreated(peerMLPArtifact.getCreated());
			localMLPArtifact.setDescription(peerMLPArtifact.getDescription());
			localMLPArtifact.setMetadata(peerMLPArtifact.getMetadata());
			localMLPArtifact.setModified(peerMLPArtifact.getModified());
			localMLPArtifact.setName(peerMLPArtifact.getName());
			localMLPArtifact.setOwnerId(getOwnerId(this.sub));
			localMLPArtifact.setSize(peerMLPArtifact.getSize());
			localMLPArtifact.setUri(peerMLPArtifact.getUri());
			localMLPArtifact.setVersion(peerMLPArtifact.getVersion());
			return localMLPArtifact;
		}

		private MLPSolution updateMLPSolution(MLPSolution peerSolution, MLPSolution localSolution,
				ICommonDataServiceRestClient cdsClient) {
			log.info(EELFLoggerDelegate.debugLogger,
					"Updating Local MLP Solution for peer solution " + peerSolution);

			if (!peerSolution.getSolutionId().equals(localSolution.getSolutionId()))
				throw new IllegalArgumentException("Local and Peer identifier mismatch");

			//localSolution.setSolutionId(peerSolution.getSolutionId());
			localSolution.setName(peerSolution.getName());
			localSolution.setDescription(peerSolution.getDescription());
			localSolution.setAccessTypeCode(peerSolution.getAccessTypeCode());
			localSolution.setMetadata(peerSolution.getMetadata());
			localSolution.setModelTypeCode(peerSolution.getModelTypeCode());
			localSolution.setProvider(peerSolution.getProvider());
			localSolution.setActive(peerSolution.isActive());
			localSolution.setToolkitTypeCode(peerSolution.getToolkitTypeCode());
			// reset validation status to its default
			localSolution.setValidationStatusCode(this.peer.getValidationStatusCode());
			{
				String newOwnerId = getOwnerId(this.sub);
				if (!newOwnerId.equals(localSolution.getOwnerId())) {
					// is this solution being updated as part of different/new subscription?
					log.warn(EELFLoggerDelegate.errorLogger, "updating solution " + localSolution.getSolutionId()
							+ " as part of subscription " + this.sub.getSubId() + " triggers an ownership change");
				}
				localSolution.setOwnerId(newOwnerId);
			}

			{
				if (localSolution.getSourceId() == null) {
					//this is a local solution that made its way back
					log.info(EELFLoggerDelegate.debugLogger, "Solution " + localSolution.getSolutionId()
							+ " as part of subscription " + this.sub.getSubId() + " was originally provisioned locally");
				}
				else {
					String newSourceId = this.peer.getPeerId();
					if (!newSourceId.equals(localSolution.getSourceId())) {
						// we will see this if a solution is available in more than one peer
						log.warn(EELFLoggerDelegate.errorLogger, "updating solution " + localSolution.getSolutionId()
								+ " as part of subscription " + this.sub.getSubId() + " triggers a source change");
					}
					localSolution.setSourceId(newSourceId);
				}
			}

			try {
				cdsClient.updateSolution(localSolution);
				return localSolution;
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
				log.warn(EELFLoggerDelegate.debugLogger, "No revisions were retrieved");
				return;
			}

			// check if we have locally the latest revision available on the peer
			// TODO: this is just one possible policy regarding the handling of
			// such a mismatch
			List<MLPSolutionRevision> cdsRevisions = Collections.EMPTY_LIST;
			try {
				cdsRevisions = cdsClient.getSolutionRevisions(theSolution.getSolutionId());
			}
			catch (HttpStatusCodeException restx) {
				if (!Errors.isCDSNotFound(restx)) {
					log.error(EELFLoggerDelegate.errorLogger,
							"getSolutionRevisions CDS call failed. CDS message is " + restx.getResponseBodyAsString(),
							restx);
					throw restx;
				}
			}
			final List<MLPSolutionRevision> localRevisions = cdsRevisions;

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
				// get peer artifacts
				List<MLPArtifact> peerArtifacts = null;
				try {
					peerArtifacts = (List<MLPArtifact>) fedClient
							.getArtifacts(theSolution.getSolutionId(), peerRevision.getRevisionId()).getContent();
				}
				catch (Exception x) {
					log.warn(EELFLoggerDelegate.errorLogger, "Failed to retrieve peer acumos artifacts", x);
					throw x;
				}

				List<MLPArtifact> cdsArtifacts = Collections.EMPTY_LIST;
				if (localRevision == null) {
					localRevision = createMLPSolutionRevision(peerRevision, cdsClient);
				}
				else {
					try {
						cdsArtifacts = cdsClient.getSolutionRevisionArtifacts(theSolution.getSolutionId(),
								localRevision.getRevisionId());
					}
					catch (HttpStatusCodeException restx) {
						if (!Errors.isCDSNotFound(restx)) {
							log.error(EELFLoggerDelegate.errorLogger,
									"getArtifact CDS call failed. CDS message is " + restx.getResponseBodyAsString(),
									restx);
							throw restx;
						}
					}
				}

				final List<MLPArtifact> localArtifacts = cdsArtifacts;
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
							copyMLPArtifact(peerArtifact, localArtifact);
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
							artifactContent = fedClient.downloadArtifact(peerArtifact.getArtifactId());
							log.info(EELFLoggerDelegate.debugLogger, "Received {} bytes of artifact content", artifactContent.contentLength()); 
						}
						catch (Exception x) {
							log.error(EELFLoggerDelegate.errorLogger, "Failed to retrieve acumos artifact content", x);
						}

						UploadArtifactInfo uploadInfo = null;
						if (artifactContent != null) {
							try {
								uploadInfo = PeerGateway.this.clients.getNexusClient()
										.uploadArtifact(PeerGateway.this.env.getProperty("nexus.groupId"),
												localArtifact.getName(), /* probably wrong */
												localArtifact.getVersion(), "", /* should receive this from peer */
												artifactContent.contentLength(), artifactContent.getInputStream());
								log.info(EELFLoggerDelegate.debugLogger, "Wrote artifact content locally to {}", uploadInfo.getArtifactMvnPath()); 
							}
							catch (Exception x) {
								log.error(EELFLoggerDelegate.errorLogger,
										"Failed to push artifact content to local Nexus repo", x);
							}
						}

						if (uploadInfo != null) {
							// update artifact with local repo reference
							localArtifact.setUri(uploadInfo.getArtifactMvnPath());
							// the artifact info will need to be updated with local content uri
							doUpdate = true;
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
				}
			}
		} // mapSolution
	}
}

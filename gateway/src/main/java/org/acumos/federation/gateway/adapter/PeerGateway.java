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

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.acumos.federation.gateway.common.GatewayCondition;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.event.PeerSubscriptionEvent;
import org.acumos.federation.gateway.service.impl.Clients;
import org.acumos.federation.gateway.service.impl.FederationClient;
import org.acumos.federation.gateway.util.Errors;
import org.acumos.federation.gateway.util.Utils;

import org.acumos.nexus.client.data.UploadArtifactInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.BeanInitializationException;
//import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;

import org.acumos.cds.AccessTypeCode;
import org.acumos.cds.ValidationStatusCode;
import org.acumos.cds.client.CommonDataServiceRestClientImpl;
import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPPeerSubscription;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;


@Component("peergateway")
//@Scope("singleton")
@ConfigurationProperties(prefix="federation")
@Conditional(GatewayCondition.class)
public class PeerGateway {

	private final EELFLoggerDelegate logger = EELFLoggerDelegate.getLogger(PeerGateway.class);
	private String				operator;
	private TaskExecutor	taskExecutor; 
	@Autowired
	private Environment env;
	@Autowired
	private Clients clients;


	@PostConstruct
	public void initGateway() {
		logger.debug(EELFLoggerDelegate.debugLogger, "initPeerGateway");

		/* make sure an operator was specified and that it is a declared user */
		if (null == this.env.getProperty("federation.operator")) {
			throw new BeanInitializationException("Missing 'federation.operator' configuration");
		}
		else {
			try {
				if (null == this.clients.getClient().getUser(this.env.getProperty("federation.operator"))) {
					logger.warn(EELFLoggerDelegate.errorLogger, "'federation.operator' does not point to an existing user");
				}
			}
			catch (/*HttpStatusCode*/Exception dx) {
				logger.warn(EELFLoggerDelegate.errorLogger, "failed to verify 'federation.operator' value", dx);
			}
		}

		this.taskExecutor = new ThreadPoolTaskExecutor();
		((ThreadPoolTaskExecutor)this.taskExecutor).setCorePoolSize(1);
		((ThreadPoolTaskExecutor)this.taskExecutor).setMaxPoolSize(1);
		((ThreadPoolTaskExecutor)this.taskExecutor).setQueueCapacity(25);
		((ThreadPoolTaskExecutor)this.taskExecutor).initialize();

		// Done
		logger.debug(EELFLoggerDelegate.debugLogger, "PeerGateway available");
	}

	@PreDestroy
	public void cleanupGateway() {
		logger.debug(EELFLoggerDelegate.debugLogger, "PeerGateway destroyed");
	}

	protected String getOwnerId(MLPPeerSubscription theSubscription/*,
															MLPSolution theSolution*/) {
		// Need to get from c_user table . It has to be admin user
		return this.env.getProperty("federation.operator");
	}

	@EventListener
	public void handlePeerSubscriptionUpdate(PeerSubscriptionEvent theEvent) {
		logger.info(EELFLoggerDelegate.debugLogger, "received peer subscription update event " + theEvent);
		taskExecutor.execute(
			new PeerGatewayUpdateTask(theEvent.getPeer(),
																theEvent.getSubscription(),
																theEvent.getSolutions()));
	}


	public class PeerGatewayUpdateTask implements Runnable {

		private MLPPeer							peer;
		private MLPPeerSubscription sub;
		private List<MLPSolution> 	solutions;
		

		public PeerGatewayUpdateTask(MLPPeer thePeer,
																 MLPPeerSubscription theSub,
																 List<MLPSolution> theSolutions) {
			this.peer = thePeer;
			this.sub = theSub;
			this.solutions = theSolutions;
		}

		public void run() {

			//list with category and subcategory currently used for onap
			//more dynamic mapping to come: based on solution information it will provide sdc assettype, categoty and subcategoty
			ICommonDataServiceRestClient cdsClient = PeerGateway.this.clients.getClient();
			
			logger.info(EELFLoggerDelegate.debugLogger, "Received peer " + this.peer + " solutions: " + this.solutions);

			for (MLPSolution acumosSolution: this.solutions) {
				try {
					//Check if the Model already exists in the Local Acumos
					MLPSolution mlpSolution  = null;
					try {
						mlpSolution = cdsClient.getSolution(acumosSolution.getSolutionId());
					} catch (Exception e) {
						logger.info(EELFLoggerDelegate.debugLogger, "Solution Id : " + acumosSolution.getSolutionId() + " does not exists locally, Adding it to local catalog ");
					}
					
					//Verify if MLPSolution is not same
					if(mlpSolution != null &&
						 isSameMLPSolution(acumosSolution, mlpSolution)) {
						//if already exists locally then loop through next
						mlpSolution = updateMLPSolution(acumosSolution, mlpSolution, cdsClient);
						
					} 
					else {
						mlpSolution = createMLPSolution(acumosSolution, cdsClient);
					}

					if (mlpSolution != null) {	
						updateMLPSolution(mlpSolution, cdsClient);
					}
				}
				catch (Exception x) {
					logger.warn(EELFLoggerDelegate.debugLogger, "Mapping of acumos solution failed for: " + acumosSolution, x);
				}
			}
		}
		
		private MLPSolution createMLPSolution(
																MLPSolution peerMLPSolution,
																ICommonDataServiceRestClient cdsClient) {
			logger.info(EELFLoggerDelegate.debugLogger, "Creating Local MLP Solution for peer solution " + peerMLPSolution);
			MLPSolution mlpSolution = new MLPSolution();
			mlpSolution.setSolutionId(peerMLPSolution.getSolutionId());
			mlpSolution.setName(peerMLPSolution.getName());
			mlpSolution.setDescription(peerMLPSolution.getDescription());
			mlpSolution.setAccessTypeCode(AccessTypeCode.PB.toString());
			mlpSolution.setMetadata(peerMLPSolution.getMetadata());
			mlpSolution.setModelTypeCode(peerMLPSolution.getModelTypeCode());
			mlpSolution.setProvider("ATTAcumosInc");
			mlpSolution.setActive(peerMLPSolution.isActive());
			mlpSolution.setToolkitTypeCode(peerMLPSolution.getToolkitTypeCode());
			mlpSolution.setValidationStatusCode(ValidationStatusCode.PS.toString());
			mlpSolution.setCreated(peerMLPSolution.getCreated());
			mlpSolution.setModified(peerMLPSolution.getModified());
			mlpSolution.setOwnerId(getOwnerId(this.sub)); 
			try {
				cdsClient.createSolution(mlpSolution);
				return mlpSolution;
			}
			catch (HttpStatusCodeException restx) {
				logger.error(EELFLoggerDelegate.debugLogger, "createSolution CDS call failed. CDS message is " + restx.getResponseBodyAsString(),  restx);
				return null;
			}
			catch (Exception x) {
				logger.error(EELFLoggerDelegate.debugLogger, "createMLPSolution unexpected failure",  x);
				return null;
			}
		}
		
		private MLPSolutionRevision createMLPSolutionRevision(
																		MLPSolutionRevision mlpSolutionRevision,
																		ICommonDataServiceRestClient cdsClient) {
			MLPSolutionRevision solutionRevision = new MLPSolutionRevision();
			solutionRevision.setSolutionId(mlpSolutionRevision.getSolutionId());
			solutionRevision.setRevisionId(mlpSolutionRevision.getRevisionId());
			solutionRevision.setVersion(mlpSolutionRevision.getVersion());
			solutionRevision.setDescription(mlpSolutionRevision.getDescription());
			solutionRevision.setOwnerId(getOwnerId(this.sub));
			solutionRevision.setMetadata(mlpSolutionRevision.getMetadata());
			solutionRevision.setCreated(mlpSolutionRevision.getCreated());
			solutionRevision.setModified(mlpSolutionRevision.getModified());
			try {
				cdsClient.createSolutionRevision(solutionRevision);
				return solutionRevision;
			}
			catch (HttpStatusCodeException restx) {
				logger.error(EELFLoggerDelegate.debugLogger, "createSolutionRevision CDS call failed. CDS message is " + restx.getResponseBodyAsString(),  restx);
				return null;
			}
			catch (Exception x) {
				logger.error(EELFLoggerDelegate.debugLogger, "createSolutionRevision unexpected failure",  x);
				return null;
			}
		}
		
		private MLPArtifact createMLPArtifact(
				String theSolutionId,
				String theRevisionId,
				MLPArtifact mlpArtifact,
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
			artifact.setSize(mlpArtifact.getSize());;
			artifact.setUri(mlpArtifact.getUri());
			artifact.setVersion(mlpArtifact.getVersion());
			try {
				cdsClient.createArtifact(artifact);
				cdsClient.addSolutionRevisionArtifact(theSolutionId, theRevisionId, mlpArtifact.getArtifactId());
				return artifact;
			}
			catch (HttpStatusCodeException restx) {
				logger.error(EELFLoggerDelegate.debugLogger, "createArtifact CDS call failed. CDS message is " + restx.getResponseBodyAsString(),  restx);
				return null;
			}
			catch (Exception x) {
				logger.error(EELFLoggerDelegate.debugLogger, "createArtifact unexpected failure", x);
				return null;
			}
		}
		
		private MLPArtifact updateMLPArtifact(MLPArtifact peerMLPArtifact, MLPArtifact localMLPArtifact, ICommonDataServiceRestClient cdsClient) {
			logger.info(EELFLoggerDelegate.debugLogger, "Updating Local MLP Artifact for peer artifact " + peerMLPArtifact);
			
			localMLPArtifact.setArtifactId(peerMLPArtifact.getArtifactId());
			localMLPArtifact.setArtifactTypeCode(peerMLPArtifact.getArtifactTypeCode());
			localMLPArtifact.setCreated(peerMLPArtifact.getCreated());
			localMLPArtifact.setDescription(peerMLPArtifact.getDescription());
			localMLPArtifact.setMetadata(peerMLPArtifact.getMetadata());
			localMLPArtifact.setModified(peerMLPArtifact.getModified());
			localMLPArtifact.setName(peerMLPArtifact.getName());
			localMLPArtifact.setOwnerId(getOwnerId(this.sub));
			localMLPArtifact.setSize(peerMLPArtifact.getSize());;
			localMLPArtifact.setUri(peerMLPArtifact.getUri());
			localMLPArtifact.setVersion(peerMLPArtifact.getVersion());
			try {
				cdsClient.updateArtifact(localMLPArtifact);
				return localMLPArtifact;
			}
			catch (HttpStatusCodeException restx) {
				logger.error(EELFLoggerDelegate.debugLogger, "updateArtifact CDS call failed. CDS message is " + restx.getResponseBodyAsString(),  restx);
				return null;
			}
			catch (Exception x) {
				logger.error(EELFLoggerDelegate.debugLogger, "updateArtifact unexpected failure",  x);
				return null;
			}
		}
		
		private MLPSolution updateMLPSolution(MLPSolution peerMLPSolution, MLPSolution localMLPSolution, ICommonDataServiceRestClient cdsClient) {
			logger.info(EELFLoggerDelegate.debugLogger, "Updating Local MLP Solution for peer solution " + peerMLPSolution);
			localMLPSolution.setSolutionId(peerMLPSolution.getSolutionId());
			localMLPSolution.setName(peerMLPSolution.getName());
			localMLPSolution.setDescription(peerMLPSolution.getDescription());
			localMLPSolution.setAccessTypeCode(peerMLPSolution.getAccessTypeCode());
			localMLPSolution.setMetadata(peerMLPSolution.getMetadata());
			localMLPSolution.setModelTypeCode(peerMLPSolution.getModelTypeCode());
			localMLPSolution.setProvider(peerMLPSolution.getProvider());
			localMLPSolution.setActive(peerMLPSolution.isActive());
			localMLPSolution.setToolkitTypeCode(peerMLPSolution.getToolkitTypeCode());
			localMLPSolution.setValidationStatusCode(localMLPSolution.getValidationStatusCode());
			localMLPSolution.setOwnerId(getOwnerId(this.sub));

			try {
				cdsClient.updateSolution(localMLPSolution);
				return localMLPSolution;
			}
			catch (HttpStatusCodeException restx) {
				logger.error(EELFLoggerDelegate.debugLogger, "updateSolution CDS call failed. CDS message is " + restx.getResponseBodyAsString(),  restx);
				return null;
			}
			catch (Exception x) {
				logger.error(EELFLoggerDelegate.debugLogger, "updateSolution unexpected failure",  x);
				return null;
			}
		}
		
		private boolean isSameMLPSolution(MLPSolution peerMLPSolution, MLPSolution localMLPSolution) {
			boolean isSame = false;
			if(peerMLPSolution != null && localMLPSolution != null) {
				
				if((!Utils.isEmptyOrNullString(peerMLPSolution.getName()) && !Utils.isEmptyOrNullString(localMLPSolution.getName()) && localMLPSolution.getName().equalsIgnoreCase(peerMLPSolution.getName()))
					|| (!Utils.isEmptyOrNullString(peerMLPSolution.getDescription()) && !Utils.isEmptyOrNullString(localMLPSolution.getDescription()) && localMLPSolution.getDescription().equalsIgnoreCase(peerMLPSolution.getDescription()))
					|| (!Utils.isEmptyOrNullString(peerMLPSolution.getAccessTypeCode()) && !Utils.isEmptyOrNullString(localMLPSolution.getAccessTypeCode()) && localMLPSolution.getAccessTypeCode().equalsIgnoreCase(peerMLPSolution.getAccessTypeCode()))
					|| (!Utils.isEmptyOrNullString(peerMLPSolution.getMetadata()) && !Utils.isEmptyOrNullString(localMLPSolution.getMetadata()) && localMLPSolution.getMetadata().equalsIgnoreCase(peerMLPSolution.getMetadata()))
					|| (!Utils.isEmptyOrNullString(peerMLPSolution.getModelTypeCode()) && !Utils.isEmptyOrNullString(localMLPSolution.getModelTypeCode()) && localMLPSolution.getModelTypeCode().equalsIgnoreCase(peerMLPSolution.getModelTypeCode())) 
					|| (!Utils.isEmptyOrNullString(peerMLPSolution.getProvider()) && !Utils.isEmptyOrNullString(localMLPSolution.getProvider()) && localMLPSolution.getProvider().equalsIgnoreCase(peerMLPSolution.getProvider())) 
					|| (!Utils.isEmptyOrNullString(peerMLPSolution.getToolkitTypeCode()) && !Utils.isEmptyOrNullString(localMLPSolution.getToolkitTypeCode()) && localMLPSolution.getToolkitTypeCode().equalsIgnoreCase(peerMLPSolution.getToolkitTypeCode()))
					|| (Utils.isEmptyOrNullString(peerMLPSolution.getMetadata()) && Utils.isEmptyOrNullString(localMLPSolution.getMetadata()))
					|| (Utils.isEmptyOrNullString(peerMLPSolution.getDescription()) && Utils.isEmptyOrNullString(localMLPSolution.getDescription()))
					|| (Utils.isEmptyOrNullString(peerMLPSolution.getAccessTypeCode()) && Utils.isEmptyOrNullString(localMLPSolution.getAccessTypeCode()))
					|| (Utils.isEmptyOrNullString(peerMLPSolution.getModelTypeCode()) && Utils.isEmptyOrNullString(localMLPSolution.getModelTypeCode()))
					|| (Utils.isEmptyOrNullString(peerMLPSolution.getToolkitTypeCode()) && Utils.isEmptyOrNullString(localMLPSolution.getToolkitTypeCode()))) {
					isSame = true;
				}
			}
			return isSame;
		}
		
		public void updateMLPSolution(MLPSolution theSolution,  ICommonDataServiceRestClient cdsClient) throws Exception {
			FederationClient fedClient =
					clients.getFederationClient(this.peer.getApiUrl());

			//get revisions
			List<MLPSolutionRevision> peerRevisions = null;
			try {
				peerRevisions = (List<MLPSolutionRevision>)
						fedClient.getSolutionRevisions(theSolution.getSolutionId()).getResponseBody();
			}
			catch (Exception x) {
				logger.warn(EELFLoggerDelegate.debugLogger, "Failed to retrieve acumos revisions", x);
				throw x;
			}

			//check if we have locally the latest revision available on the peer
			//TODO: this is just one possible policy regarding the handling of
			//such a mismatch
			MLPSolutionRevision localRevision = null;
			try {
				localRevision =
					cdsClient.getSolutionRevision(
						theSolution.getSolutionId(),
						peerRevisions.get(peerRevisions.size()-1).getRevisionId());
			}
			catch (HttpStatusCodeException restx) {
				if (!Errors.isCDSNotFound(restx)) {
					logger.error(EELFLoggerDelegate.debugLogger, "getSolutionRevision CDS call failed. CDS message is " + restx.getResponseBodyAsString(),  restx);
					throw restx;
				}
			}

			if(localRevision == null) {
				localRevision = createMLPSolutionRevision(
													peerRevisions.get(peerRevisions.size()-1), cdsClient);
			}
			else {
				//update the revision information
			}

			//continue to verify that we have the latest version of the artifacts
			//for this revision
			List<MLPArtifact> peerArtifacts = null;
			try {
					peerArtifacts = (List<MLPArtifact>)
						fedClient.getArtifacts(
							theSolution.getSolutionId(),
							peerRevisions.get(peerRevisions.size()-1).getRevisionId())
								.getResponseBody();
			}
			catch (Exception x) {
				logger.warn(EELFLoggerDelegate.debugLogger, "Failed to retrieve peer acumos artifacts", x);
				throw x;
			}
			
			if(localRevision != null) {
				for(MLPArtifact peerArtifact : peerArtifacts) {
					MLPArtifact localArtifact = null;
					try {
						localArtifact =
							cdsClient.getArtifact(peerArtifact.getArtifactId());
					}
					catch (HttpStatusCodeException restx) {
						if (!Errors.isCDSNotFound(restx)) {
							logger.error(EELFLoggerDelegate.debugLogger, "getArtifact CDS call failed. CDS message is " + restx.getResponseBodyAsString(),  restx);
							throw restx;
						}
					}

					if(localArtifact == null) {
						localArtifact = createMLPArtifact(
														theSolution.getSolutionId(),
														localRevision.getRevisionId(),
														peerArtifact,
														cdsClient);
					}
					else {
						//an update might not actually be necessary but we cannot compare
						//timestamps as they are locally generated 
						localArtifact = updateMLPArtifact(peerArtifact, localArtifact, cdsClient);
					}

					//TODO: add the delete of those who are not available anymore

					//if (localArtifact == null) {
						//not transactional .. hard to recover from, we'll re-attempt
						//next time we process the enclosing solution/revision (should be
						//marked accordingly)
						//if anything happened an exception 
					//}

					//artifacts file download and push it to nexus: we continue here 
					//as we persisted the peer URI 
					Resource artifactContent = null;
					try {
						artifactContent = fedClient.downloadArtifact(peerArtifact.getArtifactId());
					}
					catch (Exception x) {
						logger.warn(EELFLoggerDelegate.debugLogger, "Failed to retrieve acumos artifact content", x);
					}

					UploadArtifactInfo uploadInfo = null;
					if (artifactContent != null) {
						try {
							uploadInfo = 
								PeerGateway.this.clients.getNexusClient()
									.uploadArtifact(
											PeerGateway.this.env.getProperty("nexus.groupId"),
											localArtifact.getName(), /* probably wrong */
											localArtifact.getVersion(),
											"", /* should receive this from peer */
											artifactContent.contentLength(),
											artifactContent.getInputStream());
						}
						catch (Exception x) {
							logger.warn(EELFLoggerDelegate.debugLogger, "Failed to push artifact content to local Nexus repo", x);
						}
					}

					if (uploadInfo != null) {
						//update artifact with local repo reference
						localArtifact.setUri(uploadInfo.getArtifactMvnPath());
						try {
							cdsClient.updateArtifact(localArtifact);
						}
						catch (HttpStatusCodeException restx) {
							logger.error(EELFLoggerDelegate.debugLogger, "updateArtifact CDS call failed. CDS message is " + restx.getResponseBodyAsString(),  restx);
						}
						catch (Exception x) {
							logger.error(EELFLoggerDelegate.debugLogger, "updateArtifact unexpected failure",  x);
						}
					}
				}
				
			}
			
		}
	}
}

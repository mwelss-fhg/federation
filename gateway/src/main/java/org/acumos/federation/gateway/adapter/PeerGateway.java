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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.acumos.federation.gateway.common.GatewayCondition;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.event.PeerSubscriptionEvent;
import org.acumos.federation.gateway.service.impl.Clients;
import org.acumos.federation.gateway.service.impl.FederationClient;
import org.acumos.federation.gateway.util.Utils;

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
	public void initPeerGateway() {
		logger.debug(EELFLoggerDelegate.debugLogger, "initPeerGateway");

		/* make sure an operator was specified and that it is a declared user */
		if (null == this.env.getProperty("federation.operator")) {
			throw new BeanInitializationException("Missing 'federation.operator' configuration");
		}
		else {
			try {
				if (null == this.clients.getClient().getUser(
											this.env.getProperty("federation.operator"))) {
					logger.warn(EELFLoggerDelegate.errorLogger, "'federation.operator' does not point to an existing user");
				}
			}
			catch (HttpStatusCodeException dx) {
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
	public void cleanupOnap() {
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
			ICommonDataServiceRestClient cdsClient =
				new CommonDataServiceRestClientImpl(
							env.getProperty("cdms.client.url"),
							env.getProperty("cdms.client.username"),
							env.getProperty("cdms.client.password"));
			
			logger.info(EELFLoggerDelegate.debugLogger, "Received Acumos solutions: " + this.solutions);

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
					if(mlpSolution != null && !isSameMLPSolution(acumosSolution, mlpSolution)) {
						//if already exists locally then loop through next
						mlpSolution = updateMLPSolution(acumosSolution, mlpSolution, cdsClient);
						
					} else {
						mlpSolution = createMLPSolution(acumosSolution, cdsClient);
					}
					updateMLPSolutionArtifacts(mlpSolution, cdsClient);
					//ONAP.this.asdc.checkinResource(UUID.fromString(sdcAsset.getString("artifactUUID")), ONAP.this.asdcOperator, "solution imported " + " the acumos revision number ");
				}
				catch (Exception x) {
					logger.warn(EELFLoggerDelegate.debugLogger, "Mapping of acumos solution failed for: " + acumosSolution + ": " + x);
				}
			}
		}
		
		private MLPSolution createMLPSolution(
																MLPSolution peerMLPSolution,
																ICommonDataServiceRestClient cdsClient) {
			logger.info(EELFLoggerDelegate.debugLogger, "Creating Local MLP Solutino for peer solution " + peerMLPSolution);
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
			} catch (Exception e) {
				logger.error(EELFLoggerDelegate.debugLogger, "createMLPSolution  failed for: ",  e);
			}
			return mlpSolution;
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
			} catch (Exception e) {
				logger.error(EELFLoggerDelegate.debugLogger, "createMLPSolutionRevision  failed for: ",  e);
			}
			return solutionRevision;
		}
		
		private MLPArtifact createMLPArtifact(MLPArtifact mlpArtifact, ICommonDataServiceRestClient cdsClient) {
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
			} catch (Exception e) {
				logger.error(EELFLoggerDelegate.debugLogger, "createMLPArtifact  failed for: ",  e);
			}
			return artifact;
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
			} catch (Exception e) {
				logger.error(EELFLoggerDelegate.debugLogger, "updateMLPArtifact  failed for: ",  e);
			}
			return localMLPArtifact;
		}
		
		private MLPSolution updateMLPSolution(MLPSolution peerMLPSolution, MLPSolution localMLPSolution, ICommonDataServiceRestClient cdsClient) {
			logger.info(EELFLoggerDelegate.debugLogger, "Updating Local MLP Solutino for peer solution " + peerMLPSolution);
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
			} catch (Exception e) {
				logger.error(EELFLoggerDelegate.debugLogger, "createMLPSolution  failed for: ",  e);
			}
			return localMLPSolution;
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
		
		public void updateMLPSolutionArtifacts(MLPSolution theSolution,  ICommonDataServiceRestClient cdsClient) throws Exception {
			//fetch the solution artifacts: for now we'll do this for the latest acumos revision
			FederationClient fedClient =
					clients.getFederationClient(this.peer.getApiUrl());

			//Get List of all SolutionRevisions for a solution Id
			List<MLPSolutionRevision> acumosRevisions = null;
			try {
				acumosRevisions = (List<MLPSolutionRevision>)
						fedClient.getSolutionsRevisionListFromPeer(theSolution.getSolutionId(), null).getResponseBody();
			}
			catch (Exception x) {
				logger.warn(EELFLoggerDelegate.debugLogger, "Failed to retrieve acumos revisions: " + x);
				throw x;
			}
			
			List<MLPArtifact> acumosArtifacts = null;
			try {
				acumosArtifacts = (List<MLPArtifact>)
						fedClient.getArtifactsListFromPeer(theSolution.getSolutionId(), acumosRevisions.get(acumosRevisions.size()-1).getRevisionId(), null)
						.getResponseBody();
			}
			catch (Exception x) {
				logger.warn(EELFLoggerDelegate.debugLogger, "Failed to retrieve acumos artifacts" + x);
				throw x;
			}
			
			MLPSolutionRevision mlpSolutionRevision = cdsClient.getSolutionRevision(theSolution.getSolutionId(), acumosRevisions.get(acumosRevisions.size()-1).getRevisionId());
			if(mlpSolutionRevision == null && !Utils.isEmptyList(acumosArtifacts)) {
				//If SolutinoRevision is null, we need to create a Solution Revision in Local Acumos
				mlpSolutionRevision = createMLPSolutionRevision(acumosRevisions.get(acumosRevisions.size()-1), cdsClient);
			} 
			
			if(mlpSolutionRevision != null) {
				for(MLPArtifact artifact : acumosArtifacts) {
					MLPArtifact mlpArtifact = cdsClient.getArtifact(artifact.getArtifactId());
					if(mlpArtifact == null) {
						createMLPArtifact(mlpArtifact, cdsClient);
					} else {
						updateMLPArtifact(artifact, mlpArtifact, cdsClient);
					}
				}
				
			}
			
			//TODO Artifacts file download and push it to nexus
		}
	}
}

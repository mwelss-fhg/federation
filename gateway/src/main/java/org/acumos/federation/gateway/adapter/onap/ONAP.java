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

package org.acumos.federation.gateway.adapter.onap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.acumos.cds.ArtifactTypeCode;
import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.federation.gateway.adapter.onap.sdc.ASDC;
import org.acumos.federation.gateway.adapter.onap.sdc.ASDC.ArtifactGroupType;
import org.acumos.federation.gateway.adapter.onap.sdc.ASDC.ArtifactType;
import org.acumos.federation.gateway.adapter.onap.sdc.ASDC.AssetType;
import org.acumos.federation.gateway.adapter.onap.sdc.ASDC.LifecycleState;
import org.acumos.federation.gateway.adapter.onap.sdc.ASDCException;
import org.acumos.federation.gateway.common.Clients;
import org.acumos.federation.gateway.common.FederationClient;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.event.PeerSubscriptionEvent;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Component("onap")
@Scope("singleton")
@ConfigurationProperties(prefix = "onap")
@Conditional(ONAPAdapterCondition.class)
public class ONAP {

	private final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(ONAP.class);
	private ASDC asdc = new ASDC();
	private String asdcOperator;
	private TaskExecutor taskExecutor;
	private ToscaLab	toscalab = new ToscaLab();
	@Autowired
	private Clients clients;

	public ONAP() {
		log.debug(EELFLoggerDelegate.debugLogger, "ONAP::new");
	}

	public void setSdcUri(URI theUri) {
		this.asdc.setUri(theUri);
	}

	public void setSdcRootPath(String thePath) {
		this.asdc.setRootPath(thePath);
	}

	public void setSdcOperator(String theUid) {
		this.asdcOperator = theUid;
	}

	@PostConstruct
	public void initOnap() {
		log.trace(EELFLoggerDelegate.debugLogger, "initOnap");

		if (this.asdc.getUri() == null)
			throw new BeanInitializationException("Forgot to configure the SDC uri ('onap.sdcUri') ??");
		if (this.asdcOperator == null)
			throw new BeanInitializationException("Forgot to configure the SDC user ('onap.sdcOperator) ??");

		this.taskExecutor = new ThreadPoolTaskExecutor();
		((ThreadPoolTaskExecutor) this.taskExecutor).setCorePoolSize(1);
		((ThreadPoolTaskExecutor) this.taskExecutor).setMaxPoolSize(1);
		((ThreadPoolTaskExecutor) this.taskExecutor).setQueueCapacity(25);
		((ThreadPoolTaskExecutor) this.taskExecutor).initialize();

		// temporary
		cleanup();

		// Done
		log.trace(EELFLoggerDelegate.debugLogger, "Onap available");
	}

	@PreDestroy
	public void cleanupOnap() {
		log.trace(EELFLoggerDelegate.debugLogger, "Onap destroyed");
	}

	@EventListener
	public void handlePeerSubscriptionUpdate(PeerSubscriptionEvent theEvent) {
		log.info(EELFLoggerDelegate.debugLogger, "received peer subscription update event " + theEvent);
		taskExecutor.execute(new ONAPPushTask(theEvent.getPeer(), theEvent.getSolutions()));
	}

	public class ONAPPushTask implements Runnable {

		private MLPPeer peer;
		private List<MLPSolution> solutions;

		public ONAPPushTask(MLPPeer thePeer, List<MLPSolution> theSolutions) {
			this.peer = thePeer;
			this.solutions = theSolutions;
		}

		public void run() {

			// list with category and subcategory currently used for onap
			// more dynamic mapping to come: based on solution information it will provide
			// sdc assettype, category and subcategory
			log.info(EELFLoggerDelegate.debugLogger, "Processing {} Acumos solutions received from {}", solutions.size(), peer);

			JSONArray sdcAssets = null;
			try {
				sdcAssets = asdc.getAssets(AssetType.resource, JSONArray.class, "Generic", "Abstract").waitForResult();
			}
			catch (Exception x) {
				log.error(EELFLoggerDelegate.errorLogger, "Failed to list ONAP SDC assets: " + x.getCause(), x);
				// if this is a 404 NotFound, continue, otherwise, fail
				if (ASDCException.isNotFound(x))
					sdcAssets = new JSONArray();
				else
					return;
			}
			log.info(EELFLoggerDelegate.debugLogger, "Mapping received Acumos solutions \n{}\n to retrieved ONAP SDC assets \n{}",
			this.solutions, sdcAssets);

			for (MLPSolution acumosSolution : this.solutions) {

				FederationClient fedClient = clients.getFederationClient(this.peer.getApiUrl());

				List<MLPSolutionRevision> acumosRevisions = null;
				try {
					acumosRevisions = (List<MLPSolutionRevision>) fedClient
							.getSolutionRevisions(acumosSolution.getSolutionId()).getContent();
				}
				catch (Exception x) {
					log.error(EELFLoggerDelegate.errorLogger, "Failed to retrieve acumos revisions", x);
					throw x;
				}
				sortAcumosSolutionRevisions(acumosRevisions);

				try {
					// does the solution already exist in sdc
					JSONObject sdcAsset = lookupSdcAsset(acumosSolution, sdcAssets);
					if (sdcAsset == null) {
						// new solution
						sdcAsset = createSdcAsset(acumosSolution, acumosRevisions.get(acumosRevisions.size()-1));
					}
					else {
						// ONAP.this.asdc.checkoutResource(UUID.fromString(sdcAsset.getString("artifactUUID")),
						// ONAP.this.asdcOperator, "updated solution import");
						sdcAsset = updateSdcAsset(sdcAsset, acumosSolution, acumosRevisions);
					}
					updateAssetArtifacts(sdcAsset, acumosSolution, acumosRevisions);
					// ONAP.this.asdc.checkinResource(UUID.fromString(sdcAsset.getString("artifactUUID")),
					// ONAP.this.asdcOperator, "solution imported " + " the acumos revision number
					// ");
				}
				catch (Exception x) {
					log.error(EELFLoggerDelegate.errorLogger,
							"Mapping of acumos solution failed for: " + acumosSolution + ": " + x);
				}
			}
		}

		public JSONObject lookupSdcAsset(MLPSolution theSolution, JSONArray theAssets) {
			if (theAssets == null || theAssets.length() == 0)
				return null;
			for (int i = 0; i < theAssets.length(); i++) {
				JSONObject asset = theAssets.optJSONObject(i);
				if (sameId(theSolution, asset))
					return asset;
			}
			return null;
		}

		public JSONObject createSdcAsset(MLPSolution theSolution, MLPSolutionRevision theRevision) throws Exception {
			log.info(EELFLoggerDelegate.debugLogger, "Creating ONAP SDC VF for solution " + theSolution);

			String description = null;// theSolution.getDescription();
			if (description == null) {
				description = theRevision.getDescription();
				if (description == null) {
					description = theSolution.getSolutionId();// + "@acumos";
				}
			}

			try {
				return ONAP.this.asdc.createVF()
						.withCategory("Generic")
						.withSubCategory("Abstract")
						.withName(theSolution.getName() + "-" + theSolution.getSolutionId()) // sdc names are unique,
																								// acumos ones not so
						.withDescription(description)
						.withVendorName("Acumos")
						.withVendorRelease(theRevision.getVersion()) //is this meaningful ? given that it cannot be updated ..
						.withTags("acumos", theSolution.getSolutionId()) // can I fit an UUID as tag ??
						.withOperator(ONAP.this.asdcOperator/* theSolution.getOwnerId() */) // probably won't work, SDC
																							// expects an att uuid
						.execute().waitForResult();
			}
			catch (Exception x) {
				log.error(EELFLoggerDelegate.errorLogger, "Failed to create ONAP SDC VF", x);
				throw x;
			}
		}

		/**
		 * There is no such thing as updating an asset in the ASDC REST API, we can only
		 * update the artifacts ..
		 * 
		 * @param theAssetInfo
		 *            Asset info
		 * @param theSolution
		 *            solution
		 * @return SDC Asset info
		 */
		public JSONObject updateSdcAsset(JSONObject theAssetInfo, MLPSolution theSolution, List<MLPSolutionRevision> theRevisions) {
			log.info(EELFLoggerDelegate.debugLogger,
					"Updating ONAP SDC VF " + theAssetInfo.optString("uuid") + " for Acumos solution " + theSolution);
			return theAssetInfo;
		}

		public void updateAssetArtifacts(JSONObject theAssetInfo, MLPSolution theSolution, List<MLPSolutionRevision> theRevisions)
																																																										throws Exception {
			try {
				theAssetInfo = ONAP.this.asdc
						.getAsset(AssetType.resource, UUID.fromString(theAssetInfo.getString("uuid")), JSONObject.class)
						.waitForResult();
			}
			catch (Exception x) {
				log.error(EELFLoggerDelegate.errorLogger,
						"Failed to retrieve ONAP SDC asset metadata for " + theAssetInfo.getString("uuid") + " : " + x);
				throw x;
			}

			JSONArray sdcArtifacts = theAssetInfo.optJSONArray("artifacts");
			if (sdcArtifacts == null) {
				sdcArtifacts = new JSONArray();
			}

			//we could have a new model, a new model revision or updates to the currently mapped revision's artifacts.
			//currently we always fast-forward to the latest revision available in acumos
			MLPSolutionRevision mappedAcumosRevision = mappedAcumosRevision = theRevisions.get(theRevisions.size() - 1);

			List<MLPArtifact> acumosArtifacts = null;
			try {
				acumosArtifacts = (List<MLPArtifact>) clients.getFederationClient(this.peer.getApiUrl())
					.getArtifacts(theSolution.getSolutionId(), mappedAcumosRevision.getRevisionId())
						.getContent();
			}
			catch (Exception x) {
				log.error(EELFLoggerDelegate.errorLogger, "Failed to retrieve acumos artifacts" + x);
				throw x;
			}

			if (acumosArtifacts == null)
				acumosArtifacts = new LinkedList<MLPArtifact>();

			//add an artifact to be mapped for revision tracking purposes
			{
				MLPArtifact mapper = new MLPArtifact(mappedAcumosRevision.getVersion(),
																ArtifactTypeCode.MD.toString(),
																"mapper",
																null,
																"", //owner: never sees CDS so irrelevant 
																0);
				mapper.setArtifactId("0");//a unique value among the other artifacts of this revision
				acumosArtifacts.add(mapper);
			}

			// all this could be better writen but the 2 sets are expected to be small so we
			// favor readability

			//!! we support a 1-to-n mapping of artifacts from Acumos to SDC

			// acumos artifacts that do not exist locally need to be added
			List<MLPArtifact> newArtifacts = new LinkedList<MLPArtifact>();
			Map<MLPArtifact, JSONArray> updatedArtifacts = new HashMap<MLPArtifact, JSONArray>();
			// List<JSONObject> oldArtifacts = new LinkedList<JSONObject>();

			log.info(EELFLoggerDelegate.debugLogger, "Acumos artifacts: " + acumosArtifacts);
			log.info(EELFLoggerDelegate.debugLogger, "SDC artifacts: " + sdcArtifacts);

			for (MLPArtifact acumosArtifact : acumosArtifacts) {
				JSONArray sdcMappedArtifacts = new JSONArray();
				for (int i = 0; i < sdcArtifacts.length(); i++) {
					JSONObject sdcArtifact = sdcArtifacts.getJSONObject(i);
					if (sameId(acumosArtifact, sdcArtifact)) {
						sdcMappedArtifacts.put(sdcArtifact);
					}
				}

				if (sdcMappedArtifacts.length() > 0) {
					//sdc artifacts mapped to the acumos artifacts were found
					//if not at the same version, update
					//TODO: add a coherence check to make sure all sdcArtifacts are at the same (acumos) version
					if (!sameVersion(acumosArtifact, sdcMappedArtifacts.getJSONObject(0))) {
						updatedArtifacts.put(acumosArtifact, sdcMappedArtifacts);
					}
				}
				else {
					newArtifacts.add(acumosArtifact);
				}
			}

			log.info(EELFLoggerDelegate.debugLogger, "New artifacts: " + newArtifacts);
			for (MLPArtifact acumosArtifact : newArtifacts) {
				try {
					for (ASDC.ArtifactUploadAction uploadAction:  mapNewArtifact(theAssetInfo, acumosArtifact)) {
						uploadAction.execute().waitForResult();
					}
				}
				catch (Exception x) {
					log.error(EELFLoggerDelegate.errorLogger, "Failed to create ONAP SDC VF Artifacts for " + acumosArtifact, x);
				}
			}

			log.warn(EELFLoggerDelegate.debugLogger, "Updated SDC artifacts: " + updatedArtifacts.keySet());
			for (Map.Entry<MLPArtifact, JSONArray> updateEntry : updatedArtifacts.entrySet()) {
				MLPArtifact acumosArtifact = updateEntry.getKey();
				try {
					for (ASDC.ArtifactUpdateAction updateAction:  mapArtifact(theAssetInfo, updateEntry.getKey(), updateEntry.getValue())) {
						updateAction.execute().waitForResult();
					}
				}
				catch (Exception x) {
					log.error(EELFLoggerDelegate.errorLogger, "Failed to update ONAP SDC VF Artifact for " + updateEntry.getKey(), x);
				}
			}

			// sdc artifacts that do not have a acumos counterpart should be deleted (if
			// they are labeled as having
			// originated in acumos).
			List<JSONObject> deletedArtifacts = new LinkedList<JSONObject>();
			for (int i = 0; i < sdcArtifacts.length(); i++) {
				JSONObject sdcArtifact = sdcArtifacts.getJSONObject(i);
				boolean found = false;
				for (MLPArtifact acumosArtifact : acumosArtifacts) {
					if (sameId(acumosArtifact, sdcArtifact)) {
						found = true;
						break;
					}
				}
				if (!found && isAcumosOriginated(sdcArtifact)) {
					deletedArtifacts.add(sdcArtifact);
				}
			}
			log.warn(EELFLoggerDelegate.debugLogger, "Deleted SDC artifacts: " + deletedArtifacts);
			for (JSONObject sdcArtifact : deletedArtifacts) {
				try {
					asdc.deleteAssetArtifact(AssetType.resource, UUID.fromString(theAssetInfo.getString("uuid")),
							UUID.fromString(sdcArtifact.getString("artifactUUID"))).withOperator(ONAP.this.asdcOperator)
							.execute().waitForResult();
				}
				catch (Exception x) {
					log.error(EELFLoggerDelegate.errorLogger, "Failed to delete ONAP SDC VF Artifact", x);
				}
			}
		}

		/**
		 */
		private List<ASDC.ArtifactUploadAction> mapNewArtifact(JSONObject theSDCAsset, MLPArtifact theAcumosArtifact) {

			if (isDCAEComponentSpecification(theAcumosArtifact)) {

				byte[] content = null;
				try {
					content = retrieveContent(theAcumosArtifact);
				}
				catch (Exception x) {
					log.error(EELFLoggerDelegate.errorLogger, "Failed to retrieve Acumoms artifact content from " + theAcumosArtifact.getUri(), x);
					return Collections.EMPTY_LIST;
				}

				JSONObject models = null;
				try {
					models = new JSONObject(toscalab.create_model(new ByteArrayInputStream(content)));
				}
				catch (JSONException jsonx) {
					log.error(EELFLoggerDelegate.errorLogger, "Failed to parse toscalab output", jsonx);
					return Collections.EMPTY_LIST;
				}
				catch (Exception x) {
					log.error(EELFLoggerDelegate.errorLogger, "Failed to process DCAE component specification from " + theAcumosArtifact, x);
					return Collections.EMPTY_LIST;
				}

				List<ASDC.ArtifactUploadAction> actions = new LinkedList<ASDC.ArtifactUploadAction>();
				for (String model: models.keySet()) {
					actions.add(
						asdc.createAssetArtifact(AssetType.resource, UUID.fromString(theSDCAsset.getString("uuid")))
						.withOperator(ONAP.this.asdcOperator)
						.withEncodedContent(models.getString(model))
						.withLabel(theAcumosArtifact.getArtifactTypeCode())
						.withName(model/*theAcumosArtifact.getName()*/)
						.withDisplayName(theAcumosArtifact.getMetadata())
						.withType(ArtifactType.DCAE_TOSCA/*ArtifactType.OTHER*/)
						.withGroupType(ArtifactGroupType.DEPLOYMENT)
						.withDescription(theAcumosArtifact.getArtifactId() + "@" + theAcumosArtifact.getVersion())
					);
				}
				return actions;
			}
			else if (isMapper(theAcumosArtifact)) {
				return Collections.singletonList(
					asdc.createAssetArtifact(AssetType.resource, UUID.fromString(theSDCAsset.getString("uuid")))
						.withOperator(ONAP.this.asdcOperator)
						.withContent("{}".getBytes())
						.withLabel(theAcumosArtifact.getArtifactTypeCode())
						.withName(theAcumosArtifact.getName())
						.withDisplayName("mapper")
						.withType(ArtifactType.OTHER)
						.withGroupType(ArtifactGroupType.DEPLOYMENT)
						.withDescription(theAcumosArtifact.getArtifactId() + "@" + theAcumosArtifact.getVersion())
				);
			} 
			else {
				//everything else gets ignored at this point
				return Collections.EMPTY_LIST;
			}
		}
		
		private List<ASDC.ArtifactUpdateAction> mapArtifact(JSONObject theSDCAsset, MLPArtifact theAcumosArtifact, JSONArray theSDCArtifacts) {
			
			if (isDCAEComponentSpecification(theAcumosArtifact)) {
				byte[] content = null;
				try {
					content = retrieveContent(theAcumosArtifact);
				}
				catch (Exception x) {
					log.error(EELFLoggerDelegate.errorLogger, "Failed to retrieve Acumoms artifact content from " + theAcumosArtifact.getUri(), x);
					return Collections.EMPTY_LIST;
				}

				JSONObject models = null;
				try {
					models = new JSONObject(toscalab.create_model(new ByteArrayInputStream(content)));
				}
				catch (JSONException jsonx) {
					log.error(EELFLoggerDelegate.errorLogger, "Failed to parse toscalab output", jsonx);
					return Collections.EMPTY_LIST;
				}
				catch (Exception x) {
					log.error(EELFLoggerDelegate.errorLogger, "Failed to process DCAE component specification from " + theAcumosArtifact, x);
					return Collections.EMPTY_LIST;
				}

				List<ASDC.ArtifactUpdateAction> actions = new LinkedList<ASDC.ArtifactUpdateAction>();
				for (int i = 0; i < theSDCArtifacts.length(); i++) {
					JSONObject sdcArtifact = theSDCArtifacts.getJSONObject(i);
					actions.add(
						asdc.updateAssetArtifact(AssetType.resource, UUID.fromString(theSDCAsset.getString("uuid")), sdcArtifact)
							.withOperator(ONAP.this.asdcOperator)
							.withEncodedContent(models.getString(sdcArtifact.getString("name")))
							.withName(sdcArtifact.getString("name"))
							.withDescription(theAcumosArtifact.getArtifactId() + "@"	+ theAcumosArtifact.getVersion())
					);
				}
				return actions;
			}
			else if (isMapper(theAcumosArtifact)) {
				if (theSDCArtifacts.length() != 1)
					log.warn(EELFLoggerDelegate.errorLogger, "Found more than one mapper artifact {}", theSDCArtifacts);
				return Collections.singletonList(
						asdc.updateAssetArtifact(AssetType.resource, UUID.fromString(theSDCAsset.getString("uuid")), theSDCArtifacts.getJSONObject(0))
							.withOperator(ONAP.this.asdcOperator)
							.withName(theAcumosArtifact.getName())
							.withDescription(theAcumosArtifact.getArtifactId() + "@"	+ theAcumosArtifact.getVersion()));
			} 
			else {
				log.error(EELFLoggerDelegate.errorLogger, "Found sdc artifacts for mlp artifact we do not process {}: {} ", theAcumosArtifact, theSDCArtifacts);
				return Collections.EMPTY_LIST;
			}
		}

		private boolean isDCAEComponentSpecification(MLPArtifact theArtifact) {
			return theArtifact.getName().equals("component-specification.json");
		}
		
		private boolean isMapper(MLPArtifact theArtifact) {
			return theArtifact.getName().equals("mapper");
		}

		private boolean sameId(MLPSolution theAcumosSolution, JSONObject theSDCAsset) {

			return theSDCAsset.optString("name", "")
					.equals(theAcumosSolution.getName() + "-" + theAcumosSolution.getSolutionId());
		}

		private boolean sameId(MLPArtifact theAcumosArtifact, JSONObject theSDCArtifact) {
			return acumosArtifactId(theSDCArtifact).equals(theAcumosArtifact.getArtifactId());
		}

		/*
		 * Only safe to call if 'same' returned true
		 */
		private boolean sameVersion(MLPArtifact theAcumosArtifact, JSONObject theSDCArtifact) {
			return acumosArtifactVersion(theSDCArtifact).equals(theAcumosArtifact.getVersion());
		}

		private String acumosArtifactId(JSONObject theSDCArtifact) {
			return theSDCArtifact.optString("artifactDescription","@").split("@")[0];
		}

		private String acumosArtifactVersion(JSONObject theSDCArtifact) {
			return theSDCArtifact.optString("artifactDescription","@").split("@")[1];
		}

		private boolean isAcumosOriginated(JSONObject theSDCArtifact) {
			boolean isAcumos = theSDCArtifact.optString("artifactType").equals(ArtifactType.OTHER.toString())
					&& theSDCArtifact.optString("artifactGroupType").equals(ArtifactGroupType.DEPLOYMENT.toString());
			String[] parts = theSDCArtifact.optString("artifactDescription", "@").split("@");
			isAcumos &= (parts.length == 2); // and the first part can be parsed as an UUID
			return isAcumos;
		}

		private byte[] retrieveContent(MLPArtifact theAcumosArtifact) throws Exception {

			if (this.peer.isLocal()) {
				return clients.getNexusClient().getArtifact(theAcumosArtifact.getUri()).toByteArray();
			}
			else { //non-local peer
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				StreamUtils.copy(
					clients.getFederationClient(this.peer.getApiUrl()).downloadArtifact(theAcumosArtifact.getArtifactId()).getInputStream(),
					bos);
				return bos.toByteArray();
			}
			//else {
			//	return IOUtils.toByteArray(new URI(theAcumosArtifact.getUri()));
			//}
		}
	}


	/**
	 * Removes all (non-commited) Acumos solutions imported into ONAP SDC
	 */
	protected void cleanup() {

		JSONArray sdcAssets = null;
		try {
			sdcAssets = asdc.getAssets(AssetType.resource, JSONArray.class, "Generic", "Abstract").waitForResult();
		} catch (Throwable x) {
			log.info(EELFLoggerDelegate.debugLogger, "Cleanup failed to list ONAP SDC assets: " + x.getCause(), x);
		}

		if (sdcAssets == null)
			return;

		for (int i = 0; i < sdcAssets.length(); i++) {
			JSONObject sdcAsset = sdcAssets.optJSONObject(i);
			String state = sdcAsset.optString("lifecycleState");
			if (state != null && "NOT_CERTIFIED_CHECKEOUT".equals(state)) {
				try {
					asdc.cycleAsset(AssetType.resource, UUID.fromString(sdcAsset.getString("uuid")),
							LifecycleState.undocheckout, ONAP.this.asdcOperator, null).waitForResult();
				}
				catch (Exception x) {
					log.error(EELFLoggerDelegate.errorLogger, "Cleanup ONAP SDC asset: " + sdcAsset.optString("uuid"), x);
				}
			}
		}

	}
	
	/** */
	private void sortAcumosSolutionRevisions(List<MLPSolutionRevision> theRevisions) {

		Collections.sort(theRevisions,
										 new Comparator<MLPSolutionRevision>() {
											@Override
											public int compare(MLPSolutionRevision theFirst, MLPSolutionRevision theSecond) {
												return String.CASE_INSENSITIVE_ORDER.compare(theFirst.getVersion(), theSecond.getVersion());
											}
										 });
										
	}
}

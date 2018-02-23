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

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

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
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.event.PeerSubscriptionEvent;
import org.acumos.federation.gateway.common.Clients;
import org.acumos.federation.gateway.common.FederationClient;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Component("onap")
@Scope("singleton")
@ConfigurationProperties(prefix = "onap")
@Conditional(ONAPAdapterCondition.class)
public class ONAP {

	private final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(ONAP.class);
	private ASDC asdc = new ASDC();
	private String asdcOperator;
	private TaskExecutor taskExecutor;
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
			// sdc assettype, categoty and subcategoty

			JSONArray sdcAssets = null;
			try {
				sdcAssets = asdc.getAssets(AssetType.resource, JSONArray.class, "Generic", "Abstract").waitForResult();
			}
			catch (Throwable x) {
				log.error(EELFLoggerDelegate.errorLogger, "Failed to list ONAP SDC assets: " + x.getCause(), x);
				// if this is a 404 NotFound, continue, otherwise, fail
				if (x instanceof ASDCException && x.getCause() instanceof HttpClientErrorException
						&& ((HttpClientErrorException) x.getCause()).getStatusCode() == HttpStatus.NOT_FOUND) {
					sdcAssets = new JSONArray();
				}
				else
					return;
			}
			// log.info(EELFLoggerDelegate.debugLogger, "Retrieved ONAP SDC assets: " +
			// sdcAssets);
			// log.info(EELFLoggerDelegate.debugLogger, "Received Acumos solutions: " +
			// this.solutions);

			for (MLPSolution acumosSolution : this.solutions) {
				try {
					// does it already exist in sdc
					JSONObject sdcAsset = lookupSdcAsset(acumosSolution, sdcAssets);
					if (sdcAsset == null) {
						// new solution
						sdcAsset = createSdcAsset(acumosSolution);
					}
					else {
						// ONAP.this.asdc.checkoutResource(UUID.fromString(sdcAsset.getString("artifactUUID")),
						// ONAP.this.asdcOperator, "updated solution import");
						sdcAsset = updateSdcAsset(sdcAsset, acumosSolution);
					}
					updateAssetArtifacts(sdcAsset, acumosSolution);
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
				if (same(theSolution, asset))
					return asset;
			}
			return null;
		}

		public JSONObject createSdcAsset(MLPSolution theSolution) throws Exception {
			log.info(EELFLoggerDelegate.debugLogger, "Creating ONAP SDC VF for solution " + theSolution);

			String description = null;// theSolution.getDescription();
			if (description == null)
				description = theSolution.getSolutionId();// + "@acumos";

			try {
				return ONAP.this.asdc.createVF().withCategory("Generic").withSubCategory("Abstract")
						.withName(theSolution.getName() + "-" + theSolution.getSolutionId()) // sdc names are unique,
																								// acumos ones not so
						.withDescription(description).withVendorName("AcumosInc").withVendorRelease("1.0") // cannot
																											// update so
																											// .. but
																											// might
																											// want to
																											// fetch the
																											// last
																											// Acumos
																											// revision
																											// version
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
		public JSONObject updateSdcAsset(JSONObject theAssetInfo, MLPSolution theSolution) {
			log.info(EELFLoggerDelegate.debugLogger,
					"Updating ONAP SDC VF " + theAssetInfo.optString("uuid") + " for Acumosb solution " + theSolution);
			return theAssetInfo;
		}

		public void updateAssetArtifacts(JSONObject theAssetInfo, MLPSolution theSolution) throws Exception {
			// fetch the solution artifacts: for now we'll do this for the latest acumos
			// revision
			FederationClient fedClient = clients.getFederationClient(this.peer.getApiUrl());

			List<MLPSolutionRevision> acumosRevisions = null;
			try {
				acumosRevisions = (List<MLPSolutionRevision>) fedClient
						.getSolutionRevisions(theSolution.getSolutionId()).getContent();
			}
			catch (Exception x) {
				log.error(EELFLoggerDelegate.errorLogger, "Failed to retrieve acumos revisions: " + x);
				throw x;
			}

			List<MLPArtifact> acumosArtifacts = null;
			try {
				acumosArtifacts = (List<MLPArtifact>) fedClient.getArtifacts(theSolution.getSolutionId(),
						acumosRevisions.get(acumosRevisions.size() - 1).getRevisionId()).getContent();
			}
			catch (Exception x) {
				log.error(EELFLoggerDelegate.errorLogger, "Failed to retrieve acumos artifacts" + x);
				throw x;
			}

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
			if (sdcArtifacts == null)
				sdcArtifacts = new JSONArray();

			// all this could be better writen but the 2 sets are expected to be small so we
			// favor readability

			// acumos artifacts that do not exist locally need to be added
			List<MLPArtifact> newArtifacts = new LinkedList<MLPArtifact>();
			Map<MLPArtifact, JSONObject> updatedArtifacts = new HashMap<MLPArtifact, JSONObject>();
			// List<JSONObject> oldArtifacts = new LinkedList<JSONObject>();

			log.info(EELFLoggerDelegate.debugLogger, "Acumos artifacts: " + acumosArtifacts);
			log.info(EELFLoggerDelegate.debugLogger, "ONAP SDC artifacts: " + sdcArtifacts);

			for (MLPArtifact acumosArtifact : acumosArtifacts) {
				boolean found = false;
				for (int i = 0; !found && i < sdcArtifacts.length(); i++) {
					JSONObject sdcArtifact = sdcArtifacts.getJSONObject(i);
					String sdcArtifactDescription = sdcArtifact.optString("artifactDescription");
					if (sdcArtifactDescription != null) {
						String[] parts = sdcArtifactDescription.split("@");
						if (parts.length == 2) {
							if (parts[0].equals(acumosArtifact.getArtifactId())) {
								found = true;
								// compare the versions so that we find the
								if (!parts[1].equals(acumosArtifact.getVersion()))
									updatedArtifacts.put(acumosArtifact, sdcArtifact);
							}
						}
					}
				}
				if (!found)
					newArtifacts.add(acumosArtifact);
			}

			log.info(EELFLoggerDelegate.debugLogger, "New SDC artifacts: " + newArtifacts);
			for (MLPArtifact acumosArtifact : newArtifacts) {
				try {
					byte[] content = IOUtils.toByteArray(new URI(acumosArtifact.getUri()));
					if (content == null)
						throw new Exception("Unable to fetch artifact content from " + acumosArtifact.getUri());
					if (content.length == 0)
						log.warn(EELFLoggerDelegate.debugLogger,
								"Acumos artifact has empty content, not acceptable in ONAP SDC");
					// more sophisticated mapping needed here
					asdc.createAssetArtifact(AssetType.resource, UUID.fromString(theAssetInfo.getString("uuid")))
							.withOperator(ONAP.this.asdcOperator).withContent(content)
							.withLabel(acumosArtifact.getArtifactTypeCode()).withName(acumosArtifact.getName())
							.withDisplayName(acumosArtifact.getMetadata()).withType(ArtifactType.OTHER)
							.withGroupType(ArtifactGroupType.DEPLOYMENT)
							.withDescription(acumosArtifact.getArtifactId() + "@"
									+ acumosArtifact.getVersion()/* acumosArtifact.getDescription() */)
							.execute().waitForResult();
				}
				catch (Exception x) {
					log.error(EELFLoggerDelegate.errorLogger, "Failed to create ONAP SDC VF Artifact", x);
				}
			}

			log.warn(EELFLoggerDelegate.debugLogger, "Updated SDC artifacts: " + updatedArtifacts.keySet());
			for (Map.Entry<MLPArtifact, JSONObject> updateEntry : updatedArtifacts.entrySet()) {
				MLPArtifact acumosArtifact = updateEntry.getKey();
				try {
					byte[] content = IOUtils.toByteArray(new URI(acumosArtifact.getUri()));
					if (content == null)
						throw new Exception("Unable to fetch artifact content from " + acumosArtifact.getUri());
					// more sophisticated mapping needed here
					asdc.updateAssetArtifact(AssetType.resource, UUID.fromString(theAssetInfo.getString("uuid")),
							updateEntry.getValue()).withOperator(ONAP.this.asdcOperator).withContent(content)
							// .withName(acumosArtifact.getName())
							.withDescription(acumosArtifact.getArtifactId() + "@"
									+ acumosArtifact.getVersion()/* acumosArtifact.getDescription() */)
							.execute().waitForResult();
				}
				catch (Exception x) {
					log.error(EELFLoggerDelegate.errorLogger, "Failed to update ONAP SDC VF Artifact", x);
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
					if (same(acumosArtifact, sdcArtifact)) {
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

		private boolean same(MLPSolution theAcumosSolution, JSONObject theSDCAsset) {

			return theSDCAsset.optString("name", "")
					.equals(theAcumosSolution.getName() + "-" + theAcumosSolution.getSolutionId());
		}

		private boolean same(MLPArtifact theAcumosArtifact, JSONObject theSDCArtifact) {

			String sdcArtifactDescription = theSDCArtifact.optString("artifactDescription");
			if (sdcArtifactDescription != null) {
				String[] parts = sdcArtifactDescription.split("@");
				if (parts.length == 2) {
					if (parts[0].equals(theAcumosArtifact.getArtifactId())) {
						return true;
					}
				}
			}
			return false;
		}

		// only safe to call if 'same' returned true
		/*
		 * private boolean sameVersion(MLPArtifact theAcumosArtifact, JSONObject
		 * theSDCArtifact) { return
		 * theSDCArtifact.optString("artifactDescription","@").split("@")[1].equals(
		 * theAcumosArtifact.getVersion()); }
		 */

		private boolean isAcumosOriginated(JSONObject theSDCArtifact) {
			boolean isAcumos = theSDCArtifact.optString("artifactType").equals(ArtifactType.OTHER.toString())
					&& theSDCArtifact.optString("artifactGroupType").equals(ArtifactGroupType.DEPLOYMENT.toString());
			String[] parts = theSDCArtifact.optString("artifactDescription", "@").split("@");
			isAcumos &= (parts.length == 2); // and the first part can be parsed as an UUID
			return isAcumos;
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
}

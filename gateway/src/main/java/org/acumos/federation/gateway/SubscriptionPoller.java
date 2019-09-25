/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2017-2019 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
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
package org.acumos.federation.gateway;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Function;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.client.ResourceAccessException;

import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPCatalog;
import org.acumos.cds.domain.MLPDocument;
import org.acumos.cds.domain.MLPNotification;
import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPPeerSubscription;
import org.acumos.cds.domain.MLPRevCatDescription;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;

import org.acumos.licensemanager.client.model.RegisterAssetRequest;
import org.acumos.licensemanager.client.model.RegisterAssetResponse;

import org.acumos.federation.client.FederationClient;
import org.acumos.federation.client.data.Solution;
import org.acumos.federation.client.data.SolutionRevision;

/**
 * Service bean for periodic polling of subscriptions to other peer's catalogs.
 */
public class SubscriptionPoller {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	@Autowired
	private TaskScheduler scheduler;

	@Autowired
	private PeerService peerService;

	@Autowired
	private CatalogService catalogService;

	@Autowired
	private ContentService contentService;

	@Autowired
	private FederationConfig federation;

	@Autowired
	private Clients clients;

	private static final ObjectMapper mapper = new ObjectMapper();
	private static final TypeReference<Map<String, Object>> trMapStoO = new TypeReference<Map<String, Object>>(){};

	private static <T> HashMap<String, T> index(List<T> list, Function<T, String> getId) {
		HashMap<String, T> ret = new HashMap<>();
		for (T item: list) {
			ret.put(getId.apply(item), item);
		}
		return ret;
	}

	private enum Action {
		PROCESS("Processed", "processing"),
		FETCH("Fetched", "fetching"),
		PARSE("Parsed", "parsing"),
		CREATE("Created", "creating"),
		UPDATE("Updated", "updating"),
		ADD("Added", "adding"),
		DELETE("Deleted", "deleting"),
		COPY("Copied", "copying");

		private String done;
		private String during;

		public String getDone() {
			return(done);
		}

		public String getDuring() {
			return(during);
		}

		Action(String done, String during) {
			this.done = done;
			this.during = during;
		}
	}

	private static class PendingAction	{
		private PendingAction parent;
		private Action action;
		private String item;
		private boolean force;
		private Instant start = Instant.now();

		public PendingAction(PendingAction parent, Action action, String item, boolean force) {
			this.parent = parent;
			this.action = action;
			this.item = " " + item;
			this.force = force;
		}

		public void setForce() {
			this.force = true;
		}

		public boolean getForce() {
			return(this.force);
		}

		public PendingAction pop() {
			PendingAction ret = parent;
			parent = null;
			return(ret);
		}

		public String getDone() {
			return(action.getDone() + item);
		}

		public String getDuring() {
			return(action.getDuring() + item);
		}

		public String getItem() {
			return(item);
		}
		
		public Instant getStart() {
			return(start);
		}
	}

	private static class Notifier	{
		private PendingAction actions;
		private PendingAction leaf;
		private ICommonDataServiceRestClient cds;
		private String userId;

		public Notifier(ICommonDataServiceRestClient cds, String userId) {
			this.cds = cds;
			this.userId = userId;
		}

		public PendingAction begin(String item, Object... args) {
			end();
			actions = new PendingAction(actions, Action.PROCESS, String.format(item, args), false);
			return actions;
		}

		public void noteEnd(PendingAction handle) {
			handle.setForce();
			end(handle);
		}

		public void end(PendingAction handle) {
			end();
			do {
				leaf = actions;
				actions = leaf.pop();
			} while (end() != handle);
		}

		public void check(Action action, String item, Object... args) {
			end();
			leaf = new PendingAction(null, action, String.format(item, args), false);
		}

		public void action(Action action, String item, Object... args) {
			end();
			leaf = new PendingAction(null, action, String.format(item, args), true);
		}

		private void note(PendingAction cur, String sev, String msg) {
			MLPNotification note = new MLPNotification(cur.getItem(), sev, cur.getStart(), Instant.now());
			note.setMessage(msg);
			cds.addUserToNotification(cds.createNotification(note).getNotificationId(), userId);
		}

		public PendingAction end() {
			if (leaf != null) {
				PendingAction cur = leaf;
				boolean logit = leaf.getForce();
				leaf = null;
				if (logit) {
					note(cur, "LO", cur.getDone());
				}
				return(cur);
			}
			return(null);
		}

		public void fail(PendingAction handle, String msg) {
			String format = "Error occurred while %s: %s";
			String sev = "HI";
			PendingAction cur = null;
			do {
				cur = leaf;
				leaf = null;
				if (cur == null) {
					cur = actions;
					actions = cur.pop();
				}
				String during = cur.getDuring();
				note(cur, sev, String.format(format, during, msg));
				msg = during;
				format = "While %s, an error occurred: %s";
				sev = "ME";
			} while (cur != handle && actions != null);
		}
	}

	private class PeerSubscriptionPoller implements Runnable {
		private long subId;
		private String userId;
		private String peerId;
		private Long interval;
		private ScheduledFuture future;
		private Notifier events;

		public PeerSubscriptionPoller(long subId, String userId, String peerId, Long interval) {
			this.subId = subId;
			this.userId = userId;
			this.peerId = peerId;
			this.interval = interval;
		}

		public Long getInterval() {
			return interval;
		}

		public void setInterval(Long interval) {
			this.interval = interval;
		}

		public synchronized void cancel() {
			if (future != null) {
				future.cancel(false);
				future = null;
			}
		}

		public synchronized void schedule() {
			if (interval == null || interval.longValue() <= 0L) {
				scheduler.schedule(this, Instant.now());
			} else {
				future = scheduler.scheduleAtFixedRate(this, 1000L * interval.longValue());
			}
		}

		private boolean checkRevision(String revisionId, String solutionId, String catalogId, FederationClient peer) {
			log.info("Checking revision {} from peer {}", revisionId, peerId);
			PendingAction act = events.begin("revision %s", revisionId);
			events.check(Action.FETCH, "remote revision");
			SolutionRevision pRev = (SolutionRevision)peer.getSolutionRevision(solutionId, revisionId, catalogId);
			events.check(Action.FETCH, "local revision");
			SolutionRevision lRev = (SolutionRevision)catalogService.getRevision(revisionId, catalogId);
			boolean changed = false;
			boolean isnew = lRev == null;
			pRev.setUserId(userId);
			if (isnew) {
				log.info("Revision {} doesn't exist locally.  Creating it", revisionId);
				pRev.setSourceId(peerId);
				events.action(Action.CREATE, "revision %s", revisionId);
				lRev = (SolutionRevision)catalogService.createRevision(pRev);
			}
			MLPRevCatDescription pDesc = pRev.getRevCatDescription();
			MLPRevCatDescription lDesc = lRev.getRevCatDescription();
			if (pDesc != null) {
				if (lDesc == null) {
					log.info("Description for revision {} in catalog {} doesn't exist locally.  Creating it", revisionId, catalogId);
					events.action(Action.CREATE, "revision description");
					catalogService.createDescription(pDesc);
				} else if (!Objects.equals(pDesc.getDescription(), lDesc.getDescription())) {
					log.info("Updating description for revision {} in catalog {}", revisionId, catalogId);
					events.action(Action.UPDATE, "revision description");
					catalogService.updateDescription(pDesc);
				}
			} else if (lDesc != null) {
				log.info("Deleting old description for revision {} in catalog {}", revisionId, catalogId);
				events.action(Action.DELETE, "revision description");
				catalogService.deleteDescription(revisionId, catalogId);
			}
			List<MLPArtifact> pArts = pRev.getArtifacts();
			HashMap<String, MLPArtifact> lArts = index(lRev.getArtifacts(), MLPArtifact::getArtifactId);
			for (MLPArtifact pArt: pArts) {
				String artifactId = pArt.getArtifactId();
				log.debug("Checking artifact {} from peer {}", artifactId, peerId);
				String pTag = pArt.getDescription();
				pArt.setUserId(userId);
				contentService.setArtifactUri(solutionId, pArt);
				MLPArtifact lArt = lArts.get(artifactId);
				if (lArt == null) {
					events.check(Action.FETCH, "local artifact %s metadata", artifactId);
					lArt = catalogService.getArtifact(artifactId);
				}
				if (lArt == null) {
					log.info("Artifact {} doesn't exist locally.  Creating it", artifactId);
					events.action(Action.CREATE, "artifact %s metadata", artifactId);
					lArt = catalogService.createArtifact(pArt);
				} else if (!Objects.equals(pArt.getSize(), lArt.getSize()) || !Objects.equals(pArt.getVersion(), lArt.getVersion())) {
					log.info("Updating artifact {}", artifactId);
					events.action(Action.UPDATE, "artifact %s metadata", artifactId);
					catalogService.updateArtifact(pArt);
				} else {
					continue;
				}
				changed = true;
				events.check(Action.FETCH, "artifact %s content", artifactId);
				try (InputStream is = peer.getArtifactContent(artifactId)) {
					events.action(Action.COPY, "artifact %s content", artifactId);
					contentService.putArtifactContent(pArt, pTag, is);
				} catch (IOException ioe) {
					throw new ResourceAccessException("Failure copying artifact " + artifactId + " from peer " + peerId, ioe);
				}
			}
			for (MLPArtifact pArt: pArts) {
				if (lArts.get(pArt.getArtifactId()) == null) {
					log.info("Adding artifact {} to revision {}", pArt.getArtifactId(), revisionId);
					events.action(Action.ADD, "artifact %s to revision %s", pArt.getArtifactId(), revisionId);
					catalogService.addArtifact(solutionId, revisionId, pArt.getArtifactId());
				}
			}
			List<MLPDocument> pDocs = pRev.getDocuments();
			HashMap<String, MLPDocument> lDocs = index(lRev.getDocuments(), MLPDocument::getDocumentId);
			for (MLPDocument pDoc: pDocs) {
				String documentId = pDoc.getDocumentId();
				log.debug("Checking document {} from peer {}", documentId, peerId);
				pDoc.setUserId(userId);
				contentService.setDocumentUri(solutionId, pDoc);
				MLPDocument lDoc = lDocs.get(documentId);
				if (lDoc == null) {
					events.check(Action.FETCH, "local document %s metadata", documentId);
					lDoc = catalogService.getDocument(documentId);
				}
				if (lDoc == null) {
					log.info("Document {} doesn't exist locally.  Creating it", documentId);
					events.action(Action.CREATE, "document %s metadata", documentId);
					catalogService.createDocument(pDoc);
				} else if (!Objects.equals(pDoc.getSize(), lDoc.getSize()) || !Objects.equals(pDoc.getVersion(), lDoc.getVersion())) {
					log.info("Updating document {}", documentId);
					events.action(Action.UPDATE, "document %s metadata", documentId);
					catalogService.updateDocument(pDoc);
				} else {
					continue;
				}
				events.check(Action.FETCH, "document %s content", documentId);
				try (InputStream is = peer.getDocumentContent(documentId)) {
					events.action(Action.COPY, "document %s content", documentId);
					contentService.putDocumentContent(pDoc, is);
				} catch (IOException ioe) {
					throw new ResourceAccessException("Failure copying document " + documentId + " from peer " + peerId, ioe);
				}
			}
			for (MLPDocument pDoc: pDocs) {
				if (lDocs.get(pDoc.getDocumentId()) == null) {
					log.info("Adding document {} to revision {} in catalog {}", pDoc.getDocumentId(), revisionId, catalogId);
					events.action(Action.ADD, "document %s to revision %s in catalog %s", pDoc.getDocumentId(), revisionId, catalogId);
					catalogService.addDocument(revisionId, catalogId, pDoc.getDocumentId());
				}
			}
			changed |= isnew;
			if (changed && !isnew) {
				events.action(Action.UPDATE, "revision %s", revisionId);
				catalogService.updateRevision(pRev);
			}
			if (changed) {
				new Thread(() -> {
					try {
						clients.getSVClient().securityVerificationScan(solutionId, revisionId, "created", userId);
					} catch (Exception e) {
						log.error("SV scan failure on revision " + revisionId, e);
					}
				}).start();
				new Thread(() -> {
					try {
						RegisterAssetRequest rar = new RegisterAssetRequest();
						rar.setSolutionId(solutionId);
						rar.setRevisionId(revisionId);
						rar.setLoggedIdUser(userId);
						RegisterAssetResponse rax = clients.getLMClient().register(rar).get();
						if (!rax.isSuccess()) {
							log.error("License asset registration failure on revision " + revisionId + ": " + rax.getMessage());
						}
					} catch (Exception e) {
						log.error("License asset registration failure on revision " + revisionId, e);
					}
				}).start();
			}
			events.end(act);
			return(changed);
		}

		private void checkSolution(String solutionId, String catalogId, boolean inLocalCatalog, FederationClient peer) {
			log.info("Checking solution {} from peer {}", solutionId, peerId);
			PendingAction act = events.begin("solution %s", solutionId);
			events.check(Action.FETCH, "remote solution");
			Solution pSol = (Solution)peer.getSolution(solutionId);
			events.check(Action.FETCH, "local solution");
			Solution lSol = (Solution)catalogService.getSolution(solutionId);
			boolean changed = false;
			boolean isnew = lSol == null;
			if (isnew) {
				log.info("Solution {} doesn't exist locally.  Creating it", solutionId);
				pSol.setDownloadCount(0L);
				pSol.setFeatured(false);
				pSol.setRatingAverageTenths(0L);
				pSol.setRatingCount(0L);
				pSol.setSourceId(peerId);
				pSol.setUserId(userId);
				pSol.setViewCount(0L);
				events.action(Action.CREATE, "solution %s", solutionId);
				lSol = (Solution)catalogService.createSolution(pSol);
			} else {
				pSol.setActive(lSol.isActive());
				pSol.setDownloadCount(lSol.getDownloadCount());
				pSol.setFeatured(lSol.isFeatured());
				pSol.setMetadata(lSol.getMetadata());
				pSol.setName(lSol.getName());
				pSol.setOrigin(lSol.getOrigin());
				pSol.setRatingAverageTenths(lSol.getRatingAverageTenths());
				pSol.setRatingCount(lSol.getRatingCount());
				pSol.setToolkitTypeCode(lSol.getToolkitTypeCode());
				pSol.setViewCount(lSol.getViewCount());
				if (lSol.getSourceId() == null) {
					pSol.setSourceId(null);
					pSol.setUserId(lSol.getUserId());
				} else {
					pSol.setUserId(userId);
				}
				changed |= !pSol.getTags().equals(lSol.getTags());
				changed |= !Objects.equals(lSol.getSourceId(), pSol.getSourceId());
				changed |= !Objects.equals(lSol.getUserId(), pSol.getUserId());
			}
			if (!Arrays.equals(lSol.getPicture(), pSol.getPicture())) {
				log.info("Updating picture for solution {}", solutionId);
				events.action(Action.UPDATE, "picture for solution %s", solutionId);
				catalogService.savePicture(solutionId, pSol.getPicture());
			}
			if (!inLocalCatalog) {
				log.info("Adding solution {} to catalog {}", solutionId, catalogId);
				events.action(Action.ADD, "solution %s to catalog %s", solutionId, catalogId);
				catalogService.addSolution(solutionId, catalogId);
			}
			for (MLPSolutionRevision rev: pSol.getRevisions()) {
				changed |= checkRevision(rev.getRevisionId(), solutionId, catalogId, peer);
			}
			if (changed && !isnew) {
				events.action(Action.UPDATE, "solution %s", solutionId);
				catalogService.updateSolution(pSol);
				log.info("Updated solution {} from peer {}", solutionId, peerId);
			}
			events.end(act);
		}

		private void checkCatalog(String catalogId) {
			log.info("Checking catalog {} from peer {}", catalogId, peerId);
			PendingAction act = events.begin("catalog %s from peer %s", catalogId, peerId);
			FederationClient peer = clients.getFederationClient(peerService.getPeer(peerId).getApiUrl());
			events.check(Action.FETCH, "list of solutions in remote catalog");
			List<MLPSolution> peerSolutions = peer.getSolutions(catalogId);
			events.check(Action.FETCH, "list of solutions in local catalog");
			HashMap<String, MLPSolution> localSolutions = index(catalogService.getSolutions(catalogId), MLPSolution::getSolutionId);
			if (localSolutions.isEmpty() && !peerSolutions.isEmpty() && index(catalogService.getAllCatalogs(), MLPCatalog::getCatalogId).get(catalogId) == null) {
				log.info("Catalog {} doesn't exist locally.  Creating it", catalogId);
				events.action(Action.CREATE, "catalog %s", catalogId);
				catalogService.createCatalog(index(peer.getCatalogs(), MLPCatalog::getCatalogId).get(catalogId));
				events.end();
			}
			for (MLPSolution solution: peerSolutions) {
				checkSolution(solution.getSolutionId(), catalogId, localSolutions.get(solution.getSolutionId()) != null, peer);
			}
			log.info("Checked catalog {} from peer {}", catalogId, peerId);
			events.noteEnd(act);
		}

		private void checkSubscription() {
			log.info("Processing subscription {} for peer {}", subId, peerId);
			PendingAction act = events.begin("subscription %s for peer %s", subId, peerId);
			events.check(Action.FETCH, "subscription %s", subId);
			MLPPeerSubscription subscription = peerService.getSubscription(subId);
			events.check(Action.PARSE, "subscription's selector");
			Object xcatalogs;
			try {
				xcatalogs = ((Map<String, Object>)mapper.readValue(subscription.getSelector(), trMapStoO)).get("catalogId");
				if (xcatalogs instanceof String) {
					xcatalogs = new String[] { (String)xcatalogs };
				} else {
					xcatalogs = ((List)xcatalogs).toArray(new String[((List)xcatalogs).size()]);
				}
			} catch (IOException | NullPointerException | ArrayStoreException | ClassCastException ioe) {
				log.error(String.format("Malformed selector %s on subscription %s to peer %s", subscription.getSelector(), subId, peerId));
				events.fail(act, "Subscription selector was malformed");
				return;
			}
			events.end();
			String[] catalogs = (String[])xcatalogs;
			Instant startTime = Instant.now();
			for (String catalogId: catalogs) {
				checkCatalog(catalogId);
			}
			subscription.setProcessed(startTime);
			peerService.updateSubscription(subscription);
			log.info("Subscription {} processed for peer {}", subId, peerId);
			events.end(act);
		}

		public void run() {
			events = new Notifier(clients.getCDSClient(), userId);
			try {
				checkSubscription();
			} catch (Exception ex) {
				log.error(String.format("Unexpected error processing subscription %s for peer %s", subId, peerId), ex);
				events.fail(null, ex.toString());
			}
		}
	}


	private HashMap<Long, PeerSubscriptionPoller> subscriptions = new HashMap<>();

	/**
	 * Schedule an immediate poll of the specified subscription.
	 * @param subscription The subscription to poll.
	 */
	public void
	triggerSubscription(MLPPeerSubscription subscription) {
		require(subscription, true);
	}

	/**
	 * Check for changes to the list of peers and subscriptions.
	 */
	@Scheduled(initialDelay=5000,fixedRateString="${peer.jobchecker.interval:400}000")
	public void checkPeerJobs() {
		HashSet<Long> valid = new HashSet<>();
		for (MLPPeer peer: peerService.getPeers()) {
			if (peer.isSelf()) {
				continue;
			}
			for (MLPPeerSubscription subscription: peerService.getSubscriptions(peer.getPeerId())) {
				require(subscription, false);
				valid.add(subscription.getSubId());
			}
		}
		HashSet<Long> invalid = new HashSet<>();
		for (Long subid: subscriptions.keySet()) {
			if (!valid.contains(subid)) {
				invalid.add(subid);
			}
		}
		for (Long subid: invalid) {
			subscriptions.remove(subid).cancel();
		}
	}


	private void require(MLPPeerSubscription subscription, boolean force) {
		Long subId = subscription.getSubId();
		Long interval = subscription.getRefreshInterval();
		PeerSubscriptionPoller poller = subscriptions.get(subId);
		if (poller == null) {
			poller = new PeerSubscriptionPoller(subId, subscription.getUserId(), subscription.getPeerId(), interval);
			subscriptions.put(subId, poller);
			if (force || (interval != null && (interval.longValue() > 0L || subscription.getProcessed() == null))) {
				poller.schedule();
			}
		} else if (force || (interval != null && !interval.equals(poller.getInterval()))) {
			poller.cancel();
			poller.setInterval(interval);
			poller.schedule();
		} else if (interval == null && poller.getInterval() != null) {
			poller.cancel();
			poller.setInterval(interval);
		}
	}
}

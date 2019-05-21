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

import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPCatalog;
import org.acumos.cds.domain.MLPDocument;
import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPPeerSubscription;
import org.acumos.cds.domain.MLPRevCatDescription;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;

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

	private class PeerSubscriptionPoller implements Runnable {
		private long subId;
		private String peerId;
		private Long interval;
		private ScheduledFuture future;
		private String userId;

		public PeerSubscriptionPoller(long subId, String peerId, Long interval) {
			this.subId = subId;
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
			SolutionRevision pRev = (SolutionRevision)peer.getSolutionRevision(solutionId, revisionId, catalogId);
			SolutionRevision lRev = (SolutionRevision)catalogService.getRevision(revisionId, catalogId);
			boolean changed = false;
			boolean isnew = lRev == null;
			pRev.setUserId(userId);
			if (isnew) {
				log.info("Revision {} doesn't exist locally.  Creating it", revisionId);
				pRev.setSourceId(peerId);
				lRev = (SolutionRevision)catalogService.createRevision(pRev);
			}
			MLPRevCatDescription pDesc = pRev.getRevCatDescription();
			MLPRevCatDescription lDesc = lRev.getRevCatDescription();
			if (pDesc != null) {
				if (lDesc == null) {
					log.info("Description for revision {} in catalog {} doesn't exist locally.  Creating it", revisionId, catalogId);
					catalogService.createDescription(pDesc);
				} else if (!Objects.equals(pDesc.getDescription(), lDesc.getDescription())) {
					log.info("Updating description for revision {} in catalog {}", revisionId, catalogId);
					catalogService.updateDescription(pDesc);
				}
			} else if (lDesc != null) {
				log.info("Deleting old description for revision {} in catalog {}", revisionId, catalogId);
				catalogService.deleteDescription(revisionId, catalogId);
			}
			List<MLPArtifact> pArts = pRev.getArtifacts();
			HashMap<String, MLPArtifact> lArts = index(lRev.getArtifacts(), MLPArtifact::getArtifactId);
			for (MLPArtifact pArt: pArts) {
				String artifactId = pArt.getArtifactId();
				log.debug("Checking artifact {} from peer {}", artifactId, peerId);
				String pTag = pArt.getDescription();
				MLPArtifact lArt = catalogService.getArtifact(artifactId);
				pArt.setUserId(userId);
				contentService.setArtifactUri(solutionId, pArt);
				if (lArt == null) {
					log.info("Artifact {} doesn't exist locally.  Creating it", artifactId);
					lArt = catalogService.createArtifact(pArt);
				} else if (!Objects.equals(pArt.getSize(), lArt.getSize()) || !Objects.equals(pArt.getVersion(), lArt.getVersion())) {
					log.info("Updating artifact {}", artifactId);
					catalogService.updateArtifact(pArt);
				} else {
					continue;
				}
				changed = true;
				try (InputStream is = peer.getArtifactContent(artifactId)) {
					contentService.putArtifactContent(pArt, pTag, is);
				} catch (IOException ioe) {
					throw new ResourceAccessException("Failure copying artifact " + artifactId + " from peer " + peerId, ioe);
				}
			}
			for (MLPArtifact pArt: pArts) {
				if (lArts.get(pArt.getArtifactId()) == null) {
					log.info("Adding artifact {} to revision {}", pArt.getArtifactId(), revisionId);
					catalogService.addArtifact(solutionId, revisionId, pArt.getArtifactId());
				}
			}
			List<MLPDocument> pDocs = pRev.getDocuments();
			HashMap<String, MLPDocument> lDocs = index(lRev.getDocuments(), MLPDocument::getDocumentId);
			for (MLPDocument pDoc: pDocs) {
				String documentId = pDoc.getDocumentId();
				log.debug("Checking document {} from peer {}", documentId, peerId);
				MLPDocument lDoc = catalogService.getDocument(documentId);
				pDoc.setUserId(userId);
				contentService.setDocumentUri(solutionId, pDoc);
				if (lDoc == null) {
					log.info("Document {} doesn't exist locally.  Creating it", documentId);
					catalogService.createDocument(pDoc);
				} else if (!Objects.equals(pDoc.getSize(), lDoc.getSize()) || !Objects.equals(pDoc.getVersion(), lDoc.getVersion())) {
					log.info("Updating document {}", documentId);
					catalogService.updateDocument(pDoc);
				} else {
					continue;
				}
				try (InputStream is = peer.getDocumentContent(documentId)) {
					contentService.putDocumentContent(pDoc, is);
				} catch (IOException ioe) {
					throw new ResourceAccessException("Failure copying document " + documentId + " from peer " + peerId, ioe);
				}
			}
			for (MLPDocument pDoc: pDocs) {
				if (lDocs.get(pDoc.getDocumentId()) == null) {
					log.info("Adding document {} to revision {} in catalog {}", pDoc.getDocumentId(), revisionId, catalogId);
					catalogService.addDocument(revisionId, catalogId, pDoc.getDocumentId());
				}
			}
			changed |= isnew;
			if (changed && !isnew) {
				catalogService.updateRevision(pRev);
			}
			return changed;
		}

		private void checkSolution(String solutionId, String catalogId, boolean inLocalCatalog, FederationClient peer) {
			log.info("Checking solution {} from peer {}", solutionId, peerId);
			Solution pSol = (Solution)peer.getSolution(solutionId);
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
				catalogService.savePicture(solutionId, pSol.getPicture());
			}
			if (!inLocalCatalog) {
				log.info("Adding solution {} to catalog {}", solutionId, catalogId);
				catalogService.addSolution(solutionId, catalogId);
			}
			for (MLPSolutionRevision rev: pSol.getRevisions()) {
				changed |= checkRevision(rev.getRevisionId(), solutionId, catalogId, peer);
			}
			if (changed && !isnew) {
				catalogService.updateSolution(pSol);
				log.info("Updated solution {} from peer {}", solutionId, peerId);
			}
		}

		private void checkCatalog(String catalogId) {
			log.info("Checking catalog {} from peer {}", catalogId, peerId);
			FederationClient peer = clients.getFederationClient(peerService.getPeer(peerId).getApiUrl());
			List<MLPSolution> peerSolutions = peer.getSolutions(catalogId);
			HashMap<String, MLPSolution> localSolutions = index(catalogService.getSolutions(catalogId), MLPSolution::getSolutionId);
			if (localSolutions.isEmpty() && !peerSolutions.isEmpty() && index(catalogService.getAllCatalogs(), MLPCatalog::getCatalogId).get(catalogId) == null) {
				log.info("Catalog {} doesn't exist locally.  Creating it", catalogId);
				catalogService.createCatalog(index(peer.getCatalogs(), MLPCatalog::getCatalogId).get(catalogId));
			}
			for (MLPSolution solution: peer.getSolutions(catalogId)) {
				checkSolution(solution.getSolutionId(), catalogId, localSolutions.get(solution.getSolutionId()) != null, peer);
			}
			log.info("Checked catalog {} from peer {}", catalogId, peerId);
		}

		private void checkSubscription() {
			log.info("Processing subscription {} for peer {}", subId, peerId);
			MLPPeerSubscription subscription = peerService.getSubscription(subId);
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
				return;
			}
			String[] catalogs = (String[])xcatalogs;
			Instant startTime = Instant.now();
			userId = subscription.getUserId();
			for (String catalogId: catalogs) {
				checkCatalog(catalogId);
			}
			subscription.setProcessed(startTime);
			peerService.updateSubscription(subscription);
			log.info("Subscription {} processed for peer {}", subId, peerId);
		}

		public void run() {
			try {
				checkSubscription();
			} catch (Exception ex) {
				log.error(String.format("Unexpected error processing subscription %s for peer %s", subId, peerId), ex);
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
			poller = new PeerSubscriptionPoller(subId, subscription.getPeerId(), interval);
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

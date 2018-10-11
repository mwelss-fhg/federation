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

package org.acumos.federation.gateway.task;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PreDestroy;

import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPPeerSubscription;
import org.acumos.federation.gateway.cds.PeerStatus;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.service.PeerService;
import org.acumos.federation.gateway.service.PeerSubscriptionService;
import org.acumos.federation.gateway.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * 
 * Task Scheduler for the Federated Gateway of Acumos
 */
@Component
@EnableScheduling
public class PeerSubscriptionTaskScheduler {

	private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private Environment env;
	@Autowired
	private PeerService peerService;
	@Autowired
	private PeerSubscriptionService peerSubscriptionService;
	@Autowired
	private ApplicationContext appCtx;
	@Autowired
	private TaskScheduler taskScheduler = null;

	private Table<String, Long, PeerTaskHandler> peersSubsTask = HashBasedTable.create();

	@PreDestroy
	public void cleanUpTasks() {
		log.debug(EELFLoggerDelegate.debugLogger, "cleanUpTasks");
		try {
			log.debug(EELFLoggerDelegate.debugLogger, "cleanUpTasks: " + this.peersSubsTask.size() + " tasks");
			//this.taskScheduler.shutdown();

		}
		catch (Exception e) {
			log.error(EELFLoggerDelegate.errorLogger, "Exception occurred while cleanUpTasks: ", e);
		}
	}

	protected boolean same(MLPPeerSubscription theFirstSub, MLPPeerSubscription theSecondSub) {
		log.debug(EELFLoggerDelegate.debugLogger,
				"comparing subs : [" + theFirstSub.getSubId() + "," + theFirstSub.getCreated() + ","
						+ theFirstSub.getModified() + "] vs. [" + theSecondSub.getSubId() + ","
						+ theSecondSub.getCreated() + "," + theSecondSub.getModified() + "]");
		return theFirstSub.getSubId() == theSecondSub.getSubId()
				&& theFirstSub.getCreated().equals(theSecondSub.getCreated())
				&& ((theFirstSub.getModified() == null && theSecondSub.getModified() == null)
						|| (theFirstSub.getModified() != null && theSecondSub.getModified() != null
								&& theFirstSub.getModified().equals(theSecondSub.getModified())));
	}

	/**
	 * Schedule a one time execution of the subscription. The scheduler will not track the execution of suck a task.
	 */
	public void runOnce(MLPPeer thePeer, MLPPeerSubscription theSub) {
		new PeerTaskHandler().runTask(thePeer, theSub);
	}

	/** */
	private boolean shouldRun(MLPPeerSubscription theSub) {
		if (theSub.getRefreshInterval() == null) {
			//on demand only subscription
			return false;
		}

		if (theSub.getRefreshInterval().longValue() == 0 &&
				theSub.getProcessed() != null) {
			//one timer that has already been processed
			return false;
		}

		return true;
	}

	private void terminatePeerSubsTask(String thePeerId) {

		Map<Long, PeerTaskHandler> subsTask = this.peersSubsTask.row(thePeerId);
		Set<Long> subsToTerminate = null;

		if (subsTask != null) {
			subsToTerminate = new HashSet<Long>();

			for (Map.Entry<Long, PeerTaskHandler> subTaskEntry : subsTask.entrySet()) {
				subTaskEntry.getValue().stopTask();
				subsToTerminate.add(subTaskEntry.getKey());
				log.debug(EELFLoggerDelegate.debugLogger,	"Terminated task for peer {} subscription {}",
									thePeerId, subTaskEntry.getKey());
			}
		}

		for (Long subId: subsToTerminate) {	
			this.peersSubsTask.remove(thePeerId, subId);
		}
	}

	@Scheduled(initialDelay = 5000, fixedRateString = "${peer.jobchecker.interval:400}000")
	public void checkPeerJobs() {

		log.debug(EELFLoggerDelegate.debugLogger, "checkPeerSubscriptionJobs");
		// Get the List of MLP Peers
		List<MLPPeer> peers = peerService.getPeers();
		if (Utils.isEmptyList(peers)) {
			log.info(EELFLoggerDelegate.debugLogger, "no peers from " + peerService);
			return;
		}

		//terminate peer tasks for deleted peers
		Set<String> activePeerIds = this.peersSubsTask.rowKeySet();
		Set<String> peersToTerminate = new HashSet<String>();
		for (String activePeerId: activePeerIds) {
			MLPPeer activePeer = peers.stream().filter(peer -> peer.getPeerId().equals(activePeerId)).findFirst().orElse(null);
			if (activePeer == null) {
				peersToTerminate.add(activePeerId);
			}
		}

		for (String peerId: peersToTerminate) {
			terminatePeerSubsTask(peerId);
		}

		//for each existing peer
		for (MLPPeer peer : peers) {
			log.info(EELFLoggerDelegate.debugLogger, "check peer {}", peer);

			if (peer.isSelf())
				continue;

			log.debug(EELFLoggerDelegate.debugLogger,	"processing peer {}", peer.getPeerId());
			// terminate peer tasks for non-active peers
			if (PeerStatus.Active != PeerStatus.forCode(peer.getStatusCode())) {
				// cancel all peer sub tasks for this peer
				log.debug(EELFLoggerDelegate.debugLogger,
						"peer {} no longer active, terminating active tasks", peer);
				terminatePeerSubsTask(peer.getPeerId());
				continue;
			}

			//currently provisioned peer subs
			List<MLPPeerSubscription> peerSubs = peerSubscriptionService.getPeerSubscriptions(peer.getPeerId());
			//currently active peer subs
			Map<Long, PeerTaskHandler> peerSubsTask = this.peersSubsTask.row(peer.getPeerId());

			//terminate all active peer sub tasks that have no provisioned equivalent
			if (peerSubsTask != null) {
				Set<Long> subsToTerminate = new HashSet<Long>();
				for (Map.Entry<Long, PeerTaskHandler> peerSubTaskEntry : peerSubsTask.entrySet()) {
					//fugly
					if (!peerSubs.stream()
								.filter(peerSub -> peerSub.getSubId().equals(peerSubTaskEntry.getKey())).findAny().isPresent()) {
						peerSubTaskEntry.getValue().stopTask();
						subsToTerminate.add(peerSubTaskEntry.getKey());
						log.debug(EELFLoggerDelegate.debugLogger,	"Terminated task for peer {} subscription {}",
											peer.getPeerId(), peerSubTaskEntry.getKey());
					}
				}

				for (Long subId: subsToTerminate) {
					this.peersSubsTask.remove(peer.getPeerId(), subId);
				}
			}

			//start/update tasks for all current subscriptions
			for (MLPPeerSubscription peerSub : peerSubs) {
				log.info(EELFLoggerDelegate.debugLogger, "checkSub " + peerSub);
				PeerTaskHandler peerSubTask = this.peersSubsTask.get(peer.getPeerId(), peerSub.getSubId());
				if (peerSubTask != null) {
					MLPPeerSubscription taskSub = peerSubTask.getSubscription();
					// was the subscription updated? if yes, cancel current task.
					//TODO: this does not correctly identify one time executions that were completed
					if (!((peerSub.getModified() == null && taskSub.getModified() == null) ||
							  (peerSub.getModified() != null && taskSub.getModified() != null &&
								 peerSub.getModified().equals(taskSub.getModified())))) {
						log.debug(EELFLoggerDelegate.debugLogger,
								"peer {} subscription {} was updated, terminating current task", peer.getPeerId(), peerSub.getSubId());
						peerSubTask.stopTask();
						peerSubTask = null;
						//this remove can be inlines as we are iterating over peersSubsTasks at this time
						this.peersSubsTask.remove(peer.getPeerId(), peerSub.getSubId());
					}
				}

				if (peerSubTask == null && shouldRun(peerSub)) {
					log.info(EELFLoggerDelegate.debugLogger, "Scheduling peer sub task for peer {}, subscription {}", peer.getName(), peerSub.getSubId());
					PeerTaskHandler hnd = new PeerTaskHandler().startTask(peer, peerSub);
					if (hnd != null) {
						this.peersSubsTask.put(peer.getPeerId(), peerSub.getSubId(), hnd);
					}
				}
			}
		}
	}

	/**
	 * We need this contraption simply because we cannot retrieve the task (in order
	 * to check the subscription it is working on) from the ScheduledFuture ..
	 */
	private class PeerTaskHandler {

		private ScheduledFuture future;
		private PeerSubscriptionTask task;

		public synchronized PeerTaskHandler startTask(MLPPeer thePeer, MLPPeerSubscription theSub) {
			Long refreshInterval = theSub.getRefreshInterval();
	
			this.task = (PeerSubscriptionTask) PeerSubscriptionTaskScheduler.this.appCtx.getBean("peerSubscriptionTask");
			if (refreshInterval.longValue() == 0) {
				this.future = PeerSubscriptionTaskScheduler.this.taskScheduler
												.schedule(this.task.handle(thePeer, theSub), new Date(System.currentTimeMillis() + 5000));
			}
			else {
				this.future = PeerSubscriptionTaskScheduler.this.taskScheduler
												.scheduleAtFixedRate(this.task.handle(thePeer, theSub), 1000 * refreshInterval.longValue() );
			}
			return this;
		}
		
		public synchronized PeerTaskHandler runTask(MLPPeer thePeer, MLPPeerSubscription theSub) {
			this.task = (PeerSubscriptionTask) PeerSubscriptionTaskScheduler.this.appCtx.getBean("peerSubscriptionTask");
			this.future = PeerSubscriptionTaskScheduler.this.taskScheduler
												.schedule(this.task.handle(thePeer, theSub), new Date(System.currentTimeMillis() + 5000));
			return this;
		}

		public synchronized PeerTaskHandler stopTask() {
			if (this.future == null)
				throw new IllegalStateException("Not started");

			this.future.cancel(true);
			return this;
		}

		public synchronized MLPPeerSubscription getSubscription() {
			if (this.task == null)
				throw new IllegalStateException("Not started");

			return this.task.getSubscription();
		}
	}	

}

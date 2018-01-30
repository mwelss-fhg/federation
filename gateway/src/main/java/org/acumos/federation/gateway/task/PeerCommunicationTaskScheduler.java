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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PreDestroy;

import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPPeerSubscription;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.service.PeerService;
import org.acumos.federation.gateway.service.PeerSubscriptionService;
import org.acumos.federation.gateway.task.PeerCommunicationTask;
import org.acumos.federation.gateway.util.Utils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * 
 * Task Scheduler for the Federated Gateway of Acumos
 */
@Component
@EnableScheduling
@Configuration
public class PeerCommunicationTaskScheduler implements 	ApplicationContextAware {

	private final EELFLoggerDelegate logger = EELFLoggerDelegate.getLogger(PeerCommunicationTaskScheduler.class);

	@Autowired
	private Environment env;

	@Autowired
	private PeerService peerService;

	@Autowired
	private PeerSubscriptionService peerSubscriptionService;

	private ApplicationContext			appCtx;

	private static Table<String, Long, PeerTaskHandler> peersSubsTask = HashBasedTable.create();

	public void setApplicationContext(ApplicationContext theCtx) { //throws BeansException {
    this.appCtx = theCtx;
	}

	//what was this for?
	//@Bean(destroyMethod = "shutdown")
	//public Executor taskExecutor() {
	//	return Executors.newScheduledThreadPool(10);//Hardcode for now
	//}

	@Bean
	public ThreadPoolTaskScheduler taskScheduler() {
		String name = env.getProperty("federation.instance.name") + "-" + env.getProperty("federation.instance") + "-taskscheduler";
		ThreadPoolTaskScheduler threadPoolTaskScheduler = null;
		try {
			threadPoolTaskScheduler = (ThreadPoolTaskScheduler)this.appCtx.getBean(name);
		}
		catch (BeansException bix) {
			//instantiation should fail the first time because we create teh bean below
		}

		if (threadPoolTaskScheduler == null) {
			logger.debug(EELFLoggerDelegate.debugLogger, "creating task scheduler");
			threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
			threadPoolTaskScheduler.setPoolSize(20);//Make it configurable later
			threadPoolTaskScheduler.setBeanName(name);
			threadPoolTaskScheduler.initialize();
		}
		return threadPoolTaskScheduler; 
	}

	@PreDestroy
	public void cleanUpTasks() {
		logger.debug(EELFLoggerDelegate.debugLogger, "cleanUpTasks");
		try {
			logger.debug(EELFLoggerDelegate.debugLogger, "cleanUpTasks: " + this.peersSubsTask.size() + " tasks");
			taskScheduler().shutdown();

		} catch (Exception e) {
			logger.error(EELFLoggerDelegate.errorLogger, "Exception occurred while cleanUpTasks: ", e);
		}
	}

	protected boolean same(MLPPeerSubscription theFirstSub, MLPPeerSubscription theSecondSub) {
		logger.debug(EELFLoggerDelegate.debugLogger, "comparing subs : [" + theFirstSub.getSubId() + "," + theFirstSub.getCreated() + "," + theFirstSub.getModified() + "] vs. [" + theSecondSub.getSubId() + "," + theSecondSub.getCreated() + "," + theSecondSub.getModified() + "]");
		return theFirstSub.getSubId() == theSecondSub.getSubId() &&
					 theFirstSub.getCreated().equals(theSecondSub.getCreated()) &&
					 ((theFirstSub.getModified() == null && theSecondSub.getModified() == null) ||
					  (theFirstSub.getModified() != null && theSecondSub.getModified() != null && theFirstSub.getModified().equals(theSecondSub.getModified()))
					 );
	}

	@Scheduled(initialDelay = 1000,fixedRateString = "${peer.jobchecker.interval:400}000")
	public void checkPeerJobs() {

		//Get the List of MLP Peers
		List<MLPPeer> mlpPeers = peerService.getPeers();
		if(Utils.isEmptyList(mlpPeers)) {
			logger.info(EELFLoggerDelegate.debugLogger, "checkPeer : no peers from " + peerService);
			return;
		}
	
		for(MLPPeer mlpPeer : mlpPeers){
			logger.info(EELFLoggerDelegate.debugLogger, "checkPeer : " + mlpPeer);

			//cancel peer tasks for inactive peers 
			if(!mlpPeer.isActive()) {
				//cancel all peer sub tasks for this peer
				logger.debug(EELFLoggerDelegate.debugLogger, "checkPeer : peer no longer active, removing active tasks");
				Map<Long, PeerTaskHandler> subsTask = this.peersSubsTask.row(mlpPeer.getPeerId());
				if (subsTask != null) {
					for (Map.Entry<Long, PeerTaskHandler> subTaskEntry: subsTask.entrySet()) {
						subTaskEntry.getValue().stopTask();
						this.peersSubsTask.remove(mlpPeer.getPeerId(), subTaskEntry.getKey());
					}
				}

				//or peers that have been removed
				//
				continue;
			}

			List<MLPPeerSubscription> mlpSubs = peerSubscriptionService.getPeerSubscriptions(mlpPeer.getPeerId());
			if(Utils.isEmptyList(mlpSubs)) {
				//the peer is still there but has no subscriptions: cancel any ongoing tasks

				continue;
			}

			for(MLPPeerSubscription mlpSub : mlpSubs) {
				logger.info(EELFLoggerDelegate.debugLogger, "checkSub " + mlpSub);
				PeerTaskHandler peerSubTask = peersSubsTask.get(mlpPeer.getPeerId(), mlpSub.getSubId());
				if (peerSubTask != null) {
					//was the subscription updated? if yes, cancel current task. 
					MLPPeerSubscription mlpCurrentSub = peerSubTask.getSubscription();
					if (!same(mlpSub, mlpCurrentSub)) {
						logger.debug(EELFLoggerDelegate.debugLogger, "checkSub: subscription was updated, stopping current task");
						peerSubTask.stopTask();
						peerSubTask = null; //in order to trigger its reset below: no need to remove the entry as we are about
																//to replace it with a new one.
					}
				}

				if (peerSubTask == null) {
					logger.info(EELFLoggerDelegate.debugLogger, "Scheduled peer sub task for " + mlpPeer.getApiUrl());
					this.peersSubsTask.put(mlpPeer.getPeerId(), mlpSub.getSubId(), new PeerTaskHandler().startTask(mlpPeer, mlpSub));
				}
			}
		}
	}

	/**
	 * We need this contraption simply because we cannot retrieve the task (in order to check the subscription it is
	 * working on) from the ScheduledFuture ..
	 */
	private class PeerTaskHandler {

		private ScheduledFuture 			future;
		private PeerCommunicationTask	task;

		public synchronized PeerTaskHandler startTask(MLPPeer thePeer, MLPPeerSubscription theSub) {
			this.task =	(PeerCommunicationTask)PeerCommunicationTaskScheduler.this.appCtx.getBean("peerSubscriptionTask");
			this.future = PeerCommunicationTaskScheduler.this.taskScheduler().scheduleAtFixedRate(
																			this.task.handle(thePeer, theSub), theSub.getRefreshInterval());
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

	//TODO Make it dynamic to add jobs and track them
	/*@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.setTaskScheduler(taskScheduler());
		taskRegistrar.addTriggerTask( new Runnable() {
                    @Override public void run() {
                    	System.out.println("blah");
                        System.out.println(System.currentTimeMillis());
                    }
                },
                new Trigger() {
                    @Override public Date nextExecutionTime(TriggerContext triggerContext) {
                        Calendar nextExecutionTime =  new GregorianCalendar();
                        Date lastActualExecutionTime = triggerContext.lastActualExecutionTime();
                        nextExecutionTime.setTime(lastActualExecutionTime != null ? lastActualExecutionTime : new Date());
                        nextExecutionTime.add(Calendar.MILLISECOND,1000); //you can get the value from wherever you want
                        return nextExecutionTime.getTime();
                    }
                }
        );

	}*/
}

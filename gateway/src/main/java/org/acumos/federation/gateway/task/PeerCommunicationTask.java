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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.acumos.federation.gateway.common.JsonResponse;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.service.impl.Clients;
import org.acumos.federation.gateway.service.impl.FederationClient;
import org.acumos.federation.gateway.util.Utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import org.acumos.cds.AccessTypeCode;
import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPPeerSubscription;
import org.acumos.cds.domain.MLPSolution;

import org.acumos.federation.gateway.event.PeerSubscriptionEvent;

import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;

/**
 * 
 * Peer Acumos Task to Communicate to Remote Acumos and fetch Solutions & Catalogs
 */
@Component("peerSubscriptionTask")
@Scope("prototype")
public class PeerCommunicationTask implements Runnable {

	private final EELFLoggerDelegate logger = EELFLoggerDelegate.getLogger(PeerCommunicationTask.class);
	

	@Autowired
	private ApplicationEventPublisher eventPublisher;
	
	private MLPPeer mlpPeer;
	private MLPPeerSubscription mlpSubscription;

  @Autowired
  private Clients clients;

  public PeerCommunicationTask() {
	}	

	public PeerCommunicationTask handle(MLPPeer peer, MLPPeerSubscription subscription) {
		this.mlpPeer = peer;
		this.mlpSubscription = subscription;
		return this;
  }

	public MLPPeer getPeer() {
		return this.mlpPeer;
	}

	public MLPPeerSubscription getSubscription() {
		return this.mlpSubscription;
	}


	@Override
	public void run() {
    	
		if (this.mlpPeer == null || this.mlpSubscription == null) {
			logger.info(EELFLoggerDelegate.debugLogger, "Peer task has no peer subscription info");
			return;
		}

		try {
			logger.info(EELFLoggerDelegate.debugLogger, "Peer task: " + mlpPeer);
    		
        			
      logger.info(EELFLoggerDelegate.debugLogger, "Peer task: invoking getSolutions from Remote instance " + mlpPeer.getApiUrl());
      FederationClient fedClient =
          clients.getFederationClient(this.mlpPeer.getApiUrl());      

      //Map<String, Object> queryParameters = new HashMap<String, Object>();
      //queryParameters.put("modelTypeCode", mlpSubscription.getSelector()); // Subscriptions
     	logger.info(EELFLoggerDelegate.debugLogger, "Peer Task: filter " + mlpSubscription.getSelector());
			
     	JsonResponse<List<MLPSolution>> jsonResponse = 
						fedClient.getSolutionsListFromPeer(
							Utils.jsonStringToMap(mlpSubscription.getSelector()));
			if(jsonResponse != null && jsonResponse.getResponseBody() != null) {
				List<MLPSolution> mlpSolutions = jsonResponse.getResponseBody();
        logger.debug(EELFLoggerDelegate.debugLogger, "Peer task: Number of Solutions fetch from Remote Instance: " + mlpSolutions.size());
				if (mlpSolutions.size() > 0) {
					this.eventPublisher.publishEvent(
								new PeerSubscriptionEvent(this, this.mlpPeer, this.mlpSubscription, mlpSolutions));
        }
			}
    }
		catch (Exception x) {
			// TODO: handle exception
			logger.info(EELFLoggerDelegate.errorLogger, "Peer task for " + this.mlpPeer + " failed", x);
		}
    //	System.out.println(mlpPeer.getName() + " : Runnable Task with " + message + " on thread " + Thread.currentThread().getName() + ", id:"+ Thread.currentThread().getId());
   }
}

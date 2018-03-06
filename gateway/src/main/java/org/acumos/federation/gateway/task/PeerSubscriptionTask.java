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

import java.util.Date;
import java.util.List;

import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPPeerSubscription;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.federation.gateway.common.JsonResponse;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.event.PeerSubscriptionEvent;
import org.acumos.federation.gateway.common.Clients;
import org.acumos.federation.gateway.common.FederationClient;
import org.acumos.federation.gateway.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * 
 * Peer Acumos Task to Communicate to Remote Acumos and fetch Solutions and
 * Catalogs.
 * This is a Component/Bean so that it can be autowired.
 */
@Component
@Scope("prototype")
public class PeerSubscriptionTask implements Runnable {

	private final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(getClass().getName());

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	private MLPPeer	peer;
	private MLPPeerSubscription subscription;

	@Autowired
	private Clients clients;

	public PeerSubscriptionTask() {
	}

	public PeerSubscriptionTask handle(MLPPeer peer, MLPPeerSubscription subscription) {
		this.peer = peer;
		this.subscription = subscription;
		return this;
	}

	public MLPPeer getPeer() {
		return this.peer;
	}

	public MLPPeerSubscription getSubscription() {
		return this.subscription;
	}

	@Override
	public void run() {

		if (this.peer == null || this.subscription == null) {
			log.info(EELFLoggerDelegate.debugLogger, "Peer task has no peer subscription info");
			return;
		}

		try {
			log.info(EELFLoggerDelegate.debugLogger, "Peer task for peer {}, subscription {}", this.peer.getName(), this.subscription.getSubId());
			FederationClient fedClient = clients.getFederationClient(this.peer.getApiUrl());
			JsonResponse<List<MLPSolution>> response =
				fedClient.getSolutions(Utils.jsonStringToMap(this.subscription.getSelector()));
			log.info(EELFLoggerDelegate.debugLogger, "Peer task for peer {}, subscription {} got response {}", this.peer.getName(), this.subscription.getSubId(), response);
			if (response != null && response.getContent() != null) {
				List<MLPSolution> solutions = response.getContent();
				if (solutions.size() > 0) {
					this.eventPublisher.publishEvent(
							new PeerSubscriptionEvent(this, this.peer, this.subscription, solutions));
				}
			}

			this.subscription.setProcessed(new Date());
			this.clients.getCDSClient().updatePeerSubscription(this.subscription);
		}
		catch (Exception x) {
			log.error(EELFLoggerDelegate.errorLogger, "Peer task failed for " + peer.getName() + ", " + peer.getApiUrl() + ", " + subscription.getSelector(), x);
		}
	}
}

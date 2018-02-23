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

	private MLPPeer mlpPeer;
	private MLPPeerSubscription mlpSubscription;

	@Autowired
	private Clients clients;

	public PeerSubscriptionTask() {
	}

	public PeerSubscriptionTask handle(MLPPeer peer, MLPPeerSubscription subscription) {
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
			log.info(EELFLoggerDelegate.debugLogger, "Peer task has no peer subscription info");
			return;
		}

		try {
			log.info(EELFLoggerDelegate.debugLogger, "Peer task for " + mlpPeer.getName() + ", " + mlpPeer.getApiUrl() + ", " + mlpSubscription.getSelector());
			FederationClient fedClient = clients.getFederationClient(this.mlpPeer.getApiUrl());
			JsonResponse<List<MLPSolution>> response =
				fedClient.getSolutions(Utils.jsonStringToMap(mlpSubscription.getSelector()));
			log.debug(EELFLoggerDelegate.debugLogger,
						"Peer task got response " + response + " for " + mlpPeer.getName() + ", " + mlpPeer.getApiUrl() + ", " + mlpSubscription.getSelector());
			if (response != null && response.getContent() != null) {
				List<MLPSolution> mlpSolutions = response.getContent();
				if (mlpSolutions.size() > 0) {
					this.eventPublisher.publishEvent(
							new PeerSubscriptionEvent(this, this.mlpPeer, this.mlpSubscription, mlpSolutions));
				}
			}
		}
		catch (Exception x) {
			log.error(EELFLoggerDelegate.errorLogger, "Peer task failed for " + mlpPeer.getName() + ", " + mlpPeer.getApiUrl() + ", " + mlpSubscription.getSelector(), x);
		}
	}
}

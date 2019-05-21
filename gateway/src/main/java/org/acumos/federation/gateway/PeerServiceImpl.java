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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;

import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPPeerSubscription;

import org.acumos.federation.client.FederationClient;

/**
 * Service bean for implementing the peer service using CDS.
 */
public class PeerServiceImpl implements PeerService {
	@Autowired
	private Clients clients;

	@Override
	public List<MLPPeer> getPeers() {
		return Application.cdsAll(pr -> clients.getCDSClient().getPeers(pr));
	}

	@Override
	public MLPPeer getPeer(String peerId) {
		return clients.getCDSClient().getPeer(peerId);
	}

	@Override
	public MLPPeer getPeerBySubject(String subject) {
		List<MLPPeer> candidates = Application.cdsAll(pr -> clients.getCDSClient().searchPeers(Collections.singletonMap("subjectName", subject), false, pr));
		if (candidates.isEmpty()) {
			return null;
		}
		return candidates.get(0);
	}

	@Override
	public MLPPeer getSelf(String subject) {
		HashMap<String, Object> filter = new HashMap<>();
		filter.put("subjectName", subject);
		filter.put("self", true);
		List<MLPPeer> candidates = Application.cdsAll(pr -> clients.getCDSClient().searchPeers(filter, false, pr));
		if (candidates.isEmpty()) {
			return null;
		}
		return candidates.get(0);
	}

	@Override
	public List<MLPPeerSubscription> getSubscriptions(String peerId) {
		return clients.getCDSClient().getPeerSubscriptions(peerId);
	}

	@Override
	public MLPPeerSubscription getSubscription(long subscriptionId) {
		return clients.getCDSClient().getPeerSubscription(subscriptionId);
	}

	@Override
	public void updateSubscription(MLPPeerSubscription sub) {
		clients.getCDSClient().updatePeerSubscription(sub);
	}

	@Override
	public void register() {
		if (Security.getCurrentPeerId() != null) {
			throw new BadRequestException(HttpServletResponse.SC_CONFLICT, "Already registered as " + Security.getCurrentPeerId());
		}
		clients.getCDSClient().createPeer(Security.getCertificatePeer());
	}

	@Override
	public void unregister() {
		MLPPeer peer = clients.getCDSClient().getPeer(Security.getCurrentPeerId());
		peer.setStatusCode(FederationClient.PSC_RENOUNCED);
		clients.getCDSClient().updatePeer(peer);
	}
}

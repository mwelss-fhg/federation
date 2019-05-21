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

import java.util.List;

import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPPeerSubscription;

/**
 * API for accessing information about peers and related items.
 *
 * Provides a mechanism for looking up all or specific peers, for
 * requesting and cancelling peer registration, and for accessing
 * subscriptions to peers.
 */
public interface PeerService {
	/**
	 * List all the peers.
	 *
	 * @return The list of peers.
	 */
	public List<MLPPeer> getPeers();
	/**
	 * Get a peer by peer ID.
	 *
	 * @param peerId The ID of the peer.
	 * @return The peer.
	 */
	public MLPPeer getPeer(String peerId);
	/**
	 * Get a peer by subject.
	 *
	 * @param subject The common name from the subject of the peer's X.509 certificate.
	 * @return The peer.
	 */
	public MLPPeer getPeerBySubject(String subject);
	/**
	 * Get the self peer by subject.
	 *
	 * @param subject The common name from the subject of the gateway's X.509 certificate.
	 * @return The peer.
	 */
	public MLPPeer getSelf(String subject);
	/**
	 * Get subscriptions to a peer.
	 *
	 * @param peerId The ID of the peer.
	 * @return The subscriptions to the peer's catalogs.
	 */
	public List<MLPPeerSubscription> getSubscriptions(String peerId);
	/**
	 * Get a subscription to a peer.
	 *
	 * @param subId The ID of the subscription.
	 * @return The subscription.
	 */
	public MLPPeerSubscription getSubscription(long subId);
	/**
	 * Update a subscription to a peer.
	 *
	 * @param sub The subscription.
	 */
	public void updateSubscription(MLPPeerSubscription sub);
	/**
	 * Request registration for the peer.
	 */
	public void register();
	/**
	 * Cancel registration for the peer.
	 */
	public void unregister();
}

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

package org.acumos.federation.gateway.service;

import java.util.List;

import org.acumos.cds.domain.MLPPeerSubscription;

/**
 * 
 *
 */
public interface PeerSubscriptionService {

	/**
	 * @param peerId
	 *            Peer ID
	 * @return List of PeerSubscription configured in the Local Acumos Instance
	 */
	List<MLPPeerSubscription> getPeerSubscriptions(String peerId);

	/**
	 * @param subId
	 *            Peer subscription ID
	 * @return Peer Subscription based on the configured Subject Name
	 */
	MLPPeerSubscription getPeerSubscription(Long subId);

	/**
	 * @param mlpPeerSubscription
	 *            MLPPeer Configuration that needs to be updated on the Platform
	 */
	void updatePeerSubscription(MLPPeerSubscription mlpPeerSubscription) throws ServiceException;

}

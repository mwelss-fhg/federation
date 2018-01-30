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

import org.acumos.cds.domain.MLPPeer;

/**
 * Defines the interface of a service providing local peer information.
 *
 */
public interface PeerService {

	/**
	 * Strictly an internal service call.
	 * Needs to avoid the 'chicken,egg' problem: one needs a context to access
	 * peers, including the 'self' peer.
	 */
	public MLPPeer getSelf();

	/**
	 * Provide the list of locally registered peers to one of our peers
	 * It is the responsability of the implementation to decide which peer
	 * information to expose in each case
	 */
	public List<MLPPeer> getPeers(ServiceContext theContext);
	
	public default List<MLPPeer> getPeers() {
		return getPeers(ServiceContext.selfService());
	}
	
	/**
	 * @return Peer based on the configured Subject Name
	 */
	public List<MLPPeer> getPeerBySubjectName(
													String theSubjectName, ServiceContext theContext);
	
	public default List<MLPPeer> getPeerBySubjectName(String theSubjectName) {
		return getPeerBySubjectName(theSubjectName, ServiceContext.selfService());
	}
	
	/** */
	public MLPPeer getPeerById(String thePeerId, ServiceContext theContext);
	
	public default MLPPeer getPeerById(String thePeerId) {
		return getPeerById(thePeerId, ServiceContext.selfService());
	}
	
	/**
	 * Optional operation allowing the gateway to provision a peer in some
	 * initial state as part of a in-band peer handshake mechanism.
	 * The whole handshake procedure is to be completed elsewhere (portal);
	 * We do not pass a context as this operation is performed with
	 * frespect to 'self' (to reconsider).
	 *
	 * @param mlpPeer MLPPeer New Peer info to be submitted to the platform
	 * 
	 * @return Peer configuration that has been created.
	 */
	public void subscribePeer(MLPPeer mlpPeer) throws ServiceException;
	
	/**
	 * Optional operation allowing the gateway to update a peer and mark it for
	 * removal as part of a in-band peer handshake mechanism.
	 *
	 * @param mlpPeer MLPPeer New Peer info to be submitted to the platform
	 * 
	 * @return Peer configuration that has been created.
	 */
	public void unsubscribePeer(MLPPeer mlpPeer) throws ServiceException;
	
	
}

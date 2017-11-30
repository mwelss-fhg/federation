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
 * 
 *
 */
public interface PeerAcumosConfigService {

	/**
	 * @return List of Peers configured in the Local Acumos Instance
	 */
	List<MLPPeer> getPeers();
	
	/**
	 * @return Peer based on the configured Subject Name
	 */
	List<MLPPeer> getPeer(String subjectName);
	
	MLPPeer getOnePeer(String peerId);
	
	/**
	 * @param mlpPeer MLPPeer Configuration that needs to be created on the Platform
	 * 
	 * @return Peer configuration that has been created.
	 */
	MLPPeer savePeer(MLPPeer mlpPeer);
	
	
	/**
	 * @param mlpPeer MLPPeer Configuration that needs to be updated on the Platform
	 * 
	 * @return Peer configuration that has been updated.
	 */
	boolean updatePeer(MLPPeer mlpPeer);
	
	/**
	 * @param mlpPeer MLPPeer Configuration that needs to be deleted on the Platform
	 * 
	 * @return true if successfully deleted else false.
	 */
	boolean deletePeer(MLPPeer mlpPeer);
}

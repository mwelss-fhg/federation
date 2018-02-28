/* 
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
package org.acumos.federation.gateway.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import org.springframework.beans.factory.annotation.Autowired;

import org.acumos.federation.gateway.service.PeerService;
import org.acumos.federation.gateway.cds.PeerStatus;

import org.acumos.cds.domain.MLPPeer;

/**
 * Peers constitute the users of the federation gateway.
 */
public class Peer extends User {

	private MLPPeer peerInfo;

	public Peer(MLPPeer thePeerInfo, Role theRole) {
		this(thePeerInfo, theRole.priviledges());
	}

	public Peer(MLPPeer thePeerInfo, Collection<? extends GrantedAuthority> theAuthorities) {
		super(thePeerInfo.getName(), "", true, true, true, true, theAuthorities);
		this.peerInfo = thePeerInfo;
	}

	public MLPPeer getPeerInfo() {
		return this.peerInfo;
	}

//	@Override
//	public boolean isEnabled() {
//		if (this.peerInfo == null)
//			return false;

//		PeerStatus peerStatus =  PeerStatus.forCode(this.peerInfo.getStatusCode());
//		return peerStatus == PeerStatus.Active;
//	}

	private static PeerService peerService = null;

	@Autowired
	public void setPeerService(PeerService thePeerService) {
		if (peerService != null)
			throw new IllegalStateException("Already set");

		peerService = thePeerService;
	}

	private static Peer self = null;

	public static Peer self() {
		if (self == null) {
			if (peerService == null)
				throw new IllegalStateException("Initialization not completed");
			self = new Peer(peerService.getSelf(), Role.SELF.priviledges());
		}

		return self;
	}

}

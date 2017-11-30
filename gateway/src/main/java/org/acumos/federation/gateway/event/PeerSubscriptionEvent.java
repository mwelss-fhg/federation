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

package org.acumos.federation.gateway.event;

import java.util.List;
import java.util.EventObject;

import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPPeerSubscription;
import org.acumos.cds.domain.MLPSolution;

/**
 * Carries event information related to a peer subscription check
 */
public class PeerSubscriptionEvent extends EventObject {

	private MLPPeer							peer;
	private MLPPeerSubscription	subscription;
	private List<MLPSolution>		solutions;


	public PeerSubscriptionEvent(Object theSource,
															 MLPPeer thePeer,
															 MLPPeerSubscription theSubscription,
															 List<MLPSolution> theSolutions) {
		super(theSource);
		this.peer = thePeer;
		this.subscription = theSubscription;
		this.solutions = theSolutions;
	}

	public MLPPeer getPeer() {
		return this.peer;
	}

	public MLPPeerSubscription getSubscription() {
		return this.subscription;
	}

	public List<MLPSolution> getSolutions() {
		return this.solutions;
	}

}

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
package org.acumos.federation.gateway.cds;

import java.util.EnumSet;

import org.acumos.cds.PeerStatusCode;

/**
 * Supplements the CDS representation of a peer status information.
 * Not needed but I did not notice the equivalent class in CDS .. To be removed once the set of
 * values in CDS is updated.
 */
public enum PeerStatus {

	Active(PeerStatusCode.AC.name()),
	Inactive(PeerStatusCode.IN.name()),
	Requested(PeerStatusCode.RQ.name()),/* the 'peer' has made a request to be registered with this Acumos instance (pending active)  */
	Renounced(PeerStatusCode.RN.name()),  /* the peer has made a request to be unregistered as a peer of this Acumos instance (pending remove) */
	Declined(PeerStatusCode.DC.name()) /* (removed) */
	;

	private String 				code;
	//private MLPPeerStatus	mlp;

	private PeerStatus(String theCode) {
		this.code = theCode;
		//mlp = new MLPPeerStatus(theCode, name());
	}

	public String code() {
		return this.code;
	}

	//public MLPPeerStatus mlp() {
	//	return this.mlp;
	//}

	public static PeerStatus forCode(final String theCode) {
		return EnumSet.allOf(PeerStatus.class)
						.stream()
						.filter(status -> status.code().equals(theCode))
						.findFirst()
						.orElse(null);
	}
}



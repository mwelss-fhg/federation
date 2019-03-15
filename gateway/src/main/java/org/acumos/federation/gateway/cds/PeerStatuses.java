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

import java.util.List;

/**
 */
public class PeerStatuses extends CodeNames<PeerStatus> {

	public static final PeerStatus Active = forCode("AC");
	public static final PeerStatus Inactive = forCode("IN");
	public static final PeerStatus Requested = forCode("RQ");
	public static final PeerStatus Renounced = forCode("RN");
	public static final PeerStatus Declined = forCode("DC");
	public static final PeerStatus Unknown = forCode("UK");

	public PeerStatuses() {
	}

	public static PeerStatus forCode(String theCode) {
		return CodeNames.forCode(theCode, PeerStatus.class);
	}

	public static List<PeerStatus> codes() {
		return codes(PeerStatus.class);
	} 

}



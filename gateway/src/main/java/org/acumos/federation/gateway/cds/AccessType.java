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

/**
 * Supplements the CDS representation of a solution revision access type information.
 */
public enum AccessType {

	Organization("OR"), //
	Public("PB"), //
	Private("PR"), //
	Restricted("RS")//
	;

	private String 							code;
	//private MLPAccessTypeCode	mlp;

	private AccessType(String theCode) {
		this.code = theCode;
	}

	public String code() {
		return this.code;
	}

	//public MLPAccessType mlp() {
	//	return this.mlp;
	//}

	public static AccessType forCode(final String theCode) {
		return EnumSet.allOf(AccessType.class)
						.stream()
						.filter(status -> status.code().equals(theCode))
						.findFirst()
						.orElse(null);
	}
}



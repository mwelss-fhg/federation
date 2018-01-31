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

import org.springframework.security.core.GrantedAuthority;

/**
 * An enumeratoin of federated access fine grained proviledges
 */
public enum Priviledge implements GrantedAuthority {

	/**
	 * Gives access to catalog items (solutions); coarse at this point, all
	 * (list/read/download) or nothing
	 */
	CATALOG_ACCESS,
	/**
	 * Gives access to the local list of peers. In the future we might want to
	 * refine this by defining which peers should be provided (byb some base
	 * selection criteria)
	 */
	PEERS_ACCESS,
	/**
	 * The right to submit a subscription request. This is granted to ANY if so
	 * enabled system wide.
	 */
	SUBSCRIPTION;

	Priviledge() {
	}

	@Override
	public String getAuthority() {
		return name();
	}
}

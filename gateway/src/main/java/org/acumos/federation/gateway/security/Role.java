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
import java.util.Collections;
import java.util.Arrays;

import org.springframework.security.core.GrantedAuthority;

/**
 * Each Role states a predefined set of available federation priviledges.
 */
public enum Role {

	/**
	 * Un-authenticated client. Will at most be granted access to subscribe
	 * functionality
	 */
	ANY(Collections.EMPTY_LIST),
	/**
	 * Common peer, grants generic solution catalog access
	 */
	PEER(Collections.unmodifiableList(Arrays.asList(Priviledge.CATALOG_ACCESS))),
	/**
	 * Enhanced peer, gains (some lovel of) read access to the local peer list
	 */
	PARTNER(Collections.unmodifiableList(Arrays.asList(Priviledge.CATALOG_ACCESS, Priviledge.PEERS_ACCESS))),
	/**
	 * The Acumos instance this gateway is serving, including local calls and calls
	 * received through the gateways' private interface from other components,
	 * grants all priviledges
	 */
	SELF(Collections.unmodifiableList(Arrays.asList(Priviledge.class.getEnumConstants())));

	private Collection<Priviledge> priviledges;

	Role(Collection<Priviledge> thePriviledges) {
		this.priviledges = thePriviledges;
	}

	public Collection<Priviledge> priviledges() {
		return this.priviledges;
	}

}

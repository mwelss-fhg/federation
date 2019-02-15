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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

/**
 */
public class FederationMethodSecurityExpressionRoot 
  																extends SecurityExpressionRoot 
																	implements MethodSecurityExpressionOperations {

	private final Logger log = LoggerFactory.getLogger(getClass().getName());

	private Object filterObject;
	private Object returnObject;
	public  boolean	isActive = false;
	public  boolean	isKnown = false;
 
	public FederationMethodSecurityExpressionRoot(Authentication theAuthentication) {
		super(theAuthentication);
		log.info("built with {}", theAuthentication);
		this.isKnown = ((Peer) this.getPrincipal()).isKnown();
		this.isActive = ((Peer) this.getPrincipal()).isActive();	
	}

	@Override
	public Object getFilterObject() {
		return this.filterObject;
	}

	@Override
	public Object getReturnObject() {
		return this.returnObject;
	}

	@Override
	public Object getThis() {
		return this;
	}

	@Override
	public void setFilterObject(Object obj) {
		this.filterObject = obj;
	}

	@Override
	public void setReturnObject(Object obj) {
		this.returnObject = obj;
	}

}

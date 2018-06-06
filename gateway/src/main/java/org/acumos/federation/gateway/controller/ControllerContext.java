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

package org.acumos.federation.gateway.controller;

import java.util.Map;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.security.Peer;

/**
 * The security context is thread local so we do the same for attributes.
 * There is a risk when attribute are not cleared and processing threads are pooled.
 */
public class ControllerContext implements ServiceContext {

	private ThreadLocal<Map<String, Object>> attributes =
		new ThreadLocal<Map<String, Object>>() {
			public Map<String, Object> initialValue() {
				return new HashMap<String, Object>();
			}
		};																				

	public Peer getPeer() {
		return (Peer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	}
	
	public ServiceContext withAttribute(String theName, Object theValue) {
		attributes.get().put(theName, theValue);
		return this;
	}

	public Object getAttribute(String theName) {
		return attributes.get().get(theName);
	}
	
}

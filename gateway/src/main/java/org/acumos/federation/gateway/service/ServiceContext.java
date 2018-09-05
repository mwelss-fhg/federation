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

import java.util.HashMap;
import java.util.Map;

import org.acumos.federation.gateway.security.Peer;

/**
 * Expose the invocation frame for service calls (whenever a call is selectively
 * provided).
 */
public interface ServiceContext {

	/* Retrieve a context attribute.
	 */
	public Object getAttribute(String theName);

	/* Attach a context attribute
	 */
	public void setAttribute(String theName, Object theValue);

	/* Fluent version of setAttribute
	 */
	public ServiceContext withAttribute(String theName, Object theValue);

	/*
	 * In who's behalf are we providing the service.
	 */
	public Peer getPeer();

	/*
	 * Is the service to be provided for the benefit of the local Acumos system?
	 */
	public default boolean isSelf() {
		return getPeer().getPeerInfo().isSelf();
	}

	/*
	 */
	//public static ServiceContext selfService() {
	//	return forPeer(/*how to get a reference to the self peer in here ??*/);
	//}

	/*
	 */
	public static ServiceContext forPeer(final Peer thePeer) {
		return new ServiceContext() {

			private Map<String, Object> attributes = new HashMap<String, Object>();
			private Peer								peer;

			{
				peer = thePeer;
			}

			@Override
			public Peer getPeer() { return peer; }

			@Override
			public Object getAttribute(String theName) {
				return attributes.get(theName);
			}

			@Override
			public void setAttribute(String theName, Object theValue) {
				attributes.put(theName, theValue);
			}

			@Override
			public ServiceContext withAttribute(String theName, Object theValue) {
				setAttribute(theName, theValue);
				return this;
			}

		};
	}
}

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

package org.acumos.federation.gateway.common;

import java.net.URI;

/**
 * Common root for errors occuring during federation interactions (with the a peer)
 */
public class FederationException extends Exception {

	public FederationException(URI theUri, Throwable theCause) {
		this(theUri.toString(), theCause);
	}

	public FederationException(String theUri, Throwable theCause) {
		super(theUri + " failed", theCause);
	}

	public FederationException(URI theUri) {
		this(theUri.toString());
	}

	public FederationException(String theUri) {
		super(theUri + " failed");
	}
}

/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
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
package org.acumos.federation.client.config;

import lombok.Data;

/**
 * Configuration for a client.
 *
 * Currently, the configuration properties are all related to security and
 * authentication.  Which, if any, of the properties are required depends on
 * the particular security mechanisms used by the server the client
 * is intended to use.  The currently supported mechanisms are login/password
 * via HTTP(S) Basic Authentication, and HTTPS with and without client
 * certificates.
 */
@Data
public class ClientConfig {
	/**
	 * TLS (https) configuration for the client.
	 *
	 * @param ssl TLS configuration for the client.
	 * @return TLS configuration for the client.
	 */
	private TlsConfig ssl;
	/**
	 * Basic authentication credentials for the client.
	 *
	 * @param creds Credentials for the client.
	 * @return Credentials for the client.
	 */
	private BasicAuthConfig creds;
}

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
package org.acumos.federation.gateway;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.acumos.federation.client.config.ClientConfig;
import org.springframework.boot.web.server.Ssl.ClientAuth;

/**
 * Configuration bean for the internal and external servers in the Acumos Federation Gateway.
 */
@Data
@EqualsAndHashCode(callSuper=true)
public class ServerConfig extends ClientConfig {
	/**
	 * Configuration bean for the server port number
	 */
	@Data
	static public class Server {
		/**
		 * The TCP/IP port to listen on.
		 *
		 * @param port The port.
		 * @return The port.
		 */
		private int port;
	}
	/**
	 * The IP address or host name to listen on.
	 *
	 * @param address The IP address or host name.
	 * @return The IP address or host name.
	 */
	private String address;
	/**
	 * The TCP/IP port to listen on.
	 *
	 * @param server The Server specifying the port.
	 * @return The Server specifying the port.
	 */
	private Server server;
	/**
	 * Client certificate request mode.
	 *
	 * @param clientAuth The mode.
	 * @return The mode.
	 */
	ClientAuth clientAuth = ClientAuth.WANT;
}

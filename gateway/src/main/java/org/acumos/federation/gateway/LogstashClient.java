/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2020 Nordix Foundation
 * ===================================================================================
 * This Acumos software file is distributed by Nordix Foundation
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

import org.acumos.federation.client.ClientBase;
import org.acumos.federation.client.config.ClientConfig;
import org.springframework.web.client.RestClientException;
import org.acumos.federation.client.data.ModelData;


/**
 * Client for accessing the Logstash service configured with a http input plugin.
 * https://www.elastic.co/guide/en/logstash/current/plugins-inputs-http.html
 */
public class LogstashClient extends ClientBase {

	/**
	 * Create a logstash client
	 *
	 * @param url URL for accessing the Logstash service.
	 * @param cc  Credentials and TLS parameters for mutual authentication.
	 */
	public LogstashClient(String url, ClientConfig cc) {
		super(url, cc, null, null);
	}

	public void saveModelData(ModelData modelData) throws RestClientException {
		// We are ignoring resulting string for now but in order to have valid
		// message converter using String.
		restTemplate.postForObject("/", modelData, String.class);
	}

}

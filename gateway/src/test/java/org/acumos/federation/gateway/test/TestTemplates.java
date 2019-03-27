/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property & Tech
 * 						Mahindra. All rights reserved.
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
package org.acumos.federation.gateway.test;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.acumos.federation.gateway.config.InterfaceConfigurationBuilder;
import org.acumos.federation.gateway.config.InterfaceConfigurationBuilder.SSLBuilder;

public class TestTemplates	{
	public final static TestRestTemplate SELF = build("acumosa");
	public final static TestRestTemplate PEER = build("acumosb");
	public final static TestRestTemplate UNREGISTERED = build("acumosc");

	public static TestRestTemplate build(String id) {
		TestRestTemplate ret = new TestRestTemplate();
		((HttpComponentsClientHttpRequestFactory)ret.getRestTemplate().getRequestFactory()).setHttpClient(new InterfaceConfigurationBuilder()
		    .withSSL(new SSLBuilder()
			.withKeyStore("classpath:/" + id + ".pkcs12")
			.withKeyStorePassword(id)
			.withTrustStore("classpath:/acumosTrustStore.jks")
			.withTrustStoreType("JKS")
			.withTrustStorePassword("acumos")
			.build())
		    .buildConfig()
		    .buildClient());
		return ret;
	}
}

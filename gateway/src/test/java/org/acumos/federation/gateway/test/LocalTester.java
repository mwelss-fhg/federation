/*-
 * ===============LICENSE_START=======================================================
 * Acumos Apache-2.0
 * ===================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
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

import java.util.List;

import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPSolution;
/* this is not good for unit testing .. */
import org.acumos.federation.gateway.common.JsonResponse;
import org.acumos.federation.gateway.config.InterfaceConfigurationBuilder;
import org.acumos.federation.gateway.config.InterfaceConfigurationBuilder.SSLBuilder;
import org.apache.http.client.HttpClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;



/**
 * Standalone tester of the local interface
 */
public class LocalTester {

	public static void main(String[] theArgs) throws Exception {

		RestTemplate template = new RestTemplate(
															new HttpComponentsClientHttpRequestFactory(
																prepareHttpClient(theArgs[0])));

		ResponseEntity<JsonResponse<MLPPeer>> pingResponse =
			template.exchange(theArgs[1] + "/ping", HttpMethod.GET, prepareRequest(), new ParameterizedTypeReference<JsonResponse<MLPPeer>>() {});
		
		if (pingResponse != null)	{
			System.out.println("testPeerPing: " + pingResponse);
		}


		ResponseEntity<JsonResponse<List<MLPSolution>>> solutionsResponse =
			template.exchange(theArgs[1] + "/solutions", HttpMethod.GET, prepareRequest(), new ParameterizedTypeReference<JsonResponse<List<MLPSolution>>>() {});
		
		if (solutionsResponse != null)	{
			System.out.println("testPeerSolutions: " + solutionsResponse);
		}

	}

	private static HttpEntity prepareRequest() {
		HttpHeaders headers = new HttpHeaders();
 		headers.setContentType(MediaType.APPLICATION_JSON);
 		return new HttpEntity<String>(headers);
	}

	private static HttpClient prepareHttpClient(String theTestSystem) {
		return new InterfaceConfigurationBuilder()
								.withSSL(new SSLBuilder()
															.withKeyStore("classpath:/acumos" + theTestSystem + ".pkcs12")
															.withKeyStorePassword("acumos" + theTestSystem)
															//.withKeyPassword("acumosb")
															.withTrustStore("classpath:/acumosTrustStore.jks")
															.withTrustStoreType("JKS")
															.withTrustStorePassword("acumos")
															.build())
								.buildConfig()
								.buildClient();
	}
}

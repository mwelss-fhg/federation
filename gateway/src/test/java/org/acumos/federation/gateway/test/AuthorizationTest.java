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
package org.acumos.federation.gateway.test;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.federation.gateway.common.JsonResponse;
import org.acumos.federation.gateway.config.InterfaceConfigurationBuilder;
import org.acumos.federation.gateway.config.InterfaceConfigurationBuilder.SSLBuilder;
import org.apache.http.client.HttpClient;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
/* this is not good for unit testing .. */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringRunner;



/**
 */

//@RunWith(SpringJUnit4ClassRunner.class)
@RunWith(SpringRunner.class)
@ContextHierarchy({
	@ContextConfiguration(classes = org.acumos.federation.gateway.test.TestAdapterConfiguration.class),
	@ContextConfiguration(classes = org.acumos.federation.gateway.config.FederationConfiguration.class)
})
@SpringBootTest(classes = org.acumos.federation.gateway.Application.class,
								webEnvironment = WebEnvironment.RANDOM_PORT,
								properties = {
									"spring.main.allow-bean-definition-overriding=true",
									"federation.instance=adapter",
									"federation.instance.name=test",
									"federation.operator=admin",
									"federation.registration.enabled=true",
									"codes-local.source=classpath:test-codes.json",
									"peers-local.source=classpath:test-peers.json",
									"catalog-local.source=classpath:test-catalog.json",
									"federation.ssl.key-store=classpath:acumosa.pkcs12",
									"federation.ssl.key-store-password=acumosa",
									"federation.ssl.key-store-type=PKCS12",
									"federation.ssl.key-password = acumosa",
									"federation.ssl.trust-store=classpath:acumosTrustStore.jks",
									"federation.ssl.trust-store-password=acumos",
									"federation.ssl.client-auth=need"
								})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AuthorizationTest {

	private final Logger log = LoggerFactory.getLogger(getClass().getName());
	@Autowired
	private TestRestTemplate restTemplate;
	@Value("${local.server.port}")
	int port;

	@Test
	public void testUnknownPeerSolutionsAccess() {

    ((HttpComponentsClientHttpRequestFactory)
			this.restTemplate.getRestTemplate().getRequestFactory())
				.setHttpClient(prepareUnknownHttpClient());

		ResponseEntity<JsonResponse<List<MLPSolution>>> response =
			this.restTemplate.exchange("https://localhost:" + this.port + "/solutions", HttpMethod.GET, prepareRequest(), new ParameterizedTypeReference<JsonResponse<List<MLPSolution>>>() {});

		if (response != null)	{
			log.info("test unknown peer access: {}", response.getBody());
			log.info("test unknown peer access: {}", response);
		}
		
		assertTrue(response != null);
		assertTrue("Expected status code 401, got " + response.getStatusCodeValue(), response.getStatusCodeValue() == 401);
	}

	@Test
	public void testKnownPeerSolutionsAccess() {

    ((HttpComponentsClientHttpRequestFactory)
			this.restTemplate.getRestTemplate().getRequestFactory())
				.setHttpClient(prepareKnownHttpClient());

		ResponseEntity<JsonResponse<List<MLPSolution>>> response =
			this.restTemplate.exchange("https://localhost:" + this.port + "/solutions", HttpMethod.GET, prepareRequest(), new ParameterizedTypeReference<JsonResponse<List<MLPSolution>>>() {});
		
		if (response != null)	{
			log.info("test known peer access: {}", response.getBody());
			log.info("test known peer access: {}", response);
		}
		
		assertTrue(response != null);
		assertTrue("Expected status code 200, got " + response.getStatusCodeValue(), response.getStatusCodeValue() == 200);
		assertTrue(response.getBody().getContent().size() == 1);
	
	}

	@Test
	public void testUnknownRegisterAccess() {

    ((HttpComponentsClientHttpRequestFactory)
			this.restTemplate.getRestTemplate().getRequestFactory())
				.setHttpClient(prepareUnknownHttpClient());
		
		ResponseEntity<JsonResponse<MLPPeer>> response =
			this.restTemplate.exchange("https://localhost:" + this.port + "/peer/register", HttpMethod.POST, prepareRequest(), new ParameterizedTypeReference<JsonResponse<MLPPeer>>() {});
		
		if (response != null)	{
			log.info("test unknown peer access to register: {}", response.getBody());
			log.info("test unknown peer access to register: {}", response);
		}
		
		assertTrue(response != null);
		assertTrue("Expected status code 202, got " + response.getStatusCodeValue(), response.getStatusCodeValue() == 202);
	}


	private HttpEntity prepareRequest(String theResourceName) {
		String content = new Scanner(
    									   Thread.currentThread().getContextClassLoader().getResourceAsStream(theResourceName), "UTF-8")
											.useDelimiter("\\Z").next();

		HttpHeaders headers = new HttpHeaders();
 		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
 		headers.setContentType(MediaType.APPLICATION_JSON);
 		return new HttpEntity<String>(content, headers);
	}
	
	private HttpEntity prepareRequest() {
		HttpHeaders headers = new HttpHeaders();
 		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
 		headers.setContentType(MediaType.APPLICATION_JSON);
 		return new HttpEntity<String>(headers);
	}

	private HttpClient prepareKnownHttpClient() {
		return new InterfaceConfigurationBuilder()
								.withSSL(new SSLBuilder()
															.withKeyStore("classpath:/acumosb.pkcs12")
															.withKeyStorePassword("acumosb")
															//.withKeyPassword("acumosb")
															.withTrustStore("classpath:/acumosTrustStore.jks")
															.withTrustStoreType("JKS")
															.withTrustStorePassword("acumos")
															.build())
								.buildConfig()
								.buildClient();
	}

	private HttpClient prepareUnknownHttpClient() {
		return new InterfaceConfigurationBuilder()
								.withSSL(new SSLBuilder()
															.withKeyStore("classpath:/acumosc.pkcs12")
															.withKeyStorePassword("acumosc")
															//.withKeyPassword("acumosb")
															.withTrustStore("classpath:/acumosTrustStore.jks")
															.withTrustStoreType("JKS")
															.withTrustStorePassword("acumos")
															.build())
								.buildConfig()
								.buildClient();
	}
}


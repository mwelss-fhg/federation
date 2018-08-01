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

import java.util.List;
import java.util.Scanner;

import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.federation.gateway.common.JsonResponse;
/* this is not good for unit testing .. */
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.config.InterfaceConfigurationBuilder;
import org.acumos.federation.gateway.config.InterfaceConfigurationBuilder.SSLBuilder;
import org.apache.http.client.HttpClient;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
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
									"federation.instance=adapter",
									"federation.instance.name=test",
									"federation.operator=admin",
									"peersLocal.source=classpath:test-peers.json",
									"catalogLocal.source=classpath:test-catalog.json",
									"federation.ssl.key-store=classpath:acumosa.pkcs12",
									"federation.ssl.key-store-password=acumosa",
									"federation.ssl.key-store-type=PKCS12",
									"federation.ssl.key-password = acumosa",
									"federation.ssl.trust-store=classpath:acumosTrustStore.jks",
									"federation.ssl.trust-store-password=acumos",
									"federation.ssl.client-auth=need",
									"federation.registration.enabled=true"
								})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ControllerTest {

	private final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(getClass().getName());
	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void testSolutions() {

    ((HttpComponentsClientHttpRequestFactory)
			this.restTemplate.getRestTemplate().getRequestFactory())
				.setHttpClient(prepareHttpClient());
		
		ResponseEntity<JsonResponse<List<MLPSolution>>> response =
			this.restTemplate.exchange("/solutions", HttpMethod.GET, prepareRequest(), new ParameterizedTypeReference<JsonResponse<List<MLPSolution>>>() {});
		
		if (response != null)	{
			log.info(EELFLoggerDelegate.debugLogger, "testSolutions: {}", response.getBody());
			log.info(EELFLoggerDelegate.debugLogger, "testSolutions: {}", response);
		}
		
		assertTrue(response != null);
		assertTrue(response.getStatusCodeValue() == 200);
		assertTrue(response.getBody().getContent().size() == 1);
	}


	@Test
	public void testSolutionSuccess() {

    ((HttpComponentsClientHttpRequestFactory)
			this.restTemplate.getRestTemplate().getRequestFactory())
				.setHttpClient(prepareHttpClient());

		ResponseEntity<JsonResponse<MLPSolution>> response =
			this.restTemplate.exchange("/solutions/00000000-0000-0000-0000-000000000000", HttpMethod.GET, prepareRequest(), new ParameterizedTypeReference<JsonResponse<MLPSolution>>() {} );
	
		if (response != null)	{
			log.info(EELFLoggerDelegate.debugLogger, "testSolution: {}", response.getBody());
			log.info(EELFLoggerDelegate.debugLogger, "testSolution: {}", response);
		}

		assertTrue(response != null);
		assertTrue(response.getStatusCodeValue() == 200);
		assertTrue(response.getBody().getContent().getModelTypeCode().equals("CL")); //no errors
	}

	@Test
	public void testSolutionRevisionsSuccess() {
    
		((HttpComponentsClientHttpRequestFactory)
			this.restTemplate.getRestTemplate().getRequestFactory())
				.setHttpClient(prepareHttpClient());

		ResponseEntity<JsonResponse<List<MLPSolutionRevision>>> response =
			this.restTemplate.exchange("/solutions/00000000-0000-0000-0000-000000000000/revisions", HttpMethod.GET, prepareRequest(), new ParameterizedTypeReference<JsonResponse<List<MLPSolutionRevision>>>() {});
		
		if (response != null)	{
			log.info(EELFLoggerDelegate.debugLogger, "testSolutionRevisions: {}", response.getBody());
			log.info(EELFLoggerDelegate.debugLogger, "testSolutionRevisions: {}", response);
		}

		assertTrue(response != null);
		assertTrue(response.getStatusCodeValue() == 200);
		assertTrue(response.getBody().getContent().size() == 1); //no errors
	}

	@Test
	public void testSolutionRevisionSuccess() {

    ((HttpComponentsClientHttpRequestFactory)
			this.restTemplate.getRestTemplate().getRequestFactory())
				.setHttpClient(prepareHttpClient());

		ResponseEntity<JsonResponse<MLPSolution>> response =
			this.restTemplate.exchange("/solutions/00000000-0000-0000-0000-000000000000/revisions/01010101-0101-0101-0101-010101010101", HttpMethod.GET, prepareRequest(), new ParameterizedTypeReference<JsonResponse<MLPSolution>>() {} );
	
		if (response != null)	{
			log.info(EELFLoggerDelegate.debugLogger, "testSolutionRevision: {}", response.getBody());
			log.info(EELFLoggerDelegate.debugLogger, "testSolutionRevision: {}", response);
		}

		assertTrue(response != null);
		assertTrue(response.getStatusCodeValue() == 200);
		assertTrue(response.getBody().getContent().getUserId().equals("admin")); //no errors
	}

	@Test
	public void testSolutionRevisionArtifactsSuccess() {
    
		((HttpComponentsClientHttpRequestFactory)
			this.restTemplate.getRestTemplate().getRequestFactory())
				.setHttpClient(prepareHttpClient());

		ResponseEntity<JsonResponse<List<MLPArtifact>>> response =
			this.restTemplate.exchange("/solutions/00000000-0000-0000-0000-000000000000/revisions/01010101-0101-0101-0101-010101010101/artifacts", HttpMethod.GET, prepareRequest(), new ParameterizedTypeReference<JsonResponse<List<MLPArtifact>>>() {});
		
		if (response != null)	{
			log.info(EELFLoggerDelegate.debugLogger, "testSolutionRevisionArtifacts: {}", response.getBody());
			log.info(EELFLoggerDelegate.debugLogger, "testSolutionRevisionArtifacts: {}", response);
		}

		assertTrue(response != null);
		assertTrue(response.getStatusCodeValue() == 200);
		assertTrue(response.getBody().getContent().size() == 1); //no errors
	}
	
	@Test
	public void testRegister() {

    ((HttpComponentsClientHttpRequestFactory)
			this.restTemplate.getRestTemplate().getRequestFactory())
				.setHttpClient(prepareHttpClient("acumosc"));

		ResponseEntity<JsonResponse<MLPPeer>> response =
			this.restTemplate.exchange("/peer/register", HttpMethod.POST, prepareRequest(), new ParameterizedTypeReference<JsonResponse<MLPPeer>>() {} );
		if (response != null)	{
			log.info(EELFLoggerDelegate.debugLogger, "testRegister: {}", response.getBody());
			log.info(EELFLoggerDelegate.debugLogger, "testRegister: {}", response);
		}
	
		assertTrue(response != null);
		assertTrue("Expected 202 status code, got " + response.getStatusCodeValue(), response.getStatusCodeValue() == 202);

		//an attempt to re-register should trigger an error
		response =
			this.restTemplate.exchange("/peer/register", HttpMethod.POST, prepareRequest(), new ParameterizedTypeReference<JsonResponse<MLPPeer>>() {} );
		if (response != null)	{
			log.info(EELFLoggerDelegate.debugLogger, "test(re)Register: {}", response.getBody());
			log.info(EELFLoggerDelegate.debugLogger, "test(re)Register: {}", response);
		}
	
		assertTrue(response != null);
		assertTrue("Expected 400 status code, got " + response.getStatusCodeValue(), response.getStatusCodeValue() == 400);
	}

	@Test
	public void testUnregister() {

    ((HttpComponentsClientHttpRequestFactory)
			this.restTemplate.getRestTemplate().getRequestFactory())
				.setHttpClient(prepareHttpClient("acumosb"));

		ResponseEntity<JsonResponse<MLPPeer>> response =
			this.restTemplate.exchange("/peer/unregister", HttpMethod.POST, prepareRequest(), new ParameterizedTypeReference<JsonResponse<MLPPeer>>() {} );
		if (response != null)	{
			log.info(EELFLoggerDelegate.debugLogger, "testUnregister: {}", response.getBody());
			log.info(EELFLoggerDelegate.debugLogger, "testUnregister: {}", response);
		}
	
		assertTrue(response != null);
		assertTrue("Expected 202 status code, got " + response.getStatusCodeValue(), response.getStatusCodeValue() == 202);
	}

	@Test
	public void testUnregisterNonExistent() {

    ((HttpComponentsClientHttpRequestFactory)
			this.restTemplate.getRestTemplate().getRequestFactory())
				.setHttpClient(prepareHttpClient("acumosc"));

		ResponseEntity<JsonResponse<MLPPeer>> response =
			this.restTemplate.exchange("/peer/unregister", HttpMethod.POST, prepareRequest(), new ParameterizedTypeReference<JsonResponse<MLPPeer>>() {} );
		if (response != null)	{
			log.info(EELFLoggerDelegate.debugLogger, "testUnregisterNonExistent: {}", response.getBody());
			log.info(EELFLoggerDelegate.debugLogger, "testUnregisterNonExistent: {}", response);
		}
	
		assertTrue(response != null);
		assertTrue("Expected 400 status code, got " + response.getStatusCodeValue(), response.getStatusCodeValue() == 400);
	}


	@Test
	public void testPeersForbidden() {

    ((HttpComponentsClientHttpRequestFactory)
			this.restTemplate.getRestTemplate().getRequestFactory())
				.setHttpClient(prepareHttpClient());

		ResponseEntity<JsonResponse<List<MLPPeer>>> response =
			this.restTemplate.exchange("/peers", HttpMethod.GET, prepareRequest(), new ParameterizedTypeReference<JsonResponse<List<MLPPeer>>>() {} );
	
		if (response != null)	{
			log.info(EELFLoggerDelegate.debugLogger, "testPeers: {}", response.getBody());
			log.info(EELFLoggerDelegate.debugLogger, "testPeers: {}", response);
			System.out.println("testPeers: " + response.getBody());
			System.out.println("testPeers: " + response);
		}

		assertTrue(response.getStatusCodeValue() == 401);
	}

	private HttpEntity prepareRequest(String theResourceName) {
		String content = new Scanner(
    									   Thread.currentThread().getContextClassLoader().getResourceAsStream(theResourceName), "UTF-8")
											.useDelimiter("\\Z").next();

		HttpHeaders headers = new HttpHeaders();
 		headers.setContentType(MediaType.APPLICATION_JSON);
 		return new HttpEntity<String>(content, headers);
	}
	
	private HttpEntity prepareRequest() {
		HttpHeaders headers = new HttpHeaders();
 		headers.setContentType(MediaType.APPLICATION_JSON);
 		return new HttpEntity<String>(headers);
	}

	private HttpClient prepareHttpClient() {
		return prepareHttpClient("acumosb");
	}

	private HttpClient prepareHttpClient(String theIdentity) {
		return new InterfaceConfigurationBuilder()
								.withSSL(new SSLBuilder()
															.withKeyStore("classpath:/" + theIdentity + ".pkcs12")
															.withKeyStorePassword(theIdentity)
															//.withKeyPassword("acumosb")
															.withTrustStore("classpath:/acumosTrustStore.jks")
															.withTrustStoreType("JKS")
															.withTrustStorePassword("acumos")
															.build())
								.buildConfig()
								.buildClient();
	}
}


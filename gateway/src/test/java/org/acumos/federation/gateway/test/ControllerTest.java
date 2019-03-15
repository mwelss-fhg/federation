/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2017 - 2019 AT&T Intellectual Property & Tech
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPDocument;
import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.federation.gateway.common.JsonResponse;
import org.acumos.federation.gateway.config.InterfaceConfigurationBuilder;
import org.acumos.federation.gateway.config.InterfaceConfigurationBuilder.SSLBuilder;
import org.apache.http.client.HttpClient;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
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
									"codes-local.source=classpath:test-codes.json",
									"peers-local.source=classpath:test-peers.json",
									"catalog-local.source=classpath:test-catalog.json",
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

	private final Logger log = LoggerFactory.getLogger(getClass().getName());
	private static final String SELFID = "acumosa";
	private static final String PEERID = "acumosb";
	private static final String UNREGISTEREDID = "acumosc";

	@Autowired
	private TestRestTemplate restTemplate;

	@Value("${local.server.port}")
	private int port;

	public void setClient(String id) {
		((HttpComponentsClientHttpRequestFactory)this.restTemplate.getRestTemplate().getRequestFactory()).setHttpClient(prepareHttpClient(id));
	}

	private <T> void assertGoodResponse(String testname, ResponseEntity<T> response) {
		if (response != null)   {
			log.info("{}: {}", testname, response.getBody());
			log.info("{}: {}", testname, response);
		}
		assertNotNull(response);
		assertEquals(200, response.getStatusCodeValue());
	}

	private <T> void assertGoodResponseWith(String testname, ResponseEntity<JsonResponse<T>> response, java.util.function.Predicate<T> fcn) {
		assertGoodResponse(testname, response);
		assertTrue(fcn.test(response.getBody().getContent()));
	}

	@Test
	public void testPeers() {
		setClient(SELFID);

		ResponseEntity<JsonResponse<List<MLPPeer>>> response =
			this.restTemplate.exchange("https://localhost:" + this.port + "/peers", HttpMethod.GET, prepareRequest(), new ParameterizedTypeReference<JsonResponse<List<MLPPeer>>>() {});
		
		assertGoodResponse("testPeers", response);
	}

	@Test
	public void testPing() {
		setClient(PEERID);

		ResponseEntity<JsonResponse<MLPPeer>> response =
			this.restTemplate.exchange("https://localhost:" + this.port + "/ping", HttpMethod.GET, prepareRequest(), new ParameterizedTypeReference<JsonResponse<MLPPeer>>() {});
		
		assertGoodResponse("testPing", response);
	}

	@Test
	public void testSolutions() {
		setClient(PEERID);
		
		ResponseEntity<JsonResponse<List<MLPSolution>>> response =
			this.restTemplate.exchange("https://localhost:" + this.port + "/solutions", HttpMethod.GET, prepareRequest(), new ParameterizedTypeReference<JsonResponse<List<MLPSolution>>>() {});
		
		assertGoodResponseWith("testSolutions", response, content -> content.size() == 1);
	}


	@Test
	public void testSolutionSuccess() {
		setClient(PEERID);

		ResponseEntity<JsonResponse<MLPSolution>> response =
			this.restTemplate.exchange("https://localhost:" + this.port + "/solutions/00000000-0000-0000-0000-000000000000", HttpMethod.GET, prepareRequest(), new ParameterizedTypeReference<JsonResponse<MLPSolution>>() {} );
	
		assertGoodResponseWith("testSolutionSuccess", response, content -> content.getModelTypeCode().equals("CL"));
	}

	@Test
	public void testSolutionRevisionsSuccess() {
		setClient(PEERID);

		ResponseEntity<JsonResponse<List<MLPSolutionRevision>>> response =
			this.restTemplate.exchange("https://localhost:" + this.port + "/solutions/00000000-0000-0000-0000-000000000000/revisions", HttpMethod.GET, prepareRequest(), new ParameterizedTypeReference<JsonResponse<List<MLPSolutionRevision>>>() {});
		
		assertGoodResponseWith("testSolutionRevisionSuccess", response, content ->content.size() == 1);
	}

	@Test
	public void testSolutionRevisionSuccess() {
		setClient(PEERID);

		ResponseEntity<JsonResponse<MLPSolution>> response =
			this.restTemplate.exchange("https://localhost:" + this.port + "/solutions/00000000-0000-0000-0000-000000000000/revisions/01010101-0101-0101-0101-010101010101", HttpMethod.GET, prepareRequest(), new ParameterizedTypeReference<JsonResponse<MLPSolution>>() {} );
	
		assertGoodResponseWith("testSolutionRevision", response, content -> content.getUserId().equals("admin"));
	}

	@Test
	public void testSolutionRevisionArtifactsSuccess() {
		setClient(PEERID);

		ResponseEntity<JsonResponse<List<MLPArtifact>>> response =
			this.restTemplate.exchange("https://localhost:" + this.port + "/solutions/00000000-0000-0000-0000-000000000000/revisions/01010101-0101-0101-0101-010101010101/artifacts", HttpMethod.GET, prepareRequest(), new ParameterizedTypeReference<JsonResponse<List<MLPArtifact>>>() {});
		
		assertGoodResponseWith("testSolutionRevisionArtifacts", response, content -> content.size() == 1);
	}

	@Test
	public void testSolutionRevisionArtifactContentSuccess() {
		setClient(PEERID);

		ResponseEntity<byte[]> response =
			this.restTemplate.exchange("https://localhost:" + this.port + "/solutions/00000000-0000-0000-0000-000000000000/revisions/01010101-0101-0101-0101-010101010101/artifacts/a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0/content", HttpMethod.GET, prepareBinaryRequest(), new ParameterizedTypeReference<byte[]>() {});
		
		assertGoodResponse("testSolutionRevisionArtifact", response);
	}

	@Test
	public void testSolutionRevisionDocumentsSuccess() {
		setClient(PEERID);

		ResponseEntity<JsonResponse<List<MLPDocument>>> response =
			this.restTemplate.exchange("https://localhost:" + this.port + "/solutions/00000000-0000-0000-0000-000000000000/revisions/01010101-0101-0101-0101-010101010101/documents", HttpMethod.GET, prepareRequest(), new ParameterizedTypeReference<JsonResponse<List<MLPDocument>>>() {});
		
		assertGoodResponseWith("testSolutionRevisionDocuments", response, content -> content.size() == 1);
	}

	@Test
	public void testSolutionRevisionDocumentContentSuccess() {
		setClient(PEERID);

		ResponseEntity<byte[]> response =
			this.restTemplate.exchange("https://localhost:" + this.port + "/solutions/00000000-0000-0000-0000-000000000000/revisions/01010101-0101-0101-0101-010101010101/documents/2c2c2c2c-6e6f-47d9-b7a4-c4e674d2b342/content", HttpMethod.GET, prepareBinaryRequest(), new ParameterizedTypeReference<byte[]>() {});
		
		assertGoodResponse("testSolutionRevisionDocumentContent", response);
	}
	
	@Test
	public void testRegister() {
		setClient(UNREGISTEREDID);

		ResponseEntity<JsonResponse<MLPPeer>> response =
			this.restTemplate.exchange("https://localhost:" + this.port + "/peer/register", HttpMethod.POST, prepareRequest(), new ParameterizedTypeReference<JsonResponse<MLPPeer>>() {} );
		if (response != null)	{
			log.info("testRegister: {}", response.getBody());
			log.info("testRegister: {}", response);
		}
	
		assertNotNull(response);
		assertEquals("Expected 202 status code, got " + response.getStatusCodeValue(), 202, response.getStatusCodeValue());

		//an attempt to re-register should trigger an error
		response =
			this.restTemplate.exchange("https://localhost:" + this.port + "/peer/register", HttpMethod.POST, prepareRequest(), new ParameterizedTypeReference<JsonResponse<MLPPeer>>() {} );
		if (response != null)	{
			log.info("test(re)Register: {}", response.getBody());
			log.info("test(re)Register: {}", response);
		}
	
		assertNotNull(response);
		assertEquals("Expected 400 status code, got " + response.getStatusCodeValue(), 400, response.getStatusCodeValue());
	}

	@Test
	public void testUnregister() {
		setClient(PEERID);

		ResponseEntity<JsonResponse<MLPPeer>> response =
			this.restTemplate.exchange("https://localhost:" + this.port + "/peer/unregister", HttpMethod.POST, prepareRequest(), new ParameterizedTypeReference<JsonResponse<MLPPeer>>() {} );
		if (response != null)	{
			log.info("testUnregister: {}", response.getBody());
			log.info("testUnregister: {}", response);
		}
	
		assertNotNull(response);
		assertEquals("Expected 202 status code, got " + response.getStatusCodeValue(), 202, response.getStatusCodeValue());
	}

	@Test
	public void testUnregisterNonExistent() {
		setClient("acumosc");

		ResponseEntity<JsonResponse<MLPPeer>> response =
			this.restTemplate.exchange("https://localhost:" + this.port + "/peer/unregister", HttpMethod.POST, prepareRequest(), new ParameterizedTypeReference<JsonResponse<MLPPeer>>() {} );
		if (response != null)	{
			log.info("testUnregisterNonExistent: {}", response.getBody());
			log.info("testUnregisterNonExistent: {}", response);
		}
	
		assertNotNull(response);
		assertEquals("Expected 401 status code, got " + response.getStatusCodeValue(), 401, response.getStatusCodeValue());
	}


	@Test
	public void testPeersForbidden() {
		setClient(PEERID);

		ResponseEntity<JsonResponse<List<MLPPeer>>> response =
			this.restTemplate.exchange("https://localhost:" + this.port + "/peers", HttpMethod.GET, prepareRequest(), new ParameterizedTypeReference<JsonResponse<List<MLPPeer>>>() {} );
	
		if (response != null)	{
			log.info("testPeers: {}", response.getBody());
			log.info("testPeers: {}", response);
		}

		assertEquals(401, response.getStatusCodeValue());
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
	
	private HttpEntity prepareBinaryRequest() {
		HttpHeaders headers = new HttpHeaders();
 		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM));
 		headers.setContentType(MediaType.APPLICATION_JSON);
 		return new HttpEntity<String>(headers);
	}

	private HttpClient prepareHttpClient(String theIdentity) {
		return new InterfaceConfigurationBuilder()
								.withSSL(new SSLBuilder()
															.withKeyStore("classpath:/" + theIdentity + ".pkcs12")
															.withKeyStorePassword(theIdentity)
															.withTrustStore("classpath:/acumosTrustStore.jks")
															.withTrustStoreType("JKS")
															.withTrustStorePassword("acumos")
															.build())
								.buildConfig()
								.buildClient();
	}
}


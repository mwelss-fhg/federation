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

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.federation.gateway.common.Clients;
import org.acumos.federation.gateway.common.FederationClient;
import org.acumos.federation.gateway.common.JsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.acumos.federation.gateway.config.InterfaceConfigurationBuilder;
import org.acumos.federation.gateway.config.InterfaceConfigurationBuilder.SSLBuilder;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;



/**
 * Contains tests of the local interface of a federation gateway
 */

//@RunWith(SpringJUnit4ClassRunner.class)
@RunWith(SpringRunner.class)
@ContextHierarchy({
	@ContextConfiguration(classes = org.acumos.federation.gateway.test.TestAdapterConfiguration.class),
	@ContextConfiguration(classes = org.acumos.federation.gateway.config.LocalConfiguration.class)
})
@SpringBootTest(classes = org.acumos.federation.gateway.Application.class,
								webEnvironment = WebEnvironment.DEFINED_PORT,
								properties = {
									"spring.main.allow-bean-definition-overriding=true",
									"federation.instance=adapter",
									"federation.instance.name=test",
									"federation.operator=admin",
									"codes-local.source=classpath:test-codes.json",
									"peers-local.source=classpath:test-peers.json",
									"catalog-local.source=classpath:test-catalog.json",
									"federation.server.port=${random.int[49152,65535]}",
									"federation.ssl.key-store=classpath:acumosa.pkcs12",
									"federation.ssl.key-store-password=acumosa",
									"federation.ssl.key-store-type=PKCS12",
									"federation.ssl.key-password = acumosa",
									"federation.ssl.trust-store=classpath:acumosTrustStore.jks",
									"federation.ssl.trust-store-password=acumos",
									"local.server.port=${random.int[49152,65535]}",
									"local.ssl.key-store=classpath:acumosa.pkcs12",
									"local.ssl.key-store-password=acumosa",
									"local.ssl.key-store-type=PKCS12",
									"local.ssl.key-password = acumosa",
									"local.ssl.trust-store=classpath:acumosTrustStore.jks",
									"local.ssl.trust-store-password=acumos"
								})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LocalControllerTest {

	private final Logger log = LoggerFactory.getLogger(getClass().getName());
//	@Autowired
//	private TestRestTemplate restTemplate;

	private RestTemplate	restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
	
	@Value("${local.server.port}")
	private int localPort;

	@MockBean(name = "federationClient")
	private HttpClient	federationClient;

	@MockBean(name = "clients")
	private Clients	clients;

	@Before
	public void initLocalTests() throws IOException {

		MockitoAnnotations.initMocks(this);

		BasicHttpResponse mockSolutionsResponse = 
			new BasicHttpResponse(
				new BasicStatusLine(
					new ProtocolVersion("HTTP",1,1), 200, "Success"));
		ClassPathResource mockSolutions =
			new ClassPathResource("mockPeerSolutionsResponse.json");
		mockSolutionsResponse.setEntity(
			new InputStreamEntity(mockSolutions.getInputStream()));
		mockSolutionsResponse
			.addHeader("Content-Type", ContentType.APPLICATION_JSON.toString());
		mockSolutionsResponse
			.addHeader("Content-Length", String.valueOf(mockSolutions.contentLength()));

		BasicHttpResponse mockSolutionResponse = 
			new BasicHttpResponse(
				new BasicStatusLine(
					new ProtocolVersion("HTTP",1,1), 200, "Success"));
		ClassPathResource mockSolution =
			new ClassPathResource("mockPeerSolutionResponse.json");
		mockSolutionResponse.setEntity(
			new InputStreamEntity(mockSolution.getInputStream()));
		mockSolutionResponse
			.addHeader("Content-Type", ContentType.APPLICATION_JSON.toString());
		mockSolutionResponse
			.addHeader("Content-Length", String.valueOf(mockSolution.contentLength()));

		BasicHttpResponse mockPingResponse = 
			new BasicHttpResponse(
				new BasicStatusLine(
					new ProtocolVersion("HTTP",1,1), 200, "Success"));
		ClassPathResource mockPing =
			new ClassPathResource("mockPeerPingResponse.json");
		mockPingResponse.setEntity(
			new InputStreamEntity(mockPing.getInputStream()));
		mockPingResponse
			.addHeader("Content-Type", ContentType.APPLICATION_JSON.toString());
		mockPingResponse
			.addHeader("Content-Length", String.valueOf(mockPing.contentLength()));

		BasicHttpResponse mockPeersResponse = 
			new BasicHttpResponse(
				new BasicStatusLine(
					new ProtocolVersion("HTTP",1,1), 200, "Success"));
		ClassPathResource mockPeers =
			new ClassPathResource("mockPeerPeersResponse.json");
		mockPeersResponse.setEntity(
			new InputStreamEntity(mockPeers.getInputStream()));
		mockPeersResponse
			.addHeader("Content-Type", ContentType.APPLICATION_JSON.toString());
		mockPeersResponse
			.addHeader("Content-Length", String.valueOf(mockPeers.contentLength()));

		BasicHttpResponse mockSubscriptionResponse = 
			new BasicHttpResponse(
				new BasicStatusLine(
					new ProtocolVersion("HTTP",1,1), 200, "Success"));
		ClassPathResource mockSubscription =
			new ClassPathResource("mockPeerSubscriptionResponse.json");
		mockSubscriptionResponse.setEntity(
			new InputStreamEntity(mockSubscription.getInputStream()));
		mockSubscriptionResponse
			.addHeader("Content-Type", ContentType.APPLICATION_JSON.toString());
		mockSubscriptionResponse
			.addHeader("Content-Length", String.valueOf(mockSubscription.contentLength()));

		when(
			this.federationClient.execute(
				any(HttpUriRequest.class), any(HttpContext.class)
			)
		).thenAnswer(new Answer<HttpResponse>() {
				public HttpResponse answer(InvocationOnMock theInvocation) {
					HttpUriRequest req = (HttpUriRequest)
						theInvocation.getArguments()[0];
					String path = req.getURI().getPath();
					log.warn("Mock path " + path);
					if (path.equals("/solutions"))
						return mockSolutionsResponse;
					if (path.endsWith("/ping"))
						return mockPingResponse;
					if (path.endsWith("/peers"))
						return mockPeersResponse;
					if (path.contains("/solutions/"))
						return mockSolutionResponse;
					if (path.contains("/subscription/"))
						return mockSubscriptionResponse;

					log.warn(" *** Mock unhandled path " + path);
					return null;
				}
			});

		when(
			this.clients.getFederationClient(
				any(String.class)
			)
		)
		.thenAnswer(new Answer<FederationClient>() {
			public FederationClient answer(InvocationOnMock theInvocation) {
				//this should end up providing a client based on the mocked http
				//client
				log.warn("Mock client for " + theInvocation.getArguments()[0]);
			  return new FederationClient(
                  (String)theInvocation.getArguments()[0]/*the URI*/,
                  federationClient);
			}
		});
	}

	@Test
	public void testPeerSolutions() {

    ((HttpComponentsClientHttpRequestFactory)
			this.restTemplate.getRequestFactory())
				.setHttpClient(prepareHttpClient());
		
		String url = "https://localhost:" + this.localPort + "/peer/11111111-1111-1111-1111-111111111111/solutions";

		log.info("testPeerSolutions: {}", url);
		ResponseEntity<JsonResponse<List<MLPSolution>>> response =
			this.restTemplate.exchange(url, HttpMethod.GET, prepareRequest(), new ParameterizedTypeReference<JsonResponse<List<MLPSolution>>>() {});
		
		if (response != null)	{
			log.info("testPeerSolutions: {}", response.getBody());
			log.info("testPeerSolutions: {}", response);
		}
		
		assertTrue(response != null);
		assertTrue(response.getStatusCodeValue() == 200);
		assertTrue(response.getBody().getContent().size() == 1);
	}


	@Test
	public void testPeerSolution() {

    ((HttpComponentsClientHttpRequestFactory)
			this.restTemplate.getRequestFactory())
				.setHttpClient(prepareHttpClient());

		String url = "https://localhost:" + this.localPort + "/peer/11111111-1111-1111-1111-111111111111/solutions/00000000-0000-0000-0000-000000000000";

		log.info("testPeerSolution: {}", url);
		ResponseEntity<JsonResponse<MLPSolution>> response =
			this.restTemplate.exchange(url, HttpMethod.GET, prepareRequest(), new ParameterizedTypeReference<JsonResponse<MLPSolution>>() {} );
	
		if (response != null)	{
			log.info("testSolution: {}", response.getBody());
			log.info("testSolution: {}", response);
		}

		assertTrue(response != null);
		assertTrue(response.getStatusCodeValue() == 200);
		assertTrue(response.getBody().getContent().getModelTypeCode().equals("CL")); //no errors
	}

	@Test
	public void testPeerPing() {
    
		((HttpComponentsClientHttpRequestFactory)
			this.restTemplate.getRequestFactory())
				.setHttpClient(prepareHttpClient());

		String url = "https://localhost:" + this.localPort + "/peer/11111111-1111-1111-1111-111111111111/ping";

		log.info("testPeerPing: {}", url);
		ResponseEntity<JsonResponse<MLPPeer>> response =
			this.restTemplate.exchange(url, HttpMethod.GET, prepareRequest(), new ParameterizedTypeReference<JsonResponse<MLPPeer>>() {});
		
		if (response != null)	{
			log.info("testPing: {}", response.getBody());
			log.info("testPing: {}", response);
		}

		assertTrue(response != null);
		assertTrue(response.getStatusCodeValue() == 200);
		assertTrue(response.getBody().getContent().getPeerId().equals("11111111-1111-1111-1111-111111111111")); //no errors
	}

	@Test
	public void testPeerPeers() {

    ((HttpComponentsClientHttpRequestFactory)
			this.restTemplate.getRequestFactory())
				.setHttpClient(prepareHttpClient());

		String url = "https://localhost:" + this.localPort + "/peer/11111111-1111-1111-1111-111111111111/peers";
		
		log.info("testPeerPeers: {}", url);
		ResponseEntity<JsonResponse<List<MLPPeer>>> response =
			this.restTemplate.exchange(url, HttpMethod.GET, prepareRequest(), new ParameterizedTypeReference<JsonResponse<List<MLPPeer>>>() {} );
	
		if (response != null)	{
			log.info("testPeerPeers: {}", response.getBody());
			log.info("testPeerPeers: {}", response);
		}

		assertTrue(response != null);
		assertTrue(response.getStatusCodeValue() == 200);
		assertTrue(response.getBody().getContent().get(0).getName().startsWith("acumos")); //no errors
	}

	@Test
	public void testPeerSubscription() {
    
		((HttpComponentsClientHttpRequestFactory)
			this.restTemplate.getRequestFactory())
				.setHttpClient(prepareHttpClient());

		String url = "https://localhost:" + this.localPort + "/peer/11111111-1111-1111-1111-111111111111/subscription/1";

		log.info("testPeerSubscription: {}", url);
		ResponseEntity<JsonResponse<String>> response =
			this.restTemplate.exchange(url, HttpMethod.POST, prepareRequest(), new ParameterizedTypeReference<JsonResponse<String>>() {});
		
		if (response != null)	{
			log.info("testPeerSubscription: {}", response.getBody());
			log.info("testPeerSubscription: {}", response);
		}

		assertTrue(response != null);
		assertTrue(response.getStatusCodeValue() == 200);
	}

	@Test
	public void testPeerNoSuch() {

    ((HttpComponentsClientHttpRequestFactory)
			this.restTemplate.getRequestFactory())
				.setHttpClient(prepareHttpClient());

		String url = "https://localhost:" + this.localPort + "/peer/11111111-1111-1111-1111-111111111112/ping";
	
		log.info("testPeerNoSuch: {}", url);
		try {
			this.restTemplate.exchange(url, HttpMethod.GET, prepareRequest(), new ParameterizedTypeReference<JsonResponse<MLPPeer>>() {} );
			assertTrue("expected to fail", true);
		}
		catch (HttpClientErrorException httpx) {
			assertTrue(httpx.getStatusCode().value() == 404);
		}
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
		return new InterfaceConfigurationBuilder()
								.withSSL(new SSLBuilder()
															.withKeyStore("classpath:/acumosa.pkcs12")
															.withKeyStorePassword("acumosa")
															//.withKeyPassword("acumosa")
															.withTrustStore("classpath:/acumosTrustStore.jks")
															.withTrustStoreType("JKS")
															.withTrustStorePassword("acumos")
															.build())
								.buildConfig()
								.buildClient();
	}
}


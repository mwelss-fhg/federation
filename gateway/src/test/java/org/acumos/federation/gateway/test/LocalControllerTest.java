/*-
 * ===============LICENSE_START=======================================================
 * Acumos Apache-2.0
 * ===================================================================================
 * Copyright (C) 2017-2019 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
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
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.HashMap;
import java.util.Scanner;

import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.federation.gateway.common.API;
import org.acumos.federation.gateway.common.Clients;
import org.acumos.federation.gateway.common.FederationClient;
import org.acumos.federation.gateway.common.FederationException;
import org.acumos.federation.gateway.common.JsonResponse;
import org.acumos.federation.gateway.common.JsonRequest;
import org.acumos.federation.gateway.common.PeerException;
import org.acumos.federation.gateway.config.InterfaceConfigurationBuilder;
import org.acumos.federation.gateway.config.InterfaceConfigurationBuilder.SSLBuilder;
import org.acumos.federation.gateway.config.InterfaceConfiguration;
import org.acumos.federation.gateway.service.PeerService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpStatusCodeException;
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

	private static class XFederationClient extends FederationClient {

		public XFederationClient(String theTarget, HttpClient theClient) {
			super(theTarget, theClient);
		}

		public Resource xdownload(URI theUri) throws FederationException {
			return download(theUri);
		}
		public void xsetTarget(String theTarget) {
			setTarget(theTarget);
		}
	}


	private RestTemplate	restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
	
	@Value("${local.server.port}")
	private int localPort;

	@MockBean(name = "federationClient")
	private HttpClient	federationClient;

	@MockBean(name = "clients")
	private Clients	clients;

	@Autowired
	private PeerService peerService;

	private MockAnswer peerAnswer = new MockAnswer();

	@Before
	public void initLocalTests() throws IOException {

		MockitoAnnotations.initMocks(this);

		peerAnswer
			.mockResponse(info -> info.getPath().equals("/solutions"), MockResponse.success("mockPeerSolutionsResponse.json"))
			.mockResponse(info -> info.getPath().contains("/solutions/") && !info.getPath().contains("/revisions"), MockResponse.success("mockPeerSolutionResponse.json"))
			.mockResponse(info -> info.getPath().endsWith("/revisions"), MockResponse.success("mockPeerSolutionRevisionsResponse.json"))
			.mockResponse(info -> info.getPath().endsWith("/artifacts"), MockResponse.success("mockPeerSolutionRevisionArtifactsResponse.json"))
			.mockResponse(info -> info.getPath().endsWith("/documents"), MockResponse.success("mockPeerSolutionRevisionDocumentsResponse.json"))
			.mockResponse(info -> info.getPath().endsWith("/download"), MockResponse.success("mockPeerDownload.tgz"))
			.mockResponse(info -> info.getPath().endsWith("/ping"), MockResponse.success("mockPeerPingResponse.json"))
			.mockResponse(info -> info.getPath().endsWith("/peer/register"), MockResponse.success("mockPeerRegisterResponse.json"))
			.mockResponse(info -> info.getPath().endsWith("/peers"), MockResponse.success("mockPeerPeersResponse.json"))
			.mockResponse(info -> info.getPath().contains("/subscription/"), MockResponse.success("mockPeerSubscriptionResponse.json"));

		when(
			this.federationClient.execute(
				any(HttpUriRequest.class), any(HttpContext.class)
			)
		).thenAnswer(peerAnswer);

		when(
			this.clients.getFederationClient(
				any(String.class)
			)
		)
		.thenAnswer(new Answer<FederationClient>() {
			public FederationClient answer(InvocationOnMock theInvocation) {
				//this should end up providing a client based on the mocked http
				//client
				log.warn("Mock client for {}", theInvocation.getArguments()[0]);
			  return new XFederationClient(
                  (String)theInvocation.getArguments()[0]/*the URI*/,
                  federationClient);
			}
		});
		((HttpComponentsClientHttpRequestFactory)this.restTemplate.getRequestFactory()).setHttpClient(prepareHttpClient());
	}

	private <T> void assertGoodResponse(String testname, ResponseEntity<JsonResponse<T>> response) {
		if (response != null)	{
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

	private <T> void verifyFail(String url, HttpMethod meth, ParameterizedTypeReference<T> rtype, int code) {
		try {
			log.info("testPeerNoSuch: {}", url);
			this.restTemplate.exchange(url, meth, prepareRequest(), rtype);
			fail("expected to fail");
		} catch (HttpStatusCodeException httpx) {
			assertEquals(code, httpx.getStatusCode().value());
		}
	}

	@Test
	public void testPeerSolutions() {
		String url = "https://localhost:" + this.localPort + "/peer/11111111-1111-1111-1111-111111111111/solutions";

		log.info("testPeerSolutions: {}", url);
		ResponseEntity<JsonResponse<List<MLPSolution>>> response =
			this.restTemplate.exchange(url, HttpMethod.GET, prepareRequest(), new ParameterizedTypeReference<JsonResponse<List<MLPSolution>>>() {});
		
		assertGoodResponseWith("testPeerSolutions", response, content -> content.size() == 1);
	}


	@Test
	public void testPeerSolution() {
		String url = "https://localhost:" + this.localPort + "/peer/11111111-1111-1111-1111-111111111111/solutions/00000000-0000-0000-0000-000000000000";

		log.info("testPeerSolution: {}", url);
		ResponseEntity<JsonResponse<MLPSolution>> response =
			this.restTemplate.exchange(url, HttpMethod.GET, prepareRequest(), new ParameterizedTypeReference<JsonResponse<MLPSolution>>() {} );
	
		assertGoodResponseWith("testPeerSolution", response, content -> content.getModelTypeCode().equals("CL"));
	}

	@Test
	public void testPeerPing() {
		String url = "https://localhost:" + this.localPort + "/peer/11111111-1111-1111-1111-111111111111/ping";

		log.info("testPeerPing: {}", url);
		ResponseEntity<JsonResponse<MLPPeer>> response =
			this.restTemplate.exchange(url, HttpMethod.GET, prepareRequest(), new ParameterizedTypeReference<JsonResponse<MLPPeer>>() {});
		
		assertGoodResponseWith("testPeerPing", response, content -> content.getPeerId().equals("11111111-1111-1111-1111-111111111111"));
	}

	@Test
	public void testPeerPeers() {
		String url = "https://localhost:" + this.localPort + "/peer/11111111-1111-1111-1111-111111111111/peers";
		
		log.info("testPeerPeers: {}", url);
		ResponseEntity<JsonResponse<List<MLPPeer>>> response =
			this.restTemplate.exchange(url, HttpMethod.GET, prepareRequest(), new ParameterizedTypeReference<JsonResponse<List<MLPPeer>>>() {} );
	
		assertGoodResponseWith("testPeerPeers", response, content -> content.get(0).getName().startsWith("acumos"));
	}

	@Test
	public void testPeerSubscription() {
		String url = "https://localhost:" + this.localPort + "/peer/11111111-1111-1111-1111-111111111111/subscription/1";

		log.info("testPeerSubscription: {}", url);
		ResponseEntity<JsonResponse<String>> response =
			this.restTemplate.exchange(url, HttpMethod.POST, prepareRequest(), new ParameterizedTypeReference<JsonResponse<String>>() {});
		
		assertGoodResponse("testPeerSubscription", response);
	}

	@Test
	public void testPeerRegistration() {
		String url = "https://localhost:" + this.localPort + "/peer/11111111-1111-1111-1111-111111111111/peer/register";
		log.info("testPeerRegistration: {}", url);
		ResponseEntity<JsonResponse<MLPPeer>> response =
			this.restTemplate.exchange(url, HttpMethod.POST, prepareRequest(), new ParameterizedTypeReference<JsonResponse<MLPPeer>>() {});

		assertGoodResponse("testPeerRegistration", response);
	}

	@Test
	public void testPeerNoSuch() {
		String badpeer = "https://localhost:" + this.localPort + "/peer/11111111-1111-1111-1111-111111111112";
	
		verifyFail(badpeer + "/ping", HttpMethod.GET, new ParameterizedTypeReference<JsonResponse<MLPPeer>>() {}, 404);
		verifyFail(badpeer + "/peers", HttpMethod.GET, new ParameterizedTypeReference<JsonResponse<List<MLPPeer>>>() {}, 500);
		verifyFail(badpeer + "/solutions?selector=e30K", HttpMethod.GET, new ParameterizedTypeReference<JsonResponse<List<MLPSolution>>>() {}, 500);
		verifyFail(badpeer + "/solutions/00000000-0000-0000-0000-000000000000", HttpMethod.GET, new ParameterizedTypeReference<JsonResponse<MLPSolution>>() {}, 500);
	}

	@Test
	public void testFederationClient() throws Exception {
		XFederationClient fedcli = (XFederationClient)clients.getFederationClient(peerService.getPeerById("11111111-1111-1111-1111-111111111111").getApiUrl());
		fedcli.getSolutionRevisions("00000000-0000-0000-0000-000000000000");
		fedcli.getArtifacts("00000000-0000-0000-0000-000000000000", "00000000-0000-0000-0000-000000000000");
		fedcli.getDocuments("00000000-0000-0000-0000-000000000000", "00000000-0000-0000-0000-000000000000");
		fedcli.xdownload(new URI("https://localhost:" + this.localPort + "/download"));
		try {
			fedcli.xsetTarget(null);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException iae) {
			// We want this
		}
		try {
			fedcli.xsetTarget("a b c");
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException iae) {
			// We want this
		}
	}

	@Test
	public void testAPI() {
		assertEquals("/solutions", API.SOLUTIONS.path());
		assertEquals("/solutions", API.SOLUTIONS.toString());
		assertEquals(1, API.SOLUTIONS.query().length);
		assertEquals(1, API.SOLUTIONS.queryParams(new HashMap()).size());
		API.SOLUTIONS.uriBuilder("https://localhost");
	}

	@Test
	public void testJsonRequest() {
		JsonRequest<String> jr = new JsonRequest<String>();
		jr.setRequestId("abc");
		jr.setBody("def");
		jr.setRequestFrom("ghi");
		assertEquals("abc", jr.getRequestId());
		assertEquals("def", jr.getBody());
		assertEquals("ghi", jr.getRequestFrom());
	}

	@Test
	public void testFederationException() throws Exception {
		new FederationException("A string");
		new FederationException(new URI("https://localhost"));
	}

	@Test
	public void testPeerException() throws Exception {
		HttpStatusCodeException hsce = new HttpClientErrorException(HttpStatus.BAD_REQUEST);

		new PeerException(new URI("https://localhost"), hsce);
		new PeerException("https://localhost", hsce);
	}

	@Test
	public void testInterfaceConfiguration() throws Exception {
		InterfaceConfiguration.Client cli = new InterfaceConfiguration.Client();
		cli.setUsername("username");
		cli.setPassword("password");
		cli = new InterfaceConfiguration.Client(cli.getUsername(), cli.getPassword());
		assertEquals("username", cli.getUsername());
		assertEquals("password", cli.getPassword());
		cli.toString();
		InterfaceConfiguration ic = new InterfaceConfiguration();
		ic.setAddress("localhost");
		assertEquals("localhost", ic.getAddress());
		ic.setClient(cli);
		assertEquals(cli, ic.getClient());
		assertTrue(ic.getSubjectName() == null);
		ic.setSSL(new SSLBuilder().build());
		assertEquals("need", ic.getSSL().getClientAuth());
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
		return InterfaceConfigurationBuilder.buildFrom(new InterfaceConfigurationBuilder()
								.withSSL(new SSLBuilder()
															.withKeyStoreType("JKS")
															.withKeyAlias(null)
															.withKeyStore("classpath:/acumosa.pkcs12")
															.withKeyStorePassword("acumosa")
															.withTrustStore("classpath:/acumosTrustStore.jks")
															.withTrustStoreType("JKS")
															.withTrustStorePassword("acumos")
															.build())
								.buildConfig())
								.buildConfig()
								.buildClient();
	}
}

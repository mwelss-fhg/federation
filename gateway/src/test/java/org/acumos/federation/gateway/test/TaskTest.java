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

import java.util.List;
import java.util.Scanner;
import java.util.Collections;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;
import static org.assertj.core.api.Assertions.assertThat; 

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Bean;

import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;

import static org.mockito.Mockito.*;
import static org.mockito.Matchers.*;
import org.mockito.stubbing.Answer;
import org.mockito.invocation.InvocationOnMock;

import org.apache.http.ProtocolVersion;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HttpContext;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;

/* this is not good for unit testing .. */
import org.acumos.federation.gateway.common.JsonResponse;
import org.acumos.federation.gateway.common.HttpClientConfigurationBuilder;
import org.acumos.federation.gateway.event.PeerSubscriptionEvent;
import static org.acumos.federation.gateway.common.HttpClientConfigurationBuilder.SSLBuilder;

import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.cds.domain.MLPArtifact;



/**
 */

//@RunWith(SpringJUnit4ClassRunner.class)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = org.acumos.federation.gateway.Application.class,
								webEnvironment = WebEnvironment.RANDOM_PORT,
								properties = {
									"federation.instance=adapter",
									"federation.instance.name=ghost",
									"peersLocal.source=classpath:/task-test-peers.json",
									"catalogLocal.source=classpath:/task-test-catalog.json",
									"server.ssl.key-store=classpath:acumosa.pkcs12",
									"server.ssl.key-store-password=acumosa",
									"server.ssl.key-store-type=PKCS12",
									"server.ssl.key-password = acumosa",
									"server.ssl.trust-store=classpath:acumosTrustStore.jks",
									"server.ssl.trust-store-password=acumos",
									"server.ssl.client-auth=need"
								})
@ContextConfiguration(classes = {TaskTest.TaskTestConfiguration.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TaskTest {


	@MockBean(name = "federationClient")
	private HttpClient	federationClient;

	@Autowired
	private ApplicationContext context;

	/**
	 * Called by the test framework and hanging until the event that
	 * we are expecting is coming.
	 * The test config sets up a short check interval.
	 */
	@Test
	public void testPeerTask() {

		PeerSubscriptionListener listener =
			(PeerSubscriptionListener)this.context.getBean("testListener");

		try {
			BasicHttpResponse mockResponse = 
				new BasicHttpResponse(
					new BasicStatusLine(
						new ProtocolVersion("HTTP",1,1), 200, "Success"));
/*
			String mockContent = "{" +
					"\"status\": true," +
 					"\"response_code\": 200," +
 					"\"response_detail\": \"Success\"," +
 					"\"response_body\": [{" +
					"\"solutionId\":\"6793411f-c7a1-4e93-85bc-f91d267541d8\"," +
					"\"name\":\"mock model\"," +
  				"\"description\":\"Test mock model\"," +
  				"\"ownerId\":\"admin\"," +
  				"\"active\":\"true\"," +
  				"\"modelTypeCode\":\"CL\"," +
  				"\"toolkitTypeCode\":\"\"," +
  				"\"validationStatusCode\":\"\"," +
  				"\"metadata\":\"acumosa\"," +
  				"\"created\":\"2017-08-10\"," +
  				"\"modified\":\"2017-08-11\"" +
  				"}]}";

			byte[] mockContentBytes = mockContent.getBytes("UTF-8");

			mockResponse.setEntity(
				new ByteArrayEntity(mockContentBytes));
			mockResponse.addHeader("Content-Type", ContentType.APPLICATION_JSON.toString());
			mockResponse.addHeader("Content-Length", String.valueOf(mockContentBytes.length));
*/
			ClassPathResource mockResource = new ClassPathResource("mockPeerSolutionsResponse.json");

			mockResponse.setEntity(
				new InputStreamEntity(mockResource.getInputStream()));
			mockResponse.addHeader("Content-Type", ContentType.APPLICATION_JSON.toString());
			mockResponse.addHeader("Content-Length", String.valueOf(mockResource.contentLength()));

			MLPSolution mockSolution = new MLPSolution();
			mockSolution.setSolutionId("1");
			mockSolution.setName("mock model");
			mockSolution.setDescription("mock model test");
			mockSolution.setActive(true);

			JsonResponse<List<MLPSolution>> mockPayload = new JsonResponse();
			mockPayload.setStatus(Boolean.TRUE);
			mockPayload.setResponseCode("200");
			mockPayload.setResponseDetail("Success");
			mockPayload.setResponseBody(Collections.singletonList(mockSolution));

			when(
				this.federationClient.execute(
					any(HttpHost.class), any(HttpRequest.class)
				)
			).thenReturn(mockResponse);

			when(
				this.federationClient.execute(
					any(HttpHost.class), any(HttpRequest.class), any(HttpContext.class)
				)
			).thenReturn(mockResponse);
			
			when(
				this.federationClient.execute(
					any(HttpUriRequest.class)
				)
			).thenReturn(mockResponse);

//this one gets called!
			when(
				this.federationClient.execute(
					any(HttpUriRequest.class), any(HttpContext.class)
				)
			//).thenReturn(mockResponse);
			).thenAnswer(new Answer<HttpResponse>() {
					public HttpResponse answer(InvocationOnMock theInvocation) {
						return mockResponse;
					}
				});

			when(
				this.federationClient.execute(
					any(HttpHost.class), any(HttpRequest.class), any(ResponseHandler.class)
				)
			).thenReturn(mockPayload);
						
			when(
				this.federationClient.execute(
					any(HttpHost.class), any(HttpRequest.class), any(ResponseHandler.class), any(HttpContext.class)
				)
			).thenReturn(mockPayload);
			
			when(
				this.federationClient.execute(
					any(HttpUriRequest.class), any(ResponseHandler.class)
				)
			).thenReturn(mockPayload);
						
			when(
				this.federationClient.execute(
					any(HttpUriRequest.class), any(ResponseHandler.class), any(HttpContext.class)
				)
			).thenReturn(mockPayload);

		}
		catch(Exception x) {
			System.out.println(" *** Failed to setup mock : " + x);
			x.printStackTrace();
			assertTrue(1 == 0);
		}

		try {
			assertTrue(listener.peerEventLatch.await(10, TimeUnit.SECONDS));
		}
		catch (InterruptedException ix) {
			assertTrue(1 == 0);
		}
		//
		assertTrue(listener.event != null);
		assertTrue(listener.event.getSolutions().size() == 1);
		assertTrue(listener.event.getSolutions().get(0).getName().equals("mock model"));
	}

	public static class TaskTestConfiguration {

		@Bean({"testListener"})
		public PeerSubscriptionListener testListener() {
			return new PeerSubscriptionListener();
		}
	}

	public static class PeerSubscriptionListener {
	
		CountDownLatch 				peerEventLatch = new CountDownLatch(1);
		PeerSubscriptionEvent event;

		@EventListener
		public void onApplicationEvent(PeerSubscriptionEvent theEvent) {
			this.event = theEvent;
			this.peerEventLatch.countDown();
		}
	}
}

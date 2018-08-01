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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.acumos.cds.domain.MLPSolution;
import org.acumos.federation.gateway.common.Clients;
import org.acumos.federation.gateway.common.FederationClient;
import org.acumos.federation.gateway.common.JsonResponse;
/* this is not good for unit testing .. */
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.event.PeerSubscriptionEvent;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HttpContext;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringRunner;



/**
 */

//@RunWith(SpringJUnit4ClassRunner.class)
@RunWith(SpringRunner.class)
@ContextHierarchy({
	@ContextConfiguration(classes = org.acumos.federation.gateway.test.TestAdapterConfiguration.class),
	@ContextConfiguration(classes = org.acumos.federation.gateway.config.FederationConfiguration.class),
	@ContextConfiguration(classes = TaskTest.TaskTestConfiguration.class)
})
@SpringBootTest(classes = org.acumos.federation.gateway.Application.class,
								webEnvironment = WebEnvironment.RANDOM_PORT,
								properties = {
									"federation.instance=adapter",
									"federation.instance.name=test",
									"federation.ssl.key-store=classpath:acumosa.pkcs12",
									"federation.ssl.key-store-password=acumosa",
									"federation.ssl.key-store-type=PKCS12",
									"federation.ssl.key-password = acumosa",
									"federation.ssl.trust-store=classpath:acumosTrustStore.jks",
									"federation.ssl.trust-store-password=acumos",
									"federation.ssl.client-auth=need",
									"peersLocal.source=classpath:/task-test-peers.json",
									"catalogLocal.source=classpath:/task-test-catalog.json"
								})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TaskTest {

	private final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(getClass().getName());
	@MockBean(name = "clients")
	private Clients	clients;
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
			mockPayload.setMessage("Success");
			mockPayload.setContent(Collections.singletonList(mockSolution));

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

			//prepare the clients
			when(
				this.clients.getFederationClient(
					any(String.class)
				)
			)
			.thenAnswer(new Answer<FederationClient>() {
					public FederationClient answer(InvocationOnMock theInvocation) {
						//this ends up providing a client based on the mocked http client
					  return new FederationClient(
                  (String)theInvocation.getArguments()[0]/*the URI*/,
                  federationClient);
					}
				});

	
		}
		catch(Exception x) {
			log.error(EELFLoggerDelegate.errorLogger, "Failed to setup mock", x);
			assertTrue(1 == 0);
		}

		try {
			boolean complete = listener.peerEventLatch.await(10, TimeUnit.SECONDS);
			log.info(EELFLoggerDelegate.debugLogger, "event: " + complete + "/" + listener.peerEventLatch.getCount());
			assertTrue("All expected events have occured in the test interval",
								 complete);
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

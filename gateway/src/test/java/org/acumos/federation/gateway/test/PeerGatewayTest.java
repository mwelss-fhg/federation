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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPPeerSubscription;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.cds.transport.RestPageRequest;
import org.acumos.cds.transport.RestPageResponse;
import org.acumos.federation.gateway.cds.PeerStatus;
import org.acumos.federation.gateway.common.Clients;
import org.acumos.federation.gateway.common.FederationClient;
import org.acumos.nexus.client.NexusArtifactClient;
import org.acumos.nexus.client.data.UploadArtifactInfo;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;


/**
 */

//@RunWith(SpringJUnit4ClassRunner.class)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = org.acumos.federation.gateway.Application.class,
								webEnvironment = WebEnvironment.RANDOM_PORT,
								properties = {
									"federation.instance=gateway",
									"federation.instance.name=test",
									"federation.operator=admin",
									"federation.ssl.key-store=classpath:acumosa.pkcs12",
									"federation.ssl.key-store-password=acumosa",
									"federation.ssl.key-store-type=PKCS12",
									"federation.ssl.key-password = acumosa",
									"federation.ssl.trust-store=classpath:acumosTrustStore.jks",
									"federation.ssl.trust-store-password=acumos",
									"federation.ssl.client-auth=need"
									//no actual cds info needed as we mock the cds client
								})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PeerGatewayTest {

	@Mock
	private ICommonDataServiceRestClient cdsClient;

	@Mock
	private NexusArtifactClient nexusClient;

	@MockBean(name = "federationClient")
	private HttpClient	federationClient;

	@MockBean(name = "clients")
	private Clients	clients;

	@Autowired
	private ApplicationContext context;

	//initialize with the number of checkpoints
	private CountDownLatch stepLatch = new CountDownLatch(4);

	@Before
	public void initTest() {
		MockitoAnnotations.initMocks(this);
	}

	/**
	 * The gateway behaviour is triggered by the availability of other solutions
	 * in a peer, as provided by the federation client.  
	 */
	@Test
	public void testGateway() {

		try {
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

			BasicHttpResponse mockSolutionRevisionsResponse = 
				new BasicHttpResponse(
					new BasicStatusLine(
						new ProtocolVersion("HTTP",1,1), 200, "Success"));

			ClassPathResource mockSolutionRevisions =
				new ClassPathResource("mockPeerSolutionRevisionsResponse.json");

			mockSolutionRevisionsResponse.setEntity(
				new InputStreamEntity(mockSolutionRevisions.getInputStream()));
			mockSolutionRevisionsResponse
				.addHeader("Content-Type", ContentType.APPLICATION_JSON.toString());
			mockSolutionRevisionsResponse
				.addHeader("Content-Length", String.valueOf(mockSolutionRevisions.contentLength()));

			BasicHttpResponse mockSolutionRevisionArtifactsResponse = 
				new BasicHttpResponse(
					new BasicStatusLine(
						new ProtocolVersion("HTTP",1,1), 200, "Success"));

			ClassPathResource mockSolutionRevisionArtifacts =
				new ClassPathResource("mockPeerSolutionRevisionArtifactsResponse.json");

			mockSolutionRevisionArtifactsResponse.setEntity(
				new InputStreamEntity(mockSolutionRevisionArtifacts.getInputStream()));
			mockSolutionRevisionArtifactsResponse
				.addHeader("Content-Type", ContentType.APPLICATION_JSON.toString());
			mockSolutionRevisionArtifactsResponse
				.addHeader("Content-Length", String.valueOf(mockSolutionRevisionArtifacts.contentLength()));

			BasicHttpResponse mockDownloadResponse = 
				new BasicHttpResponse(
					new BasicStatusLine(
						new ProtocolVersion("HTTP",1,1), 200, "Success"));

			mockDownloadResponse.setEntity(
				new ByteArrayEntity(new byte[] {}));
			mockDownloadResponse
				.addHeader("Content-Type", ContentType.APPLICATION_OCTET_STREAM.toString());
			mockDownloadResponse
				.addHeader("Content-Length", String.valueOf(0));

			//prepare the clients
			when(
				this.clients.getCDSClient()
			)
			//.thenReturn(cdsClient);
			.thenAnswer(new Answer<ICommonDataServiceRestClient>() {
					public ICommonDataServiceRestClient answer(InvocationOnMock theInvocation) {
						return cdsClient;
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
					  return new FederationClient(
                  (String)theInvocation.getArguments()[0]/*the URI*/,
                  federationClient);
					/* not working as real method relies on the application context
						 which is not set because we work on a  mock
						try {
							return (FederationClient)theInvocation.callRealMethod();
						}
						catch (Throwable t) {
							t.printStackTrace();
							return null;
						}
					*/
					}
				});

			when(
				this.clients.getNexusClient()
			)
			.thenReturn(nexusClient);

			when(
				this.nexusClient.uploadArtifact(
					any(String.class),any(String.class),any(String.class),any(String.class),any(Long.class),any(InputStream.class)
				)
			)
			.thenReturn(new UploadArtifactInfo("","","","","",0));

			when(
				this.cdsClient.searchPeers(
					any(Map.class), any(Boolean.class), any(RestPageRequest.class)
				)
			)
			.thenAnswer(new Answer<RestPageResponse<MLPPeer>>() {
					public RestPageResponse<MLPPeer> answer(InvocationOnMock theInvocation) {
						Map selector = (Map)theInvocation.getArguments()[0];
						MLPPeer peer = new MLPPeer();
						if (selector != null && selector.containsKey("isSelf") && selector.get("isSelf").equals(Boolean.TRUE)) {
							peer.setPeerId("0");
							peer.setName("testSelf");
							peer.setSubjectName("test.org");
							peer.setStatusCode(PeerStatus.Active.code());
							peer.setSelf(true);
							peer.setApiUrl("https://localhost:1110");
						}
						else {
							peer.setPeerId("1");
							peer.setName("testPeer");
							peer.setSubjectName("test.org");
							peer.setStatusCode(PeerStatus.Active.code());
							peer.setSelf(false);
							peer.setApiUrl("https://localhost:1111");
						}
	
						RestPageResponse page = new RestPageResponse(Collections.singletonList(peer));
						page.setNumber(1);
						page.setSize(1);
						page.setTotalPages(1);
						page.setTotalElements(1);
						page.setFirst(true);
						page.setLast(true);
						return page;
					}
				});
		
			when(
				this.cdsClient.getPeers(
					any(RestPageRequest.class)
				)
			)
			.thenAnswer(new Answer<RestPageResponse<MLPPeer>>() {
					public RestPageResponse<MLPPeer> answer(InvocationOnMock theInvocation) {
						MLPPeer peer = new MLPPeer();
						peer.setPeerId("1");
						peer.setName("testPeer");
						peer.setSubjectName("test.org");
						peer.setStatusCode(PeerStatus.Active.code());
						peer.setSelf(false);
						peer.setApiUrl("https://localhost:1111");

						RestPageResponse page = new RestPageResponse(Collections.singletonList(peer));
						page.setNumber(1);
						page.setSize(1);
						page.setTotalPages(1);
						page.setTotalElements(1);
						page.setFirst(true);
						page.setLast(true);
						return page;
					}
				});
	
			when(
				this.cdsClient.getPeerSubscriptions(
					any(String.class)
				)
			)
			.thenAnswer(new Answer<List<MLPPeerSubscription>>() {
					public List<MLPPeerSubscription> answer(InvocationOnMock theInvocation) {
						MLPPeerSubscription sub = new MLPPeerSubscription();
						sub.setSubId(Long.valueOf(12));
						sub.setPeerId("1");
						sub.setSelector("");
						sub.setRefreshInterval(Long.valueOf(300));

						return Collections.singletonList(sub);
					}
				});
	
 
			//pretend the solution does not exist locally .. 
			when(
				this.cdsClient.getSolution(
					any(String.class)
				)
			) 
			.thenReturn(null);

			//as a result the gateway should attempt to create a local solution
			//with the information it got from the 'peer' (see the mock response)
			when(
				this.cdsClient.createSolution(
					any(MLPSolution.class)
				)
			)
			//.thenReturn(mockSolution);
			.thenAnswer(new Answer<MLPSolution>() {
					public MLPSolution answer(InvocationOnMock theInvocation) {
						stepLatch.countDown();
						return (MLPSolution)theInvocation.getArguments()[0];
					}
				});

			//the gateway should attempt to get the revisions from the peer	and
			//compare them against locally available ones (based on the last one)
			when(
				this.cdsClient.getSolutionRevisions(
					any(String.class)
				)
			) 
			.thenAnswer(new Answer<List<MLPSolutionRevision>>() {
					public List<MLPSolutionRevision> answer(InvocationOnMock theInvocation) {
						stepLatch.countDown();
						//pretend we do not have a local match so that we trigger
						//an insert
						return Collections.EMPTY_LIST;
					}
				});

			when(
				this.cdsClient.createSolutionRevision(
					any(MLPSolutionRevision.class)
				)
			)
			.thenAnswer(new Answer<MLPSolutionRevision>() {
					public MLPSolutionRevision answer(InvocationOnMock theInvocation) {
						stepLatch.countDown();
						return (MLPSolutionRevision)theInvocation.getArguments()[0];
					}
				});

			//pretend artifact is not available locally
			when(
				this.cdsClient.getArtifact(
					any(String.class)
				)
			) 
			.thenReturn(null);

			when(
				this.cdsClient.createArtifact(
					any(MLPArtifact.class)
				)
			) 
			.thenAnswer(new Answer<MLPArtifact>() {
					public MLPArtifact answer(InvocationOnMock theInvocation) {
						stepLatch.countDown();
						return (MLPArtifact)theInvocation.getArguments()[0];
					}
				});

			doAnswer(new Answer<Void>() {
					public Void answer(InvocationOnMock theInvocation) {
						//enable this when download mocking is complete
						//stepLatch.countDown();
						return null;
					}
				}).when(this.cdsClient).updateArtifact(any(MLPArtifact.class));

			doAnswer(new Answer<Void>() {
					public Void answer(InvocationOnMock theInvocation) {
						//stepLatch.countDown();
						return null;
					}
				}).when(this.cdsClient)
				.addSolutionRevisionArtifact(
					any(String.class),any(String.class),any(String.class));


			//see TaskTest for full mocking of the http client. this is thte method
			//that gets used by the RestTemplate so we'll keep it short here
			//we need to 
			when(
				this.federationClient.execute(
					any(HttpUriRequest.class), any(HttpContext.class)
				)
			//).thenReturn(mockResponse);
			).thenAnswer(new Answer<HttpResponse>() {
					public HttpResponse answer(InvocationOnMock theInvocation) {
						HttpUriRequest req = (HttpUriRequest)
							theInvocation.getArguments()[0];
						String path = req.getURI().getPath();
						if (path.equals("/solutions"))
							return mockSolutionsResponse;
						if (path.endsWith("/revisions"))
							return mockSolutionRevisionsResponse;
						if (path.endsWith("/artifacts"))
							return mockSolutionRevisionArtifactsResponse;
						if (path.endsWith("/download"))
							return mockDownloadResponse;

	System.out.println(" *** Mock unhandled path " + path);
						return null;
					}
				});
					
		}
		catch(Exception x) {
			System.out.println(" *** Failed to setup mock : " + x);
			x.printStackTrace();
			assertTrue(1 == 0);
		}

		//let the test wait for a few seconds so that the expected
		//behaviour kicks in
		boolean completed = false;
		try {
			completed = stepLatch.await(10, TimeUnit.SECONDS);
		}
		catch (InterruptedException ix) {
			assertTrue(1 == 0);
		}
		if (!completed)
			System.out.println(" *** Failed to complete,  " + stepLatch.getCount() + " steps left");
			
		//if we are here is that all steps that we expected took place
		assertTrue(completed);
	}

}

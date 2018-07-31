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

import java.io.InputStream;
import java.io.Closeable;
import java.io.IOException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Collections;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.mockito.invocation.InvocationOnMock;

import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HttpContext;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse; 
import org.apache.http.message.BasicStatusLine;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;

/* this is not good for unit testing .. */
import org.acumos.federation.gateway.common.JsonResponse;
import org.acumos.federation.gateway.event.PeerSubscriptionEvent;
import org.acumos.federation.gateway.common.Clients;
import org.acumos.federation.gateway.config.LocalInterfaceConfiguration;
import org.acumos.federation.gateway.cds.PeerStatus;
import org.acumos.federation.gateway.security.Peer;
import org.acumos.federation.gateway.security.Role;
import org.acumos.federation.gateway.service.PeerService;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.service.ServiceException;

import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPPeerSubscription;


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
									"federation.ssl.key-store=classpath:acumosb.pkcs12",
									"federation.ssl.key-store-password=acumosb",
									"federation.ssl.key-store-type=PKCS12",
									"federation.ssl.key-password = acumosb",
									"federation.ssl.trust-store=classpath:acumosTrustStore.jks",
									"federation.ssl.trust-store-password=acumos",
									"federation.ssl.client-auth=need",
									"local.addr=127.0.0.1",
									"local.server.port=9011",
									"local.ssl.key-store=classpath:acumosb.pkcs12",
									"local.ssl.key-store-password=acumosb",
									"local.ssl.key-store-type=PKCS12",
									"local.ssl.key-password=acumosb",
									"cdms.client.url=http://localhost:8000/ccds",
									"cdms.client.username=username",
									"cdms.client.password=password"
									})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PeerServiceTest extends ServiceTest {

	@Autowired
	private PeerService peerService;

	protected void initMockResponses() {
		registerMockResponse("GET /ccds/peer/search?isSelf=true&_j=a", MockResponse.success("mockCDSPeerSearchSelfResponse.json"));
		registerMockResponse("GET /ccds/peer?page=0&size=100", MockResponse.success("mockCDSPeerSearchAllResponse.json"));
		//registerMockResponse("/ccds/peer", MockResponse.success("mockCDSPeerSearchAllResponse.json"));
		registerMockResponse("GET /ccds/peer/search?subjectName=gateway.acumosa.org&_j=a", MockResponse.success("mockCDSPeerSearchResponse.json"));
		registerMockResponse("GET /ccds/peer/search?subjectName=gateway.acumosc.org&_j=a", MockResponse.success("mockCDSSearchEmptyResponse.json"));
		registerMockResponse("PUT /ccds/peer/a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0", MockResponse.success("mockCDSPeerUpdateResponse.json"));
		registerMockResponse("POST /ccds/peer", MockResponse.success("mockCDSPeerCreateResponse.json"));
	}


	/**
	 * The gateway behaviour is triggered by the availability of other solutions
	 * in a peer, as provided by the federation client.  
	 */
	@Test
	public void testPeerService() {

		try {
			ServiceContext selfService = 
				ServiceContext.forPeer(new Peer(new MLPPeer("acumosb", "gateway.acumosb.org", "https://gateway.acumosb.org:9084", false, false, "admin@acumosab.org", "AC", "PS"), Role.SELF));

			List<MLPPeer> peers = peerService.getPeers(selfService);
			assertTrue("Unexpected all peers response", peers.size() == 2);

			List<MLPPeer> peersn = peerService.getPeerBySubjectName("gateway.acumosa.org");
			assertTrue("Expected one peer to be found", peersn.size() == 1);

			try {
				peerService.registerPeer(new MLPPeer("acumosc", "gateway.acumosc.org", "https://gateway.acumosc.org:9084", false, false, "admin@acumosc.org", "AC", "PS"));
			}
			catch(ServiceException sx) {
				fail("Expected peer register to succeed: " + sx + "/" + sx.getCause());
			}

			try {
				peerService.unregisterPeer(peersn.get(0));
			}
			catch(ServiceException sx) {
				fail("Expected peer unregister to succeed: " + sx + "/" + sx.getCause());
			}
		}
		catch(Exception sx) {
			fail("Unexpected peer test outcome: " + sx);
		}
	}

}

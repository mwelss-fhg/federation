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
import org.acumos.federation.gateway.service.CatalogService;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.service.ServiceException;
import org.acumos.federation.gateway.cds.Solution;
import org.acumos.federation.gateway.cds.SolutionRevision;

import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPPeerSubscription;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.transport.RestPageRequest;
import org.acumos.cds.transport.RestPageResponse;

import org.acumos.nexus.client.NexusArtifactClient;
import org.acumos.nexus.client.data.UploadArtifactInfo;


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
									"federation.ssl.client-auth=need",
									"local.addr=127.0.0.1",
									"local.server.port=9011",
									"local.ssl.key-store=classpath:acumosa.pkcs12",
									"local.ssl.key-store-password=acumosa",
									"local.ssl.key-store-type=PKCS12",
									"local.ssl.key-password=acumosa",
									"cdms.client.url=http://localhost:8000/ccds",
									"cdms.client.username=username",
									"cdms.client.password=password"
									})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CatalogServiceTest extends ServiceTest {

	
	@Autowired
	private CatalogService catalog;

	protected void initMockResponses() {

		registerMockResponse("GET /ccds/solution/search/portal?atc=PB&vsc=PS&active=true&page=0&size=50", MockResponse.success("mockCDSPortalSolutionsResponse.json"));
		registerMockResponse("GET /ccds/solution/search/date?atc=PB&datems=1531747662&vsc=PS&active=true&page=0&size=50", MockResponse.success("mockCDSDateSolutionsResponsePage0.json"));
		registerMockResponse("GET /ccds/solution/search/date?atc=PB&datems=1531747662&vsc=PS&active=true&page=1&size=50", MockResponse.success("mockCDSDateSolutionsResponsePage1.json"));
		registerMockResponse("GET /ccds/solution/10101010-1010-1010-1010-101010101010", MockResponse.success("mockCDSSolutionResponse.json"));
		registerMockResponse("GET /ccds/solution/10101010-1010-1010-1010-101010101010/revision", MockResponse.success("mockCDSSolutionRevisionsResponse.json"));
		registerMockResponse("GET /ccds/solution/10101010-1010-1010-1010-101010101010/revision/a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0/artifact", MockResponse.success("mockCDSSolutionRevisionArtifactsResponse.json"));
		registerMockResponse("GET /ccds/solution/f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0", new MockResponse(400, "Error", "mockCDSNoEntryWithIDResponse.json"));
		registerMockResponse("GET /ccds/solution/f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0/revision", new MockResponse(400, "Error", "mockCDSNoEntryWithIDResponse.json"));
		registerMockResponse("GET /ccds/solution/f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0/revision/f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0/artifact", new MockResponse(400, "Error", "mockCDSNoEntryWithIDResponse.json"));
		registerMockResponse("GET /ccds/solution/f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1", new MockResponse(412, "Error", "mockCDSErrorResponse.json"));
		registerMockResponse("GET /ccds/solution/f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1/revision", new MockResponse(412, "Error", "mockCDSErrorResponse.json"));
		registerMockResponse("GET /ccds/solution/f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1/revision/f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1/artifact", new MockResponse(412, "Error", "mockCDSErrorResponse.json"));
		registerMockResponse("GET /ccds/peer/search?isSelf=true&_j=a", MockResponse.success("mockCDSPeerSearchSelfResponse.json"));
	}

	/**
	 * The gateway behaviour is triggered by the availability of other solutions
	 * in a peer, as provided by the federation client.  
	 */
	@Test
	public void testCatalogService() {

		try {
			ServiceContext selfService = 
				ServiceContext.forPeer(new Peer(new MLPPeer("acumosb", "gateway.acumosb.org", "https://gateway.acumosb.org:9084", false, false, "admin@acumosab.org", "AC", "PS"), Role.SELF));

			List<MLPSolution> solutions = catalog.getSolutions(Collections.EMPTY_MAP, selfService);
			assertTrue("Unexpected solutions count: " + solutions.size(), solutions.size() == 5);
			
			Solution solution = catalog.getSolution("10101010-1010-1010-1010-101010101010", selfService);
			assertTrue("Unexpected solution info", solution.getName().equals("test"));

			List<? extends MLPSolutionRevision> revisions = solution.getRevisions();
			if (revisions != null && !revisions.isEmpty()) {
				assertTrue("Unexpected revisions count: " + revisions.size(), revisions.size() == 1);
				for (MLPSolutionRevision revision: revisions) {
					assertTrue("Unexpected revision info", revision.getDescription().startsWith("test"));
					List<MLPArtifact> artifacts = catalog.getSolutionRevisionArtifacts(solution.getSolutionId(), revision.getRevisionId());
					assertTrue("Unexpected artifacts count: " + artifacts.size(), artifacts.size() == 1);
						//catalog.getSolutionRevisionArtifact();
				}
			}

			//no such entry
			Solution nsSolution = catalog.getSolution("f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0", selfService);
			assertTrue("Unexpected no such solution outcome", nsSolution == null);

			List<MLPSolutionRevision> nsRevisions = catalog.getSolutionRevisions("f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0", selfService);
			assertTrue("Unexpected no such solution (revisions) outcome", nsRevisions == null);

			List<MLPArtifact> nsArtifacts = catalog.getSolutionRevisionArtifacts("f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0", "f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0", selfService);
			assertTrue("Unexpected no such solution (revision artifacts) outcome", nsArtifacts == null);

			//other errors
			try {
				catalog.getSolution("f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1", selfService);
				assertTrue("Expected service exception, got none", 1 == 0);
			}
			catch (ServiceException sx) {
			}
		
			try {
				catalog.getSolutionRevisions("f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1", selfService);
				assertTrue("Expected service exception, got none", 1 == 0);
			}
			catch (ServiceException sx) {
			}

			try {
				catalog.getSolutionRevisionArtifacts("f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1", "f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1", selfService);
				assertTrue("Expected service exception, got none", 1 == 0);
			}
			catch (ServiceException sx) {
			}
		}
		catch (Exception x) {
			assertTrue("Unexpected catalog test outcome: " + x, 1 == 0);
		}
	}

}

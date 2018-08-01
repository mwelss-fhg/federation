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
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.List;

import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.federation.gateway.cds.Solution;
import org.acumos.federation.gateway.security.Peer;
import org.acumos.federation.gateway.security.Role;
import org.acumos.federation.gateway.service.CatalogService;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.service.ServiceException;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
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
		//registerMockResponse("GET /ccds/solution/10101010-1010-1010-1010-101010101010/revision/a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0/artifact", MockResponse.success("mockCDSSolutionRevisionArtifactsResponse.json"));
		registerMockResponse("GET /ccds/revision/a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0/artifact", MockResponse.success("mockCDSSolutionRevisionArtifactsResponse.json"));
		registerMockResponse("GET /ccds/solution/f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0", new MockResponse(400, "Error", "mockCDSNoEntryWithIDResponse.json"));
		registerMockResponse("GET /ccds/solution/f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0/revision", new MockResponse(400, "Error", "mockCDSNoEntryWithIDResponse.json"));
		//registerMockResponse("GET /ccds/solution/f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0/revision/f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0/artifact", new MockResponse(400, "Error", "mockCDSNoEntryWithIDResponse.json"));
		registerMockResponse("GET /ccds/revision/f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0/artifact", new MockResponse(400, "Error", "mockCDSNoEntryWithIDResponse.json"));
		registerMockResponse("GET /ccds/solution/f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1", new MockResponse(412, "Error", "mockCDSErrorResponse.json"));
		registerMockResponse("GET /ccds/solution/f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1/revision", new MockResponse(412, "Error", "mockCDSErrorResponse.json"));
		//registerMockResponse("GET /ccds/solution/f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1/revision/f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1/artifact", new MockResponse(412, "Error", "mockCDSErrorResponse.json"));
		registerMockResponse("GET /ccds/revision/f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1/artifact", new MockResponse(412, "Error", "mockCDSErrorResponse.json"));
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
			fail("Unexpected catalog test outcome: " + x);
		}
	}

}

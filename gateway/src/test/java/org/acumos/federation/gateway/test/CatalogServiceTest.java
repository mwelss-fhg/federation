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

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
									"spring.main.allow-bean-definition-overriding=true",
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

	private static Map<String, Object> selector(Object... flds) {
		Map<String, Object> ret = new HashMap<String, Object>();
		for (int i = 0; i < flds.length; i += 2) {
			ret.put((String)flds[i], flds[i + 1]);
		}
		return(ret);
	}

	protected void initMockResponses() {

		registerMockResponse("GET /ccds/catalog/mycatalog/solution?page=0&size=100", MockResponse.success("mockCDSPortalSolutionsResponse.json"));
		registerMockResponse("GET /ccds/solution/search/date?atc=PB&inst=1000&active=true&page=0&size=100", MockResponse.success("mockCDSPortalSolutionsResponse.json"));
		registerMockResponse("GET /ccds/solution/search/date?atc=PB&inst=1531747662000&active=true&page=0&size=100", MockResponse.success("mockCDSDateSolutionsResponsePage0.json"));
		registerMockResponse("GET /ccds/solution/search/date?atc=PB&inst=1531747662000&active=true&page=1&size=100", MockResponse.success("mockCDSDateSolutionsResponsePage1.json"));
		registerMockResponse("GET /ccds/solution/10101010-1010-1010-1010-101010101010", MockResponse.success("mockCDSSolutionResponse.json"));
		registerMockResponse("GET /ccds/solution/10101010-1010-1010-1010-101010101010/revision", MockResponse.success("mockCDSSolutionRevisionsResponse.json"));
		registerMockResponse("GET /ccds/revision/a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0/artifact", MockResponse.success("mockCDSSolutionRevisionArtifactsResponse.json"));
		registerMockResponse("GET /ccds/solution/f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0", new MockResponse(400, "Error", "mockCDSNoEntryWithIDResponse.json"));
		registerMockResponse("GET /ccds/solution/f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0/revision", new MockResponse(400, "Error", "mockCDSNoEntryWithIDResponse.json"));
		registerMockResponse("GET /ccds/revision/f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0/artifact", new MockResponse(400, "Error", "mockCDSNoEntryWithIDResponse.json"));
		registerMockResponse("GET /ccds/solution/f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1", new MockResponse(412, "Error", "mockCDSErrorResponse.json"));
		registerMockResponse("GET /ccds/solution/f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1/revision", new MockResponse(412, "Error", "mockCDSErrorResponse.json"));
		registerMockResponse("GET /ccds/revision/f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1/artifact", new MockResponse(412, "Error", "mockCDSErrorResponse.json"));
		registerMockResponse("GET /ccds/peer/search?self=true&_j=a&page=0&size=100", MockResponse.success("mockCDSPeerSearchSelfResponse.json"));
	}

	/**
	 * The gateway behaviour is triggered by the availability of other solutions
	 * in a peer, as provided by the federation client.  
	 */
	@Test
	public void testCatalogService() {

		try {
			ServiceContext selfService = 
				ServiceContext.forPeer(new Peer(new MLPPeer("acumosa", "gateway.acumosa.org", "https://gateway.acumosa.org:9084", false, false, "admin@acumosa.org", "AC"), Role.SELF));

			List<MLPSolution> solutions = catalog.getSolutions(selector("catalogId", "mycatalog"), selfService);
			assertTrue("Unexpected solutions count: " + solutions.size(), solutions.size() == 5);
			solutions = catalog.getSolutions(selector("catalogId", "mycatalog", "modelTypeCode", "RG"), selfService);
			assertTrue("Unexpected solutions count: " + solutions.size(), solutions.size() == 2);
			solutions = catalog.getSolutions(selector("catalogId", "mycatalog", "toolkitTypeCode", new CopyOnWriteArrayList(new String[] {"CP", "TF" })), selfService);
			assertTrue("Unexpected solutions count: " + solutions.size(), solutions.size() == 3);
			solutions = catalog.getSolutions(selector("catalogId", "mycatalog", "tags", "subtract"), selfService);
			assertTrue("Unexpected solutions count: " + solutions.size(), solutions.size() == 1);
			solutions = catalog.getSolutions(selector("catalogId", "mycatalog", "tags", new CopyOnWriteArrayList(new String[] { "subtract", "poutput"})), selfService);
			assertTrue("Unexpected solutions count: " + solutions.size(), solutions.size() == 2);
			solutions = catalog.getSolutions(selector("catalogId", "mycatalog", "solutionId", "38efeef1-e4f4-4298-9f68-6f0052d6ade9"), selfService);
			assertTrue("Unexpected solutions count: " + solutions.size(), solutions.size() == 1);
			try {
				catalog.getSolutions(selector("catalogId", "mycatalog", "name", new CopyOnWriteArrayList(new String[] { "A", "B" })), selfService);
				assertTrue("Expected service exception, got none", 1 == 0);
			}
			catch (ServiceException sx) {
			}
			try {
				catalog.getSolutions(selector("catalogId", "mycatalog", "name", true), selfService);
				assertTrue("Expected service exception, got none", 1 == 0);
			}
			catch (ServiceException sx) {
			}
			try {
				catalog.getSolutions(selector("catalogId", "mycatalog", "tags", true), selfService);
				assertTrue("Expected service exception, got none", 1 == 0);
			}
			catch (ServiceException sx) {
			}
			solutions = catalog.getSolutions(Collections.EMPTY_MAP, selfService);
			assertTrue("Unexpected solutions count: " + solutions.size(), solutions.size() == 5);
		
			Solution solution = catalog.getSolution("10101010-1010-1010-1010-101010101010", selfService);
			assertTrue("Unexpected solution info", solution.getName().equals("test"));

			List<? extends MLPSolutionRevision> revisions = solution.getRevisions();
			if (revisions != null && !revisions.isEmpty()) {
				assertTrue("Unexpected revisions count: " + revisions.size(), revisions.size() == 1);
				for (MLPSolutionRevision revision: revisions) {
					assertTrue("Unexpected revision info", revision.getVersion().startsWith("1"));
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
			fail("Unexpected catalog test outcome: " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(x));
		}
	}

}

/*-
 * ===============LICENSE_START=======================================================
 * Acumos
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPCatalog;
import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.federation.gateway.cds.Artifact;
import org.acumos.federation.gateway.cds.Catalog;
import org.acumos.federation.gateway.cds.Document;
import org.acumos.federation.gateway.cds.Solution;
import org.acumos.federation.gateway.cds.SolutionRevision;
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

	protected void initMockResponses() {

		registerMockResponse("GET /ccds/catalog/search?accessTypeCode=PB&_j=a&page=0&size=100", MockResponse.success("mockCDSPortalCatalogsResponse.json"));
		registerMockResponse("GET /ccds/catalog/5ebbc521-1642-4d4c-a732-d9e8a6b51f4a/solution/count", MockResponse.success("mockCDSPortalSolutionCountResponse.json"));
		registerMockResponse("GET /ccds/catalog/e072d118-0875-438b-8c9e-cf1f8ef3d9cb/solution/count", MockResponse.success("mockCDSPortalSolutionCountResponse.json"));
		registerMockResponse("GET /ccds/catalog/mycatalog/solution/count", MockResponse.success("mockCDSPortalSolutionCountResponse.json"));
		registerMockResponse("GET /ccds/access/peer/testpeerid/catalog", MockResponse.success("mockCDSPortalPeerAccessCatalogIdsResponse.json"));
		registerMockResponse("GET /ccds/catalog/mycatalog", MockResponse.success("mockCDSPortalGetCatalogResponse.json"));
		registerMockResponse("GET /ccds/catalog/solution?ctlg=mycatalog&page=0&size=100", MockResponse.success("mockCDSPortalSolutionsResponse.json"));
		registerMockResponse("GET /ccds/solution/search/date?atc=PB&inst=1000&active=true&page=0&size=100", MockResponse.success("mockCDSPortalSolutionsResponse.json"));
		registerMockResponse("GET /ccds/solution/search/date?atc=PB&inst=1531747662000&active=true&page=0&size=100", MockResponse.success("mockCDSDateSolutionsResponsePage0.json"));
		registerMockResponse("GET /ccds/solution/search/date?atc=PB&inst=1531747662000&active=true&page=1&size=100", MockResponse.success("mockCDSDateSolutionsResponsePage1.json"));
		registerMockResponse("GET /ccds/solution/10101010-1010-1010-1010-101010101010", MockResponse.success("mockCDSSolutionResponse.json"));
		registerMockResponse("GET /ccds/solution/10101010-1010-1010-1010-101010101010/revision", MockResponse.success("mockCDSSolutionRevisionsResponse.json"));
		registerMockResponse("GET /ccds/solution/38efeef1-e4f4-4298-9f68-6f0052d6ade9/revision/2c7e4481-6e6f-47d9-b7a4-c4e674d2b341", MockResponse.success("mockCDSSolutionRevisionResponse.json"));
		registerMockResponse("GET /ccds/revision/2c7e4481-6e6f-47d9-b7a4-c4e674d2b341", MockResponse.success("mockCDSSolutionRevisionResponse.json"));
		registerMockResponse("GET /ccds/solution/10101010-1010-1010-1010-101010101010/pic", MockResponse.success("mockCDSSolutionPicResponse.tgz"));
		registerMockResponse("GET /ccds/revision/a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0/artifact", MockResponse.success("mockCDSSolutionRevisionArtifactsResponse.json"));
		registerMockResponse("GET /ccds/revision/2c7e4481-6e6f-47d9-b7a4-c4e674d2b341/artifact", MockResponse.success("mockCDSSolutionRevisionArtifactsResponse.json"));
		registerMockResponse("GET /ccds/revision/2c7e4481-6e6f-47d9-b7a4-c4e674d2b341/catalog/mycatalog/document", MockResponse.success("mockCDSSolutionRevisionDocumentsResponse.json"));
		registerMockResponse("GET /ccds/revision/2c7e4481-6e6f-47d9-b7a4-c4e674d2b341/catalog/mycatalog/descr", MockResponse.success("mockCDSSolutionRevisionDescriptionResponse.json"));
		registerMockResponse("GET /ccds/artifact/2c2c2c2c-6e6f-47d9-b7a4-c4e674d2b341", MockResponse.success("mockCDSSolutionRevisionArtifactResponse.json"));
		registerMockResponse("GET /ccds/document/2c2c2c2c-6e6f-47d9-b7a4-c4e674d2b342", MockResponse.success("mockCDSSolutionRevisionDocumentResponse.json"));
		registerMockResponse("GET /ccds/solution/f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0", new MockResponse(400, "Error", "mockCDSNoEntryWithIDResponse.json"));
		registerMockResponse("GET /ccds/solution/f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0/revision", new MockResponse(400, "Error", "mockCDSNoEntryWithIDResponse.json"));
		registerMockResponse("GET /ccds/revision/f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0/artifact", new MockResponse(400, "Error", "mockCDSNoEntryWithIDResponse.json"));
		registerMockResponse("GET /ccds/solution/f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1", new MockResponse(412, "Error", "mockCDSErrorResponse.json"));
		registerMockResponse("GET /ccds/solution/f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1/revision", new MockResponse(412, "Error", "mockCDSErrorResponse.json"));
		registerMockResponse("GET /ccds/revision/f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1/artifact", new MockResponse(412, "Error", "mockCDSErrorResponse.json"));
		registerMockResponse("GET /ccds/peer/search?self=true&_j=a&page=0&size=100", MockResponse.success("mockCDSPeerSearchSelfResponse.json"));
		registerMockResponse("GET /ccds/access/peer/testpeerid/catalog/forbidden", MockResponse.success("mockCDSForbiddenResponse.json"));
		registerMockResponse("GET /ccds/access/peer/testpeerid/catalog/allowed", MockResponse.success("mockCDSAllowedResponse.json"));
		registerMockResponse("GET /ccds/access/peer/testpeerid/solution/forbidden", MockResponse.success("mockCDSForbiddenResponse.json"));
		registerMockResponse("GET /ccds/access/peer/testpeerid/solution/allowed", MockResponse.success("mockCDSAllowedResponse.json"));
		registerMockResponse("GET /ccds/solution/ignored/revision/allowed", MockResponse.success("mockCDSSolutionRevisionResponse.json"));
		registerMockResponse("GET /ccds/access/peer/testpeerid/solution/10101010-1010-1010-1010-101010101010", MockResponse.success("mockCDSAllowedResponse.json"));
		registerMockResponse("GET /ccds/solution/ignored/revision/forbidden", MockResponse.success("mockCDSForbiddenSolutionRevisionResponse.json"));
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

			ServiceContext contextWithPeerId = 
				ServiceContext.forPeer(new Peer(new MLPPeer("acumosa", "gateway.acumosa.org", "https://gateway.acumosa.org:9084", false, false, "admin@acumosa.org", "AC"), Role.SELF));
			contextWithPeerId.getPeer().getPeerInfo().setPeerId("testpeerid");

			List<MLPCatalog> catalogs = catalog.getCatalogs(selfService);
			assertEquals("Unexpected catalogs count", 2, catalogs.size());
			assertEquals("Unexpected catalog solution count", 5, ((Catalog)catalogs.get(0)).getSize());
			catalogs = catalog.getCatalogs(contextWithPeerId);
			assertEquals("Unexpected catalogs count", 3, catalogs.size());
			List<MLPSolution> solutions = catalog.getSolutions("mycatalog", selfService);
			assertEquals("Unexpected solutions count", 5, solutions.size());
			SolutionRevision rev = catalog.getRevision("mycatalog", "38efeef1-e4f4-4298-9f68-6f0052d6ade9", "2c7e4481-6e6f-47d9-b7a4-c4e674d2b341");
			Artifact art = catalog.getArtifact("2c2c2c2c-6e6f-47d9-b7a4-c4e674d2b341");
			Document doc = catalog.getDocument("2c2c2c2c-6e6f-47d9-b7a4-c4e674d2b342");
			Solution solution = catalog.getSolution("10101010-1010-1010-1010-101010101010", selfService);
			assertEquals("Unexpected solution info", "test", solution.getName());

			List<? extends MLPSolutionRevision> revisions = solution.getRevisions();
			if (revisions != null && !revisions.isEmpty()) {
				assertEquals("Unexpected revisions count", 1, revisions.size());
				for (MLPSolutionRevision revision: revisions) {
					assertTrue("Unexpected revision info", revision.getVersion().startsWith("1"));
					List<MLPArtifact> artifacts = catalog.getArtifacts(solution.getSolutionId(), revision.getRevisionId());
					assertEquals("Unexpected artifacts count", 1, artifacts.size());
				}
			}

			//no such entry
			Solution nsSolution = catalog.getSolution("f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0", selfService);
			assertNull("Unexpected no such solution outcome", nsSolution);

			List<MLPSolutionRevision> nsRevisions = catalog.getRevisions("f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0", selfService);
			assertNull("Unexpected no such solution (revisions) outcome", nsRevisions);

			List<MLPArtifact> nsArtifacts = catalog.getArtifacts("f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0", "f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0", selfService);
			assertNull("Unexpected no such solution (revision artifacts) outcome", nsArtifacts);

			//other errors
			try {
				catalog.getSolution("f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1", selfService);
				fail("Expected service exception, got none");
			}
			catch (ServiceException sx) {
			}
		
			try {
				catalog.getRevisions("f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1", selfService);
				fail("Expected service exception, got none");
			}
			catch (ServiceException sx) {
			}

			try {
				catalog.getArtifacts("f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1", "f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1", selfService);
				fail("Expected service exception, got none");
			}
			catch (ServiceException sx) {
			}
			assertFalse(catalog.isCatalogAllowed("forbidden", contextWithPeerId));
			assertTrue(catalog.isCatalogAllowed("allowed", contextWithPeerId));
			assertFalse(catalog.isSolutionAllowed("forbidden", contextWithPeerId));
			assertTrue(catalog.isSolutionAllowed("allowed", contextWithPeerId));
			assertFalse(catalog.isRevisionAllowed("forbidden", contextWithPeerId));
			assertTrue(catalog.isRevisionAllowed("allowed", contextWithPeerId));
		}
		catch (Exception x) {
			fail("Unexpected catalog test outcome: " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(x));
		}
	}
}

/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
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
package org.acumos.federation.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.apache.http.entity.ContentType;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.web.client.HttpClientErrorException.Forbidden;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.util.UriTemplateHandler;


import org.acumos.cds.domain.MLPSolution;

import org.acumos.federation.client.config.ClientConfig;
import org.acumos.federation.client.data.ModelData;
import org.acumos.federation.client.data.Solution;
import org.acumos.federation.client.data.SolutionRevision;

import org.acumos.federation.client.test.ClientMocking;
import static org.acumos.federation.client.test.ClientMocking.getConfig;
import static org.acumos.federation.client.test.ClientMocking.xq;

public class ClientTest {
	@Test
	public void testConfig() throws Exception {
		ClientBase.setDefaultMapper(ClientBase.getDefaultMapper());
		ClientConfig cx = getConfig("acumosa");
		cx.getSsl().setKeyAlias("acumosa");
		try {
			new FederationClient("malformed:9999", cx, null, null);
			fail();
		} catch (IllegalArgumentException iae) {
			// expected case
		}
		new FederationClient("http://localhost:9999", cx);
		cx.getSsl().setKeyStore(null);
		cx.getSsl().setTrustStore("--no-such-file--");
		try {
			new FederationClient("http://localhost:9999", cx, ClientBase.getDefaultMapper(), new DefaultResourceLoader());
			fail();
		} catch (TlsConfigException tcx) {
			// expected case
		}
		cx.getSsl().setTrustStore("/dev/null");
		try {
			new FederationClient("http://localhost:9999", cx, null, null);
			fail();
		} catch (TlsConfigException tcx) {
			// expected case
		}
		cx.getSsl().setTrustStorePassword(null);
		cx.getCreds().setPassword(null);
		new FederationClient("http://localhost:9999", cx, null, null);
		cx.getSsl().setTrustStoreType(null);
		cx.getCreds().setUsername(null);
		new FederationClient("http://localhost:9999", cx, null, null);
		cx.setSsl(null);
		cx.setCreds(null);
		new FederationClient("http://localhost:9999", cx, null, null);
	}

	@Test
	public void testGateway() throws Exception {
		GatewayClient client = new GatewayClient("http://localhost:8888", getConfig("acumosa"));
		(new ClientMocking())
		    .errorOnNoAuth(401, "Unauthorized")
		    .errorOnBadAuth("acumosa", "acumosa", 403, "Forbidden")
		    .on("GET /peer/somepeerid/ping", "{}")
		    .on("GET /peer/somepeerid/solutions/someid", xq("{ 'content': { 'solutionId': 'someId', 'picture': '9999', 'revisions': [ { 'artifacts': [], 'documents': [], 'revCatDescription': {} }] }}"))
		    .on("GET /peer/somepeerid/solutions?catalogId=somecatid", xq("{ 'content': [ { 'solutionId': 'someId' }, { 'solutionId': 'someOtherId' }]}"))
		    .on("GET /peer/somepeerid/catalogs", xq("{ 'content': [ { 'catalogId': '1' }, { 'catalogId': '2' }]}"))
		    .on("GET /peer/somepeerid/peers", xq("{ 'content': [ { 'peerId': '1' } ] }"))
		    .on("POST /peer/somepeerid/peer/register", xq("{ 'content': { 'peerId': '1' } }"))
		    .on("POST /peer/somepeerid/subscription/99", xq("{ }"))
		    .applyTo(client);
		assertNull(client.ping("somepeerid"));
		Solution sol = (Solution)client.getSolution("somepeerid", "someid");
		assertNotNull(sol.getSolutionId());
		assertNotNull(sol.getPicture());
		assertNotNull(sol.getRevisions());
		List<MLPSolution> sols = client.getSolutions("somepeerid", "somecatid");
		assertEquals(2, sols.size());
		assertEquals(2, client.getCatalogs("somepeerid").size());
		assertEquals(1, client.getPeers("somepeerid").size());
		assertNotNull(client.register("somepeerid"));
		client.triggerPeerSubscription("somepeerid", 99);
	}

	@Test
	public void testGatewayModelData() throws Exception {
		GatewayClient client = new GatewayClient("http://localhost:8888", getConfig("acumosa"));
		(new ClientMocking()).errorOnNoAuth(401, "Unauthorized")
		.errorOnBadAuth("acumosa", "acumosa", 403, "Forbidden")
		    .on("POST /peer/somepeerid/modeldata", xq("{ 'content': 'successfully send model data to peer' }"))
		    .applyTo(client);
		ObjectMapper objectMapper = new ObjectMapper();
		ModelData modelData =
				objectMapper.readValue("{\"model\": { \"solutionId\": \"UUID\"}}", ModelData.class);
				try {
					client.sendModelData("somepeerid", modelData);
				} catch (Exception e) {
					fail("failed to send model data");
				}
	}


	@Test
	public void testFederation() throws Exception {
		FederationClient client = new FederationClient("http://localhost:9999", getConfig("acumosa"), null, null);
		(new ClientMocking())
		    .on("GET /ping", "{}")
		    .on("GET /solutions/someid", xq("{ 'content': { 'solutionId': 'someId', 'picture': '9999', 'revisions': [ { 'artifacts': [], 'documents': [], 'revCatDescription': {} }] }}"))
		    .on("GET /solutions?catalogId=somecatid", xq("{ 'content': [ { 'solutionId': 'someId' }, { 'solutionId': 'someOtherId' }]}"))
		    .on("GET /solutions?catalogId=emptyanswer", "")
		    .on("GET /catalogs", xq("{ 'content': [ { 'catalogId': '1', 'size': 7 }, { 'catalogId': '2' }]}"))
		    .on("GET /peers", xq("{ 'content': [ { 'peerId': '1' } ] }"))
		    .on("GET /solutions/solid/revisions", xq("{ 'content': [ { 'revisionId': '1' } ] }"))
		    .on("GET /revisions/revid/documents?catalogId=catid", xq("{ 'content': [ { 'documentId': '1' } ] }"))
		    .on("GET /solutions/solid/revisions/revid/artifacts", xq("{ 'content': [ { 'artifactId': '1' } ] }"))
		    .on("GET /solutions/solid/revisions/revid", xq("{ 'content': { 'revisionId': 'revid', 'artifacts': [ { 'artifactId': '1' } ] } }"))
		    .on("GET /solutions/solid/revisions/revid?catalogId=catid", xq("{ 'content': { 'revisionId': 'revid', 'artifacts': [ { 'artifactId': '1' } ], 'documents': [ { 'documentId': '2' } ] } }"))
		    .on("POST /peer/register", xq("{ 'content': { 'peerId': '1' } }"))
		    .on("POST /peer/unregister", xq("{ 'content': { 'peerId': '1' } }"))
		    .on("GET /artifacts/artid/content", "abcde", ContentType.APPLICATION_OCTET_STREAM)
		    .on("GET /documents/docid/content", "abcd", ContentType.APPLICATION_OCTET_STREAM)
		    .errorOn("GET /artifacts/artnotallowed/content", 403, "Forbidden")
		    .applyTo(client);
		assertNull(client.ping());
		Solution sol = (Solution)client.getSolution("someid");
		assertNotNull(sol.getSolutionId());
		assertNotNull(sol.getPicture());
		assertNotNull(sol.getRevisions());
		List<MLPSolution> sols = client.getSolutions("somecatid");
		assertEquals(2, sols.size());
		client.getSolutions("emptyanswer");
		assertEquals(2, client.getCatalogs().size());
		assertEquals(1, client.getPeers().size());
		assertEquals(1, client.getSolutionRevisions("solid").size());
		assertEquals(1, client.getSolutionRevisions("solid").size());
		assertEquals(1, client.getDocuments("revid", "catid").size());
		assertEquals(1, client.getArtifacts("solid", "revid").size());
		assertEquals(1, ((SolutionRevision)client.getSolutionRevision("solid", "revid", null)).getArtifacts().size());
		assertEquals(1, ((SolutionRevision)client.getSolutionRevision("solid", "revid", "catid")).getDocuments().size());
		assertNotNull(client.register());
		assertNotNull(client.unregister());
		byte buf[] = new byte[6];
		InputStream is = client.getArtifactContent("artid");
		assertEquals(5, is.read(buf));
		is.close();
		is = client.getDocumentContent("docid");
		assertEquals(4, is.read(buf));
		is.close();
		try {
			client.getDocumentContent("nosuchdocid");
			fail();
		} catch (ResourceAccessException rse) {
			// expected case
		}
		try {
			client.getArtifactContent("artnotallowed");
			fail();
		} catch (Forbidden un) {
			// expected case
		}
	}

	private static class UploadTest extends ClientBase {
		public UploadTest() throws Exception {
			super("http://example", getConfig("acumosa"), null, null);
		}
		public void up(String param, InputStream data) {
			upload("/something/{someparam}", data, param);
		}
	}

	@Test
	public void testUpload() throws Exception {
		UploadTest client = new UploadTest();
		assertNotNull(client);
		(new ClientMocking())
		    .errorOnNoAuth(401, "Unauthorized")
		    .errorOnBadAuth("acumosa", "acumosa", 403, "Forbidden")
		    .on("PUT /something/paramvalue", "")
		    .applyTo(client);
		client.up("paramvalue", new ByteArrayInputStream("hello".getBytes()));
	}

	@Test
	public void testURLEncoding() throws Exception {
		UriTemplateHandler uth = ClientBase.buildRestTemplate("https://hostname:999/funky%+ url//", new ClientConfig(), null, null).getUriTemplateHandler();
		assertEquals("https://hostname:999/funky%25+%20url/x%20%40%25%2BA%7B%7D%20x/y%20B%2FC%20y", uth.expand("/x {var1} x/y {var2} y", "@%+A{}", "B/C").toString());
	}
}

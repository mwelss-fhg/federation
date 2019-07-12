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
package org.acumos.federation.gateway;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.HttpClientErrorException.Forbidden;
import org.springframework.web.client.HttpClientErrorException.NotFound;

import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.client.CommonDataServiceRestClientImpl;

import org.acumos.federation.client.FederationClient;
import org.acumos.federation.client.GatewayClient;
import org.acumos.federation.client.ClientBase;
import org.acumos.federation.client.config.ClientConfig;
import org.acumos.federation.client.config.BasicAuthConfig;
import org.acumos.federation.client.config.TlsConfig;

import org.acumos.federation.client.test.ClientMocking;
import static org.acumos.federation.client.test.ClientMocking.getConfig;
import static org.acumos.federation.client.test.ClientMocking.xq;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes=GatewayServer.class)
@SpringBootTest(
    classes = Application.class,
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
	"spring.main.allow-bean-definition-overriding=true",
	"local.ssl.key-store=classpath:acumosa.pkcs12",
	"local.ssl.key-store-password=acumosa",
	"local.ssl.key-store-type=PKCS12",
	"local.ssl.trust-store=classpath:acumosTrustStore.jks",
	"local.ssl.trust-store-password=acumos",
	"nexus.group-id=nxsgrpid",
	"nexus.name-separator=,",
	"docker.registry-url=someregistry:9999",
	"federation.operator=defuserid"
    }
)
public class GatewayControllerTest {
	@LocalServerPort
	private int port;

	@Autowired
	private ServerConfig local;

	@MockBean
	private Clients clients;

	private CountDownLatch steps;

	private final Consumer<ClientMocking.RequestInfo> count = x -> this.steps.countDown();

	private SimulatedDockerClient docker;

	static ClientConfig anonConfig() {
		ClientConfig ret = getConfig("bogus");
		ret.getSsl().setKeyStore(null);
		ret.setCreds(null);
		return ret;
	}

	private static class RawAnonClient extends ClientBase {
		public RawAnonClient(String url) throws Exception {
			super(url, anonConfig(), null, null);
		}

		public byte[] get(String uri) {
			return handle(uri, HttpMethod.GET, new ParameterizedTypeReference<byte[]>(){});
		}
	}

	@Before
	public void init() throws Exception {
		ICommonDataServiceRestClient cdsClient = CommonDataServiceRestClientImpl.getInstance("http://cds:999", ClientBase.buildRestTemplate("http://cds:999", new ClientConfig(), null, null));

		(new ClientMocking())
		    .on("GET /peer/search?self=true&subjectName=gateway.acumosa.org&_j=a&page=0&size=100", xq("{ 'content': [ {'peerId': '1', 'subjectName': 'gateway.acumosa.org', 'statusCode': 'AC', 'self': true } ], 'last': true, 'number': 0, 'size': 100, 'numberOfElements': 1 }"))
		    .on("GET /peer/somepeer", xq("{ 'peerId': 'somepeer', 'apiUrl': 'https://somepeer.org:999'}"))
		    .on("GET /peer/unknownpeer", "")
		    .on("GET /peer/search?subjectName=gateway.acumosa.org&_j=a&page=0&size=100", xq("{ 'content': [ {'peerId': 'acumosa', 'subjectName': 'gateway.acumosa.org', 'statusCode': 'AC', 'self': true } ], 'last': true, 'number': 0, 'size': 100, 'numberOfElements': 1 }"))
		    .on("GET /peer/search?subjectName=gateway.acumosb.org&_j=a&page=0&size=100", xq("{ 'content': [ {'peerId': 'acumosb', 'subjectName': 'gateway.acumosb.org', 'statusCode': 'AC', 'self': false } ], 'last': true, 'number': 0, 'size': 100, 'numberOfElements': 1 }"))
		    .on("GET /peer/search?subjectName=gateway.acumosc.org&_j=a&page=0&size=100", xq("{ 'content': [ ], 'last': true, 'number': 0, 'size': 100, 'numberOfElements': 0 }"))
		    .on("GET /peer/sub/999", xq("{ 'subId': 999, 'peerId': 'somepeer', 'selector': '{ \\'catalogId\\': \\'somecatalog\\' } ' }"))
		    .on("GET /peer/sub/998", "")
		    .on("GET /peer/sub/997", xq("{ 'subId': 997, 'peerId': 'someotherpeer' }"))
		    .on("GET /peer/sub/992", xq("{ 'subId': 992, 'peerId': 'somepeer', 'selector': '{ \\'catalogId\\': [ \\'firstcatalog\\', \\'secondcatalog\\' ] }', 'refreshInterval': 3600, 'userId': 'someUser' }"))
		    .on("GET /peer/sub/993", xq("{ 'subId': 993, 'peerId': 'somepeer', 'selector': '{ \\'catalogId\\': true }', 'refreshInterval': 3600 }"))
		    .on("GET /peer/sub/994", xq("{ 'subId': 994, 'peerId': 'somepeer', 'selector': '{ \\'catalogId\\': [ \\'x\\', true ] }', 'refreshInterval': 3600 }"))
		    .on("GET /peer/sub/995", xq("{ 'subId': 995, 'peerId': 'somepeer', 'selector': '}', 'refreshInterval': 3600 }"))
		    .on("GET /peer/sub/996", xq("{ 'subId': 996, 'peerId': 'somepeer', 'selector': '{}', 'refreshInterval': 3600 }"))
		    .on("PUT /peer/sub/999", "", count)
		    .on("GET /catalog/solution?ctlg=somecatalog&page=0&size=100", xq("{ 'content': [], 'last': true, 'number': 0, 'size': 100, 'numberOfElements': 0 }"))
		    .on("GET /catalog?page=0&size=100", xq("{ 'content': [], 'last': true, 'number': 0, 'size': 100, 'numberOfElements': 0 }"))
		    .on("POST /catalog", "{}", count)
		    .on("GET /solution/somesolution", "")
		    .on("GET /solution/somesolution/revision", "[]")
		    .on("POST /solution", "{}", count)
		    .on("POST /catalog/somecatalog/solution/somesolution", "", count)
		    .on("PUT /solution/somesolution/pic", "", count)
		    .on("GET /solution/ignored/revision/revid1", "")
		    .on("POST /solution/somesolution/revision", xq("{ 'solutionId': 'somesolution', 'revisionId': 'revid1' }"), count)
		    .on("POST /revision/revid1/catalog/somecatalog/descr", "", count)
		    .on("GET /artifact/artid1", "")
		    .on("POST /artifact", "", count)
		    .on("POST /revision/revid1/artifact/artid1", "", count)
		    .on("GET /document/docid1", "")
		    .on("POST /document", "", count)
		    .on("POST /revision/revid1/catalog/somecatalog/document/docid1", "", count)
		    .on("GET /catalog/solution?ctlg=firstcatalog&page=0&size=100", xq("{ 'content': [ ], 'last': true, 'number': 0, 'size': 100, 'numberOfElements': 0 }"))
		    .on("GET /catalog/solution?ctlg=secondcatalog&page=0&size=100", xq("{ 'content': [ { 'solutionId': 'cat2soln' } ], 'last': true, 'number': 1, 'size': 100, 'numberOfElements': 1 }"))
		    .on("GET /solution/cat2soln", xq("{ 'solutionId': 'cat2soln' }"))
		    .on("GET /solution/cat2soln/revision", xq("[ { 'revisionId': 'cat2rev', 'solutionId': 'cat2sol' }, { 'revisionId': 'cat2rev2', 'solutionId': 'cat2sol' } ]"))
		    .on("GET /solution/cat2soln/pic", "asdf")
		    .on("GET /solution/ignored/revision/cat2rev", xq("{ 'revisionId': 'cat2rev', 'solutionId': 'cat2sol' }"))
		    .on("GET /solution/ignored/revision/cat2rev2", xq("{ 'revisionId': 'cat2rev2', 'solutionId': 'cat2sol' }"))
		    .on("GET /revision/cat2rev/artifact", xq("[ { 'artifactId': 'artid2', 'filename': 'artfile2.arttype', 'version': 'artversionA' } ]"))
		    .on("GET /revision/cat2rev2/artifact", "[]")
		    .on("GET /revision/cat2rev/catalog/secondcatalog/descr", xq("{ 'revisionId': 'cat2rev', 'catalogId': 'secondcatalog', 'description': 'old description' }"))
		    .on("GET /revision/cat2rev2/catalog/secondcatalog/descr", xq("{ 'revisionId': 'cat2rev2', 'catalogId': 'secondcatalog', 'description': 'description A' }"))
		    .on("GET /revision/cat2rev/catalog/secondcatalog/document", "[]")
		    .on("GET /document/docid2", xq("{ 'documentId': 'docid2', 'filename': 'docfile2.doctype', 'version': 'docversionA' }"))
		    .on("GET /artifact/artid2", xq("{ 'artifactId': 'artid2', 'filename': 'artfile2.arttype', 'version': 'artversionA' }"))
		    .on("GET /revision/cat2rev2/catalog/secondcatalog/document", "[]")
		    .on("DELETE /revision/cat2rev/catalog/secondcatalog/descr", "")
		    .on("PUT /artifact/artid2", "")
		    .on("PUT /document/docid2", "")
		    .on("POST /revision/cat2rev/catalog/secondcatalog/document/docid2", "")
		    .on("PUT /solution/cat2soln/revision/cat2rev", "")
		    .on("PUT /revision/cat2rev2/catalog/secondcatalog/descr", "")
		    .on("POST /solution/cat2soln/tag/tag1", "")
		    .on("PUT /solution/cat2soln", "")
		    .on("PUT /peer/sub/992", "", count)
		    .applyTo(cdsClient);
		when(clients.getCDSClient()).thenReturn(cdsClient);

		NexusClient nexusClient = new NexusClient("https://nexus:999", new ClientConfig());
		(new ClientMocking())
		    .on("PUT /nxsgrpid,somesolution/somefile/someversion/somefile-someversion.type", "")
		    .on("PUT /nxsgrpid,somesolution/docfile/na/docfile-na.doctype", "")
		    .on("PUT /nxsgrpid,cat2soln/docfile2/na/docfile2-na.doctype", "")
		    .on("PUT /nxsgrpid,cat2soln/artfile2/artversion2B/artfile2-artversion2B.arttype", "")
		    .applyTo(nexusClient);
		when(clients.getNexusClient()).thenReturn(nexusClient);

		FederationClient fedClient = new FederationClient("https://peer:999", new ClientConfig());
		(new ClientMocking())
		    .on("GET /catalogs", xq("{ 'content': [ {}, {} ]}"))
		    .on("GET /ping", xq("{ 'content': {}}"))
		    .on("GET /peers", xq("{ 'content': [{}, {}]}"))
		    .on("POST /peer/register", xq("{ 'content': {}}"))
		    .on("GET /solutions?catalogId=somecatalog", xq("{ 'content': [ { 'solutionId': 'somesolution' } ]}"))
		    .on("GET /solutions/somesolution", xq("{ 'content': { 'picture': 'YXNkZg==', 'revisions': [ { 'revisionId': 'revid1' } ] }}"))
		    .on("GET /solutions/somesolution/revisions/revid1?catalogId=somecatalog", xq("{ 'content': { 'solutionId': 'somesolution', 'revisionId': 'revid1', 'documents': [ { 'documentId': 'docid1', 'filename': 'docfile.doctype', 'version': 'docversion' } ], 'artifacts': [ { 'artifactId': 'artid1', 'name': 'somename', 'filename': 'someimage', 'version': 'someversion', 'artifactTypeCode': 'DI', 'description': 'thisimage:thistag' } ], 'revCatDescription': { 'revisionId': 'revid1', 'catalogId': 'somecatalog', 'description': 'some description' }}}"))
		    .on("GET /artifacts/artid1/content", "Artifact Content")
		    .on("GET /artifacts/artid2/content", "Artifact Content 2")
		    .on("GET /documents/docid1/content", "Document Content")
		    .on("GET /documents/docid2/content", "Document Content 2")
		    .on("GET /solutions?catalogId=firstcatalog", xq("{ 'content': [ ]}"))
		    .on("GET /solutions?catalogId=secondcatalog", xq("{ 'content': [ { 'solutionId': 'cat2soln' } ]}"))
		    .on("GET /solutions/cat2soln", xq("{ 'content': { 'solutionId': 'cat2soln', 'picture': 'YXNkZg==', 'revisions': [ { 'revisionId': 'cat2rev' }, { 'revisionId': 'cat2rev2' } ], 'tags': [ { 'tag': 'tag1' } ] }}"))
		    .on("GET /solutions/cat2soln/revisions/cat2rev?catalogId=secondcatalog", xq("{ 'content': { 'solutionId': 'cat2soln', 'revisionId': 'cat2rev', 'documents': [ { 'documentId': 'docid2', 'filename': 'docfile2.doctype', 'version': 'docversionB' } ], 'artifacts': [ { 'artifactId': 'artid2', 'filename': 'artfile2.arttype', 'version': 'artversion2B' } ] }}"))
		    .on("GET /solutions/cat2soln/revisions/cat2rev2?catalogId=secondcatalog", xq("{ 'content': { 'solutionId': 'cat2soln', 'revisionId': 'cat2rev2', 'revCatDescription': { 'catalogId': 'secondcatalog', 'revisionId': 'cat2rev2', 'description': 'description B' }, 'documents': [  ], 'artifacts': [ ] }}"))
		    .applyTo(fedClient);
		when(clients.getFederationClient(any(String.class))).thenReturn(fedClient);

		docker = new SimulatedDockerClient();
		when(clients.getDockerClient()).thenReturn(docker.getClient());
	}

	@Test
	public void testConfig() throws Exception {
		assertEquals("acumosa", local.getSsl().getKeyStorePassword());

		GatewayClient self = new GatewayClient("https://localhost:" + port, getConfig("acumosa"));
		GatewayClient known = new GatewayClient("https://localhost:" + port, getConfig("acumosb"));
		GatewayClient unknown = new GatewayClient("https://localhost:" + port, getConfig("acumosc"));
		assertNotNull(self.ping("somepeer"));
		assertNotNull(self.register("somepeer"));
		assertNotNull(self.getPeers("somepeer"));
		try {
			known.ping("somepeer");
			fail();
		} catch (Forbidden ux) {
			// expected case
		}
		try {
			unknown.ping("somepeer");
			fail();
		} catch (Forbidden ux) {
			// expected case
		}
		try {
			self.ping("unknownpeer");
			fail();
		} catch (NotFound nf) {
			// expected case
		}
		assertEquals(2, self.getCatalogs("somepeer").size());
		assertEquals(1, self.getSolutions("somepeer", "somecatalog").size());
		assertNotNull(self.getSolution("somepeer", "somesolution"));
		try {
			self.triggerPeerSubscription("unknownpeer", 999);
			fail();
		} catch (NotFound nf) {
			// expected case
		}
		try {
			self.triggerPeerSubscription("somepeer", 998);
			fail();
		} catch (NotFound nf) {
			// expected case
		}
		try {
			self.triggerPeerSubscription("somepeer", 997);
			fail();
		} catch (NotFound nf) {
			// expected case
		}
		docker.clearImages();
		docker.addImage("imageid1", "tagA:1", "tagB:2");
		docker.addImage("imageid2", "tagX:1", "thisimage:thistag");
		steps = new CountDownLatch(12 + 1);
		self.triggerPeerSubscription("somepeer", 992);
		self.triggerPeerSubscription("somepeer", 993);
		self.triggerPeerSubscription("somepeer", 994);
		self.triggerPeerSubscription("somepeer", 995);
		self.triggerPeerSubscription("somepeer", 996);
		self.triggerPeerSubscription("somepeer", 999);
		steps.await(2, TimeUnit.SECONDS);
		assertEquals("Incomplete steps remain", 0, steps.getCount() - 1);
	}


	@Test
	public void testSwagger() throws Exception {
		RawAnonClient rac = new RawAnonClient("https://localhost:" + port);
		rac.get("/swagger-ui.html");
		rac.get("/v2/api-docs");
	}
}

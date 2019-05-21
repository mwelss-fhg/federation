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

import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import org.springframework.web.client.HttpClientErrorException.Conflict;
import org.springframework.web.client.HttpClientErrorException.Forbidden;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import org.springframework.web.client.HttpServerErrorException.InternalServerError;

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
@ContextConfiguration(classes=FederationServer.class)
@SpringBootTest(
    classes = Application.class,
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
	"spring.main.allow-bean-definition-overriding=true",
	"federation.ssl.key-store=classpath:acumosa.pkcs12",
	"federation.ssl.key-store-password=acumosa",
	"federation.ssl.key-store-type=PKCS12",
	"federation.ssl.trust-store=classpath:acumosTrustStore.jks",
	"federation.ssl.trust-store-password=acumos",
	"federation.address=localhost",
	"federation.server.port=0",
	"federation.registration-enabled=true",
	"cdms.client.url=http://dummy.org:999",
	"cdms.client.username=dummyuser",
	"cdms.client.password=dummypass",
	"nexus.url=http://dummy.org:1234"
    }
)
public class FederationControllerTest {
	@LocalServerPort
	private int port;

	@Autowired
	private ServiceConfig cdmsConfig;

	@Autowired
	private NexusConfig nexusConfig;

	@Autowired
	private PeerService peerService;

	@MockBean
	private Clients clients;

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
		String url = cdmsConfig.getUrl();
		ClientConfig ccc = new ClientConfig();
		ccc.setCreds(cdmsConfig);
		ICommonDataServiceRestClient cdsClient = CommonDataServiceRestClientImpl.getInstance(url, ClientBase.buildRestTemplate(url, ccc, null, null));

		(new ClientMocking())
		    .on("GET /peer/search?self=true&subjectName=gateway.acumosa.org&_j=a&page=0&size=100", xq("{ 'content': [ {'peerId': '1', 'subjectName': 'gateway.acumosa.org', 'statusCode': 'AC', 'self': true, 'local': true } ], 'last': true, 'number': 0, 'size': 100, 'numberOfElements': 1 }"))
		    .on("GET /peer/search?subjectName=gateway.acumosa.org&_j=a&page=0&size=100", xq("{ 'content': [ {'peerId': '1', 'subjectName': 'gateway.acumosa.org', 'statusCode': 'AC', 'self': true } ], 'last': true, 'number': 0, 'size': 100, 'numberOfElements': 1 }"))
		    .on("GET /peer/search?subjectName=gateway.acumosb.org&_j=a&page=0&size=100", xq("{ 'content': [ {'peerId': '2', 'subjectName': 'gateway.acumosb.org', 'statusCode': 'XX', 'self': false } ], 'last': true, 'number': 0, 'size': 100, 'numberOfElements': 1 }"))
		    .on("GET /peer/search?self=true&subjectName=Bad=?#/.CN%Value&_j=a&page=0&size=100", xq("{}"))
		    .on("GET /peer/2", xq("{'peerId': '2', 'subjectName': 'gateway.acumosb.org', 'statusCode': 'RQ', 'self': false }"))
		    .on("PUT /peer/2", "")
		    .on("GET /peer/search?subjectName=gateway.acumosc.org&_j=a&page=0&size=100", xq("{ 'content': [ ], 'last': true, 'number': 0, 'size': 100, 'numberOfElements': 0 }"))
		    .on("POST /peer", xq("{}"))
		    .on("GET /catalog/search?accessTypeCode=PB&_j=a&page=0&size=100", xq("{ 'content': [ { 'catalogId': '1' }, { 'catalogId': '2' } ], 'last': true, 'number': 2, 'size': 100, 'numberOfElements': 2 }"))
		    .on("GET /access/peer/1/catalog", xq("[ '2', '7', '8' ]"))
		    .on("GET /peer?page=0&size=100", xq("{ 'content': [ { 'peerId': '1' }, { 'peerId': '2' } ], 'last': true, 'number': 2, 'size': 100, 'numberOfElements': 2 }"))
		    .on("GET /catalog/1/solution/count", xq("{ 'count': 1 }"))
		    .on("GET /catalog/2/solution/count", xq("{ 'count': 2 }"))
		    .on("GET /catalog/7", xq("{ 'catalogId': '7' }"))
		    .on("GET /catalog/7/solution/count", xq("{ 'count': 3 }"))
		    .on("GET /catalog/8", "")
		    .on("GET /peer/search?self=true&subjectName=No.Such.Peer&_j=a&page=0&size=100", xq("{ 'content': [], 'last': true, 'number': 0, 'size': 100, 'numberOfElements': 0 }"))
		    .on("GET /access/peer/1/catalog/somecatid", xq("{ 'count': '1' }"))
		    .on("GET /access/peer/1/catalog/badcatid", xq("{ 'count': '0' }"))
		    .on("GET /catalog/solution?ctlg=somecatid&page=0&size=100", xq("{ 'content': [ { 'solutionId': 'somesolid' }, { 'solutionId': 'othersolid', 'origin': 'https://someoneelse.org:1234/solution/othersolid' } ], 'last': true, 'number': 2, 'size': 100, 'numberOfElements': 2 }"))
		    .on("GET /access/peer/1/solution/somesolid", xq("{ 'count': '1' }"))
		    .on("GET /access/peer/1/solution/badsolid", xq("{ 'count': '0' }"))
		    .on("GET /access/peer/1/solution/norevssolid", xq("{ 'count': '1' }"))
		    .on("GET /solution/somesolid", xq("{ 'solutionId': 'somesolid' }"))
		    .on("GET /solution/norevssolid", xq("{ 'solutionId': 'norevssolid' }"))
		    .on("GET /solution/somesolid/revision", xq("[ { 'solutionId': 'somesolid', 'revisionId': 'somerevid' }, { 'solutionId': 'somesolid', 'revisionId': 'otherrevid' } ]"))
		    .on("GET /solution/norevssolid/revision", "[]")
		    .on("GET /solution/somesolid/pic", "")
		    .on("GET /solution/ignored/revision/somerevid", xq("{ 'solutionId': 'somesolid', 'revisionId': 'somerevid' }"))
		    .on("GET /solution/ignored/revision/badrevid", xq("{ 'solutionId': 'badsolid', 'revisionId': 'badrevid' }"))
		    .on("GET /solution/ignored/revision/norevid", "")
		    .on("GET /revision/somerevid/artifact", xq("[ { 'artifactId': 'someartid' }, { 'artifactId': 'otherartid', 'uri': 'dockerregistry:997/a/b', 'artifactTypeCode': 'DI' }, { 'artifactId': 'nexusartid', 'uri': 'a/b/c.x' } ]"))
		    .on("GET /revision/badrevid/artifact", xq("[ { 'artifactId': 'badartid' }, { 'artifactId': 'otherbadartid' } ]"))
		    .on("GET /revision/somerevid/catalog/somecatid/descr", xq("{ 'revisionId': 'somerevid', 'catalogId': 'somecatid', 'description': 'Etc., etc., etc.' }"))
		    .on("GET /revision/somerevid/catalog/somecatid/document", xq("[ { 'documentId': 'somedocid' }, { 'documentId': 'otherdocid' } ]"))
		    .on("GET /artifact/someartid", xq("{ 'artifactId': 'someartid', 'uri': 'a/b/c/d' }"))
		    .on("GET /artifact/dockerartid", xq("{ 'artifactId': 'someartid', 'uri': 'dockerregistry:1234/a/b:1.0', 'artifactTypeCode': 'DI' }"))
		    .on("GET /document/somedocid", xq("{ 'documentId': 'somedocid', 'uri': 'd/c/b/a' }"))
		    .on("GET /peer/1/sub", "[]")
		    .on("GET /peer/2/sub", "[]")
		    .on("GET /solution/ignored/revision/altrevid", xq("{ 'solutionId': 'somesolid', 'revisionId': 'altrevid' }"))
		    .on("GET /revision/altrevid/artifact", xq("[ { 'artifactId': 'altart1', 'artifactTypeCode': 'DI', 'version': 'aa1ver', 'uri': 'host:999/xxx/stuff:aa1ver' }, { 'artifactId': 'altart2', 'artifactTypeCode': 'DI', 'version': 'aa2ver', 'uri': 'someimagename' }]"))
		    .on("GET /revision/altrevid/catalog/somecatid/document", xq("[ { 'documentId': 'altdoc1', 'version': 'ad1ver', 'uri': 'somepath/ad1name/ua/ad1name-ua.ad1type' }, { 'documentId': 'altdoc2', 'version': 'ad2ver', 'uri': 'somepath/ad2name/ua/ad2name.ad2type' }]"))
		    .applyTo(cdsClient);

		when(clients.getCDSClient()).thenReturn(cdsClient);

		ClientConfig ncc = new ClientConfig();
		ncc.setCreds(nexusConfig);
		NexusClient nexusClient = new NexusClient(nexusConfig.getUrl(), ncc);

		(new ClientMocking())
		    .on("GET /a/b/c/d", "vwxyz")
		    .on("GET /d/c/b/a", "wxyz")
		    .applyTo(nexusClient);
		when(clients.getNexusClient()).thenReturn(nexusClient);

		docker = new SimulatedDockerClient();
		docker.setSaveResult("abcdefg".getBytes());
		when(clients.getDockerClient()).thenReturn(docker.getClient());
	}

	@Test
	public void testConfig() throws Exception {

		FederationClient self = new FederationClient("https://localhost:" + port, getConfig("acumosa"));
		FederationClient known = new FederationClient("https://localhost:" + port, getConfig("acumosb"));
		FederationClient unknown = new FederationClient("https://localhost:" + port, getConfig("acumosc"));
		assertNotNull(unknown.register());
		try {
			self.register();
			fail();
		} catch (Conflict c) {
			// expected case
		}
		assertNotNull(known.unregister());
		assertNotNull(self.ping());
		assertEquals(2, self.getPeers().size());
		try {
			unknown.unregister();
			fail();
		} catch (Forbidden ux) {
			// expected case
		}
		try {
			known.ping();
			fail();
		} catch (Forbidden ux) {
			// expected case
		}
		assertEquals(3, self.getCatalogs().size());
		assertNull(peerService.getSelf("No.Such.Peer"));
		assertEquals(2, self.getSolutions("somecatid").size());
		try {
			self.getSolutions("badcatid");
			fail();
		} catch (NotFound nf) {
			// expected case
		}
		assertNotNull(self.getSolution("somesolid"));
		try {
			self.getSolution("badsolid");
			fail();
		} catch (NotFound nf) {
			// expected case
		}
		try {
			self.getSolution("norevssolid");
			fail();
		} catch (NotFound nf) {
			// expected case
		}
		assertEquals(2, self.getSolutionRevisions("somesolid").size());
		try {
			self.getSolutionRevisions("badsolid");
			fail();
		} catch (NotFound nf) {
			// expected case
		}
		try {
			self.getSolutionRevisions("norevssolid");
			fail();
		} catch (NotFound nf) {
			// expected case
		}
		assertNotNull(self.getSolutionRevision("somesolid", "somerevid", "somecatid"));
		try {
			self.getSolutionRevision("somesolid", "badrevid", "badcatid");
			fail();
		} catch (NotFound nf) {
			// expected case
		}
		try {
			self.getSolutionRevision("somesolid", "norevid", null);
			fail();
		} catch (NotFound nf) {
			// expected case
		}
		assertEquals(3, self.getArtifacts("somesolid", "somerevid").size());
		try {
			self.getArtifacts("somesolid", "badrevid");
			fail();
		} catch (NotFound nf) {
			// expected case
		}
		try {
			self.getArtifacts("somesolid", "norevid");
			fail();
		} catch (NotFound nf) {
			// expected case
		}
		byte[] buf = new byte[1000];
		try (InputStream is = self.getArtifactContent("someartid")) {
			assertEquals(5, is.read(buf));
		}
		try (InputStream is = self.getArtifactContent("dockerartid")) {
			assertEquals(7, is.read(buf));
		}
		docker.setDoPullTimeout(true);
		try {
			self.getArtifactContent("dockerartid");
			fail();
		} catch (InternalServerError ise) {
			// expected case
		}
		assertEquals(2, self.getDocuments("somerevid", "somecatid").size());
		try (InputStream is = self.getDocumentContent("somedocid")) {
			assertEquals(4, is.read(buf));
		}
		try {
			self.getDocuments("somerevid", "badcatid");
			fail();
		} catch (NotFound nf) {
			// expected case
		}
		assertNotNull(self.getDocuments("altrevid", "somecatid"));
		assertNotNull(self.getArtifacts("somesolid", "altrevid"));
	}

	@Test
	public void testSwagger() throws Exception {
		RawAnonClient rac = new RawAnonClient("https://localhost:" + port);
		rac.get("/swagger-ui.html");
		rac.get("/v2/api-docs");
	}
}

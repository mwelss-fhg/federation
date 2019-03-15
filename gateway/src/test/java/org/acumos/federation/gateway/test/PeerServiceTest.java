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

import java.util.List;

import org.acumos.cds.domain.MLPPeer;
import org.acumos.federation.gateway.security.Peer;
import org.acumos.federation.gateway.security.Role;
import org.acumos.federation.gateway.service.PeerService;
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
									"federation.ssl.key-password=acumosa",
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
public class PeerServiceTest extends ServiceTest {

	@Autowired
	private PeerService peerService;

	protected void initMockResponses() {
		registerMockResponse("GET /ccds/peer/search?self=true&_j=a&page=0&size=100", MockResponse.success("mockCDSPeerSearchSelfResponse.json"));
		registerMockResponse("GET /ccds/peer?page=0&size=100", MockResponse.success("mockCDSPeerSearchAllResponse.json"));
		registerMockResponse("GET /ccds/peer/search?subjectName=gateway.acumosb.org&_j=a", MockResponse.success("mockCDSPeerSearchResponse.json"));
		registerMockResponse("GET /ccds/peer/search?subjectName=gateway.acumosc.org&_j=a", MockResponse.success("mockCDSSearchEmptyResponse.json"));
		registerMockResponse("PUT /ccds/peer/b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0", MockResponse.success("mockCDSPeerUpdateResponse.json"));
		registerMockResponse("POST /ccds/peer", MockResponse.success("mockCDSPeerCreateResponse.json"));
		registerMockResponse("GET /ccds/peer/b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0", MockResponse.success("mockCDSPeerResponse.json"));
		registerMockResponse("GET /ccds/code/pair/PEER_STATUS", MockResponse.success("mockCDSPeerStatusResponse.json"));
	}


	/**
	 * The gateway behaviour is triggered by the availability of other solutions
	 * in a peer, as provided by the federation client.  
	 */
	@Test
	public void testPeerService() {

		try {
			ServiceContext selfService = 
				ServiceContext.forPeer(new Peer(new MLPPeer("acumosb", "gateway.acumosb.org", "https://gateway.acumosb.org:9084", false, false, "admin@acumosab.org", "AC"), Role.SELF));

			List<MLPPeer> peers = peerService.getPeers(selfService);
			assertTrue("Unexpected all peers response", peers.size() == 2);

			List<MLPPeer> peersn = peerService.getPeerBySubjectName("gateway.acumosb.org");
			assertTrue("Expected one peer to be found", peersn.size() == 1);
			peerService.getPeerById(peersn.get(0).getPeerId());

			try {
				peerService.registerPeer(new MLPPeer("acumosc", "gateway.acumosc.org", "https://gateway.acumosc.org:9084", false, false, "admin@acumosc.org", "AC"));
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

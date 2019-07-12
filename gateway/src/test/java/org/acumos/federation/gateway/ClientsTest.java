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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.runner.RunWith;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

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
	"nexus.url=http://nexus.example.org",
	"docker.host=tcp://localhost:999",
	"cdms.client.url=http://cdms.example.org"
    }
)
public class ClientsTest {
	/*
	 * Implementation note:
	 *
	 * Since "Clients" is the common hub for outbound activity from the
	 * Federation Gateway software, and since the destinations for that
	 * activity don't exist during automated testing, Clients is mocked
	 * out in all the other tests.  Without those external systems, there
	 * still isn't a lot, that can actually be tested, here, but
	 * this does at least exercise the code by insuring that all the
	 * methods get invoked and that, for the clients that are cached,
	 * the methods get invoked twice and the answers tested for equality.
	 */

	@Autowired
	private Clients clients;

	@Test
	public void testClients() throws Exception {
		assertNotNull(clients.getFederationClient("https://somepeer.example.org"));
		assertEquals(clients.getCDSClient(), clients.getCDSClient());
		assertEquals(clients.getNexusClient(), clients.getNexusClient());
		assertNotNull(clients.getDockerClient());
	}
}

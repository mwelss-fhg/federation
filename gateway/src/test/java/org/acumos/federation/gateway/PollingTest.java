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

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.when;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.ContextConfiguration;

import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.client.CommonDataServiceRestClientImpl;

import org.acumos.federation.client.FederationClient;
import org.acumos.federation.client.ClientBase;
import org.acumos.federation.client.config.ClientConfig;

import org.acumos.federation.client.test.ClientMocking;
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
	"peer.jobchecker.interval=2",
	"federation.operator=defuserid"
    }
)
public class PollingTest {
	@MockBean
	private Clients clients;

	private CountDownLatch steps;

	private final Consumer<ClientMocking.RequestInfo> count = x -> this.steps.countDown();

	private ICommonDataServiceRestClient cdsClient;

	@Before
	public void init() throws Exception {
		cdsClient = CommonDataServiceRestClientImpl.getInstance("http://cds:999", ClientBase.buildRestTemplate("http://cds:999", new ClientConfig(), null, null));
		(new ClientMocking())
		    .on("GET /peer?page=0&size=100", xq("{ 'content': [ { 'peerId': '1', 'self': true } ], 'last': true, 'number': 1, 'size': 100, 'numberOfElements': 1 }"))
		    .applyTo(cdsClient);
		when(clients.getCDSClient()).thenReturn(cdsClient);

	}

	@Test
	public void testPolling() throws Exception {
		steps = new CountDownLatch(6);
		(new ClientMocking())
		    .on("GET /peer?page=0&size=100", xq("{ 'content': [ { 'peerId': '1', 'self': true }, { 'peerId': '2' } ], 'last': true, 'number': 2, 'size': 100, 'numberOfElements': 2 }"), count)
		    .on("GET /peer/2/sub", xq("[ { 'subId': 1, 'peerId': '2', 'selector': '{ \\'catalogId\\': [] }'}, { 'subId': 2, 'peerId': '2', 'selector': '{ \\'catalogId\\': [] }', 'refreshInterval': 3600}, { 'subId': 3, 'peerId': '2', 'selector': '{ \\'catalogId\\': [] }', 'refreshInterval': 0}, { 'subId': 4, 'peerId': '2', 'selector': '{ \\'catalogId\\': [] }', 'refreshInterval': 0, 'processed': '2019-01-01T00:00:00Z' }]"), count)
		    .on("GET /peer/sub/2", xq("{ 'subId': 2, 'peerId': '2', 'selector': '{ \\'catalogId\\': [] }', 'refreshInterval': 3600}"), count)
		    .on("GET /peer/sub/3", xq("{ 'subId': 3, 'peerId': '2', 'selector': '{ \\'catalogId\\': [] }', 'refreshInterval': 0}"), count)
		    .on("PUT /peer/sub/2", "", count)
		    .on("PUT /peer/sub/3", "", count)
		    .applyTo(cdsClient);
		steps.await(6, TimeUnit.SECONDS);
		assertEquals("Incomplete steps remain", 0, steps.getCount());
		steps = new CountDownLatch(4);
		(new ClientMocking())
		    .on("GET /peer?page=0&size=100", xq("{ 'content': [ { 'peerId': '1', 'self': true }, { 'peerId': '2' } ], 'last': true, 'number': 2, 'size': 100, 'numberOfElements': 2 }"), count)
		    .on("GET /peer/2/sub", xq("[ { 'subId': 1, 'peerId': '2', 'selector': '{ \\'catalogId\\': []}', 'refreshInterval': 1800 }, { 'subId': 2, 'peerId': '2', 'selector': '{ \\'catalogId\\': [] }', 'refreshInterval': 3600}, { 'subId': 3, 'peerId': '2', 'selector': '{ \\'catalogId\\': [] }' } ]"), count)
		    .on("GET /peer/sub/1", xq("{ 'subId': 1, 'peerId': '2', 'selector': '{ \\'catalogId\\': [] }', 'refreshInterval': 3600}"), count)
		    .on("PUT /peer/sub/1", "", count)
		    .applyTo(cdsClient);
		steps.await(3, TimeUnit.SECONDS);
		assertEquals("Incomplete steps remain", 0, steps.getCount());
		steps = new CountDownLatch(2);
		(new ClientMocking())
		    .on("GET /peer?page=0&size=100", xq("{ 'content': [ { 'peerId': '1', 'self': true }, { 'peerId': '2' } ], 'last': true, 'number': 2, 'size': 100, 'numberOfElements': 2 }"), count)
		    .on("GET /peer/2/sub", xq("[ { 'subId': 2, 'peerId': '2', 'selector': '{ \\'catalogId\\': [] }', 'refreshInterval': 3600}, { 'subId': 3, 'peerId': '2', 'selector': '{ \\'catalogId\\': [] }' } ]"), count)
		    .applyTo(cdsClient);
		steps.await(3, TimeUnit.SECONDS);
		assertEquals("Incomplete steps remain", 0, steps.getCount());
		steps = new CountDownLatch(1);
		steps.await(1, TimeUnit.SECONDS);
		assertEquals("Extra steps executed", 1, steps.getCount());
	}
}

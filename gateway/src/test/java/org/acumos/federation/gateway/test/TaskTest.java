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

import java.util.List;
import java.util.Scanner;

import static org.junit.Assert.assertTrue;
import static org.assertj.core.api.Assertions.assertThat; 

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.core.ParameterizedTypeReference;

import org.apache.http.client.HttpClient;

/* this is not good for unit testing .. */
import org.acumos.federation.gateway.common.JsonResponse;
import org.acumos.federation.gateway.common.HttpClientConfigurationBuilder;
import static org.acumos.federation.gateway.common.HttpClientConfigurationBuilder.SSLBuilder;

import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.cds.domain.MLPArtifact;



/**
 */

//@RunWith(SpringJUnit4ClassRunner.class)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = org.acumos.federation.gateway.Application.class,
								webEnvironment = WebEnvironment.RANDOM_PORT,
								properties = {
									"federation.instance=adapter",
									"federation.instance.name=Ghost",
									"peersLocal.source=classpath:/test-peers.json",
									"catalogLocal.source=classpath:/test-catalog.json",
									"server.ssl.key-store=classpath:acumosa.pkcs12",
									"server.ssl.key-store-password=acumosa",
									"server.ssl.key-store-type=PKCS12",
									"server.ssl.key-password = acumosa",
									"server.ssl.trust-store=classpath:acumosTrustStore.jks",
									"server.ssl.trust-store-password=acumos",
									"server.ssl.client-auth=need"
								})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TaskTest {

	@Test
	public void testPeerTask() {

	}

}

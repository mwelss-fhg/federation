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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.acumos.federation.gateway.event.PeerSubscriptionEvent;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
/* this is not good for unit testing .. */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringRunner;



/**
 */

//@RunWith(SpringJUnit4ClassRunner.class)
@RunWith(SpringRunner.class)
@ContextHierarchy({
	@ContextConfiguration(classes = org.acumos.federation.gateway.test.TestAdapterConfiguration.class),
	@ContextConfiguration(classes = org.acumos.federation.gateway.config.FederationConfiguration.class),
	@ContextConfiguration(classes = TaskTest.TaskTestConfiguration.class)
})
@SpringBootTest(classes = org.acumos.federation.gateway.Application.class,
								webEnvironment = WebEnvironment.RANDOM_PORT,
								properties = {
									"spring.main.allow-bean-definition-overriding=true",
									"federation.instance=adapter",
									"federation.instance.name=test",
									"federation.ssl.key-store=classpath:acumosa.pkcs12",
									"federation.ssl.key-store-password=acumosa",
									"federation.ssl.key-store-type=PKCS12",
									"federation.ssl.key-password = acumosa",
									"federation.ssl.trust-store=classpath:acumosTrustStore.jks",
									"federation.ssl.trust-store-password=acumos",
									"federation.ssl.client-auth=need",
									"codes-local.source=classpath:/test-codes.json",
									"peers-local.source=classpath:/task-test-peers.json",
									"catalog-local.source=classpath:/task-test-catalog.json"
								})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TaskTest {

	private final Logger log = LoggerFactory.getLogger(getClass().getName());
	@Autowired
	private ApplicationContext context;

	/**
	 * Called by the test framework and hanging until the event that
	 * we are expecting is coming.
	 * The test config sets up a short check interval.
	 */
	@Test
	public void testPeerTask() {

		PeerSubscriptionListener listener =
			(PeerSubscriptionListener)this.context.getBean("testListener");

		try {
			boolean complete = listener.peerEventLatch.await(10, TimeUnit.SECONDS);
			log.info("event: " + complete + "/" + listener.peerEventLatch.getCount());
			assertTrue("All expected events have occured in the test interval",
								 complete);
		}
		catch (InterruptedException ix) {
			assertTrue(1 == 0);
		}
		//
		assertTrue(listener.event != null);
		assertTrue(listener.event.getSubscription().getPeerId().equals("11111111-1111-1111-1111-111111111111"));
	}

	public static class TaskTestConfiguration {

		@Bean({"testListener"})
		public PeerSubscriptionListener testListener() {
			return new PeerSubscriptionListener();
		}
	}

	public static class PeerSubscriptionListener {
	
		CountDownLatch 				peerEventLatch = new CountDownLatch(1);
		PeerSubscriptionEvent event;

		@EventListener
		public void onApplicationEvent(PeerSubscriptionEvent theEvent) {
			this.event = theEvent;
			this.peerEventLatch.countDown();
		}
	}
}

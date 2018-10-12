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

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.nio.charset.Charset;

import org.acumos.federation.gateway.adapter.onap.sdc.ASDC;
import org.acumos.federation.gateway.adapter.onap.sdc.ASDCException;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

public class AsdcTest {

	private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(MethodHandles.lookup().lookupClass());

	@Test
	public void testASDC() throws Exception {
		for (ASDC.AssetType e : ASDC.AssetType.values())
			log.info("assest type {}, code {}", e);
		for (ASDC.ArtifactType e : ASDC.ArtifactType.values())
			log.info("artifact type {}, code {}", e);
		for (ASDC.ArtifactGroupType e : ASDC.ArtifactGroupType.values())
			log.info("artifact group type {}, code {}", e);
		for (ASDC.LifecycleState e : ASDC.LifecycleState.values())
			log.info("lifecycle state {}, code {}", e);
		ASDC asdc = new ASDC();
		URI uriNoFrag = new URI("http://user:pass@host:443/context/path");
		try {
			asdc.setUri(uriNoFrag);
			Assert.assertTrue(false);
		} catch (Exception ex) {
			log.info("Caught exception as expected");
		}
		URI uri = new URI("http://user:pass@host:443/context/path#fragment");
		asdc.setUri(uri);
		Assert.assertNotNull(asdc.getUri());
	}

	@Test
	public void testASDCException() throws Exception {
		String responseBody = "{\"requestError\": {\"policyException\":{\"text\":\"hello\",\"variables\":[\"d\",\"e\"]}}}";
		HttpClientErrorException ex = new HttpClientErrorException(HttpStatus.I_AM_A_TEAPOT, 
				"status text", responseBody.getBytes(), Charset.forName("UTF-8"));
		ASDCException e = new ASDCException(ex);
		e.addSuppressed(new IllegalArgumentException());
		log.info("testASDCException: ASDC msg {}", e.getASDCMessage());
		log.info("testASDCException: msg {}", e.getMessage());
		log.info("testASDCException: is found? {}", ASDCException.isNotFound(new IllegalArgumentException()));
	}

}

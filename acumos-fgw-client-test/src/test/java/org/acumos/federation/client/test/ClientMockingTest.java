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
package org.acumos.federation.client.test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Test;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpClientErrorException.Unauthorized;
import org.springframework.web.client.HttpClientErrorException.Forbidden;
import org.springframework.web.client.ResourceAccessException;

import static org.acumos.federation.client.test.ClientMocking.xq;
import static org.acumos.federation.client.test.ClientMocking.getConfig;

public class ClientMockingTest {
	private RestTemplate restTemplate;
	public int count;

	public static class X {
		private String y;
		public String getY() {
			return y;
		}
		public void setY(String y) {
			this.y = y;
		}
	}

	@Test
	public void testClientMocking() throws Exception {
		assertEquals("{\"a\":\"b\"}", xq("{'a':'b'}"));
		assertNotNull(getConfig("acumosa"));
		ClientMockingTest rt = this;
		restTemplate = new RestTemplate();
		ClientMocking answers = (new ClientMocking())
			.errorOnNoAuth(401, "Unauthorized")
			.errorOnBadAuth("user", "pass", 403, "Forbidden")
			.on("GET /xyzzy", "[ \"string\" ]")
			.on("GET /empty?withparam=Z", "", info -> { rt.count++; })
			.errorOn("GET /fail", 403, "Forbidden")
			.applyTo(this);
		try {
			List<String> ret = restTemplate.exchange("http://example/xyzzy", HttpMethod.GET, null, new ParameterizedTypeReference<List<String>>(){}).getBody();
			fail();
		} catch (Unauthorized ux) {
			// expected case
		}
		ClientMocking noNoAuthAnswers = (new ClientMocking())
			.errorOnBadAuth("user", "pass", 403, "Forbidden")
			.applyTo(this);
		try {
			List<String> ret = restTemplate.exchange("http://example/xyzzy", HttpMethod.GET, null, new ParameterizedTypeReference<List<String>>(){}).getBody();
			fail();
		} catch (Forbidden fx) {
			// expected case
		}
		restTemplate = new RestTemplateBuilder().basicAuthentication("user", "wrong").build();
		answers.applyTo(this);
		try {
			List<String> ret = restTemplate.exchange("http://example/xyzzy", HttpMethod.GET, null, new ParameterizedTypeReference<List<String>>(){}).getBody();
			fail();
		} catch (Forbidden fx) {
			// expected case
		}
		restTemplate = new RestTemplateBuilder().basicAuthentication("user", "pass").rootUri("http://example").build();
		answers.applyTo(this);
		List<String> ret = restTemplate.exchange("/xyzzy", HttpMethod.GET, null, new ParameterizedTypeReference<List<String>>(){}).getBody();
		assertEquals(1, ret.size());
		assertEquals("string", ret.get(0));
		assertNull(restTemplate.exchange("/empty?withparam={pvalue}", HttpMethod.GET, null, new ParameterizedTypeReference<X>(){}, "Z").getBody());
		try {
			restTemplate.exchange("/fail", HttpMethod.GET, null, new ParameterizedTypeReference<List<String>>(){});
			fail();
		} catch (Forbidden fx) {
			// expected case
		}
		try {
			restTemplate.exchange("/unhandled", HttpMethod.GET, null, new ParameterizedTypeReference<List<String>>(){});
			fail();
		} catch (ResourceAccessException rae) {
			// expected case
		}
		assertEquals(1, count);
	}
}

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.acumos.federation.gateway.util.Utils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UtilsTest {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Test
	public void coverUtilMethods() throws Exception {
		Utils utils = new Utils();
		Assert.assertNotNull(utils);

		Assert.assertTrue(Utils.isEmptyOrNullString(null));
		Assert.assertFalse(Utils.isEmptyOrNullString("hello"));
		
		List<String> list = new ArrayList<>();
		Assert.assertTrue(Utils.isEmptyList(list));
		list.add("abc");
		Assert.assertFalse(Utils.isEmptyList(list));

		final String json = "{\"a\":\"b\"}";
		Map<String,Object> map = Utils.jsonStringToMap(json);
		Assert.assertFalse(map.isEmpty());
		String s = Utils.mapToJsonString(map);
		Assert.assertEquals(json, s);

		try {
			Utils.jsonStringToMap("bogus");
			Assert.assertTrue(false);
		} catch (Exception ex) {
			logger.info("jsonStringToMap failed as expected: {}", ex.toString());
		}

		try {
			map.clear();
			map.put("nonsense", new Object());
			Utils.mapToJsonString(map);
			Assert.assertTrue(false);
		} catch (Exception ex) {
			logger.info("mapToJsonString failed as expected: {}", ex.toString());
		}
		
	}

}

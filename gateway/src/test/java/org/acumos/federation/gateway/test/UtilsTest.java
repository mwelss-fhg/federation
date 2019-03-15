/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2017-2019 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;

import org.acumos.federation.gateway.util.Future;
import org.acumos.federation.gateway.util.Futures;
import org.acumos.federation.gateway.util.ListBuilder;
import org.acumos.federation.gateway.util.MapBuilder;
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

	@Test
	public void testMapBuilder() {
		MapBuilder<String, String> mb = new MapBuilder<String, String>();

		Assert.assertTrue(mb.isEmpty());
		Assert.assertNull(mb.buildOpt());
		mb.putOpt("A", null)
			.putOpt("B", "x")
			.put(new AbstractMap.SimpleEntry<String, String>("C", "y"))
			.putOpt(null)
			.putAll(new MapBuilder<String, String>().build().entrySet())
			.putAll(new MapBuilder<String, String>().putOpt("E", "z").build().entrySet())
			.forceAll(new MapBuilder<String, String>().putOpt("B", "w").build().entrySet(), x -> x.getKey() + "_1")
			.putOpt(new AbstractMap.SimpleEntry<String, String>("F", "v"))
			.putAll(new MapBuilder<String, String>().build())
			.put("G", "u");
		Assert.assertEquals("w", mb.build().get("B_1"));
		Assert.assertEquals("x", mb.build().get("B"));
	}

	@Test
	public void testListBuilder() {
		ListBuilder<String> lb = new ListBuilder<String>();
		HashSet<String> hs = new HashSet();

		hs.add("xyzzy");
		Assert.assertTrue(lb.isEmpty());
		Assert.assertNull(lb.buildOpt());
		lb.add("A").addAll(new String[] { "B", "C" }).addAll(new ListBuilder<String>().build()).addAll(hs).build();
		Assert.assertNull(ListBuilder.asListOpt(new String[]{}));
		Assert.assertNotNull(ListBuilder.asList(new String[]{}));
	}

	@Test
	public void testFutures() throws Exception {
		Future<String> f = Futures.future();
		Assert.assertFalse(f.complete());
		f = Futures.succeededFuture("yes");
		Assert.assertEquals("yes", f.waitForResult());
		f = Futures.failedFuture(new IllegalArgumentException());
		Assert.assertFalse(f.succeeded());
		Assert.assertTrue(f.failed());
		f.waitForCompletion();
		try {
			f.waitForResult();
			Assert.fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException iae) {
			// we want this
		}
		Future<Integer> g = Futures.future();
		f = Futures.advance(g, x -> x.toString());
		Assert.assertFalse(f.complete());
		g.result(new Integer(29));
		Assert.assertEquals("29", f.waitForResult());
		g = Futures.future();
		f = Futures.advance(g, x -> x.toString(), e -> new IllegalArgumentException(e));
		Assert.assertFalse(f.complete());
		g.cause(new Exception());
		Assert.assertTrue(f.failed());
		try {
			f.waitForResult();
			Assert.fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException iae) {
			// we want this
		}
		Futures.Accumulator<String> ac = new Futures.Accumulator();
		f = Futures.future();
		Future<String> f2 = Futures.future();
		Futures.Accumulator<String> ac2 = new Futures.Accumulator();
		ac2.add(f2);
		ac.add(f);
		ac.addAll(ac2);
		Future<List<String>> a = ac.accumulate();
		f.result("x");
		f2.cause(new Exception());
		a.waitForResult();
	}
}

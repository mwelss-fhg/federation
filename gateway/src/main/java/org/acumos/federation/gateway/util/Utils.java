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

package org.acumos.federation.gateway.util;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.acumos.federation.gateway.config.EELFLoggerDelegate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Utils {

	private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(MethodHandles.lookup().lookupClass());

	private static ObjectMapper objectMapper = new ObjectMapper();

	public static boolean isEmptyOrNullString(String input) {
		boolean isEmpty = false;
		if (null == input || 0 == input.trim().length()) {
			isEmpty = true;
		}
		return isEmpty;
	}

	public static boolean isEmptyList(@SuppressWarnings("rawtypes") List input) {
		boolean isEmpty = false;
		if (null == input || 0 == input.size()) {
			isEmpty = true;
		}
		return isEmpty;
	}

	public static Map<String, Object> jsonStringToMap(String jsonString) {
		Map<String, Object> map = new HashMap<>();

		if (!isEmptyOrNullString(jsonString)) {
			try {
				map = objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {
				});
			} catch (IOException x) {
				log.error("jsongStringToMap failed", x);
				throw new IllegalArgumentException("Argument not a map", x);
			}
		}
		return map;
	}

	public static String mapToJsonString(Map<String, ?> theMap) {

		try {
			return objectMapper.writeValueAsString(theMap);
		} catch (JsonProcessingException x) {
			log.error("mapToJsonString failed", x);
			throw new IllegalArgumentException("Failed to convert", x);
		}
	}

}

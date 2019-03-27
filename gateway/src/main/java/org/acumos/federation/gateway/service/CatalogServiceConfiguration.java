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

package org.acumos.federation.gateway.service;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Map;

import org.acumos.federation.gateway.cds.Solution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Component;

/**
 * Allows for the configuration of the base selector which determine the
 * catalogs exposed through federation.
 * An implementation of the CatalogService should only provide those catalogs
 * that pass the catalogs selector
 */
@Component
@ConfigurationProperties(prefix = "catalog")
public class CatalogServiceConfiguration {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	public static final String PUBLIC_ACCESS_TYPE_CODE = "PB";

	private Map<String, Object>	catalogsSelector = Collections.singletonMap(Solution.Fields.accessTypeCode, PUBLIC_ACCESS_TYPE_CODE);

	public Map<String, Object> getCatalogsSelector() {
		return(catalogsSelector);
	}

	public void setCatalogsSelector(String theSelector) {
		catalogsSelector = Collections.unmodifiableMap(JsonParserFactory.getJsonParser().parseMap(theSelector));
	}
}

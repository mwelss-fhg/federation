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

package org.acumos.federation.gateway.service;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.acumos.cds.AccessTypeCode;
import org.acumos.federation.gateway.cds.Solution;
import org.acumos.federation.gateway.cds.SolutionRevision;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Component;

/**
 * Allows for the configuration of the base selector which determine the solutions exposed through federation.
 * An implementation of the CatalogService should only provide those solutions that pass the solutions selector and
 * only thos revisions of these solutions that pass the solution revisions selector.
 */
@Component
@ConfigurationProperties(prefix = "catalog")
public class CatalogServiceConfiguration {

	private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(MethodHandles.lookup().lookupClass());

	private Map<String, Object>	solutionsSelector;
	private Map<String, Object>	solutionsSelectorDefaults;
	private Map<String, Object>	solutionRevisionsSelector;


	public CatalogServiceConfiguration() {
		reset();
	}

	/**
	 * Resets the selectors to their default value.
	 * The validationStatusCode == Passed(PS) criteria has been removed from both solutions and solutionRevisions selector because
	 * the corresponding field is no longer maintained in the backend. It ca always be re-introduced through configuration.
	 */
	private void reset() {
		this.solutionsSelectorDefaults = new HashMap<String, Object>();
		// If not otherwise specified last updated since the beggining of times
		this.solutionsSelectorDefaults.put(Solution.Fields.modified, new Long(1));

		//this selector forces certain criteria such as no client can impose/submit them
		this.solutionsSelector = new HashMap<String, Object>();
		// Fetch only active solutions
		this.solutionsSelector.put(Solution.Fields.active, true);
		// Fetch only Public models
		this.solutionsSelector.put(Solution.Fields.accessTypeCode, AccessTypeCode.PB.toString());

		this.solutionRevisionsSelector = new HashMap<String, Object>();
		// Fetch only for Public revisions
		this.solutionRevisionsSelector.put(SolutionRevision.Fields.accessTypeCode, AccessTypeCode.PB.toString());
		
		this.solutionsSelector = Collections.unmodifiableMap(this.solutionsSelector);;
		this.solutionRevisionsSelector = Collections.unmodifiableMap(this.solutionRevisionsSelector);;
	}

	public Map<String, Object> getSolutionsSelectorDefaults() {
		return this.solutionsSelectorDefaults;
	}

	public void setSolutionsSelectorDefaults(String theSelector) {
		this.solutionsSelectorDefaults = Collections.unmodifiableMap(
															JsonParserFactory.getJsonParser().parseMap(theSelector));
  }

	public Map<String, Object> getSolutionsSelector() {
		return this.solutionsSelector;
	}

	public void setSolutionsSelector(String theSelector) {
		this.solutionsSelector = Collections.unmodifiableMap(
															JsonParserFactory.getJsonParser().parseMap(theSelector));
  }

	public Map<String, Object> getSolutionRevisionsSelector() {
		return this.solutionRevisionsSelector;
	}

	public void setSolutionRevisionsSelector(String theSelector) {
		this.solutionRevisionsSelector = Collections.unmodifiableMap(
																			JsonParserFactory.getJsonParser().parseMap(theSelector));
	}

}

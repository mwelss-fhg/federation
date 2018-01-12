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
package org.acumos.federation.gateway.common;

import java.net.URI;
import java.util.Map;
import java.util.Collection;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * Specifies the REST API that makes up the federation interface. I could add
 * the expected return type and the doc here too.
 */
public enum API {

	SOLUTIONS(Paths.SOLUTIONS, Queries.SOLUTIONS), SOLUTION_DETAIL(Paths.SOLUTION_DETAILS), SOLUTION_REVISIONS(
			Paths.SOLUTION_REVISIONS), SOLUTION_REVISION_DETAILS(
					Paths.SOLUTION_REVISION_DETAILS), SOLUTION_REVISION_ARTIFACTS(
							Paths.SOLUTION_REVISION_ARTIFACTS), ARTIFACT_DETAILS(
									Paths.ARTIFACT_DETAILS), ARTIFACT_DOWNLOAD(
											Paths.ARTIFACT_DOWNLOAD), PEERS(Paths.PEERS);

	private String path;
	private String[] query;

	API(String thePath) {
		this.path = thePath;
	}

	API(String thePath, String[] theQueryParams) {
		this.path = thePath;
		this.query = theQueryParams;
	}

	public String path() {
		return this.path;
	}

	public String[] query() {
		return this.query;
	}

	public Map<String, ?> queryParams(Map<String, ?> theParams) {
		for (String queryParam : this.query) {
			if (!theParams.containsKey(queryParam)) {
				theParams.put(queryParam, null);
			}
		}
		return theParams;
	}

	public String toString() {
		return this.path;
	}

	/**
	 * Prepares a 'full' URI for this API call. It will contain all query
	 * parameters.
	 * 
	 * @param theHttpUrl
	 *            URL
	 * @return URI
	 */
	public UriComponentsBuilder buildUri(String theHttpUrl) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(theHttpUrl).path(this.path);
		if (this.query != null) {
			for (String queryParam : this.query) {
				builder.queryParam(queryParam, "{" + queryParam + "}");
			}
		}
		return builder;
	}

	/**
	 * Prepares a URI containing only the query param present in the given
	 * collection.
	 * 
	 * @param theHttpUrl
	 *            URL
	 * @param theParams
	 *            parameters
	 * @return URI
	 */
	public UriComponentsBuilder buildUri(String theHttpUrl, Collection<String> theParams) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(theHttpUrl).path(this.path);
		if (this.query != null) {
			for (String queryParam : this.query) {
				if (theParams.contains(queryParam)) {
					builder.queryParam(queryParam, "{" + queryParam + "}");
				}
			}
		}
		return builder;
	}

	/*
	 * the params include both path and query params.
	 */
	public URI buildUri(String theHttpUrl, Map<String, ?> theParams) {
		return buildUri(theHttpUrl, theParams.keySet()).buildAndExpand(theParams).encode().toUri();
	}

	/**
	 * Order based version. All query params must be present.
	 * 
	 * @param theHttpUrl
	 *            URL
	 * @param theParams
	 *            Parameters
	 * @return URI
	 */
	public URI buildUri(String theHttpUrl, String... theParams) {
		return buildUri(theHttpUrl).buildAndExpand(theParams).encode().toUri();
	}

	public static class Paths {

		public static final String SOLUTIONS = "/solutions";
		public static final String SOLUTION_DETAILS = "/solutions/{solutionId}";

		public static final String SOLUTION_REVISIONS = "/solutions/{solutionId}/revisions";
		public static final String SOLUTION_REVISION_DETAILS = "/solutions/{solutionId}/revisions/{revisionId}";

		public static final String SOLUTION_REVISION_ARTIFACTS = "/solutions/{solutionId}/revisions/{revisionId}/artifacts";
		public static final String ARTIFACT_DETAILS = "/artifacts/{artifactId}";
		public static final String ARTIFACT_DOWNLOAD = "/artifacts/{artifactId}/download";

		public static final String PEERS = "/peers";

		// public static final String PEER_SUBSCRIBE = "/peer/subscribe";
		// public static final String PEER_UNSUBSCRIBE = "/peer/unsubscribe";
	}

	public static class QueryParameters {

		public static final String SOLUTIONS_SELECTOR = "selector";
	}

	public static class Queries {

		public static final String[] SOLUTIONS = { QueryParameters.SOLUTIONS_SELECTOR };
	}
}

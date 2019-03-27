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
package org.acumos.federation.gateway.common;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * Specifies the REST API that makes up the federation interface. I could add
 * the expected return type and the doc here too.
 */
public enum API {

	CATALOGS(Paths.CATALOGS),
	SOLUTIONS(Paths.SOLUTIONS, Queries.CATALOG_ID),
	SOLUTION_DETAIL(Paths.SOLUTION_DETAILS),
	SOLUTION_REVISIONS(Paths.SOLUTION_REVISIONS),
	SOLUTION_REVISION_DETAILS(Paths.SOLUTION_REVISION_DETAILS, Queries.CATALOG_ID),
	SOLUTION_REVISION_ARTIFACTS(Paths.SOLUTION_REVISION_ARTIFACTS),
	ARTIFACT_CONTENT(Paths.ARTIFACT_CONTENT),
	SOLUTION_REVISION_DOCUMENTS(Paths.SOLUTION_REVISION_DOCUMENTS, Queries.CATALOG_ID),
	DOCUMENT_CONTENT(Paths.DOCUMENT_CONTENT),
	PEERS(Paths.PEERS),
	SUBSCRIPTION(Paths.SUBSCRIPTION),
	PING(Paths.PING),
	PEER_REGISTER(Paths.PEER_REGISTER);

	private String path;
	private String[] query;

	API(String thePath) {
		this.path = thePath;
	}

	API(String thePath, List<String> theQueryParams) {
		this.path = thePath;
		this.query = theQueryParams.toArray(new String[theQueryParams.size()]);
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

	@Override
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
	public UriComponentsBuilder uriBuilder(String theHttpUrl) {
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
	public UriComponentsBuilder uriBuilder(String theHttpUrl, Collection<String> theParams) {
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
		/* While encoding seems like a good/safe idea no API URI actually requires encoding and this causes
		problems when encoding base64 encoded selectors */
		return uriBuilder(theHttpUrl, theParams.keySet()).buildAndExpand(theParams)/*.encode()*/.toUri();
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
		return uriBuilder(theHttpUrl).buildAndExpand(theParams).encode().toUri();
	}

	public static interface Roots {

		public static final String FEDERATION = "/";
		/**
		 * Maybe too particular but at this point all LOCAL interface operations are with respect to one peer
		 */
		public static final String LOCAL = "/peer/{peerId}";
	}

	public static interface Paths {

		public static final String CATALOGS = "/catalogs";
		public static final String SOLUTIONS = "/solutions";
		public static final String SOLUTION_DETAILS = "/solutions/{solutionId}";
		public static final String SOLUTION_REVISIONS = "/solutions/{solutionId}/revisions";
		public static final String SOLUTION_REVISION_DETAILS = "/solutions/{solutionId}/revisions/{revisionId}";
		public static final String SOLUTION_REVISION_ARTIFACTS = "/solutions/{solutionId}/revisions/{revisionId}/artifacts";
		public static final String ARTIFACT_CONTENT = "/artifacts/{artifactId}/content";
		public static final String SOLUTION_REVISION_DOCUMENTS = "/revisions/{revisionId}/documents";
		public static final String DOCUMENT_CONTENT = "/documents/{documentId}/content";

		public static final String SUBSCRIPTION = "/subscription/{subscriptionId}";

		public static final String PEERS = "/peers";
		public static final String PING = "/ping";

		public static final String PEER_REGISTER = "/peer/register";
		public static final String PEER_UNREGISTER = "/peer/unregister";
	}

	public static interface PathParameters {
		public static final String PEER_ID = "peerId";
		public static final String CATALOG_ID = "catalogId";
		public static final String SOLUTION_ID = "solutionId";
		public static final String REVISION_ID = "revisionId";
		public static final String ARTIFACT_ID = "artifactId";
		public static final String DOCUMENT_ID = "documentId";
	}
	public static interface QueryParameters {

		public static final String CATALOG_ID = "catalogId";
	}

	public static interface Queries {

		public static final List<String> CATALOG_ID = Collections.unmodifiableList(Arrays.asList(new String[] { QueryParameters.CATALOG_ID }));
	}
}

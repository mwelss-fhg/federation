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

package org.acumos.federation.gateway.service.impl;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.acumos.federation.gateway.common.JsonResponse;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import org.springframework.beans.factory.annotation.Autowired;

import org.acumos.cds.CCDSConstants;
import org.acumos.cds.client.HttpComponentsClientHttpRequestFactoryBasicAuth;
import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPPeerSubscription;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.cds.domain.MLPUser;
import org.acumos.cds.transport.LoginTransport;
import org.acumos.cds.transport.RestPageRequest;
import org.acumos.cds.transport.RestPageResponse;


/**
 * 
 * 
 * 
 *         Temporary Client until we have login functions available in Common
 *         Data MicroService
 */
public abstract class AbstractClient {

	protected final EELFLoggerDelegate logger = EELFLoggerDelegate.getLogger(getClass().getName());
	
	protected final String baseUrl;
	protected final RestTemplate restTemplate;

	/**
	 * Builds a restTemplate. If user and pass are both supplied, uses basic
	 * HTTP authentication; if either one is missing, no authentication is used.
	 * 
	 * @param theTarget
	 *            URL of the web endpoint
	 * @param theClient
	 *						underlying http client
	 */
	public AbstractClient(String theTarget,
													HttpClient theClient) {
		if (theTarget == null)
			throw new IllegalArgumentException("Null URL not permitted");

		URL url = null;
		try {
			url = new URL(theTarget);
			this.baseUrl = url.toExternalForm();
		}
		catch (MalformedURLException ex) {
			throw new RuntimeException("Failed to parse targedt URL", ex);
		}

		this.restTemplate = new RestTemplateBuilder()
														.requestFactory(
															new HttpComponentsClientHttpRequestFactory(theClient))	
														.rootUri(this.baseUrl)
														.build();
	}


	/**
	 * Builds URI by adding specified path segments and query parameters to the
	 * base URL.
	 * 
	 * @param path
	 *            Array of path segments
	 * @param queryParams
	 *            key-value pairs; ignored if null or empty. Gives special
	 *            treatment to Date-type values.
	 * @param restPageRequest
	 *            TODO
	 * @param pageRequest
	 *            page, size and sort specification; ignored if null.
	 * @return
	 */
	protected URI buildUri(final String[] path, final Map<String, Object> queryParams, RestPageRequest pageRequest) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(this.baseUrl);
		for (int p = 0; p < path.length; ++p)
			builder.pathSegment(path[p]);
		if (queryParams != null && queryParams.size() > 0) {
			for (Map.Entry<String, ? extends Object> entry : queryParams.entrySet()) {
				Object value = null;
				// Server expect Date as Long.
				if (entry.getValue() instanceof Date)
					value = ((Date) entry.getValue()).getTime();
				else
					value = entry.getValue().toString();
				builder.queryParam(entry.getKey(), value);
			}
		}
		if (pageRequest != null) {
			if (pageRequest.getSize() != null)
				builder.queryParam("page", Integer.toString(pageRequest.getPage()));
			if (pageRequest.getPage() != null)
				builder.queryParam("size", Integer.toString(pageRequest.getSize()));
			if (pageRequest.getFieldToDirectionMap() != null && pageRequest.getFieldToDirectionMap().size() > 0) {
				for (Map.Entry<String, String> entry : pageRequest.getFieldToDirectionMap().entrySet()) {
					String value = entry.getKey() + (entry.getValue() == null ? "" : ("," + entry.getValue()));
					builder.queryParam("sort", value);
				}
			}
		}
		return builder.build().encode().toUri();
	}

}	

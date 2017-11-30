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

import org.apache.http.client.HttpClient;

import org.acumos.federation.gateway.common.JsonResponse;
import org.acumos.federation.gateway.config.APIConstants;
import org.acumos.federation.gateway.config.APINames;
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
public class FederationClient extends AbstractClient {


	/**
	 */
	public FederationClient(String theTarget,
													HttpClient theClient) {
		super(theTarget, theClient);
	}

	/**
	 * 
	 * @param queryParameters
	 * 			key-value pairs; ignored if null or empty. Gives special
	 *            treatment to Date-type values.
	 * @return List of MLPSolutions from Remote Acumos
	 * @throws HttpStatusCodeException
	 * 		Throws HttpStatusCodeException is remote acumos is not available
	 */
	public JsonResponse<List<MLPSolution>> getSolutionsListFromPeer(Map<String, Object> queryParameters) throws HttpStatusCodeException {
		URI uri = buildUri(new String[] { APIConstants.SOLUTIONS }, queryParameters,
				null);
		logger.info(EELFLoggerDelegate.debugLogger, "getPeerSubscriptions: uri " + uri);
		System.out.println("getPeerSubscriptions: uri " + uri);
		ResponseEntity<JsonResponse<List<MLPSolution>>> response = null;
		try {
			response = restTemplate.exchange(
									uri,
									HttpMethod.GET,
									null, 
									new ParameterizedTypeReference<JsonResponse<List<MLPSolution>>>() {});
		}
		catch (HttpStatusCodeException x) {
		System.out.println("getPeerSubscriptions: error " + x);
			throw x;
		}
		catch (Throwable t) {
			System.out.println("getPeerSubscriptions: error " + t);
		}
		finally {
		System.out.println("getPeerSubscriptions: response " + response);
		}
		return response == null ? null : response.getBody();
	}
	
	/**
	 * 
	 * @param queryParameters
	 * 			key-value pairs; ignored if null or empty. Gives special
	 *            treatment to Date-type values.
	 * @return List of MLPSolution Revisions from Remote Acumos
	 * @throws HttpStatusCodeException
	 * 		Throws HttpStatusCodeException is remote acumos is not available
	 */
	public JsonResponse<List<MLPSolutionRevision>> getSolutionsRevisionListFromPeer(String solutinoId, Map<String, Object> queryParameters) throws HttpStatusCodeException {
		URI uri = buildUri(new String[] { APIConstants.SOLUTIONS, solutinoId, APIConstants.REVISIONS }, queryParameters,
				null);
		logger.info(EELFLoggerDelegate.debugLogger, "getPeerSubscriptions: uri " + uri);
		ResponseEntity<JsonResponse<List<MLPSolutionRevision>>> response = restTemplate.exchange(uri, HttpMethod.GET,
				null, new ParameterizedTypeReference<JsonResponse<List<MLPSolutionRevision>>>() {
				});
		return response.getBody();
	}
	
	/**
	 * 
	 * @param queryParameters
	 * 			key-value pairs; ignored if null or empty. Gives special
	 *            treatment to Date-type values.
	 * @return List of MLPArtifacts from Remote Acumos
	 * @throws HttpStatusCodeException
	 * 		Throws HttpStatusCodeException is remote acumos is not available
	 */
	public JsonResponse<List<MLPArtifact>> getArtifactsListFromPeer(String solutinoId, String revisionId, Map<String, Object> queryParameters) throws HttpStatusCodeException {
		URI uri = buildUri(new String[] { APIConstants.SOLUTIONS, solutinoId, APIConstants.REVISIONS, revisionId }, queryParameters,
				null);
		logger.info(EELFLoggerDelegate.debugLogger, "getPeerSubscriptions: uri " + uri);
		ResponseEntity<JsonResponse<List<MLPArtifact>>> response = restTemplate.exchange(uri, HttpMethod.GET,
				null, new ParameterizedTypeReference<JsonResponse<List<MLPArtifact>>>() {
				});
		return response.getBody();
	}
}

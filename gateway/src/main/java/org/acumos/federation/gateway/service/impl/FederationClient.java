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
import java.util.HashMap;
import java.util.Collections;

import org.apache.http.client.HttpClient;

import org.acumos.federation.gateway.common.JsonResponse;
import org.acumos.federation.gateway.common.API;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.util.Utils;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.Resource;

//import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import org.springframework.util.Base64Utils;

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
	public JsonResponse<List<MLPSolution>> getSolutions(Map<String, Object> theSelection) throws HttpStatusCodeException {

		String selectorParam = null;
		try {
			selectorParam = theSelection == null ? null
																					 //: UriUtils.encodeQueryParam(Utils.mapToJsonString(theSelection),"UTF-8"); 
																					 : Base64Utils.encodeToString(Utils.mapToJsonString(theSelection).getBytes("UTF-8")); 
		}
		catch (Exception x) {
			throw new IllegalArgumentException("Cannot process the selection argument", x);
		}

		URI uri =	
			API.SOLUTIONS.buildUri(
				this.baseUrl,
				selectorParam == null ? Collections.EMPTY_MAP
															: Collections.singletonMap(API.QueryParameters.SOLUTIONS_SELECTOR, selectorParam));
		logger.info(EELFLoggerDelegate.debugLogger, "Query for " + uri);
		ResponseEntity<JsonResponse<List<MLPSolution>>> response = null;
		try {
			response = restTemplate.exchange(
									uri,
									HttpMethod.GET,
									null, 
									new ParameterizedTypeReference<JsonResponse<List<MLPSolution>>>() {});
		}
		catch (HttpStatusCodeException x) {
			logger.info(EELFLoggerDelegate.debugLogger, uri + " failed.", x);
			throw x;
		}
		catch (Throwable t) {
			logger.info(EELFLoggerDelegate.debugLogger, uri + " unexpected failure.", t);
		}
		finally {
			logger.info(EELFLoggerDelegate.debugLogger, uri + " response " + response);
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
	public JsonResponse<List<MLPSolutionRevision>> getSolutionRevisions(
		String theSolutionId)	throws HttpStatusCodeException {

		URI uri =	API.SOLUTION_REVISIONS.buildUri(this.baseUrl, theSolutionId);
		logger.info(EELFLoggerDelegate.debugLogger, "Query for " + uri);
		ResponseEntity<JsonResponse<List<MLPSolutionRevision>>> response = null;
		try {
			response = restTemplate.exchange(
									uri,
									HttpMethod.GET,
									null,
									new ParameterizedTypeReference<JsonResponse<List<MLPSolutionRevision>>>() {
				});
		}
		catch (HttpStatusCodeException x) {
			logger.info(EELFLoggerDelegate.debugLogger, uri + " failed.", x);
			throw x;
		}
		catch (Throwable t) {
			logger.info(EELFLoggerDelegate.debugLogger, uri + " unexpected failure.", t);
		}
		finally {
			logger.info(EELFLoggerDelegate.debugLogger, uri + " response " + response);
		}
		return response == null ? null : response.getBody();
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
	public JsonResponse<List<MLPArtifact>> getArtifacts(
		String theSolutionId, String theRevisionId) throws HttpStatusCodeException {
		URI uri =	API.SOLUTION_REVISION_ARTIFACTS.buildUri(this.baseUrl, theSolutionId, theRevisionId);
		logger.info(EELFLoggerDelegate.debugLogger, "Query for " + uri);
		ResponseEntity<JsonResponse<List<MLPArtifact>>> response = null;
		try {
			response = restTemplate.exchange(
									uri,
									HttpMethod.GET,
									null,
									new ParameterizedTypeReference<JsonResponse<List<MLPArtifact>>>() {
				});
		}
		catch (HttpStatusCodeException x) {
			logger.info(EELFLoggerDelegate.debugLogger, uri + " failed.", x);
			throw x;
		}
		catch (Throwable t) {
			logger.info(EELFLoggerDelegate.debugLogger, uri + " unexpected failure.", t);
		}
		finally {
			logger.info(EELFLoggerDelegate.debugLogger, uri + " response " + response);
		}
		return response == null ? null : response.getBody();
	}
	
	/**
	 */
	public Resource downloadArtifact(String theArtifactId) 
																								throws HttpStatusCodeException {
		URI uri =	API.ARTIFACT_DOWNLOAD.buildUri(this.baseUrl, theArtifactId);
		logger.info(EELFLoggerDelegate.debugLogger, "Query for " + uri);
		ResponseEntity<Resource> response = null;
		try {
			response = restTemplate.exchange(
									uri,
									HttpMethod.GET,
									null,
									Resource.class);
		}
		catch (HttpStatusCodeException x) {
			logger.info(EELFLoggerDelegate.debugLogger, uri + " failed.", x);
			throw x;
		}
		catch (Throwable t) {
			logger.info(EELFLoggerDelegate.debugLogger, uri + " unexpected failure.", t);
		}
		finally {
			logger.info(EELFLoggerDelegate.debugLogger, uri + " response " + response);
		}
		return response == null ? null : response.getBody();
	}

}

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

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.util.Utils;
import org.apache.http.client.HttpClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 */
public class FederationClient extends AbstractClient {

	private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * @param theTarget
	 *            Target
	 * @param theClient
	 *            HttpClient
	 */
	public FederationClient(String theTarget, HttpClient theClient) {
		super(theTarget, theClient);
	}

	public FederationClient(String theTarget, HttpClient theClient, ObjectMapper theMapper) {
		super(theTarget, theClient, theMapper);
	}

	/**
	 * @return Ping information from/for Remote Acumos
	 * @throws HttpStatusCodeException
	 *             Throws HttpStatusCodeException if remote acumos interaction has failed.
	 */
	public JsonResponse<MLPPeer> ping()
			throws HttpStatusCodeException {
		URI uri = API.PING.buildUri(this.baseUrl);
		log.info(EELFLoggerDelegate.debugLogger, "Query for " + uri);
		ResponseEntity<JsonResponse<MLPPeer>> response = null;
		try {
			response = restTemplate.exchange(uri, HttpMethod.GET, null,
					new ParameterizedTypeReference<JsonResponse<MLPPeer>>() {
					});
		}
		catch (HttpStatusCodeException x) {
			log.error(EELFLoggerDelegate.errorLogger, uri + " failed", x);
			throw x;
		}
		catch (Throwable t) {
			log.error(EELFLoggerDelegate.errorLogger, uri + " unexpected failure.", t);
		}
		finally {
			log.info(EELFLoggerDelegate.debugLogger, uri + " response " + response);
		}
		return response == null ? null : response.getBody();
	}

	/**
	 */
	public JsonResponse<List<MLPPeer>> getPeers()
			throws HttpStatusCodeException {
		URI uri = API.PEERS.buildUri(this.baseUrl);
		log.info(EELFLoggerDelegate.debugLogger, "Query for " + uri);
		ResponseEntity<JsonResponse<List<MLPPeer>>> response = null;
		try {
			response = restTemplate.exchange(uri, HttpMethod.GET, null,
					new ParameterizedTypeReference<JsonResponse<List<MLPPeer>>>() {
					});
		}
		catch (HttpStatusCodeException x) {
			log.error(EELFLoggerDelegate.errorLogger, uri + " failed", x);
			throw x;
		}
		catch (Throwable t) {
			log.error(EELFLoggerDelegate.errorLogger, uri + " unexpected failure.", t);
		}
		finally {
			log.info(EELFLoggerDelegate.debugLogger, uri + " response " + response);
		}
		return response == null ? null : response.getBody();
	}	


	/**
	 * 
	 * @param theSelection
	 *            key-value pairs; ignored if null or empty. Gives special treatment
	 *            to Date-type values.
	 * @return List of MLPSolutions from Remote Acumos
	 * @throws HttpStatusCodeException
	 *             Throws HttpStatusCodeException is remote acumos is not available
	 */
	public JsonResponse<List<MLPSolution>> getSolutions(Map<String, Object> theSelection)
			throws HttpStatusCodeException {

		String selectorParam = null;
		try {
			selectorParam = theSelection == null ? null
					// : UriUtils.encodeQueryParam(Utils.mapToJsonString(theSelection),"UTF-8");
					: Base64Utils.encodeToString(Utils.mapToJsonString(theSelection).getBytes("UTF-8"));
		}
		catch (Exception x) {
			throw new IllegalArgumentException("Cannot process the selection argument", x);
		}

		URI uri = API.SOLUTIONS.buildUri(this.baseUrl, selectorParam == null ? Collections.EMPTY_MAP
				: Collections.singletonMap(API.QueryParameters.SOLUTIONS_SELECTOR, selectorParam));
		log.info(EELFLoggerDelegate.debugLogger, "Query for " + uri);
		ResponseEntity<JsonResponse<List<MLPSolution>>> response = null;
		try {
			response = restTemplate.exchange(uri, HttpMethod.GET, null,
					new ParameterizedTypeReference<JsonResponse<List<MLPSolution>>>() {
					});
		}
		catch (HttpStatusCodeException x) {
			log.error(EELFLoggerDelegate.errorLogger, uri + " failed", x);
			throw x;
		}
		catch (Throwable t) {
			log.error(EELFLoggerDelegate.errorLogger, uri + " unexpected failure.", t);
		}
		finally {
			log.info(EELFLoggerDelegate.debugLogger, uri + " response " + response);
		}
		return response == null ? null : response.getBody();
	}

	/**
	 * @param theSolutionId the solution id
	 * @return Peer information from Remote Acumos
	 * @throws HttpStatusCodeException
	 *             Throws HttpStatusCodeException if remote acumos interaction has failed.
	 */
	public JsonResponse<MLPSolution> getSolution(String theSolutionId)
			throws HttpStatusCodeException {

		URI uri = API.SOLUTION_DETAIL.buildUri(this.baseUrl, theSolutionId);
		log.info(EELFLoggerDelegate.debugLogger, "Query for " + uri);
		ResponseEntity<JsonResponse<MLPSolution>> response = null;
		try {
			response = restTemplate.exchange(uri, HttpMethod.GET, null,
					new ParameterizedTypeReference<JsonResponse<MLPSolution>>() {
					});
		}
		catch (HttpStatusCodeException x) {
			log.error(EELFLoggerDelegate.errorLogger, uri + " failed", x);
			throw x;
		}
		catch (Throwable t) {
			log.error(EELFLoggerDelegate.errorLogger, uri + " unexpected failure.", t);
		}
		finally {
			log.info(EELFLoggerDelegate.debugLogger, uri + " response " + response);
		}
		return response == null ? null : response.getBody();
	}

	/**
	 * 
	 * @param theSolutionId the solution id
	 * @return List of MLPSolution Revisions from Remote Acumos
	 * @throws HttpStatusCodeException
	 *             Throws HttpStatusCodeException is remote acumos is not available
	 */
	public JsonResponse<List<MLPSolutionRevision>> getSolutionRevisions(String theSolutionId)
			throws HttpStatusCodeException {

		URI uri = API.SOLUTION_REVISIONS.buildUri(this.baseUrl, theSolutionId);
		log.info(EELFLoggerDelegate.debugLogger, "Query for " + uri);
		ResponseEntity<JsonResponse<List<MLPSolutionRevision>>> response = null;
		try {
			response = restTemplate.exchange(uri, HttpMethod.GET, null,
					new ParameterizedTypeReference<JsonResponse<List<MLPSolutionRevision>>>() {
					});
		}
		catch (HttpStatusCodeException x) {
			log.error(EELFLoggerDelegate.errorLogger, uri + " failed", x);
			throw x;
		}
		catch (Throwable t) {
			log.info(EELFLoggerDelegate.errorLogger, uri + " unexpected failure.", t);
		}
		finally {
			log.info(EELFLoggerDelegate.debugLogger, uri + " response " + response);
		}
		return response == null ? null : response.getBody();
	}

	/**
	 * 
	 * @param theSolutionId
	 *            Solution ID
	 * @param theRevisionId
	 *            Revision ID
	 * @return List of MLPArtifacts from Remote Acumos
	 * @throws HttpStatusCodeException
	 *             Throws HttpStatusCodeException is remote acumos is not available
	 */
	public JsonResponse<List<MLPArtifact>> getArtifacts(String theSolutionId, String theRevisionId)
			throws HttpStatusCodeException {
		URI uri = API.SOLUTION_REVISION_ARTIFACTS.buildUri(this.baseUrl, theSolutionId, theRevisionId);
		log.info(EELFLoggerDelegate.debugLogger, "Query for " + uri);
		ResponseEntity<JsonResponse<List<MLPArtifact>>> response = null;
		try {
			response = restTemplate.exchange(uri, HttpMethod.GET, null,
					new ParameterizedTypeReference<JsonResponse<List<MLPArtifact>>>() {
					});
		}
		catch (HttpStatusCodeException x) {
			log.error(EELFLoggerDelegate.errorLogger, uri + " failed", x);
			throw x;
		}
		catch (Throwable t) {
			log.error(EELFLoggerDelegate.errorLogger, uri + " unexpected failure.", t);
		}
		finally {
			log.info(EELFLoggerDelegate.debugLogger, uri + " response " + response);
		}
		return response == null ? null : response.getBody();
	}

	/**
	 * @param theArtifactId
	 *            Artifact ID
	 * @return Resource
	 * @throws HttpStatusCodeException
	 *             On failure
	 */
	public Resource downloadArtifact(String theArtifactId) throws HttpStatusCodeException {
		URI uri = API.ARTIFACT_DOWNLOAD.buildUri(this.baseUrl, theArtifactId);
		log.info(EELFLoggerDelegate.debugLogger, "Query for " + uri);
		ResponseEntity<Resource> response = null;
		try {
			response = restTemplate.exchange(uri, HttpMethod.GET, null, Resource.class);
		}
		catch (HttpStatusCodeException x) {
			log.error(EELFLoggerDelegate.errorLogger, uri + " failed", x);
			throw x;
		}
		catch (Throwable t) {
			log.error(EELFLoggerDelegate.errorLogger, uri + " unexpected failure.", t);
			//not very clean
			throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, uri + " unexpected failure: " + t);
		}
		finally {
			log.info(EELFLoggerDelegate.debugLogger, uri + " response " + response);
		}

		if (response == null) {
			//should never get here		
			return null;
		}
		else {
			return response.getBody();
		}
	}

	/**
	 * @return Register self with the peer this client points to.
	 * @throws HttpStatusCodeException
	 *             Throws HttpStatusCodeException if remote acumos interaction has failed.
	 */
	public JsonResponse<MLPPeer> register(MLPPeer theSelf)
			throws HttpStatusCodeException {
		URI uri = API.PEER_REGISTER.buildUri(this.baseUrl);
		log.info(EELFLoggerDelegate.debugLogger, "Query for " + uri);
		ResponseEntity<JsonResponse<MLPPeer>> response = null;
		try {
			response = restTemplate.exchange(uri, HttpMethod.GET, null,
					new ParameterizedTypeReference<JsonResponse<MLPPeer>>() {
					});
		}
		catch (HttpStatusCodeException x) {
			log.error(EELFLoggerDelegate.errorLogger, uri + " failed", x);
			throw x;
		}
		catch (Throwable t) {
			log.error(EELFLoggerDelegate.errorLogger, uri + " unexpected failure.", t);
		}
		finally {
			log.info(EELFLoggerDelegate.debugLogger, uri + " response " + response);
		}
		return response == null ? null : response.getBody();
	}	
}

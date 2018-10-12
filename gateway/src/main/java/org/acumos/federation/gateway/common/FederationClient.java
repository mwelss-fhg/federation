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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPDocument;
import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.util.Utils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.client.HttpClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.Base64Utils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 */
public class FederationClient extends AbstractClient {

	private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(MethodHandles.lookup().lookupClass());

	private HttpClient client;

	/**
	 * @param theTarget
	 *            Target
	 * @param theClient
	 *            HttpClient
	 */
	public FederationClient(String theTarget, HttpClient theClient) {
		super(theTarget, theClient);
		this.client = theClient;
	}

	public FederationClient(String theTarget, HttpClient theClient, ObjectMapper theMapper) {
		super(theTarget, theClient, theMapper);
		this.client = theClient;
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
	 * @param theSolutionId
	 *            Solution ID
	 * @param theRevisionId
	 *            Revision ID
	 * @return Detailed artifact information from Remote Acumos. The returned value can be safely cast to ..gateway.cds.SolutionRevision.
	 * @throws HttpStatusCodeException
	 *             Throws HttpStatusCodeException is remote acumos is not available
	 */
	public JsonResponse<MLPSolutionRevision> getSolutionRevision(String theSolutionId, String theRevisionId)
			throws HttpStatusCodeException {

		URI uri = API.SOLUTION_REVISION_DETAILS.buildUri(this.baseUrl, theSolutionId, theRevisionId);
		log.info(EELFLoggerDelegate.debugLogger, "Query for " + uri);
		ResponseEntity<JsonResponse<MLPSolutionRevision>> response = null;
		try {
			response = restTemplate.exchange(uri, HttpMethod.GET, null,
					new ParameterizedTypeReference<JsonResponse<MLPSolutionRevision>>() {
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
	 * @param theSolutionId
	 *            Solution ID
	 * @param theRevisionId
	 *            Revision ID
	 * @param theArtifactId
	 *            Artifact ID
	 * @return Resource
	 * @throws HttpStatusCodeException
	 *             On failure
	 */
	public Resource getArtifactContent(String theSolutionId, String theRevisionId, String theArtifactId)
																																											throws HttpStatusCodeException {
		return download2(API.ARTIFACT_CONTENT.buildUri(this.baseUrl, theSolutionId, theRevisionId, theArtifactId));
	}

	/**
	 * 
	 * @param theSolutionId
	 *            Solution ID
	 * @param theRevisionId
	 *            Revision ID
	 * @return List of MLPDocuments from Remote Acumos
	 * @throws HttpStatusCodeException
	 *             Throws HttpStatusCodeException is remote acumos is not available
	 */
	public JsonResponse<List<MLPDocument>> getDocuments(String theSolutionId, String theRevisionId)
			throws HttpStatusCodeException {
		URI uri = API.SOLUTION_REVISION_DOCUMENTS.buildUri(this.baseUrl, theSolutionId, theRevisionId);
		log.info(EELFLoggerDelegate.debugLogger, "Query for " + uri);
		ResponseEntity<JsonResponse<List<MLPDocument>>> response = null;
		try {
			response = restTemplate.exchange(uri, HttpMethod.GET, null,
					new ParameterizedTypeReference<JsonResponse<List<MLPDocument>>>() {
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
	 * @param theSolutionId
	 *            Solution ID
	 * @param theRevisionId
	 *            Revision ID
	 * @param theDocumentId
	 *            Document ID
	 * @return Resource
	 * @throws HttpStatusCodeException
	 *             On failure
	 */
	public Resource getDocumentContent(String theSolutionId, String theRevisionId, String theDocumentId)
																																										throws HttpStatusCodeException {
		return download2(API.DOCUMENT_CONTENT.buildUri(this.baseUrl, theSolutionId, theRevisionId, theDocumentId));
	}

	protected Resource download(URI theUri) throws HttpStatusCodeException {
		log.info(EELFLoggerDelegate.debugLogger, "Query for download {}", theUri);
		ResponseEntity<Resource> response = null;
		RequestEntity<Void> request = RequestEntity
																	.get(theUri)
																	.accept(MediaType.ALL)
																	.build();
		try {
			response = restTemplate.exchange(request, Resource.class);
		}
		catch (HttpStatusCodeException x) {
			log.error(EELFLoggerDelegate.errorLogger, theUri + " failed", x);
			throw x;
		}
		catch (Throwable t) {
			log.error(EELFLoggerDelegate.errorLogger, theUri + " unexpected failure.", t);
			//not very clean
			throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, theUri + " unexpected failure: " + t);
		}
		finally {
			log.info(EELFLoggerDelegate.debugLogger, theUri + " response " + response);
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
	 * Important: the Resource returned by this method MUST BE CLOSED by whoever uses it.
	 */
	protected StreamingResource download2(URI theUri) throws HttpStatusCodeException {
		log.info(EELFLoggerDelegate.debugLogger, "Query for download {}", theUri);
		ClientHttpResponse response = null;
		try {
			ClientHttpRequest request =	new HttpComponentsClientHttpRequestFactory(this.client)
																		.createRequest(theUri, HttpMethod.GET);
			request.getHeaders().setAccept(Collections.singletonList(MediaType.ALL));
			response = request.execute();
			HttpStatus status = HttpStatus.valueOf(response.getRawStatusCode());
			if (!status.is2xxSuccessful())
				throw new HttpClientErrorException(status, response.getStatusText());
		
			log.info(EELFLoggerDelegate.debugLogger, "Query for download got response {}", response);
	
			return new StreamingResource(response);
		}
		catch (IOException ex) {
			throw new ResourceAccessException("I/O error for " + theUri + ": " + ex.getMessage(), ex);
		}
	}

	public static class StreamingResource extends InputStreamResource
																				implements Closeable {

		private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(MethodHandles.lookup().lookupClass());
		private ClientHttpResponse response;

		StreamingResource(ClientHttpResponse theResponse) throws IOException {
			super(theResponse.getBody());
			this.response = theResponse;
		}

		@Override
		public InputStream getInputStream() throws IOException, IllegalStateException {
			log.trace(EELFLoggerDelegate.debugLogger, "Download input stream access at {}",ExceptionUtils.getStackTrace(new RuntimeException("Input stream access")) );
			return super.getInputStream();
		}

		@Override
		public long contentLength() throws java.io.IOException {
			return this.response.getHeaders().getContentLength();
		}

		@Override
		public void close()  throws IOException {
			log.info(EELFLoggerDelegate.debugLogger, "Streaming resource closed");
			this.response.close();
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

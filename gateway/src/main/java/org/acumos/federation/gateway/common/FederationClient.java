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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPCatalog;
import org.acumos.cds.domain.MLPDocument;
import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.federation.gateway.util.Utils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 */
public class FederationClient extends AbstractClient {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
	 * @throws FederationException
	 *             Throws FederationException if remote acumos interaction has failed.
	 */
	public JsonResponse<MLPPeer> ping() throws FederationException {
		return handle(
		    API.PING.buildUri(this.baseUrl),
		    new ParameterizedTypeReference<JsonResponse<MLPPeer>>(){});
	}

	public JsonResponse<List<MLPPeer>> getPeers()
			throws FederationException {
		return handle(
		    API.PEERS.buildUri(this.baseUrl),
		    new ParameterizedTypeReference<JsonResponse<List<MLPPeer>>>(){});
	}	


	/**
	 * List catalogs in the remote Acumos.
	 *
	 * @return List of MLPCatalogs from remote Acumos.
	 *
	 * @throws FederationException
	 *             If remote acumos is not available
	 */
	public JsonResponse<List<MLPCatalog>> getCatalogs() throws FederationException {
		return handle(
		    API.CATALOGS.buildUri(this.baseUrl),
		    new ParameterizedTypeReference<JsonResponse<List<MLPCatalog>>>(){});
	}
	/**
	 * 
	 * @param theSelection
	 *            key-value pairs; ignored if null or empty. Gives special treatment
	 *            to Date-type values.
	 * @return List of MLPSolutions from Remote Acumos
	 * @throws FederationException
	 *             If remote acumos is not available
	 */
	public JsonResponse<List<MLPSolution>> getSolutions(Map<String, Object> theSelection)
			throws FederationException {

		String selectorParam = null;
		try {
			log.info("getSolutions selector {}", Utils.mapToJsonString(theSelection));
			selectorParam = theSelection == null ? null
					: Base64Utils.encodeToString(Utils.mapToJsonString(theSelection).getBytes("UTF-8"));
			log.info("getSolutions encoded selector {}", selectorParam);
		}
		catch (Exception x) {
			throw new IllegalArgumentException("Cannot process the selection argument", x);
		}

		URI uri = API.SOLUTIONS.buildUri(this.baseUrl, selectorParam == null ? Collections.EMPTY_MAP : Collections.singletonMap(API.QueryParameters.SOLUTIONS_SELECTOR, selectorParam));
		return handle(uri, new ParameterizedTypeReference<JsonResponse<List<MLPSolution>>>() {});
	}

	/**
	 * @param theSolutionId the solution id
	 * @return Peer information from Remote Acumos
	 * @throws FederationException
	 *             if remote acumos interaction has failed.
	 */
	public JsonResponse<MLPSolution> getSolution(String theSolutionId)
			throws FederationException {

		return handle(
		    API.SOLUTION_DETAIL.buildUri(this.baseUrl, theSolutionId),
		    new ParameterizedTypeReference<JsonResponse<MLPSolution>>() {});
	}

	/**
	 * 
	 * @param theSolutionId the solution id
	 * @return List of MLPSolution Revisions from Remote Acumos
	 * @throws FederationException
	 *             if remote acumos is not available
	 */
	public JsonResponse<List<MLPSolutionRevision>> getSolutionRevisions(String theSolutionId)
			throws FederationException {

		return handle(
		    API.SOLUTION_REVISIONS.buildUri(this.baseUrl, theSolutionId),
		    new ParameterizedTypeReference<JsonResponse<List<MLPSolutionRevision>>>() {});
	}

	/**
	 * @param theSolutionId
	 *            Solution ID
	 * @param theRevisionId
	 *            Revision ID
	 * @return Detailed artifact information from Remote Acumos. The returned value can be safely cast to ..gateway.cds.SolutionRevision.
	 * @throws FederationException
	 *             if remote acumos is not available
	 */
	public JsonResponse<MLPSolutionRevision> getSolutionRevision(String theSolutionId, String theRevisionId)
			throws FederationException {

		return handle(
		     API.SOLUTION_REVISION_DETAILS.buildUri(this.baseUrl, theSolutionId, theRevisionId),
		     new ParameterizedTypeReference<JsonResponse<MLPSolutionRevision>>() {});
	}

	/**
	 * 
	 * @param theSolutionId
	 *            Solution ID
	 * @param theRevisionId
	 *            Revision ID
	 * @return List of MLPArtifacts from Remote Acumos
	 * @throws FederationException
	 *             if remote acumos is not available
	 */
	public JsonResponse<List<MLPArtifact>> getArtifacts(String theSolutionId, String theRevisionId) throws FederationException {
		return handle(
		    API.SOLUTION_REVISION_ARTIFACTS.buildUri(this.baseUrl, theSolutionId, theRevisionId),
		    new ParameterizedTypeReference<JsonResponse<List<MLPArtifact>>>() {});
	}

	/**
	 * @param theSolutionId
	 *            Solution ID
	 * @param theRevisionId
	 *            Revision ID
	 * @param theArtifactId
	 *            Artifact ID
	 * @return Resource
	 * @throws FederationException On failure
	 */
	public Resource getArtifactContent(String theSolutionId, String theRevisionId, String theArtifactId) throws FederationException {
		return download2(API.ARTIFACT_CONTENT.buildUri(this.baseUrl, theSolutionId, theRevisionId, theArtifactId));
	}

	/**
	 * 
	 * @param theSolutionId
	 *            Solution ID
	 * @param theRevisionId
	 *            Revision ID
	 * @return List of MLPDocuments from Remote Acumos
	 * @throws FederationException
	 *             if remote acumos is not available
	 */
	public JsonResponse<List<MLPDocument>> getDocuments(String theSolutionId, String theRevisionId) throws FederationException {
		return handle(
		    API.SOLUTION_REVISION_DOCUMENTS.buildUri(this.baseUrl, theSolutionId, theRevisionId),
		    new ParameterizedTypeReference<JsonResponse<List<MLPDocument>>>() {});
	}

	/**
	 * @param theSolutionId
	 *            Solution ID
	 * @param theRevisionId
	 *            Revision ID
	 * @param theDocumentId
	 *            Document ID
	 * @return Resource
	 * @throws FederationException
	 *             On failure
	 */
	public Resource getDocumentContent(String theSolutionId, String theRevisionId, String theDocumentId) throws FederationException {
		return download2(API.DOCUMENT_CONTENT.buildUri(this.baseUrl, theSolutionId, theRevisionId, theDocumentId));
	}

	/**
	 * @param theSelf self
	 * @return Register self with the peer this client points to.
	 * @throws FederationException
	 *             if remote acumos is not available
	 */
	public JsonResponse<MLPPeer> register(MLPPeer theSelf) throws FederationException {
		return handle(
		    API.PEER_REGISTER.buildUri(this.baseUrl),
		    new ParameterizedTypeReference<JsonResponse<MLPPeer>>() {});
	}	

	/**
	 * Handle errors when making requests to remote Acumos.
	 * @param uri the URI on the remote Acumos
	 * @param fcn the function to make the remote call
	 * @return the body of the response from the remote call
	 * @throws FederationException if remote acumos is not available
	 */
	private <T> T handle(URI uri, Supplier<ResponseEntity<T>> fcn) throws FederationException {
		try {
			log.info("Query for {}", uri);
			ResponseEntity<T> response = fcn.get();
			log.debug("{} response {}", uri, response);
			return response == null? null: response.getBody();
		} catch (HttpStatusCodeException scx) {
			log.error(uri + " failed", scx);
			throw new PeerException(uri, scx);
		} catch (Throwable t) {
			log.error(uri + " unexpected failure.", t);
			throw new FederationException(uri, t);
		}
	}

	/**
	 * Handle common part of making most requests to remote Acumos.
	 * @param uri the URI on the remote Acumos
	 * @param type parameterized type reference for parsing return type
	 * @return the body of the response from the remote call
	 * @throws FederationException if remote acumos is not available
	 */
	private <T> T handle(URI uri, ParameterizedTypeReference<T> type) throws FederationException {
		return handle(uri, () -> restTemplate.exchange(uri, HttpMethod.GET, null, type));
	}

	protected Resource download(URI theUri) throws FederationException {
		RequestEntity<Void> request = RequestEntity.get(theUri).accept(MediaType.ALL).build();
		return handle(theUri, () -> restTemplate.exchange(request, Resource.class));
	}

	/**
	 * Download content of a URI.
	 * E.g. the body of an artifact or document.
	 * Important: the Resource returned by this method MUST BE CLOSED by whoever uses it.
	 * @param theUri URI
	 * @return Resource
	 * @throws FederationException on failure
	 */
	protected StreamingResource download2(URI theUri) throws FederationException {
		log.info("Query for download {}", theUri);
		ClientHttpResponse response = null;
		try {
			ClientHttpRequest request =	new HttpComponentsClientHttpRequestFactory(this.client)
																		.createRequest(theUri, HttpMethod.GET);
			request.getHeaders().setAccept(Collections.singletonList(MediaType.ALL));
			response = request.execute();
			HttpStatus status = HttpStatus.valueOf(response.getRawStatusCode());
			if (!status.is2xxSuccessful())
				throw new HttpClientErrorException(status, response.getStatusText());
		
			log.info("Query for download got response {}", response);
	
			return new StreamingResource(response);
		}
		catch (IOException iox) {
			throw new FederationException(theUri, iox);
		}
	}

	public static class StreamingResource extends InputStreamResource implements Closeable {

		private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
		private ClientHttpResponse response;

		StreamingResource(ClientHttpResponse theResponse) throws IOException {
			super(theResponse.getBody());
			this.response = theResponse;
		}

		@Override
		public InputStream getInputStream() throws IOException, IllegalStateException {
			log.trace("Download input stream access at {}",ExceptionUtils.getStackTrace(new RuntimeException("Input stream access")) );
			return super.getInputStream();
		}

		@Override
		public long contentLength() throws java.io.IOException {
			return this.response.getHeaders().getContentLength();
		}

		@Override
		public void close()  throws IOException {
			log.info("Streaming resource closed");
			this.response.close();
		}
	}
}

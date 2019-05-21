/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
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
package org.acumos.federation.client;

import java.util.List;
import java.io.InputStream;
import java.net.URI;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;

import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPCatalog;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPDocument;

import org.acumos.federation.client.config.ClientConfig;
import org.acumos.federation.client.data.JsonResponse;

/**
 * Client for the Federation Server's public (E5) API.  Note that servers
 * implementing the API may restrict what information they
 * share with clients.  Servers may refuse access to some clients, may refuse
 * access to some operations, may restrict what data is visible to clients,
 * etc., based on their particular policies.  This may result in client
 * methods returning null, returning lists with reduced numbers of elements, or 
 * throwing {@link org.springframework.web.client.RestClientException} or its
 * subclasses.
 * @see GatewayClient
 */
public class FederationClient extends ClientBase {
	/**
	 * The base URI for pinging.
	 */
	public static final String PING_URI = "/ping";
	/**
	 * The base URI for listing known peers.
	 */
	public static final String PEERS_URI = "/peers";
	/**
	 * The base URI for listing visible catalogs.
	 */
	public static final String CATALOGS_URI = "/catalogs";
	/**
	 * The base URI for listing catalog solutions.
	 */
	public static final String SOLUTIONS_URI = "/solutions";
	/**
	 * The base URI for fetching solution metadata.
	 */
	public static final String SOLUTION_URI = "/solutions/{solutionId}";
	/**
	 * The base URI for listing solution revisions.
	 */
	public static final String REVISIONS_URI = "/solutions/{solutionId}/revisions";
	/**
	 * The base URI for fetching revision metadata.
	 */
	public static final String REVISION_URI = "/solutions/{solutionId}/revisions/{revisionId}";
	/**
	 * The base URI for listing revision artifacts.
	 */
	public static final String ARTIFACTS_URI = "/solutions/{solutionId}/revisions/{revisionId}/artifacts";
	/**
	 * The base URI for fetching artifact content.
	 */
	public static final String ARTIFACT_URI = "/artifacts/{artifactId}/content";
	/**
	 * The base URI for listing revision documents.
	 */
	public static final String DOCUMENTS_URI = "/revisions/{revisionId}/documents";
	/**
	 * The base URI for fetching document content.
	 */
	public static final String DOCUMENT_URI = "/documents/{documentId}/content";
	/**
	 * The base URI for registering.
	 */
	public static final String REGISTER_URI = "/peer/register";
	/**
	 * The base URI for unregistering.
	 */
	public static final String UNREGISTER_URI = "/peer/unregister";

	/**
	 * The query for specifying a catalog ID.
	 */
	public static final String CATID_QUERY = "?catalogId={catalogId}";

	/**
	 * Peer Status Code for Active
	 */
	public static final String PSC_ACTIVE = "AC";
	/**
	 * Peer Status Code for Renounced
	 */
	public static final String PSC_RENOUNCED = "RN";
	/**
	 * Peer Status Code for Requested
	 */
	public static final String PSC_REQUESTED = "RQ";

	/**
	 * Artifact Type Code for Docker Image
	 */
	public static final String ATC_DOCKER = "DI";

	/**
	 * Create a Federation Client with the default mapper and resource loader.
	 *
	 * @param target The base URL for the server to be accessed.
	 * @param conf The configuration for certificates and credentials.
	 */
	public FederationClient(String target, ClientConfig conf) {
		this(target, conf, null, null);
	}

	/**
	 * Create a Federation Client.
	 *
	 * @param target The base URL for the server to be accessed.
	 * @param conf The configuration for certificates and credentials.
	 * @param mapper The object mapper.  If mapper is null, the default
	 *               object mapper is used to read and write JSON.
	 * @param loader The resource loader.  If loader is null, a
	 *               DefaultResourceLoader is created and used.
	 *               The loader is used for accessing the key store
	 *               and trust store for TLS certificates.
	 */
	public FederationClient(String target, ClientConfig conf, ObjectMapper mapper, ResourceLoader loader) {
		super(target, conf, mapper, loader);
	}

	/**
	 * Check connectivity to the server.
	 *
	 * @return The server's own MLPPeer record.
	 */
	public MLPPeer ping() {
		return handleResponse(PING_URI, new ParameterizedTypeReference<JsonResponse<MLPPeer>>(){});
	}

	/**
	 * Get a list of the server's peers.
	 *
	 * @return The list of peers.
	 */
	public List<MLPPeer> getPeers() {
		return handleResponse(PEERS_URI, new ParameterizedTypeReference<JsonResponse<List<MLPPeer>>>(){});
	}

	/**
	 * Request the server to add this client to its list of peers.
	 *
	 * @return The server's own MLPPeer record.
	 */
	public MLPPeer register() {
		return handleResponse(REGISTER_URI, HttpMethod.POST, new ParameterizedTypeReference<JsonResponse<MLPPeer>>(){});
	}

	/**
	 * Request that the server drop this client from its list of peers.
	 *
	 * @return The server's own MLPPeer record.
	 */
	public MLPPeer unregister() {
		return handleResponse(UNREGISTER_URI, HttpMethod.POST, new ParameterizedTypeReference<JsonResponse<MLPPeer>>(){});
	}

	/**
	 * Get a list of the server's catalogs.
	 *
	 * @return The list of catalogs (enhanced with their sizes), the peer is willing to share.
	 */
	public List<MLPCatalog> getCatalogs() {
		return handleResponse(CATALOGS_URI, new ParameterizedTypeReference<JsonResponse<List<MLPCatalog>>>(){});
	}

	/**
	 * Get a list of the solutions in a catalog.
	 *
	 * @param catalogId The ID of the catalog containing the solutions.
	 * @return The list of solutions in the catalog.
	 */
	public List<MLPSolution> getSolutions(String catalogId) {
		return handleResponse(SOLUTIONS_URI + CATID_QUERY, new ParameterizedTypeReference<JsonResponse<List<MLPSolution>>>(){}, catalogId);
	}

	/**
	 * Get information about a solution.
	 *
	 * @param solutionId The ID of the solution.
	 * @return The solution's metadata, enhanced with its picture and revisions.
	 */
	public MLPSolution getSolution(String solutionId) {
		return handleResponse(SOLUTION_URI, new ParameterizedTypeReference<JsonResponse<MLPSolution>>(){}, solutionId);
	}

	/**
	 * Get a list of revisions in a solution.
	 *
	 * @param solutionId The ID of the solution.
	 * @return The solution's revisions.
	 */
	public List<MLPSolutionRevision> getSolutionRevisions(String solutionId) {
		return handleResponse(REVISIONS_URI, new ParameterizedTypeReference<JsonResponse<List<MLPSolutionRevision>>>(){}, solutionId);
	}

	/**
	 * Get information about a revision.
	 *
	 * Note: if catalogId is null, no description or documents will be returned.
	 * @param solutionId The ID of the solution.
	 * @param revisionId The ID of the revision.
	 * @param catalogId The ID of the catalog listing the solution.
	 * @return The revision's metadata, enhanced with its description, artifacts, and documents.
	 */
	public MLPSolutionRevision getSolutionRevision(String solutionId, String revisionId, String catalogId) {
		if (catalogId != null) {
			return handleResponse(REVISION_URI + CATID_QUERY, new ParameterizedTypeReference<JsonResponse<MLPSolutionRevision>>(){}, solutionId, revisionId, catalogId);
		}
		return handleResponse(REVISION_URI, new ParameterizedTypeReference<JsonResponse<MLPSolutionRevision>>(){}, solutionId, revisionId);
	}

	/**
	 * Get a list of artifacts in a revision.
	 *
	 * @param solutionId The ID of the solution.
	 * @param revisionId The ID of the revision.
	 * @return The revision's artifacts.
	 */
	public List<MLPArtifact> getArtifacts(String solutionId, String revisionId) {
		return handleResponse(ARTIFACTS_URI, new ParameterizedTypeReference<JsonResponse<List<MLPArtifact>>>(){}, solutionId, revisionId);
	}

	/**
	 * Fetch the content of an artifact.
	 *
	 * Note: artifact content can be large (gigabytes).
	 * @param artifactId The ID of the artifact.
	 * @return An InputStream for retrieving the artifact content.
	 */
	public InputStream getArtifactContent(String artifactId) {
		return download(ARTIFACT_URI, artifactId);
	}

	/**
	 * List the documents in a revision and catalog.
	 *
	 * @param revisionId The ID of the revision.
	 * @param catalogId The ID of the catalog listing the revision's solution.
	 * @return The revision's documents.
	 */
	public List<MLPDocument> getDocuments(String revisionId, String catalogId) {
		return handleResponse(DOCUMENTS_URI + CATID_QUERY, new ParameterizedTypeReference<JsonResponse<List<MLPDocument>>>(){}, revisionId, catalogId);
	}

	/**
	 * Fetch the content of a document.
	 *
	 * @param documentId The ID of the document.
	 * @return An InputStream for retrieving the artifact content.
	 */
	public InputStream getDocumentContent(String documentId) {
		return download(DOCUMENT_URI, documentId);
	}
}

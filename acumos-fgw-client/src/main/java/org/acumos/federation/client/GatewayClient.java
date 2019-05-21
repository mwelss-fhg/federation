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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;


import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPCatalog;
import org.acumos.cds.domain.MLPSolution;

import org.acumos.federation.client.config.ClientConfig;
import org.acumos.federation.client.data.JsonResponse;

/**
 * Client for the Federation Gateway's private API.  Except as specified,
 * these operations simply request that the gateway server proxy the specified
 * request using the gateway server's own credentials, to the specified peer
 * federation server.  Both the gateway server, and the federation server
 * responding to a request proxied by the gateway server, may refuse access to
 * some clients, may refuse access to some operations, may restrict what data is
 * visible to clients, etc. based on their particular policies.  This may result
 * in client methods returning null, returning lists with reduced numbers of
 * elements, or throwing
 * {@link org.springframework.web.client.RestClientException} or its subclasses.
 * @see FederationClient
 */
public class GatewayClient extends ClientBase {
	/**
	 * The URI prefix for specifying what peer the request refers to.
	 */
	public static final String PEER_PFX = "/peer/{peerId}";

	/**
	 * The base URI for triggering subscriptions.
	 */
	public static final String SUBSCRIPTION_URI = "/subscription/{subscriptionId}";

	/**
	 * Create a Gateway Client with the default mapper and resource loader.
	 *
	 * @param target The base URL for the server to be accessed.
	 * @param conf The configuration for certificates and credentials.
	 */
	public GatewayClient(String target, ClientConfig conf) {
		this(target, conf, null, null);
	}

	/**
	 * Create a Gateway Client.
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
	public GatewayClient(String target, ClientConfig conf, ObjectMapper mapper, ResourceLoader loader) {
		super(target, conf, mapper, loader);
	}

	/**
	 * Verify connectivity between the local gateway server and the remote
	 * federation server.
	 *
	 * @param peerId The ID of the peer Acumos.
	 * @return The remote server's own MLPPeer record.
	 */
	public MLPPeer ping(String peerId) {
		return handleResponse(PEER_PFX + FederationClient.PING_URI, new ParameterizedTypeReference<JsonResponse<MLPPeer>>(){}, peerId);
	}

	/**
	 * Ask the remote federation server for
	 * a list of its peers.
	 *
	 * @param peerId The ID of the peer Acumos.
	 * @return The list of the peer's peers.
	 */
	public List<MLPPeer> getPeers(String peerId) {
		return handleResponse(PEER_PFX + FederationClient.PEERS_URI, new ParameterizedTypeReference<JsonResponse<List<MLPPeer>>>(){}, peerId);
	}

	/**
	 * Ask the remote federation server to
	 * add the local server to its list of peers.
	 *
	 * @param peerId The ID of the peer Acumos.
	 * @return The remote server's own MLPPeer record.
	 */
	public MLPPeer register(String peerId) {
		return handleResponse(PEER_PFX + FederationClient.REGISTER_URI, HttpMethod.POST, new ParameterizedTypeReference<JsonResponse<MLPPeer>>(){}, peerId);
	}

	/**
	 * Ask the remote federation server
	 * for a list of catalogs.
	 *
	 * @param peerId The ID of the peer Acumos.
	 * @return The list of catalogs (enhanced with their sizes), the peer is willing to share.
	 */
	public List<MLPCatalog> getCatalogs(String peerId) {
		return handleResponse(PEER_PFX + FederationClient.CATALOGS_URI, new ParameterizedTypeReference<JsonResponse<List<MLPCatalog>>>(){}, peerId);
	}

	/**
	 * Ask the remote federation server
	 * for a list of solutions.
	 *
	 * @param peerId The ID of the peer Acumos.
	 * @param catalogId The ID of the catalog to query.
	 * @return The list of solutions in the peer's catalog.
	 */
	public List<MLPSolution> getSolutions(String peerId, String catalogId) {
		return handleResponse(PEER_PFX + FederationClient.SOLUTIONS_URI + FederationClient.CATID_QUERY, new ParameterizedTypeReference<JsonResponse<List<MLPSolution>>>(){}, peerId, catalogId);
	}

	/**
	 * Ask the remote federation server for information about a solution.
	 *
	 * @param peerId The ID of the peer Acumos.
	 * @param solutionId The ID of the solution.
	 * @return The solution's metadata, enhanced with its picture and revisions.
	 */
	public MLPSolution getSolution(String peerId, String solutionId) {
		return handleResponse(PEER_PFX + FederationClient.SOLUTION_URI, new ParameterizedTypeReference<JsonResponse<MLPSolution>>(){}, peerId, solutionId);
	}

	/**
	 * Ask the local gateway server to poll a subscription.  Note that
	 * this operation is not proxied by the local gateway server but
	 * rather requests the local gateway server to immediately poll the
	 * specified subscription to the remote federation server, without
	 * waiting until its normally scheduled time.
	 *
	 * @param peerId The ID of the peer Acumos being subscribed.
	 * @param subscriptionId The ID of the local Acumos' subscription to the peer.
	 */
	public void triggerPeerSubscription(String peerId, long subscriptionId) {
		handleResponse(PEER_PFX + SUBSCRIPTION_URI, HttpMethod.POST, new ParameterizedTypeReference<JsonResponse<Void>>(){}, peerId, subscriptionId);
	}
}

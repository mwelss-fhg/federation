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

package org.acumos.federation.gateway.controller;

import java.lang.invoke.MethodHandles;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import org.acumos.cds.domain.MLPPeer;

import org.acumos.federation.gateway.common.Clients;
import org.acumos.federation.gateway.common.FederationClient;
import org.acumos.federation.gateway.common.JsonResponse;
import org.acumos.federation.gateway.service.PeerService;


public abstract class AbstractController {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@FunctionalInterface
	protected interface ThrowingFunction<T, R> {
		R apply(T t) throws Exception;
	}

	@Autowired
	protected Clients clients;
	@Autowired
	protected PeerService peerService;

	protected static final String APPLICATION_JSON = MediaType.APPLICATION_JSON_UTF8_VALUE;
	protected static final String APPLICATION_OCTET_STREAM = MediaType.APPLICATION_OCTET_STREAM_VALUE;

	protected final ObjectMapper mapper;

	public AbstractController() {
		mapper = new ObjectMapper();
	}

	/**
	 * Handle common aspects of forwarding a local request to a peer Acumos.
	 * @param <T> the type of the expected response, from the peer
	 * @param opname the name of the operation to perform
	 * @param response the HTTP response for setting error codes
	 * @param peerId the ID of the remote peer to call
	 * @param fcn the operation to invoke on the remote peer
	 * @return response from peer
	 */
	protected <T> JsonResponse<T> callPeer(String opname, HttpServletResponse response, String peerId, ThrowingFunction<FederationClient, JsonResponse<T>> fcn) {
		try {
			MLPPeer peer = peerService.getPeerById(peerId);
			if (peer == null) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return JsonResponse.<T>buildErrorResponse()
				    .withMessage("No peer with id " + peerId + " found.")
				    .build();
			}
			JsonResponse<T> ret = fcn.apply(clients.getFederationClient(peer.getApiUrl()));
			response.setStatus(HttpServletResponse.SC_OK);
			return ret;
		} catch (Exception x) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log.error("Exception occurred during peer " + peerId + " " + opname, x);
			return JsonResponse.<T>buildErrorResponse().withError(x).build();
		}
	}
}

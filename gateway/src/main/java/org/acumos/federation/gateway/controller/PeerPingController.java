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

package org.acumos.federation.gateway.controller;

import java.lang.invoke.MethodHandles;

import javax.servlet.http.HttpServletResponse;

import org.acumos.cds.domain.MLPPeer;
import org.acumos.federation.gateway.common.API;
import org.acumos.federation.gateway.common.Clients;
import org.acumos.federation.gateway.common.JsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.acumos.federation.gateway.service.PeerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import io.swagger.annotations.ApiOperation;

@Controller
@RequestMapping(API.Roots.LOCAL)
public class PeerPingController extends AbstractController {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private Clients	clients;
	@Autowired
	private PeerService peerService;
	

	/**
	 * Allows local components to ping a peer.
	 * @param theHttpResponse
	 *            HttpServletResponse
	 * @param thePeerId Peer ID
	 * @return The remote peer information
	 */
	@CrossOrigin
	@PreAuthorize("hasAuthority(T(org.acumos.federation.gateway.security.Priviledge).PEER_ACCESS)")
	@ApiOperation(value = "Invoked by local Acumos to get peer Acumos status and information.", response = MLPPeer.class)
	@RequestMapping(value = { API.Paths.PING }, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<MLPPeer> ping(
			/* HttpServletRequest theHttpRequest, */
			HttpServletResponse theHttpResponse,
			@PathVariable("peerId") String thePeerId) {

		JsonResponse<MLPPeer> response = new JsonResponse<MLPPeer>();
		log.debug(API.Roots.LOCAL + "" + API.Paths.PING);
		try {
			MLPPeer peer = this.peerService.getPeerById(thePeerId);
			if (peer == null) {
				response = JsonResponse.<MLPPeer> buildErrorResponse()
																 .withMessage("No peer with id " + thePeerId + " found.")
																 .build();
				theHttpResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}
			else {
				response = this.clients.getFederationClient(peer.getApiUrl()).ping();
				theHttpResponse.setStatus(HttpServletResponse.SC_OK);
			}
		} 
		catch (Exception x) {
			response = JsonResponse.<MLPPeer> buildErrorResponse()
														 .withError(x)
														 .build();
			theHttpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log.error("Exception occurred during peer ping", x);
		}
		return response;
	}

}

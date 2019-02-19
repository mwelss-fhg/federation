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
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.acumos.cds.domain.MLPPeer;
import org.acumos.federation.gateway.common.API;
import org.acumos.federation.gateway.common.JsonResponse;
import org.acumos.federation.gateway.service.PeerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import io.swagger.annotations.ApiOperation;

@Controller
@RequestMapping(API.Roots.FEDERATION)
public class PeersController extends AbstractController {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	PeerService peerService;

	/**
	 * Basic built-in 'discovery' service.
	 * @param theHttpResponse
	 *            HttpServletResponse
	 * @return List of Published ML Solutions in JSON format.
	 */
	@CrossOrigin
	@PreAuthorize("hasAuthority('PEERS_ACCESS')")
	@ApiOperation(value = "Invoked by Peer Acumos to get a list of peers from local Acumos Instance .", response = MLPPeer.class, responseContainer = "List")
	@RequestMapping(value = { API.Paths.PEERS }, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<List<MLPPeer>> getPeers(
			/* HttpServletRequest theHttpRequest, */
			HttpServletResponse theHttpResponse) {

		JsonResponse<List<MLPPeer>> response = null;
		List<MLPPeer> peers = null;
		log.debug(API.Paths.PEERS);
		try {
			peers = peerService.getPeers(new ControllerContext());
			response = JsonResponse.<List<MLPPeer>> buildResponse()
														 .withMessage("peers")
														 .withContent(peers)
														 .build();
			theHttpResponse.setStatus(HttpServletResponse.SC_OK);
			log.debug("peers request provided {} peers.", peers == null ? 0 : peers.size());
		} 
		catch (Exception x) {
			response = JsonResponse.<List<MLPPeer>> buildErrorResponse()
														 .withError(x)
														 .build();
			theHttpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log.error("An error ooccured while fetching local peers", x);
		}
		return response;
	}

}

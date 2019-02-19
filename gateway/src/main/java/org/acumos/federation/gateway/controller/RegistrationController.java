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
import org.acumos.federation.gateway.common.JsonResponse;
import org.acumos.federation.gateway.service.PeerService;
import org.acumos.federation.gateway.service.ServiceException;
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

/**
 * Provide in-band federation handshake interface.
 */
@Controller
@RequestMapping(API.Roots.FEDERATION)
public class RegistrationController extends AbstractController {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private PeerService peerService;

	/**
	 * @param theHttpResponse
	 *            HttpServletResponse
	 * @return Request status information
	 */
	@CrossOrigin
	@PreAuthorize("hasAuthority(T(org.acumos.federation.gateway.security.Priviledge).REGISTRATION_ACCESS)")
	@ApiOperation(value = "Invoked by another Acumos Instance to request federation.", response = MLPPeer.class)
	@RequestMapping(value = { API.Paths.PEER_REGISTER }, method = RequestMethod.POST, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<MLPPeer> registerPeer(
			/* HttpServletRequest theHttpRequest, */
			HttpServletResponse theHttpResponse) {

		log.debug(API.Paths.PEER_REGISTER);
		JsonResponse<MLPPeer> response = null;
		ControllerContext context = new ControllerContext();
		try {
			MLPPeer peer = context.getPeer().getPeerInfo();
			peerService.registerPeer(peer);

			response = JsonResponse.<MLPPeer> buildResponse()
									.withMessage("registration request accepted")																								
									.withContent(peer)
									.build();
									
			theHttpResponse.setStatus(HttpServletResponse.SC_ACCEPTED);
			log.debug("peer registration request " + peer + " was accepted");
		}
		catch (ServiceException sx) {
			response = JsonResponse.<MLPPeer> buildErrorResponse()
									.withMessage(sx.getMessage())
									.build();
			theHttpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			log.error("A service error occured during peer registration", sx);
		}
		catch (Exception x) {
			response = JsonResponse.<MLPPeer> buildErrorResponse()
									.withError(x)
									.build();
			theHttpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log.error("An unexpected error occured during peer registration", x);
		}
		return response;
	}

	@CrossOrigin
	@PreAuthorize("isKnown && hasAuthority(T(org.acumos.federation.gateway.security.Priviledge).REGISTRATION_ACCESS)")
	@ApiOperation(value = "Invoked by another Acumos Instance to request federation termination.", response = MLPPeer.class)
	@RequestMapping(value = { API.Paths.PEER_UNREGISTER }, method = RequestMethod.POST, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<MLPPeer> unregisterPeer(
			/* HttpServletRequest theHttpRequest, */
			HttpServletResponse theHttpResponse) {

		log.debug(API.Paths.PEER_REGISTER);
		JsonResponse<MLPPeer> response = null;
		ControllerContext context = new ControllerContext();
		try {
			MLPPeer peer = context.getPeer().getPeerInfo();
			peerService.unregisterPeer(peer);

			response = JsonResponse.<MLPPeer> buildResponse()
									.withMessage("federation termination request accepted")																								
									.withContent(peer)
									.build();
									
			theHttpResponse.setStatus(HttpServletResponse.SC_ACCEPTED);
			log.debug("federation termination request from " + peer + " was registered");
		}
		catch (ServiceException sx) {
			response = JsonResponse.<MLPPeer> buildErrorResponse()
									.withMessage(sx.getMessage())
									.build();
			theHttpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			log.error("A service error occured during peer unregister", sx);
		}
		catch (Exception x) {
			response = JsonResponse.<MLPPeer> buildErrorResponse()
									.withError(x)
									.build();
			theHttpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log.error("An unexpected error occured during peer register", x);
		}
		return response;
	}

}

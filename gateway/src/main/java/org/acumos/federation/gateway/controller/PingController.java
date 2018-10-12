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
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.service.PeerService;
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
public class PingController extends AbstractController {

	private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private PeerService peerService;

	/**
	 * Ping service.
	 * @param theHttpResponse
	 *            HttpServletResponse
	 * @return List of Published ML Solutions in JSON format.
	 */
	@CrossOrigin
	@PreAuthorize("isActive")
	@ApiOperation(value = "Invoked by Peer Acumos to get status and self information.", response = MLPPeer.class)
	@RequestMapping(value = { API.Paths.PING }, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<MLPPeer> ping(
			/* HttpServletRequest theHttpRequest, */
			HttpServletResponse theHttpResponse) {

		JsonResponse<MLPPeer> response = new JsonResponse<MLPPeer>();
		log.debug(EELFLoggerDelegate.debugLogger, API.Paths.PING);
		try {
			MLPPeer self = peerService.getSelf();
			response = JsonResponse.<MLPPeer> buildResponse()
														.withMessage("ping")
														.withContent(self)
														.build();
			theHttpResponse.setStatus(HttpServletResponse.SC_OK);
		} 
		catch (Exception x) {
			response = JsonResponse.<MLPPeer> buildErrorResponse()
														 .withError(x)
														 .build();
			theHttpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log.error(EELFLoggerDelegate.errorLogger, "An error occurred while handling a ping request", x);
		}
		return response;
	}

}

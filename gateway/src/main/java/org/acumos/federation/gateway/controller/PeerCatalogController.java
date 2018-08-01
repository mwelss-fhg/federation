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
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.federation.gateway.common.API;
import org.acumos.federation.gateway.common.Clients;
import org.acumos.federation.gateway.common.JsonResponse;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.service.PeerService;
import org.acumos.federation.gateway.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.util.Base64Utils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import io.swagger.annotations.ApiOperation;

/**
 * Provides an interface for the local Acumos components to query a peer's catalog.
 * Limited to solution level information. 
 *
 */
@Controller
@RequestMapping(API.Roots.LOCAL)
public class PeerCatalogController extends AbstractController {

	private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private Clients clients;
	@Autowired
	private PeerService peerService;

	/**
	 * Provides access 
	 * @param theHttpResponse
	 *            HttpServletResponse
	 * @param theSelector
	 *            Solutions selector
	 * @return List of Published ML Solutions in JSON format.
	 */
	@CrossOrigin
	@PreAuthorize("hasAuthority(T(org.acumos.federation.gateway.security.Priviledge).PEER_ACCESS)")
	@ApiOperation(value = "Invoked by local Acumos to get a list of solutions available from a peer Acumos instance .", response = MLPSolution.class, responseContainer = "List")
	@RequestMapping(value = { API.Paths.SOLUTIONS }, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<List<MLPSolution>> getSolutions(
			/* HttpServletRequest theHttpRequest, */
			HttpServletResponse theHttpResponse,
			@PathVariable("peerId") String thePeerId,
			@RequestParam(value = API.QueryParameters.SOLUTIONS_SELECTOR, required = false) String theSelector) {
		log.debug(EELFLoggerDelegate.debugLogger, API.Roots.LOCAL + "" + API.Paths.SOLUTIONS);
		JsonResponse<List<MLPSolution>> response = null;
		try {
			MLPPeer peer = this.peerService.getPeerById(thePeerId);
			Map<String, Object> selector = null;
			if (theSelector != null)
				selector = Utils.jsonStringToMap(new String(Base64Utils.decodeFromString(theSelector), "UTF-8"));
			response = this.clients.getFederationClient(peer.getApiUrl()).getSolutions(selector);
			
			theHttpResponse.setStatus(HttpServletResponse.SC_OK);
		}
		//catch (ServiceException sx) {}
		catch (Exception x) {
			response = JsonResponse.<List<MLPSolution>> buildErrorResponse()
														 .withError(x)
														 .build();
			theHttpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log.error(EELFLoggerDelegate.errorLogger, "An error occurred while processing a local peer solutions request for peer " + thePeerId, x);
		}
		return response;
	}

	@CrossOrigin
	@PreAuthorize("hasAuthority(T(org.acumos.federation.gateway.security.Priviledge).PEER_ACCESS)")
	@ApiOperation(value = "Invoked by local Acumos to get detailed solution information from the catalog of a peer acumos Instance.", response = MLPSolution.class)
	@RequestMapping(value = { API.Paths.SOLUTION_DETAILS }, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<MLPSolution> getSolutionDetails(
			HttpServletResponse theHttpResponse,
			@PathVariable("peerId") String thePeerId,
			@PathVariable(value = "solutionId") String theSolutionId) {
		log.debug(EELFLoggerDelegate.debugLogger, API.Roots.LOCAL + "" + API.Paths.SOLUTION_DETAILS);
		JsonResponse<MLPSolution> response = null;
		try {
			MLPPeer peer = this.peerService.getPeerById(thePeerId);
			response = this.clients.getFederationClient(peer.getApiUrl()).getSolution(theSolutionId);
			
			theHttpResponse.setStatus(HttpServletResponse.SC_OK);
		}
		catch (Exception x) {
			response = JsonResponse.<MLPSolution> buildErrorResponse()
														 .withError(x)
														 .build();
			theHttpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log.error(EELFLoggerDelegate.errorLogger, "An error occurred while fetching solution " + theSolutionId + " from peer " + thePeerId, x);
		}
		return response;
	}
}

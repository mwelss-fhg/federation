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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import org.acumos.cds.domain.MLPPeer;
import org.acumos.federation.gateway.common.JSONTags;
import org.acumos.federation.gateway.common.JsonResponse;
import org.acumos.federation.gateway.common.API;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.service.PeerService;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.security.Peer;

import io.swagger.annotations.ApiOperation;

/**
 * 
 *
 */
@Controller
@RequestMapping("/")
public class PeerController extends AbstractController {
	
	
	@Autowired
	PeerService peerService;
	
	/**
	 * @param request
	 *            HttpServletRequest
	 * @param response
	 * 			HttpServletResponse
	 * @return List of Published ML Solutions in JSON format.
	 */
	@CrossOrigin
	@PreAuthorize("hasAuthority('PEERS_ACCESS')")
	@ApiOperation(value = "Invoked by Peer Acumos to get a list of peers from local Acumos Instance .", response = MLPPeer.class, responseContainer = "List")
	@RequestMapping(value = {API.Paths.PEERS}, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<List<MLPPeer>> getSolutions(
			/* HttpServletRequest theHttpRequest,*/
			HttpServletResponse theHttpResponse) {

		JsonResponse<List<MLPPeer>> response = null;
		List<MLPPeer> peers = null;
		log.debug(EELFLoggerDelegate.debugLogger, API.Paths.PEERS);
		try {
			response = new JsonResponse<List<MLPPeer>>();
			log.debug(EELFLoggerDelegate.debugLogger, "getPeers");

			peers = peerService.getPeers(new ControllerContext());
/*
 * TODO: We only expose simple peers, not the partners.
 * But we only serve this service
 * to parners so .. ?? No pb.
 */
			if(peers != null) {
				response.setResponseBody(peers);
				response.setResponseCode(String.valueOf(HttpServletResponse.SC_OK));
				response.setResponseDetail(JSONTags.TAG_STATUS_SUCCESS);
				response.setStatus(true);
				theHttpResponse.setStatus(HttpServletResponse.SC_OK);
				log.debug(EELFLoggerDelegate.debugLogger, "getPeers: size is " + peers.size());
			}
		} catch (Exception e) {
			response.setResponseCode(String.valueOf(HttpServletResponse.SC_BAD_REQUEST));
			response.setResponseDetail(JSONTags.TAG_STATUS_FAILURE);
			response.setStatus(false);
			theHttpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			log.error(EELFLoggerDelegate.errorLogger, "Exception Occurred Fetching Peers", e);
		}
		return response;
	}

	protected class ControllerContext implements ServiceContext {

		public Peer getPeer() {
			return (Peer)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		}
	}
}


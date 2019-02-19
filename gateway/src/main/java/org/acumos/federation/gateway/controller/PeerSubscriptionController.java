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
import org.acumos.federation.gateway.cds.PeerSubscription;
import org.acumos.federation.gateway.common.API;
import org.acumos.federation.gateway.common.JsonResponse;
import org.acumos.federation.gateway.service.PeerService;
import org.acumos.federation.gateway.service.PeerSubscriptionService;
import org.acumos.federation.gateway.task.PeerSubscriptionTaskScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import io.swagger.annotations.ApiOperation;

/**
 * Servivr offered to local components allowing then to trigger the execution of a subscription.
 */
@Controller
@RequestMapping(API.Roots.LOCAL)
public class PeerSubscriptionController extends AbstractController {
 
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private PeerService peerService;
	@Autowired
	private PeerSubscriptionService peerSubscriptionService;
	@Autowired
	private ApplicationContext appCtx;


	@CrossOrigin
	@PreAuthorize("hasAuthority(T(org.acumos.federation.gateway.security.Priviledge).PEER_ACCESS)")
	@ApiOperation(value = "Invoked by other Acumos components in order to trigger subscription execution", response = String.class)
	@RequestMapping(value = { API.Paths.SUBSCRIPTION }, method = RequestMethod.POST, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<String> triggerPeerSubscription(
			/* HttpServletRequest theHttpRequest, */
			HttpServletResponse theHttpResponse,
			@PathVariable("peerId") String thePeerId,
			@PathVariable("subscriptionId") Long theSubscriptionId) {

		log.debug(API.Roots.LOCAL + "" + API.Paths.SUBSCRIPTION);
		JsonResponse<String> response = null;
		try {
			log.debug("trigger");
	
			MLPPeer peer = this.peerService.getPeerById(thePeerId);
			PeerSubscription subscription = this.peerSubscriptionService.getPeerSubscription(theSubscriptionId);
			//coherence check
			//subscription.getPeerId().equals(thePeerId);

			((PeerSubscriptionTaskScheduler)this.appCtx.getBean("peerSubscriptionTaskScheduler")).runOnce(peer, subscription);

			response = JsonResponse.<String> buildResponse()
														.withContent("subscription execution triggered")
														.build();
			theHttpResponse.setStatus(HttpServletResponse.SC_OK);
			log.debug("subscription execution triggered");
		} 
		catch (Exception x) {
			response = JsonResponse.<String> buildErrorResponse()
														 .withError(x)
														 .build();
			theHttpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log.error("On-demand subscription execution failed", x);
		}
		return response;
	}

}

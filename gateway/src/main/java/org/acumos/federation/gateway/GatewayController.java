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
package org.acumos.federation.gateway;

import java.util.List;
import java.util.function.Function;
import java.lang.invoke.MethodHandles;

import javax.servlet.http.HttpServletResponse;

import io.swagger.annotations.ApiOperation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.security.access.annotation.Secured;

import org.acumos.cds.domain.MLPCatalog;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPPeerSubscription;

import org.acumos.federation.client.FederationClient;
import org.acumos.federation.client.GatewayClient;
import org.acumos.federation.client.data.JsonResponse;


/**
 * Controller bean for the internal (gateway) API.
 */
@Controller
@CrossOrigin
@Secured(Security.ROLE_INTERNAL)
@RequestMapping(GatewayClient.PEER_PFX)
public class GatewayController {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private Clients clients;

	@Autowired
	private PeerService peerService;

	@Autowired
	private SubscriptionPoller poller;

	@ApiOperation(value = "Invoked by local Acumos to get a list of catalogs available from a peer Acumos instance .", response = MLPCatalog.class, responseContainer = "List")
	@RequestMapping(value = FederationClient.CATALOGS_URI, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	public JsonResponse<List<MLPCatalog>> getCatalogs(
	    HttpServletResponse response,
	    @PathVariable("peerId") String peerId) {
		log.debug("/peer/{}/catalogs", peerId);
		return callPeer(response, peerId, FederationClient::getCatalogs);
	}

	@ApiOperation(value = "Invoked by local Acumos to get a list of solutions available from a peer Acumos instance .", response = MLPSolution.class, responseContainer = "List")
	@RequestMapping(value = FederationClient.SOLUTIONS_URI, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	public JsonResponse<List<MLPSolution>> getSolutions(
	    HttpServletResponse response,
	    @PathVariable("peerId") String peerId,
	    @RequestParam(value="catalogId", required=true) String catalogId) {
		log.debug("/peer/{}/solutions", peerId);
		return callPeer(response, peerId, peer -> peer.getSolutions(catalogId));
	}

	@ApiOperation(value = "Invoked by local Acumos to get detailed solution information from the catalog of a peer acumos Instance.", response = MLPSolution.class)
	@RequestMapping(value = FederationClient.SOLUTION_URI, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	public JsonResponse<MLPSolution> getSolution(
	    HttpServletResponse response,
	    @PathVariable("peerId") String peerId,
	    @PathVariable("solutionId") String solutionId) {
		log.debug("/peer/{}/solutions/{}", peerId, solutionId);
		return callPeer(response, peerId, peer -> peer.getSolution(solutionId));
	}

	@ApiOperation(value = "Invoked by local Acumos to get peers information from remote Acumos peer.", response = MLPPeer.class, responseContainer = "List")
	@RequestMapping(value = FederationClient.PEERS_URI, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	public JsonResponse<List<MLPPeer>> getPeers(
	    HttpServletResponse response,
	    @PathVariable("peerId") String peerId) {
		log.debug("/peer/{}/peers", peerId);
		return callPeer(response, peerId, FederationClient::getPeers);
	}

	@ApiOperation(value = "Invoked by local Acumos to get peer Acumos status and information.", response = MLPPeer.class)
	@RequestMapping(value = FederationClient.PING_URI, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	public JsonResponse<MLPPeer> ping(
	    HttpServletResponse response,
	    @PathVariable("peerId") String peerId) {
		log.debug("/peer/{}/ping", peerId);
		return callPeer(response, peerId, FederationClient::ping);
	}

	@ApiOperation(value = "Invoked by local Acumos to register with a remote Acumos peer.", response = MLPPeer.class)
	@RequestMapping(value = FederationClient.REGISTER_URI, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	public JsonResponse<MLPPeer> register(
	    HttpServletResponse response,
	    @PathVariable("peerId") String peerId) {
		log.debug("/peer/{}/peer/register", peerId);
		return callPeer(response, peerId, FederationClient::register);
	}

	@ApiOperation(value = "Invoked by other Acumos components in order to trigger subscription execution")
	@RequestMapping(value = GatewayClient.SUBSCRIPTION_URI, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	public JsonResponse<Void> triggerPeerSubscription(
	    HttpServletResponse response,
	    @PathVariable("peerId") String peerId,
	    @PathVariable("subscriptionId") long subscriptionId) {
		log.debug("/peer/{}/subscription/{}", peerId, subscriptionId);
		MLPPeerSubscription subscription = peerService.getSubscription(subscriptionId);
		JsonResponse<Void> ret = new JsonResponse();
		if (subscription == null || !peerId.equals(subscription.getPeerId())) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			ret.setMessage(String.format("No subscription with id %s found.", subscriptionId));
		} else {
			poller.triggerSubscription(subscription);
			response.setStatus(HttpServletResponse.SC_OK);
		}
		return ret;
	}

	private <T> JsonResponse<T> callPeer(HttpServletResponse response, String peerId, Function<FederationClient, T> fcn) {
		MLPPeer peer = peerService.getPeer(peerId);
		JsonResponse<T> ret = new JsonResponse();
		if (peer == null) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			ret.setMessage(String.format("No peer with id %s found.", peerId));
		} else {
			response.setStatus(HttpServletResponse.SC_OK);
			ret.setContent(fcn.apply(clients.getFederationClient(peer.getApiUrl())));
		}
		return ret;
	}
}

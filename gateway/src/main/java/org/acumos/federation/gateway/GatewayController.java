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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.method.P;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.acumos.cds.domain.MLPCatalog;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPPeerSubscription;

import org.acumos.federation.client.FederationClient;
import org.acumos.federation.client.GatewayClient;
import org.acumos.federation.client.data.JsonResponse;
import org.acumos.federation.client.data.ModelData;
import org.acumos.federation.client.data.ModelInfo;



/**
 * Controller bean for the internal (gateway) API.
 */
@Controller
@CrossOrigin
@Secured(Security.ROLE_INTERNAL)
@RequestMapping(value = GatewayClient.PEER_PFX, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class GatewayController {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private Clients clients;

	@Autowired
	private PeerService peerService;

	@Autowired
	private SubscriptionPoller poller;

	@Autowired
	private WebSecurityConfigurerAdapter security;

	@ApiOperation(value = "Invoked by local Acumos to get a list of catalogs available from a peer Acumos instance .", response = MLPCatalog.class, responseContainer = "List")
	@GetMapping(FederationClient.CATALOGS_URI)
	@ResponseBody
	public JsonResponse<List<MLPCatalog>> getCatalogs(
	    HttpServletResponse response,
	    @PathVariable("peerId") String peerId) {
		log.debug("/peer/{}/catalogs", peerId);
		return callPeer(response, peerId, FederationClient::getCatalogs);
	}

	@ApiOperation(value = "Invoked by local Acumos to get a list of solutions available from a peer Acumos instance .", response = MLPSolution.class, responseContainer = "List")
	@GetMapping(FederationClient.SOLUTIONS_URI)
	@ResponseBody
	public JsonResponse<List<MLPSolution>> getSolutions(
	    HttpServletResponse response,
	    @PathVariable("peerId") String peerId,
	    @RequestParam(value="catalogId", required=true) String catalogId) {
		log.debug("/peer/{}/solutions", peerId);
		return callPeer(response, peerId, peer -> peer.getSolutions(catalogId));
	}

	@ApiOperation(value = "Invoked by local Acumos to get detailed solution information from the catalog of a peer acumos Instance.", response = MLPSolution.class)
	@GetMapping(FederationClient.SOLUTION_URI)
	@ResponseBody
	public JsonResponse<MLPSolution> getSolution(
	    HttpServletResponse response,
	    @PathVariable("peerId") String peerId,
	    @PathVariable("solutionId") String solutionId) {
		log.debug("/peer/{}/solutions/{}", peerId, solutionId);
		return callPeer(response, peerId, peer -> peer.getSolution(solutionId));
	}

	@ApiOperation(value = "Invoked by local Acumos to get peers information from remote Acumos peer.", response = MLPPeer.class, responseContainer = "List")
	@GetMapping(FederationClient.PEERS_URI)
	@ResponseBody
	public JsonResponse<List<MLPPeer>> getPeers(
	    HttpServletResponse response,
	    @PathVariable("peerId") String peerId) {
		log.debug("/peer/{}/peers", peerId);
		return callPeer(response, peerId, FederationClient::getPeers);
	}

	@ApiOperation(value = "Invoked by local Acumos to get peer Acumos status and information.", response = MLPPeer.class)
	@GetMapping(FederationClient.PING_URI)
	@ResponseBody
	public JsonResponse<MLPPeer> ping(
	    HttpServletResponse response,
	    @PathVariable("peerId") String peerId) {
		log.debug("/peer/{}/ping", peerId);
		return callPeer(response, peerId, FederationClient::ping);
	}

	@ApiOperation(value = "Invoked by local Acumos to register with a remote Acumos peer.", response = MLPPeer.class)
	@PostMapping(FederationClient.REGISTER_URI)
	@ResponseBody
	public JsonResponse<MLPPeer> register(
	    HttpServletResponse response,
	    @PathVariable("peerId") String peerId) {
		log.debug("/peer/{}/peer/register", peerId);
		return callPeer(response, peerId, FederationClient::register);
	}

	@ApiOperation("Invoked by other Acumos components in order to trigger subscription execution")
	@PostMapping(GatewayClient.SUBSCRIPTION_URI)
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
	/**
	 * Receives incoming log message from logstash and Sends to {@link FederationController#receiveModelData(ModelData, HttpServletResponse)}
	 *
	 * @param payload model data payload The payload must have a model.solutionId
	 *
	 * @param theHttpResponse HttpServletResponse
	 * @param peerIdPathVar PeerID from url path param or USE_SOLUTION_SOURCE to lookup peer based on model.solutionId field
	 * @return success message in JSON format
	 *
	 */
	@Secured(Security.ROLE_PEER)
	@ApiOperation(
			value = "Invoked by local Acumos to post incoming model data to respective remote peer Acumos instance .",
			response = ModelData.class)
	@PostMapping(FederationClient.MODEL_DATA)
	@ResponseBody
	public JsonResponse<Void> peerModelData(HttpServletResponse theHttpResponse,
	    @RequestBody ModelData payload, @PathVariable("peerId") String peerIdPathVar) {
		log.debug("/peer/{}/modeldata  payload: {}", peerIdPathVar, payload);
		ModelInfo modelInfo = payload.getModel();
		String peerId = peerIdPathVar;
		JsonResponse response = new JsonResponse();
		// peer id lookup from solution if peerid from path variable is null
		if(peerId.indexOf("USE_SOLUTION_SOURCE") != -1){
			String solutionId = modelInfo.getSolutionId();
			peerId = getPeerIdFromCds(solutionId);
		}
		
		MLPPeer self = ((Security) security).getSelf();
		modelInfo.setSubscriberName(self.getSubjectName());
	
		try {

			// check if thePeerId matches to the
			// Ignore request if for local peer i.e. peerId same as local peer
			//
			log.debug("Attempting to connect to peer id {}", peerId);
			if (peerId == null) {
				log.debug("ignore logging to self-peer {}", peerId);
				return this.getSuccessResponse(theHttpResponse,
						"ignore logging to self-peer");
			}

			log.debug("calling peer with request {}", payload);
			callPeer(theHttpResponse, peerId, peer -> peer.receiveModelData(payload));
		} catch (Exception ex) {
			log.error("failed posting to remote peerId:" + peerId + " exception {}", ex);
			throw new BadRequestException(HttpServletResponse.SC_BAD_GATEWAY, "failed posting to remote peerId:" + peerId);
		}
		return response;

	}

	private JsonResponse getSuccessResponse(
		HttpServletResponse theHttpResponse,
		String message) {
		JsonResponse response = new JsonResponse();
		response.setMessage("modelData - " + message);
		return response;
	}

	private String getPeerIdFromCds(String solutionId) {
		try {
			String peerId = clients.getCDSClient().getSolution(solutionId).getSourceId();
			return peerId;
		} catch (RestClientResponseException ex) {
			log.error("getSolution failed, server reports: {}", ex);
			throw new BadRequestException(HttpServletResponse.SC_NOT_FOUND, "Not Found");
		}
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

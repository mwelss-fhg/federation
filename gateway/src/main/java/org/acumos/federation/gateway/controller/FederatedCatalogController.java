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
import org.springframework.stereotype.Controller;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.federation.gateway.common.JSONTags;
import org.acumos.federation.gateway.common.JsonResponse;
import org.acumos.federation.gateway.config.APINames;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.service.FederatedCatalogService;

import io.swagger.annotations.ApiOperation;

/**
 * 
 *
 */
@Controller
@RequestMapping("/")
public class FederatedCatalogController extends AbstractController {
	
	private final EELFLoggerDelegate logger = EELFLoggerDelegate.getLogger(FederatedCatalogController.class);
	
	@Autowired
	FederatedCatalogService federationGatewayService;
	
	
//	/**
//	 * @param request
//	 *            HttpServletRequest
//	 * @param response
//	 * 			HttpServletResponse
//	 * @return List of Published ML Solutions in JSON format.
//	 */
//	@CrossOrigin
//	@ApiOperation(value = "Invoked by Peer Acumos to get a Paginated list of Published Solutions from the Catalog of the local Acumos Instance .", response = MLPSolution.class, responseContainer = "Page")
//	@RequestMapping(value = {APINames.PEER_SOLUTIONS}, method = RequestMethod.GET, produces = APPLICATION_JSON)
//	@ResponseBody
//	public JsonResponse<RestPageResponse<MLPSolution>> getSolutionsListFromPeer(HttpServletRequest request, HttpServletResponse response,
//			@RequestParam("pageNumber") Integer pageNumber, @RequestParam("maxSize") Integer maxSize, 
//			@RequestParam(required = false) String sortingOrder, @RequestParam(required = false) String mlpModelTypes) {
//		JsonResponse<RestPageResponse<MLPSolution>> data = null;
//		RestPageResponse<MLPSolution> peerCatalogSolutions = null;
//		try {
//			data = new JsonResponse<RestPageResponse<MLPSolution>>();
//			peerCatalogSolutions = federationGatewayService.getPeerCatalogSolutions(pageNumber, maxSize, sortingOrder, null);
//			if(peerCatalogSolutions != null) {
//				data.setResponseBody(peerCatalogSolutions);
//				logger.debug(EELFLoggerDelegate.debugLogger, "getSolutionsListFromPeer: size is {} ");
//			}
//		} catch (Exception e) {
//			logger.error(EELFLoggerDelegate.errorLogger, "Exception Occurred Fetching Solutions for Market Place Catalog", e);
//		}
//		return data;
//	}
	
	/**
	 * @param request
	 *            HttpServletRequest
	 * @param response
	 * 			HttpServletResponse
	 * @return List of Published ML Solutions in JSON format.
	 */
	@CrossOrigin
	@PreAuthorize("hasAuthority('PEER')")
	@ApiOperation(value = "Invoked by Peer Acumos to get a list of Published Solutions from the Catalog of the local Acumos Instance .", response = MLPSolution.class, responseContainer = "List")
	@RequestMapping(value = {APINames.PEER_SOLUTIONS}, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<List<MLPSolution>> getSolutionsListFromPeer(HttpServletRequest request, HttpServletResponse response, 
			@RequestParam(value = "modelTypeCode", required = false) String mlpModelTypes) {
		JsonResponse<List<MLPSolution>> data = null;
		List<MLPSolution> peerCatalogSolutions = null;
		try {
			data = new JsonResponse<List<MLPSolution>>();
			logger.debug(EELFLoggerDelegate.debugLogger, "getSolutionsListFromPeer: model types " + mlpModelTypes);
			peerCatalogSolutions = federationGatewayService.getPeerCatalogSolutionsList(mlpModelTypes);
			if(peerCatalogSolutions != null) {
				data.setResponseBody(peerCatalogSolutions);
				data.setResponseCode(String.valueOf(HttpServletResponse.SC_OK));
				data.setResponseDetail(JSONTags.TAG_STATUS_SUCCESS);
				data.setStatus(true);
				response.setStatus(HttpServletResponse.SC_OK);
				logger.debug(EELFLoggerDelegate.debugLogger, "getSolutionsListFromPeer: size is " + peerCatalogSolutions.size());
			}
		} catch (Exception e) {
			data.setResponseCode(String.valueOf(HttpServletResponse.SC_BAD_REQUEST));
			data.setResponseDetail(JSONTags.TAG_STATUS_FAILURE);
			data.setStatus(false);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			logger.error(EELFLoggerDelegate.errorLogger, "Exception Occurred Fetching Solutions for Market Place Catalog", e);
		}
		return data;
	}
	
	/**
	 * @param request
	 *            HttpServletRequest
	 * @param response
	 * 			HttpServletResponse
	 * @return List of Published ML Solutions in JSON format.
	 */
	@CrossOrigin
	@PreAuthorize("hasAuthority('PEER')")
	@ApiOperation(value = "Invoked by Peer Acumos to get a list of Solution Revision from the Catalog of the local Acumos Instance .", response = MLPSolutionRevision.class, responseContainer = "List")
	@RequestMapping(value = {APINames.PEER_SOLUTIONS_REVISIONS}, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<List<MLPSolutionRevision>> getSolutionsRevisionListFromPeer(HttpServletRequest request, HttpServletResponse response, 
			@PathVariable("solutionId") String solutionId) {
		JsonResponse<List<MLPSolutionRevision>> data = null;
		List<MLPSolutionRevision> peerCatalogSolutionRevisions= null;
		try {
			data = new JsonResponse<List<MLPSolutionRevision>>();
			peerCatalogSolutionRevisions = federationGatewayService.getPeerCatalogSolutionRevision(solutionId);
			if(peerCatalogSolutionRevisions != null) {
				data.setResponseBody(peerCatalogSolutionRevisions);
				data.setResponseCode(String.valueOf(HttpServletResponse.SC_OK));
				data.setResponseDetail(JSONTags.TAG_STATUS_SUCCESS);
				data.setStatus(true);
				response.setStatus(HttpServletResponse.SC_OK);
				logger.debug(EELFLoggerDelegate.debugLogger, "getSolutionsRevisionListFromPeer: size is {} ", peerCatalogSolutionRevisions.size());
			}
		} catch (Exception e) {
			data.setResponseCode(String.valueOf(HttpServletResponse.SC_BAD_REQUEST));
			data.setResponseDetail(JSONTags.TAG_STATUS_FAILURE);
			data.setStatus(false);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			logger.error(EELFLoggerDelegate.errorLogger, "Exception Occurred Fetching Solution Revisions for Market Place Catalog", e);
		}
		return data;
	}
	
	/**
	 * @param request
	 *            HttpServletRequest
	 * @param response
	 * 			HttpServletResponse
	 * @return List of Published ML Solutions in JSON format.
	 */
	@CrossOrigin
	@PreAuthorize("hasAuthority('PEER')")
	@ApiOperation(value = "Invoked by Peer Acumos to get a list of Solution Revision Artifacts from the Catalog of the local Acumos Instance .", response = MLPArtifact.class, responseContainer = "List")
	@RequestMapping(value = {APINames.PEER_SOLUTIONS_REVISIONS_ARTIFACTS}, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<List<MLPArtifact>> getSolutionsRevisionArtifactListFromPeer(HttpServletRequest request, HttpServletResponse response, 
			@PathVariable("solutionId") String solutionId, @PathVariable("revisionId") String revisionId) {
		JsonResponse<List<MLPArtifact>> data = null;
		List<MLPArtifact> peerSolutionArtifacts= null;
		try {
			data = new JsonResponse<List<MLPArtifact>>();
			peerSolutionArtifacts = federationGatewayService.getPeerSolutionArtifacts(solutionId, revisionId);
			if(peerSolutionArtifacts != null) {
				//re-encode the artifact uri
				{
		      UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(request.getRequestURL().toString());

					for (MLPArtifact artifact: peerSolutionArtifacts) {
						artifact.setUri(uriBuilder.replacePath("/artifacts/" + artifact.getArtifactId() + "/download")
																			.toUriString());
					}
				}

				data.setResponseBody(peerSolutionArtifacts);
				data.setResponseCode(String.valueOf(HttpServletResponse.SC_OK));
				data.setResponseDetail(JSONTags.TAG_STATUS_SUCCESS);
				data.setStatus(true);
				response.setStatus(HttpServletResponse.SC_OK);
				logger.debug(EELFLoggerDelegate.debugLogger, "getSolutionsRevisionArtifactListFromPeer: size is {} ", peerSolutionArtifacts.size());
			}
		} catch (Exception e) {
			data.setResponseCode(String.valueOf(HttpServletResponse.SC_BAD_REQUEST));
			data.setResponseDetail(JSONTags.TAG_STATUS_FAILURE);
			data.setStatus(false);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			logger.error(EELFLoggerDelegate.errorLogger, "Exception Occurred Fetching Solution Revisions Artifacts for Market Place Catalog", e);
		}
		return data;
	}
	
	/**
	 * @param request
	 *            HttpServletRequest
	 * @param response
	 * 			HttpServletResponse
	 * @return Archive file of the Artifact for the Solution.
	 */
	@CrossOrigin
	@PreAuthorize("hasAuthority('PEER')")
	@ApiOperation(value = "API to download the Machine Learning Artifact of the Machine Learning Solution", response = InputStreamResource.class, code = 200)
	@RequestMapping(value = {APINames.PEER_ARTIFACT_download}, method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ResponseBody
	public InputStreamResource downloadSolutionArtifact(@PathVariable("artifactId") String artifactId,
    		HttpServletRequest request, HttpServletResponse response) {
		InputStreamResource inputStreamResource = null;
		try {
			inputStreamResource = federationGatewayService.getPeerSolutionArtifactFile(artifactId);
			//TODO : Need to Implement a logic to download a Artifact or Docker Image from Nexus
			response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
			response.setHeader("Pragma", "no-cache");
			response.setHeader("Expires", "0");
			response.setStatus(HttpServletResponse.SC_OK);
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			logger.error(EELFLoggerDelegate.errorLogger, "Exception Occurred downloading a artifact for a Solution in Market Place Catalog", e);
		}
		return inputStreamResource;
	}
}


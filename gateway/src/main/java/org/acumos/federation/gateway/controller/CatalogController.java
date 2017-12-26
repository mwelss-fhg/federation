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

import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.federation.gateway.common.JSONTags;
import org.acumos.federation.gateway.common.JsonResponse;
import org.acumos.federation.gateway.config.APINames;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.service.CatalogService;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.security.Peer;

import io.swagger.annotations.ApiOperation;

/**
 * 
 *
 */
@Controller
@RequestMapping("/")
public class CatalogController extends AbstractController {
	
	
	@Autowired
	CatalogService catalogService;
	
	
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
//@PreAuthorize("hasAuthority('PEER')"
	@PreAuthorize("hasAuthority(T(org.acumos.federation.gateway.security.Priviledge).CATALOG_ACCESS)")
	@ApiOperation(value = "Invoked by Peer Acumos to get a list of Published Solutions from the Catalog of the local Acumos Instance .", response = MLPSolution.class, responseContainer = "List")
	@RequestMapping(value = {APINames.PEER_SOLUTIONS}, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<List<MLPSolution>> getSolutions(
			/* HttpServletRequest theHttpRequest,*/
			HttpServletResponse theHttpResponse,
			@RequestParam(value = "modelTypeCode", required = false) String mlpModelTypes) {
		JsonResponse<List<MLPSolution>> response = null;
		List<MLPSolution> peerCatalogSolutions = null;
		log.debug(EELFLoggerDelegate.debugLogger, APINames.PEER_SOLUTIONS);
		try {
			response = new JsonResponse<List<MLPSolution>>();
			log.debug(EELFLoggerDelegate.debugLogger, "getSolutionsListFromPeer: model types " + mlpModelTypes);
			peerCatalogSolutions = catalogService.getSolutions(mlpModelTypes, new ControllerContext());
			if(peerCatalogSolutions != null) {
				response.setResponseBody(peerCatalogSolutions);
				response.setResponseCode(String.valueOf(HttpServletResponse.SC_OK));
				response.setResponseDetail(JSONTags.TAG_STATUS_SUCCESS);
				response.setStatus(true);
				theHttpResponse.setStatus(HttpServletResponse.SC_OK);
				log.debug(EELFLoggerDelegate.debugLogger, "getSolutions: size is " + peerCatalogSolutions.size());
			}
		} catch (Exception e) {
			response.setResponseCode(String.valueOf(HttpServletResponse.SC_BAD_REQUEST));
			response.setResponseDetail(JSONTags.TAG_STATUS_FAILURE);
			response.setStatus(false);
			theHttpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			log.error(EELFLoggerDelegate.errorLogger, "Exception occurred fetching Solutions for Market Place Catalog", e);
		}
		return response;
	}

	@CrossOrigin
	@PreAuthorize("hasAuthority('CATALOG_ACCESS')")
	@ApiOperation(value = "Invoked by Peer Acumos to get a list detailed solution information from the Catalog of the local Acumos Instance .", response = MLPSolution.class)
	@RequestMapping(value = {APINames.PEER_SOLUTION_DETAILS}, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<MLPSolution> getSolutionDetails(
			HttpServletResponse theHttpResponse,
			@PathVariable(value="solutionId") String theSolutionId) {
		JsonResponse<MLPSolution> response = null;
		MLPSolution solution = null;
		log.debug(EELFLoggerDelegate.debugLogger, APINames.PEER_SOLUTION_DETAILS + ": " + theSolutionId);
		try {
			response = new JsonResponse<MLPSolution>();
			solution = catalogService.getSolution(theSolutionId, new ControllerContext());
			if(solution != null) {
				response.setResponseBody(solution);
				response.setResponseCode(String.valueOf(HttpServletResponse.SC_OK));
				response.setResponseDetail(JSONTags.TAG_STATUS_SUCCESS);
				response.setStatus(true);
				theHttpResponse.setStatus(HttpServletResponse.SC_OK);
			}
		}
		catch (Exception e) {
			response.setResponseCode(String.valueOf(HttpServletResponse.SC_BAD_REQUEST));
			response.setResponseDetail(JSONTags.TAG_STATUS_FAILURE);
			response.setStatus(false);
			theHttpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			log.error(EELFLoggerDelegate.errorLogger, "An error occurred fetching solution " + theSolutionId, e);
		}
		return response;
	}


	
	/**
	 * @param request
	 *            HttpServletRequest
	 * @param response
	 * 			HttpServletResponse
	 * @return List of Published ML Solutions in JSON format.
	 */
	@CrossOrigin
	@PreAuthorize("hasAuthority('CATALOG_ACCESS')")
	@ApiOperation(value = "Invoked by Peer Acumos to get a list of Solution Revision from the Catalog of the local Acumos Instance .", response = MLPSolutionRevision.class, responseContainer = "List")
	@RequestMapping(value = {APINames.PEER_SOLUTION_REVISIONS}, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<List<MLPSolutionRevision>> getSolutionRevisions(
			HttpServletResponse theHttpResponse,
			@PathVariable("solutionId") String theSolutionId) {
		JsonResponse<List<MLPSolutionRevision>> response = null;
		List<MLPSolutionRevision> solutionRevisions= null;
		log.debug(EELFLoggerDelegate.debugLogger, APINames.PEER_SOLUTION_REVISIONS);
		try {
			response = new JsonResponse<List<MLPSolutionRevision>>();
			solutionRevisions = catalogService.getSolutionRevisions(theSolutionId, new ControllerContext());
			if(solutionRevisions != null) {
				response.setResponseBody(solutionRevisions);
				response.setResponseCode(String.valueOf(HttpServletResponse.SC_OK));
				response.setResponseDetail(JSONTags.TAG_STATUS_SUCCESS);
				response.setStatus(true);
				theHttpResponse.setStatus(HttpServletResponse.SC_OK);
				log.debug(EELFLoggerDelegate.debugLogger, "getSolutionsRevisions: size is {} ", solutionRevisions.size());
			}
		}
		catch (Exception e) {
			response.setResponseCode(String.valueOf(HttpServletResponse.SC_BAD_REQUEST));
			response.setResponseDetail(JSONTags.TAG_STATUS_FAILURE);
			response.setStatus(false);
			theHttpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			log.error(EELFLoggerDelegate.errorLogger, "Exception Occurred Fetching Solution Revisions", e);
		}
		return response;
	}

 /**
	 * @param request
	 *            HttpServletRequest
	 * @param response
	 * 			HttpServletResponse
	 * @return List of Published ML Solutions in JSON format.
	 */
	@CrossOrigin
	@PreAuthorize("hasAuthority('CATALOG_ACCESS')")
	@ApiOperation(value = "Invoked by Peer Acumos to get Solution Revision details from the Catalog of the local Acumos Instance .", response = MLPSolutionRevision.class)
	@RequestMapping(value = {APINames.PEER_SOLUTION_REVISION_DETAILS}, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<MLPSolutionRevision> getSolutionRevisionDetails(
			HttpServletResponse theHttpResponse,
			@PathVariable("solutionId") String theSolutionId,
			@PathVariable("revisionId") String theRevisionId) {
		JsonResponse<MLPSolutionRevision> response = null;
		MLPSolutionRevision solutionRevision= null;
		log.debug(EELFLoggerDelegate.debugLogger, APINames.PEER_SOLUTION_REVISION_DETAILS);
		try {
			response = new JsonResponse<MLPSolutionRevision>();
			solutionRevision = catalogService.getSolutionRevision(theSolutionId, theRevisionId, new ControllerContext());
			if(solutionRevision != null) {
				response.setResponseBody(solutionRevision);
				response.setResponseCode(String.valueOf(HttpServletResponse.SC_OK));
				response.setResponseDetail(JSONTags.TAG_STATUS_SUCCESS);
				response.setStatus(true);
				theHttpResponse.setStatus(HttpServletResponse.SC_OK);
			}
		}
		catch (Exception e) {
			response.setResponseCode(String.valueOf(HttpServletResponse.SC_BAD_REQUEST));
			response.setResponseDetail(JSONTags.TAG_STATUS_FAILURE);
			response.setStatus(false);
			theHttpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			log.error(EELFLoggerDelegate.errorLogger, "Exception Occurred Fetching Solution Revision", e);
		}
		return response;
	}
	
	/**
	 * @param request
	 *            HttpServletRequest
	 * @param response
	 * 			HttpServletResponse
	 * @return List of Published ML Solutions in JSON format.
	 */
	@CrossOrigin
	@PreAuthorize("hasAuthority('CATALOG_ACCESS')")
	@ApiOperation(value = "Invoked by Peer Acumos to get a list of Solution Revision Artifacts from the Catalog of the local Acumos Instance .", response = MLPArtifact.class, responseContainer = "List")
	@RequestMapping(value = {APINames.PEER_SOLUTION_REVISION_ARTIFACTS}, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<List<MLPArtifact>> getSolutionRevisionArtifacts (
			HttpServletRequest theHttpRequest,
			HttpServletResponse theHttpResponse,
			@PathVariable("solutionId") String theSolutionId,
			@PathVariable("revisionId") String theRevisionId) {
		JsonResponse<List<MLPArtifact>> response = null;
		List<MLPArtifact> solutionRevisionArtifacts= null;
		log.debug(EELFLoggerDelegate.debugLogger, APINames.PEER_SOLUTION_REVISION_ARTIFACTS + ":" + theSolutionId + ":" + theRevisionId);
		try {
			response = new JsonResponse<List<MLPArtifact>>();
			solutionRevisionArtifacts = catalogService.getSolutionRevisionArtifacts(theSolutionId, theRevisionId, new ControllerContext());
			if(solutionRevisionArtifacts != null) {
				//re-encode the artifact uri
				{
		      UriComponentsBuilder uriBuilder = 
						UriComponentsBuilder.fromHttpUrl(theHttpRequest.getRequestURL().toString());

					for (MLPArtifact artifact: solutionRevisionArtifacts) {
						artifact.setUri(uriBuilder.replacePath("/artifacts/" + artifact.getArtifactId() + "/download")
																			.toUriString());
					}
				}

				response.setResponseBody(solutionRevisionArtifacts);
				response.setResponseCode(String.valueOf(HttpServletResponse.SC_OK));
				response.setResponseDetail(JSONTags.TAG_STATUS_SUCCESS);
				response.setStatus(true);
				theHttpResponse.setStatus(HttpServletResponse.SC_OK);
				log.debug(EELFLoggerDelegate.debugLogger, "getSolutionRevisionArtifacts: size is {} ", solutionRevisionArtifacts.size());
			}
		}
		catch (Exception e) {
			response.setResponseCode(String.valueOf(HttpServletResponse.SC_BAD_REQUEST));
			response.setResponseDetail(JSONTags.TAG_STATUS_FAILURE);
			response.setStatus(false);
			theHttpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			log.error(EELFLoggerDelegate.errorLogger, "Exception Occurred Fetching Solution Revisions Artifacts for Market Place Catalog", e);
		}
		return response;
	}
	
	/**
	 * @param request
	 *            HttpServletRequest
	 * @param response
	 * 			HttpServletResponse
	 * @return Archive file of the Artifact for the Solution.
	 */
	@CrossOrigin
	@PreAuthorize("hasAuthority('CATALOG_ACCESS')")
	@ApiOperation(value = "API to download the Machine Learning Artifact of the Machine Learning Solution", response = InputStreamResource.class, code = 200)
	@RequestMapping(value = {APINames.PEER_ARTIFACT_DOWNLOAD}, method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ResponseBody
	public InputStreamResource downloadSolutionArtifact(
			HttpServletRequest theHttpRequest,
			HttpServletResponse theHttpResponse,
			@PathVariable("artifactId") String theArtifactId) {
		InputStreamResource inputStreamResource = null;
		try {
			inputStreamResource = catalogService.getSolutionRevisionArtifactContent(theArtifactId, new ControllerContext());
			//TODO : Need to Implement a logic to download a Artifact or Docker Image from Nexus
			theHttpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
			theHttpResponse.setHeader("Pragma", "no-cache");
			theHttpResponse.setHeader("Expires", "0");
			theHttpResponse.setStatus(HttpServletResponse.SC_OK);
		}
		catch (Exception e) {
			theHttpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			log.error(EELFLoggerDelegate.errorLogger, "Exception Occurred downloading a artifact for a Solution in Market Place Catalog", e);
		}
		return inputStreamResource;
	}

	/**
	 */
	protected class ControllerContext implements ServiceContext {

		public Peer getPeer() {
			return (Peer)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		}
	}
}


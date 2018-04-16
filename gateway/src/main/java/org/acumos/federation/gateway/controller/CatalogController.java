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

import java.net.URI;
import java.net.URISyntaxException;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.federation.gateway.common.API;
import org.acumos.federation.gateway.common.JSONTags;
import org.acumos.federation.gateway.common.JsonResponse;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.security.Peer;
import org.acumos.federation.gateway.service.CatalogService;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
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
 * 
 *
 */
@Controller
@RequestMapping(API.Roots.FEDERATION)
public class CatalogController extends AbstractController {

	private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(CatalogController.class.getName());

	@Autowired
	private CatalogService catalogService;

	/**
	 * @param theHttpResponse
	 *            HttpServletResponse
	 * @param theSelector
	 *            Solutions selector
	 * @return List of Published ML Solutions in JSON format.
	 */
	@CrossOrigin
	// @PreAuthorize("hasAuthority('PEER')"
	@PreAuthorize("hasAuthority(T(org.acumos.federation.gateway.security.Priviledge).CATALOG_ACCESS)")
	@ApiOperation(value = "Invoked by Peer Acumos to get a list of Published Solutions from the Catalog of the local Acumos Instance .", response = MLPSolution.class, responseContainer = "List")
	@RequestMapping(value = { API.Paths.SOLUTIONS }, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<List<MLPSolution>> getSolutions(
			HttpServletRequest theHttpRequest,
			HttpServletResponse theHttpResponse,
			@RequestParam(value = API.QueryParameters.SOLUTIONS_SELECTOR, required = false) String theSelector) {
		JsonResponse<List<MLPSolution>> response = null;
		List<MLPSolution> solutions = null;
		log.debug(EELFLoggerDelegate.debugLogger, API.Paths.SOLUTIONS);
		try {
			log.debug(EELFLoggerDelegate.debugLogger, "getSolutionsListFromPeer: selector " + theSelector);
			Map<String, ?> selector = null;
			if (theSelector != null)
				selector = Utils.jsonStringToMap(new String(Base64Utils.decodeFromString(theSelector), "UTF-8"));

			solutions = catalogService.getSolutions(selector, new ControllerContext());
			if (solutions != null) {
				for (MLPSolution solution: solutions) {
					encodeSolution(solution, theHttpRequest);
				}
			}

			response = JsonResponse.<List<MLPSolution>> buildResponse()
														.withMessage("available public solution for given filter")
														.withContent(solutions)
														.build();
			theHttpResponse.setStatus(HttpServletResponse.SC_OK);
			log.debug(EELFLoggerDelegate.debugLogger, "getSolutions: provided {} solutions", solutions == null ? 0 : solutions.size());
		}
		catch (Exception x) {
			response = JsonResponse.<List<MLPSolution>> buildErrorResponse()
														 .withError(x)
														 .build();
			theHttpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log.error(EELFLoggerDelegate.errorLogger, "Exception occurred while fetching solutions", x);
		}
		return response;
	}

	@CrossOrigin
	@PreAuthorize("hasAuthority('CATALOG_ACCESS')")
	@ApiOperation(value = "Invoked by Peer Acumos to get a list detailed solution information from the Catalog of the local Acumos Instance .", response = MLPSolution.class)
	@RequestMapping(value = { API.Paths.SOLUTION_DETAILS }, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<MLPSolution> getSolutionDetails(
			HttpServletRequest theHttpRequest,
			HttpServletResponse theHttpResponse,
			@PathVariable(value = "solutionId") String theSolutionId) {
		JsonResponse<MLPSolution> response = null;
		MLPSolution solution = null;
		log.debug(EELFLoggerDelegate.debugLogger, API.Paths.SOLUTION_DETAILS + ": " + theSolutionId);
		try {
			solution = catalogService.getSolution(theSolutionId, new ControllerContext());
			encodeSolution(solution, theHttpRequest);
			response = JsonResponse.<MLPSolution> buildResponse()
														.withMessage("solution details")
														.withContent(solution)
														.build();
			theHttpResponse.setStatus(HttpServletResponse.SC_OK);
		}
		catch (Exception x) {
			response = JsonResponse.<MLPSolution> buildErrorResponse()
														 .withError(x)
														 .build();
			theHttpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log.error(EELFLoggerDelegate.errorLogger, "An error occurred while fetching solution " + theSolutionId, x);
		}
		return response;
	}

	/**
	 * @param theSolutionId
	 *            Solution ID
	 * @param theHttpResponse
	 *            HttpServletResponse
	 * @return List of Published ML Solutions in JSON format.
	 */
	@CrossOrigin
	@PreAuthorize("hasAuthority('CATALOG_ACCESS')")
	@ApiOperation(value = "Invoked by Peer Acumos to get a list of Solution Revision from the Catalog of the local Acumos Instance .", response = MLPSolutionRevision.class, responseContainer = "List")
	@RequestMapping(value = { API.Paths.SOLUTION_REVISIONS }, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<List<MLPSolutionRevision>> getSolutionRevisions(HttpServletResponse theHttpResponse,
			@PathVariable("solutionId") String theSolutionId) {
		JsonResponse<List<MLPSolutionRevision>> response = null;
		List<MLPSolutionRevision> solutionRevisions = null;
		log.debug(EELFLoggerDelegate.debugLogger, API.Paths.SOLUTION_REVISIONS);
		try {
			solutionRevisions = catalogService.getSolutionRevisions(theSolutionId, new ControllerContext());
			response = JsonResponse.<List<MLPSolutionRevision>> buildResponse()
														.withMessage("solution revisions")
														.withContent(solutionRevisions)
														.build();
			theHttpResponse.setStatus(HttpServletResponse.SC_OK);
			log.debug(EELFLoggerDelegate.debugLogger, "getSolutionsRevisions for solution {} provided {} revisions",
						theSolutionId, solutionRevisions == null ? 0 : solutionRevisions.size());
		}
		catch (Exception x) {
			response = JsonResponse.<List<MLPSolutionRevision>> buildErrorResponse()
														 .withError(x)
														 .build();
			theHttpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log.error(EELFLoggerDelegate.errorLogger, "An error occured while fetching solution " + theSolutionId + " revisions", x);
		}
		return response;
	}

	/**
	 * 
	 * @param theSolutionId
	 *            Solution ID
	 * @param theRevisionId
	 *            Revision ID
	 * @param theHttpResponse
	 *            HttpServletResponse
	 * @return List of Published ML Solutions in JSON format.
	 */
	@CrossOrigin
	@PreAuthorize("hasAuthority('CATALOG_ACCESS')")
	@ApiOperation(value = "Invoked by peer Acumos to get solution revision details from the local Acumos Instance .", response = MLPSolutionRevision.class)
	@RequestMapping(value = {
			API.Paths.SOLUTION_REVISION_DETAILS }, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<MLPSolutionRevision> getSolutionRevisionDetails(HttpServletResponse theHttpResponse,
			@PathVariable("solutionId") String theSolutionId, @PathVariable("revisionId") String theRevisionId) {
		JsonResponse<MLPSolutionRevision> response = null;
		MLPSolutionRevision solutionRevision = null;
		log.debug(EELFLoggerDelegate.debugLogger,
				API.Paths.SOLUTION_REVISION_DETAILS + "(" + theSolutionId + "," + theRevisionId + ")");
		try {
			solutionRevision = catalogService.getSolutionRevision(theSolutionId, theRevisionId,
					new ControllerContext());
			response = JsonResponse.<MLPSolutionRevision> buildResponse()
														.withMessage("solution revision details")
														.withContent(solutionRevision)
														.build();
			theHttpResponse.setStatus(HttpServletResponse.SC_OK);
		}
		catch (Exception x) {
			response = JsonResponse.<MLPSolutionRevision> buildErrorResponse()
														 .withError(x)
														 .build();
			theHttpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log.error(EELFLoggerDelegate.errorLogger, "An error occured while fetching solution " + theSolutionId + " revision " + theRevisionId + " details", x);
		}
		return response;
	}

	/**
	 * @param theHttpRequest
	 *            HttpServletRequest
	 * @param theHttpResponse
	 *            HttpServletResponse
	 * @param theSolutionId
	 *            Solution ID
	 * @param theRevisionId
	 *            Revision ID
	 * @return List of Published ML Solutions in JSON format.
	 */
	@CrossOrigin
	@PreAuthorize("hasAuthority('CATALOG_ACCESS')")
	@ApiOperation(value = "Invoked by Peer Acumos to get a list of solution revision artifacts from the local Acumos Instance .", response = MLPArtifact.class, responseContainer = "List")
	@RequestMapping(value = {
			API.Paths.SOLUTION_REVISION_ARTIFACTS }, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<List<MLPArtifact>> getSolutionRevisionArtifacts(HttpServletRequest theHttpRequest,
			HttpServletResponse theHttpResponse, @PathVariable("solutionId") String theSolutionId,
			@PathVariable("revisionId") String theRevisionId) {
		JsonResponse<List<MLPArtifact>> response = null;
		List<MLPArtifact> solutionRevisionArtifacts = null;
		ControllerContext context = new ControllerContext();
		log.debug(EELFLoggerDelegate.debugLogger,
				API.Paths.SOLUTION_REVISION_ARTIFACTS + "(" + theSolutionId + "," + theRevisionId + ")");
		try {
			solutionRevisionArtifacts = catalogService.getSolutionRevisionArtifacts(theSolutionId, theRevisionId, context);
			if (solutionRevisionArtifacts != null &&
					!context.getPeer().getPeerInfo().isLocal()) {
				// re-encode the artifact uri
				for (MLPArtifact artifact : solutionRevisionArtifacts) {
					encodeArtifact(artifact, theHttpRequest);
				}
			}
			response = JsonResponse.<List<MLPArtifact>> buildResponse()
													.withMessage("solution revision artifacts")
													.withContent(solutionRevisionArtifacts)
													.build();
			theHttpResponse.setStatus(HttpServletResponse.SC_OK);
			log.debug(EELFLoggerDelegate.debugLogger, "getSolutionRevisionArtifacts provided {} artifacts",
						solutionRevisionArtifacts == null ? 0 : solutionRevisionArtifacts.size());
		} 
		catch (Exception x) {
			response = JsonResponse.<List<MLPArtifact>> buildErrorResponse()
														 .withError(x)
														 .build();
			theHttpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log.error(EELFLoggerDelegate.errorLogger, "An error occured while fetching solution " + theSolutionId + " revision " + theRevisionId + " artifacts", x);
		}
		return response;
	}

	/**
	 * @param theHttpRequest
	 *            HttpServletRequest
	 * @param theHttpResponse
	 *            HttpServletResponse
	 * @param theArtifactId
	 *            Artifact ID
	 * @return Archive file of the Artifact for the Solution.
	 */
	@CrossOrigin
	@PreAuthorize("hasAuthority('CATALOG_ACCESS')")
	@ApiOperation(value = "API to download the Machine Learning Artifact of the Machine Learning Solution", response = InputStreamResource.class, code = 200)
	@RequestMapping(value = {
			API.Paths.ARTIFACT_DOWNLOAD }, method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ResponseBody
	public InputStreamResource downloadSolutionArtifact(HttpServletRequest theHttpRequest,
			HttpServletResponse theHttpResponse, @PathVariable("artifactId") String theArtifactId) {
		InputStreamResource resource = null;
		try {
			resource = catalogService.getSolutionRevisionArtifactContent(theArtifactId,
					new ControllerContext());
			if (resource == null) {
				theHttpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
			else {
				theHttpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
				theHttpResponse.setHeader("Pragma", "no-cache");
				theHttpResponse.setHeader("Expires", "0");
				theHttpResponse.setStatus(HttpServletResponse.SC_OK);
			}
		} 
		catch (Exception x) {
			theHttpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log.error(EELFLoggerDelegate.errorLogger,
					"An error occurred while downloading artifact " + theArtifactId, x);
		}
		return resource;
	}

	/** */
	private void encodeSolution(MLPSolution theSolution, HttpServletRequest theRequest) throws URISyntaxException {
		//encode the 'origin'
		if (null == theSolution.getOrigin()) {
			URI requestUri = new URI(theRequest.getRequestURL().toString());
			URI solutionUri = API.SOLUTION_DETAIL
												.buildUri(
													new URI(requestUri.getScheme(), null, requestUri.getHost(),
																	requestUri.getPort(), null, null, null).toString(),
													theSolution.getSolutionId());
			theSolution.setOrigin(solutionUri.toString());	
		}
	}
	
	/** */
	private void encodeArtifact(MLPArtifact theArtifact, HttpServletRequest theRequest) throws URISyntaxException {
		if (theArtifact.getUri() != null) {
			URI requestUri = new URI(theRequest.getRequestURL().toString());
			URI artifactUri = API.ARTIFACT_DOWNLOAD
												.buildUri(
													new URI(requestUri.getScheme(), null, requestUri.getHost(),
																	requestUri.getPort(), null, null, null).toString(),
													theArtifact.getArtifactId());
			log.debug(EELFLoggerDelegate.debugLogger,	"getSolutionRevisionArtifacts: encoded content uri " + artifactUri);
			theArtifact.setUri(artifactUri.toString());
		}
	}

}

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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPDocument;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.federation.gateway.cds.Artifact;
import org.acumos.federation.gateway.cds.ArtifactTypes;
import org.acumos.federation.gateway.cds.Document;
import org.acumos.federation.gateway.cds.SolutionRevision;
import org.acumos.federation.gateway.common.API;
import org.acumos.federation.gateway.common.JsonResponse;
import org.acumos.federation.gateway.service.CatalogService;
import org.acumos.federation.gateway.service.ContentService;
import org.acumos.federation.gateway.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
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
 * 
 *
 */
@Controller
@RequestMapping(API.Roots.FEDERATION)
public class CatalogController extends AbstractController {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private CatalogService catalogService;
	@Autowired
	private ContentService contentService;

	/**
	 * @param theHttpRequest Request
	 * @param theHttpResponse
	 *            HttpServletResponse
	 * @param theSelector
	 *            Solutions selector
	 * @return List of Published ML Solutions in JSON format.
	 */
	@CrossOrigin
	// @PreAuthorize("hasAuthority('PEER')"
	@PreAuthorize("isActive && hasAuthority(T(org.acumos.federation.gateway.security.Priviledge).CATALOG_ACCESS)")
	@ApiOperation(value = "Invoked by Peer Acumos to get a list of Published Solutions from the Catalog of the local Acumos Instance .", response = MLPSolution.class, responseContainer = "List")
	@RequestMapping(value = { API.Paths.SOLUTIONS }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	public JsonResponse<List<MLPSolution>> getSolutions(
			HttpServletRequest theHttpRequest,
			HttpServletResponse theHttpResponse,
			@RequestParam(value = API.QueryParameters.SOLUTIONS_SELECTOR, required = false) String theSelector) {
		JsonResponse<List<MLPSolution>> response = null;
		List<MLPSolution> solutions = null;
		log.debug(API.Paths.SOLUTIONS);
		try {
			log.debug("getSolutions: selector " + theSelector);
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
			log.debug("getSolutions: provided {} solutions", solutions == null ? 0 : solutions.size());
		}
		catch (/*Exception*/Throwable x) {
			response = JsonResponse.<List<MLPSolution>> buildErrorResponse()
														 .withError(x)
														 .build();
			theHttpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log.error("Exception occurred while fetching solutions", x);
		}
		return response;
	}

	@CrossOrigin
	@PreAuthorize("isActive && hasAuthority('CATALOG_ACCESS')")
	@ApiOperation(value = "Invoked by Peer Acumos to get a list detailed solution information from the Catalog of the local Acumos Instance .", response = MLPSolution.class)
	@RequestMapping(value = { API.Paths.SOLUTION_DETAILS }, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<MLPSolution> getSolutionDetails(
			HttpServletRequest theHttpRequest,
			HttpServletResponse theHttpResponse,
			@PathVariable(value = "solutionId") String theSolutionId) {
		JsonResponse<MLPSolution> response = null;
		MLPSolution solution = null;
		log.debug(API.Paths.SOLUTION_DETAILS + ": " + theSolutionId);
		try {
			solution = catalogService.getSolution(theSolutionId, new ControllerContext());
			if (null == solution) {
				response = JsonResponse.<MLPSolution> buildResponse()
															.withMessage("No solution with id " + theSolutionId + " is available.")
															.build();
				theHttpResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}
			else {
				encodeSolution(solution, theHttpRequest);
				response = JsonResponse.<MLPSolution> buildResponse()
															.withMessage("solution details")
															.withContent(solution)
															.build();
				theHttpResponse.setStatus(HttpServletResponse.SC_OK);
			}
		}
		catch (Exception x) {
			response = JsonResponse.<MLPSolution> buildErrorResponse()
														 .withError(x)
														 .build();
			theHttpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log.error("An error occurred while fetching solution " + theSolutionId, x);
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
	@PreAuthorize("isActive && hasAuthority('CATALOG_ACCESS')")
	@ApiOperation(value = "Invoked by Peer Acumos to get a list of Solution Revision from the Catalog of the local Acumos Instance .", response = MLPSolutionRevision.class, responseContainer = "List")
	@RequestMapping(value = { API.Paths.SOLUTION_REVISIONS }, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<List<MLPSolutionRevision>> getSolutionRevisions(HttpServletResponse theHttpResponse,
			@PathVariable("solutionId") String theSolutionId) {
		JsonResponse<List<MLPSolutionRevision>> response = null;
		List<MLPSolutionRevision> solutionRevisions = null;
		log.debug(API.Paths.SOLUTION_REVISIONS);
		try {
			solutionRevisions = catalogService.getSolutionRevisions(theSolutionId, new ControllerContext());
			if (null == solutionRevisions) {
				response = JsonResponse.<List<MLPSolutionRevision>> buildResponse()
															.withMessage("No solution with id " + theSolutionId + " is available.")
															.build();
				theHttpResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}
			else {
				response = JsonResponse.<List<MLPSolutionRevision>> buildResponse()
															.withMessage("solution revisions")
															.withContent(solutionRevisions)
															.build();
				theHttpResponse.setStatus(HttpServletResponse.SC_OK);
				log.debug("getSolutionsRevisions for solution {} provided {} revisions",
						theSolutionId, solutionRevisions.size());
			}
		}
		catch (Exception x) {
			response = JsonResponse.<List<MLPSolutionRevision>> buildErrorResponse()
														 .withError(x)
														 .build();
			theHttpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log.error("An error occured while fetching solution " + theSolutionId + " revisions", x);
		}
		return response;
	}

	/**
	 * 
	 * @param theHttpRequest Request
	 * @param theSolutionId
	 *            Solution ID
	 * @param theRevisionId
	 *            Revision ID
	 * @param theHttpResponse
	 *            HttpServletResponse
	 * @return List of Published ML Solutions in JSON format.
	 */
	@CrossOrigin
	@PreAuthorize("isActive && hasAuthority('CATALOG_ACCESS')")
	@ApiOperation(value = "Invoked by peer Acumos to get solution revision details from the local Acumos Instance .", response = MLPSolutionRevision.class)
	@RequestMapping(value = {	API.Paths.SOLUTION_REVISION_DETAILS }, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<MLPSolutionRevision> getSolutionRevisionDetails(
			HttpServletRequest theHttpRequest, HttpServletResponse theHttpResponse,
			@PathVariable("solutionId") String theSolutionId, @PathVariable("revisionId") String theRevisionId) {
		ControllerContext context = new ControllerContext();
		JsonResponse<MLPSolutionRevision> response = null;
		SolutionRevision solutionRevision = null;
		log.debug(API.Paths.SOLUTION_REVISION_DETAILS + "(" + theSolutionId + "," + theRevisionId + ")");
		try {
			solutionRevision = catalogService.getSolutionRevision(theSolutionId, theRevisionId, context);
			if (null == solutionRevision) {
				response = JsonResponse.<MLPSolutionRevision> buildResponse()
																.withMessage("No solution revision " + theSolutionId + "/" + theRevisionId + " is available.")
																.build();
				theHttpResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}
			else {
				for (MLPArtifact artifact : solutionRevision.getArtifacts()) {
					if (!context.getPeer().getPeerInfo().isLocal()) {
						encodeArtifact(theSolutionId, theRevisionId, (Artifact)artifact, theHttpRequest);
					}
				}
				for (MLPDocument document : solutionRevision.getDocuments()) {
					if (!context.getPeer().getPeerInfo().isLocal()) {
						encodeDocument(theSolutionId, theRevisionId, (Document)document, theHttpRequest);
					}
				}
	
				response = JsonResponse.<MLPSolutionRevision> buildResponse()
																.withMessage("solution revision details")
																.withContent(solutionRevision)
																.build();
				theHttpResponse.setStatus(HttpServletResponse.SC_OK);
			}
		}
		catch (Exception x) {
			response = JsonResponse.<MLPSolutionRevision> buildErrorResponse()
														 .withError(x)
														 .build();
			theHttpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log.error("An error occured while fetching solution " + theSolutionId + " revision " + theRevisionId + " details", x);
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
	 * @return List of solution revision artifacts in JSON format.
	 */
	@CrossOrigin
	@PreAuthorize("isActive && hasAuthority('CATALOG_ACCESS')")
	@ApiOperation(value = "Invoked by Peer Acumos to get a list of solution revision artifacts from the local Acumos Instance .", response = MLPArtifact.class, responseContainer = "List")
	@RequestMapping(value = {
			API.Paths.SOLUTION_REVISION_ARTIFACTS }, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<List<MLPArtifact>> getSolutionRevisionArtifacts(
			HttpServletRequest theHttpRequest, HttpServletResponse theHttpResponse,
			@PathVariable("solutionId") String theSolutionId,	@PathVariable("revisionId") String theRevisionId) {
		JsonResponse<List<MLPArtifact>> response = null;
		List<MLPArtifact> solutionRevisionArtifacts = null;
		ControllerContext context = new ControllerContext();
		log.debug(API.Paths.SOLUTION_REVISION_ARTIFACTS + "(" + theSolutionId + "," + theRevisionId + ")");
		try {
			solutionRevisionArtifacts = catalogService.getSolutionRevisionArtifacts(theSolutionId, theRevisionId, context);
			if (null == solutionRevisionArtifacts) {
				response = JsonResponse.<List<MLPArtifact>> buildResponse()
																.withMessage("No solution revision " + theSolutionId + "/" + theRevisionId + " is available.")
																.build();
				theHttpResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}
			else {
				for (MLPArtifact artifact : solutionRevisionArtifacts) {
					if (!context.getPeer().getPeerInfo().isLocal()) {
						encodeArtifact(theSolutionId, theRevisionId, (Artifact)artifact, theHttpRequest);
					}
				}
				response = JsonResponse.<List<MLPArtifact>> buildResponse()
														.withMessage("solution revision artifacts")
														.withContent(solutionRevisionArtifacts)
														.build();
				theHttpResponse.setStatus(HttpServletResponse.SC_OK);
				log.debug("getSolutionRevisionArtifacts provided {} artifacts",
							solutionRevisionArtifacts.size());
			}
		} 
		catch (Exception x) {
			response = JsonResponse.<List<MLPArtifact>> buildErrorResponse()
														 .withError(x)
														 .build();
			theHttpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log.error("An error occured while fetching solution " + theSolutionId + " revision " + theRevisionId + " artifacts", x);
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
	 * @return List of solution revision documents in JSON format.
	 */
	@CrossOrigin
	@PreAuthorize("isActive && hasAuthority('CATALOG_ACCESS')")
	@ApiOperation(value = "Invoked by Peer Acumos to get a list of solution revision public documents from the local Acumos Instance .", response = MLPArtifact.class, responseContainer = "List")
	@RequestMapping(value = {
			API.Paths.SOLUTION_REVISION_DOCUMENTS }, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<List<MLPDocument>> getSolutionRevisionDocuments(
			HttpServletRequest theHttpRequest, HttpServletResponse theHttpResponse,
			@PathVariable("solutionId") String theSolutionId,	@PathVariable("revisionId") String theRevisionId) {
		JsonResponse<List<MLPDocument>> response = null;
		List<MLPDocument> solutionRevisionDocuments = null;
		ControllerContext context = new ControllerContext();
		log.debug(API.Paths.SOLUTION_REVISION_DOCUMENTS + "(" + theSolutionId + "," + theRevisionId + ")");
		try {
			solutionRevisionDocuments = catalogService.getSolutionRevisionDocuments(theSolutionId, theRevisionId, context);
			if (null == solutionRevisionDocuments) {
				response = JsonResponse.<List<MLPDocument>> buildResponse()
																.withMessage("No solution revision " + theSolutionId + "/" + theRevisionId + " is available.")
																.build();
				theHttpResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}
			else {
				for (MLPDocument document : solutionRevisionDocuments) {
					if (!context.getPeer().getPeerInfo().isLocal()) {
						encodeDocument(theSolutionId, theRevisionId, (Document)document, theHttpRequest);
					}
				}
				response = JsonResponse.<List<MLPDocument>> buildResponse()
														.withMessage("solution revision documents")
														.withContent(solutionRevisionDocuments)
														.build();
				theHttpResponse.setStatus(HttpServletResponse.SC_OK);
				log.debug("getSolutionRevisionDocuments provided {} documents",
							solutionRevisionDocuments.size());
			}
		} 
		catch (Exception x) {
			response = JsonResponse.<List<MLPDocument>> buildErrorResponse()
														 .withError(x)
														 .build();
			theHttpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log.error("An error occured while fetching solution " + theSolutionId + " revision " + theRevisionId + " documents", x);
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
	 * @param theArtifactId
	 *            Artifact ID
	 * @return Archive file of the Artifact for the Solution.
	 */
	@CrossOrigin
	@PreAuthorize("isActive && hasAuthority('CATALOG_ACCESS')")
	@ApiOperation(value = "API to download artifact content", response = Resource.class, code = 200)
	@RequestMapping(value = {
			API.Paths.ARTIFACT_CONTENT }, method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ResponseBody
	public Callable<Resource> getArtifactContent(HttpServletRequest theHttpRequest,
			HttpServletResponse theHttpResponse, @PathVariable("solutionId") String theSolutionId,
			@PathVariable("revisionId") String theRevisionId, @PathVariable("artifactId") String theArtifactId) {
			
		theHttpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
		theHttpResponse.setHeader("Pragma", "no-cache");
		theHttpResponse.setHeader("Expires", "0");
		theHttpResponse.setStatus(HttpServletResponse.SC_OK);

		final ControllerContext ctx = new ControllerContext();
		return new Callable<Resource>() {
			public Resource call() throws Exception {
				try {	
					return contentService.getArtifactContent(
						theSolutionId, theRevisionId, catalogService.getSolutionRevisionArtifact(theArtifactId, ctx), ctx);
				} 
				catch (Exception x) {
					log.error("An error occurred while retrieving artifact content " + theArtifactId, x);
					throw x;
				}
			}
		};
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
	 * @param theDocumentId
	 *            Document ID
	 * @return Archive file of the Artifact for the Solution.
	 */
	@CrossOrigin
	@PreAuthorize("isActive && hasAuthority('CATALOG_ACCESS')")
	@ApiOperation(value = "API to download a documents' content", response = Resource.class, code = 200)
	@RequestMapping(value = {
			API.Paths.DOCUMENT_CONTENT }, method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ResponseBody
	public Callable<Resource> getDocumentContent(HttpServletRequest theHttpRequest,
			HttpServletResponse theHttpResponse, @PathVariable("solutionId") String theSolutionId,
			@PathVariable("revisionId") String theRevisionId, @PathVariable("documentId") String theDocumentId) {
			
		theHttpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
		theHttpResponse.setHeader("Pragma", "no-cache");
		theHttpResponse.setHeader("Expires", "0");
		theHttpResponse.setStatus(HttpServletResponse.SC_OK);

		final ControllerContext ctx = new ControllerContext();
		return new Callable<Resource>() {
			public Resource call() throws Exception {
				try {	
					return contentService.getDocumentContent(
									theSolutionId, theRevisionId, catalogService.getSolutionRevisionDocument(theDocumentId, ctx), ctx);
				} 
				catch (Exception x) {
					log.error("An error occurred while retrieving artifact content " + theDocumentId, x);
					throw x;
				}
			}
		};
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
	private void encodeArtifact(String theSolutionId, String theRevisionId, Artifact theArtifact, HttpServletRequest theRequest)
																																						throws URISyntaxException {
		String artifactUri = theArtifact.getUri();

		//remember the artifact filename before redirecting
		theArtifact.setFilename(theArtifact.getCanonicalFilename());

		//redirect		
		{
			URI requestUri = new URI(theRequest.getRequestURL().toString());
			URI redirectUri = API.ARTIFACT_CONTENT
												.buildUri(
													new URI(requestUri.getScheme(), null, requestUri.getHost(),
																	requestUri.getPort(), null, null, null).toString(),
													theSolutionId, theRevisionId, theArtifact.getArtifactId());
			log.debug("encodeArtifact: redirected artifact uri " + redirectUri);
			theArtifact.setUri(redirectUri.toString());
		}
		
		if (ArtifactTypes.DockerImage.getCode().equals(theArtifact.getArtifactTypeCode())) {
			if (artifactUri != null) {
				//ugly but avoids parsing the manifest on the receiving side
				//this seems to be what on-boarding is doing anyway, otherwise this would amount to information loss
				theArtifact.setDescription(artifactUri);
			}
		}
	}
	
	/** */
	private void encodeDocument(String theSolutionId, String theRevisionId, Document theDocument, HttpServletRequest theRequest)
																																							throws URISyntaxException {
		String artifactUri = theDocument.getUri();

		//remember the document filename before redirecting
		theDocument.setFilename(theDocument.getCanonicalFilename());

		//redirect		
		{
			URI requestUri = new URI(theRequest.getRequestURL().toString());
			URI redirectUri = API.DOCUMENT_CONTENT
												.buildUri(
													new URI(requestUri.getScheme(), null, requestUri.getHost(),
																	requestUri.getPort(), null, null, null).toString(),
													theSolutionId, theRevisionId, theDocument.getDocumentId());
			log.debug("encodeDocument: redirected document uri " + redirectUri);
			theDocument.setUri(redirectUri.toString());
		}
	}

}

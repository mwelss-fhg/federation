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

package org.acumos.federation.gateway.controller;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.Callable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPCatalog;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
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
	 * List all catalogs the user is permitted to see.
	 *
	 * @param theHttpRequest Request
	 * @param theHttpResponse
	 *            HttpServletResponse
	 * @return List of Catalogs in JSON format.
	 */
	@CrossOrigin
	@PreAuthorize("isActive() && hasAuthority(T(org.acumos.federation.gateway.security.Priviledge).CATALOG_ACCESS)")
	@ApiOperation(value = "Invoked by Peer Acumos to get a list of visible Catalogs from the local Acumos Instance .", response = MLPCatalog.class, responseContainer = "List")
	@RequestMapping(value = API.Paths.CATALOGS, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<List<MLPCatalog>> getCatalogs(
	    HttpServletRequest theHttpRequest,
	    HttpServletResponse theHttpResponse) {
		JsonResponse<List<MLPCatalog>> response = null;
		List<MLPCatalog> catalogs = null;
		log.debug(API.Paths.CATALOGS);
		try {
			log.debug("getCatalogs");
			catalogs = catalogService.getCatalogs(new ControllerContext());
			response = JsonResponse.<List<MLPCatalog>> buildResponse()
														.withMessage("available catalogs")
														.withContent(catalogs)
														.build();
			theHttpResponse.setStatus(HttpServletResponse.SC_OK);
			log.debug("getCatalogs: provided {} catalogs", catalogs == null ? 0 : catalogs.size());
		} catch (Throwable x) {
			response = JsonResponse.<List<MLPCatalog>> buildErrorResponse()
														 .withError(x)
														 .build();
			theHttpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log.error("Exception occurred while fetching catalogs", x);
		}
		return response;
	}

	/**
	 * List all solutions in the specified catalog.
	 *
	 * @param theHttpRequest Request
	 * @param theHttpResponse
	 *            HttpServletResponse
	 * @param theCatalogId
	 *            The catalog
	 * @return List of Published ML Solutions in JSON format.
	 */
	@CrossOrigin
	@PreAuthorize("isActive() && hasAuthority(T(org.acumos.federation.gateway.security.Priviledge).CATALOG_ACCESS)")
	@ApiOperation(value = "Invoked by Peer Acumos to get a list of Published Solutions from the Catalog of the local Acumos Instance .", response = MLPSolution.class, responseContainer = "List")
	@RequestMapping(value = API.Paths.SOLUTIONS, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<List<MLPSolution>> getSolutions(
	    HttpServletRequest theHttpRequest,
	    HttpServletResponse theHttpResponse,
	    @RequestParam(value=API.QueryParameters.CATALOG_ID, required = true) String theCatalogId) {
		JsonResponse<List<MLPSolution>> response = null;
		List<MLPSolution> solutions = null;
		log.debug(API.Paths.SOLUTIONS);
		try {
			if (catalogService.isCatalogAllowed(theCatalogId, new ControllerContext())) {
				solutions = catalogService.getSolutions(theCatalogId, new ControllerContext());
			}
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

	/**
	 * Retrieve details of the specified solution.
	 *
	 * @param theHttpRequest Request
	 * @param theHttpResponse
	 *            HttpServletResponse
	 * @param theSolutionId
	 *            The solution
	 * @return Published MLSolution in JSON format.
	 */
	@CrossOrigin
	@PreAuthorize("isActive() && hasAuthority('CATALOG_ACCESS')")
	@ApiOperation(value = "Invoked by Peer Acumos to get a list detailed solution information from the Catalog of the local Acumos Instance .", response = MLPSolution.class)
	@RequestMapping(value = API.Paths.SOLUTION_DETAILS, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<MLPSolution> getSolutionDetails(
			HttpServletRequest theHttpRequest,
			HttpServletResponse theHttpResponse,
			@PathVariable(API.PathParameters.SOLUTION_ID) String theSolutionId) {
		JsonResponse<MLPSolution> response = null;
		MLPSolution solution = null;
		log.debug("{}: {}", API.Paths.SOLUTION_DETAILS, theSolutionId);
		try {
			if (catalogService.isSolutionAllowed(theSolutionId, new ControllerContext())) {
				solution = catalogService.getSolution(theSolutionId, new ControllerContext());
			}
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
	 * List the revisions of the specified solution.
	 *
	 * @param theHttpResponse
	 *            HttpServletResponse
	 * @param theSolutionId
	 *            Solution ID
	 * @return List of Published ML Solutions in JSON format.
	 */
	@CrossOrigin
	@PreAuthorize("isActive() && hasAuthority('CATALOG_ACCESS')")
	@ApiOperation(value = "Invoked by Peer Acumos to get a list of Solution Revision from the Catalog of the local Acumos Instance .", response = MLPSolutionRevision.class, responseContainer = "List")
	@RequestMapping(value = API.Paths.SOLUTION_REVISIONS, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<List<MLPSolutionRevision>> getSolutionRevisions(
	    HttpServletResponse theHttpResponse,
	    @PathVariable(API.PathParameters.SOLUTION_ID) String theSolutionId) {
		JsonResponse<List<MLPSolutionRevision>> response = null;
		List<MLPSolutionRevision> solutionRevisions = null;
		log.debug(API.Paths.SOLUTION_REVISIONS);
		try {
			if (catalogService.isSolutionAllowed(theSolutionId, new ControllerContext())) {
				solutionRevisions = catalogService.getRevisions(theSolutionId, new ControllerContext());
			}
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
	 * Get details of the specified revision, including the list of artifacts.
	 * If the catalogID is specified, also return the list of documents and the description.
	 * @param theHttpRequest Request
	 * @param theHttpResponse
	 *            HttpServletResponse
	 * @param theSolutionId
	 *            Solution ID
	 * @param theRevisionId
	 *            Revision ID
	 * @param theCatalogId
	 *            Catalog ID
	 * @return List of MLSolutionRevisions in JSON format.
	 */
	@CrossOrigin
	@PreAuthorize("isActive() && hasAuthority('CATALOG_ACCESS')")
	@ApiOperation(value = "Invoked by peer Acumos to get solution revision details from the local Acumos Instance .", response = MLPSolutionRevision.class)
	@RequestMapping(value = API.Paths.SOLUTION_REVISION_DETAILS, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<MLPSolutionRevision> getSolutionRevisionDetails(
	    HttpServletRequest theHttpRequest,
	    HttpServletResponse theHttpResponse,
	    @PathVariable(API.PathParameters.SOLUTION_ID) String theSolutionId,
	    @PathVariable(API.PathParameters.REVISION_ID) String theRevisionId,
	    @RequestParam(value=API.QueryParameters.CATALOG_ID, required = false) String theCatalogId) {
		ControllerContext context = new ControllerContext();
		JsonResponse<MLPSolutionRevision> response = null;
		SolutionRevision solutionRevision = null;
		log.debug("{}({},{})", API.Paths.SOLUTION_REVISION_DETAILS, theSolutionId, theRevisionId);
		try {
			if (theCatalogId != null && !catalogService.isCatalogAllowed(theCatalogId, context)) {
				theCatalogId = null;
			}
			solutionRevision = catalogService.getRevision(theCatalogId, theSolutionId, theRevisionId, context);
			if (solutionRevision != null && !catalogService.isSolutionAllowed(solutionRevision.getSolutionId(), context)) {
				solutionRevision = null;
			}
			if (null == solutionRevision) {
				response = JsonResponse.<MLPSolutionRevision> buildResponse()
				    .withMessage("No solution revision " + theSolutionId + "/" + theRevisionId + " is available.")
				    .build();
				theHttpResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}
			else {
				for (MLPArtifact artifact : solutionRevision.getArtifacts()) {
					if (!context.getPeer().getPeerInfo().isLocal()) {
						encodeArtifact((Artifact)artifact, theHttpRequest);
					}
				}
				for (MLPDocument document : solutionRevision.getDocuments()) {
					if (!context.getPeer().getPeerInfo().isLocal()) {
						encodeDocument((Document)document, theHttpRequest);
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
	 * List the artifacts of the specified revision.
	 *
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
	@PreAuthorize("isActive() && hasAuthority('CATALOG_ACCESS')")
	@ApiOperation(value = "Invoked by Peer Acumos to get a list of solution revision artifacts from the local Acumos Instance .", response = MLPArtifact.class, responseContainer = "List")
	@RequestMapping(value = API.Paths.SOLUTION_REVISION_ARTIFACTS, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<List<MLPArtifact>> getSolutionRevisionArtifacts(
	    HttpServletRequest theHttpRequest,
	    HttpServletResponse theHttpResponse,
	    @PathVariable(API.PathParameters.SOLUTION_ID) String theSolutionId,
	    @PathVariable(API.PathParameters.REVISION_ID) String theRevisionId) {
		JsonResponse<List<MLPArtifact>> response = null;
		List<MLPArtifact> solutionRevisionArtifacts = null;
		ControllerContext context = new ControllerContext();
		log.debug("{}({}, {})", API.Paths.SOLUTION_REVISION_ARTIFACTS, theSolutionId, theRevisionId);
		try {
			if (catalogService.isRevisionAllowed(theRevisionId, context)) {
				solutionRevisionArtifacts = catalogService.getArtifacts(theSolutionId, theRevisionId, context);
			}
			if (null == solutionRevisionArtifacts) {
				response = JsonResponse.<List<MLPArtifact>> buildResponse()
				    .withMessage("No solution revision " + theSolutionId + "/" + theRevisionId + " is available.")
				    .build();
				theHttpResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}
			else {
				for (MLPArtifact artifact : solutionRevisionArtifacts) {
					if (!context.getPeer().getPeerInfo().isLocal()) {
						encodeArtifact((Artifact)artifact, theHttpRequest);
					}
				}
				response = JsonResponse.<List<MLPArtifact>> buildResponse()
				    .withMessage("solution revision artifacts")
				    .withContent(solutionRevisionArtifacts)
				    .build();
				theHttpResponse.setStatus(HttpServletResponse.SC_OK);
				log.debug("getSolutionRevisionArtifacts provided {} artifacts", solutionRevisionArtifacts.size());
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
	 * List the documents in the specified catalog for the revision.
	 *
	 * @param theHttpRequest
	 *            HttpServletRequest
	 * @param theHttpResponse
	 *            HttpServletResponse
	 * @param theRevisionId
	 *            Revision ID
	 * @param theCatalogId
	 *            Catalog ID
	 * @return List of solution revision documents in JSON format.
	 */
	@CrossOrigin
	@PreAuthorize("isActive() && hasAuthority('CATALOG_ACCESS')")
	@ApiOperation(value = "Invoked by Peer Acumos to get a list of solution revision public documents from the local Acumos Instance .", response = MLPArtifact.class, responseContainer = "List")
	@RequestMapping(value = API.Paths.SOLUTION_REVISION_DOCUMENTS, method = RequestMethod.GET, produces = APPLICATION_JSON)
	@ResponseBody
	public JsonResponse<List<MLPDocument>> getSolutionRevisionDocuments(
	    HttpServletRequest theHttpRequest,
	    HttpServletResponse theHttpResponse,
	    @PathVariable(API.PathParameters.REVISION_ID) String theRevisionId,
	    @RequestParam(value=API.QueryParameters.CATALOG_ID, required = true) String theCatalogId) {
		JsonResponse<List<MLPDocument>> response = null;
		List<MLPDocument> solutionRevisionDocuments = null;
		ControllerContext context = new ControllerContext();
		log.debug("{}({})", API.Paths.SOLUTION_REVISION_DOCUMENTS, theRevisionId);
		try {
			if (catalogService.isCatalogAllowed(theCatalogId, context)) {
				solutionRevisionDocuments = catalogService.getDocuments(theCatalogId, theRevisionId, context);
			}
			if (null == solutionRevisionDocuments) {
				response = JsonResponse.<List<MLPDocument>> buildResponse()
				    .withMessage("No documents for " + theRevisionId + " are available.")
				    .build();
				theHttpResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}
			else {
				for (MLPDocument document : solutionRevisionDocuments) {
					if (!context.getPeer().getPeerInfo().isLocal()) {
						encodeDocument((Document)document, theHttpRequest);
					}
				}
				response = JsonResponse.<List<MLPDocument>> buildResponse()
				    .withMessage("solution revision documents")
				    .withContent(solutionRevisionDocuments)
				    .build();
				theHttpResponse.setStatus(HttpServletResponse.SC_OK);
				log.debug("getSolutionRevisionDocuments provided {} documents", solutionRevisionDocuments.size());
			}
		} 
		catch (Exception x) {
			response = JsonResponse.<List<MLPDocument>> buildErrorResponse()
			    .withError(x)
			    .build();
			theHttpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log.error("An error occured while fetching revision " + theRevisionId + " documents", x);
		}
		return response;
	}


	/**
	 * Get the content of the specified artifact.
	 *
	 * @param theHttpRequest
	 *            HttpServletRequest
	 * @param theHttpResponse
	 *            HttpServletResponse
	 * @param theArtifactId
	 *            Artifact ID
	 * @return Archive file of the Artifact for the Solution.
	 */
	@CrossOrigin
	@PreAuthorize("isActive() && hasAuthority('CATALOG_ACCESS')")
	@ApiOperation(value = "API to download artifact content", response = Resource.class, code = 200)
	@RequestMapping(value = API.Paths.ARTIFACT_CONTENT, method = RequestMethod.GET, produces = APPLICATION_OCTET_STREAM)
	@ResponseBody
	public Callable<Resource> getArtifactContent(
	    HttpServletRequest theHttpRequest,
	    HttpServletResponse theHttpResponse,
	    @PathVariable(API.PathParameters.ARTIFACT_ID) String theArtifactId) {
			
		theHttpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
		theHttpResponse.setHeader("Pragma", "no-cache");
		theHttpResponse.setHeader("Expires", "0");
		theHttpResponse.setStatus(HttpServletResponse.SC_OK);
		final ControllerContext ctx = new ControllerContext();
		if (!catalogService.isArtifactAllowed(theArtifactId, ctx)) {
			return () -> null;
		}
		return new Callable<Resource>() {
			public Resource call() throws Exception {
				try {	
					return contentService.getArtifactContent(catalogService.getArtifact(theArtifactId, ctx), ctx);
				} 
				catch (Exception x) {
					log.error("An error occurred while retrieving artifact content " + theArtifactId, x);
					throw x;
				}
			}
		};
	}

	/**
	 * Get the body of the specified document.
	 *
	 * @param theHttpRequest
	 *            HttpServletRequest
	 * @param theHttpResponse
	 *            HttpServletResponse
	 * @param theDocumentId
	 *            Document ID
	 * @return Archive file of the Artifact for the Solution.
	 */
	@CrossOrigin
	@PreAuthorize("isActive() && hasAuthority('CATALOG_ACCESS')")
	@ApiOperation(value = "API to download a documents' content", response = Resource.class, code = 200)
	@RequestMapping(value = API.Paths.DOCUMENT_CONTENT, method = RequestMethod.GET, produces = APPLICATION_OCTET_STREAM)
	@ResponseBody
	public Callable<Resource> getDocumentContent(
	    HttpServletRequest theHttpRequest,
	    HttpServletResponse theHttpResponse,
	    @PathVariable(API.PathParameters.DOCUMENT_ID) String theDocumentId) {
		theHttpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
		theHttpResponse.setHeader("Pragma", "no-cache");
		theHttpResponse.setHeader("Expires", "0");
		theHttpResponse.setStatus(HttpServletResponse.SC_OK);
		final ControllerContext ctx = new ControllerContext();
		if (!catalogService.isDocumentAllowed(theDocumentId, ctx)) {
			return () -> null;
		}
		return new Callable<Resource>() {
			public Resource call() throws Exception {
				try {	
					return contentService.getDocumentContent(catalogService.getDocument(theDocumentId, ctx), ctx);
				} 
				catch (Exception x) {
					log.error("An error occurred while retrieving artifact content " + theDocumentId, x);
					throw x;
				}
			}
		};
	}

	private void encodeSolution(MLPSolution theSolution, HttpServletRequest theRequest) throws URISyntaxException {
		//encode the 'origin'
		if (null == theSolution.getOrigin()) {
			URI requestUri = new URI(theRequest.getRequestURL().toString());
			URI solutionUri = API.SOLUTION_DETAIL.buildUri(
			    new URI(requestUri.getScheme(), null, requestUri.getHost(), requestUri.getPort(), null, null, null).toString(),
			    theSolution.getSolutionId());
			theSolution.setOrigin(solutionUri.toString());	
		}
	}
	
	private void encodeArtifact(Artifact theArtifact, HttpServletRequest theRequest) throws URISyntaxException {
		String artifactUri = theArtifact.getUri();
		//remember the artifact filename before redirecting
		theArtifact.setFilename(theArtifact.getCanonicalFilename());
		//redirect		
		URI requestUri = new URI(theRequest.getRequestURL().toString());
		URI redirectUri = API.ARTIFACT_CONTENT
		    .buildUri(
			new URI(requestUri.getScheme(), null, requestUri.getHost(), requestUri.getPort(), null, null, null).toString(),
			theArtifact.getArtifactId());
		log.debug("encodeArtifact: redirected artifact uri {}", redirectUri);
		theArtifact.setUri(redirectUri.toString());
		if (ArtifactTypes.DockerImage.getCode().equals(theArtifact.getArtifactTypeCode())) {
			if (artifactUri != null) {
				//ugly but avoids parsing the manifest on the receiving side
				//this seems to be what on-boarding is doing anyway, otherwise this would amount to information loss
				theArtifact.setDescription(artifactUri);
			}
		}
	}
	
	private void encodeDocument(Document theDocument, HttpServletRequest theRequest) throws URISyntaxException {
		//remember the document filename before redirecting
		theDocument.setFilename(theDocument.getCanonicalFilename());
		//redirect		
		URI requestUri = new URI(theRequest.getRequestURL().toString());
		URI redirectUri = API.DOCUMENT_CONTENT
		    .buildUri(
			new URI(requestUri.getScheme(), null, requestUri.getHost(), requestUri.getPort(), null, null, null).toString(),
													theDocument.getDocumentId());
		log.debug("encodeDocument: redirected document uri {}", redirectUri);
		theDocument.setUri(redirectUri.toString());
	}
}

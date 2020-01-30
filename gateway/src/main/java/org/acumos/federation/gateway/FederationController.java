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

import java.lang.invoke.MethodHandles;
import java.util.concurrent.Callable;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.swagger.annotations.ApiOperation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriTemplateHandler;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPCatalog;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPDocument;

import org.acumos.federation.client.ClientBase;
import org.acumos.federation.client.config.ClientConfig;
import org.acumos.federation.client.FederationClient;
import org.acumos.federation.client.data.Artifact;
import org.acumos.federation.client.data.Document;
import org.acumos.federation.client.data.JsonResponse;
import org.acumos.federation.client.data.ModelData;
import org.acumos.federation.client.data.SolutionRevision;

/**
 * Controller bean for the external (public) API.
 */
@Controller
@CrossOrigin
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class FederationController {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private FederationConfig federation;

	@Autowired
	private WebSecurityConfigurerAdapter security;

	@Autowired
	private PeerService peerService;

	@Autowired
	private CatalogService catalogService;

	@Autowired
	private LogstashService logstashService;

	@Autowired
	private ContentService contentService;

	private UriTemplateHandler originBuilder;

	private String makeOrigin(String uri, Object... params) {
		if (originBuilder == null) {
			originBuilder = ClientBase.buildRestTemplate("https://" + ((Security)security).getSelf().getSubjectName() + ":" + federation.getServer().getPort(), new ClientConfig(), null, null).getUriTemplateHandler();
		}
		return originBuilder.expand(uri, params).toString();
	}

	private void markOrigin(MLPSolution sol) {
		if (sol.getOrigin() == null) {
			sol.setOrigin(makeOrigin(FederationClient.SOLUTION_URI, sol.getSolutionId()));
		}
	}

	private static String makeFilename(String uri, boolean isnexus) {
		if (uri == null) {
			return null;
		}
		String[] urix = uri.split("/");
		int len = urix.length;
		String tag = "";
		int off = urix[len - 1].lastIndexOf('.');
		if (isnexus && off != -1) {
			tag = urix[len - 1].substring(off);
			urix[len - 1] = urix[len - 1].substring(0, off);
		}
		if (isnexus && len >= 3 && urix[len - 1].equals(urix[len - 3] + '-' + urix[len - 2])) {
			return urix[len - 3] + tag;
		}
		uri = urix[len - 1];
		off = uri.indexOf(isnexus? '-': ':');
		if (off != -1) {
			uri = uri.substring(0, off);
		}
		return uri + tag;
	}

	private void markOrigin(MLPArtifact art) {
		((Artifact)art).setFilename(makeFilename(art.getUri(), !FederationClient.ATC_DOCKER.equals(art.getArtifactTypeCode())));
		if (!Security.isCurrentPeerLocal()) {
			if (art.getUri() != null && FederationClient.ATC_DOCKER.equals(art.getArtifactTypeCode())) {
				art.setDescription(art.getUri());
			}
			art.setUri(makeOrigin(FederationClient.ARTIFACT_URI, art.getArtifactId()));
		}
	}

	private void markOrigin(MLPDocument doc) {
		((Document)doc).setFilename(makeFilename(doc.getUri(), true));
		if (!Security.isCurrentPeerLocal()) {
			doc.setUri(makeOrigin(FederationClient.DOCUMENT_URI, doc.getDocumentId()));
		}
	}

	@Secured(Security.ROLE_PEER)
	@ApiOperation(value = "Invoked by Peer Acumos to get status and self information.", response = MLPPeer.class)
	@GetMapping(FederationClient.PING_URI)
	@ResponseBody
	public JsonResponse<MLPPeer> ping() {
		log.debug("/ping");
		return respond(((Security)security).getSelf());
	}

	@Secured(Security.ROLE_PARTNER)
	@ApiOperation(value = "Invoked by Peer Acumos to get a list of peers from local Acumos Instance .", response = MLPPeer.class, responseContainer = "List")
	@GetMapping(FederationClient.PEERS_URI)
	@ResponseBody
	public JsonResponse<List<MLPPeer>> getPeers() {
		log.debug("/peers");
		return respond(peerService.getPeers());
	}

	@Secured(Security.ROLE_REGISTER)
	@ApiOperation(value = "Invoked by another Acumos Instance to request federation.", response = MLPPeer.class)
	@PostMapping(FederationClient.REGISTER_URI)
	@ResponseBody
	public JsonResponse<MLPPeer> register() {
		log.debug("/peer/register");
		if (!federation.isRegistrationEnabled()) {
			throw new BadRequestException(HttpServletResponse.SC_NOT_FOUND, "Not Found");
		}
		peerService.register();
		return respond(((Security)security).getSelf());
	}

	@Secured(Security.ROLE_UNREGISTER)
	@ApiOperation(value = "Invoked by another Acumos Instance to request federation termination.", response = MLPPeer.class)
	@PostMapping(FederationClient.UNREGISTER_URI)
	@ResponseBody
	public JsonResponse<MLPPeer> unregister() {
		log.debug("/peer/unregister");
		if (!federation.isRegistrationEnabled()) {
			throw new BadRequestException(HttpServletResponse.SC_NOT_FOUND, "Not Found");
		}
		peerService.unregister();
		return respond(((Security)security).getSelf());
	}

	@Secured(Security.ROLE_PEER)
	@ApiOperation(value = "Invoked by Peer Acumos to get a list of visible Catalogs from the local Acumos Instance .", response = MLPCatalog.class, responseContainer = "List")
	@GetMapping(FederationClient.CATALOGS_URI)
	@ResponseBody
	public JsonResponse<List<MLPCatalog>> getCatalogs() {
		log.debug("/catalogs");
		return respond(catalogService.getCatalogs());
	}

	@Secured(Security.ROLE_PEER)
	@ApiOperation(value = "Invoked by Peer Acumos to get a list of Published Solutions from the Catalog of the local Acumos Instance .", response = MLPSolution.class, responseContainer = "List")
	@GetMapping(FederationClient.SOLUTIONS_URI)
	@ResponseBody
	public JsonResponse<List<MLPSolution>> getSolutions(@RequestParam(value="catalogId", required = true) String catalogId) {
		log.debug("/solutions?catalogId={}", catalogId);
		if (!catalogService.isCatalogAllowed(catalogId)) {
			throw new BadRequestException(HttpServletResponse.SC_NOT_FOUND, "No catalog with id " + catalogId);
		}
		List<MLPSolution> ret = catalogService.getSolutions(catalogId);
		for (MLPSolution sol: ret) {
			markOrigin(sol);
		}
		return respond(ret);
	}

	@Secured(Security.ROLE_PEER)
	@ApiOperation(value = "Invoked by Peer Acumos to get a list detailed solution information from the Catalog of the local Acumos Instance .", response = MLPSolution.class)
	@GetMapping(FederationClient.SOLUTION_URI)
	@ResponseBody
	public JsonResponse<MLPSolution> getSolution(@PathVariable("solutionId") String solutionId) {
		log.debug("/solutions/{}", solutionId);
		MLPSolution ret = null;
		if (!catalogService.isSolutionAllowed(solutionId) || (ret = catalogService.getSolution(solutionId)) == null) {
			throw new BadRequestException(HttpServletResponse.SC_NOT_FOUND, "No solution with id " + solutionId);
		}
		markOrigin(ret);
		return respond(ret);
	}

	@Secured(Security.ROLE_PEER)
	@ApiOperation(value = "Invoked by Peer Acumos to get a list of Solution Revision from the Catalog of the local Acumos Instance .", response = MLPSolutionRevision.class, responseContainer = "List")
	@GetMapping(FederationClient.REVISIONS_URI)
	@ResponseBody
	public JsonResponse<List<MLPSolutionRevision>> getRevisions(@PathVariable("solutionId") String solutionId) {
		log.debug("/solutions/{}/revisions", solutionId);
		List<MLPSolutionRevision> ret = null;
		if (!catalogService.isSolutionAllowed(solutionId) || (ret = catalogService.getRevisions(solutionId)).isEmpty()) {
			throw new BadRequestException(HttpServletResponse.SC_NOT_FOUND, "No solution with id " + solutionId);
		}
		return respond(ret);
	}

	@Secured(Security.ROLE_PEER)
	@ApiOperation(value = "Invoked by peer Acumos to get solution revision details from the local Acumos Instance .", response = MLPSolutionRevision.class)
	@GetMapping(FederationClient.REVISION_URI)
	@ResponseBody
	public JsonResponse<MLPSolutionRevision> getRevision(
	    @PathVariable("solutionId") String solutionId,
	    @PathVariable("revisionId") String revisionId,
	    @RequestParam(value = "catalogId", required = false) String catalogId) {
		log.debug("/solutions/{}/revisions/{}?catalogId={}", solutionId, revisionId, catalogId);
		if (catalogId != null && !catalogService.isCatalogAllowed(catalogId)) {
			catalogId = null;
		}
		SolutionRevision ret = (SolutionRevision)catalogService.getRevision(revisionId, catalogId);
		if (ret == null || !catalogService.isSolutionAllowed(ret.getSolutionId())) {
			throw new BadRequestException(HttpServletResponse.SC_NOT_FOUND, "No revision with id " + revisionId);
		}
		for (MLPArtifact art: ret.getArtifacts()) {
			markOrigin(art);
		}
		for (MLPDocument doc: ret.getDocuments()) {
			markOrigin(doc);
		}
		return respond(ret);
	}

	@Secured(Security.ROLE_PEER)
	@ApiOperation(value = "Invoked by Peer Acumos to get a list of solution revision artifacts from the local Acumos Instance .", response = MLPArtifact.class, responseContainer = "List")
	@GetMapping(FederationClient.ARTIFACTS_URI)
	@ResponseBody
	public JsonResponse<List<MLPArtifact>> getArtifacts(
	    @PathVariable("solutionId") String solutionId,
	    @PathVariable("revisionId") String revisionId) {
		log.debug("/solutions/{}/revisions/{}/artifacts", solutionId, revisionId);
		if (!catalogService.isRevisionAllowed(revisionId)) {
			throw new BadRequestException(HttpServletResponse.SC_NOT_FOUND, "No revision with id " + revisionId);
		}
		List<MLPArtifact> ret = catalogService.getArtifacts(revisionId);
		for (MLPArtifact art: ret) {
			markOrigin(art);
		}
		return respond(ret);
	}

	@Secured(Security.ROLE_PEER)
	@ApiOperation(value = "Invoked by Peer Acumos to get a list of solution revision public documents from the local Acumos Instance .", response = MLPArtifact.class, responseContainer = "List")
	@GetMapping(FederationClient.DOCUMENTS_URI)
	@ResponseBody
	public JsonResponse<List<MLPDocument>> getDocuments(
	    @PathVariable("revisionId") String revisionId,
	    @RequestParam(value = "catalogId", required = true) String catalogId) {
		log.debug("/revisions/{}/documents?catalogId={}", revisionId, catalogId);
		if (!catalogService.isCatalogAllowed(catalogId)) {
			throw new BadRequestException(HttpServletResponse.SC_NOT_FOUND, "No catalog with id " + catalogId);
		}
		List<MLPDocument> ret = catalogService.getDocuments(revisionId, catalogId);
		for (MLPDocument doc: ret) {
			markOrigin(doc);
		}
		return respond(ret);
	}

	@Secured(Security.ROLE_PEER)
	@ApiOperation(value = "API to download artifact content", response = Resource.class, code = 200)
	@GetMapping(value = FederationClient.ARTIFACT_URI, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ResponseBody
	public Callable<Resource> getArtifactContent(@PathVariable("artifactId") String artifactId) {
		log.debug("/artifacts/{}/content", artifactId);
		if (!catalogService.isArtifactAllowed(artifactId)) {
			throw new BadRequestException(HttpServletResponse.SC_NOT_FOUND, "No artifact with id " + artifactId);
		}
		return () -> new InputStreamResource(contentService.getArtifactContent(catalogService.getArtifact(artifactId)));
	}

	@Secured(Security.ROLE_PEER)
	@ApiOperation(value = "API to download document content", response = Resource.class, code = 200)
	@GetMapping(value = FederationClient.DOCUMENT_URI, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ResponseBody
	public Resource getDocumentContent(@PathVariable("documentId") String documentId) {
		log.debug("/documents/{}/content", documentId);
		if (!catalogService.isDocumentAllowed(documentId)) {
			throw new BadRequestException(HttpServletResponse.SC_NOT_FOUND, "No document with id " + documentId);
		}
		return new InputStreamResource(contentService.getDocumentContent(catalogService.getDocument(documentId)));
	}

	private <T> JsonResponse<T> respond(T content) {
		JsonResponse<T> ret = new JsonResponse<>();
		ret.setContent(content);
		return ret;
	}

	@ExceptionHandler(BadRequestException.class)
	@ResponseBody
	public JsonResponse<Void> badRequestError(HttpServletRequest request, HttpServletResponse response, BadRequestException badRequest) {
		log.warn("Rejecting invalid request {}: {} {} {}", request.getRequestURI(), badRequest.getMessage(), badRequest.getCode(), badRequest);
		JsonResponse<Void> ret = new JsonResponse<>();
		ret.setError(badRequest.getMessage());
		response.setStatus(badRequest.getCode());
		return ret;
	}

	/**
	 * Receives model data payload from
	 * {@link GatewayController#peerModelData(HttpServletResponse, ModelData, String)}
	 *
	 * @param payload         model data payload The payload must have a model.solutionId
	 * 
	 * @param theHttpResponse HttpServletResponse
	 * 
	 */
	@CrossOrigin
	@Secured(Security.ROLE_PEER)
	@ApiOperation(value = "Invoked by Peer Acumos to post model data to elastic search service .",
			response = Void.class)
	@PostMapping(FederationClient.MODEL_DATA)
	@ResponseBody
	public void receiveModelData(@RequestBody ModelData payload,
			HttpServletResponse theHttpResponse) {

		log.debug(FederationClient.MODEL_DATA);
		log.debug("Model parameters: {}", payload);
		logstashService.sendModelData(payload);
	}
}

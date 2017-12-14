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

package org.acumos.federation.gateway.service.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import org.apache.commons.io.FileUtils;

import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.service.FederationService;
import org.acumos.federation.gateway.util.Utils;
import org.acumos.federation.gateway.util.LocalWatchService;
import org.acumos.federation.gateway.common.AdapterCondition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.core.env.Environment;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Conditional;
import org.springframework.boot.context.properties.ConfigurationProperties;

import org.acumos.cds.AccessTypeCode;
import org.acumos.cds.ValidationStatusCode;
import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.cds.transport.RestPageResponse;


/**
 * 
 *
 */
@Service
@ConfigurationProperties(prefix="catalogLocal")
@Conditional(AdapterCondition.class)
public class FederationServiceLocalImpl extends AbstractServiceLocalImpl
																				 implements FederationService {

	private List<FLPSolution>					solutions;


	@PostConstruct
	public void initService() {

		checkResource();
		try {
	    watcher.watchOn(this.resource.getURL().toURI(),
  	                  (uri) -> { loadSolutionsCatalogInfo(); });

    }
    catch (IOException | URISyntaxException iox) {
      log.info(EELFLoggerDelegate.errorLogger, "Catalog watcher registration failed for " + this.resource, iox);
    }

		loadSolutionsCatalogInfo();

    // Done
    log.debug(EELFLoggerDelegate.debugLogger, "Local FederationService available");
	}

	@PreDestroy
	public void cleanupService() {
	}		

	/** */
	private void loadSolutionsCatalogInfo() {
		synchronized (this) {
			try {
				ObjectReader objectReader = 
														new ObjectMapper().reader(FLPSolution.class);
				MappingIterator objectIterator =
														objectReader.readValues(this.resource.getURL());
				this.solutions = objectIterator.readAll();
				log.info(EELFLoggerDelegate.debugLogger, "loaded " + this.solutions.size() + " solutions");
			}
			catch (Exception x) {
				throw new BeanInitializationException("Failed to load solutions catalog from " + this.resource, x);
			}
		}
	}

	/**
	 */
	@Override
	public RestPageResponse<MLPSolution>  getPeerCatalogSolutions(Integer pageNumber, Integer maxSize, String sortingOrder,
			List<String> mlpModelTypes) {
		return null;
	}

	@Override
	public List<MLPSolution> getPeerCatalogSolutionsList(String mlpModelTypes) {
		log.debug(EELFLoggerDelegate.debugLogger, "getPeerCatalogSolutionsList");
		final List<String> modelTypes =
			mlpModelTypes == null ? null : Arrays.asList(mlpModelTypes.split(","));
		return solutions.stream()
							.filter(solution -> {
		log.debug(EELFLoggerDelegate.debugLogger, "getPeerCatalogSolutionsList: looking for " + modelTypes + ", has " + solution.getModelTypeCode());
												return modelTypes == null ||
															 modelTypes.contains(solution.getModelTypeCode());
											})
							.collect(Collectors.toList());
	}

	
	@Override
	public List<MLPSolutionRevision> getPeerCatalogSolutionRevision(
																					final String theSolutionId) {
		log.debug(EELFLoggerDelegate.debugLogger, "getPeerCatalogSolutionRevision`");
		FLPSolution solution =
						this.solutions.stream()
							.filter(sol ->
												sol.getSolutionId().equals(theSolutionId))
							.findFirst()
							.orElse(null);
		
		return (solution == null) ? null : solution.getMLPRevisions();
	}

	@Override
	public List<MLPArtifact> getPeerSolutionArtifacts(
																						final String theSolutionId,
																						final String theRevisionId) {
		log.debug(EELFLoggerDelegate.debugLogger, "getPeerSolutionArtifacts`");
		FLPSolution solution =
						 this.solutions.stream()
							.filter(sol ->
												sol.getSolutionId().equals(theSolutionId))
							.findFirst()
							.orElse(null);

		if (solution == null)
			return null;

		FLPRevision revision = 
			solution.getRevisions().stream()
							.filter(rev ->
												rev.getRevisionId().equals(theRevisionId))
							.findFirst()
							.orElse(null);

		return (revision == null) ? null : revision.getArtifacts();
	}

	@Override
	public InputStreamResource getPeerSolutionArtifactFile(
														final String theArtifactId) {
		return null;
	}

	/** */
	public static class FLPSolution extends MLPSolution {

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
		private List<FLPRevision> revisions;

		//@JsonIgnore
		public List<FLPRevision> getRevisions() {
			return this.revisions;
		}
		
		//@JsonIgnore
		protected List<MLPSolutionRevision> getMLPRevisions() {
			return this.revisions == null ? null :
								this.revisions.stream()
											.map(rev -> (MLPSolutionRevision)rev)
											.collect(Collectors.toList());
		}

		public void setRevisions(List<FLPRevision> theRevisions) {
			this.revisions = theRevisions;
		}

	}
	
	/** */
	public static class FLPRevision extends MLPSolutionRevision {

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
		private List<MLPArtifact> artifacts;

		//@JsonIgnore
		public List<MLPArtifact> getArtifacts() {
			return this.artifacts;
		}

		public void setArtifacts(List<MLPArtifact> theArtifacts) {
			this.artifacts = theArtifacts;
		}
	}


}

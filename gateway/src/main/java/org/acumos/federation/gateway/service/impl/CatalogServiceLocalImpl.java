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
import org.acumos.federation.gateway.service.CatalogService;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.service.ServiceException;
import org.acumos.federation.gateway.util.Utils;

import org.acumos.cds.AccessTypeCode;
import org.acumos.cds.ValidationStatusCode;
import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.cds.transport.RestPageResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.core.env.Environment;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Conditional;
import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * 
 *
 */
@Service
@ConfigurationProperties(prefix = "catalogLocal")
public class CatalogServiceLocalImpl extends AbstractServiceLocalImpl implements CatalogService {

	private List<FLPSolution> solutions;

	@PostConstruct
	public void initService() {

		checkResource();
		try {
			watcher.watchOn(this.resource.getURL().toURI(), (uri) -> {
				loadSolutionsCatalogInfo();
			});

		} catch (IOException | URISyntaxException iox) {
			log.info(EELFLoggerDelegate.errorLogger, "Catalog watcher registration failed for " + this.resource, iox);
		}

		loadSolutionsCatalogInfo();

		// Done
		log.debug(EELFLoggerDelegate.debugLogger, "Local CatalogService available");
	}

	@PreDestroy
	public void cleanupService() {
	}

	/** */
	private void loadSolutionsCatalogInfo() {
		synchronized (this) {
			try {
				ObjectReader objectReader = new ObjectMapper().reader(FLPSolution.class);
				MappingIterator objectIterator = objectReader.readValues(this.resource.getURL());
				this.solutions = objectIterator.readAll();
				log.info(EELFLoggerDelegate.debugLogger, "loaded " + this.solutions.size() + " solutions");
			} catch (Exception x) {
				throw new BeanInitializationException("Failed to load solutions catalog from " + this.resource, x);
			}
		}
	}

	@Override
	public List<MLPSolution> getSolutions(Map<String, ?> theSelector, ServiceContext theContext) {

		log.debug(EELFLoggerDelegate.debugLogger, "getSolutions");
		String modelTypeSelector = theSelector == null ? null : (String) theSelector.get("modelTypeCode");
		String toolkitTypeSelector = theSelector == null ? null : (String) theSelector.get("toolkitTypeCode");
		final List<String> modelTypes = modelTypeSelector == null ? null : Arrays.asList(modelTypeSelector.split(","));
		final List<String> toolkitTypes = toolkitTypeSelector == null ? null : Arrays.asList(toolkitTypeSelector.split(","));

		return solutions.stream()
			.filter(solution -> {
				log.debug(EELFLoggerDelegate.debugLogger,
					"getPeerCatalogSolutionsList: looking for model type " + modelTypes + ", has " + solution.getModelTypeCode());
				return modelTypes == null || modelTypes.contains(solution.getModelTypeCode());
			})
			.filter(solution -> {
				log.debug(EELFLoggerDelegate.debugLogger,
					"getPeerCatalogSolutionsList: looking for toolkit type " + toolkitTypes + ", has " + solution.getToolkitTypeCode());
				return toolkitTypes == null || toolkitTypes.contains(solution.getToolkitTypeCode());
			})
			.collect(Collectors.toList());
	}

	@Override
	public MLPSolution getSolution(final String theSolutionId, ServiceContext theContext) {

		log.debug(EELFLoggerDelegate.debugLogger, "getSolution");
		return solutions.stream().filter(solution -> {
			return theSolutionId.equals(solution.getSolutionId());
		}).findFirst().orElse(null);
	}

	@Override
	public List<MLPSolutionRevision> getSolutionRevisions(final String theSolutionId, ServiceContext theContext) {

		log.debug(EELFLoggerDelegate.debugLogger, "getSolutionRevisions");
		FLPSolution solution = this.solutions.stream().filter(sol -> sol.getSolutionId().equals(theSolutionId))
				.findFirst().orElse(null);

		return (solution == null) ? null : solution.getMLPRevisions();
	}

	@Override
	public MLPSolutionRevision getSolutionRevision(String theSolutionId, String theRevisionId,
			ServiceContext theContext) {

		log.debug(EELFLoggerDelegate.debugLogger, "getSolutionRevision");
		List<MLPSolutionRevision> revisions = getSolutionRevisions(theSolutionId, theContext);

		if (revisions == null)
			return null;

		return revisions.stream().filter(rev -> rev.getRevisionId().equals(theRevisionId)).findFirst().orElse(null);
	}

	@Override
	public List<MLPArtifact> getSolutionRevisionArtifacts(final String theSolutionId, final String theRevisionId,
			ServiceContext theContext) {
		log.debug(EELFLoggerDelegate.debugLogger, "getSolutionRevisionArtifacts");

		FLPRevision revision = (FLPRevision) getSolutionRevision(theSolutionId, theRevisionId, theContext);

		return (revision == null) ? null : revision.getArtifacts();
	}

	@Override
	public InputStreamResource getSolutionRevisionArtifactContent(String theArtifactId, ServiceContext theContext) 
																																																throws ServiceException {

		log.debug(EELFLoggerDelegate.debugLogger, "getSolutionRevisionArtifactContent");
		// cumbersome
		for (FLPSolution solution : this.solutions) {
			for (FLPRevision revision : solution.getRevisions()) {
				for (MLPArtifact artifact : revision.getArtifacts()) {
					if (artifact.getArtifactId().equals(theArtifactId)) {
						try {
							return new InputStreamResource(new URI(artifact.getUri()).toURL().openStream());
						} catch (Exception x) {
							log.debug(EELFLoggerDelegate.debugLogger,
									"failed to load artifact content from " + artifact.getUri(), x);
							throw new ServiceException("Failed to retrieve content for artifact " + theArtifactId, x);
						}
					}
				}
			}
		}

		return null;
	}

	/** */
	public static class FLPSolution extends MLPSolution {

		@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
		private List<FLPRevision> revisions;

		// @JsonIgnore
		public List<FLPRevision> getRevisions() {
			return this.revisions;
		}

		// @JsonIgnore
		protected List<MLPSolutionRevision> getMLPRevisions() {
			return this.revisions == null ? null
					: this.revisions.stream().map(rev -> (MLPSolutionRevision) rev).collect(Collectors.toList());
		}

		public void setRevisions(List<FLPRevision> theRevisions) {
			this.revisions = theRevisions;
		}

	}

	/** */
	public static class FLPRevision extends MLPSolutionRevision {

		@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
		private List<MLPArtifact> artifacts;

		// @JsonIgnore
		// we send a deep clone as the client can modify them and we only have one copy
		public List<MLPArtifact> getArtifacts() {
			List<MLPArtifact> copy = new LinkedList<MLPArtifact>();
			for (MLPArtifact artifact : this.artifacts) {
				MLPArtifact acopy = new MLPArtifact();
				acopy.setArtifactId(artifact.getArtifactId());
				acopy.setArtifactTypeCode(artifact.getArtifactTypeCode());
				acopy.setDescription(artifact.getDescription());
				acopy.setUri(artifact.getUri());
				acopy.setName(artifact.getName());
				acopy.setSize(artifact.getSize());
				acopy.setOwnerId(artifact.getOwnerId());
				acopy.setCreated(artifact.getCreated());
				acopy.setModified(artifact.getModified());
				acopy.setMetadata(artifact.getMetadata());

				copy.add(acopy);
			}
			return copy;
		}

		public void setArtifacts(List<MLPArtifact> theArtifacts) {
			this.artifacts = theArtifacts;
		}
	}

}

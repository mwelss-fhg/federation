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
import java.util.Collections;
import java.util.stream.Collectors;

import java.lang.invoke.MethodHandles;

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

import org.acumos.federation.gateway.cds.Solution;
import org.acumos.federation.gateway.cds.SolutionRevision;
import org.acumos.federation.gateway.cds.Artifact;
import org.acumos.federation.gateway.cds.Mapper;

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

	private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(MethodHandles.lookup().lookupClass());

	private List<Solution> solutions;

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
 				ObjectReader objectReader = Mapper.build().reader(Solution.class);
				MappingIterator objectIterator = objectReader.readValues(this.resource.getURL());
				this.solutions = objectIterator.readAll();
				log.info(EELFLoggerDelegate.debugLogger, "loaded " + this.solutions.size() + " solutions");
			} catch (Exception x) {
				throw new BeanInitializationException("Failed to load solutions catalog from " + this.resource, x);
			}
		}
	}

	@Override
	public List<MLPSolution> getSolutions(Map<String, ?> theSelector, ServiceContext theContext) throws ServiceException {

		log.debug(EELFLoggerDelegate.debugLogger, "getSolutions, selector {}", theSelector);

		return solutions.stream()
			.filter(solution -> ServiceImpl.isSelectable(solution, theSelector))
			.collect(Collectors.toList());
	}

	@Override
	public Solution getSolution(final String theSolutionId, ServiceContext theContext) throws ServiceException {

		log.debug(EELFLoggerDelegate.debugLogger, "getSolution");
		return solutions.stream().filter(solution -> {
			return theSolutionId.equals(solution.getSolutionId());
		}).findFirst().orElse(null);
	}

	@Override
	public List<MLPSolutionRevision> getSolutionRevisions(final String theSolutionId, ServiceContext theContext) throws ServiceException {

		log.debug(EELFLoggerDelegate.debugLogger, "getSolutionRevisions");

		Solution solution = getSolution(theSolutionId, theContext);
		return (solution == null) ? Collections.EMPTY_LIST : (List)solution.getRevisions();
	}

	@Override
	public SolutionRevision getSolutionRevision(String theSolutionId, String theRevisionId,
			ServiceContext theContext) throws ServiceException  {

		log.debug(EELFLoggerDelegate.debugLogger, "getSolutionRevision");
		return (SolutionRevision)getSolutionRevisions(theSolutionId, theContext).stream()
						.filter(rev -> rev.getRevisionId().equals(theRevisionId)).findFirst().orElse(null);
	}

	@Override
	public List<MLPArtifact> getSolutionRevisionArtifacts(final String theSolutionId, final String theRevisionId,
			ServiceContext theContext) throws ServiceException {
		log.debug(EELFLoggerDelegate.debugLogger, "getSolutionRevisionArtifacts");

		SolutionRevision revision = getSolutionRevision(theSolutionId, theRevisionId, theContext);
		return (revision == null) ? Collections.EMPTY_LIST : (List)revision.getArtifacts();
	}

	@Override
	public Artifact getSolutionRevisionArtifact(String theArtifactId, ServiceContext theContext) 
																																																throws ServiceException {
		log.debug(EELFLoggerDelegate.debugLogger, "getSolutionRevisionArtifact");
		// cumbersome
		for (Solution solution : this.solutions) {
			for (MLPSolutionRevision revision : solution.getRevisions()) {
				for (MLPArtifact artifact : ((SolutionRevision)revision).getArtifacts()) {
					if (artifact.getArtifactId().equals(theArtifactId)) {
						return (Artifact)artifact;
					}
				}
			}
		}

		return null;
	}
}

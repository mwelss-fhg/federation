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

/**
 * 
 */
package org.acumos.federation.gateway.service.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;

import org.acumos.federation.gateway.util.Utils;
import org.acumos.federation.gateway.util.Errors;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.service.CatalogService;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.service.ServiceException;

import org.acumos.nexus.client.NexusArtifactClient;

import org.acumos.cds.AccessTypeCode;
import org.acumos.cds.ValidationStatusCode;
import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.cds.transport.RestPageResponse;
import org.acumos.cds.transport.RestPageRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.client.HttpStatusCodeException;

import org.acumos.federation.gateway.cds.Solution;
import org.acumos.federation.gateway.cds.SolutionRevision;

/**
 * CDS based implementation of the CatalogService.
 *
 */
@Service
public class CatalogServiceImpl extends AbstractServiceImpl
																implements CatalogService {

	private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(CatalogServiceImpl.class.getName());

	@Autowired
	private Environment env;

	private Map<String, Object> baseSelector;

	@PostConstruct
	public void initService() {
		baseSelector = new HashMap<String, Object>();

		// Fetch all active solutions
		baseSelector.put("active", true);
		// Fetch allowed only for Public models
		baseSelector.put("accessTypeCode", AccessTypeCode.PB.toString());
		// Validation status should be passed locally
		baseSelector.put("validationStatusCode", ValidationStatusCode.PS.toString());
	}

	@Override
	/*
	 */
	public List<MLPSolution> getSolutions(Map<String, ?> theSelector, ServiceContext theContext) throws ServiceException {
		log.debug(EELFLoggerDelegate.debugLogger, "getSolutions with selector {}", theSelector);

		Map<String, Object> selector = new HashMap<String, Object>();
		if (theSelector != null)
			selector.putAll(theSelector);
		//it is essential that this gets done at the end as to force all baseSelector criteria (otherwise a submitted accessTypeCode
		//could overwrite the basic one end expose non public solutions ..).
		selector.putAll(this.baseSelector);
		log.debug(EELFLoggerDelegate.debugLogger, "getSolutions with full selector {}", selector);

		RestPageRequest pageRequest = new RestPageRequest(0, 5);
		RestPageResponse<MLPSolution> pageResponse = null;
		List<MLPSolution> solutions = new ArrayList<MLPSolution>(),
											pageSolutions = null;
		ICommonDataServiceRestClient cdsClient = getClient();

		do {
			log.debug(EELFLoggerDelegate.debugLogger, "getSolutions page {}", pageResponse);
			if (theSelector.containsKey("modified")) {
				//Use the dedicated api: this is a 'deep' application of the 'modified' criteria as it will look into revisions
				//and artifacts for related information modified since.
				pageResponse =
					cdsClient.findSolutionsByDate(
						(Boolean)baseSelector.get("active"),
						new String[] {baseSelector.get("accessTypeCode").toString()},
						new String[] {baseSelector.get("validationStatusCode").toString()},
						new Date((Long)theSelector.get("modified")),
						pageRequest);
			
				//we need to post-process all other selection criteria
				pageSolutions = pageResponse.getContent().stream()
													.filter(solution -> ServiceImpl.isSelectable(solution, theSelector))
													.collect(Collectors.toList());
			}
			else {
				pageResponse =
					cdsClient.searchSolutions(selector, false, pageRequest);
				pageSolutions = pageResponse.getContent();
			}
			log.debug(EELFLoggerDelegate.debugLogger, "getSolutions page response {}", pageResponse);
		
			pageRequest.setPage(pageResponse.getNumber() + 1);
			solutions.addAll(pageSolutions);
		}
		while (!pageResponse.isLast());

		log.debug(EELFLoggerDelegate.debugLogger, "getSolutions: cds solutions count {}", solutions.size());
		return solutions;
	}

	@Override
	public MLPSolution getSolution(String theSolutionId, ServiceContext theContext) throws ServiceException {

		log.trace(EELFLoggerDelegate.debugLogger, "getSolution {}", theSolutionId);
		ICommonDataServiceRestClient cdsClient = getClient();
		try {
			Solution solution = (Solution)cdsClient.getSolution(theSolutionId);
			solution.setRevisions(cdsClient.getSolutionRevisions(theSolutionId));
			return solution;
		}
		catch (HttpStatusCodeException restx) {
			if (Errors.isCDSNotFound(restx))
				return null;
			else
				throw new ServiceException("Failed to retrieve solution information", restx);
		}
	}

	@Override
	public List<MLPSolutionRevision> getSolutionRevisions(String theSolutionId, ServiceContext theContext) throws ServiceException {

		log.trace(EELFLoggerDelegate.debugLogger, "getSolutionRevisions");
		try {
			return getClient().getSolutionRevisions(theSolutionId);
		}
		catch (HttpStatusCodeException restx) {
			if (Errors.isCDSNotFound(restx))
				return null;
			else
				throw new ServiceException("Failed to retrieve solution revision information", restx);
		}
	}

	@Override
	public MLPSolutionRevision getSolutionRevision(String theSolutionId, String theRevisionId,
			ServiceContext theContext) throws ServiceException {

		log.trace(EELFLoggerDelegate.debugLogger, "getSolutionRevision");
		ICommonDataServiceRestClient cdsClient = getClient();
		try {
			SolutionRevision revision =
					(SolutionRevision)cdsClient.getSolutionRevision(theSolutionId, theRevisionId);
			revision.setArtifacts(cdsClient.getSolutionRevisionArtifacts(theSolutionId, theRevisionId));
			return revision;
		}	
		catch (HttpStatusCodeException restx) {
			if (Errors.isCDSNotFound(restx))
				return null;
			else
				throw new ServiceException("Failed to retrieve solution revision information", restx);
		}
	}

	@Override
	public List<MLPArtifact> getSolutionRevisionArtifacts(String theSolutionId, String theRevisionId,
			ServiceContext theContext) throws ServiceException {

		log.trace(EELFLoggerDelegate.debugLogger, "getSolutionRevisionArtifacts");
		try {
			return getClient().getSolutionRevisionArtifacts(theSolutionId, theRevisionId);
		}
		catch (HttpStatusCodeException restx) {
			if (Errors.isCDSNotFound(restx))
				return null;
			else
				throw new ServiceException("Failed to retrieve solution information", restx);
		}
	}

	/**
	 * @return catalog artifact representation
	 * @throws ServiceException if failing to retrieve artifact information or retrieve content 
	 */
	@Override
	public MLPArtifact getSolutionRevisionArtifact(String theArtifactId, ServiceContext theContext) throws ServiceException {

		log.trace(EELFLoggerDelegate.debugLogger, "getSolutionRevisionArtifact");
		try {
			return getClient().getArtifact(theArtifactId);
		}	
		catch (HttpStatusCodeException restx) {
			if (Errors.isCDSNotFound(restx))
				return null;
			else
				throw new ServiceException("Failed to retrieve solution revision artifact information", restx);
		}
	}

}

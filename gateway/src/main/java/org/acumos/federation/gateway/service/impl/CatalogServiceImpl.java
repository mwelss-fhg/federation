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
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.service.CatalogService;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.util.Utils;
import org.acumos.federation.gateway.common.GatewayCondition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Conditional;

import org.acumos.nexus.client.NexusArtifactClient;

import org.acumos.cds.AccessTypeCode;
import org.acumos.cds.ValidationStatusCode;
import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.cds.transport.RestPageResponse;

/**
 * CDS based implementation of the CatalogService. 
 *
 */
@Service
@Conditional(GatewayCondition.class)
public class CatalogServiceImpl 
								extends AbstractServiceImpl
								implements CatalogService {

	@Autowired
	private Environment env;

	private Map<String, Object> baseSelector;
	
	@PostConstruct
	public void initService() {
		baseSelector = new HashMap<String, Object>();

		baseSelector.put("active", true); //Fetch all active solutions
		baseSelector.put("accessTypeCode", AccessTypeCode.PB.toString()); // Fetch allowed only for Public models
		baseSelector.put("validationStatusCode", ValidationStatusCode.PS.toString()); // Validation status should be Passed locally
//		baseSelector.put("source", "");
	
	}

	@Override
	public List<MLPSolution> getSolutions(
		Map<String,?> theSelector, ServiceContext theContext) {
		
		log.debug(EELFLoggerDelegate.debugLogger, "getSolutions");
		List<MLPSolution> filteredMLPSolutions = null;
		ICommonDataServiceRestClient cdsClient = getClient();
	
		Map<String, Object> selector =
				new HashMap<String, Object>(this.baseSelector);
		if (theSelector != null)
			selector.putAll(theSelector);

		List<MLPSolution> solutions = cdsClient.searchSolutions(selector, false);
		log.debug(EELFLoggerDelegate.debugLogger, "getSolutions: cds solutions " + solutions);

		return solutions;
	}

	@Override
	public MLPSolution getSolution(
		String theSolutionId, ServiceContext theContext) {
		
		log.debug(EELFLoggerDelegate.debugLogger, "getSolution");
		ICommonDataServiceRestClient cdsClient = getClient();
		return cdsClient.getSolution(theSolutionId);
	}
	
	@Override
	public List<MLPSolutionRevision> getSolutionRevisions(
		String theSolutionId, ServiceContext theContext) {

		log.debug(EELFLoggerDelegate.debugLogger, "getSolutionRevisions");
		ICommonDataServiceRestClient cdsClient = getClient();
		List<MLPSolutionRevision> mlpSolutionRevisions =
			cdsClient.getSolutionRevisions(theSolutionId);
		return mlpSolutionRevisions;
	}

	@Override
	public MLPSolutionRevision getSolutionRevision(
		String theSolutionId, String theRevisionId, ServiceContext theContext) {

		log.debug(EELFLoggerDelegate.debugLogger, "getSolutionRevision");
		ICommonDataServiceRestClient cdsClient = getClient();
		MLPSolutionRevision mlpSolutionRevision =
			cdsClient.getSolutionRevision(theSolutionId, theRevisionId);
		return mlpSolutionRevision;
	}

	@Override
	public List<MLPArtifact> getSolutionRevisionArtifacts(
		String theSolutionId, String theRevisionId, ServiceContext theContext) {
		
		log.debug(EELFLoggerDelegate.debugLogger, "getSolutionRevisionArtifacts");
		ICommonDataServiceRestClient cdsClient = getClient();
		List<MLPArtifact> mlpArtifacts = cdsClient.getSolutionRevisionArtifacts(theSolutionId, theRevisionId);
		return mlpArtifacts;
	}

	@Override
	public InputStreamResource getSolutionRevisionArtifactContent(
		String theArtifactId, ServiceContext theContext) {

		InputStreamResource streamResource = null;
		ByteArrayOutputStream byteArrayOutputStream  = null;
		try{
			ICommonDataServiceRestClient cdsClient = getClient();
			MLPArtifact mlpArtifact = cdsClient.getArtifact(theArtifactId);
			
			NexusArtifactClient artifactClient = this.clients.getNexusClient();
			
			byteArrayOutputStream = artifactClient.getArtifact(mlpArtifact.getUri());
			InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
			
			//Plain Old Java. Sprint 3 will use try resource handling
			if(inputStream != null) {
				streamResource = new InputStreamResource(inputStream
																								 /*, some_description
																									*/);
			}
			if(byteArrayOutputStream != null) {
				byteArrayOutputStream.close();
			}
			
		} catch (Exception e) {
			log.error(EELFLoggerDelegate.errorLogger, "getSolutionRevisionArtifactiContent", e);
		} 
		// TODO Auto-generated method stub
		return streamResource;
	}

	
}

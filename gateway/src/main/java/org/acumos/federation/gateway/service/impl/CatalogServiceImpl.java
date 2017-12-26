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
import org.acumos.nexus.client.RepositoryLocation;

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
//		baseSelector.put("provider", env.getProperty("federated.instance.name"));
	
	}

	/**
	 * This needs to be implemented for future enhancement where pagination is allowed
	 */
/*
	@Override
	public RestPageResponse<MLPSolution>  getPeerCatalogSolutions(Integer pageNumber, Integer maxSize, String sortingOrder,
			List<String> mlpModelTypes) {
		return null;
	}
*/
	@Override
	public List<MLPSolution> getSolutions(
		String mlpModelTypes, ServiceContext theContext) {
		
		log.debug(EELFLoggerDelegate.debugLogger, "getSolutions");
		List<MLPSolution> filteredMLPSolutions = null;
		ICommonDataServiceRestClient dataServiceRestClient = getClient();
		//TODO: revisit this code to pass query parameters to CCDS Service
		Map<String, Object> queryParameters = new HashMap<String, Object>(this.baseSelector);
		List<MLPSolution> mlpSolutions = dataServiceRestClient.searchSolutions(queryParameters, false);
		log.debug(EELFLoggerDelegate.debugLogger, "getSolutions: data service provided solutions " + mlpSolutions);

		if(mlpSolutions != null && mlpSolutions.size() > 0 && !Utils.isEmptyOrNullString(mlpModelTypes)) {
			//Filter List using Lamba to get solutions which matches the ML Model Type
			filteredMLPSolutions = mlpSolutions.stream()
																.filter(mlpSolution -> { String modelType = mlpSolution.getModelTypeCode();
																												 return modelType == null || //for testing only
																																mlpModelTypes.contains(modelType);	
																											 })
																.collect(Collectors.toList());
		} else {
			filteredMLPSolutions = mlpSolutions;
		}
		return filteredMLPSolutions;
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
		ICommonDataServiceRestClient dataServiceRestClient = getClient();
		List<MLPSolutionRevision> mlpSolutionRevisions =
			dataServiceRestClient.getSolutionRevisions(theSolutionId);
		return mlpSolutionRevisions;
	}

	@Override
	public MLPSolutionRevision getSolutionRevision(
		String theSolutionId, String theRevisionId, ServiceContext theContext) {

		log.debug(EELFLoggerDelegate.debugLogger, "getSolutionRevision");
		ICommonDataServiceRestClient dataServiceRestClient = getClient();
		MLPSolutionRevision mlpSolutionRevision =
			dataServiceRestClient.getSolutionRevision(theSolutionId, theRevisionId);
		return mlpSolutionRevision;
	}

	@Override
	public List<MLPArtifact> getSolutionRevisionArtifacts(
		String theSolutionId, String theRevisionId, ServiceContext theContext) {
		
		log.debug(EELFLoggerDelegate.debugLogger, "getSolutionRevisionArtifacts");
		List<MLPArtifact> mlpArtifacts = new ArrayList<>();
		FederationDataClient commonDataClient = getCommonDataClient();
		Iterable<MLPArtifact> artifacts = commonDataClient.getSolutionRevisionArtifacts(theSolutionId, theRevisionId);
		for (MLPArtifact mlpArtifact : artifacts) {
			mlpArtifacts.add(mlpArtifact);
		}
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
			
			String path = Utils.getTempFolderPath(mlpArtifact.getName(), mlpArtifact.getVersion(), env.getProperty("nexus.tempFolder", ""));
			
			RepositoryLocation repositoryLocation = new RepositoryLocation();
			repositoryLocation.setId("1");
			repositoryLocation.setUrl(env.getProperty("nexus.url"));
			repositoryLocation.setUsername(env.getProperty("nexus.username"));
			repositoryLocation.setPassword(env.getProperty("nexus.password"));
			repositoryLocation.setProxy(env.getProperty("nexus.proxy"));
			
			// if you need a proxy to access the Nexus
			NexusArtifactClient artifactClient = new NexusArtifactClient(repositoryLocation);
			
			byteArrayOutputStream = artifactClient.getArtifact(mlpArtifact.getUri());
			InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
			
			//Plain Old Java. Sprint 3 will use try resource handling
			if(inputStream != null) {
				streamResource = new InputStreamResource(inputStream);
			}
			if(byteArrayOutputStream != null) {
				byteArrayOutputStream.close();
			}
			Utils.deletetTempFiles(path);
			
		} catch (Exception e) {
			log.error(EELFLoggerDelegate.errorLogger, "getSolutionRevisionArtifactiContent", e);
		} 
		// TODO Auto-generated method stub
		return streamResource;
	}

	
}

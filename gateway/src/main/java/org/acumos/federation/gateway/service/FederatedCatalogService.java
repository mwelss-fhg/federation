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
package org.acumos.federation.gateway.service;

import java.io.File;
import java.util.List;

import org.springframework.core.io.InputStreamResource;

import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.cds.transport.RestPageResponse;

/**
 * 
 *
 */
public interface FederatedCatalogService {
	
	
	/**
	 * 
	 *  API to be invoked by Peer Acumos to fetch the Catalog Solutions Information.
	 *  Pageable Response allowing Peer Acumos's to Specify the Page Numb\er and the maximum results of the Solutions list.
	 * 
	 * @param pageNumber : Page Number for the a specific set of the Solution List
	 * 
	 * @param maxSize : Maximum Number of objects returned in the response
	 * 
	 * @param sortingOrder : Sorting Order Type for the Response
	 * 
	 * @param mlpModelTypes : List of the ML Model Types for which Catalog Solutions needs to be returned
	 * 
	 * @return Pageable List of the Catalog Solutions
	 */
	RestPageResponse<MLPSolution>  getPeerCatalogSolutions(Integer pageNumber, Integer maxSize, String sortingOrder, 
			List<String> mlpModelTypes);
	
	/**
	 * 
	 *  API to be invoked by Peer Acumos to fetch the Catalog Solutions List.
	 * 
	 * @param mlpModelTypes : Comma Separate value of the ML Model Types for which Catalog Solutions needs to be returned
	 * 
	 * @return List of the Catalog Solutions for the specified list of query parameters
	 */
	List<MLPSolution> getPeerCatalogSolutionsList(String mlpModelTypes);
	
	
	/**
	 * @param solutionId : SolutionId for which Solution Revision Needs to be returned
	 * 
	 * @return List of the Solution Revision for the specified solution Id
	 */
	List<MLPSolutionRevision> getPeerCatalogSolutionRevision(String solutionId);
	
	/**
	 * @param solutionId : SolutionId for which Solution Revision Artifacts Needs to be returned
	 * 
	 * @param revisionid : RevisionId of the Solution for which List of Artifacts are needed.
	 * 
	 * @return List of the Solution Artifacts for the specified solution Id & revisionId
	 */
	List<MLPArtifact> getPeerSolutionArtifacts(String solutionId, String revisionId);
	
	
	
	/**
	 * @param artifactId of the File stored in Nexus repository
	 * @return Artifact File for the Machine Learning Solution 
	 */
	InputStreamResource getPeerSolutionArtifactFile(String artifactId);
	
}

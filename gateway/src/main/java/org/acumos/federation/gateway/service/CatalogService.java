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

import java.util.List;
import java.util.Map;

import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.springframework.core.io.InputStreamResource;

/**
 * Handles access to the solutions catalog
 */
public interface CatalogService {

	/*
	 * 
	 * API to be invoked by Peer Acumos to fetch the Catalog Solutions Information.
	 * Pageable Response allowing Peer Acumos's to Specify the Page Numb\er and the
	 * maximum results of the Solutions list.
	 * 
	 * @param pageNumber : Page Number for the a specific set of the Solution List
	 * 
	 * @param maxSize : Maximum Number of objects returned in the response
	 * 
	 * @param sortingOrder : Sorting Order Type for the Response
	 * 
	 * @param mlpModelTypes : List of the ML Model Types for which Catalog Solutions
	 * needs to be returned
	 * 
	 * @return Pageable List of the Catalog Solutions
	 */
	/*
	 * RestPageResponse<MLPSolution> getPeerCatalogSolutions(Integer pageNumber,
	 * Integer maxSize, String sortingOrder, List<String> mlpModelTypes);
	 */

	/**
	 * API to be invoked by Peer Acumos to fetch the Catalog Solutions List.
	 * 
	 * @param theSelector
	 *            Comma Separate value of the ML Model Types for which Catalog
	 *            Solutions needs to be returned
	 * @param theContext
	 *            ServiceContext
	 * @return List of the Catalog Solutions for the specified list of query
	 *         parameters
	 */
	List<MLPSolution> getSolutions(Map<String, ?> theSelector, ServiceContext theContext);

	/**
	 * @param theSolutionId
	 *            SolutionId for which Solution Needs to be returned
	 * @param theContext
	 *            ServiceContext
	 * @return MLPSolution
	 */
	MLPSolution getSolution(String theSolutionId, ServiceContext theContext);

	/**
	 * @param theSolutionId
	 *            SolutionId for which Solution Revision Needs to be returned
	 * @param theContext
	 *            ServiceContext
	 * @return List of the Solution Revision for the specified solution Id
	 */
	List<MLPSolutionRevision> getSolutionRevisions(String theSolutionId, ServiceContext theContext);

	/**
	 * @param theSolutionId
	 *            SolutionId for which Solution Revision Needs to be returned
	 * @param theRevisionId
	 *            RevisionId of the Solution
	 * @param theContext
	 *            ServiceContext
	 * @return MLPSolutionRevision
	 */
	MLPSolutionRevision getSolutionRevision(String theSolutionId, String theRevisionId, ServiceContext theContext);

	/**
	 * @param theSolutionId
	 *            SolutionId for which Solution Revision Artifacts Needs to be
	 *            returned
	 * @param theRevisionId
	 *            RevisionId of the Solution for which List of Artifacts are needed.
	 * @param theContext
	 *            ServiceContext
	 * @return List of the Solution Artifacts for the specified solution Id and
	 *         revisionId
	 */
	List<MLPArtifact> getSolutionRevisionArtifacts(String theSolutionId, String theRevisionId,
			ServiceContext theContext);

	/**
	 * @param theArtifactId
	 *            of the File stored in Nexus repository
	 * @param theContext
	 *            ServiceContext
	 * @return Artifact File for the Machine Learning Solution
	 */
	InputStreamResource getSolutionRevisionArtifactContent(String theArtifactId, ServiceContext theContext);

}

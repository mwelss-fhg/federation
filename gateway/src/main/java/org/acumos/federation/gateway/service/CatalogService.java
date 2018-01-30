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
import java.util.Map;
import java.util.List;

import org.springframework.core.io.InputStreamResource;

import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.cds.transport.RestPageResponse;


/**
 * Handles access to the solutions catalog 
 */
public interface CatalogService {
	

	/**
	 * 
	 *  API to be invoked by Peer Acumos to fetch the Catalog Solutions List.
	 * 
	 * @param theSelector contains the selection criteria.
	 * 
	 * @return List of the Catalog Solutions for the specified list of query parameters
	 */
	public List<MLPSolution> getSolutions(Map<String,?> theSelector, ServiceContext theContext);
	
	public default List<MLPSolution> getSolutions(Map<String,?> theSelector) {
		return getSolutions(theSelector, ServiceContext.selfService());
	}

	/**
	 */
	public MLPSolution getSolution(String theSolutionId, ServiceContext theContext);
	
	public default MLPSolution getSolution(String theSolutionId) {
		return getSolution(theSolutionId, ServiceContext.selfService());
	}
	
	
	/**
	 * @param solutionId : SolutionId for which Solution Revision Needs to be returned
	 * 
	 * @return List of the Solution Revision for the specified solution Id
	 */
	public List<MLPSolutionRevision> getSolutionRevisions(
		String theSolutionId, ServiceContext theContext);
	
	public default List<MLPSolutionRevision> getSolutionRevisions(String theSolutionId) {
		return getSolutionRevisions(theSolutionId, ServiceContext.selfService());
	}
	/**
	 */
	public MLPSolutionRevision getSolutionRevision(
		String theSolutionId, String theRevisionId, ServiceContext theContext);

	public default MLPSolutionRevision getSolutionRevision(
		String theSolutionId, String theRevisionId) {
		return getSolutionRevision(theSolutionId, theRevisionId, ServiceContext.selfService());
	}

	/**
	 * @param solutionId : SolutionId for which Solution Revision Artifacts Needs to be returned
	 * 
	 * @param revisionid : RevisionId of the Solution for which List of Artifacts are needed.
	 * 
	 * @return List of the Solution Artifacts for the specified solution Id & revisionId
	 */
	public List<MLPArtifact> getSolutionRevisionArtifacts(
		String theSolutionId, String theRevisionId, ServiceContext theContext);
	
	public default List<MLPArtifact> getSolutionRevisionArtifacts(
		String theSolutionId, String theRevisionId) {
		return getSolutionRevisionArtifacts(theSolutionId, theRevisionId, ServiceContext.selfService());
	}
	
	/**
	 * @param artifactId of the File stored in Nexus repository
	 * @return Artifact File for the Machine Learning Solution 
	 */
	public InputStreamResource getSolutionRevisionArtifactContent(String theArtifactId, ServiceContext theContext);
	
}

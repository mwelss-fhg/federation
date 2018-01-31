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
 * Handles access to the solutions catalog. The APIs of tis interface take a
 * ServiceContext argument which determines the identity of the peer on bahalf
 * of whom the call is executed. This information allows us to tailor the
 * response according to a peer's granted access. It is the responsability of
 * each implementation to ensure that the peer on whose behalf the service is
 * executed only accesses solutions it was granted access to.
 */
public interface CatalogService {

	/**
	 * API to be invoked by Peer Acumos to fetch the Catalog Solutions List.
	 * 
	 * @param theSelector
	 *            contains the selection criteria. Must match the available criteria
	 *            in CDS.
	 * @param theContext
	 *            the execution context.
	 * 
	 * @return List of the Catalog Solutions for the selection criteria
	 */
	public List<MLPSolution> getSolutions(Map<String, ?> theSelector, ServiceContext theContext);

	/**
	 * Default interface for calls in behalf of the local Acumos service.
	 *
	 * @param theSelector
	 *            contains the selection criteria. Must match the available criteria
	 *            in CDS
	 * @return list of the solutions for the selection criteria
	 */
	public default List<MLPSolution> getSolutions(Map<String, ?> theSelector) {
		return getSolutions(theSelector, ServiceContext.selfService());
	}

	/**
	 * Retrieve a solution's details from CDS.
	 * 
	 * @param theSolutionId
	 *            solution identifier (UUID).
	 * @param theContext
	 *            the execution context
	 * @return solution information
	 */
	public MLPSolution getSolution(String theSolutionId, ServiceContext theContext);

	/**
	 * Default solution access interface for calls in behalf of the local Acumos
	 * service.
	 *
	 * @param theSolutionId
	 *            solution identifier (UUID).
	 * @return solution information
	 */
	public default MLPSolution getSolution(String theSolutionId) {
		return getSolution(theSolutionId, ServiceContext.selfService());
	}

	/**
	 * Provides revision information given a solution identifier.
	 * 
	 * @param theSolutionId
	 *            identifier of the solution whose revisions are to be provided
	 * @param theContext
	 *            the execution context
	 * @return list of the solution revision for the specified solution Id
	 */
	public List<MLPSolutionRevision> getSolutionRevisions(String theSolutionId, ServiceContext theContext);

	public default List<MLPSolutionRevision> getSolutionRevisions(String theSolutionId) {
		return getSolutionRevisions(theSolutionId, ServiceContext.selfService());
	}

	/**
	 * Access to a solution revision information.
	 *
	 * @param theSolutionId
	 *            solution identifier (UUID).
	 * @param theRevisionId
	 *            revision identifier (UUID).
	 * @param theContext
	 *            the execution context
	 * @return solution revision information
	 */
	public MLPSolutionRevision getSolutionRevision(String theSolutionId, String theRevisionId,
			ServiceContext theContext);

	/**
	 * Default solution revision access interface for calls in behalf of the local
	 * Acumos service.
	 *
	 * @param theSolutionId
	 *            solution identifier (UUID).
	 * @param theRevisionId
	 *            revision identifier (UUID).
	 * @return solution revision information
	 */
	public default MLPSolutionRevision getSolutionRevision(String theSolutionId, String theRevisionId) {
		return getSolutionRevision(theSolutionId, theRevisionId, ServiceContext.selfService());
	}

	/**
	 * Access the list of solution revision artifacts.
	 * 
	 * @param theSolutionId
	 *            solution identifier (UUID).
	 * @param theRevisionId
	 *            revision identifier (UUID).
	 * @param theContext
	 *            the execution context
	 * @return list of the related artifacts
	 */
	public List<MLPArtifact> getSolutionRevisionArtifacts(String theSolutionId, String theRevisionId,
			ServiceContext theContext);

	/**
	 * Default solution revision access interface for calls in behalf of the local
	 * Acumos service.
	 *
	 * @param theSolutionId
	 *            solution identifier (UUID).
	 * @param theRevisionId
	 *            revision identifier (UUID).
	 * @return list of the related artifacts
	 */
	public default List<MLPArtifact> getSolutionRevisionArtifacts(String theSolutionId, String theRevisionId) {
		return getSolutionRevisionArtifacts(theSolutionId, theRevisionId, ServiceContext.selfService());
	}

	/**
	 * Retrieve artifact content.
	 *
	 * @param theArtifactId
	 *            identifier of the acumos artifact whose content needs to be
	 *            retrieved
	 * @param theContext
	 *            the execution context
	 * @return resource containing access to the actual artifact content
	 */
	public InputStreamResource getSolutionRevisionArtifactContent(String theArtifactId, ServiceContext theContext);

}

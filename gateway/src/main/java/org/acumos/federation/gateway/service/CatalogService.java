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
import org.acumos.cds.domain.MLPDocument;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.federation.gateway.cds.Artifact;
import org.acumos.federation.gateway.cds.Document;
import org.acumos.federation.gateway.cds.Solution;
import org.acumos.federation.gateway.cds.SolutionRevision;

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
	 * @return List of the Catalog Solutions for the selection criteria. An empty list is returned when no
	 * solutions satisfy the criteria.
	 * @throws ServiceException if an error is encoutered during processing
	 */
	public List<MLPSolution> getSolutions(Map<String, ?> theSelector, ServiceContext theContext) throws ServiceException;

	/**
	 * Default interface for calls in behalf of the local Acumos service.
	 *
	 * @param theSelector
	 *            contains the selection criteria. Must match the available criteria
	 *            in CDS
	 * @return List of the Catalog Solutions for the selection criteria. An empty list is returned when no
	 * solutions satisfy the criteria.
	 * @throws ServiceException if an error is encoutered during processing
	 */
	public default List<MLPSolution> getSolutions(Map<String, ?> theSelector) throws ServiceException {
		return getSolutions(theSelector, selfService());
	}

	/**
	 * Retrieve a solution's details from CDS.
	 * 
	 * @param theSolutionId
	 *            solution identifier (UUID).
	 * @param theContext
	 *            the execution context
	 * @return Extended solution information, possibly inclulding related revision information.
	 * 				 Will return null if the given solution id does not match an existing solution;
	 * @throws ServiceException if an error is encoutered during processing
	 */
	public Solution getSolution(String theSolutionId, ServiceContext theContext) throws ServiceException ;

	/**
	 * Default solution access interface for calls in behalf of the local Acumos
	 * service.
	 *
	 * @param theSolutionId
	 *            solution identifier (UUID).
	 * @return Extended solution information
	 * @throws ServiceException if an error is encoutered during processing
	 */
	public default Solution getSolution(String theSolutionId) throws ServiceException {
		return getSolution(theSolutionId, selfService());
	}

	/**
	 * Create or update the given solution information set and directly associated information such as tags.
	 * Whether a create or update is attemopted depends on the creteTime property of the given solution: if 0
	 * a create will be attempted, otherwise an update.
	 * @param theSolution
	 *						extended solution information set
	 * @param theContext
	 *            the execution context
	 * @return the solution information in its new service representation
	 * @throws ServiceException if an error is encoutered during processing
	 */
	public Solution putSolution(Solution theSolution, ServiceContext theContext) throws ServiceException; 

	/**
	 * Provides revision information given a solution identifier.
	 * 
	 * @param theSolutionId
	 *            identifier of the solution whose revisions are to be provided
	 * @param theContext
	 *            the execution context
	 * @return list of the solution revision for the specified solution Id. Will return an empty list if the
	 * given solution does not have revisions. Null is return if no solution with the given id exists.
	 * @throws ServiceException if an error is encoutered during processing
	 */
	public List<MLPSolutionRevision> getSolutionRevisions(String theSolutionId, ServiceContext theContext) throws ServiceException ;

	public default List<MLPSolutionRevision> getSolutionRevisions(String theSolutionId) throws ServiceException {
		return getSolutionRevisions(theSolutionId, selfService());
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
	 * @return Extended solution revision information, possibly including related artifact information.
	 * 				 Null if given solution id/revision id do ont match existing information.
	 * @throws ServiceException if an error is encoutered during processing
	 */
	public SolutionRevision getSolutionRevision(String theSolutionId, String theRevisionId,
			ServiceContext theContext) throws ServiceException ;

	/**
	 * Default solution revision access interface for calls in behalf of the local
	 * Acumos service.
	 *
	 * @param theSolutionId
	 *            solution identifier (UUID).
	 * @param theRevisionId
	 *            revision identifier (UUID).
	 * @return Extended solution revision information
	 * @throws ServiceException if an error is encoutered during processing
	 */
	public default SolutionRevision getSolutionRevision(String theSolutionId, String theRevisionId) throws ServiceException {
		return getSolutionRevision(theSolutionId, theRevisionId, selfService());
	}

	/**
	 * Create or update the given solution revision information set.
	 * Whether a create or update is attemopted depends on the creteTime property of the given revision: if 0
	 * a create will be attempted, otherwise an update.
	 * Should this handle associated information such as artifacts and documents ?
	 * @param theRevision
	 *						Extended revision information set including potential artifacts/documents/..
	 * @param theContext
	 *            the execution context
	 * @return the SolutionRevision as it will now be provided by the service.
	 * @throws ServiceException if an error is encoutered during processing
	 */
	public SolutionRevision putSolutionRevision(SolutionRevision theRevision,	ServiceContext theContext) throws ServiceException; 

	/**
	 * Access the list of solution revision artifacts.
	 * 
	 * @param theSolutionId
	 *            solution identifier (UUID).
	 * @param theRevisionId
	 *            revision identifier (UUID).
	 * @param theContext
	 *            the execution context
	 * @return list of the related artifacts. Null is returned if the solution id or the revision id do not indicate existing items.
	 * @throws ServiceException if an error is encoutered during processing
	 */
	public List<MLPArtifact> getSolutionRevisionArtifacts(String theSolutionId, String theRevisionId,
			ServiceContext theContext) throws ServiceException;

	/**
	 * Default solution revision access interface for calls in behalf of the local
	 * Acumos service.
	 *
	 * @param theSolutionId
	 *            solution identifier (UUID).
	 * @param theRevisionId
	 *            revision identifier (UUID).
	 * @return list of the related artifacts
	 * @throws ServiceException if an error is encoutered during processing
	 */
	public default List<MLPArtifact> getSolutionRevisionArtifacts(String theSolutionId, String theRevisionId) throws ServiceException {
		return getSolutionRevisionArtifacts(theSolutionId, theRevisionId, selfService());
	}

	/**
	 * Retrieve artifact content.
	 *
	 * @param theArtifactId
	 *            identifier of the acumos artifact whose content needs to be
	 *            retrieved
	 * @param theContext
	 *            the execution context
	 * @return Extended artifact information
	 * @throws ServiceException if an error is encoutered during processing
	 */
	public Artifact getSolutionRevisionArtifact(String theArtifactId, ServiceContext theContext)
																																													throws ServiceException;

	/**
	 * Retrieve artifact details.
	 *
	 * @param theArtifactId
	 *            identifier of the acumos artifact whose content needs to be
	 *            retrieved
	 * @return Extended artifact information
	 * @throws ServiceException if an error is encoutered during processing
	 */
	public default Artifact getSolutionRevisionArtifact(String theArtifactId) throws ServiceException {
		return getSolutionRevisionArtifact(theArtifactId, selfService());
	}

	/**
	 * Access the list of solution revision documents.
	 * 
	 * @param theSolutionId
	 *            solution identifier (UUID).
	 * @param theRevisionId
	 *            revision identifier (UUID).
	 * @param theContext
	 *            the execution context
	 * @return list of the related documents. Null is returned if the solution id or the revision id do not indicate existing items.
	 * @throws ServiceException if an error is encoutered during processing
	 */
	public List<MLPDocument> getSolutionRevisionDocuments(String theSolutionId, String theRevisionId,
			ServiceContext theContext) throws ServiceException;

	/**
	 * Default solution revision access interface for calls in behalf of the local
	 * Acumos service.
	 *
	 * @param theSolutionId
	 *            solution identifier (UUID).
	 * @param theRevisionId
	 *            revision identifier (UUID).
	 * @return list of the related artifacts
	 * @throws ServiceException if an error is encoutered during processing
	 */
	public default List<MLPDocument> getSolutionRevisionDocuments(String theSolutionId, String theRevisionId) throws ServiceException {
		return getSolutionRevisionDocuments(theSolutionId, theRevisionId, selfService());
	}

	/**
	 * Retrieve document details.
	 *
	 * @param theDocumentId
	 *            identifier of the acumos document whose details needs to be
	 *            retrieved
	 * @param theContext
	 *            the execution context
	 * @return Extended document information
	 * @throws ServiceException if an error is encoutered during processing
	 */
	public Document getSolutionRevisionDocument(String theDocumentId, ServiceContext theContext)
																																													throws ServiceException;

	/**
	 * Retrieve document details.
	 *
	 * @param theDocumentId
	 *            identifier of the acumos document whose details needs to be
	 *            retrieved
	 * @return Extended document information
	 * @throws ServiceException if an error is encoutered during processing
	 */
	public default Document getSolutionRevisionDocument(String theDocumentId) throws ServiceException {
		return getSolutionRevisionDocument(theDocumentId, selfService());
	}
	
	/**
	 * This would belong as a static method of ServiceContext but ServicrCOntext are not beans so I cannot wire them to access the
	 * self bean; in here it exposes an implementation detail which is ugly ..
	 * @return Context
	 */
	public ServiceContext selfService();

}

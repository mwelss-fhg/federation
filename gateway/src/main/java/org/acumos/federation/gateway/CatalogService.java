/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2017-2019 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
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
package org.acumos.federation.gateway;

import java.util.List;

import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPCatalog;
import org.acumos.cds.domain.MLPDocument;
import org.acumos.cds.domain.MLPRevCatDescription;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;

/**
 * API for accessing catalogs and related items.
 *
 * Provides methods for finding catalogs, solutions, revisions, artifacts, and
 * documents, and for checking whether the current peer has access to them.
 */
public interface CatalogService {
	/**
	 * Get a list of all catalogs.
	 *
	 * @return List of catalogs.
	 */
	public List<MLPCatalog> getAllCatalogs();
	/**
	 * Get a list of the catalogs visible to the current peer.
	 *
	 * @return List of visible catalogs.
	 */
	public List<MLPCatalog> getCatalogs();
	/**
	 * Create a catalog
	 *
	 * @param catalog The catalog
	 */
	public void createCatalog(MLPCatalog catalog);
	/**
	 * Add a solution to a catalog
	 *
	 * @param solutionId The ID of the solution.
	 * @param catalogId The ID of the catalog.
	 */
	public void addSolution(String solutionId, String catalogId);
	/**
	 * Get a list of the solutions in a catalog.
	 *
	 * @param catalogId The ID of the catalog to search.
	 * @return List of solutions in the catalog.
	 */
	public List<MLPSolution> getSolutions(String catalogId);
	/**
	 * Get details of a solution.
	 *
	 * @param solutionId The ID of the solution to retrieve.
	 * @return The solution extended with a list of its revisions.
	 */
	public MLPSolution getSolution(String solutionId);
	/**
	 * Create a solution.
	 *
	 * @param solution The solution to create.
	 * @return The created solution.
	 */
	public MLPSolution createSolution(MLPSolution solution);
	/**
	 * Update a solution.
	 *
	 * @param solution The solution to update.
	 */
	public void updateSolution(MLPSolution solution);
	/**
	 * Save the picture for a solution.
	 *
	 * @param solutionId The ID of the solution.
	 * @param picture The picture for the solution.
	 */
	public void savePicture(String solutionId, byte[] picture);
	/**
	 * Get revisions of a solution.
	 *
	 * @param solutionId The ID of the solution.
	 * @return The revisions of the solution.
	 */
	public List<MLPSolutionRevision> getRevisions(String solutionId);
	/**
	 * Get details of a revision.
	 *
	 * If catalogId is null, no description and no documents for the
	 * revision are returned.
	 *
	 * @param revisionId The ID of the revision.
	 * @param catalogId The ID of the catalog (can be null).
	 * @return The revision extended with its description, artifacts, and documents.
	 */
	public MLPSolutionRevision getRevision(String revisionId, String catalogId);
	/**
	 * Create a revision.
	 *
	 * @param revision The revision to create.
	 * @return The created revision.
	 */
	public MLPSolutionRevision createRevision(MLPSolutionRevision revision);
	/**
	 * Update a revision.
	 *
	 * @param revision The revision to update.
	 */
	public void updateRevision(MLPSolutionRevision revision);
	/**
	 * Get artifacts of a revision.
	 *
	 * @param revisionId The ID of the revision.
	 * @return The artifacts of the revision.
	 */
	public List<MLPArtifact> getArtifacts(String revisionId);
	/**
	 * Get details of an artifact.
	 *
	 * @param artifactId The ID of the artifact.
	 * @return The artifact.
	 */
	public MLPArtifact getArtifact(String artifactId);
	/**
	 * Create an artifact.
	 *
	 * @param artifact The artifact to create.
	 * @return The created artifact.
	 */
	public MLPArtifact createArtifact(MLPArtifact artifact);
	/**
	 * Update an artifact.
	 *
	 * @param artifact The artifact to update.
	 */
	public void updateArtifact(MLPArtifact artifact);
	/**
	 * Add an artifact to a revision.
	 *
	 * @param solutionId The ID of the revision's solution.
	 * @param revisionId The ID of the revision.
	 * @param artifactId The ID of the artifact.
	 */
	public void addArtifact(String solutionId, String revisionId, String artifactId);
	/**
	 * Create a revision description in a catalog.
	 *
	 * @param revCatDescription The description
	 * @return The description
	 */
	public MLPRevCatDescription createDescription(MLPRevCatDescription revCatDescription);
	/**
	 * Update a revision description in a catalog.
	 *
	 * @param revCatDescription The description
	 */
	public void updateDescription(MLPRevCatDescription revCatDescription);
	/**
	 * Delete a revision description from a catalog.
	 *
	 * @param revisionId The ID of the revision.
	 * @param catalogId The ID of the catalog.
	 */
	public void deleteDescription(String revisionId, String catalogId);
	/**
	 * Get documents of a revision in a catalog.
	 *
	 * @param revisionId The ID of the revision.
	 * @param catalogId The ID of the catalog.
	 * @return The documents.
	 */
	public List<MLPDocument> getDocuments(String revisionId, String catalogId);
	/**
	 * Get details of a document.
	 *
	 * @param documentId The ID of the document.
	 * @return The document.
	 */
	public MLPDocument getDocument(String documentId);
	/**
	 * Create a document.
	 *
	 * @param document The document.
	 * @return The document.
	 */
	public MLPDocument createDocument(MLPDocument document);
	/**
	 * Update a document.
	 *
	 * @param document The document.
	 */
	public void updateDocument(MLPDocument document);
	/**
	 * Add a document to a catalog.
	 *
	 * @param revisionId The ID of the revision.
	 * @param catalogId The ID of the catalog.
	 * @param documentId The ID of the document.
	 */
	public void addDocument(String revisionId, String catalogId, String documentId);
	/**
	 * Determine whether the current peer has access to a catalog.
	 *
	 * A peer should have access if the catalog is public or if the peer
	 * has been granted specific access to the catalog.
	 *
	 * @param catalogId The ID of the catalog.
	 * @return true if the current peer has access.
	 */
	public boolean isCatalogAllowed(String catalogId);
	/**
	 * Determine whether the current peer has access to a solution.
	 *
	 * A peer should have access if the solution is in a catalog
	 * the peer can access.
	 *
	 * @param solutionId The ID of the solution.
	 * @return true if the current peer has access.
	 */
	public boolean isSolutionAllowed(String solutionId);
	/**
	 * Determine whether the current peer has access to a revision.
	 *
	 * A peer should have access if the revision is in a solution
	 * the peer can access.
	 *
	 * @param revisionId The ID of the revision.
	 * @return true if the current peer has access.
	 */
	public boolean isRevisionAllowed(String revisionId);
	/**
	 * Determine whether the current peer has access to an artifact.
	 *
	 * A peer should have access if the artifact is in a revision
	 * the peer can access.
	 *
	 * @param artifactId The ID of the artifact.
	 * @return true if the current peer has access.
	 */
	public boolean isArtifactAllowed(String artifactId);
	/**
	 * Determine whether the current peer has access to a document.
	 *
	 * A peer should have access if the document is in a catalog
	 * the peer can access.
	 *
	 * @param documentId The ID of the document.
	 * @return true if the current peer has access.
	 */
	public boolean isDocumentAllowed(String documentId);
}

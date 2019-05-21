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

import java.io.InputStream;

import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPDocument;

/**
 * API for accessing content of artifacts and documents
 */
public interface ContentService {
	/**
	 * Get the body of an artifact.
	 *
	 * @param artifact The artifact to retrieve.
	 * @return An InputStream for reading the artifact's content.
	 */
	public InputStream getArtifactContent(MLPArtifact artifact);

	/**
	 * Set the URI for an artifact.
	 *
	 * @param solutionId The ID of the solution.
	 * @param artifact The artifact to set the URI on.
	 */
	public void setArtifactUri(String solutionId, MLPArtifact artifact);

	/**
	 * Put the content of an artifact.
	 *
	 * @param artifact The artifact to put.
	 * @param tag The image tag in the input data.
	 * @param is The data to put.  Implementations must close the input stream.
	 */
	public void putArtifactContent(MLPArtifact artifact, String tag, InputStream is);

	/**
	 * Get the body of a document.
	 *
	 * @param document The document to retrieve.
	 * @return An InputStream for reading the document's content.
	 */
	public InputStream getDocumentContent(MLPDocument document);

	/**
	 * Set the URI for an document.
	 *
	 * @param solutionId The ID of the solution.
	 * @param document The document to set the URI on.
	 */
	public void setDocumentUri(String solutionId, MLPDocument document);

	/**
	 * Put the content of a document.
	 *
	 * @param document The document to put.
	 * @param is The data to put.  Implementations must close the input stream.
	 */
	public void putDocumentContent(MLPDocument document, InputStream is);
}

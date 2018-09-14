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

import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPDocument;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

/**
 * Handles access to the content repository. 
 */
public interface ContentService {

	/**
	 * @param theSolutionId
	 *						The solution the revision belongs to
	 * @param theRevisionId
	 *						The solution revision the artifact belongs to
	 * @param theArtifact
	 *            The CDS representation of artifact metadata
	 * @param theContext
	 *            the execution context
	 * @return resource containing access to the actual artifact content
	 */
	public InputStreamResource getArtifactContent(
			String theSolutionId, String theRevisionId, MLPArtifact theArtifact, ServiceContext theContext)
																																										throws ServiceException;

	/**
	 * @param theSolutionId
	 *						The solution the revision belongs to
	 * @param theRevisionId
	 *						The solution revision the artifact belongs to
	 * @param theArtifact
	 *            The CDS representation of artifact metadata
	 * @return resource containing access to the actual artifact content
	 */
	public default InputStreamResource getArtifactContent(
			String theSolutionId, String theRevisionId, MLPArtifact theArtifact)					throws ServiceException {
		return getArtifactContent(theSolutionId, theRevisionId, theArtifact, selfService());
	}

	/**
	 * If the call is succesful the artifact information is updated with the content uri.
	 * @param theSolutionId
	 *						The solution the revision belongs to
	 * @param theRevisionId
	 *						The solution revision the artifact belongs to
	 * @param theArtifact
	 *            The CDS representation of artifact metadata
	 * @param theResource
	 *            the resource providing the artifact content
	 * @param theContext
	 *            the service execution context
	 */
	public void putArtifactContent(
		String theSolutionId, String theRevisionId, MLPArtifact theArtifact, Resource theResource, ServiceContext theContext)
																																										throws ServiceException;
	/**
	 * If the call is succesful the artifact information is updated with the content uri.
	 * @param theSolutionId
	 *						The solution the revision belongs to
	 * @param theRevisionId
	 *						The solution revision the artifact belongs to
	 * @param theArtifact
	 *            The CDS representation of artifact metadata
	 * @param theResource
	 *            the resource providing the artifact content
	 */
	public default void putArtifactContent(
		String theSolutionId, String theRevisionId, MLPArtifact theArtifact, Resource theResource)
																																										throws ServiceException {
		putArtifactContent(theSolutionId, theRevisionId, theArtifact, theResource, selfService());
	}

	/**
	 * @param theSolutionId
	 *						The solution the revision belongs to
	 * @param theRevisionId
	 *						The solution revision the artifact belongs to
	 * @param theDocument
	 *            The CDS representation of document metadata
	 * @param theContext
	 *            the execution context
	 * @return resource containing access to the actual document content
	 */
	public InputStreamResource getDocumentContent(
		String theSolutionId, String theRevisionId, MLPDocument theDocument, ServiceContext theContext)
																																										throws ServiceException;

	/**
	 * @param theSolutionId
	 *						The solution the revision belongs to
	 * @param theRevisionId
	 *						The solution revision the artifact belongs to
	 * @param theDocument
	 *            The CDS representation of document metadata
	 * @return resource containing access to the actual document content
	 */
	public default InputStreamResource getDocumentContent(
		String theSolutionId, String theRevisionId, MLPDocument theDocument) throws ServiceException {

		return getDocumentContent(theSolutionId, theRevisionId, theDocument, selfService());
	}

	/**
	 * If the call is succesful the document information is updated with the content uri.
	 * @param theSolutionId
	 *						The solution the revision belongs to
	 * @param theRevisionId
	 *						The solution revision the artifact belongs to
	 * @param theDocument
	 *            The CDS representation of document metadata
	 * @param theResource
	 *            the resource providing the document content
	 * @param theContext
	 *            the execution context
	 */
	public void putDocumentContent(
		String theSolutionId, String theRevisionId, MLPDocument theDocument, Resource theResource, ServiceContext theContext)
																																										throws ServiceException;

	/**
	 * If the call is succesful the document information is updated with the content uri.
	 * @param theSolutionId
	 *						The solution the revision belongs to
	 * @param theRevisionId
	 *						The solution revision the artifact belongs to
	 * @param theDocument
	 *            The CDS representation of document metadata
	 * @param theResource
	 *            the resource providing the document content
	 */
	public default void putDocumentContent(
		String theSolutionId, String theRevisionId, MLPDocument theDocument, Resource theResource)
																																										throws ServiceException {
		putDocumentContent(theSolutionId, theRevisionId, theDocument, theResource, selfService());
	}


	/**
	 * Provide a self service execution context.
	 */
	public ServiceContext selfService();
}

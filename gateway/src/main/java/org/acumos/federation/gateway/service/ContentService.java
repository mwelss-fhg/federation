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

import org.acumos.federation.gateway.cds.Artifact;
import org.acumos.federation.gateway.cds.Document;
import org.springframework.core.io.Resource;

/**
 * Handles access to the content repository. 
 */
public interface ContentService {

	/**
	 * @param theArtifact
	 *            The CDS representation of artifact metadata
	 * @param theContext
	 *            the execution context
	 * @return resource containing access to the actual artifact content
	 * @throws ServiceException On failure
	 */
	public Resource getArtifactContent(Artifact theArtifact, ServiceContext theContext)
																																										throws ServiceException;

	/**
	 * @param theArtifact
	 *            The CDS representation of artifact metadata
	 * @return resource containing access to the actual artifact content
	 * @throws ServiceException On failure
	 */
	public default Resource getArtifactContent(Artifact theArtifact)					throws ServiceException {
		return getArtifactContent(theArtifact, selfService());
	}

	/**
	 * If the call is succesful the artifact information is updated with the content uri.
	 * @param theSolutionId
	 *            The solution id
	 * @param theArtifact
	 *            The CDS representation of artifact metadata
	 * @param theResource
	 *            the resource providing the artifact content
	 * @param theContext
	 *            the service execution context
	 * @throws ServiceException On failure
	 */
	public void putArtifactContent(String theSolutionId, Artifact theArtifact, Resource theResource, ServiceContext theContext) throws ServiceException;
	/**
	 * If the call is succesful the artifact information is updated with the content uri.
	 * @param theSolutionId
	 *            The solution id
	 * @param theArtifact
	 *            The CDS representation of artifact metadata
	 * @param theResource
	 *            the resource providing the artifact content
	 * @throws ServiceException On failure
	 */
	public default void putArtifactContent(String theSolutionId, Artifact theArtifact, Resource theResource) throws ServiceException {
		putArtifactContent(theSolutionId, theArtifact, theResource, selfService());
	}

	/**
	 * @param theDocument
	 *            The CDS representation of document metadata
	 * @param theContext
	 *            the execution context
	 * @return resource containing access to the actual document content
	 * @throws ServiceException On failure
	 */
	public Resource getDocumentContent(Document theDocument, ServiceContext theContext) throws ServiceException;

	/**
	 * @param theDocument
	 *            The CDS representation of document metadata
	 * @return resource containing access to the actual document content
	 * @throws ServiceException On failure
	 */
	public default Resource getDocumentContent(Document theDocument) throws ServiceException {
		return getDocumentContent(theDocument, selfService());
	}

	/**
	 * If the call is successful the document information is updated with the content uri.
	 * @param theSolutionId
	 *            The solution id
	 * @param theDocument
	 *            The CDS representation of document metadata
	 * @param theResource
	 *            the resource providing the document content
	 * @param theContext
	 *            the execution context
	 * @throws ServiceException On failure
	 */
	public void putDocumentContent(String theSolutionId, Document theDocument, Resource theResource, ServiceContext theContext) throws ServiceException;

	/**
	 * If the call is successful the document information is updated with the content uri.
	 * @param theSolutionId
	 *            The solution id
	 * @param theDocument
	 *            The CDS representation of document metadata
	 * @param theResource
	 *            the resource providing the document content
	 * @throws ServiceException On failure
	 */
	public default void putDocumentContent(String theSolutionId, Document theDocument, Resource theResource) throws ServiceException {
		putDocumentContent(theSolutionId, theDocument, theResource, selfService());
	}


	/**
	 * Provide a self service execution context.
	 * @return Context
	 */
	public ServiceContext selfService();
}

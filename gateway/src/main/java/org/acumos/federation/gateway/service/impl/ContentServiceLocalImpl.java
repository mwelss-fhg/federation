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
package org.acumos.federation.gateway.service.impl;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;

import org.acumos.federation.gateway.cds.Artifact;
import org.acumos.federation.gateway.cds.Document;
import org.acumos.federation.gateway.service.ContentService;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.service.ServiceException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * File based implementation of the ContentService.
 *
 */
@Service
public class ContentServiceLocalImpl extends AbstractServiceImpl
																	implements ContentService {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * @return a resource containing the content or null if the artifact has no content
	 * @throws ServiceException if failing to retrieve artifact information or retrieve content 
	 */
	@Override
	public Resource getArtifactContent(
		String theSolutionId, String theRevisionId, Artifact theArtifact, ServiceContext theContext)
																																															throws ServiceException {
		if (theArtifact.getUri() == null) {
			throw new ServiceException("No artifact uri available for " + theArtifact);
		}

		try {
			return new InputStreamResource(new URI(theArtifact.getUri()).toURL().openStream());
		}
		catch (Exception x) {
			log.error("Failed to retrieve artifact content for artifact " + theArtifact, x);
			throw new ServiceException("Failed to retrieve artifact content for artifact " + theArtifact, x);
		}
	}

	/**
	 * Should add a configuration parameter for the location of the file.
	 */
	@Override
	public void putArtifactContent(
		String theSolutionId, String theRevisionId, Artifact theArtifact, Resource theResource, ServiceContext theContext)
																																										throws ServiceException {
		File target = null;
		try {
			target = File.createTempFile(theArtifact.getName() + "-" + theArtifact.getVersion(), null /*""*//*,File directory*/);
			FileUtils.copyInputStreamToFile(theResource.getInputStream(), target);
		}
		catch (IOException iox) {
			log.error("Failed to write artifact content for artifact " + theArtifact, iox);
			throw new ServiceException("Failed to write artifact content for artifact " + theArtifact, iox);
		}

		theArtifact.setUri(target.toURI().toString());
	}	

	@Override
	public Resource getDocumentContent(
		String theSolutionId, String theRevisionId, Document theDocument, ServiceContext theContext)
																																										throws ServiceException {
		if (theDocument.getUri() == null) {
			throw new ServiceException("No document uri available for " + theDocument);
		}

		try {
			return new InputStreamResource(new URI(theDocument.getUri()).toURL().openStream());
		}
		catch (Exception x) {
			log.error("Failed to retrieve document content for document " + theDocument, x);
			throw new ServiceException("Failed to retrieve document content for document " + theDocument, x);
		}
	}

	@Override
	public void putDocumentContent(
		String theSolutionId, String theRevisionId, Document theDocument, Resource theResource, ServiceContext theContext)
																																										throws ServiceException {
		File target = null;
		try {
			target = File.createTempFile(theDocument.getName() + "-" + theDocument.getVersion(), null /*""*//*,File directory*/);
			FileUtils.copyInputStreamToFile(theResource.getInputStream(), target);
		}
		catch (IOException iox) {
			log.error("Failed to write document content for document " + theDocument, iox);
			throw new ServiceException("Failed to write document content for document " + theDocument, iox);
		}

		theDocument.setUri(target.toURI().toString());
	}

}

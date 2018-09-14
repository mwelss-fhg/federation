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
package org.acumos.federation.gateway.service.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.invoke.MethodHandles;

import org.acumos.cds.AccessTypeCode;
import org.acumos.cds.domain.MLPDocument;
import org.acumos.cds.domain.MLPArtifact;
import org.acumos.federation.gateway.cds.ArtifactType;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.config.NexusConfiguration;
import org.acumos.federation.gateway.config.DockerConfiguration;
import org.acumos.federation.gateway.service.ContentService;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.service.ServiceException;
import org.acumos.nexus.client.NexusArtifactClient;
import org.acumos.nexus.client.data.UploadArtifactInfo;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Identifier;
import com.github.dockerjava.api.model.Repository;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.core.command.PushImageResultCallback;


/**
 * Nexus based implementation of the ContentService.
 *
 */
@Service
public class ContentServiceImpl extends AbstractServiceImpl
																	implements ContentService {

	private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private NexusConfiguration nexusConfig;
  @Autowired
  private DockerConfiguration dockerConfig;

	@Override
	public InputStreamResource getArtifactContent(
		String theSolutionId, String theRevisionId, MLPArtifact theArtifact, ServiceContext theContext)
																																															throws ServiceException {
		if (theArtifact.getUri() == null) {
			throw new ServiceException("No artifact uri available for " + theArtifact);
		}
		log.info(EELFLoggerDelegate.debugLogger, "Retrieving artifact content for {}", theArtifact);

		if (ArtifactType.DockerImage == ArtifactType.forCode(theArtifact.getArtifactTypeCode())) {
			try {
				//pull followed by save
				DockerClient docker = this.dockerConfig.getDockerClient();

				try (PullImageResultCallback pullResult = new PullImageResultCallback()) {
					docker.pullImageCmd(theArtifact.getUri())
								.exec(pullResult);
					pullResult.awaitCompletion();
				}

				return new InputStreamResource(docker.saveImageCmd(theArtifact.getUri()).exec());
			}
			catch (Exception x) {
				log.error(EELFLoggerDelegate.errorLogger, "Failed to retrieve artifact content for docker artifact " + theArtifact, x);
				throw new ServiceException("Failed to retrieve artifact content for docker artifact " + theArtifact, x);
			}
		}
		else {	
			return getNexusContent(theArtifact.getUri());
		}
	}

	@Override
	public void putArtifactContent(
		String theSolutionId, String theRevisionId, MLPArtifact theArtifact, Resource theResource, ServiceContext theContext)
																																														throws ServiceException {

		if (ArtifactType.DockerImage == ArtifactType.forCode(theArtifact.getArtifactTypeCode())) {
			try {
				//load followed by push
				DockerClient docker = this.dockerConfig.getDockerClient();

				docker.loadImageCmd(theResource.getInputStream())
							.exec(); //sync xecution

				// there is an assumption here that the repo info was stripped from the artifact name by the originator
				Identifier imageId =
					new Identifier(
						new Repository(dockerConfig.getRegistryUrl().toString()),
													 theArtifact.getName() /*the tag*/);
				try (PushImageResultCallback pushResult = new PushImageResultCallback()) {
					docker.pushImageCmd(imageId)
								.exec(pushResult);
					pushResult.awaitCompletion();
				}	
				// update artifact with local repo reference. we also update the name and description in order to stay
				// alligned with on-boarding's unwritten rules
				theArtifact.setUri(imageId.toString());
				theArtifact.setName(imageId.toString());
				theArtifact.setDescription(imageId.toString());
			}
			catch (Exception x) {
				log.error(EELFLoggerDelegate.errorLogger,
									"Failed to push docker artifact content to Nexus repo", x);
				throw new ServiceException("Failed to push docker artifact content to Nexus repo", x);
			}
		}
		else {
			String[] nameParts = splitName(theArtifact.getName());
			UploadArtifactInfo info = putNexusContent(
				nexusPrefix(theSolutionId, theRevisionId), nameParts[0], theArtifact.getVersion(), nameParts[1], theResource);
			// update artifact with local repo reference
			theArtifact.setUri(info.getArtifactMvnPath());
		}
	}

	@Override
	public InputStreamResource getDocumentContent(
		String theSolutionId, String theRevisionId, MLPDocument theDocument, ServiceContext theContext)
																																										throws ServiceException {
		if (theDocument.getUri() == null) {
			throw new ServiceException("No document uri available for " + theDocument);
		}
		log.info(EELFLoggerDelegate.debugLogger, "Retrieving document content for {}", theDocument);
		return getNexusContent(theDocument.getUri());
	}

	@Override
	public void putDocumentContent(
		String theSolutionId, String theRevisionId, MLPDocument theDocument, Resource theResource, ServiceContext theContext)
																																										throws ServiceException {
		String[] nameParts = splitName(theDocument.getName());
		UploadArtifactInfo info = putNexusContent(
			nexusPrefix(theSolutionId, theRevisionId), nameParts[0], AccessTypeCode.PB.name(), nameParts[1], theResource);
		theDocument.setUri(info.getArtifactMvnPath());
	}

	protected InputStreamResource getNexusContent(String theUri) throws ServiceException {
		try {
			NexusArtifactClient artifactClient = this.nexusConfig.getNexusClient();
			ByteArrayOutputStream artifactContent = artifactClient.getArtifact(theUri);
			log.info(EELFLoggerDelegate.debugLogger, "Retrieved {} bytes of content from {}", artifactContent.size(), theUri);
			return new InputStreamResource(
										new ByteArrayInputStream(
											artifactContent.toByteArray()
									));
		}
		catch (Exception x) {
			log.error(EELFLoggerDelegate.errorLogger, "Failed to retrieve content from  " + theUri, x);
			throw new ServiceException("Failed to retrieve content from " + theUri, x);
		}
	}

	protected UploadArtifactInfo putNexusContent(
		String theGroupId, String theContentId, String theVersion, String thePackaging, Resource theResource) throws ServiceException {

		try {
			UploadArtifactInfo info = this.nexusConfig.getNexusClient()
																	.uploadArtifact(theGroupId, theContentId, theVersion, thePackaging,
																									theResource.contentLength(), theResource.getInputStream());

			log.info(EELFLoggerDelegate.debugLogger, "Wrote artifact content to {}", info.getArtifactMvnPath());
			return info;
		}
		catch (Exception x) {
			log.error(EELFLoggerDelegate.errorLogger,	"Failed to push content to Nexus repo", x);
			throw new ServiceException("Failed to push content to Nexus repo", x);
		}
	}

	private String nexusPrefix(String theSolutionId, String theRevisionId) {
		return String.join(nexusConfig.getNameSeparator(), nexusConfig.getGroupId(), theSolutionId, theRevisionId);
	}

	/**
	 * Split a file name into its core name and extension parts.
	 * @param theName file name to split
	 * @return a string array containing the 2 part or null if the part was missing
	 */
	private String[] splitName(String theName) {
		int pos = theName.lastIndexOf('.');
		return (pos < 0) ?
			new String[] {theName, "" /*null: better coding but does not facilitate callers*/} :
			pos == theName.length() - 1 ? new String[] {theName.substring(0,pos), ""} :
																		new String[] {theName.substring(0,pos), theName.substring(pos+1)};
	}
}

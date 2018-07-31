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
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.stream.Collectors;

import java.lang.invoke.MethodHandles;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;

import org.acumos.federation.gateway.cds.ArtifactType;
import org.acumos.federation.gateway.util.Utils;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.service.ArtifactService;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.service.ServiceException;

import org.acumos.nexus.client.NexusArtifactClient;
import org.acumos.nexus.client.data.UploadArtifactInfo;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Identifier;
import com.github.dockerjava.api.model.Repository;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.core.command.PushImageResultCallback;

import org.acumos.cds.AccessTypeCode;
import org.acumos.cds.ValidationStatusCode;
import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.domain.MLPArtifact;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Conditional;


/**
 * CDS based implementation of the CatalogService.
 *
 */
@Service
public class ArtifactServiceImpl extends AbstractServiceImpl
																	implements ArtifactService {

	private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * @return a resource containing the content or null if the artifact has no content
	 * @throws ServiceException if failing to retrieve artifact information or retrieve content 
	 */
	@Override
	public InputStreamResource getArtifactContent(MLPArtifact theArtifact, ServiceContext theContext)
																																															throws ServiceException {
		if (theArtifact.getUri() == null) {
			throw new ServiceException("No artifact uri available for " + theArtifact);
		}
		log.info(EELFLoggerDelegate.debugLogger, "Retrieving artifact content for {}",theArtifact);

		InputStreamResource streamResource = null;
		try {
			if (ArtifactType.DockerImage == ArtifactType.forCode(theArtifact.getArtifactTypeCode())) {
				//pull followed by save
				DockerClient docker = this.clients.getDockerClient();

				try (PullImageResultCallback pullResult = new PullImageResultCallback()) {
					docker.pullImageCmd(theArtifact.getUri())
								.exec(pullResult);
					pullResult.awaitCompletion();
				}

				return new InputStreamResource(docker.saveImageCmd(theArtifact.getUri()).exec());
			}
			else {	
				NexusArtifactClient artifactClient = this.clients.getNexusClient();
				ByteArrayOutputStream artifactContent = artifactClient.getArtifact(theArtifact.getUri());
				log.info(EELFLoggerDelegate.debugLogger, "Retrieved {} bytes of artifact content", artifactContent.size());
				streamResource = new InputStreamResource(
													new ByteArrayInputStream(
														artifactContent.toByteArray()
												));
			}
		}
		catch (Exception x) {
			log.error(EELFLoggerDelegate.errorLogger, "Failed to retrieve artifact content for artifact " + theArtifact, x);
			throw new ServiceException("Failed to retrieve artifsact content for artifact " + theArtifact, x);
		}
		return streamResource;
	}

	public void putArtifactContent(MLPArtifact theArtifact, Resource theResource) throws ServiceException {
		UploadArtifactInfo uploadInfo = null;
		try {
			if (ArtifactType.DockerImage == ArtifactType.forCode(theArtifact.getArtifactTypeCode())) {
				//load followed by push

				DockerClient docker = this.clients.getDockerClient();

				docker.loadImageCmd(theResource.getInputStream())
							.exec(); //sync xecution

				// there is an assumption here that the repo info was stripped from the artifact name by the originator
				Identifier imageId =
					new Identifier(
						new Repository(this.clients.getDockerProperty("registryUrl").toString()),
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
			else {
				uploadInfo = this.clients.getNexusClient()
											.uploadArtifact((String)this.clients.getNexusProperty("groupId"),
																			theArtifact.getName(), /* probably wrong */
																			theArtifact.getVersion(),
																			"",
																			theResource.contentLength(),
																			theResource.getInputStream());
				log.info(EELFLoggerDelegate.debugLogger, "Wrote artifact content to {}", uploadInfo.getArtifactMvnPath());
				// update artifact with local repo reference
				theArtifact.setUri(uploadInfo.getArtifactMvnPath());
			}
		}
		catch (Exception x) {
			log.error(EELFLoggerDelegate.errorLogger,
								"Failed to push artifact content to local Nexus repo", x);
			throw new ServiceException("Failed to push artifact content to local Nexus repo", x);
		}

	}
}

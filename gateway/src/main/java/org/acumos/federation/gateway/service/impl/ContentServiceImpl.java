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

import java.io.InputStream;

import java.net.URI;
import java.net.URISyntaxException;

import java.lang.invoke.MethodHandles;

import org.acumos.cds.AccessTypeCode;
import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPDocument;
import org.acumos.federation.gateway.cds.ArtifactType;
import org.acumos.federation.gateway.config.DockerConfiguration;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.config.NexusConfiguration;
import org.acumos.federation.gateway.service.ContentService;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.service.ServiceException;

import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Identifier;
import com.github.dockerjava.api.model.Repository;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.core.command.PushImageResultCallback;

import org.apache.commons.io.input.ProxyInputStream;

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
	public Resource getArtifactContent(
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
					log.debug(EELFLoggerDelegate.debugLogger, "Completed docker image pull for {}", theArtifact);
				}

				InputStream imageSource = docker.saveImageCmd(theArtifact.getUri()).exec();
				log.debug(EELFLoggerDelegate.debugLogger, "Completed docker image save for {}", theArtifact);

				return new InputStreamResource(imageSource);
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
				TrackingInputStream imageSource = new TrackingInputStream(theResource.getInputStream());
				//load followed by push
				DockerClient docker = this.dockerConfig.getDockerClient();

				docker.loadImageCmd(imageSource)
							.exec(); //sync xecution
				log.debug(EELFLoggerDelegate.debugLogger, "Completed docker image load for {}. Transfered {} bytes in {} seconds.",
					theArtifact, imageSource.size(), imageSource.duration()/1000);

				// there is an assumption here that the repo info was stripped from the artifact name by the originator
				Identifier imageId =
					new Identifier(
						new Repository(dockerConfig.getRegistryUrl().toString()),
													 theArtifact.getName() /*the tag*/);
				try (PushImageResultCallback pushResult = new PushImageResultCallback()) {
					docker.pushImageCmd(imageId)
								.exec(pushResult);
					pushResult.awaitCompletion();
					log.debug(EELFLoggerDelegate.debugLogger, "Completed docker image push for {}", theArtifact);
				}	
				// update artifact with local repo reference. we also update the name and description in order to stay
				// alligned with on-boarding's unwritten rules
				theArtifact.setSize((int)imageSource.size()); //?? is this correct
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
			String uri = putNexusContent(
				nexusPrefix(theSolutionId, theRevisionId), nameParts[0], theArtifact.getVersion(), nameParts[1], theResource);
			// update artifact with local repo reference
			theArtifact.setUri(uri);
		}
	}

	@Override
	public Resource getDocumentContent(
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
		String uri = putNexusContent(
			nexusPrefix(theSolutionId, theRevisionId), nameParts[0], AccessTypeCode.PB.name(), nameParts[1], theResource);
		theDocument.setUri(uri);
	}

	protected Resource getNexusContent(String theUri) throws ServiceException {
		URI contentUri = null;
		try {
			contentUri = new URI(this.nexusConfig.getUrl() + theUri); 
			log.info(EELFLoggerDelegate.debugLogger, "Query for {}", contentUri);
			ResponseEntity<Resource> response = null;
			RequestEntity<Void> request = RequestEntity
																		.get(contentUri)
																		.accept(MediaType.ALL)
																		.build();
			response = this.nexusConfig.getNexusClient().exchange(request, Resource.class);
			return response.getBody();	
		}
		catch (HttpStatusCodeException x) {
			log.error(EELFLoggerDelegate.errorLogger, "Failed to retrieve nexus content from  " + theUri + "(" + contentUri + ")", x);
			throw new ServiceException("Failed to retrieve nexus content from " + contentUri, x);
		}
		catch (Throwable t) {
			log.error(EELFLoggerDelegate.errorLogger, "Unexpected failure for " + contentUri + "(" + contentUri + ")", t);
			throw new ServiceException("Unexpected failure for " + contentUri, t);
		}

	}

	protected String putNexusContent(
		String theGroupId, String theContentId, String theVersion, String thePackaging, Resource theResource) throws ServiceException {

		try {
			String path = nexusPath(theGroupId, theContentId, theVersion, thePackaging);
			URI uri = new URI(this.nexusConfig.getUrl() + path);
			log.info(EELFLoggerDelegate.debugLogger, "Writing artifact content to nexus at {}", path);
			RequestEntity<Resource> request = RequestEntity
																					.put(uri)
																					.contentType(MediaType.APPLICATION_OCTET_STREAM)
																					//.contentLength()
																					.body(theResource);
			ResponseEntity<Void> response = this.nexusConfig.getNexusClient().exchange(request, Void.class);
			log.debug(EELFLoggerDelegate.debugLogger, "Writing artifact content to {} resulted in {}", path, response.getStatusCode());
			if (response.getStatusCode().is2xxSuccessful()) {
				log.info(EELFLoggerDelegate.debugLogger, "Wrote artifact content to {}", path);
				return path;
			}
			else
				throw new ServiceException("Failed to write artifact content to nexus. Got " + response.getStatusCode());
		}
		catch (Exception x) {
			log.error(EELFLoggerDelegate.errorLogger,	"Failed to push content to Nexus repo", x);
			throw new ServiceException("Failed to push content to Nexus repo", x);
		}
	}

	/**
	 * This builds the prefix passed to nexusPath
	 */
	private String nexusPrefix(String theSolutionId, String theRevisionId) {
		return String.join(nexusConfig.getNameSeparator(), nexusConfig.getGroupId(), theSolutionId, theRevisionId);
	}

	/**
	 * This mimics the procedure seen in the nexus client.
	 */
	private String nexusPath(String thePrefix, String theContentId, String theVersion, String thePackaging) {
		return new StringBuilder()
			.append(thePrefix.replace(".", "/"))
			.append("/")
			.append(theContentId)
			.append("/")
			.append(theVersion)
			.append("/")
			.append(theContentId)
			.append("-")
			.append(theVersion)
			.append(".")
			.append(thePackaging)
			.toString();
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

	private static class TrackingInputStream extends ProxyInputStream {
		
		private long size = 0;
		private long duration = 0;

		public TrackingInputStream(InputStream theSource) {
			super(theSource);
			duration = System.currentTimeMillis();
		}

		@Override
		protected void beforeRead(int n) {
		}

		@Override
		protected void afterRead(int n) {
			this.size += n;
			if (n == -1)
				this.duration = System.currentTimeMillis() - this.duration;
		}

		public long size() {
			return this.size;
		}

		public long duration() {
			return this.duration;
		}
	}

}

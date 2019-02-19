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
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

import org.acumos.cds.AccessTypeCode;
import org.acumos.federation.gateway.cds.Artifact;
import org.acumos.federation.gateway.cds.ArtifactTypes;
import org.acumos.federation.gateway.cds.Document;
import org.acumos.federation.gateway.config.DockerConfiguration;
import org.acumos.federation.gateway.config.NexusConfiguration;
import org.acumos.federation.gateway.service.ContentService;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.service.ServiceException;
import org.apache.commons.io.input.ProxyInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.util.UriUtils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.PushResponseItem;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.core.command.PushImageResultCallback;

/**
 * Nexus based implementation of the ContentService.
 *
 */
@Service
public class ContentServiceImpl extends AbstractServiceImpl
																	implements ContentService {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private NexusConfiguration nexusConfig;
  @Autowired
  private DockerConfiguration dockerConfig;

	@Override
	public Resource getArtifactContent(
		String theSolutionId, String theRevisionId, Artifact theArtifact, ServiceContext theContext)
																																															throws ServiceException {
		if (theArtifact.getUri() == null) {
			throw new ServiceException("No artifact uri available for " + theArtifact);
		}
		log.info("Retrieving artifact content for {}", theArtifact);

		if (ArtifactTypes.DockerImage.getCode().equals(theArtifact.getArtifactTypeCode())) {
			try {
				//pull followed by save
				DockerClient docker = this.dockerConfig.getDockerClient();

				try (PullImageResultCallback pullResult = new PullImageResultCallback()) {
					docker.pullImageCmd(theArtifact.getUri())
								.exec(pullResult);
					pullResult.awaitCompletion();
					log.debug("Completed docker image pull for {}", theArtifact);
				}

				InputStream imageSource = docker.saveImageCmd(theArtifact.getUri()).exec();
				log.debug("Completed docker image save for {}", theArtifact);

				return new InputStreamResource(imageSource);
			}
			catch (Exception x) {
				log.error("Failed to retrieve artifact content for docker artifact " + theArtifact, x);
				throw new ServiceException("Failed to retrieve artifact content for docker artifact " + theArtifact, x);
			}
		}
		else {	
			return getNexusContent(theArtifact.getUri());
		}
	}

	@Override
	public void putArtifactContent(
		String theSolutionId, String theRevisionId, Artifact theArtifact, Resource theResource, ServiceContext theContext)
																																														throws ServiceException {

		if (ArtifactTypes.DockerImage.getCode().equals(theArtifact.getArtifactTypeCode())) {
			TrackingPushImageResultCallback pushResult = null;
			try {
				TrackingInputStream imageSource = new TrackingInputStream(theResource.getInputStream());
				//load followed by push
				DockerClient docker = this.dockerConfig.getDockerClient();

				docker.loadImageCmd(imageSource)
							.exec(); //sync xecution
				log.debug("Completed docker image load for {}. Transfered {} bytes in {} seconds.",
					theArtifact, imageSource.size(), imageSource.duration()/1000);

				List<Image> images = docker.listImagesCmd().exec();
				log.trace("Available docker images: {}", images);

				//this relies on the presence of the original uri/tag in the description (which is what the controller does), otherwise
				//we'd need to pick this information from the manifest
				Image image = images.stream()
												.filter(i -> i.getRepoTags() != null && Stream.of(i.getRepoTags()).anyMatch(t -> t.equals(theArtifact.getDescription())))
												.findFirst()
												.orElse(null);
				if (image == null) {
					log.debug("Could not find loaded docker image with tag {}", theArtifact.getDescription());
					throw new ServiceException("Could not find loaded docker image for " + theArtifact);
				}
	
				//new image name for re-tagging
				String imageName = dockerConfig.getRegistryUrl() + "/" + theArtifact.getName().toLowerCase() + "_" + theSolutionId;
				String imageTag = imageName;
				log.debug("Attempt re-tagging of docker image {} with {}:{}", image, imageTag, theArtifact.getVersion());
				docker.tagImageCmd(image.getId(), imageTag, theArtifact.getVersion()).exec();
				log.debug("Re-tagged docker image: {} to {}:{}", image, imageTag, theArtifact.getVersion());
				//remove old tag that came with the load
				docker.removeImageCmd(theArtifact.getDescription())
							.withForce(Boolean.TRUE)
							.exec();
			
				log.debug("Attempt docker push for image {}, tag {}", image, imageTag);
				docker.pushImageCmd(imageName)
							.withAuthConfig(dockerConfig.getAuthConfig())
							.withTag(theArtifact.getVersion())
							.exec(pushResult = new TrackingPushImageResultCallback())
							.awaitCompletion();

				PushResponseItem pushResponse = pushResult.getResponseItem();
				if (pushResponse.isErrorIndicated()) {
					log.debug("Failed to push artifact {} image {} to docker registry: {}, {}", theArtifact, image, pushResponse.getError(), pushResponse.getErrorDetail());
					throw new ServiceException("Failed to push image to docker registry: " + pushResponse.getError() + "\n" + pushResponse.getErrorDetail());
				}
				else {
					log.debug("Completed docker push for artifact {} image {}", theArtifact, image);
				}
				
				String imageUri = imageName + ":" + theArtifact.getVersion();
				// update artifact with local repo reference. we also update the name and description in order to stay
				// alligned with on-boarding's unwritten rules
				theArtifact.setSize((int)imageSource.size()); //this is the decompressed size ..
				theArtifact.setUri(imageUri);
				theArtifact.setDescription(imageUri);

				//we should now delete the image from the (local) docker host
			}
			catch (Exception x) {
				log.error("Failed to put docker artifact content", x);
				throw new ServiceException("Failed to put docker artifact content", x);
			}
			finally {
				if (pushResult != null) {
					try { pushResult.close(); } catch (Exception x) {}
				}
			}
		}
		else {
			String[] nameParts = splitName(getName(theArtifact));
			String uri = putNexusContent(
				nexusPrefix(theSolutionId, theRevisionId), nameParts[0], theArtifact.getVersion(), nameParts[1], theResource);
			// update artifact with local repo reference
			theArtifact.setUri(uri);
		}
	}

	@Override
	public Resource getDocumentContent(
		String theSolutionId, String theRevisionId, Document theDocument, ServiceContext theContext)
																																										throws ServiceException {
		if (theDocument.getUri() == null) {
			throw new ServiceException("No document uri available for " + theDocument);
		}
		log.info("Retrieving document content for {}", theDocument);
		return getNexusContent(theDocument.getUri());
	}

	@Override
	public void putDocumentContent(
		String theSolutionId, String theRevisionId, Document theDocument, Resource theResource, ServiceContext theContext)
																																										throws ServiceException {
		String[] nameParts = splitName(getName(theDocument));
		String uri = putNexusContent(
			nexusPrefix(theSolutionId, theRevisionId), nameParts[0], AccessTypeCode.PB.name(), nameParts[1], theResource);
		theDocument.setUri(uri);
	}

	protected Resource getNexusContent(String thePath) throws ServiceException {
		URI contentUri = null;
		try {
			contentUri = new URI(this.nexusConfig.getUrl() + UriUtils.encodePath(thePath, "UTF-8")); 
			log.info("Query for {}", contentUri);
			ResponseEntity<Resource> response = null;
			RequestEntity<Void> request = RequestEntity
																		.get(contentUri)
																		.accept(MediaType.ALL)
																		.build();
			response = this.nexusConfig.getNexusClient().exchange(request, Resource.class);
			return response.getBody();	
		}
		catch (HttpStatusCodeException x) {
			log.error("Failed to retrieve nexus content from  " + thePath + "(" + contentUri + ")", x);
			throw new ServiceException("Failed to retrieve nexus content from " + contentUri, x);
		}
		catch (Throwable t) {
			log.error("Unexpected failure for " + contentUri + "(" + contentUri + ")", t);
			throw new ServiceException("Unexpected failure for " + contentUri, t);
		}

	}

	protected String putNexusContent(
		String theGroupId, String theContentId, String theVersion, String thePackaging, Resource theResource) throws ServiceException {

		try {
			String path = nexusPath(theGroupId, theContentId, theVersion, thePackaging);
			URI contentUri = new URI(this.nexusConfig.getUrl() + UriUtils.encodePath(path, "UTF-8"));
			//URI contentUri = new URI(this.nexusConfig.getUrl() + path);
			log.info("Writing artifact content to nexus at {}", path);
			RequestEntity<Resource> request = RequestEntity
																					.put(contentUri)
																					.contentType(MediaType.APPLICATION_OCTET_STREAM)
																					//.contentLength()
																					.body(theResource);
			ResponseEntity<Void> response = this.nexusConfig.getNexusClient().exchange(request, Void.class);
			log.debug("Writing artifact content to {} resulted in {}", path, response.getStatusCode());
			if (response.getStatusCode().is2xxSuccessful()) {
				log.info("Wrote artifact content to {}", path);
				return path;
			}
			else
				throw new ServiceException("Failed to write artifact content to nexus. Got " + response.getStatusCode());
		}
		catch (Exception x) {
			log.error("Failed to push content to Nexus repo", x);
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
		log.info("Splitting name {}", theName);
		if (null == theName)
			return new String[] {"", ""};
		int pos = theName.lastIndexOf('.');
		return (pos < 0) ?
			new String[] {theName, "" /*null: better coding but does not facilitate callers*/} :
			pos == theName.length() - 1 ? new String[] {theName.substring(0,pos), ""} :
																		new String[] {theName.substring(0,pos), theName.substring(pos+1)};
	}

	private String getName(Artifact theArtifact) {
		String name  = theArtifact.getFilename();
		if (name == null)
			name = theArtifact.getName();
		return name;
	}

	private String getName(Document theDoc) {
		String name  = theDoc.getFilename();
		if (name == null)
			name = theDoc.getName();
		return name;
	}

	/**
	 * Allows for accurate counting of the amount of data transferred. The docker image info resulting from
	 * a 'docker image ls' is slightly different ..
	 */
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

	/**
	 * It only exists because the base does not expose the response item ..
	 */
	private static class TrackingPushImageResultCallback extends PushImageResultCallback {

		private PushResponseItem	responseItem;

		@Override
		public void onNext(PushResponseItem theItem) {
			this.responseItem = theItem;
			super.onNext(theItem);
    }

		public PushResponseItem getResponseItem() {
			return this.responseItem;
		}
	}
}

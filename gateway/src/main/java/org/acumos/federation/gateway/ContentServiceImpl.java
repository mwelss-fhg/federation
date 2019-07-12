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
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.core.command.PushImageResultCallback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.ResourceAccessException;

import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPDocument;

import org.acumos.federation.client.data.Artifact;
import org.acumos.federation.client.data.Document;
import org.acumos.federation.client.FederationClient;

/**
 * Service bean for implementing the ContentService using Nexus and Docker.
 */
public class ContentServiceImpl implements ContentService {
	@Autowired
	private Clients clients;

	@Autowired
	private NexusConfig nexusConfig;

	@Autowired
	private DockerConfig dockerConfig;

	@Override
	public InputStream getArtifactContent(MLPArtifact artifact) {
		if (!FederationClient.ATC_DOCKER.equals(artifact.getArtifactTypeCode())) {
			return clients.getNexusClient().getArtifactContent(artifact);
		}
		DockerClient docker = clients.getDockerClient();
		try (PullImageResultCallback pullResult = new PullImageResultCallback()) {
			docker.pullImageCmd(artifact.getUri()).exec(pullResult);
			try {
				pullResult.awaitCompletion();
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
				throw new IOException("Interrupted while waiting for Docker Pull to complete", ie);
			}
		} catch (IOException ioe) {
			throw new ResourceAccessException("Error fetching docker artifact " + artifact.getUri(), ioe);
		}
		return docker.saveImageCmd(artifact.getUri()).exec();
	}

	@Override
	public InputStream getDocumentContent(MLPDocument document) {
		return clients.getNexusClient().getDocumentContent(document);
	}

	@Override
	public void setArtifactUri(String solutionId, MLPArtifact artifact) {
		if (FederationClient.ATC_DOCKER.equals(artifact.getArtifactTypeCode())) {
			artifact.setUri(dockerConfig.getRegistryUrl() + "/" + artifact.getName().toLowerCase() + "_" + solutionId  + ":" + artifact.getVersion());
			artifact.setDescription(artifact.getUri());
		} else {
			artifact.setUri(makeNexusUri(solutionId, ((Artifact)artifact).getFilename(), artifact.getName(), artifact.getVersion()));
		}
	}

	@Override
	public void putArtifactContent(MLPArtifact artifact, String tag, InputStream is) {
		if (FederationClient.ATC_DOCKER.equals(artifact.getArtifactTypeCode())) {
			DockerClient docker = clients.getDockerClient();
			docker.loadImageCmd(is).exec();
			List<Image> images = docker.listImagesCmd().exec();
			Image image = images.stream().filter(i -> i.getRepoTags() != null && Arrays.asList(i.getRepoTags()).contains(tag)).findAny().orElse(null);
			if (image == null) {
				throw new BadRequestException(400, "Could not find loaded docker image for " + artifact);
			}
			String name = artifact.getDescription().substring(0, artifact.getDescription().lastIndexOf(':'));
			docker.tagImageCmd(image.getId(), name, artifact.getVersion()).exec();
			docker.removeImageCmd(tag).withForce(true).exec();
			try (PushImageResultCallback result = new PushImageResultCallback()) {
				AuthConfig auth = (new AuthConfig())
				    .withUsername(dockerConfig.getRegistryUsername())
				    .withPassword(dockerConfig.getRegistryPassword())
				    .withEmail(dockerConfig.getRegistryEmail())
				    .withRegistryAddress("http://" + dockerConfig.getRegistryUrl() + "/v2/");
				try {
					docker.pushImageCmd(name).withTag(artifact.getVersion()).withAuthConfig(auth).exec(result).awaitCompletion();
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new IOException("Interrupted while waiting for Docker Push to complete", ie);
				}
			} catch (IOException ioe) {
				throw new ResourceAccessException("Error pushing docker artifact " + artifact.getUri(), ioe);
			}
		} else {
			clients.getNexusClient().putArtifactContent(artifact, is);
		}
	}

	@Override
	public void setDocumentUri(String solutionId, MLPDocument document) {
		document.setUri(makeNexusUri(solutionId, ((Document)document).getFilename(), document.getName(), "na"));
	}

	@Override
	public void putDocumentContent(MLPDocument document, InputStream is) {
		clients.getNexusClient().putDocumentContent(document, is);
	}

	private String makeNexusUri(String solutionId, String filename, String name, String version) {
		if (filename == null) {
			filename = name;
		}
		String id = "";
		String fmt = "";
		if (filename != null) {
			id = filename;
			int pos = filename.lastIndexOf('.');
			if (pos != -1) {
				id = filename.substring(0, pos);
				fmt = filename.substring(pos);
			}
		}
		return String.format("%s/%s/%s/%s-%s%s", (nexusConfig.getGroupId() + nexusConfig.getNameSeparator() + solutionId).replace('.', '/'), id, version, id, version, fmt);
	}
}

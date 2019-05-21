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

import org.springframework.beans.factory.annotation.Autowired;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;

import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.client.CommonDataServiceRestClientImpl;

import org.acumos.federation.client.config.ClientConfig;
import org.acumos.federation.client.ClientBase;
import org.acumos.federation.client.FederationClient;

/**
 * Defines all beans used to access outside services.
 *
 * By mocking this bean, all external access can be stubbed out.
 */
public class Clients {
	@Autowired
	private FederationConfig federation;

	@Autowired
	private ServiceConfig cdmsConfig;

	@Autowired
	private NexusConfig nexusConfig;

	@Autowired
	private DockerConfig dockerConfig;

	private ICommonDataServiceRestClient cdsClient;
	private NexusClient nexusClient;
	private DockerClient dockerClient;

	public FederationClient getFederationClient(String url) {
		return new FederationClient(url, federation);
	}

	public synchronized ICommonDataServiceRestClient getCDSClient() {
		if (cdsClient == null) {
			String url = cdmsConfig.getUrl();
			ClientConfig cc = new ClientConfig();
			cc.setCreds(cdmsConfig);
			cdsClient = CommonDataServiceRestClientImpl.getInstance(url, ClientBase.buildRestTemplate(url, cc, null, null));
		}
		return cdsClient;
	}

	public synchronized NexusClient getNexusClient() {
		if (nexusClient == null) {
			ClientConfig cc = new ClientConfig();
			cc.setCreds(nexusConfig);
			nexusClient = new NexusClient(nexusConfig.getUrl(), cc);
		}
		return nexusClient;
	}

	public synchronized DockerClient getDockerClient() {
		if (dockerClient == null) {
			dockerClient = DockerClientBuilder.getInstance(
			    DefaultDockerClientConfig.createDefaultConfigBuilder()
				.withDockerHost(dockerConfig.getHost())
				.withDockerTlsVerify(dockerConfig.getTlsVerify())
				.withDockerConfig(dockerConfig.getDockerConfig())
				.withDockerCertPath(dockerConfig.getDockerCertPath())
				.withApiVersion(dockerConfig.getApiVersion())
				.withRegistryUsername(dockerConfig.getRegistryUsername())
				.withRegistryPassword(dockerConfig.getRegistryPassword())
				.withRegistryEmail(dockerConfig.getRegistryEmail())
				.withRegistryUrl(dockerConfig.getRegistryUrl())
			        .build()
			    ).build();
		}
		return dockerClient;
	}
}

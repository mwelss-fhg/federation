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

import org.acumos.securityverification.service.ISecurityVerificationClientService;
import org.acumos.securityverification.service.SecurityVerificationClientServiceImpl;
import org.acumos.licensemanager.client.rtu.LicenseAsset;

/**
 * Defines all beans used to access outside services.
 *
 * By mocking this bean, all external access can be stubbed out.
 */
public class Clients {
	/*
	 * Implementation note:
	 *
	 * Ideally, all clients would be created at startup, and the getXXX()
	 * methods would just return them, however, while the Spring framework
	 * guarantees that @Autowired fields have been populated, before
	 * invoking @PostConstruct annotated methods and the afterPropertiesSet
	 * method, it doesn't guarantee that properties in those beans have
	 * been set, and the outcome is unrelable.  @Lazy could have been used,
	 * but it would need to be set both here, and in all the @Autowired
	 * uses: missing a single one, in future changes, would produce
	 * mysterious unrelable results.  So, instead, this code
	 * creates clients on first use and, where possible, keeps them for
	 * future use.
	 */

	@Autowired
	private FederationConfig federation;

	@Autowired
	private ServiceConfig cdmsConfig;

	@Autowired
	private NexusConfig nexusConfig;

	@Autowired
	private DockerConfig dockerConfig;

	@Autowired
	private ServiceConfig verificationConfig;

	@Autowired
	private ServiceConfig lmConfig;

	private ICommonDataServiceRestClient cdsClient;
	private NexusClient nexusClient;
	private ISecurityVerificationClientService svClient;
	private LicenseAsset lmClient;

	public FederationClient getFederationClient(String url) {
		/*
		 * The set of peers can change, at runtime, and there is no
		 * notification when one is deleted (or has its API URL
		 * changed).  It would have been possible to keep federation
		 * clients in a hash and fault them in, as needed, but, without
		 * a means for identifying clients that were no longer needed,
		 * that would have constituted a memory (and possibly a TCP/IP
		 * connection) leak.  So this code does not cache federation
		 * clients.
		 */
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
		/*
		 * For some reason, the DockerClient seems to go stale,
		 * resulting in operations (like the docker pull command)
		 * unexpectedly hanging, with no error or indication of a
		 * problem.  Creating a fresh DockerClient on each
		 * upload/download of a Docker image artifact, as a
		 * workaround, seems to work.
		 */
		return DockerClientBuilder.getInstance(
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

	public synchronized ISecurityVerificationClientService getSVClient() {
		if (svClient == null) {
			svClient = new SecurityVerificationClientServiceImpl(
			    verificationConfig.getUrl(),
			    cdmsConfig.getUrl(),
			    cdmsConfig.getUsername(),
			    cdmsConfig.getPassword(),
			    nexusConfig.getUrl().replaceAll("/*$", "") + "/",
			    nexusConfig.getUsername(),
			    nexusConfig.getPassword());
		}
		return svClient;
	}

	public synchronized LicenseAsset getLMClient() {
		if (lmClient == null) {
			lmClient = new LicenseAsset(getCDSClient(), lmConfig.getUrl(), nexusConfig.getUrl().replaceAll("/*$", "") + "/");
		}
		return lmClient;
	}
}

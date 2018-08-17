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

package org.acumos.federation.gateway.common;

import java.lang.invoke.MethodHandles;

import org.acumos.cds.client.CommonDataServiceRestClientImpl;
import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.federation.gateway.cds.Mapper;
import org.acumos.federation.gateway.config.NexusConfiguration;
import org.acumos.federation.gateway.config.DockerConfiguration;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.config.FederationInterfaceConfiguration;
import org.acumos.federation.gateway.config.LocalInterfaceConfiguration;
import org.acumos.nexus.client.NexusArtifactClient;
import org.acumos.nexus.client.RepositoryLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;

/**
 * Unique entry point for building clients: peer access clients, cds clients
 */
@Component("clients")
@Scope("singleton")
public class Clients {

	@Autowired
	private Environment env;
	@Autowired
	private ApplicationContext appCtx = null;
	@Autowired
	private LocalInterfaceConfiguration localConfig = null;
	@Autowired
	private FederationInterfaceConfiguration federationConfig = null;
	@Autowired
	private DockerConfiguration dockerConfig = null;
	@Autowired
	private NexusConfiguration nexusConfig = null;

	private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(MethodHandles.lookup().lookupClass());

	public Clients() {
	}

	/**
	 * @return The standard CDS client
	 */
	public ICommonDataServiceRestClient getCDSClient() {

		MappingJackson2HttpMessageConverter cdsMessageConverter = new MappingJackson2HttpMessageConverter();
    cdsMessageConverter.setObjectMapper(Mapper.build()); //try to avoid building one every time

		RestTemplateBuilder builder =
			new RestTemplateBuilder()
				.requestFactory(new HttpComponentsClientHttpRequestFactory( 
													localConfig.buildClient()))
				//.rootUri(env.getProperty("cdms.client.url"))
				.basicAuthorization(env.getProperty("cdms.client.username"),
														env.getProperty("cdms.client.password"))
				.messageConverters(cdsMessageConverter)
				;

			return new CommonDataServiceRestClientImpl(
				env.getProperty("cdms.client.url"), builder.build());
	}

	/**
	 * Build a client for the given peer uri
	 */
	public FederationClient getFederationClient(String thePeerURI) {
		return new FederationClient(thePeerURI, federationConfig.buildClient(), Mapper.build());
	}

	/** */
	public NexusArtifactClient getNexusClient() {
		return new NexusArtifactClient(nexusConfig.getRepositoryLocation());
	}

	/** */
	public DockerClient	getDockerClient() {
    return DockerClientBuilder.getInstance(dockerConfig.buildConfig())
        		.withDockerCmdExecFactory(DockerClientBuilder.getDefaultDockerCmdExecFactory())
        		.build();
	}

}

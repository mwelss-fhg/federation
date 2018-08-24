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

import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.federation.gateway.cds.Mapper;
import org.acumos.federation.gateway.config.NexusConfiguration;
import org.acumos.federation.gateway.config.DockerConfiguration;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.config.FederationInterfaceConfiguration;
import org.acumos.federation.gateway.config.LocalInterfaceConfiguration;
import org.acumos.federation.gateway.config.CDMSClientConfiguration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import org.acumos.nexus.client.NexusArtifactClient;

import com.github.dockerjava.api.DockerClient;

/**
 * Unique entry point for building clients: peer access clients, cds clients
 */
@Component("clients")
@Scope("singleton")
public class Clients {

	@Autowired
	private LocalInterfaceConfiguration localConfig = null;
	@Autowired
	private FederationInterfaceConfiguration federationConfig = null;
	@Autowired
	private DockerConfiguration dockerConfig = null;
	@Autowired
	private NexusConfiguration nexusConfig = null;
	@Autowired
	private CDMSClientConfiguration cdmsConfig = null;

	private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(MethodHandles.lookup().lookupClass());

	public Clients() {
	}

	/**
	 * @return The standard CDS client
	 */
	public ICommonDataServiceRestClient getCDSClient() {
		return cdmsConfig.getCDSClient();
	}

	/**
	 * Build a client for the given peer uri
	 */
	public FederationClient getFederationClient(String thePeerURI) {
		return new FederationClient(thePeerURI, federationConfig.buildClient(), Mapper.build());
	}

	/** */
	public NexusArtifactClient getNexusClient() {
		return nexusConfig.getNexusClient();
	}

	/** */
	public DockerClient	getDockerClient() {
    return dockerConfig.getDockerClient();
	}

}

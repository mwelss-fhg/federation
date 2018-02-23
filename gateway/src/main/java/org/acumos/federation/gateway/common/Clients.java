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

import org.apache.http.client.HttpClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;

import org.acumos.nexus.client.NexusArtifactClient;
import org.acumos.nexus.client.RepositoryLocation;

import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.config.InterfaceConfiguration;
import org.acumos.federation.gateway.config.LocalInterfaceConfiguration;
import org.acumos.federation.gateway.config.FederationInterfaceConfiguration;

import org.acumos.cds.client.CommonDataServiceRestClientImpl;
import org.acumos.cds.client.ICommonDataServiceRestClient;

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

	private final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(getClass().getName());

	public Clients() {
		log.trace(EELFLoggerDelegate.debugLogger, "Clients::new");
	}
	
	/**
	 * @return The standard CDS client
	 */
	public ICommonDataServiceRestClient getCDSClient() {

		RestTemplateBuilder builder =
			new RestTemplateBuilder()
				.requestFactory(new HttpComponentsClientHttpRequestFactory( 
													/*(HttpClient)this.appCtx.getBean("localClient")*/
													localConfig.buildClient()))
				//.rootUri(env.getProperty("cdms.client.url"))
				.basicAuthorization(env.getProperty("cdms.client.username"),
														env.getProperty("cdms.client.password"));

			return new CommonDataServiceRestClientImpl(
				env.getProperty("cdms.client.url"), builder.build());
		//return new CommonDataServiceRestClientImpl(
		//		env.getProperty("cdms.client.url"),
		//		env.getProperty("cdms.client.username"),
		//		env.getProperty("cdms.client.password"));
	}

	/**
	 * Build a client for the given peer uri
	 */
	public FederationClient getFederationClient(String thePeerURI) {
		return new FederationClient(thePeerURI, /*(HttpClient)this.appCtx.getBean("federationClient")*/federationConfig.buildClient());
	}

	/** */
	public NexusArtifactClient getNexusClient() {
		RepositoryLocation repositoryLocation = new RepositoryLocation();

		repositoryLocation.setId("1");

		repositoryLocation.setUrl(env.getProperty("nexus.url"));
		repositoryLocation.setUsername(env.getProperty("nexus.username"));
		repositoryLocation.setPassword(env.getProperty("nexus.password"));
		repositoryLocation.setProxy(env.getProperty("nexus.proxy"));
		return new NexusArtifactClient(repositoryLocation);
	}
}

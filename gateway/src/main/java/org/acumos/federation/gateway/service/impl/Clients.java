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

package org.acumos.federation.gateway.service.impl;

import org.apache.http.client.HttpClient;

import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import org.acumos.federation.gateway.config.EELFLoggerDelegate;

import org.acumos.cds.client.CommonDataServiceRestClientImpl;
import org.acumos.cds.client.ICommonDataServiceRestClient;

import org.acumos.nexus.client.NexusArtifactClient;
import org.acumos.nexus.client.RepositoryLocation;


/**
 * Unique entry point for building clients: peer access clients, cds clients
 */
@Component("clients")
@Scope("singleton")
public class Clients implements ApplicationContextAware {

	@Autowired
	protected Environment env;

	protected ApplicationContext appCtx = null;

	public void	setApplicationContext(ApplicationContext theAppContext) {
		this.appCtx = theAppContext;
	}

	/** The standard CDS client */	
	public ICommonDataServiceRestClient getClient() {
		return new CommonDataServiceRestClientImpl(
								env.getProperty("cdms.client.url"),
								env.getProperty("cdms.client.username"),
								env.getProperty("cdms.client.password"));
	}
	
	/** Federation oriented CDS layer */	
	public FederationDataClient getCommonDataClient() {
    return new FederationDataClient(
									this.env.getProperty("cdms.client.url"),
                  (HttpClient)this.appCtx.getBean("federationDataClient"));
	}

	public FederationClient getFederationClient(String thePeerURI) {
    return new FederationClient(
									thePeerURI,
                  (HttpClient)this.appCtx.getBean("federationClient"));
	}

	public NexusArtifactClient getNexusClient() {

		RepositoryLocation repositoryLocation = new RepositoryLocation();
		repositoryLocation.setId("1");
		repositoryLocation.setUrl(this.env.getProperty("nexus.url"));
		repositoryLocation.setUsername(this.env.getProperty("nexus.username"));
		repositoryLocation.setPassword(this.env.getProperty("nexus.password"));
		repositoryLocation.setProxy(this.env.getProperty("nexus.proxy"));
			
		// if you need a proxy to access the Nexus
		return new NexusArtifactClient(repositoryLocation);
	}
}

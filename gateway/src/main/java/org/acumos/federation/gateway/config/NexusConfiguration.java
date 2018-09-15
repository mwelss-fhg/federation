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

package org.acumos.federation.gateway.config;

import java.lang.invoke.MethodHandles;

import org.acumos.nexus.client.NexusArtifactClient;
import org.acumos.nexus.client.RepositoryLocation;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 
 */
@Component
@ConfigurationProperties(prefix = "nexus")
public class NexusConfiguration {

	private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(MethodHandles.lookup().lookupClass());
	private	RepositoryLocation repositoryLocation;
	private String						 groupId;
	private String						 nameSeparator;

	public NexusConfiguration() {
		reset();
	}

	private void reset() {
		this.repositoryLocation = new RepositoryLocation();
		//defaults
		this.repositoryLocation.setId("1");
		this.groupId = null;
		this.nameSeparator = ".";
	}

	public void setId(String theId) {
		this.repositoryLocation.setId(theId);
	}

	public void setUrl(String theUrl) {
		this.repositoryLocation.setUrl(theUrl);
  }

	public void setUsername(String theUsername) {
		this.repositoryLocation.setUsername(theUsername);
	}

	public void setPassword(String thePassword) {
		this.repositoryLocation.setPassword(thePassword);
	}

	public void setProxy(String theProxy) {
		this.repositoryLocation.setProxy(theProxy);
	}

	public RepositoryLocation getRepositoryLocation() {
		return this.repositoryLocation;
	}

	public void setGroupId(String theGroupId) {
		this.groupId = theGroupId;
	}

	public String getGroupId() {
		return this.groupId;
	}

	public void setNameSperator(String theNameSeparator) {
		this.nameSeparator = theNameSeparator;
	}

	public String getNameSeparator() {
		return this.nameSeparator;
	}

	/** */
	public NexusArtifactClient getNexusClient() {
		return new NexusArtifactClient(getRepositoryLocation());
	}
}

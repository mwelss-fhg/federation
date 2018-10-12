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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 
 */
@Component
@ConfigurationProperties(prefix = "nexus")
public class NexusConfiguration {

	private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(MethodHandles.lookup().lookupClass());

	private String		proxy;
	private String	  groupId;
	private String	  id;
	private String		url;
	private String		username;
	private String		password;
	private String		nameSeparator;
	@Autowired
	private LocalInterfaceConfiguration localIfConfig = null;

	public NexusConfiguration() {
		reset();
	}

	private void reset() {
		//defaults
		this.id = "1";
		this.groupId = null;
		this.nameSeparator = ".";
	}

	public void setId(String theId) {
		this.id = theId;
	}

	public void setUrl(String theUrl) {
		this.url = theUrl;
  }
	
	public String getUrl() {
		return this.url;
  }

	public void setUsername(String theUsername) {
		this.username = theUsername;
	}

	public void setPassword(String thePassword) {
		this.password = thePassword;
	}

	@Deprecated
	public void setProxy(String theProxy) {
		this.proxy = theProxy;
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

	public RestTemplate getNexusClient() {

		//cannot use the localIfConfig straightup as it does not carry the Nexus specific client authentication info
		//but this only need to be built once
		InterfaceConfiguration nexusIfConfig = InterfaceConfigurationBuilder.buildFrom(this.localIfConfig)
																							.withClient(new InterfaceConfiguration.Client(this.username, this.password))
																							.buildConfig();

		
		log.info(EELFLoggerDelegate.debugLogger, "Nexus config: {}", nexusIfConfig);

		RestTemplateBuilder builder =
			new RestTemplateBuilder()
				.requestFactory(new HttpComponentsClientHttpRequestFactory( 
													nexusIfConfig.buildClient()));
		if (this.url != null) {
			builder.rootUri(this.url);
		}
		if (this.username != null) {
			builder.basicAuthorization(this.username, this.password);
		}

		return builder.build();
	}
}

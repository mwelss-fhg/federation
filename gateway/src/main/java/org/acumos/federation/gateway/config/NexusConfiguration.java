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

import java.net.URI;
import java.net.URL;
import java.net.MalformedURLException;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.http.HttpHost;
import org.apache.http.client.AuthCache;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.BasicHttpContext;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


/**
 * 
 */
@Component
@ConfigurationProperties(prefix = "nexus")
public class NexusConfiguration {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private String		proxy;
	private String	  groupId;
	private String	  id;
	private URL				url;
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

	public void setUrl(String theSpec) throws MalformedURLException {
		this.url = new URL(theSpec);
  }
	
	public String getUrl() {
		return this.url.toString();
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

	/**
	 * Prepare a RestTemplate fitted for Nexus interactions, in particular ready to perform preemptive basic authentication.
	 * @return RestTemplate
	 */
	public RestTemplate getNexusClient() {

		RestTemplateBuilder builder =
			new RestTemplateBuilder()
				.requestFactory(
					() -> new HttpComponentsClientHttpRequestFactory(this.localIfConfig.buildClient()) {

						protected HttpContext createHttpContext(HttpMethod theMethod, URI theUri) {
							HttpHost nexusHost = new HttpHost(NexusConfiguration.this.url.getHost(), NexusConfiguration.this.url.getPort());

							CredentialsProvider nexusCreds = new BasicCredentialsProvider();
							nexusCreds.setCredentials(
       					new AuthScope(nexusHost.getHostName(), nexusHost.getPort()),
        				new UsernamePasswordCredentials(NexusConfiguration.this.username, NexusConfiguration.this.password));

							AuthCache authCache = new BasicAuthCache();
 							BasicScheme basicAuth = new BasicScheme();
							authCache.put(nexusHost, basicAuth);
 
							HttpClientContext nexusContext = HttpClientContext.create();
							nexusContext.setAuthCache(authCache);
							nexusContext.setCredentialsProvider(nexusCreds);
 							return nexusContext;
						}
					});

		return builder.build();
	}

	

}

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

import org.acumos.cds.client.CommonDataServiceRestClientImpl;
import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.federation.gateway.cds.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;

/**
 * Gathers all CDS connectivity related parameters. Allows one to construct a cds client based on the
 * given parameters routing its requests through the local interface of the gateway.
 */
@Component
@ConfigurationProperties(prefix = "cdms.client")
public class CDMSClientConfiguration {

	public static final int	DEFAULT_PAGE_SIZE = 100;

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private String		url;
	private String		username;
	private String		password;
	private int				pageSize;

	@Autowired
	private LocalInterfaceConfiguration localIfConfig = null;

	public CDMSClientConfiguration() {
		reset();
	}

	private void reset() {
		//defaults
		this.url = null;
		this.username = null;
		this.password = null;
		this.pageSize = DEFAULT_PAGE_SIZE;
	}

	public String getUrl() {
		return this.url ;
	}

	public void setUrl(String theUrl) {
		this.url = theUrl;
  }

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String theUsername) {
		this.username = theUsername;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String thePassword) {
		this.password = thePassword;
	}

	public int getPageSize() {
		return this.pageSize;
	}

	public void setPageSize(int thePageSize) {
		this.pageSize = thePageSize;
  }

	/**
	 * CDS traffic is always routed through the local interface;
	 * @return Client
	 */
	public ICommonDataServiceRestClient getCDSClient() {

		MappingJackson2HttpMessageConverter cdsMessageConverter = new MappingJackson2HttpMessageConverter();
    cdsMessageConverter.setObjectMapper(Mapper.build()); //try to avoid building one every time

		RestTemplateBuilder builder =
			new RestTemplateBuilder()
				.requestFactory(
					() -> new HttpComponentsClientHttpRequestFactory(this.localIfConfig.buildClient()))
				//.rootUri(env.getProperty("cdms.client.url"))
					.basicAuthorization(this.username, this.password)
					.messageConverters(cdsMessageConverter);

		return new CommonDataServiceRestClientImpl(this.url, builder.build());
	}

}

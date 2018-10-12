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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;

import org.apache.http.client.HttpClient;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Support class for building clients of other components of the Acumos universe that expose an http based
 * service interface.
 */
public abstract class AbstractClient {

	protected String baseUrl;
	protected RestTemplate restTemplate;

	/**
	 * Builds a restTemplate. If user and pass are both supplied, uses basic HTTP
	 * authentication; if either one is missing, no authentication is used.
	 * 
	 * @param theTarget
	 *            URL of the web endpoint
	 * @param theClient
	 *            underlying http client
	 */
	public AbstractClient(String theTarget, HttpClient theClient) {
		setTarget(theTarget);
		
		this.restTemplate = new RestTemplateBuilder()
													.requestFactory(new HttpComponentsClientHttpRequestFactory(theClient))
													.rootUri(this.baseUrl)
													.build();
	}
	
	public AbstractClient(String theTarget, HttpClient theClient, ObjectMapper theMapper) {
		setTarget(theTarget);
		
		MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
    messageConverter.setObjectMapper(theMapper); //try to avoid building one every time

		ResourceHttpMessageConverter contentConverter = new ResourceHttpMessageConverter();
		contentConverter.setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM));

		this.restTemplate = new RestTemplateBuilder()
													.requestFactory(new HttpComponentsClientHttpRequestFactory(theClient))
													.messageConverters(messageConverter, contentConverter)
													.rootUri(this.baseUrl)
													.build();
	
	}

	protected void setTarget(String theTarget) {
		if (theTarget == null)
			throw new IllegalArgumentException("Null URL not permitted");

		URL url = null;
		try {
			url = new URL(theTarget);
			this.baseUrl = url.toExternalForm();
		}
		catch (MalformedURLException ex) {
			throw new IllegalArgumentException("Failed to parse target URL", ex);
		}
	}	

}

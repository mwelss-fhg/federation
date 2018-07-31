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
package org.acumos.federation.gateway.test;

import java.io.InputStream;
import java.io.Closeable;
import java.io.IOException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Collections;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import static org.junit.Assert.assertTrue;
import static org.assertj.core.api.Assertions.assertThat; 

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Bean;

import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;

import static org.mockito.Mockito.*;
import static org.mockito.Matchers.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.mockito.invocation.InvocationOnMock;

import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HttpContext;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse; 
import org.apache.http.message.BasicStatusLine;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;

/* this is not good for unit testing .. */
import org.acumos.federation.gateway.config.LocalInterfaceConfiguration;


/**
 * Infrastrcture code for mocking underlying REST calls, such in the case of CDS api calls.
 */
public abstract class ServiceTest {

	//needed because service calls to other components are made over the local interface
	@MockBean
	private LocalInterfaceConfiguration	localConfig;

	@MockBean
	private CloseableHttpClient	localClient;

	@Autowired
	private ApplicationContext context;
	
	private	Map<String, MockResponse> mocks = new HashMap<String, MockResponse>();

	/**
	 * Derived classes should use this to register mock responses.
	 */
	protected abstract void initMockResponses();

	/**
	 * Use to register a mocked http request/response.
	 */
	protected void registerMockResponse(String thePath, MockResponse theResponse) {
		this.mocks.put(thePath, theResponse);
	}


	@Before
	public void initTest() throws IOException {
		MockitoAnnotations.initMocks(this);
		initMockResponses();

		when(
			this.localClient.execute(
				any(HttpUriRequest.class), any(HttpContext.class)
			)
		).thenAnswer(new Answer<HttpResponse>() {
				public HttpResponse answer(InvocationOnMock theInvocation) throws Throwable {
					HttpUriRequest req = (HttpUriRequest)
						theInvocation.getArguments()[0];
					String key = req.getMethod() + " " + req.getURI().getPath() + (req.getURI().getQuery() == null ? "" : ("?" + req.getURI().getQuery()));

					MockResponse mockResponse = mocks.get(key);
					if (mockResponse == null) {
						throw new IOException("Mock unhandled " + key);
					}

					BasicCloseableHttpResponse httpResponse = 
						new BasicCloseableHttpResponse(
							new BasicStatusLine(
								new ProtocolVersion("HTTP",1,1), mockResponse.getResponseCode(), mockResponse.getResponseMsg()));

					ClassPathResource resource = new ClassPathResource(mockResponse.getResourceName());

					try {
						httpResponse.setEntity(new InputStreamEntity(resource.getInputStream()));
						httpResponse.addHeader("Content-Type", ContentType.APPLICATION_JSON.toString());
						httpResponse.addHeader("Content-Length", String.valueOf(resource.contentLength()));
					}
					catch (IOException iox) {
						throw new IOException("Failed to load mock resource " + resource);
					}

					return httpResponse;
				}
			});

		when(
			this.localConfig.buildClient()
			)
		.thenAnswer(new Answer<HttpClient>() {
				public HttpClient answer(InvocationOnMock theInvocation) {
					return localClient;	
				}
			});
	}

	/**
	 * Apache's http framework expects a CloseableHttpResponse at some point and this is cheaper than mocking every time.
	 */
	private static class BasicCloseableHttpResponse extends BasicHttpResponse implements CloseableHttpResponse {

		public BasicCloseableHttpResponse(StatusLine theStatus) {
			super(theStatus);
		}

		@Override
		public void close() throws IOException {
		}
	}

}

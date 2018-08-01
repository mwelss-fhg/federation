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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.acumos.federation.gateway.config.LocalInterfaceConfiguration;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;


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
						throw new IOException("Failed to load mock resource " + resource, iox);
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

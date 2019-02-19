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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.acumos.federation.gateway.config.LocalInterfaceConfiguration;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;


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

	private MockAnswer answer = new MockAnswer();	

	/**
	 * Derived classes should use this to register mock responses.
	 */
	protected abstract void initMockResponses();

	/**
	 * Use to register a mocked http request/response.
	 */
	protected void registerMockResponse(String theLine, MockResponse theResponse) {
		this.answer.mockResponse(info -> info.getLine().equals(theLine), theResponse);
	}


	@Before
	public void initTest() throws IOException {
		MockitoAnnotations.initMocks(this);
		initMockResponses();

		when(
			this.localClient.execute(
				any(HttpUriRequest.class), any(HttpContext.class)
			)
		).thenAnswer(this.answer);

		when(
			this.localConfig.buildClient()
			)
		.thenAnswer(new Answer<HttpClient>() {
				public HttpClient answer(InvocationOnMock theInvocation) {
					return localClient;	
				}
			});
	}

}

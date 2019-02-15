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
import java.net.URLConnection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.acumos.federation.gateway.config.LocalInterfaceConfiguration;
import org.apache.commons.io.FilenameUtils;
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
import org.mockito.stubbing.Answer;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Infrastrcture code for mocking http traffic..
 */
public class MockAnswer implements Answer<HttpResponse> {

	private	Map<Predicate<RequestInfo>, MockResponse> mocks = new LinkedHashMap<Predicate<RequestInfo>, MockResponse>();


	/**
	 * Use to register a mocked http request/response.
	 * @param theTest Test
	 * @param theResponse Response
	 * @return Answer
	 */
	public MockAnswer mockResponse(Predicate<RequestInfo> theTest, MockResponse theResponse) {
		this.mocks.put(theTest, theResponse);
		return this;
	}

	@Override
	public HttpResponse answer(InvocationOnMock theInvocation) throws Throwable {

		RequestInfo reqInfo = new RequestInfo((HttpUriRequest)theInvocation.getArguments()[0]);

		//this goes over all the entrieas while we really only care about the first one ..
		MockResponse mockResponse = mocks.entrySet().stream().filter(e -> e.getKey().test(reqInfo)).map(e -> e.getValue()).findFirst().orElse(null);
		if (mockResponse == null) {	
			throw new IOException("Mock unhandled " + reqInfo.getLine());
		}
		else {
			System.out.println("Provided " + mockResponse + " to " + reqInfo.getLine());
			mockResponse.consume();
		}

		BasicCloseableHttpResponse httpResponse = 
			new BasicCloseableHttpResponse(
				new BasicStatusLine(
					new ProtocolVersion("HTTP",1,1), mockResponse.getResponseCode(), mockResponse.getResponseMsg()));

		ClassPathResource resource = new ClassPathResource(mockResponse.getResourceName());

		String contentType = null;
		String ext = FilenameUtils.getExtension(resource.getFilename());

		if ("json".equals(ext))
			contentType = ContentType.APPLICATION_JSON.toString();
		else if ("tgz".equals(ext))
			contentType = ContentType.DEFAULT_BINARY.toString();
		else
			contentType = URLConnection.getFileNameMap().getContentTypeFor(resource.getFilename());
	
		try {
			httpResponse.setEntity(new InputStreamEntity(resource.getInputStream()));
			httpResponse.addHeader("Content-Type", contentType);
			httpResponse.addHeader("Content-Length", String.valueOf(resource.contentLength()));
		}
		catch (IOException iox) {
			throw new IOException("Failed to load mock resource " + resource, iox);
		}

		return httpResponse;
	}

	/* the function below causes the compiler to crash .. */
/*
	protected String guessContentType(Resource theResource) {
		String ext = FilenameUtils.getExtension(theResource.getFilename());
		if ("json".equals(ext))
			return ContentType.APPLICATION_JSON.toString();
		if ("tgz".equals(ext))
			return ContentType.DEFAULT_BINARY.toString();
		return URLConnection.getFileNameMap().getContentTypeFor(theResource.getFilename());
	}
*/

	public static class RequestInfo {

		protected HttpUriRequest req;
		private String line;
		private MultiValueMap<String,String>	queryParams;

		protected RequestInfo(HttpUriRequest theRequest) {
			this.req = theRequest;
			this.line = this.req.getMethod() + " " + this.req.getURI().getPath() + (this.req.getURI().getQuery() == null ? "" : ("?" + this.req.getURI().getQuery()));
		}

		public String getMethod() {
			return this.req.getMethod().toString();
		}

		public String getLine() {
			return this.line;
		}

		public String getPath() {
			return this.req.getURI().getPath();
		}

		public String getQueryParam(String theParam) {
			if (this.queryParams == null)
				this.queryParams = UriComponentsBuilder.fromUri(req.getURI()).build().getQueryParams();
			return this.queryParams.getFirst(theParam);
		}
		//other pieces can be exposed as needed by matching criteria
	}


	/**
	 * Apache's http framework expects a CloseableHttpResponse at some point and this is cheaper than mocking every time.
	 */
	public static class BasicCloseableHttpResponse extends BasicHttpResponse implements CloseableHttpResponse {

		public BasicCloseableHttpResponse(StatusLine theStatus) {
			super(theStatus);
		}

		@Override
		public void close() throws IOException {
		}
	}

}

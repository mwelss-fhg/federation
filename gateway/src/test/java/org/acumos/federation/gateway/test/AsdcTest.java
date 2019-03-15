/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2017-2019 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
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

import java.lang.invoke.MethodHandles;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.UUID;

import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.json.JSONArray;
import org.json.JSONObject;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import org.acumos.federation.gateway.adapter.onap.sdc.ASDC;
import org.acumos.federation.gateway.adapter.onap.sdc.ASDCException;

public class AsdcTest {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static class RequestAsyncWrapper implements AsyncClientHttpRequest, ClientHttpRequest {
		private ClientHttpRequest wrapped;

		public RequestAsyncWrapper(ClientHttpRequest request) {
			wrapped = request;
		}

		public ListenableFuture<ClientHttpResponse> executeAsync() throws IOException {
			SettableListenableFuture<ClientHttpResponse> future = new SettableListenableFuture<>();
			try {
				future.set(wrapped.execute());
			} catch (Exception e) {
				future.setException(e);
			}
			return future;
		}

		public ClientHttpResponse execute() throws IOException {
			ClientHttpResponse ret = wrapped.execute();
			return ret;
		}

		public String getMethodValue() {
			return wrapped.getMethodValue();
		}

		public URI getURI() {
			return wrapped.getURI();
		}

		public OutputStream getBody() throws IOException {
			return wrapped.getBody();
		}

		public HttpHeaders getHeaders() {
			return wrapped.getHeaders();
		}
	}

	private static class RequestFactoryAsyncWrapper implements ClientHttpRequestFactory, AsyncClientHttpRequestFactory {
		private ClientHttpRequestFactory wrapped;

		public RequestFactoryAsyncWrapper(ClientHttpRequestFactory factory) {
			wrapped = factory;
		}

		public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) throws IOException {
			return new RequestAsyncWrapper(wrapped.createRequest(uri, httpMethod));
		}

		public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
			return wrapped.createRequest(uri, httpMethod);
		}
	}

	private MockAnswer asdcAnswer = new MockAnswer();

	@Mock
	private CloseableHttpClient asdcClient;

	private UUID someuuid = UUID.randomUUID();

	private AsyncRestTemplate mockTemplate() {
		AsyncRestTemplate ret = new AsyncRestTemplate();
		ret.setAsyncRequestFactory(new RequestFactoryAsyncWrapper(new HttpComponentsClientHttpRequestFactory(asdcClient)));
		return ret;
	}

	@Before
	public void initTest() throws IOException {
		MockitoAnnotations.initMocks(this);

		// todo set up answers
		asdcAnswer
			.mockResponse(info -> info.getPath().startsWith("/arraycontext/"), MockResponse.success("onap/sdc/EmptyArrayResponse.json"))
			.mockResponse(info -> info.getPath().startsWith("/objectcontext/"), MockResponse.success("onap/sdc/EmptyObjectResponse.json"))
			.mockResponse(info -> info.getPath().startsWith("/bytescontext/"), MockResponse.success("onap/sdc/BytesResponse.tgz"))
			;

		when(
		    asdcClient.execute(any(HttpUriRequest.class), any(HttpContext.class))
		).thenAnswer(asdcAnswer);
	}

	@Test
	public void testASDC() throws Exception {
		for (ASDC.AssetType e : ASDC.AssetType.values())
			log.info("assest type {}, code {}", e);
		for (ASDC.ArtifactType e : ASDC.ArtifactType.values())
			log.info("artifact type {}, code {}", e);
		for (ASDC.ArtifactGroupType e : ASDC.ArtifactGroupType.values())
			log.info("artifact group type {}, code {}", e);
		for (ASDC.LifecycleState e : ASDC.LifecycleState.values())
			log.info("lifecycle state {}, code {}", e);
		ASDC asdc = new ASDC();
		asdc.checkForUpdates();
		asdc.initASDC();
		asdc.getARTFactory();
		asdc.setARTFactory(() -> mockTemplate());
		URI uriNoFrag = new URI("http://user:pass@host:443/context/path");
		try {
			asdc.setUri(uriNoFrag);
			Assert.assertTrue(false);
		} catch (Exception ex) {
			log.info("Caught exception as expected");
		}
		URI uri = new URI("http://user:pass@host:443/arraycontext/path#fragment");
		asdc.setUri(uri);
		Assert.assertNotNull(asdc.getUri());
		Assert.assertEquals("user", asdc.getUser());
		Assert.assertEquals("pass", asdc.getPassword());
		Assert.assertEquals("fragment", asdc.getInstanceId());
		asdc.setInstanceId("somethingelse");
		Assert.assertEquals("somethingelse", asdc.getInstanceId());
		Assert.assertEquals("/asdc/", asdc.getRootPath());
		asdc.setRootPath("/cdsa/");
		Assert.assertEquals("/cdsa/", asdc.getRootPath());
		asdc.setRootPath("/asdc/");
		try {
			// lists
			asdc.getResources().waitForResult();
			asdc.getResources(JSONArray.class).waitForResult();
			asdc.getResources("category", "subcategory").waitForResult();
			asdc.getResources(JSONArray.class, "category", "subcategory").waitForResult();
			asdc.getServices().waitForResult();
			asdc.getServices(JSONArray.class).waitForResult();
			asdc.getServices("category", "subcategory").waitForResult();
			asdc.getServices(JSONArray.class, "category", "subcategory").waitForResult();
			asdc.getAssetsAction(ASDC.AssetType.service, JSONArray.class).execute().waitForResult();
			asdc.getAssetsAction(ASDC.AssetType.service, JSONArray.class, "category", "subcategory", "resourcetype").execute().waitForResult();
			// objects
			asdc.setUri(new URI("http://user:pass@host:443/objectcontext/path#fragment"));
			asdc.getResource(someuuid).waitForResult();
			asdc.getResource(someuuid, JSONObject.class).waitForResult();
			asdc.getService(someuuid).waitForResult();
			asdc.getService(someuuid, JSONObject.class).waitForResult();
			asdc.getAssetAction(ASDC.AssetType.service, someuuid, JSONObject.class).execute().waitForResult();
			asdc.checkoutResource(someuuid, "someuser", "somemessage").waitForResult();
			asdc.checkoutService(someuuid, "someuser", "somemessage").waitForResult();
			asdc.checkinResource(someuuid, "someuser", "somemessage").waitForResult();
			asdc.checkinService(someuuid, "someuser", "somemessage").waitForResult();
			asdc.certifyResource(someuuid, "someuser", "somemessage").waitForResult();
			asdc.certifyService(someuuid, "someuser", "somemessage").waitForResult();
			asdc.createResourceArtifact(someuuid)
				.withEncodedContent("AAAA")
				.withContent("xx".getBytes())
				.withLabel("abc")
				.withName("def")
				.withDisplayName("Displaydef")
				.withType(ASDC.ArtifactType.DCAE_JSON)
				.withGroupType(ASDC.ArtifactGroupType.INFORMATIONAL)
				.withDescription("xyz")
				.withOperator("them")
				.execute().waitForResult();
			asdc.createServiceArtifact(someuuid);
			asdc.createResourceInstanceArtifact(someuuid, "instance");
			asdc.createServiceInstanceArtifact(someuuid, "instance");
			JSONObject xobj = new JSONObject();
			xobj.put("artifactUUID", someuuid.toString());
			xobj.put("artifactType", "DCAE_JSON");
			xobj.put("artifactGroupType", "INFORMATIONAL");
			xobj.put("artifactLabel", "abc");
			asdc.updateResourceArtifact(someuuid, xobj)
				.withEncodedContent("AAAA")
				.withContent("xx".getBytes())
				.withDescription("xyz")
				.withName("def")
				.withOperator("them")
				.execute().waitForResult();
			asdc.updateServiceArtifact(someuuid, new JSONObject());
			asdc.updateResourceInstanceArtifact(someuuid, "instance", new JSONObject());
			asdc.updateServiceInstanceArtifact(someuuid, "instance", new JSONObject());
			asdc.deleteResourceArtifact(someuuid, someuuid)
				.withOperator("them")
				.execute().waitForResult();
			asdc.deleteResourceInstanceArtifact(someuuid, "instance", someuuid);
			asdc.deleteServiceArtifact(someuuid, someuuid);
			asdc.deleteServiceInstanceArtifact(someuuid, "instance", someuuid);
			asdc.createVFCMT()
				.withName("abc")
				.withDescription("xyz")
				.withVendorName("AAA")
				.withVendorRelease("1.0")
				.withTags("tag1", "tag2")
				.withIcon("iconic")
				.withOperator("them")
				.execute().waitForResult();
			asdc.createVF()
				.withCategory("cat5")
				.withSubCategory("e")
				.withName("abc")
				.withDescription("xyz")
				.withVendorName("AAA")
				.withVendorRelease("1.0")
				.withTags("tag1")
				.withIcon("ball")
				.withOperator("them")
				.execute().waitForResult();
			// raw bytes
			asdc.setUri(new URI("http://user:pass@host:443/bytescontext/path#fragment"));
			asdc.getResourceArchive(someuuid).waitForResult();
			asdc.getServiceArchive(someuuid).waitForResult();
			asdc.getAssetArchiveAction(ASDC.AssetType.resource, someuuid).execute().waitForResult();
			asdc.getResourceArtifact(someuuid, someuuid, byte[].class).waitForResult();
			asdc.getServiceArtifact(someuuid, someuuid, byte[].class).waitForResult();
			asdc.getResourceInstanceArtifact(someuuid, someuuid, "instance", byte[].class).waitForResult();
			asdc.getServiceInstanceArtifact(someuuid, someuuid, "instance", byte[].class).waitForResult();
			asdc.getAssetArtifactAction(ASDC.AssetType.resource, someuuid, someuuid, byte[].class).execute().waitForResult();
			asdc.getAssetInstanceArtifactAction(ASDC.AssetType.resource, someuuid, "instance", someuuid, byte[].class).execute().waitForResult();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	@Test
	public void testASDCException() throws Exception {
		String responseBody = "{\"requestError\": {\"policyException\":{\"text\":\"hello\",\"variables\":[\"d\",\"e\"]}}}";
		HttpClientErrorException ex = new HttpClientErrorException(HttpStatus.I_AM_A_TEAPOT, 
				"status text", responseBody.getBytes(), Charset.forName("UTF-8"));
		ASDCException e = new ASDCException(ex);
		e.addSuppressed(new IllegalArgumentException());
		log.info("testASDCException: ASDC msg {}", e.getASDCMessage());
		log.info("testASDCException: msg {}", e.getMessage());
		log.info("testASDCException: is found? {}", ASDCException.isNotFound(new IllegalArgumentException()));
	}

}

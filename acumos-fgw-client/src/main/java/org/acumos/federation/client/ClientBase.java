/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
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
package org.acumos.federation.client;

import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Collections;

import org.apache.http.client.HttpClient;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import org.acumos.cds.domain.MLPCatalog;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPDocument;

import org.acumos.federation.client.config.ClientConfig;
import org.acumos.federation.client.config.TlsConfig;
import org.acumos.federation.client.config.BasicAuthConfig;
import org.acumos.federation.client.data.JsonResponse;
import org.acumos.federation.client.data.Catalog;
import org.acumos.federation.client.data.Solution;
import org.acumos.federation.client.data.SolutionRevision;
import org.acumos.federation.client.data.Artifact;
import org.acumos.federation.client.data.Document;

/**
 * Base class for building clients.
 *
 * This class supports construction with a base URL for the server,
 * configuration of 2-way certificate authentication, credential based
 * authentication, and mapping of received data to extended classes
 * with extra fields.
 *
 * This class uses the Spring Framework RestTemplate mechanism to
 * implement the client side of REST services.
 * In most cases, a subclass only needs to provide
 * methods invoking one of this class' handle() or handleRequest()
 * methods for each transaction.
 */
public class ClientBase {
	/**
	 * Builder for mappers supporting parsing extended classes
	 * with extra fields.
	 */
	public static class MapperBuilder {
		private SimpleModule module = new SimpleModule();

		/**
		 * Create a mapper builder that will use extended
		 * classes {@link Catalog}, {@link Solution}, {@link SolutionRevision}, {@link Artifact}, and {@link Document}.
		 */
		public MapperBuilder() {
			add(MLPCatalog.class, Catalog.class);
			add(MLPSolution.class, Solution.class);
			add(MLPSolutionRevision.class, SolutionRevision.class);
			add(MLPArtifact.class, Artifact.class);
			add(MLPDocument.class, Document.class);
		}

		/**
		 * Add a mapping to the mapper.
		 *
		 * @param base The base (super) class to be mapped.
		 * @param enhanced The enhanced (sub) class to be used, instead.
		 * @return This builder.
		 */
		public <B, E extends B> MapperBuilder add(Class<B> base, Class<E> enhanced) {
			module.addDeserializer(base, new StdDeserializer<B>(base) {
				@Override
				public B deserialize(JsonParser parser, DeserializationContext context) throws IOException {
					return((ObjectMapper)parser.getCodec()).readValue(parser, enhanced);
				}
			});
			return this;
		}

		/**
		 * Build the mapper.
		 *
		 * @return A mapper with the specified class mappings added.
		 */
		public ObjectMapper build() {
			ObjectMapper mapper = new ObjectMapper();
			mapper.registerModule(module);
			mapper.registerModule(new JavaTimeModule());
			return mapper;
		}
	}

	private static ObjectMapper defaultMapper = new MapperBuilder().build();

	/**
	 * Get the current default mapper.
	 *
	 * The default mapper is used when null is specified for the mapper
	 * when constructing a client.
	 * @return The mapper.
	 */
	public static ObjectMapper getDefaultMapper() {
		return defaultMapper;
	}

	/**
	 * Replace the current default mapper.
	 *
	 * @param mapper The new default mapper.
	 */
	public static void setDefaultMapper(ObjectMapper mapper) {
		defaultMapper = mapper;
	}

	/**
	 * The Spring Framework restTemplate.
	 */
	protected RestTemplate restTemplate;

	private static InputStream openResource(ResourceLoader loader, String source) throws IOException {
		try {
			return loader.getResource(source).getURL().openStream();
		} catch (FileNotFoundException fnfe) {
			return new FileInputStream(source);
		}
	}

	private static KeyStore getKeyStore(ResourceLoader loader, String store, String type, String password) throws IOException, GeneralSecurityException {
		if (store == null || type == null || password == null) {
			return null;
		}
		try (InputStream is = openResource(loader, store)) {
			KeyStore ret = KeyStore.getInstance(type);
			ret.load(is, password.toCharArray());
			return ret;
		}
	}

	/**
	 * Build a RestTemplate for a client.
	 *
	 * If mapper is null, the default mapper is used.
	 * If loader is null, a DefaultResourceLoader is created and used.
	 * The loader is used for accessing the key store and trust store
	 * for TLS certificates.
	 *
	 * @param target The base URL for the server to be accessed.
	 * @param conf The configuration for certificates and credentials.
	 * @param mapper The object mapper.
	 * @param loader The resource loader.
	 * @return A RestTemplate for the supplied parameters.
	 */
	public static RestTemplate buildRestTemplate(String target, ClientConfig conf, ObjectMapper mapper, ResourceLoader loader) {
		try {
			target = new URL(target).toExternalForm().replaceAll("/*$", "");
		} catch (MalformedURLException mue) {
			throw new IllegalArgumentException("Bad target URL " + target, mue);
		}
		if (loader == null) {
			loader = new DefaultResourceLoader();
		}
		if (mapper == null) {
			mapper = getDefaultMapper();
		}
		SSLContextBuilder sslContextBuilder = SSLContexts.custom();
		TlsConfig tls = conf.getSsl();
		RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.create();
		registryBuilder.register("http", PlainConnectionSocketFactory.getSocketFactory());
		HttpClientBuilder clientBuilder = HttpClients.custom();
		try {
			if (tls != null) {
				KeyStore store = getKeyStore(loader, tls.getKeyStore(), tls.getKeyStoreType(), tls.getKeyStorePassword());
				if (store != null) {
					String alias = tls.getKeyAlias();
					sslContextBuilder.loadKeyMaterial(store, tls.getKeyStorePassword().toCharArray(), alias == null? null: (aliases, socket) -> alias);
				}
				store = getKeyStore(loader, tls.getTrustStore(), tls.getTrustStoreType(), tls.getTrustStorePassword());
				if (store != null) {
					sslContextBuilder.loadTrustMaterial(store, null);
				}
			}
			SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContextBuilder.build(), new String[] { "TLSv1.2" }, null, SSLConnectionSocketFactory.getDefaultHostnameVerifier());
			registryBuilder.register("https", sslSocketFactory);
			clientBuilder.setConnectionManager(new BasicHttpClientConnectionManager(registryBuilder.build()));
			clientBuilder.setSSLSocketFactory(sslSocketFactory);
		} catch (Exception ex) {
			throw new TlsConfigException("Invalid TLS configuration", ex);
		}
		HttpClient client = clientBuilder.build();
		BasicAuthConfig creds = conf.getCreds();
		MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
		messageConverter.setObjectMapper(mapper);
		ResourceHttpMessageConverter contentConverter = new ResourceHttpMessageConverter();
		contentConverter.setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM));
		HttpComponentsClientHttpRequestFactory chrf = new HttpComponentsClientHttpRequestFactory(client);
		chrf.setBufferRequestBody(false);
		RestTemplateBuilder rtb = new RestTemplateBuilder()
		    .requestFactory(() -> chrf)
		    .messageConverters(new ByteArrayHttpMessageConverter(), messageConverter, contentConverter)
		    .uriTemplateHandler(new DefaultUriBuilderFactory())
		    .rootUri(target);
		if (creds != null && creds.getUsername() != null && creds.getPassword() != null) {
			rtb = rtb.basicAuthentication(creds.getUsername(), creds.getPassword());
		}
		return rtb.build();
	}

	/**
	 * Create a client.
	 *
	 * If mapper is null, the default mapper is used.
	 * If loader is null, a DefaultResourceLoader is created and used.
	 * The loader is used for accessing the key store and trust store
	 * for TLS certificates.
	 *
	 * @param target The base URL for the server to be accessed.
	 * @param conf The configuration for certificates and credentials.
	 * @param mapper The object mapper.
	 * @param loader The resource loader.
	 */
	protected ClientBase(String target, ClientConfig conf, ObjectMapper mapper, ResourceLoader loader) {
		restTemplate = buildRestTemplate(target, conf, mapper, loader);
	}

	/**
	 * Execute a REST transaction.
	 *
	 * @param uri The URI relative, to this client's target.
	 * @param method The HTTP method.
	 * @param type The wire type of the expected response.
	 * @param params Values for parameters in the target and uri.
	 */
	protected <T> T handle(String uri, HttpMethod method, ParameterizedTypeReference<T> type, Object ... params) {
		return restTemplate.exchange(uri, method, null, type, params).getBody();
	}

	/**
	 * Execute a REST transaction and unwrap the response, returning its content.
	 *
	 * @param uri The URI relative, to this client's target.
	 * @param method The HTTP method.
	 * @param type The wire type of the expected response.
	 * @param params Values for parameters in the target and uri.
	 */
	protected <T> T handleResponse(String uri, HttpMethod method, ParameterizedTypeReference<JsonResponse<T>> type, Object ... params) {
		JsonResponse<T> ret = handle(uri, method, type, params);
		return ret == null? null: ret.getContent();
	}

	/**
	 * Execute a REST GET transaction and unwrap the response, returning its content.
	 *
	 * @param uri The URI relative, to this client's target.
	 * @param type The wire type of the expected response.
	 * @param params Values for parameters in the target and uri.
	 */
	protected <T> T handleResponse(String uri, ParameterizedTypeReference<JsonResponse<T>> type, Object ... params) {
		return handleResponse(uri, HttpMethod.GET, type, params);
	}

	/**
	 * Download potentially large binary content from the specified URL.
	 *
	 * @param uri The template for the URI to download.
	 * @param params The parameters for the template.
	 * @return An InputStream for reading the content.
	 */
	protected InputStream download(String uri, Object ... params) {
		ClientHttpResponse response = null;
		URI url = restTemplate.getUriTemplateHandler().expand(uri, params);
		try { // NOSONAR
			/*
			 * Sonar says use try-with-resources, but can't use it
			 * here: have to leave response open when returning its
			 * InputStream (normal case).  Returned InputStream
			 * is wrapped in a FilterInputStream so When returned
			 * value is closed, response will also be closed.
			 */
			ClientHttpRequest request = restTemplate.getRequestFactory().createRequest(url, HttpMethod.GET);
			ResponseErrorHandler errHandler = restTemplate.getErrorHandler();
			request.getHeaders().setAccept(Collections.singletonList(MediaType.ALL));
			response = request.execute();
			if (errHandler.hasError(response)) {
				errHandler.handleError(url, HttpMethod.GET, response);
			}
			ClientHttpResponse xr = response;
			InputStream ret = new FilterInputStream(xr.getBody()) {
				@Override
				public void close() throws IOException {
					in.close();
					xr.close();
				}
			};
			response = null;
			return ret;
		} catch (IOException ioe) {
			throw new ResourceAccessException("I/O error on GET request for \"" + url + "\": " + ioe.getMessage(), ioe);
		} finally {
			if (response != null) {
				response.close();
			}
		}
	}

	/**
	 * Upload potentially large binary content to the specified URL.
	 *
	 * @param uri The template for the URI to download.
	 * @param method The method (usually POST or PUT).
	 * @param inputStream The data to upload.
	 * @param params The parameters for the template.
	 */
	protected void upload(String uri, HttpMethod method, InputStream inputStream, Object ... params) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		restTemplate.exchange(uri, method, new HttpEntity<>(new InputStreamResource(inputStream), headers), Void.class, params);
	}

	/**
	 * Upload (PUT) potentially large binary content to the specified URL.
	 *
	 * @param uri The template for the URI to download.
	 * @param inputStream The data to upload.
	 * @param params The parameters for the template.
	 */
	protected void upload(String uri, InputStream inputStream, Object ... params) {
		upload(uri, HttpMethod.PUT, inputStream, params);
	}
}

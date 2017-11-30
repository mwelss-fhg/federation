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


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;

import java.net.URI;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;

import java.security.KeyStore;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
//import org.apache.http.ssl.SSLContexts;
//import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.SSLContextBuilder;
		
import org.acumos.federation.gateway.config.EELFLoggerDelegate;

@Configuration
//@PropertySource("classpath:configprops.properties")
@ConfigurationProperties(prefix = "client")
public class HttpClientConfiguration {

	@Autowired
	private ResourceLoader resourceLoader;
	
	protected final EELFLoggerDelegate log =
											EELFLoggerDelegate.getLogger(getClass().getName());

	private String username;
	private String passwd;
	private int		 poolSize = 10;
	private SSL		 ssl;
	  
	public String getUsername() { return this.username; }
 	public void 	setUsername(String theUsername)
																{ this.username = theUsername; }
	
	public String getPassword() { return this.username; }
 	public void 	setPassword(String thePassword)
																{ this.passwd = thePassword; }
	
	public int getPoolSize() { return this.poolSize; }
 	public void 	setPoolSize(int thePoolSize)
																{ this.poolSize = thePoolSize; }
	
	public SSL getSSL() { return this.ssl; }
 	public void 	setSSL(SSL theSSL)
																{ this.ssl = theSSL; }

	public static class SSL {

  	private String keyStore;
  	private String keyStoreType = "JKS";
  	private String keyStorePasswd;
  	private String keyAlias;
  	private String trustStore;
  	private String trustStoreType = "JKS";
  	private String trustStorePasswd;

	  public String getKeyStore() { return this.keyStore; }
  	public void 	setKeyStore(String theKeyStore)
																{ this.keyStore = theKeyStore; }
	  
	  public String getKeyStoreType() { return this.keyStoreType; }
  	public void 	setKeyStoreType(String theKeyStoreType)
																{ this.keyStoreType = theKeyStoreType; }

		public String getKeyStorePassword() { return this.keyStorePasswd; }
  	public void 	setKeyStorePassword(String theKeyStorePassword)
																{ this.keyStorePasswd = theKeyStorePassword; }
	  
		public String getKeyAlias() { return this.keyAlias; }
  	public void 	setKeyAlias(String theKeyAlias)
																{ this.keyAlias = theKeyAlias; }
	  
		public String getTrustStore() { return this.trustStore; }
  	public void 	setTrustStore(String theTrustStore)
																{ this.trustStore = theTrustStore; }
		
		public String getTrustStoreType() { return this.trustStoreType; }
  	public void 	setTrustStoreType(String theTrustStoreType)
																{ this.trustStoreType = theTrustStoreType; }

		public String getTrustStorePassword() { return this.trustStorePasswd; }
  	public void 	setTrustStorePassword(String theTrustStorePassword)
															{ this.trustStorePasswd = theTrustStorePassword; }

		protected boolean hasKeyStoreInfo() {
			return this.keyStore != null &&
						 this.keyStoreType != null &&
						 this.keyStorePasswd != null;
		}

		protected boolean hasTrustStoreInfo() {
			return this.trustStore != null &&
						 this.trustStoreType != null /*&&
						 this.trustStorePasswd != null*/;
		}

		public String toString() {
			return new StringBuilder("")	
							.append("SSL(")
							.append(this.keyStore)
							.append(",")
							.append(this.keyStoreType)
							.append(",")
							.append(this.keyAlias)
							.append("/")
							.append(this.trustStore)
							.append(",")
							.append(this.trustStoreType)
							.append(")")
							.toString();
		}
	}

	public String toString() {
		return new StringBuilder("")	
						.append("ClientConfiguration(")
						.append(this.ssl)
						.append(")")
						.toString();
	}

  public HttpClient buildClient() {

		SSLContext sslContext = null;
		log.info(EELFLoggerDelegate.debugLogger, "Build HttpClient with " + this);

		if (this.ssl == null) {
			log.info(EELFLoggerDelegate.debugLogger, "No ssl config was provided");
		}
		else {
			KeyStore keyStore = null;
			if (this.ssl.hasKeyStoreInfo()) {
				try {
					keyStore = KeyStore.getInstance(this.ssl.keyStoreType);
					keyStore.load(this.resourceLoader.getResource(this.ssl.keyStore)
														.getURL().openStream(),
												//new URI(this.ssl.keyStore).toURL().openStream(),
												this.ssl.keyStorePasswd.toCharArray());
					log.info(EELFLoggerDelegate.debugLogger, "Loaded key store: " + this.ssl.keyStore);
				}
				catch (Exception x) {
					throw new IllegalStateException(
										"Error loading key material", x);
				}
			}

			KeyStore trustStore = null;
			if (this.ssl.hasTrustStoreInfo()) {
				try {
					trustStore = KeyStore.getInstance(this.ssl.trustStoreType);
					trustStore.load(this.resourceLoader.getResource(this.ssl.trustStore)
															.getURL().openStream(),
													//new URI(this.ssl.trustStore).toURL().openStream(),
													this.ssl.trustStorePasswd.toCharArray());
					log.info(EELFLoggerDelegate.debugLogger, "Loaded trust store: " + this.ssl.trustStore);
				}
				catch (Exception x) {
					throw new IllegalStateException(
										"Error loading trust material", x);
				}
			}

			SSLContextBuilder contextBuilder = SSLContexts.custom();
			try {
				if (keyStore != null) {
  	  	  contextBuilder.loadKeyMaterial(
														keyStore,
														this.ssl.keyStorePasswd.toCharArray()/*,
														(aliases, socket) -> { 
																
																	return this.ssl.keyAlias;
														}*/);
				}

				if (trustStore != null) {				
  	     	contextBuilder.loadTrustMaterial(
														trustStore,
														(x509Certificates, s) -> false);
				}

				sslContext = contextBuilder.build();
			}
			catch (Exception x) {
				throw new IllegalStateException(
										"Error building ssl context", x);
			}
		}
//!!TODO: teh default hostname verifier needs to be changed!!
    
    SSLConnectionSocketFactory sslSocketFactory = null;
		if (sslContext != null) {
			sslSocketFactory =
							new SSLConnectionSocketFactory(
      							sslContext,
            				new String[] { "TLSv1.2" },
            				null,
            				SSLConnectionSocketFactory.getDefaultHostnameVerifier());
				log.info(EELFLoggerDelegate.debugLogger, "SSL connection factory configured");
		}

		RegistryBuilder<ConnectionSocketFactory> registryBuilder = 
						RegistryBuilder.<ConnectionSocketFactory>create();
		registryBuilder.register("http", PlainConnectionSocketFactory.getSocketFactory());
		if (sslSocketFactory != null) {
			registryBuilder.register("https", sslSocketFactory);
		}
		Registry<ConnectionSocketFactory> registry = registryBuilder.build();

		/*
		PoolingHttpClientConnectionManager connectionManager = 
			new PoolingHttpClientConnectionManager(registry);
		connectionManager.setMaxTotal(this.poolSize);
		connectionManager.setDefaultMaxPerRoute(this.poolSize);
		*/

		CredentialsProvider credsProvider = null;
    if (this.username != null && this.passwd != null) {
			credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(
				AuthScope.ANY, new UsernamePasswordCredentials(
																		this.username, this.passwd));
			log.info(EELFLoggerDelegate.debugLogger, "Credentials configured");
		}
		else {
			log.info(EELFLoggerDelegate.debugLogger, "No credentials were provided");
		}

		HttpClientBuilder clientBuilder = HttpClients.custom();

		//clientBuilder.setConnectionManager(connectionManager);
		clientBuilder.setConnectionManager(new BasicHttpClientConnectionManager(registry));

		if (sslSocketFactory != null)
    	clientBuilder.setSSLSocketFactory(sslSocketFactory);

		if (credsProvider != null)
			clientBuilder.setDefaultCredentialsProvider(credsProvider);

		return clientBuilder.build();
  }	
}


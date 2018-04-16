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

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileNotFoundException;

import java.net.URI;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;

import java.security.KeyStore;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.Ssl;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.DefaultResourceLoader;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.protocol.HttpContext;
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
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;


import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;

import org.acumos.federation.gateway.config.EELFLoggerDelegate;

@Component
public class InterfaceConfiguration {

	@Autowired
	private ResourceLoader resourceLoader;

	protected final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(getClass().getName());

	//private int poolSize = 10;

	private String 			address;
	private InetAddress	inetAddress;
	private SSL 		ssl;
	private Client	client;
	private	Server	server; 

	public InterfaceConfiguration() {
		log.info(EELFLoggerDelegate.debugLogger, this + "::new");
	}

/*
	public int getPoolSize() {
		return this.poolSize;
	}

	public void setPoolSize(int thePoolSize) {
		this.poolSize = thePoolSize;
	}
*/
	@PostConstruct
	public void initInterface() {
		log.info(EELFLoggerDelegate.debugLogger, this + "::init");
	}	

	public String getAddress() {
		return this.address;
	}

	public void setAddress(String theAddress) throws UnknownHostException {
		this.inetAddress = InetAddress.getByName(theAddress);
		this.address = theAddress;
	}

	public Client getClient() {
		return this.client;
	}

	public void setClient(Client theClient) {
		this.client = theClient;
	}

	public Server getServer() {
		return this.server;
	}

	public void setServer(Server theServer) {
		this.server = theServer;
	}

	public SSL getSSL() {
		return this.ssl;
	}

	public void setSSL(SSL theSSL) {
		this.ssl = theSSL;
	}

	protected boolean hasSSL() {
		return this.ssl != null;
	}

	protected boolean hasServer() {
		return this.server != null;
	}

	protected boolean hasClient() {
		return this.client != null &&
					 this.client.getUsername() != null &&
					 this.client.getPassword() != null;
	}

	protected boolean hasAddress() {
		return this.address != null;
	}

	/**
	 */
	public static class Client {

		private String username;
		private String passwd;

		public String getUsername() {
			return this.username;
		}

		public void setUsername(String theUsername) {
			this.username = theUsername;
		}

		public String getPassword() {
			return this.username;
		}

		public void setPassword(String thePassword) {
			this.passwd = thePassword;
		}

	}

	/**
	 */
	public static class Server {
	
		private int 		port;
	
		public int getPort() {
			return this.port;
		}

		public void setPort(int thePort) {
			this.port = thePort;
		}

	}

	/**
	 * Security information for this endpoint, applies to both client and server
	 * usage.
	 */
	public static class SSL {

		private String keyStore;
		private String keyStoreType = "JKS";
		private String keyStorePasswd;
		private String keyAlias;
		private String trustStore;
		private String trustStoreType = "JKS";
		private String trustStorePasswd;
		private String clientAuth = "need";

		public String getKeyStore() {
			return this.keyStore;
		}

		public void setKeyStore(String theKeyStore) {
			this.keyStore = theKeyStore;
		}

		public String getKeyStoreType() {
			return this.keyStoreType;
		}

		public void setKeyStoreType(String theKeyStoreType) {
			this.keyStoreType = theKeyStoreType;
		}

		public String getKeyStorePassword() {
			return this.keyStorePasswd;
		}

		public void setKeyStorePassword(String theKeyStorePassword) {
			this.keyStorePasswd = theKeyStorePassword;
		}

		public String getKeyAlias() {
			return this.keyAlias;
		}

		public void setKeyAlias(String theKeyAlias) {
			this.keyAlias = theKeyAlias;
		}

		public String getTrustStore() {
			return this.trustStore;
		}

		public void setTrustStore(String theTrustStore) {
			this.trustStore = theTrustStore;
		}

		public String getTrustStoreType() {
			return this.trustStoreType;
		}

		public void setTrustStoreType(String theTrustStoreType) {
			this.trustStoreType = theTrustStoreType;
		}

		public String getTrustStorePassword() {
			return this.trustStorePasswd;
		}

		public void setTrustStorePassword(String theTrustStorePassword) {
			this.trustStorePasswd = theTrustStorePassword;
		}

		protected boolean hasKeyStoreInfo() {
			return this.keyStore != null && this.keyStoreType != null && this.keyStorePasswd != null;
		}

		protected boolean hasTrustStoreInfo() {
			return this.trustStore != null && this.trustStoreType != null /*
																			 * && this.trustStorePasswd != null
																			 */;
		}

		public String getClientAuth() {
			return this.clientAuth;
		}

		public void setClientAuth(String theClientAuth) {
			this.clientAuth = theClientAuth;
		}

		public String toString() {
			return new StringBuilder("").append("SSL(").append(this.keyStore).append(",").append(this.keyStoreType)
					.append(",").append(this.keyAlias).append("/").append(this.trustStore).append(",")
					.append(this.trustStoreType).append(")").toString();
		}
	}

	public String toString() {
		return new StringBuilder("")
			.append(super.toString())
			.append("(")
			.append(this.address)
			.append(",")
			.append(this.server)
			.append(",")
			.append(this.ssl)
			.append(")")
			.toString();
	}

	/**
	 * Configure the existing/default/a servlet container with the configuration
	 * information of this interface.
	 * @param theContainer the servlet container to be configured
	 * @return ConfigurableEmbeddedServletContainer the container, configured
	 */
	public ConfigurableEmbeddedServletContainer configureContainer(
										ConfigurableEmbeddedServletContainer theContainer) {
		if (hasServer()) {
			theContainer.setPort(this.server.getPort());
		}
		if (hasAddress()) {
			theContainer.setAddress(this.inetAddress);
		}
		if (hasSSL()) {
			Ssl cssl = new Ssl();
			cssl.setEnabled(true);
			cssl.setProtocol("TLSv1.2");
			cssl.setKeyStore(this.ssl.getKeyStore());
			cssl.setKeyStorePassword(this.ssl.getKeyStorePassword());
			cssl.setKeyStoreType(this.ssl.getKeyStoreType());
			cssl.setTrustStore(this.ssl.getTrustStore());
			cssl.setTrustStorePassword(this.ssl.getTrustStorePassword());
			cssl.setTrustStoreType(this.ssl.getTrustStoreType());
			cssl.setKeyAlias(this.ssl.getKeyAlias());
			cssl.setClientAuth(Ssl.ClientAuth.valueOf(this.ssl.clientAuth.toUpperCase()));
			theContainer.setSsl(cssl);
		}
		return theContainer;
	}

	/**
	 * Build a tomcat connector (server) based on the configuration information.
	 *
	 * Should we even allow the constrcution of a non-secure connector ?
	 *
	 * @return tomcat nio connector
	 */
	public Connector buildConnector() {

		if (!hasServer()) {
			throw new IllegalArgumentException("No server information available");
		}

		Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
		Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
		connector.setScheme(hasSSL() ? "https" : "http");
		connector.setSecure(hasSSL());
		connector.setPort(this.server.getPort());
		if (hasAddress()) {
			protocol.setAddress(this.inetAddress);
		}
		if (hasSSL()) {
			protocol.setSSLEnabled(true);
			protocol.setKeystoreFile(this.ssl.getKeyStore());
			protocol.setKeystorePass(this.ssl.getKeyStorePassword());
			protocol.setTruststoreFile(this.ssl.getTrustStore());
			protocol.setTruststorePass(this.ssl.getTrustStorePassword());
			protocol.setKeyAlias(this.ssl.getKeyAlias());
		}
		return connector;
	}

	public HttpClient buildClient() {

		SSLContext sslContext = null;
		log.info(EELFLoggerDelegate.debugLogger, "Build HttpClient with " + this);

		if (this.resourceLoader == null)
			this.resourceLoader = new DefaultResourceLoader();

		if (this.ssl == null) {
			log.info(EELFLoggerDelegate.debugLogger, "No ssl config was provided");
		}
		else {
			KeyStore keyStore = null;
			if (this.ssl.hasKeyStoreInfo()) {
				InputStream keyStoreSource = null;
				try {
					keyStoreSource = this.resourceLoader.getResource(this.ssl.keyStore).getURL().openStream();
				}
				catch (FileNotFoundException rnfx) {
					try {
						keyStoreSource = new FileInputStream(this.ssl.keyStore);
					}
					catch (FileNotFoundException fnfx) {
						throw new IllegalStateException("Failed to find key store " + this.ssl.keyStore);
					}
				}
				catch (IOException iox) {
					throw new IllegalStateException("Error loading key material: " + iox, iox);
				}

				try {
					keyStore = KeyStore.getInstance(this.ssl.keyStoreType);
					keyStore.load(keyStoreSource,	this.ssl.keyStorePasswd.toCharArray());
					log.info(EELFLoggerDelegate.debugLogger, "Loaded key store: " + this.ssl.keyStore);
				}
				catch (Exception x) {
					throw new IllegalStateException("Error loading key material: " + x, x);
				}
				finally {
					try {
						keyStoreSource.close();
					}
					catch (IOException iox) {
						log.debug(EELFLoggerDelegate.debugLogger, "Error closing key store source", iox);
					}
				}
			}

			KeyStore trustStore = null;
			if (this.ssl.hasTrustStoreInfo()) {
				InputStream trustStoreSource = null;
				try {
					trustStoreSource = this.resourceLoader.getResource(this.ssl.trustStore).getURL().openStream();
				}
				catch (FileNotFoundException rnfx) {
					try {
						trustStoreSource = new FileInputStream(this.ssl.trustStore);
					}
					catch (FileNotFoundException fnfx) {
						throw new IllegalStateException("Failed to find trust store " + this.ssl.keyStore);
					}
				}
				catch (IOException iox) {
					throw new IllegalStateException("Error loading trust material: " + iox, iox);
				}

				try {
					trustStore = KeyStore.getInstance(this.ssl.trustStoreType);
					trustStore.load(trustStoreSource,	this.ssl.trustStorePasswd.toCharArray());
					log.info(EELFLoggerDelegate.debugLogger, "Loaded trust store: " + this.ssl.trustStore);
				}
				catch (Exception x) {
					throw new IllegalStateException("Error loading trust material: " + x, x);
				}
				finally {
					try {
						trustStoreSource.close();
					}
					catch (IOException iox) {
						log.debug(EELFLoggerDelegate.debugLogger, "Error closing trust store source", iox);
					}
				}
			}

			SSLContextBuilder contextBuilder = SSLContexts.custom();
			try {
				if (keyStore != null) {
					contextBuilder.loadKeyMaterial(keyStore,
							this.ssl.keyStorePasswd.toCharArray()/*
																	 * , (aliases, socket) -> {
																	 * 
																	 * return this.ssl.keyAlias; }
																	 */);
				}

				if (trustStore != null) {
					contextBuilder.loadTrustMaterial(trustStore, (x509Certificates, s) -> false);
				}

				sslContext = contextBuilder.build();
			} catch (Exception x) {
				throw new IllegalStateException("Error building ssl context", x);
			}
		}
		// !!TODO: teh default hostname verifier needs to be changed!!

		SSLConnectionSocketFactory sslSocketFactory = null;
		if (sslContext != null) {
			sslSocketFactory = new SSLConnectionSocketFactory(sslContext, new String[] { "TLSv1.2" }, null,
					SSLConnectionSocketFactory.getDefaultHostnameVerifier());
			log.info(EELFLoggerDelegate.debugLogger, "SSL connection factory configured");
		}

		RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.<ConnectionSocketFactory>create();
		registryBuilder.register("http", PlainConnectionSocketFactory.getSocketFactory());
		if (sslSocketFactory != null) {
			registryBuilder.register("https", sslSocketFactory);
		}
		Registry<ConnectionSocketFactory> registry = registryBuilder.build();

		/*
		 * PoolingHttpClientConnectionManager connectionManager = new
		 * PoolingHttpClientConnectionManager(registry);
		 * connectionManager.setMaxTotal(this.poolSize);
		 * connectionManager.setDefaultMaxPerRoute(this.poolSize);
		 */

		CredentialsProvider credsProvider = null;
		if (hasClient()) {
			credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(this.client.getUsername(), this.client.getPassword()));
			log.info(EELFLoggerDelegate.debugLogger, "Credentials configured");
		} else {
			log.info(EELFLoggerDelegate.debugLogger, "No credentials were provided");
		}

		HttpClientBuilder clientBuilder = HttpClients.custom();

		// clientBuilder.setConnectionManager(connectionManager);
		clientBuilder.setConnectionManager(new BasicHttpClientConnectionManager(registry));

		if (sslSocketFactory != null)
			clientBuilder.setSSLSocketFactory(sslSocketFactory);

		if (credsProvider != null)
			clientBuilder.setDefaultCredentialsProvider(credsProvider);

		if (hasAddress()) {
			clientBuilder.setRoutePlanner(
				new HttpRoutePlanner() {
					public HttpRoute determineRoute(HttpHost theTarget, HttpRequest theRequest, HttpContext theContext) {
						return new HttpRoute(theTarget, InterfaceConfiguration.this.inetAddress, hasSSL());
					}
				});
		}

		return clientBuilder.build();
	}
}

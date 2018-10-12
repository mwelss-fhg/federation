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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
//import org.apache.http.ssl.SSLContexts;
//import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.Ssl;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class InterfaceConfiguration {

	private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(MethodHandles.lookup().lookupClass());

	private String 			address;
	private InetAddress	inetAddress;
	private SSL 		ssl;
	private Client	client;
	private	Server	server; 

	public InterfaceConfiguration() {
		log.trace(EELFLoggerDelegate.debugLogger, this + "::new");
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
		log.trace(EELFLoggerDelegate.debugLogger, this + "::init");
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
	 * Provide the subject name specified in the SSL config
	 */
	public String getSubjectName() throws KeyStoreException {
		if (!hasSSL())
			return null;
	
		KeyStore keyStore = this.ssl.loadKeyStore();
		X509Certificate certEntry = null;
		
		String alias = null;
		if (this.ssl.keyAlias == null) {
			//we expect only one entry
			Enumeration<String> aliases = keyStore.aliases();
			try {
				alias = aliases.nextElement();
			}
			catch(NoSuchElementException nsex) {
				throw new KeyStoreException("Key store contains no entries");
			}

			assert(!aliases.hasMoreElements());
		}
		else {
			alias = this.ssl.keyAlias;
		}

		certEntry =  (X509Certificate)keyStore.getCertificate(alias);
		if (certEntry == null)
			return null;

		return certEntry.getSubjectX500Principal().getName();
	}

	@Override
	public String toString() {
		return new StringBuilder()
			.append(super.toString())
			.append('(')
			.append(this.address)
			.append(',')
			.append(this.server)
			.append(',')
			.append(this.client)
			.append(',')
			.append(this.ssl)
			.append(')')
			.toString();
	}


	/**
	 */
	public static class Client {

		private String username;
		private String passwd;

		public Client() {}

		public Client(String theUsername, String thePassword) {
			setUsername(theUsername);
			setPassword(thePassword);
		}

		public String getUsername() {
			return this.username;
		}

		public void setUsername(String theUsername) {
			this.username = theUsername;
		}

		public String getPassword() {
			return this.passwd;
		}

		public void setPassword(String thePassword) {
			this.passwd = thePassword;
		}

		@Override
		public String toString() {
			return new StringBuilder()
				.append(super.toString())
				.append('(')
				.append(this.username)
				.append(',')
				.append(this.passwd)
				.append(')')
				.toString();
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

		@Override
		public String toString() {
			return new StringBuilder()
				.append(super.toString())
				.append('(')
				.append(this.port)
				.append(')')
				.toString();
		}
	}

	/**
	 * Security information for this endpoint, applies to both client and server
	 * usage.
	 */
	public static class SSL {

		private final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(getClass().getName());

		@Autowired
		private ResourceLoader resourceLoader;

		private KeyStore	keyStore,
											trustStore;

		private String keyStoreLocation;
		private String keyStoreType = "JKS";
		private String keyStorePasswd;
		private String keyAlias;
		private String trustStoreLocation;
		private String trustStoreType = "JKS";
		private String trustStorePasswd;
		private String clientAuth = "need";

		public String getKeyStore() {
			return this.keyStoreLocation;
		}

		public void setKeyStore(String theKeyStoreLocation) {
			this.keyStoreLocation = theKeyStoreLocation;
			this.keyStore = null;
		}

		public String getKeyStoreType() {
			return this.keyStoreType;
		}

		public void setKeyStoreType(String theKeyStoreType) {
			this.keyStoreType = theKeyStoreType;
			this.keyStore = null;
		}

		public String getKeyStorePassword() {
			return this.keyStorePasswd;
		}

		public void setKeyStorePassword(String theKeyStorePassword) {
			this.keyStorePasswd = theKeyStorePassword;
			this.keyStore = null;
		}

		public String getKeyAlias() {
			return this.keyAlias;
		}

		public void setKeyAlias(String theKeyAlias) {
			this.keyAlias = theKeyAlias;
		}

		public String getTrustStore() {
			return this.trustStoreLocation;
		}

		public void setTrustStore(String theTrustStoreLocation) {
			this.trustStoreLocation = theTrustStoreLocation;
			this.trustStore = null;
		}

		public String getTrustStoreType() {
			return this.trustStoreType;
		}

		public void setTrustStoreType(String theTrustStoreType) {
			this.trustStoreType = theTrustStoreType;
			this.trustStore = null;
		}

		public String getTrustStorePassword() {
			return this.trustStorePasswd;
		}

		public void setTrustStorePassword(String theTrustStorePassword) {
			this.trustStorePasswd = theTrustStorePassword;
			this.trustStore = null;
		}

		protected boolean hasKeyStoreInfo() {
			return this.keyStoreLocation != null && this.keyStoreType != null && this.keyStorePasswd != null;
		}

		protected boolean hasTrustStoreInfo() {
			return this.trustStoreLocation != null && this.trustStoreType != null /*
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
			return new StringBuilder("").append("SSL(").append(this.keyStoreLocation).append(",").append(this.keyStoreType)
					.append(",").append(this.keyAlias).append("/").append(this.trustStoreLocation).append(",")
					.append(this.trustStoreType).append(")").toString();
		}

		/**
		 * Loads the specified key store
		 * @return the key store
		 */
		public KeyStore loadKeyStore() {
			return this.keyStore == null ?
							this.keyStore = loadStore(this.keyStoreLocation, this.keyStoreType, this.keyStorePasswd) :
							this.keyStore;
		}

		/**
		 * Loads the specified trust store
		 * @return the key store
		 */
		public KeyStore loadTrustStore() {
			return this.trustStore == null ? 
							this.trustStore = loadStore(this.trustStoreLocation, this.trustStoreType, this.trustStorePasswd) :
							this.trustStore;
		}

		/** */
		private KeyStore loadStore(String theLocation, String theType, String thePasswd) {
			KeyStore store = null;
			InputStream storeSource = null;

			if (this.resourceLoader == null)
				this.resourceLoader = new DefaultResourceLoader();

			try {
				storeSource = this.resourceLoader.getResource(theLocation).getURL().openStream();
			}
			catch (FileNotFoundException rnfx) {
				try {
					storeSource = new FileInputStream(theLocation);
				}
				catch (FileNotFoundException fnfx) {
					throw new IllegalStateException("Failed to find key store " + theLocation);
				}
			}
			catch (IOException iox) {
				throw new IllegalStateException("Error loading key material: " + iox, iox);
			}

			try {
				store = KeyStore.getInstance(theType);
				store.load(storeSource,	thePasswd.toCharArray());
				log.trace(EELFLoggerDelegate.debugLogger, "Loaded key store: " + theLocation);
			}
			catch (Exception x) {
				throw new IllegalStateException("Error loading key material: " + x, x);
			}
			finally {
				try {
					storeSource.close();
				}
				catch (IOException iox) {
					log.warn(EELFLoggerDelegate.debugLogger, "Error closing key store source", iox);
				}
			}
			return store;
		}
	
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
		log.trace(EELFLoggerDelegate.debugLogger, "Build HttpClient with " + this);

		if (this.ssl == null) {
			log.trace(EELFLoggerDelegate.debugLogger, "No ssl config was provided");
		}
		else {
			KeyStore keyStore = this.ssl.loadKeyStore(),
							 trustStore = this.ssl.loadTrustStore();

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
			}
			catch (Exception x) {
				throw new IllegalStateException("Error building ssl context", x);
			}
		}
		// !!TODO: teh default hostname verifier needs to be changed!!

		SSLConnectionSocketFactory sslSocketFactory = null;
		if (sslContext != null) {
			sslSocketFactory = new SSLConnectionSocketFactory(sslContext, new String[] { "TLSv1.2" }, null,
					SSLConnectionSocketFactory.getDefaultHostnameVerifier());
			log.trace(EELFLoggerDelegate.debugLogger, "SSL connection factory configured");
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
			log.trace(EELFLoggerDelegate.debugLogger, "Credentials configured");
		}
		else {
			log.trace(EELFLoggerDelegate.debugLogger, "No credentials were provided");
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

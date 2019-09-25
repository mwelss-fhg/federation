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
package org.acumos.federation.gateway;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.fasterxml.jackson.databind.ObjectMapper;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import org.acumos.cds.transport.RestPageRequest;
import org.acumos.cds.transport.RestPageResponse;

import org.acumos.federation.client.config.TlsConfig;


/**
 * Main program for the Acumos Federation Gateway Server.
 */
@SpringBootApplication
@EnableConfigurationProperties
@EnableGlobalMethodSecurity(securedEnabled=true)
@EnableScheduling
public class Application {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * Create instance of application class.  Used by SpringBoot framework.
	 */
	public Application(){} //NOSONAR

	/**
	 * Gather values from multi-page common-dataservice requests.
	 * @param fcn Function to fetch a page of values.
	 * @return All of the values.
	 */
	public static <T> List<T> cdsAll(Function<RestPageRequest, RestPageResponse<T>> fcn) {
		RestPageRequest request = new RestPageRequest(0, 100);
		List<T> ret = new ArrayList<>();
		RestPageResponse<T> response;
		do {
			response = fcn.apply(request);
			ret.addAll(response.getContent());
			request.setPage(response.getNumber() + 1);
		} while (!response.isLast());
		return ret;
	}

	/**
	 * Configure one of the child servers (Federation and Gateway).
	 *
	 * @param server The server to configure.
	 * @param config The host, port, and TLS the server should use.
	 */
	public static void configureServer(ConfigurableServletWebServerFactory server, ServerConfig config) {
		if (config.getServer() != null && config.getServer().getPort() != 0) {
			server.setPort(config.getServer().getPort());
		}
		if (config.getAddress() != null) {
			try {
				server.setAddress(InetAddress.getByName(config.getAddress()));
			} catch (UnknownHostException uhe) {
				throw new BeanInitializationException("Invalid server address", uhe);
			}
		}
		TlsConfig tls = config.getSsl();
		if (tls != null) {
			Ssl ssl = new Ssl();
			ssl.setEnabled(true);
			ssl.setProtocol("TLSv1.2");
			ssl.setKeyStore(tls.getKeyStore());
			ssl.setKeyStorePassword(tls.getKeyStorePassword());
			ssl.setKeyAlias(tls.getKeyAlias());
			ssl.setKeyStoreType(tls.getKeyStoreType());
			ssl.setTrustStore(tls.getTrustStore());
			ssl.setTrustStorePassword(tls.getTrustStorePassword());
			ssl.setTrustStoreType(tls.getTrustStoreType());
			ssl.setClientAuth(config.getClientAuth());
			server.setSsl(ssl);
		}
	}

	@Bean
	@ConfigurationProperties(prefix="local")
	ServerConfig local() {
		return new ServerConfig();
	}

	@Bean
	@ConfigurationProperties(prefix="federation")
	FederationConfig federation() {
		return new FederationConfig();
	}

	@Bean
	@ConfigurationProperties(prefix="cdms.client")
	ServiceConfig cdmsConfig() {
		return new ServiceConfig();
	}

	@Bean
	@ConfigurationProperties(prefix="nexus")
	NexusConfig nexusConfig() {
		return new NexusConfig();
	}

	@Bean
	@ConfigurationProperties(prefix="docker")
	DockerConfig dockerConfig() {
		return new DockerConfig();
	}

	@Bean
	@ConfigurationProperties(prefix="verification")
	ServiceConfig verificationConfig() {
		return new ServiceConfig();
	}

	@Bean
	@ConfigurationProperties(prefix="license-manager")
	ServiceConfig lmConfig() {
		return new ServiceConfig();
	}

	@Bean
	Clients clients() {
		return new Clients();
	}

	@Bean
	PeerService peerService() {
		return new PeerServiceImpl();
	}

	@Bean
	ContentService contentService() {
		return new ContentServiceImpl();
	}

	@Bean
	WebSecurityConfigurerAdapter security() {
		return new Security();
	}

	@Bean
	CatalogService catalogService() {
		return new CatalogServiceImpl();
	}

	static Docket getApi() {
		String version = Application.class.getPackage().getImplementationVersion();
		return new Docket(DocumentationType.SWAGGER_2)
		    .select()
		    .apis(RequestHandlerSelectors.basePackage(Application.class.getPackage().getName()))
		    .paths(PathSelectors.any())
		    .build()
		    .apiInfo(
		        new ApiInfoBuilder()
			    .title("Acumos Federation Gateway REST API")
			    .description(
			        "Provides Model Sharing services for " +
				"Acumos.  All service endpoints use " +
				"2-way TLS/HTTPS certificate authentication.")
			    .termsOfServiceUrl("Terms of service")
			    .contact(new Contact(
				"Acumos Dev Team",
				"http://acumos.readthedocs.io/en/latest/submodules/federation/docs/",
				"noreply@acumos.org"))
			    .license("Apache 2.0 License")
			    .licenseUrl("http://www.apache.org/licenses/LICENSE-2.0")
			    .version(version == null? "version not available": version)
			    .build());
	}

	public static void main(String[] args) throws IOException {
		String configenv = System.getenv("SPRING_APPLICATION_JSON");
		if (configenv != null && (new ObjectMapper()).readTree(configenv) != null) {
			log.info("Parseable environment configuration found.");
		} else {
			log.warn("No valid environment configuration found.");
		}
		SpringApplicationBuilder builder = new SpringApplicationBuilder(Application.class)
		    .properties(
		        "spring.main.allow-bean-definition-overriding=true",
		        "spring.mvc.async.request-timeout=10m")
		    .bannerMode(Banner.Mode.OFF)
		    .web(WebApplicationType.NONE);
		builder.child(FederationServer.class)
		    .bannerMode(Banner.Mode.OFF)
		    .web(WebApplicationType.SERVLET)
		    .run(args);
		builder.child(GatewayServer.class)
		    .bannerMode(Banner.Mode.OFF)
		    .web(WebApplicationType.SERVLET)
		    .run(args);
	}
}

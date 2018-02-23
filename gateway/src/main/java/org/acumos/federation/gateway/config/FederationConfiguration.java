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

import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.controller.CatalogController;
import org.acumos.federation.gateway.controller.PeersController;
import org.acumos.federation.gateway.controller.PingController;
import org.acumos.federation.gateway.security.AuthenticationConfiguration;

import org.apache.http.client.HttpClient;

import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;

/**
 * Provide those beans used in the interaction with other peers (federation)
 */
@Configuration
@Import(AuthenticationConfiguration.class)
@EnableAutoConfiguration
//@ConfigurationProperties(prefix = "federation", ignoreInvalidFields = true)
public class FederationConfiguration {

	@Autowired
	private FederationInterfaceConfiguration interfaceConfig;
	private EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(getClass().getName());

	public FederationConfiguration() {
	}

	@Bean
	public CatalogController catalogServer() {
		return new CatalogController();
	}
	
	@Bean
	public PeersController peersServer() {
		return new PeersController();
	}

	@Bean
	public PingController pingServer() {
		return new PingController();
	}

	/**
   * Build a client for interacting with peers through the defined
	 * federation interface.
	 * We assume the same configuration takes place for client and server
	 * roles when interacting with peers: we'll assume the same identity, use the
	 * same network interface, etc. If this ever needs to change we can pick
	 * the values from a separate configuration properties set.
	 */
	@Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	//@ConfigurationProperties(prefix = "server", ignoreInvalidFields = true)
	public HttpClient federationClient() {
		log.debug(EELFLoggerDelegate.debugLogger, this + "::federationClient from " + this.interfaceConfig);
		return this.interfaceConfig.buildClient();
	}

/*
	@Bean
	public EmbeddedServletContainerFactory federationServer() {
		TomcatEmbeddedServletContainerFactory tomcat =
			new TomcatEmbeddedServletContainerFactory();
		tomcat.addAdditionalTomcatConnectors(this.interfaceConfig.buildConnector());
		return tomcat;
	}
*/
	@Bean
	public EmbeddedServletContainerCustomizer federationServer() {
		log.debug(EELFLoggerDelegate.debugLogger, this + "::federationServer from " + this.interfaceConfig);
		return new EmbeddedServletContainerCustomizer() {
			@Override
			public void customize(ConfigurableEmbeddedServletContainer theContainer) {
				FederationConfiguration.this.interfaceConfig.configureContainer(theContainer);
			}
		}; 
	}
	
}

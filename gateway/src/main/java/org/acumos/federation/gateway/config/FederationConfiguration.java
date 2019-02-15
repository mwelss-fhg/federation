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

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.acumos.federation.gateway.controller.CatalogController;
import org.acumos.federation.gateway.controller.PeersController;
import org.acumos.federation.gateway.controller.PingController;
import org.acumos.federation.gateway.controller.RegistrationController;
import org.apache.http.client.HttpClient;
//import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * Provide those beans used in the interaction with other peers (federation)
 */
@Configuration
@EnableAutoConfiguration
public class FederationConfiguration {

	@Autowired
	private FederationInterfaceConfiguration interfaceConfig;
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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

	@Bean
	@Conditional({RegistrationCondition.class})
	public RegistrationController registrationServer() {
		return new RegistrationController();
	}

	/**
   * Build a client for interacting with peers through the defined
	 * federation interface.
	 * We assume the same configuration takes place for client and server
	 * roles when interacting with peers: we'll assume the same identity, use the
	 * same network interface, etc. If this ever needs to change we can pick
	 * the values from a separate configuration properties set.
	 * @return Client
	 */
	@Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	//@ConfigurationProperties(prefix = "server", ignoreInvalidFields = true)
	public HttpClient federationClient() {
		log.debug(this + "::federationClient from " + this.interfaceConfig);
		return this.interfaceConfig.buildClient();
	}

	@Bean
	public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> federationServer() {
		log.debug(this + "::federationServer from " + this.interfaceConfig);
		return new WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>() {
			@Override
			public void customize(ConfigurableServletWebServerFactory theServer) {
				FederationConfiguration.this.interfaceConfig.configureWebServer(theServer);
			}
		}; 
	}
	
}

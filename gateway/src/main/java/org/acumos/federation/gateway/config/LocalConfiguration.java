/*-
 * ===============LICENSE_START=======================================================
 * Acumos Apache-2.0
 * ===================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
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

import org.acumos.federation.gateway.controller.PeerCatalogController;
import org.acumos.federation.gateway.controller.PeerPeersController;
import org.acumos.federation.gateway.controller.PeerPingController;
import org.acumos.federation.gateway.controller.PeerSubscriptionController;
import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * Provides the beans used in interactions with the local Acumos system
 */
@Configuration
@EnableAutoConfiguration
public class LocalConfiguration {

	@Autowired
	private LocalInterfaceConfiguration interfaceConfig;
	private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(MethodHandles.lookup().lookupClass());

	public LocalConfiguration() {
	}

	@Bean
	public PeerPingController peerPingServer() {
		return new PeerPingController();
	}

	@Bean
	public PeerCatalogController peerCatalogServer() {
		return new PeerCatalogController();
	}

	@Bean
	public PeerSubscriptionController peerSubscriptionServer() {
		return new PeerSubscriptionController();
	}
	
	@Bean
	public PeerPeersController peerPeersServer() {
		return new PeerPeersController();
	}

	/**
   * Build a client for interacting with other local Acumos components
	 * through the local interface.
	 * We assume the same configuration takes place for client and server
	 * roles when interacting with peers: we'll assume the same identity, use the
	 * same network interface, etc. If this ever needs to change we can pick
	 * the values from a separate configuration properties set.
	 */
	@Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public HttpClient localClient() {
		log.debug(EELFLoggerDelegate.debugLogger, this + "::localClient from " + this.interfaceConfig);
		return interfaceConfig.buildClient();
	}

	/**
	 * Build a servlet container running on the local interface for serving
	 * local interface requests (see controllers built here).
	 */
	@Bean
	public EmbeddedServletContainerCustomizer localServer() {
		log.debug(EELFLoggerDelegate.debugLogger, this + "::localServer from " + this.interfaceConfig);
		return new EmbeddedServletContainerCustomizer() {
			@Override
			public void customize(ConfigurableEmbeddedServletContainer theContainer) {
				LocalConfiguration.this.interfaceConfig.configureContainer(theContainer);
			}
		};
	} 
	
}

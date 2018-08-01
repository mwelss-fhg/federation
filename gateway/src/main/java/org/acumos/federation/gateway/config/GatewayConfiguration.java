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

import org.acumos.federation.gateway.adapter.PeerGateway;
import org.acumos.federation.gateway.common.Clients;
import org.acumos.federation.gateway.security.AuthenticationConfiguration;
import org.acumos.federation.gateway.service.ArtifactService;
import org.acumos.federation.gateway.service.CatalogService;
import org.acumos.federation.gateway.service.PeerService;
import org.acumos.federation.gateway.service.PeerSubscriptionService;
import org.acumos.federation.gateway.service.impl.ArtifactServiceImpl;
import org.acumos.federation.gateway.service.impl.ArtifactServiceLocalImpl;
import org.acumos.federation.gateway.service.impl.CatalogServiceImpl;
import org.acumos.federation.gateway.service.impl.PeerServiceImpl;
import org.acumos.federation.gateway.service.impl.PeerSubscriptionServiceImpl;
import org.acumos.federation.gateway.task.TaskConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;


/**
 * Specifies common configuration required by the federation gateway.
 * Lists/provides all the beans required in running a federation gateway.
 * 
 */
@Configuration
@Import({TaskConfiguration.class,
				 AuthenticationConfiguration.class})
@EnableConfigurationProperties({FederationInterfaceConfiguration.class,
																LocalInterfaceConfiguration.class,
																DockerConfiguration.class})
@Conditional({GatewayCondition.class})
@EnableScheduling
public class GatewayConfiguration {

	@Bean
	public PeerGateway gateway() {
		return new PeerGateway();
	}

	@Bean
	public CatalogService catalogService() {
		return new CatalogServiceImpl();
	}

	@Bean
	public PeerService peerService() {
		return new PeerServiceImpl();
	}
	
	@Bean
	public PeerSubscriptionService peerSubscriptionService() {
		return new PeerSubscriptionServiceImpl();
	}

	/**
	 * The 'local' profile allows us to run a gateway based on a local artifact supplier, for testing purposes.

	 */
	@Bean
	@Profile({"!local"})
	public ArtifactService artifactService() {
		return new ArtifactServiceImpl();
	}

	@Bean
	@Profile({"local"})
	public ArtifactService localArtifactService() {
		return new ArtifactServiceLocalImpl();
	}


	@Bean
	public Clients clients() {
		return new Clients();
	}

}

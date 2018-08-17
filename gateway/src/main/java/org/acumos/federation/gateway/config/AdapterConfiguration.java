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

import org.acumos.federation.gateway.common.Clients;
import org.acumos.federation.gateway.security.AuthenticationConfiguration;
import org.acumos.federation.gateway.service.ContentService;
import org.acumos.federation.gateway.service.CatalogService;
import org.acumos.federation.gateway.service.LocalWatchService;
import org.acumos.federation.gateway.service.PeerService;
import org.acumos.federation.gateway.service.PeerSubscriptionService;
import org.acumos.federation.gateway.service.impl.ContentServiceLocalImpl;
import org.acumos.federation.gateway.service.impl.CatalogServiceLocalImpl;
import org.acumos.federation.gateway.service.impl.PeerServiceLocalImpl;
import org.acumos.federation.gateway.task.TaskConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Specifies common configuration required by the federation adapter.
 * This is not a full configuration as the main component (the actual adapter) is
 * not specified.
 */
@Configuration
@Import({TaskConfiguration.class,
				 AuthenticationConfiguration.class})
@EnableConfigurationProperties({FederationInterfaceConfiguration.class,
																LocalInterfaceConfiguration.class,
																DockerConfiguration.class,
																NexusConfiguration.class})
@Conditional({AdapterCondition.class})
@EnableScheduling
public abstract class AdapterConfiguration  {

	private PeerServiceLocalImpl peerSubSrv = new PeerServiceLocalImpl();

	@Bean
	public CatalogService catalogService() {
		return new CatalogServiceLocalImpl();
	}

	@Bean
	public PeerService peerService() {
		return this.peerSubSrv;
	}
	
	@Bean
	public PeerSubscriptionService peerSubscriptionService() {
		return this.peerSubSrv;
	}

	@Bean
	public ContentService localContentService() {
		return new ContentServiceLocalImpl();
	}

  @Bean
  public LocalWatchService watchService() {
    return new LocalWatchService();
  }

	@Bean
	public Clients clients() {
		return new Clients();
	}

}

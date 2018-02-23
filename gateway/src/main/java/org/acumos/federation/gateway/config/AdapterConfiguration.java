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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.acumos.federation.gateway.service.CatalogService;
import org.acumos.federation.gateway.service.PeerSubscriptionService;
import org.acumos.federation.gateway.service.PeerService;
import org.acumos.federation.gateway.service.LocalWatchService;

import org.acumos.federation.gateway.service.impl.CatalogServiceLocalImpl;
import org.acumos.federation.gateway.service.impl.PeerServiceLocalImpl;
import org.acumos.federation.gateway.common.Clients;

import org.acumos.federation.gateway.task.TaskConfiguration;

/**
 * Specifies common configuration required by the federation adapter.
 * This is not a full configuration as the main component (the actual adapter) is
 * not specified.
 */
@Configuration
//@EnableAutoConfiguration
@Import(TaskConfiguration.class)
@EnableConfigurationProperties({FederationInterfaceConfiguration.class,
																LocalInterfaceConfiguration.class})
//@Profile({"adapter"})
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
	//	return new PeerServiceLocalImpl(); //another instance ??
		return this.peerSubSrv;
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

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

package org.acumos.federation.gateway.task;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;


/**
 * Provides the beans used to setup the peer subscription tasks.
 */
@Configuration
@EnableScheduling
@EnableAutoConfiguration
@ConfigurationProperties(prefix = "task")
public class TaskConfiguration {

	private int poolSize = 20;

	public TaskConfiguration() {
	}

	public void setPoolSize(int thePoolSize) {
		this.poolSize = thePoolSize;
	}

	@Bean
	public TaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.setPoolSize(this.poolSize);
		taskScheduler.setBeanName("gatewayPeerTaskScheduler");
		taskScheduler.initialize();
		return taskScheduler;
	}

	/**
	 */
	@Bean
	public PeerSubscriptionTaskScheduler peerSubscriptionTaskScheduler() {
		return new PeerSubscriptionTaskScheduler();
	}

	/**
	 */
	@Bean
	@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public PeerSubscriptionTask peerSubscriptionTask() {
		return new PeerSubscriptionTask();
	}
}

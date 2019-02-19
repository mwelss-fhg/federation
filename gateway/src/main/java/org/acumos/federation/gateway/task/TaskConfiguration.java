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

import java.util.concurrent.Executor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Provides the beans used to setup the peer subscription tasks.
 */
@Configuration
@EnableAsync
@EnableScheduling
@EnableAutoConfiguration
@ConfigurationProperties(prefix = "task")
public class TaskConfiguration implements AsyncConfigurer {

	private int schedulerPoolSize = 100;
	private int executorCorePoolSize = 20;
	private int executorMaxPoolSize = 100;
	private int executorQueueCapacity = 50;

	public TaskConfiguration() {
	}

	public void setSchedulerPoolSize(int theSize) {
		this.schedulerPoolSize = theSize;
	}

	public void setExecutorCorePoolSize(int theSize) {
		this.executorCorePoolSize = theSize;
	}

	public void setExecutorMaxPoolSize(int theSize) {
		this.executorMaxPoolSize = theSize;
	}

	public void setExecutorQueueCapacity(int theCapacity) {
		this.executorQueueCapacity = theCapacity;
	}

	@Override
	public Executor getAsyncExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(this.executorCorePoolSize);
		executor.setMaxPoolSize(this.executorMaxPoolSize);
		executor.setQueueCapacity(this.executorQueueCapacity);
		executor.setThreadNamePrefix("GatewayExecutor-");
		executor.initialize();
		return executor;
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return null;
	}

	@Bean
	@Qualifier("acumos")
	public TaskScheduler getTaskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(this.schedulerPoolSize);
		scheduler.setThreadNamePrefix("GatewayScheduler-");
		scheduler.initialize();
		return scheduler;
	}

	@Bean
	public PeerSubscriptionTaskScheduler peerSubscriptionTaskScheduler() {
		return new PeerSubscriptionTaskScheduler();
	}

}

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

package org.acumos.federation.gateway;

import java.io.IOException;

import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.config.FederationConfiguration;
import org.acumos.federation.gateway.config.LocalConfiguration;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * Configuration classes are also Conponents so they are subject to Component scanning.
 */
@SpringBootApplication
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class,
		DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class })
@EnableConfigurationProperties
@ComponentScan(basePackages = "org.acumos.federation",
							 useDefaultFilters = false,
							 includeFilters =
								@ComponentScan.Filter(type=FilterType.ASSIGNABLE_TYPE,
																			classes={org.acumos.federation.gateway.config.GatewayConfiguration.class,
																							 org.acumos.federation.gateway.config.AdapterConfiguration.class}))
public class Application {

	private static final EELFLoggerDelegate logger = EELFLoggerDelegate.getLogger(Application.class);

	/**
	 * We should be able to swap the LocalConfiguration in the case of adapters.
	 * 
	 * @param args
	 *            Ignored
	 * @throws IOException
	 *             if environment config cannot be parsed
	 */
	public static void main(String[] args) throws IOException {
		checkEnvironmentConfig();
		SpringApplicationBuilder gatewayBuilder =
			new SpringApplicationBuilder(Application.class)
											.bannerMode(Banner.Mode.OFF)
											.web(false);
		gatewayBuilder.child(FederationConfiguration.class)
											.bannerMode(Banner.Mode.OFF)
											.web(true)
											.run(args);
		gatewayBuilder.child(LocalConfiguration.class)
											.bannerMode(Banner.Mode.OFF)
											.web(true)
											.run(args);

	}

	private static final String CONFIG_ENV_VAR_NAME = "SPRING_APPLICATION_JSON";

	private static void checkEnvironmentConfig() throws IOException {
		final String springApplicationJson = System.getenv(CONFIG_ENV_VAR_NAME);
		if (springApplicationJson != null && springApplicationJson.contains("{")) {
			final ObjectMapper mapper = new ObjectMapper();
			// ensure it's valid
			mapper.readTree(springApplicationJson);
			logger.info("main: successfully parsed configuration from environment {}", CONFIG_ENV_VAR_NAME);
		} else {
			logger.warn("main: no configuration found in environment {}", CONFIG_ENV_VAR_NAME);
		}
	}
}

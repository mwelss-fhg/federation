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

import org.springframework.stereotype.Component;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.github.dockerjava.core.DockerClientConfig; 
import com.github.dockerjava.core.DefaultDockerClientConfig; 

/**
 * 
 */
@Component
@ConfigurationProperties(prefix = "docker")
public class DockerConfiguration {

	private DefaultDockerClientConfig.Builder builder;

	public DockerConfiguration() {
		reset();
	}

	private void reset() {
		this.builder = DefaultDockerClientConfig.createDefaultConfigBuilder();
	}

	public void setHost(String theHost) {
		builder.withDockerHost(theHost);
	}

	public void setApiVersion(String theVersion) {
		builder.withApiVersion(theVersion);
  }

	public void setRegistryUsername(String theUsername) {
		builder.withRegistryUsername(theUsername);
	}

	public void setRegistryPassword(String thePassword) {
		builder.withRegistryPassword(thePassword);
	}

	public void setRegistryEmail(String theEmail) {
		builder.withRegistryEmail(theEmail);
	}

	public void setRegistryUrl(String theUrl) {
		builder.withRegistryUrl(theUrl);
	}

	public void setDockerCertPath(String thePath) {
		builder.withDockerCertPath(thePath);
	}

	public void setDockerConfig(String theConfig) {
		builder.withDockerConfig(theConfig);
	}

	public void setDockerTlsVerify(Boolean doVerify) {
		builder.withDockerTlsVerify(doVerify);
  }

	/*
	public void withCustomSslConfig(SSLConfig customSslConfig) {
	}
	*/

	public DockerClientConfig buildConfig() {
		return this.builder.build();
	}

}

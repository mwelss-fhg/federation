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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

/**
 * 
 */
@Component
@ConfigurationProperties(prefix = "docker")
public class DockerConfiguration {

	private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(MethodHandles.lookup().lookupClass());
	private DefaultDockerClientConfig.Builder builder;
	// need to repeat as the builder does not expose it and it avoids building a config object every time ..
	private String registryUrl;
 
	public DockerConfiguration() {
		reset();
	}

	private void reset() {
		this.builder = DefaultDockerClientConfig.createDefaultConfigBuilder();
		this.registryUrl = null;
	}

	public void setHost(String theHost) {
		this.builder.withDockerHost(theHost);
	}

	public void setApiVersion(String theVersion) {
		this.builder.withApiVersion(theVersion);
  }

	public void setRegistryUsername(String theUsername) {
		this.builder.withRegistryUsername(theUsername);
	}

	public void setRegistryPassword(String thePassword) {
		this.builder.withRegistryPassword(thePassword);
	}

	public void setRegistryEmail(String theEmail) {
		this.builder.withRegistryEmail(theEmail);
	}

	public void setRegistryUrl(String theUrl) {
		this.registryUrl = theUrl;
		this.builder.withRegistryUrl(theUrl);
	}
	
	public String getRegistryUrl() {
		return this.registryUrl;
	}

	public void setDockerCertPath(String thePath) {
		this.builder.withDockerCertPath(thePath);
	}

	public void setDockerConfig(String theConfig) {
		this.builder.withDockerConfig(theConfig);
	}

	public void setDockerTlsVerify(Boolean doVerify) {
		this.builder.withDockerTlsVerify(doVerify);
  }

	/*
	public void withCustomSslConfig(SSLConfig customSslConfig) {
	}
	*/

	public DockerClientConfig buildConfig() {
		return this.builder.build();
	}

	/** */
	public DockerClient	getDockerClient() {
    return DockerClientBuilder.getInstance(buildConfig())
        		.withDockerCmdExecFactory(DockerClientBuilder.getDefaultDockerCmdExecFactory())
        		.build();
	}
}

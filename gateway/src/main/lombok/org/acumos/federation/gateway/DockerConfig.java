/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
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

import lombok.Data;

/**
 * Configuration bean for Docker access parameters.
 * These parameters are passed verbatim to withXXX() methods of the
 * {@link com.github.dockerjava.core.DefaultDockerClientConfig.Builder} class.
 */
@Data
public class DockerConfig {
	/**
	 * The version of the Docker API to use when communicating with the Docker host.
	 * Optional.  Version values should be of the form X.Y where X is the
	 * major version number and Y is the minor version number of the Docker
	 * API protocol.  The Docker API version matrix can be found <a href="https://docs.docker.com/develop/sdk/#api-version-matrix">here</a>.
	 *
	 * @param apiVersion The version.
	 * @return The version.
	 */
	private String apiVersion;
	/*
	 * The URL of the unix or IP socket for accessing the local Docker host.
	 *
	 * Must be in the form tcp://hostname:port or unix://path.
	 *
	 * @param host The socket URL.
	 * @return The socket URL.
	 */
	private String host;
	/**
	 * Whether to use TLS encryption when connecting to the local Docker host.
	 *
	 * @param tlsVerify Whether to use encryption.
	 * @return Whether to use encryption.
	 */
	private Boolean tlsVerify;
	/**
	 * The path to the directory containing the PEM files for using TLS.
	 *
	 * The expected files are ca.pem, key.pem, and cert.pem.
	 *
	 * @param dockerCertPath The path to the directory.
	 * @return The path to the directory.
	 */
	private String dockerCertPath;
	/**
	 * The path to the directory containing the Docker configuration file.
	 *
	 * The name of the file, in the directory, is config.json.
	 *
	 * @param dockerConfig The path to the directory.
	 * @return The path to the directory.
	 */
	private String dockerConfig;
	/**
	 * The hostport for accessing the Docker registry in the form hostname:port.
	 *
	 * @param registryUrl The hostname:port value.
	 * @return The hostname:port value.
	 */
	private String registryUrl;
	/**
	 * The user name for authenticating to the Docker registry.
	 *
	 * @param registryUsername The user name.
	 * @return The user name.
	 */
	private String registryUsername;
	/**
	 * The password for authenticating to the Docker registry.
	 *
	 * @param registryPassword The password.
	 * @return The password.
	 */
	private String registryPassword;
	/**
	 * The email address for authenticating to the Docker registry.
	 *
	 * @param registryEmail The email address.
	 * @return The email address.
	 */
	private String registryEmail;
}

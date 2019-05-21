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
package org.acumos.federation.client.config;

import lombok.Data;

/**
 * Configuration for a TLS (https) client.
 */

@Data
public class TlsConfig {
	/**
	 * Key store location for 2-way authentication.
	 * If null, client side certificates are disabled.
	 *
	 * This value is interpreted by the
	 * {@link org.springframework.core.io.ResourceLoader#getResource(String)}
	 * method of whatever resource loader is specified when the client
	 * is created.  If that method cannot locate it, an attempt is made
	 * to interpret the value as a file name.
	 *
	 * @param keyStore The location.
	 * @return The location.
	 */
	private String keyStore;
	/**
	 * The type of the key store (default "JKS").
	 *
	 * This value is interpreted by
	 * {@link java.security.KeyStore#getInstance(String)}.
	 * The most commonly used values are "JKS" and "PKCS12" but
	 * Java security providers can and do handle
	 * many other types of key stores and there is no list of valid
	 * values.
	 *
	 * @param keyStoreType The type.
	 * @return The type.
	 */
	private String keyStoreType = "JKS";
	/**
	 * The password for the key store.
	 *
	 * @param keyStorePassword The password.
	 * @return The password.
	 */
	private String keyStorePassword;
	/**
	 * The alias of the entry in the key store to use.
	 *
	 * @param keyAlias The alias name.
	 * @return The alias name.
	 */
	private String keyAlias;
	/**
	 * Trust store location for validating server certificate.
	 * If null, the default trust store is used.
	 *
	 * This value is interpreted by the
	 * {@link org.springframework.core.io.ResourceLoader#getResource(String)}
	 * method of whatever resource loader is specified when the client
	 * is created.  If that method cannot locate it, an attempt is made
	 * to interpret the value as a file name.
	 *
	 * @param trustStore The location.
	 * @return The location.
	 */
	private String trustStore;
	/**
	 * The type of the trust store (default "JKS").
	 *
	 * This value is interpreted by
	 * {@link java.security.KeyStore#getInstance(String)}.
	 * The most commonly used values are "JKS" and "PKCS12" but
	 * Java security providers can and do handle
	 * many other types of key stores and there is no list of valid
	 * values.
	 *
	 * @param trustStoreType The type.
	 * @return The type.
	 */
	private String trustStoreType = "JKS";
	/**
	 * The password for the trust store.
	 * If null, the default trust store is used.
	 *
	 * @param trustStorePassword The password.
	 * @return The password.
	 */
	private String trustStorePassword;
}

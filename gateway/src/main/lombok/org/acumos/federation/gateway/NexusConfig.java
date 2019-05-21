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
import lombok.EqualsAndHashCode;

/**
 * Configuration bean for Nexus server access parameters.
 */
@Data
@EqualsAndHashCode(callSuper=true)
public class NexusConfig extends ServiceConfig {
	/**
	 * The name separator for creating URIs.
	 *
	 * @param nameSeparator The name separator.
	 * @return The name separator.
	 */
	private String nameSeparator = ".";
	/**
	 * The group ID for creating URIs.
	 *
	 * @param groupId The group ID.
	 * @return The group ID.
	 */
	private String groupId;
}

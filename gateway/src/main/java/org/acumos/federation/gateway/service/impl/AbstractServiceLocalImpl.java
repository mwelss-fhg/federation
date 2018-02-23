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

package org.acumos.federation.gateway.service.impl;

import org.springframework.core.io.Resource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.beans.factory.annotation.Autowired;

import org.acumos.federation.gateway.service.LocalWatchService;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;

import org.apache.commons.io.IOUtils;

public class AbstractServiceLocalImpl {

	protected EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(getClass().getName());
	protected Resource resource;

	@Autowired
	protected ApplicationContext appCtx;

	@Autowired
	protected LocalWatchService watcher;

	public void setSource(String theSource) {
		this.resource = this.appCtx.getResource(theSource);
	}

	protected void checkResource() {

		if (this.resource == null) {
			throw new BeanInitializationException("No source was configured");
		}

		if (!this.resource.exists()) {
			throw new BeanInitializationException("Source " + this.resource + " does not exist");
		}
	}

}

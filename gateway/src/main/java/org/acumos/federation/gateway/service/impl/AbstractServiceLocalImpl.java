/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2017-2019 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
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

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URISyntaxException;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.acumos.federation.gateway.cds.Mapper;
import org.acumos.federation.gateway.security.Peer;
import org.acumos.federation.gateway.service.LocalWatchService;
import org.acumos.federation.gateway.service.ServiceContext;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;

public class AbstractServiceLocalImpl {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	protected Resource resource;

	@Autowired
	protected ApplicationContext appCtx;

	@Autowired
	protected LocalWatchService watcher;

	protected synchronized <T> void reload(Class<T> clazz, Resource resource, Consumer<List<T>> setter, String label) {
		try {
			ObjectReader objectReader = Mapper.build().reader(clazz);
			MappingIterator objectIterator = objectReader.readValues(resource.getURL());
			List<T> values = objectIterator.readAll();
			setter.accept(values);
			log.info("loaded {} {}", values.size(), label);
		} catch (Exception x) {
			throw new BeanInitializationException("Failed to load " + label + " from " + resource, x);
		}
	}

	protected <T> void monitor(Class<T> clazz, Resource resource, Consumer<List<T>> setter, String label) {
		if (resource == null) {
			throw new BeanInitializationException("No source for " + label + " was configured");
		}
		if (!resource.exists()) {
			throw new BeanInitializationException("Source " + resource + " for " + label + " does not exist");
		}
		try {
			watcher.watchOn(resource.getURL().toURI(), (uri) -> reload(clazz, resource, setter, label));
		} catch (IOException | URISyntaxException iox) {
			log.info("Failed to register {} watcher for {}", label, resource);
		}
		reload(clazz, resource, setter, label);
	}

	public void setSource(String theSource) {
		this.resource = this.appCtx.getResource(theSource);
	}

	public ServiceContext selfService() {
		return ServiceContext.forPeer((Peer)appCtx.getBean("self"));		
	}
}

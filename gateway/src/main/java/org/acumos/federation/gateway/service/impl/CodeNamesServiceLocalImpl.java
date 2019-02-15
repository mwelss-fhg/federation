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

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.acumos.cds.CodeNameType;
import org.acumos.cds.domain.MLPCodeNamePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.acumos.federation.gateway.service.CodeNamesService;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.service.ServiceException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * 
 *
 */
@Service
@ConfigurationProperties(prefix = "codes-local")
public class CodeNamesServiceLocalImpl extends AbstractServiceLocalImpl implements CodeNamesService {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private Map<CodeNameType, List<MLPCodeNamePair>> codes;

	@PostConstruct
	public void initService() {

		checkResource();
		try {
			watcher.watchOn(this.resource.getURL().toURI(), (uri) -> {
				loadCodeNamesInfo();
			});

		} catch (IOException | URISyntaxException iox) {
			log.info("Code names watcher registration failed for " + this.resource, iox);
		}

		loadCodeNamesInfo();

		// Done
		log.debug("Local CodeNamesService available");
	}

	@PreDestroy
	public void cleanupService() {
	}

	/** */
	private void loadCodeNamesInfo() {
		synchronized (this) {
			try {
 				ObjectMapper mapper = new ObjectMapper()
																			.registerModule(new JavaTimeModule());
				this.codes = mapper.readValue(this.resource.getURL(), new TypeReference<Map<CodeNameType, List<MLPCodeNamePair>>>(){});
				log.info("loaded " + this.codes.size() + " codes");
			}
			catch (Exception x) {
				throw new BeanInitializationException("Failed to load codes from " + this.resource, x);
			}
		}
	}


	@Override
	public List<MLPCodeNamePair> getCodeNames(CodeNameType theType) throws ServiceException {

		log.debug("getCodeNames");
		return this.codes.getOrDefault(theType, Collections.EMPTY_LIST);
	}
}

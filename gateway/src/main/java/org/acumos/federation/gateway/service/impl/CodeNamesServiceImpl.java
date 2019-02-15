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

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.acumos.cds.CodeNameType;
import org.acumos.cds.domain.MLPCodeNamePair;
import org.acumos.federation.gateway.service.CodeNamesService;
import org.acumos.federation.gateway.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * CDS based implementation of CodeNamesService.
 */
@Service
public class CodeNamesServiceImpl extends AbstractServiceImpl implements CodeNamesService {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public CodeNamesServiceImpl() {
	}

	@Override
	public List<MLPCodeNamePair> getCodeNames(CodeNameType theType) throws ServiceException {

		log.debug("Loading codes of type: {}", theType);
		List<MLPCodeNamePair> codes = null;
		try {
			codes = getClient().getCodeNamePairs(theType);
		}
		catch(Exception x) {
			log.error("Failed to load codes for " + theType, x); 
			throw new ServiceException("Failed to load codes for " + theType , x);
		}
		log.debug("Loaded codes of type {}: {}", theType, codes.size());
		return codes;
	}

}

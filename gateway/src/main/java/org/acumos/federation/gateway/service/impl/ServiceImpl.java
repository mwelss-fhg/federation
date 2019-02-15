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
import java.util.Map;
import java.util.Set;

import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some basic tooling for service implementation.
 * Common functionality to be re-used across service implementations.
 */
public abstract class ServiceImpl {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private ServiceImpl() {
	}

	/**
	 * Bit of a primitive implementation
	 * @param theSolution solution
	 * @param theSelector selector
	 * @return Boolean
	 */
	public static boolean isSelectable(MLPSolution theSolution, Map<String, ?> theSelector) /*throws ServiceException*/ {
		boolean res = true;

		log.trace("isSelectable {}", theSolution);
		if (theSelector == null || theSelector.isEmpty())
			return true;

		Object solutionId = theSelector.get("solutionId");
		if (solutionId != null) {
			log.trace("using solutionId based selection {}", solutionId);
			if (solutionId instanceof String) {
				res &= theSolution.getSolutionId().equals(solutionId);
			}
			else {
				log.debug("unknown solutionId criteria representation {}", solutionId.getClass().getName());
				return false;
			}
		}

		Object modelTypeCode = theSelector.get("modelTypeCode");
		if (modelTypeCode != null) {
			log.trace("using modelTypeCode based selection {}", modelTypeCode);
			String solutionModelTypeCode = theSolution.getModelTypeCode();
			if (solutionModelTypeCode == null) {
				return false;
			}
			else {
				if (modelTypeCode instanceof String) {
					res &= solutionModelTypeCode.equals(modelTypeCode);
				}
				else if (modelTypeCode instanceof List) {
					res &= ((List)modelTypeCode).contains(solutionModelTypeCode);
				}
				else {
					log.debug("unknown modelTypeCode criteria representation {}", modelTypeCode.getClass().getName());
					return false;
				}
			}
		}

		Object toolkitTypeCode = theSelector.get("toolkitTypeCode");
		if (toolkitTypeCode != null) {
			log.trace("using toolkitTypeCode based selection {}", toolkitTypeCode);
			String solutionToolkitTypeCode = theSolution.getToolkitTypeCode();
			if (solutionToolkitTypeCode == null) {
				return false;
			}
			else {
				if (toolkitTypeCode instanceof String) {
					res &= solutionToolkitTypeCode.equals(toolkitTypeCode);
				}
				else if (toolkitTypeCode instanceof List) {
					res &= ((List)toolkitTypeCode).contains(solutionToolkitTypeCode);
				}
				else {
					log.debug("unknown toolkitTypeCode criteria representation {}", toolkitTypeCode.getClass().getName());
					return false;
				}
			}
		}

		Object tags = theSelector.get("tags");
		if (tags != null) {
			log.trace("using tags based selection {}", tags);
			Set<MLPTag> solutionTags = theSolution.getTags();
			if (solutionTags == null) {
				return false;
			}
			else {
				if (tags instanceof String) {
					res &= solutionTags.stream().filter(solutionTag -> tags.equals(solutionTag.getTag())).findAny().isPresent();
				}
				else if (tags instanceof List) {
					res &= solutionTags.stream().filter(solutionTag -> ((List)tags).contains(solutionTag.getTag())).findAny().isPresent();
				}
				else {
					log.debug("unknown tags criteria representation {}", tags.getClass().getName());
					return false; 
				}
			}
		}

		Object name = theSelector.get("name");
		if (name != null) {
			log.debug("using name based selection {}", name);
			String solutionName = theSolution.getName();
			if (solutionName == null) {
				return false;
			}
			else {
				res &= solutionName.contains(name.toString());
			}
		}

		return res;
	}


}

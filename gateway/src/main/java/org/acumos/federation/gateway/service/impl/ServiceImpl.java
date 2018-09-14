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
import org.acumos.federation.gateway.config.EELFLoggerDelegate;

/**
 * Some basic tooling for service implementation.
 * Common functionality to be re-used across service implementations.
 */
public abstract class ServiceImpl {

	private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(MethodHandles.lookup().lookupClass());

	private ServiceImpl() {
	}

	/**
	 * Bit of a primitive implementation
	 */
	public static boolean isSelectable(MLPSolution theSolution, Map<String, ?> theSelector) /*throws ServiceException*/ {
		boolean res = true;

		if (theSelector == null || theSelector.isEmpty())
			return true;

		Object solutionId = theSelector.get("solutionId");
		if (solutionId != null) {
			if (solutionId instanceof String) {
				res &= theSolution.getSolutionId().equals(solutionId);
			}
			else {
				log.debug(EELFLoggerDelegate.debugLogger, "unknown solutionId criteria representation {}", solutionId.getClass().getName());
				return false;
			}
		}

		Object modelTypeCode = theSelector.get("modelTypeCode");
		if (modelTypeCode != null) {
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
					log.debug(EELFLoggerDelegate.debugLogger, "unknown modelTypeCode criteria representation {}", modelTypeCode.getClass().getName());
					return false;
				}
			}
		}

		Object toolkitTypeCode = theSelector.get("toolkitTypeCode");
		if (toolkitTypeCode != null) {
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
					log.debug(EELFLoggerDelegate.debugLogger, "unknown toolkitTypeCode criteria representation {}", toolkitTypeCode.getClass().getName());
					return false;
				}
			}
		}

		Object tags = theSelector.get("tags");
		if (tags != null) {
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
					log.debug(EELFLoggerDelegate.debugLogger, "unknown tags criteria representation {}", tags.getClass().getName());
					return false; 
				}
			}
		}

		Object name = theSelector.get("name");
		if (name != null) {
			String solutionName = theSolution.getName();
			if (solutionName == null) {
				return false;
			}
			else {
				res &= solutionName.contains(name.toString());
			}
		}

		Object desc = theSelector.get("description");
		if (desc != null) {
			String solutionDesc = theSolution.getDescription();
			if (solutionDesc == null) {
				return false;
			}
			else {
				res &= solutionDesc.contains(desc.toString());
			}
		}

		return res;
	}


}

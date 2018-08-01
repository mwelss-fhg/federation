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

import java.util.List;
import java.util.Map;

import org.acumos.cds.domain.MLPSolution;

/**
 * Some basic tooling for service implementation.
 * Common functionality to be re-used across service implementations.
 */
public interface ServiceImpl {

	/**
	 * Bit of a primitive implementation
	 */
	public static boolean isSelectable(MLPSolution theSolution, Map<String, ?> theSelector) /*throws ServiceException*/ {
		boolean res = true;

		if (theSelector == null || theSelector.isEmpty())
			return true;

		Object modelTypeCode = theSelector.get("modelTypeCode");
		if (modelTypeCode != null) {
			if (modelTypeCode instanceof String) {
				res &= theSolution.getModelTypeCode().equals(modelTypeCode);
			}
			else if (modelTypeCode instanceof List) {
				res &= ((List)modelTypeCode).contains(theSolution.getModelTypeCode());
			}
			else
				res = false;
		}

		Object toolkitTypeCode = theSelector.get("toolkitTypeCode");
		if (toolkitTypeCode != null) {
			if (toolkitTypeCode instanceof String) {
				res &= theSolution.getToolkitTypeCode().equals(toolkitTypeCode);
			}
			else if (toolkitTypeCode instanceof List) {
				res &= ((List)toolkitTypeCode).contains(theSolution.getToolkitTypeCode());
			}
			else
				res = false;
		}

		return res;
	}


}

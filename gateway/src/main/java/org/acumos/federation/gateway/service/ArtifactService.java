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

/**
 * 
 */
package org.acumos.federation.gateway.service;

import org.acumos.cds.domain.MLPArtifact;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

/**
 * Handles access to the artifacts repository. 
 */
public interface ArtifactService {

	/**
	 * @param theArtifact
	 *            The CDS representation of artifact metadata
	 * @param theContext
	 *            the execution context
	 * @return resource containing access to the actual artifact content
	 */
	public InputStreamResource getArtifactContent(MLPArtifact theArtifact, ServiceContext theContext)
																																										throws ServiceException;


	/**
	 * If the call is succesful the artifact information is updated with the content uri.
	 * No service context here as this call is always used with respect to the local gateway instance.
	 * @param theArtifact
	 *            The CDS representation of artifact metadata
	 * @param theResource
	 *            the resource providing the artifact content
	 */
	public void putArtifactContent(MLPArtifact theArtifact, Resource theResource) throws ServiceException;
}

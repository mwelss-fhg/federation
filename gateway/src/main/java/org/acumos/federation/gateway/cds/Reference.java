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
package org.acumos.federation.gateway.cds;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.io.FilenameUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Capable of pointing to some (external) content. Contains the content location as an uri.
 */
public interface Reference {

	public String getUri();

	public void setUri(String theUri);

	@JsonIgnore
	public default String getUriFilename() {
		String curi = getUri();
		if (curi == null)
			return null;
		try {
			return FilenameUtils.getName(new URI(curi).getPath());
		}
		catch (URISyntaxException urisx) {
			//let's do our worst; this works in a UX env because it employs the same path separator
			//as uris.
			return FilenameUtils.getName(curi);
		}
	}

}


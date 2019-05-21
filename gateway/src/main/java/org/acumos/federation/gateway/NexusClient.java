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

import java.io.InputStream;

import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPDocument;

import org.acumos.federation.client.ClientBase;
import org.acumos.federation.client.config.ClientConfig;

/**
 * Client for accessing the Nexus server.
 */
public class NexusClient extends ClientBase {
	/**
	 * Create a nexus client.
	 *
	 * @param url URL for accessing the Nexus server.
	 * @param cc Credentials and TLS parameters for mutual authentication.
	 */
	public NexusClient(String url, ClientConfig cc) {
		super(url, cc, null, null);
	}

	/**
	 * Get a document from the Nexus server.
	 *
	 * @param document The document to fetch.
	 * @return An inputstream for reading the document's content.
	 */
	public InputStream getDocumentContent(MLPDocument document) {
		return download("/" + document.getUri());
	}

	/**
	 * Put a document to the Nexus server.
	 *
	 * @param document The document to save.
	 * @param is The data.
	 */
	public void putDocumentContent(MLPDocument document, InputStream is) {
		upload("/" + document.getUri(), is);
	}

	/**
	 * Get an artifact from the Nexus server.
	 *
	 * @param artifact The artifact to fetch.
	 * @return An inputstream for reading the artifact's content.
	 */
	public InputStream getArtifactContent(MLPArtifact artifact) {
		return download("/" + artifact.getUri());
	}

	/**
	 * Put a artifact to the Nexus server.
	 *
	 * @param artifact The artifact to save.
	 * @param is The data.
	 */
	public void putArtifactContent(MLPArtifact artifact, InputStream is) {
		upload("/" + artifact.getUri(), is);
	}
}

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

import org.acumos.cds.domain.MLPArtifact;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.dockerjava.api.model.Identifier;

/**
 */
public class Artifact extends MLPArtifact
											implements Reference, TimestampedEntity {

	private String filename;

	public Artifact() {
	}

	public Artifact(MLPArtifact theCDSArtifact) {
		super(theCDSArtifact);
	}
	
	public Artifact(Artifact theArtifact) {
		super(theArtifact);
		this.filename = theArtifact.getFilename();
	}

	public static ArtifactBuilder build() {
		return new ArtifactBuilder(new Artifact());
	}

	public static ArtifactBuilder buildFrom(Artifact theArtifact) {
		return new ArtifactBuilder(new Artifact(theArtifact));
	}

	public void setFilename(String theFilename) {
		this.filename = theFilename;
	}

	public String getFilename() {
		return this.filename;
	}

	@JsonIgnore
	@Override
	public String getUriFilename() {
		if (ArtifactTypes.DockerImage == ArtifactTypes.forCode(getArtifactTypeCode())) {
			return Identifier.fromCompoundString(getUri()).repository.getPath();
		}
		else {
			return Reference.super.getUriFilename();
		}
	}

}


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

import java.util.Date;
import java.util.List;

/**
 */
public class ArtifactBuilder {

	private Artifact artifact;

	protected ArtifactBuilder(Artifact theArtifact) {
		this.artifact = theArtifact;
	}

	public Artifact build() {
		return this.artifact;
	} 

	public ArtifactBuilder withCreatedDate(Date theDate) {
		this.artifact.setCreated(theDate);
		return this;
	}

	public ArtifactBuilder withModifiedDate(Date theDate) {
		this.artifact.setModified(theDate);
		return this;
	}

	public ArtifactBuilder withId(String theArtifactId) {
		this.artifact.setArtifactId(theArtifactId);
		return this;
	}

	public ArtifactBuilder withOwner(String theOwnerId) {
		this.artifact.setOwnerId(theOwnerId);
		return this;
	}

	public ArtifactBuilder withVersion(String theVersion) {
		this.artifact.setVersion(theVersion);
		return this;
	}

	public ArtifactBuilder withTypeCode(String theTypeCode) {
		this.artifact.setArtifactTypeCode(theTypeCode);
		return this;
	}

	public ArtifactBuilder withName(String theName) {
		this.artifact.setName(theName);
		return this;
	}

	public ArtifactBuilder withDescription(String theDesc) {
		this.artifact.setDescription(theDesc);
		return this;
	}

	public ArtifactBuilder withMetadata(String theMetadata) {
		this.artifact.setMetadata(theMetadata);
		return this;
	}

	public ArtifactBuilder withUri(String theUri) {
		this.artifact.setUri(theUri);
		return this;
	}
	
	public ArtifactBuilder withSize(Integer theSize) {
		this.artifact.setSize(theSize);
		return this;
	}

}



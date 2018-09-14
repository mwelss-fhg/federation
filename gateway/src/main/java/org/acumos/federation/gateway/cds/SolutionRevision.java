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

import java.util.Collections;
import java.util.List;

import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPDocument;
import org.acumos.cds.domain.MLPRevisionDescription;
import org.acumos.cds.domain.MLPSolutionRevision;

/**
 * Supplements the CDS representation of a solution revision with related information: artifacts and (public) documents.
 * Allows federation to pack information passed between peers.
 */
public class SolutionRevision extends MLPSolutionRevision {

	public static interface Fields {
		public static final String accessTypeCode = "accessTypeCode";
		public static final String validationStatusCode = "validationStatusCode";
	};

	private List<? extends MLPArtifact>		artifacts = Collections.EMPTY_LIST;
	private List<? extends MLPDocument>		documents = Collections.EMPTY_LIST;
	private	MLPRevisionDescription				description;

	public SolutionRevision() {
	}

	public SolutionRevision(MLPSolutionRevision theCDSRevision) {
		super(theCDSRevision);
	}

	public void setArtifacts(List<? extends MLPArtifact> theArtifacts) {
		this.artifacts = theArtifacts;
	}

	public List<? extends MLPArtifact>	getArtifacts() {
		return this.artifacts;
	}

	public void setDocuments(List<? extends MLPDocument> theDocuments) {
		this.documents = theDocuments;
	}

	public List<? extends MLPDocument>	getDocuments() {
		return this.documents;
	}

	public void setRevisionDescription(MLPRevisionDescription theDescription) {
		this.description = theDescription;	
	}

	public MLPRevisionDescription getRevisionDescription() {
		return this.description;
	}

	public static SolutionRevisionBuilder build() {
		return new SolutionRevisionBuilder(new SolutionRevision());
	}

	public static SolutionRevisionBuilder buildFrom(MLPSolutionRevision theRevision) {
		return new SolutionRevisionBuilder(new SolutionRevision(theRevision));
	}

}



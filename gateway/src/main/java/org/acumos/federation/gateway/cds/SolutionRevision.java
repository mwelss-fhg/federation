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

import java.util.List;

import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.cds.domain.MLPArtifact;

/**
 * Supplements the CDS representation of a solution with related information: revisions.
 * Allows federation to pack information passed between peers.
 */
public class SolutionRevision extends MLPSolutionRevision {

	public static interface Fields {
		public static final String accessTypeCode = "accessTypeCode";
		public static final String validationStatusCode = "validationStatusCode";
	};

	private List<? extends MLPArtifact>		artifacts;

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

	public static SolutionRevisionBuilder build() {
		return new SolutionRevisionBuilder(new SolutionRevision());
	}

	public static SolutionRevisionBuilder buildFrom(MLPSolutionRevision theRevision) {
		return new SolutionRevisionBuilder(new SolutionRevision(theRevision));
	}

}



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

import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;

/**
 * Supplements the CDS representation of a solution with related information: revisions.
 * Allows federation to pack information passed between peers.
 */
public class Solution extends MLPSolution {

	/**
	 * For the purpose of defining constants for search criteria.
	 * This exposes CDS maintained information so it has to be kept in sync.
	 * We only define those of interest for searching.
	 */
	public static interface Fields {
		public static final String solutionId = "solutionId";
		public static final String name = "name";
		public static final String active = "active";
		public static final String modelTypeCode = "modelTypeCode";
		public static final String toolkitTypeCode = "toolkitTypeCode";
		public static final String accessTypeCode = "accessTypeCode";
		public static final String validationStatusCode = "validationStatusCode";
		public static final String modified = "modified";
		public static final String sourceId = "sourceId";
	};

	private List<MLPSolutionRevision>		revisions;

	public Solution() {
	}

	public Solution(MLPSolution theCDSSolution) {
		super(theCDSSolution);
	}

	public void setRevisions(List<MLPSolutionRevision> theRevisions) {
		this.revisions = theRevisions;
	}

	public List<MLPSolutionRevision>	getRevisions() {
		return this.revisions;
	}
	
	public static SolutionBuilder build() {
		return new SolutionBuilder(new Solution());
	}

	public static SolutionBuilder buildFrom(MLPSolution theSolution) {
		return new SolutionBuilder(new Solution(theSolution));
	}

	@Override
	public String toString() {
		return super.toString() + (this.revisions == null ? "[]" : this.revisions.toString()) ;
	}
}



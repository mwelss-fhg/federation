/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2017-2019 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
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

import java.time.Instant;

public class SolutionRevisionBuilder {

	private SolutionRevision revision;

	protected SolutionRevisionBuilder(SolutionRevision theRevision) {
		this.revision = theRevision;
	}

	public SolutionRevision build() {
		return this.revision;
	}

	public SolutionRevisionBuilder withCreated(Instant theInstant) {
		this.revision.setCreated(theInstant);
		return this;
	}

	public SolutionRevisionBuilder withModified(Instant theInstant) {
		this.revision.setModified(theInstant);
		return this;
	}

	public SolutionRevisionBuilder withRevisionId(String theRevisionId) {
		this.revision.setRevisionId(theRevisionId);
		return this;
	}

	public SolutionRevisionBuilder withMetadata(String theMetadata) {
		this.revision.setMetadata(theMetadata);
		return this;
	}

	public SolutionRevisionBuilder withVersion(String theVersion) {
		this.revision.setVersion(theVersion);
		return this;
	}

	public SolutionRevisionBuilder withVerifiedLicense(String theCode) {
		this.revision.setVerifiedLicense(theCode);
		return this;
	}

	public SolutionRevisionBuilder withVerifiedVulnerability(String theCode) {
		this.revision.setVerifiedVulnerability(theCode);
		return this;
	}

	public SolutionRevisionBuilder withOrigin(String theOrigin) {
		this.revision.setOrigin(theOrigin);
		return this;
	}

	public SolutionRevisionBuilder withUser(String theUserId) {
		this.revision.setUserId(theUserId);
		return this;
	}

	public SolutionRevisionBuilder withSource(String theSourceId) {
		this.revision.setSourceId(theSourceId);
		return this;
	}

	public SolutionRevisionBuilder forSolution(String theSolutionId) {
		this.revision.setSolutionId(theSolutionId);
		return this;
	}
}



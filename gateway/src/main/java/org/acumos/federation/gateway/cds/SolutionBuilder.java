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

import java.time.Instant;

import java.util.Set;

import org.acumos.cds.domain.MLPTag;

/**
 * Supplements the CDS representation of a solution with related information: revisions.
 * Allows federation to pack information passed between peers.
 */
public class SolutionBuilder {

	private Solution solution;

	protected SolutionBuilder(Solution theSolution) {
		this.solution = theSolution;
	}

	public Solution build() {
		return this.solution;
	} 

	public SolutionBuilder withCreated(Instant theInstant) {
		this.solution.setCreated(theInstant);
		return this;
	}

	public SolutionBuilder withModified(Instant theInstant) {
		this.solution.setModified(theInstant);
		return this;
	}

	public SolutionBuilder withId(String theSolutionId) {
		this.solution.setSolutionId(theSolutionId);
		return this;
	}

	public SolutionBuilder withName(String theName) {
		this.solution.setName(theName);
		return this;
	}

	public SolutionBuilder withMetadata(String theMetadata) {
		this.solution.setMetadata(theMetadata);
		return this;
	}

	public SolutionBuilder withActive(boolean isActive) {
		this.solution.setActive(isActive);
		return this;
	}

	public SolutionBuilder withModelTypeCode(String theCode) {
		this.solution.setModelTypeCode(theCode);
		return this;
	}

	public SolutionBuilder withToolkitTypeCode(String theCode) {
		this.solution.setToolkitTypeCode(theCode);
		return this;
	}

	public SolutionBuilder withOrigin(String theOrigin) {
		this.solution.setOrigin(theOrigin);
		return this;
	}

	public SolutionBuilder withUser(String theUserId) {
		this.solution.setUserId(theUserId);
		return this;
	}
	
	public SolutionBuilder withUser(Updater<String, Object> theUpdater, Object... theArgs) {
		String newUser = theUpdater.update(theArgs);
		if (newUser != null)
			this.solution.setUserId(newUser);
		return this;
	}

	public SolutionBuilder withSource(String theSourceId) {
		this.solution.setSourceId(theSourceId);
		return this;
	}

	public SolutionBuilder withSource(Updater<String, Object> theUpdater, Object... theArgs) {
		String newSource = theUpdater.update(theArgs);
		if (newSource != null)
			this.solution.setSourceId(newSource);
		return this;
	}

	public SolutionBuilder withTags(Set<MLPTag> theTags) {
		this.solution.setTags(theTags);
		return this;
	}

	public SolutionBuilder resetStats() {
		this.solution.setViewCount(0L);
		this.solution.setDownloadCount(0L);
		this.solution.setLastDownload(null);
		this.solution.setRatingCount(0L);
		this.solution.setRatingAverageTenths(0L);
		this.solution.setFeatured(false);
		return this;
	}
}



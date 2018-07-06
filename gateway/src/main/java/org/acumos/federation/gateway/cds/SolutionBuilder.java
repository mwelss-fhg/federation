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

	public SolutionBuilder withCreatedDate(Date theDate) {
		this.solution.setCreated(theDate);
		return this;
	}

	public SolutionBuilder withModifiedDate(Date theDate) {
		this.solution.setModified(theDate);
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

	public SolutionBuilder withProvider(String theProvider) {
		this.solution.setProvider(theProvider);
		return this;
	}

	public SolutionBuilder withDescription(String theDesc) {
		this.solution.setDescription(theDesc);
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

	public SolutionBuilder withOwner(String theOwnerId) {
		this.solution.setOwnerId(theOwnerId);
		return this;
	}
	
	public SolutionBuilder withOwner(Updater<String, Object> theUpdater, Object... theArgs) {
		String newOwner = theUpdater.update(theArgs);
		if (newOwner != null)
			this.solution.setOwnerId(newOwner);
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

}



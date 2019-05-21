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
package org.acumos.federation.client.data;

import java.util.Collections;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPDocument;
import org.acumos.cds.domain.MLPRevCatDescription;
import org.acumos.cds.domain.MLPSolutionRevision;

/**
 * Revision enhanced with artifacts, documents, and description.
 */

@Data
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true, includeFieldNames=true)
public class SolutionRevision extends MLPSolutionRevision {
	/**
	 * The artifacts for the revision.
	 *
	 * @param artifacts The artifacts.
	 * @return The artifacts.
	 */
	private List<MLPArtifact> artifacts = Collections.emptyList();
	/**
	 * The documents for the revision.
	 *
	 * @param documents The documents for the revision for the current catalog.
	 * @return The documents for the revision for the current catalog.
	 */
	private List<MLPDocument> documents = Collections.emptyList();
	/**
	 * The description of the revision.
	 *
	 * @param revCatDescription The description of the revision for the current catalog.
	 * @return The description of the revision for the current catalog.
	 */
	private MLPRevCatDescription revCatDescription;
}

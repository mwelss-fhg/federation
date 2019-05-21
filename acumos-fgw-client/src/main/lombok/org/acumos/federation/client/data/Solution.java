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

import java.util.List;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Solution enhanced to add revisions and picture.
 */
@Data
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true, includeFieldNames=true)
public class Solution extends MLPSolution {
	/**
	 * The revisions of this solution.
	 *
	 * @param revisions The revisions.
	 * @return The revisions.
	 */
	private List<MLPSolutionRevision> revisions;
	/**
	 * The picture for this solution.
	 *
	 * @param picture The picture.
	 * @return The picture.
	 */
	private byte[] picture;
}

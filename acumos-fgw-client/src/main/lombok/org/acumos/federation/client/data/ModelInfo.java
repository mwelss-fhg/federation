/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2020 Nordix Foundation
 * ===================================================================================
 * This Acumos software file is distributed by Nordix Foundation
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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;


/**
 * Document enhanced with file name.
 */
@Data
@ToString(callSuper=true, includeFieldNames=true)
public class ModelInfo {
	/**
	 * CDS model id - persistent across all versions of the model
	 * 
	 * @param solutionId the primary identification in CDS for the model
	 * @return the primary identification in CDS for the model
	 */
	private String solutionId;

	/**
	 * CDS GUID for the revision of the model.
	 * 
	 * @return the unique CDS id for the version
	 * @param revisionId the unique CDS id for the version
	 */
	private String revisionId;

	/**
	 * Subject name of the peer that is sending parameter updates.
	 * @param subscriberName the name of the subscriber/peer host
	 * @return the name of the subscriber/peer host
	 */
	private String subscriberName;

}

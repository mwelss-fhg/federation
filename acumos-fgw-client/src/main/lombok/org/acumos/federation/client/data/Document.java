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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.acumos.cds.domain.MLPDocument;

/**
 * Document enhanced with file name.
 */
@Data
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true, includeFieldNames=true)
public class Document extends MLPDocument {
	/**
	 * The "original" file name of the document, without any storage path
	 * information.  E.g. Description.pdf or GreatestSolutionEver.docx.
	 *
	 * @param filename New value for the file name.
	 * @return The current value of the file name.
	 */
	private String filename;
}

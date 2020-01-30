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
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Model data is used to send data back to the supplier of the model
 * This data can include parameter data or usage data.
 */
@Data
@ToString(callSuper = true, includeFieldNames = true)
public class ModelData {
	/**
	 * Example value
	 * 
	 * <pre>
	 *  {@code
				"value": {
				"B": "121",
				"C": "270",
				"A": "601"
			}
		* }
	 * </pre>
	 * 
	 *
	 * @param value Open ended json object with key value pairs
	 * @return Param values open ended
	 */
	private JsonNode value;

	/**
	 * Used for partitioning logs
	 * This could be used for knowing if model data is for model usage or model parameters.
	 * 
	 * @param tags array of tags to be used to separate different logs
	 * @return rray of tags to be used to separate different logs
	 */
	private String[] tags;

	/**
	 * When the data was initially recorded. Since data may take some time during transfer
	 * we want to keep the original timestamp when the params were collected or 
	 * when the model was used.
	 * @param timestamp time when log was recorded
	 * @return time when log entry was recorded
	 */
	@JsonProperty("@timestamp")
	private String timestamp;

	/**
	 * Model identifying information.
	 * @param model model identification ie version, solution id, and subscriber host
	 * @return model identification ie version, solution id, and subscriber host
	 */
	private ModelInfo model;

}

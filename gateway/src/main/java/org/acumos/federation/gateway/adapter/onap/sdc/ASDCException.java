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

package org.acumos.federation.gateway.adapter.onap.sdc;

import java.util.Arrays;

import org.springframework.web.client.HttpClientErrorException;

import org.json.JSONObject;
import org.json.JSONArray;

/** */
public class ASDCException extends Exception {

	private static final String[] ERROR_CLASSES = new String[] { "policyException", "serviceException" };

	private JSONObject content;

	public ASDCException(HttpClientErrorException theError) {
		super(theError);

		String body = theError.getResponseBodyAsString();
		if (body != null) {
			JSONObject error = new JSONObject(body)
													.getJSONObject("requestError");
			if (error != null) {
				this.content = Arrays.stream(ERROR_CLASSES)
															.map(c -> error.optJSONObject(c))
															.filter(x -> x != null)
															.findFirst()
															.orElse(null);
			}
		}
	}

	public String getASDCMessageId() {
		return this.content == null ? "" : this.content.optString("messageId");
	}

	public String getASDCMessage() {
		if (this.content == null)
			return "";

		String msg = content.optString("text");
		if (msg != null) {
			JSONArray vars = content.optJSONArray("variables");
			if (vars != null) {
				for (int i = 0; i < vars.length(); i++) {
					msg = msg.replaceAll("%"+(i+1), vars.optString(i));
				}
			}
			return msg;
		}
		else
			return "";
	}

	@Override
	public String getMessage() {
		return "ASDC " + getASDCMessageId() + " " + getASDCMessage() + "\n" + super.getMessage();
	}
}

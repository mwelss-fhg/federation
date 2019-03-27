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

package org.acumos.federation.gateway.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

public class JSONHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

	public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	public JSONHttpMessageConverter() {
		super(new MediaType("application", "json", DEFAULT_CHARSET));
	}

	@Override
	protected boolean supports(Class<?> theClazz) {
		return theClazz.equals(JSONObject.class) || theClazz.equals(JSONArray.class);
	}

	@Override
	protected Object readInternal(Class<?> theClazz, HttpInputMessage theInputMessage) throws IOException {

		Reader json = new InputStreamReader(theInputMessage.getBody(), getCharset(theInputMessage.getHeaders()));

		try {
			if (theClazz.equals(JSONObject.class)) {
				return new JSONObject(new JSONTokener(json));
			} else {
				return new JSONArray(new JSONTokener(json));
			}
		} catch (JSONException jsonx) {
			throw new HttpMessageNotReadableException("Could not read JSON: " + jsonx.getMessage(), jsonx, theInputMessage);
		}
	}

	@Override
	protected void writeInternal(Object theObject, HttpOutputMessage theOutputMessage)
			throws IOException {

		Writer writer = new OutputStreamWriter(theOutputMessage.getBody(), getCharset(theOutputMessage.getHeaders()));

		try {
			if (theObject instanceof JSONObject) {
				((JSONObject) theObject).write(writer);
			} else {
				((JSONArray) theObject).write(writer);
			}
			writer.close();
		} catch (JSONException jsonx) {
			throw new HttpMessageNotWritableException("Could not write JSON: " + jsonx.getMessage(), jsonx);
		}
	}

	private Charset getCharset(HttpHeaders theHeaders) {
		if (theHeaders != null && theHeaders.getContentType() != null
				&& theHeaders.getContentType().getCharset() != null) {
			return theHeaders.getContentType().getCharset();
		}
		return DEFAULT_CHARSET;
	}

}

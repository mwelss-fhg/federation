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

package org.acumos.federation.gateway.common;

/**
* This class represents a common format set for the response send to the client over
* the REST interface.
*/

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonResponse<T> implements Serializable {

	private static final long serialVersionUID = -2934104266393591755L;

	/**
	 * Was there an error ?
	 */
	@JsonProperty(value = JSONTags.TAG_RESPONSE_ERROR)
	private String error;

	/**
	 * Additional information.
	 */
	@JsonProperty(value = JSONTags.TAG_RESPONSE_MESSAGE)
	private String message;

	/**
	 * Response content.
	 */
	@JsonProperty(value = JSONTags.TAG_RESPONSE_CONTENT)
	private T content;
	
	public String getError() {
		return this.error;
	}

	public void setError(String theError) {
		this.error = theError;
	}

	public String getMessage() {
		return this.message;
	}

	public void setMessage(String theMessage) {
		this.message = theMessage;
	}

	public T getContent() {
		return this.content;
	}

	public void setContent(T theContent) {
		this.content = theContent;
	}

	public String toString() {
		return new StringBuilder("JsonResponse")
								.append(System.identityHashCode(this))
								.append('(')
								.append(this.error)
								.append(',')
								.append(this.message == null ? "null" : this.message)
								.append(',')
								.append(this.content == null ? "null" : this.content)
								.append(')')
								.toString();
	}

	public static class JsonResponseBuilder<T> {

		private JsonResponse<T> response = new JsonResponse();

		public JsonResponseBuilder() {
			this.response.setError(null);
		}

		public JsonResponseBuilder<T>	withMessage(String theMessage) {
			this.response.setMessage(theMessage);
			return this;
		}
		
		public JsonResponseBuilder<T>	withContent(T theContent) {
			this.response.setContent(theContent);
			return this;
		}

		public JsonResponse<T> build() {
			return this.response;
		}
	}

	public static <T> JsonResponseBuilder<T> buildResponse() {
		return new JsonResponseBuilder<T>();
	}

	public static class JsonErrorResponseBuilder<T> {

		private JsonResponse<T> response = new JsonResponse();

		public JsonErrorResponseBuilder() {
			this.response.setError(Boolean.TRUE.toString());
			this.response.setContent(null);
		}

		public JsonErrorResponseBuilder<T>	withMessage(String theMessage) {
			this.response.setMessage(theMessage);
			return this;
		}
		
		public JsonErrorResponseBuilder<T>	withError(Throwable theError) {
			this.response.setError(theError.toString());
			return this;
		}

		public JsonResponse<T> build() {
			return this.response;
		}
	}
	
	public static <T> JsonErrorResponseBuilder<T> buildErrorResponse() {
		return new JsonErrorResponseBuilder<T>();
	}

}

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
package org.acumos.federation.gateway.test;

import java.util.function.Consumer;


public class MockResponse {

	private String 		resourceName;
	private int				responseCode;
	private String		responseMsg;
	private Consumer<MockResponse>	responseConsumer;

	public MockResponse(int theCode, String theMessage, String theResourceName) {
		this(theCode, theMessage, theResourceName, null);
	}

	public MockResponse(int theCode, String theMessage, String theResourceName, Consumer<MockResponse> theConsumer) {
		this.responseCode = theCode;
		this.responseMsg = theMessage;
		this.resourceName = theResourceName;
		this.responseConsumer = theConsumer;
	}

	public String getResourceName() {
		return this.resourceName;
	}

	public int getResponseCode() {
		return this.responseCode;
	}

	public String getResponseMsg() {
		return this.responseMsg;
	}

	public void consume() {
		if (this.responseConsumer != null) {
			this.responseConsumer.accept(this);
		}
	}

	public String toString() {
		return this.responseCode + " " + this.responseMsg + " : " + this.resourceName;
	}

	public static MockResponse success(String theResource) {
		return success(theResource, null);
	}

	public static MockResponse success(String theResource, Consumer<MockResponse> theConsumer) {
		return new MockResponse(200, "Success", theResource, theConsumer);
	}
	
}


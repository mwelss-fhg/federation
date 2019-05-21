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
package org.acumos.federation.gateway;

/**
 * Exception to abort current processing and reply to the peer with
 * the specified HTTP response code and message.
 */
public class BadRequestException extends RuntimeException {
	private final int code;

	/**
	 * Exception for when the peer's request is unacceptable.
	 *
	 * @param code The HTTP response code to send.
	 * @param message Text description of the problem.
	 */
	public BadRequestException(int code, String message) {
		super(message);
		this.code = code;
	}

	/**
	 * Get the HTTP response code.
	 *
	 * @return The response code.
	 */
	public int getCode() {
		return code;
	}
}

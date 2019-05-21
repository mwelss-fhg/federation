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

/**
 * Wrapper for Federation REST responses.
 */
@Data
public class JsonResponse<T> {
	/**
	 * Error message.
	 *
	 * @param error The error message.
	 * @return The error message (null on success).
	 */
	private String error;
	/**
	 * Success message.
	 *
	 * @param message The status message.
	 * @return The status message (null on error).
	 */
	private String message;
	/**
	 * Result.
	 *
	 * @param content The reply value.
	 * @return The reply value (null on error).
	 */
	private T content;
}

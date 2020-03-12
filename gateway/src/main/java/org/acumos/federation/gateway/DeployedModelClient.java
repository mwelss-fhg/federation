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
package org.acumos.federation.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import org.acumos.federation.client.ClientBase;
import org.acumos.federation.client.config.ClientConfig;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;

import java.util.Collections;


/**
 * Client for the DeployedModel API.  Note that servers
 * implementing the API may restrict what information they
 * share with clients.  Servers may refuse access to some clients, may refuse
 * access to some operations, may restrict what data is visible to clients,
 * etc., based on their particular policies.  This may result in client
 * methods returning null, returning lists with reduced numbers of elements, or
 * throwing {@link RestClientException} or its
 * subclasses.
 *
 */
public class DeployedModelClient extends ClientBase {
	/**
	 * The URI for to update model params in model.
	 */
	public static final String MODEL_UPDATE_PARAMS = "/model/methods/updateParams";
	private HttpHeaders headers = new HttpHeaders();


	/**
	 * Create a DeployedModelClient with the default mapper and resource loader.
	 *
	 * @param target The base URL for the server to be accessed.
	 * @param conf   The configuration for certificates and credentials.
	 */
	public DeployedModelClient(String target, ClientConfig conf) {
		super(target, conf, null, null);
		this.headers.setContentType(MediaType.APPLICATION_JSON);
		this.headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
	}

	/**
	 * Update params to a deployed model
	 *
	 * @param params The params to be passed on the deployed model
	 */
	public void updateModelParams(JsonNode params) throws RestClientException{
		//Using the json API for Deployed models
		HttpEntity<JsonNode> entity = new HttpEntity<>(params, this.headers);
		restTemplate.postForObject(MODEL_UPDATE_PARAMS, entity, JsonNode.class);
	}

}

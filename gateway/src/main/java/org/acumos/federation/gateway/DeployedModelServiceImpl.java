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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.domain.MLPSolutionDeployment;
import org.acumos.federation.client.config.ClientConfig;
import org.acumos.federation.client.data.ModelData;
import org.acumos.federation.client.data.ModelInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

public class DeployedModelServiceImpl implements DeployedModelService {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private Clients clients;

	private void updateModelParams(String ingressUrl, JsonNode params) throws RestClientException{
		DeployedModelClient deployedModelClient = clients.getDeployedModelClient(ingressUrl);
		deployedModelClient.updateModelParams(params);
	}

	@Override
	public String updateParamsForAllDeployments(ModelData payload) throws IOException {
		ModelInfo modelInfo = payload.getModel();

		List<MLPSolutionDeployment> mlpSolutionDeployments = Application.cdsAll(pr -> clients.getCDSClient()
		    .getSolutionDeployments(modelInfo.getSolutionId(), modelInfo.getRevisionId(), pr));
		int totalFailure = 0;
		for (MLPSolutionDeployment mlpSolutionDeployment : mlpSolutionDeployments) {
			boolean isUpdateSuccess = updateParamsForDeployment(payload, mlpSolutionDeployment);
			if (!isUpdateSuccess){
				totalFailure++;
			}
		}
		String responseMessage="";
		if(totalFailure > 0){
			responseMessage = String.format("Params data posted to deployed models,of total {} " +
			    "deployments failed posting to {} deployments",mlpSolutionDeployments.size(),totalFailure);
		}else {
			responseMessage = String.format("Params data posted to deployed models, total {} deployments updated", mlpSolutionDeployments.size());
		}
		return responseMessage;
	}

	private boolean updateParamsForDeployment(ModelData payload, MLPSolutionDeployment mlpSolutionDeployment) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode detailObject = objectMapper.readTree(mlpSolutionDeployment.getDetail());
		String ingressUrlFieldName = "nodePortUrl";
		boolean isContinousTrainingEnabled = detailObject != null;
		if (isContinousTrainingEnabled) {
			String continuousTrainingEnabledFieldName = "continuousTrainingEnabled";
			isContinousTrainingEnabled = detailObject.has(ingressUrlFieldName)
			    && detailObject.has(continuousTrainingEnabledFieldName)
			    && detailObject.get(continuousTrainingEnabledFieldName).asBoolean();
		}

		if (isContinousTrainingEnabled) {
			log.debug("detail object : " + mlpSolutionDeployment.getDetail());
			JsonNode deploymentUrl = detailObject.get(ingressUrlFieldName);
			log.debug("deployment url" + deploymentUrl.asText());
			String ingressUrl = deploymentUrl.asText();
			JsonNode params = objectMapper.createObjectNode();
			((ObjectNode) params).set("params", payload.getValue());
			try {
				updateModelParams(ingressUrl, params);
				log.info("successfully posted params to deployment with deployment id {} , solution id {} " +
								", revision id {}",
				    mlpSolutionDeployment.getDeploymentId(),
				    mlpSolutionDeployment.getSolutionId(),
				    mlpSolutionDeployment.getRevisionId());
			} catch (Exception e) {
				log.error("Failed posting params to deployment with deployment id {} , solution id {} " +
								", revision id {}",
				    mlpSolutionDeployment.getDeploymentId(),
				    mlpSolutionDeployment.getSolutionId(),
				    mlpSolutionDeployment.getRevisionId());
				return false;
			}
		}
		return true;
	}

}
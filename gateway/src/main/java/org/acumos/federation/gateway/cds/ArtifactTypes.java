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
package org.acumos.federation.gateway.cds;

import java.util.List;

/**
 * The set of codes representing artifact types.
 */
public class ArtifactTypes extends CodeNames<ArtifactType> {

	//these are the artifact type codes that we expect to find in all deployments.
  public static final ArtifactType Blueprint = forCode("BP");
  public static final ArtifactType Cdump = forCode("CD");
  public static final ArtifactType DockerImage = forCode("DI");
  public static final ArtifactType DataSource = forCode("DS");
  public static final ArtifactType Metadata = forCode("MD");
  public static final ArtifactType ModelH2O = forCode("MH");
  public static final ArtifactType ModelImage = forCode("MI");
  public static final ArtifactType ModelR = forCode("MR");
  public static final ArtifactType ModelScikit = forCode("MS");
  public static final ArtifactType ModelTensorflow = forCode("MT");
  public static final ArtifactType ToscaTemplate = forCode("TE");
  public static final ArtifactType ToscaGenerator = forCode("TG");
  public static final ArtifactType ToscaSchema = forCode("TS");
  public static final ArtifactType ToscaTranslate = forCode("TT");
  public static final ArtifactType ProtobufFile = forCode("PJ");


	public static ArtifactType forCode(String theCode) {
		return CodeNames.forCode(theCode, ArtifactType.class);
	}

	public static List<ArtifactType> codes() {
		return codes(ArtifactType.class);
	} 
}



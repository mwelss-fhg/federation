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

import java.util.EnumSet;
import org.acumos.cds.domain.MLPArtifactType;
import org.acumos.cds.ArtifactTypeCode;

/**
 * Supplements the CDS representation of artifact type information.
 */
public enum ArtifactType {


  Blueprint(ArtifactTypeCode.BP.name()), //
  Cdump(ArtifactTypeCode.CD.name()), //
  DockerImage(ArtifactTypeCode.DI.name()), //
  DataSource(ArtifactTypeCode.DS.name()), //
  Metadata(ArtifactTypeCode.MD.name()), //
  ModelH2O(ArtifactTypeCode.MH.name()), //
  ModelImage(ArtifactTypeCode.MI.name()), //
  ModelR(ArtifactTypeCode.MR.name()), //
  ModelScikit(ArtifactTypeCode.MS.name()), //
  ModelTensorflow(ArtifactTypeCode.MT.name()), //
  ToscaTemplate(ArtifactTypeCode.TE.name()), //
  ToscaGenerator(ArtifactTypeCode.TG.name()), //
  ToscaSchema(ArtifactTypeCode.TS.name()), //
  ToscaTranslate(ArtifactTypeCode.TT.name()), //
  ProtobufFile(ArtifactTypeCode.PJ.name());

	private String 				code;

	private ArtifactType(String theCode) {
		this.code = theCode;
	}

	public String code() {
		return this.code;
	}

	public static ArtifactType forCode(final String theCode) {
		return EnumSet.allOf(ArtifactType.class)
						.stream()
						.filter(status -> status.code().equals(theCode))
						.findFirst()
						.orElse(null);
	}
}



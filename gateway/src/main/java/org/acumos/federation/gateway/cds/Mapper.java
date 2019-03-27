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

package org.acumos.federation.gateway.cds;

import java.io.IOException;

import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPCatalog;
import org.acumos.cds.domain.MLPDocument;
import org.acumos.cds.domain.MLPPeerSubscription;
import org.acumos.cds.domain.MLPRevCatDescription;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


/**
 * Provides a Jackson ObjectMapper configured with an extension module for
 * processing federation data where CDS data is declared.
 */

public class Mapper {
	private static class SimpleModuleBuilder	{
		private SimpleModule module = new SimpleModule("CDSModule", new Version(1, 18, 0, null));

		public <B, E extends B> SimpleModuleBuilder add(Class<B> base, Class<E> enhanced) {
			module.addDeserializer(base, new StdDeserializer<B>(base) {
				@Override
				public B deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
					return ((ObjectMapper)parser.getCodec()).readValue(parser, enhanced);
				}
			});
			return this;
		}

		public SimpleModule build() {
			return module;
		}
	}

	public static ObjectMapper build() {
		SimpleModule fedModule = new SimpleModuleBuilder()
		    .add(MLPArtifact.class, Artifact.class)
		    .add(MLPCatalog.class, Catalog.class)
		    .add(MLPDocument.class, Document.class)
		    .add(MLPPeerSubscription.class, PeerSubscription.class)
		    .add(MLPRevCatDescription.class, RevCatDescription.class)
		    .add(MLPSolution.class, Solution.class)
		    .add(MLPSolutionRevision.class, SolutionRevision.class)
		    .build();
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(fedModule);
		mapper.registerModule(new JavaTimeModule());
		return mapper;
	}
}

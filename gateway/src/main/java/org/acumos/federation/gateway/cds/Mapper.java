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

import java.io.IOException;

import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPDocument;
import org.acumos.cds.domain.MLPPeerSubscription;
import org.acumos.cds.domain.MLPRevisionDescription;
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
 * Provides a Jackson ObjectMapper configured with an extension module for processing
 * federation data where CDS data is declared.
 */
public class Mapper {

	public static ObjectMapper build() {
		ObjectMapper mapper = new ObjectMapper();

		SimpleModule fedModule =
      new SimpleModule("CDSModule",
          new Version(1, 18, 0, null));
    fedModule.addDeserializer(MLPSolution.class, new SolutionDeserializer());
    fedModule.addDeserializer(MLPSolutionRevision.class, new SolutionRevisionDeserializer());
    fedModule.addDeserializer(MLPArtifact.class, new ArtifactDeserializer());
    fedModule.addDeserializer(MLPDocument.class, new DocumentDeserializer());
    fedModule.addDeserializer(MLPPeerSubscription.class, new PeerSubscriptionDeserializer());
    fedModule.addDeserializer(MLPRevisionDescription.class, new RevisionDescriptionDeserializer());

		mapper.registerModule(fedModule);
		mapper.registerModule(new JavaTimeModule());

		return mapper;
	}

	private static class SolutionDeserializer extends StdDeserializer<MLPSolution> {

		public SolutionDeserializer() {
			super(MLPSolution.class);
		}

		@Override
  	public MLPSolution deserialize(JsonParser theParser, DeserializationContext theCtx) 
      																								throws IOException, JsonProcessingException {
  	  ObjectMapper mapper = (ObjectMapper) theParser.getCodec();
    	return mapper.readValue(theParser, Solution.class);
  	}
	}

	private static class SolutionRevisionDeserializer extends StdDeserializer<MLPSolutionRevision> {
 
		public SolutionRevisionDeserializer() {
			super(MLPSolutionRevision.class);
		}
 
		@Override
  	public MLPSolutionRevision deserialize(JsonParser theParser, DeserializationContext theCtx) 
      																									throws IOException, JsonProcessingException {
  	  ObjectMapper mapper = (ObjectMapper) theParser.getCodec();
    	return mapper.readValue(theParser, SolutionRevision.class);
  	}
	}

	private static class ArtifactDeserializer extends StdDeserializer<MLPArtifact> {
 
		public ArtifactDeserializer() {
			super(MLPArtifact.class);
		}
 
		@Override
  	public MLPArtifact deserialize(JsonParser theParser, DeserializationContext theCtx) 
      																								throws IOException, JsonProcessingException {
  	  ObjectMapper mapper = (ObjectMapper) theParser.getCodec();
    	return mapper.readValue(theParser, Artifact.class);
  	}
	}

	private static class DocumentDeserializer extends StdDeserializer<MLPDocument> {
 
		public DocumentDeserializer() {
			super(MLPDocument.class);
		}
 
		@Override
  	public MLPDocument deserialize(JsonParser theParser, DeserializationContext theCtx) 
      																								throws IOException, JsonProcessingException {
  	  ObjectMapper mapper = (ObjectMapper) theParser.getCodec();
    	return mapper.readValue(theParser, Document.class);
  	}
	}

	private static class PeerSubscriptionDeserializer extends StdDeserializer<MLPPeerSubscription> {
 
		public PeerSubscriptionDeserializer() {
			super(MLPPeerSubscription.class);
		}
 
		@Override
  	public MLPPeerSubscription deserialize(JsonParser theParser, DeserializationContext theCtx) 
      																								throws IOException, JsonProcessingException {
  	  ObjectMapper mapper = (ObjectMapper) theParser.getCodec();
    	return mapper.readValue(theParser, PeerSubscription.class);
  	}
	}

	private static class RevisionDescriptionDeserializer extends StdDeserializer<MLPRevisionDescription> {
 
		public RevisionDescriptionDeserializer() {
			super(MLPRevisionDescription.class);
		}
 
		@Override
  	public MLPRevisionDescription deserialize(JsonParser theParser, DeserializationContext theCtx) 
      																								throws IOException, JsonProcessingException {
  	  ObjectMapper mapper = (ObjectMapper) theParser.getCodec();
    	return mapper.readValue(theParser, RevisionDescription.class);
  	}
	}
}


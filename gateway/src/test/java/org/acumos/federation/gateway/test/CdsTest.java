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

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.Set;

import org.acumos.cds.domain.MLPSolutionWeb;
import org.acumos.cds.domain.MLPTag;
import org.acumos.federation.gateway.cds.AccessType;
import org.acumos.federation.gateway.cds.Artifact;
import org.acumos.federation.gateway.cds.ArtifactBuilder;
import org.acumos.federation.gateway.cds.ArtifactType;
import org.acumos.federation.gateway.cds.Document;
import org.acumos.federation.gateway.cds.DocumentBuilder;
import org.acumos.federation.gateway.cds.Solution;
import org.acumos.federation.gateway.cds.SolutionBuilder;
import org.acumos.federation.gateway.cds.SolutionRevision;
import org.acumos.federation.gateway.cds.SolutionRevisionBuilder;
import org.acumos.federation.gateway.cds.Updater;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.junit.Assert;
import org.junit.Test;

public class CdsTest {

	private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(MethodHandles.lookup().lookupClass());

	private final boolean active = true;
	private final String atc = "PR";
	private final Date date1 = new Date();
	private final Date date2 = new Date();
	private final String desc = "desc";
	private final String id1 = "id1";
	private final String id2 = "id2";
	private final String meta = "meta";
	private final String mtc = "MT";
	private final String name = "name";
	private final String origin = "origin";
	private final String source = "source";
	private final String ttc = "TK";
	private final String uri = "uri";
	private final String user = "user";
	private final String version = "ver";
	private final Integer size = 42;
	private final String vsc = "YZ";

	// Trivial implementation just returns first argument
	private final Updater<String, Object> theUpdater = new Updater<String, Object>() {
		@Override
		public String update(Object... theArgs) {
			return theArgs[0].toString();
		}
	};

	@Test
	public void testAccessType() throws Exception {
		for (AccessType e : AccessType.values()) {
			log.info("access type {}, code {}", e, e.code());
			AccessType.forCode(e.code());
		}
	}
	
	@Test
	public void testArtifactType() throws Exception {
		for (ArtifactType e : ArtifactType.values()) {
			log.info("artifact type {}, code {}", e, e.code());
			ArtifactType.forCode(e.code());
		}
	}

	@Test
	public void testArtifactBuilder() throws Exception {
		ArtifactBuilder bldr = Artifact.build();
		bldr.withCreated(date1.getTime()).withCreatedDate(date1).withDescription(desc).withId(id1).withMetadata(meta)
				.withModifiedDate(date2).withName(name).withSize(size).withTypeCode(mtc).withVersion(version)
				.withUri(uri).withUser(user);
		Artifact a = bldr.build();
		Assert.assertNotNull(a);
		Assert.assertEquals(date1, a.getCreated());
		Assert.assertEquals(desc, a.getDescription());
		Assert.assertEquals(id1, a.getArtifactId());
		Assert.assertEquals(meta, a.getMetadata());
		Assert.assertEquals(date2, a.getModified());
		Assert.assertEquals(name, a.getName());
		Assert.assertEquals(size, a.getSize());
		Assert.assertEquals(mtc, a.getArtifactTypeCode());
		Assert.assertEquals(version, a.getVersion());
		Assert.assertEquals(uri, a.getUri());
		Assert.assertEquals(user, a.getUserId());
	}

	@Test
	public void testDocumentBuilder() throws Exception {
		DocumentBuilder bldr = Document.build();
		bldr.withCreated(date1.getTime()).withCreatedDate(date1).withId(id1).withModified(date2.getTime())
				.withModifiedDate(date2).withName(name).withSize(size).withUri(uri).withUser(user).withVersion(version);
		Document d = bldr.build();
		Assert.assertNotNull(d);
		Assert.assertEquals(date1, d.getCreated());
		Assert.assertEquals(id1, d.getDocumentId());
		Assert.assertEquals(date2, d.getModified());
		Assert.assertEquals(name, d.getName());
		Assert.assertEquals(size, d.getSize());
		Assert.assertEquals(uri, d.getUri());
		Assert.assertEquals(user, d.getUserId());
		Assert.assertEquals(version, d.getVersion());
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testSolutionBuilder() throws Exception {
		final Set<MLPTag> tags = null;
		final MLPSolutionWeb theStats = null;
		SolutionBuilder bldr = Solution.build();
		bldr.withActive(active).withCreatedDate(date1).withDescription(desc).withId(id1).withMetadata(meta)
				.withModelTypeCode(mtc).withModifiedDate(date2).withName(name).withOrigin(origin).withSource(source)
				.withToolkitTypeCode(ttc).withUser(user);
		bldr.withSource(theUpdater, source);
		bldr.withTags(tags);
		bldr.withUser(theUpdater, user);
		bldr.withWebStats(theStats);
		Solution s = bldr.build();
		Assert.assertNotNull(s);
		Assert.assertEquals(active, s.isActive());
		Assert.assertEquals(date1, s.getCreated());
		Assert.assertEquals(desc, s.getDescription());
		Assert.assertEquals(id1, s.getSolutionId());
		Assert.assertEquals(meta, s.getMetadata());
		Assert.assertEquals(mtc, s.getModelTypeCode());
		Assert.assertEquals(date2, s.getModified());
		Assert.assertEquals(name, s.getName());
		Assert.assertEquals(origin, s.getOrigin());
		Assert.assertEquals(source, s.getSourceId());
		Assert.assertEquals(ttc, s.getToolkitTypeCode());
		Assert.assertEquals(user, s.getUserId());
		Assert.assertNull(s.getTags());
		Assert.assertNull(s.getWebStats());
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testSolutionRevisionBuilder() throws Exception {
		SolutionRevisionBuilder bldr = SolutionRevision.build();
		bldr.forSolution(id1).withAccessTypeCode(atc).withCreatedDate(date1).withDescription(desc).withMetadata(meta)
				.withModifiedDate(date2).withOrigin(origin).withRevisionId(id2).withSource(source).withVersion(version)
				.withUser(user).withValidationStatusCode(vsc);
		SolutionRevision r = bldr.build();
		Assert.assertNotNull(r);
		Assert.assertEquals(id1, r.getSolutionId());
		Assert.assertEquals(atc, r.getAccessTypeCode());
		Assert.assertEquals(date1, r.getCreated());
		Assert.assertEquals(desc, r.getDescription());
		Assert.assertEquals(meta, r.getMetadata());
		Assert.assertEquals(date2, r.getModified());
		Assert.assertEquals(origin, r.getOrigin());
		Assert.assertEquals(id2, r.getRevisionId());
		Assert.assertEquals(source, r.getSourceId());
		Assert.assertEquals(version, r.getVersion());
		Assert.assertEquals(user, r.getUserId());
		Assert.assertEquals(vsc, r.getValidationStatusCode());
	}

}

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
package org.acumos.federation.gateway.test;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.Set;

import org.acumos.cds.domain.MLPTag;
import org.acumos.federation.gateway.cds.Artifact;
import org.acumos.federation.gateway.cds.ArtifactBuilder;
import org.acumos.federation.gateway.cds.Document;
import org.acumos.federation.gateway.cds.DocumentBuilder;
import org.acumos.federation.gateway.cds.Solution;
import org.acumos.federation.gateway.cds.SolutionBuilder;
import org.acumos.federation.gateway.cds.SolutionRevision;
import org.acumos.federation.gateway.cds.SolutionRevisionBuilder;
import org.acumos.federation.gateway.cds.Updater;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CdsTest {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final boolean active = true;
	private final Instant date1 = Instant.now();
	private final Instant date2 = Instant.now();
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

	// Trivial implementation just returns first argument
	private final Updater<String, Object> theUpdater = new Updater<String, Object>() {
		@Override
		public String update(Object... theArgs) {
			return theArgs[0].toString();
		}
	};

	@Test
	public void testArtifactBuilder() throws Exception {
		ArtifactBuilder bldr = Artifact.build();
		bldr.withCreated(date1).withDescription(desc).withId(id1).withMetadata(meta)
				.withModified(date2).withName(name).withSize(size).withTypeCode(mtc).withVersion(version)
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
		bldr.withCreated(date1).withId(id1).withModified(date2)
				.withModified(date2).withName(name).withSize(size).withUri(uri).withUser(user).withVersion(version);
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
		SolutionBuilder bldr = Solution.build();
		bldr.withActive(active).withCreated(date1).withId(id1).withMetadata(meta)
				.withModelTypeCode(mtc).withModified(date2).withName(name).withOrigin(origin).withSource(source)
				.withToolkitTypeCode(ttc).withUser(user);
		bldr.withSource(theUpdater, source);
		bldr.withTags(tags);
		bldr.withUser(theUpdater, user);
		Solution s = bldr.build();
		Assert.assertNotNull(s);
		Assert.assertEquals(active, s.isActive());
		Assert.assertEquals(date1, s.getCreated());
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
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testSolutionRevisionBuilder() throws Exception {
		SolutionRevisionBuilder bldr = SolutionRevision.build();
		bldr.forSolution(id1).withCreated(date1).withMetadata(meta)
				.withModified(date2).withOrigin(origin).withRevisionId(id2).withSource(source).withVersion(version)
				.withUser(user);
		SolutionRevision r = bldr.build();
		Assert.assertNotNull(r);
		Assert.assertEquals(id1, r.getSolutionId());
		Assert.assertEquals(date1, r.getCreated());
		Assert.assertEquals(meta, r.getMetadata());
		Assert.assertEquals(date2, r.getModified());
		Assert.assertEquals(origin, r.getOrigin());
		Assert.assertEquals(id2, r.getRevisionId());
		Assert.assertEquals(source, r.getSourceId());
		Assert.assertEquals(version, r.getVersion());
		Assert.assertEquals(user, r.getUserId());
	}

}

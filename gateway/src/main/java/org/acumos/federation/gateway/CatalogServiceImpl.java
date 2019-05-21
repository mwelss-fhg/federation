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
package org.acumos.federation.gateway;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.springframework.beans.factory.annotation.Autowired;

import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPCatalog;
import org.acumos.cds.domain.MLPDocument;
import org.acumos.cds.domain.MLPRevCatDescription;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;

import org.acumos.federation.client.data.Catalog;
import org.acumos.federation.client.data.Solution;
import org.acumos.federation.client.data.SolutionRevision;

/**
 * Service bean for implementing the Catalog service using Acumos CDS.
 */
public class CatalogServiceImpl implements CatalogService {
	private static final String IGNORED_SOLUTIONID = "ignored";

	@Autowired
	private Clients clients;

	@Override
	public MLPArtifact getArtifact(String artifactId) {
		return clients.getCDSClient().getArtifact(artifactId);
	}

	@Override
	public List<MLPArtifact> getArtifacts(String revisionId) {
		return clients.getCDSClient().getSolutionRevisionArtifacts(IGNORED_SOLUTIONID, revisionId);
	}

	@Override
	public MLPArtifact createArtifact(MLPArtifact artifact) {
		return clients.getCDSClient().createArtifact(artifact);
	}

	@Override
	public void updateArtifact(MLPArtifact artifact) {
		clients.getCDSClient().updateArtifact(artifact);
	}

	@Override
	public void addArtifact(String solutionId, String revisionId, String artifactId) {
		clients.getCDSClient().addSolutionRevisionArtifact(solutionId, revisionId, artifactId);
	}

	@Override
	public List<MLPCatalog> getAllCatalogs() {
		return Application.cdsAll(pr -> clients.getCDSClient().getCatalogs(pr));
	}

	@Override
	public List<MLPCatalog> getCatalogs() {
		ICommonDataServiceRestClient client = clients.getCDSClient();
		List<MLPCatalog> ret = Application.cdsAll(pr -> client.searchCatalogs(Collections.singletonMap("accessTypeCode", "PB"), false, pr));
		String peerId = Security.getCurrentPeerId();
		Set<String> toget = peerId != null? new HashSet<>(client.getPeerAccessCatalogIds(peerId)): new HashSet<>();
		for (MLPCatalog mcat: ret) {
			toget.remove(mcat.getCatalogId());
			((Catalog)mcat).setSize((int)client.getCatalogSolutionCount(mcat.getCatalogId()));
		}
		for (String catid: toget) {
			MLPCatalog mcat = client.getCatalog(catid);
			if (mcat != null) {
				((Catalog)mcat).setSize((int)client.getCatalogSolutionCount(catid));
				ret.add(mcat);
			}
		}
		return ret;
	}

	@Override
	public void createCatalog(MLPCatalog catalog) {
		clients.getCDSClient().createCatalog(catalog);
	}

	@Override
	public void savePicture(String solutionId, byte[] picture) {
		clients.getCDSClient().saveSolutionPicture(solutionId, picture);
	}

	@Override
	public MLPDocument getDocument(String documentId) {
		return clients.getCDSClient().getDocument(documentId);
	}

	@Override
	public List<MLPDocument> getDocuments(String revisionId, String catalogId) {
		return clients.getCDSClient().getRevisionCatalogDocuments(revisionId, catalogId);
	}

	@Override
	public MLPDocument createDocument(MLPDocument document) {
		return clients.getCDSClient().createDocument(document);
	}

	@Override
	public void updateDocument(MLPDocument document) {
		clients.getCDSClient().updateDocument(document);
	}

	@Override
	public void addDocument(String revisionId, String catalogId, String documentId) {
		clients.getCDSClient().addRevisionCatalogDocument(revisionId, catalogId, documentId);
	}

	@Override
	public MLPRevCatDescription createDescription(MLPRevCatDescription revCatDescription) {
		return clients.getCDSClient().createRevCatDescription(revCatDescription);
	}

	@Override
	public void updateDescription(MLPRevCatDescription revCatDescription) {
		clients.getCDSClient().updateRevCatDescription(revCatDescription);
	}

	@Override
	public void deleteDescription(String revisionId, String catalogId) {
		clients.getCDSClient().deleteRevCatDescription(revisionId, catalogId);
	}

	@Override
	public MLPSolutionRevision getRevision(String revisionId, String catalogId) {
		ICommonDataServiceRestClient client = clients.getCDSClient();
		SolutionRevision ret = (SolutionRevision)client.getSolutionRevision(IGNORED_SOLUTIONID, revisionId);
		if (ret == null) {
			return ret;
		}
		ret.setArtifacts(getArtifacts(revisionId));
		if (catalogId == null) {
			return ret;
		}
		ret.setRevCatDescription(client.getRevCatDescription(revisionId, catalogId));
		ret.setDocuments(getDocuments(revisionId, catalogId));
		return ret;
	}

	@Override
	public List<MLPSolutionRevision> getRevisions(String solutionId) {
		return clients.getCDSClient().getSolutionRevisions(solutionId);
	}

	@Override
	public MLPSolutionRevision createRevision(MLPSolutionRevision revision) {
		return clients.getCDSClient().createSolutionRevision(revision);
	}

	@Override
	public void updateRevision(MLPSolutionRevision revision) {
		clients.getCDSClient().updateSolutionRevision(revision);
	}

	@Override
	public MLPSolution getSolution(String solutionId) {
		ICommonDataServiceRestClient client = clients.getCDSClient();
		Solution ret = (Solution)client.getSolution(solutionId);
		if (ret == null) {
			return null;
		}
		ret.setRevisions(getRevisions(solutionId));
		if (ret.getRevisions() == null || ret.getRevisions().isEmpty()) {
			return null;
		}
		ret.setPicture(client.getSolutionPicture(solutionId));
		return ret;
	}

	@Override
	public List<MLPSolution> getSolutions(String catalogId) {
		return Application.cdsAll(pr -> clients.getCDSClient().getSolutionsInCatalogs(new String[] { catalogId }, pr));
	}

	@Override
	public MLPSolution createSolution(MLPSolution solution) {
		return clients.getCDSClient().createSolution(solution);
	}

	@Override
	public void updateSolution(MLPSolution solution) {
		clients.getCDSClient().updateSolution(solution);
	}

	@Override
	public void addSolution(String solutionId, String catalogId) {
		clients.getCDSClient().addSolutionToCatalog(solutionId, catalogId);
	}

	@Override
	public boolean isArtifactAllowed(String artifactId) {
		return true;
	}

	@Override
	public boolean isCatalogAllowed(String catalogId) {
		return clients.getCDSClient().isPeerAccessToCatalog(Security.getCurrentPeerId(), catalogId);
	}

	@Override
	public boolean isDocumentAllowed(String documentId) {
		return true;
	}

	@Override
	public boolean isRevisionAllowed(String revisionId) {
		MLPSolutionRevision rev = clients.getCDSClient().getSolutionRevision(IGNORED_SOLUTIONID, revisionId);
		return rev != null && isSolutionAllowed(rev.getSolutionId());
	}

	@Override
	public boolean isSolutionAllowed(String solutionId) {
		return clients.getCDSClient().isPeerAccessToSolution(Security.getCurrentPeerId(), solutionId);
	}
}

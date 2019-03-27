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

package org.acumos.federation.gateway.service.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPCatalog;
import org.acumos.cds.domain.MLPDocument;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.federation.gateway.cds.Artifact;
import org.acumos.federation.gateway.cds.Catalog;
import org.acumos.federation.gateway.cds.Document;
import org.acumos.federation.gateway.cds.Solution;
import org.acumos.federation.gateway.cds.SolutionRevision;
import org.acumos.federation.gateway.service.CatalogService;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;


/**
 * 
 *
 */
@Service
@ConfigurationProperties(prefix = "catalog-local")
public class CatalogServiceLocalImpl extends AbstractServiceLocalImpl implements CatalogService {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private Resource catalogresource;
	private List<Catalog> catalogs;
	private List<Solution> solutions;

	public void setCatalogs(String theCatalogs) {
		this.catalogresource = appCtx.getResource(theCatalogs);
	}

	@PostConstruct
	public void initService() {
		monitor(Solution.class, resource, solns -> this.solutions = solns, "solutions");
		monitor(Catalog.class, catalogresource, cats -> this.catalogs = cats, "catalogs");
		// Done
		log.debug("Local CatalogService available");
	}

	@PreDestroy
	public void cleanupService() {
	}

	@Override
	public List<MLPCatalog> getCatalogs(ServiceContext theContext) throws ServiceException {
		log.debug("getCatalogs()");
		return new ArrayList<>(catalogs);
	}

	@Override
	public List<MLPSolution> getSolutions(String theCatalogId, ServiceContext theContext) throws ServiceException {

		log.debug("getSolutions");
		return(new ArrayList<MLPSolution>(solutions));
	}

	@Override
	public Solution getSolution(final String theSolutionId, ServiceContext theContext) throws ServiceException {

		log.debug("getSolution");
		return solutions.stream()
		    .filter(solution -> theSolutionId.equals(solution.getSolutionId()))
		    .findFirst().orElse(null);
	}

	@Override	
	public Solution putSolution(Solution theSolution, ServiceContext theContext) throws ServiceException {
		
		log.trace("putSolution {}", theSolution);
		return theSolution;
	}

	@Override
	public List<MLPSolutionRevision> getRevisions(final String theSolutionId, ServiceContext theContext) throws ServiceException {

		log.debug("getRevisions");

		Solution solution = getSolution(theSolutionId, theContext);
		return (solution == null) ? Collections.emptyList() : solution.getRevisions();
	}

	@Override
	public SolutionRevision getRevision(String theCatalogId, String theSolutionId, String theRevisionId, ServiceContext theContext) throws ServiceException  {

		log.debug("getRevision");
		return (SolutionRevision)getRevisions(theSolutionId, theContext).stream()
		    .filter(rev -> rev.getRevisionId().equals(theRevisionId)).findFirst().orElse(null);
	}

	@Override
	public SolutionRevision putRevision(SolutionRevision theRevision, ServiceContext theContext) throws ServiceException {
		log.trace("putSolutionRevision {}", theRevision);
		return theRevision;	
	}

	@Override
	public List<MLPArtifact> getArtifacts(final String theSolutionId, final String theRevisionId, ServiceContext theContext) throws ServiceException {
		log.debug("getArtifacts");

		SolutionRevision revision = getRevision(null, theSolutionId, theRevisionId, theContext);
		return (revision == null) ? Collections.emptyList() : revision.getArtifacts();
	}

	@Override
	public Artifact getArtifact(String theArtifactId, ServiceContext theContext) throws ServiceException {
		log.debug("getSolutionRevisionArtifact");
		// cumbersome
		for (Solution solution : this.solutions) {
			for (MLPSolutionRevision revision : solution.getRevisions()) {
				for (MLPArtifact artifact : ((SolutionRevision)revision).getArtifacts()) {
					if (artifact.getArtifactId().equals(theArtifactId)) {
						return (Artifact)artifact;
					}
				}
			}
		}

		return null;
	}
	
	@Override
	public List<MLPDocument> getDocuments(String theCatalogId, String theRevisionId, ServiceContext theContext) throws ServiceException {
		log.debug("getSolutionRevisionDocuments {} {}", theCatalogId, theRevisionId);
		for (Solution s: solutions) {
			for (MLPSolutionRevision rx: s.getRevisions()) {
				if (rx.getRevisionId().equals(theRevisionId)) {
					SolutionRevision r = (SolutionRevision)rx;
					if (r.getDocuments() != null) {
						return(r.getDocuments());
					}
				}
			}
		}
		return Collections.emptyList();
	}

	@Override
	public Document getDocument(String theDocumentId, ServiceContext theContext) throws ServiceException {
		log.debug("getSolutionRevisionDocument");
		// cumbersome
		for (Solution solution : this.solutions) {
			for (MLPSolutionRevision revision : solution.getRevisions()) {
				for (MLPDocument document : ((SolutionRevision)revision).getDocuments()) {
					if (document.getDocumentId().equals(theDocumentId)) {
						return (Document)document;
					}
				}
			}
		}

		return null;
	}

	@Override
	public boolean isCatalogAllowed(String theCatalogId, ServiceContext theContext) {
		return true;
	}

	@Override
	public boolean isSolutionAllowed(String theSolutionId, ServiceContext theContext) {
		return true;
	}

	@Override
	public boolean isRevisionAllowed(String theRevisionId, ServiceContext theContext) {
		return true;
	}

	@Override
	public boolean isArtifactAllowed(String theArtifactId, ServiceContext theContext) {
		return true;
	}

	@Override
	public boolean isDocumentAllowed(String theDocumentId, ServiceContext theContext) {
		return true;
	}
}

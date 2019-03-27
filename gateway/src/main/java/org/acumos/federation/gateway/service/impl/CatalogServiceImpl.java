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

/**
 * 
 */
package org.acumos.federation.gateway.service.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.PostConstruct;

import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPCatalog;
import org.acumos.cds.domain.MLPDocument;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.cds.domain.MLPTag;
import org.acumos.cds.transport.RestPageRequest;
import org.acumos.cds.transport.RestPageResponse;
import org.acumos.federation.gateway.cds.Artifact;
import org.acumos.federation.gateway.cds.Catalog;
import org.acumos.federation.gateway.cds.Document;
import org.acumos.federation.gateway.cds.Solution;
import org.acumos.federation.gateway.cds.SolutionRevision;
import org.acumos.federation.gateway.cds.TimestampedEntity;
import org.acumos.federation.gateway.service.CatalogService;
import org.acumos.federation.gateway.service.CatalogServiceConfiguration;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.service.ServiceException;
import org.acumos.federation.gateway.util.Errors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * CDS based implementation of the CatalogService.
 *
 */
@Service
public class CatalogServiceImpl extends AbstractServiceImpl implements CatalogService {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private CatalogServiceConfiguration config;

	@PostConstruct
	public void initService() {
	}

	@Override
	public List<MLPCatalog> getCatalogs(ServiceContext theContext) throws ServiceException {
		log.debug("getCatalogs");
		ICommonDataServiceRestClient cdsClient = getClient(theContext);
		List<MLPCatalog> catalogs = allPages("searchCatalogs", page -> cdsClient.searchCatalogs(config.getCatalogsSelector(), false, page));
		try {
			String peerId = getPeerId(theContext);
			Set<String> toget = peerId != null? new HashSet<>(cdsClient.getPeerAccessCatalogIds(peerId)): new HashSet<>();
			for (MLPCatalog mcat: catalogs) {
				toget.remove(mcat.getCatalogId());
				((Catalog)mcat).setSize((int)cdsClient.getCatalogSolutionCount(mcat.getCatalogId()));
			}
			for (String catid: toget) {
				MLPCatalog mcat = cdsClient.getCatalog(catid);
				if (mcat != null) {
					((Catalog)mcat).setSize((int)cdsClient.getCatalogSolutionCount(catid));
					catalogs.add(mcat);
				}
			}
		} catch (HttpStatusCodeException restx) {
			log.debug("getCatalogs failed {}: {}", restx, restx.getResponseBodyAsString());
			throw new ServiceException("Failed on getCatalogs", restx);
		}
		log.debug("getCatalogs: catalogs count {}", catalogs.size());
		return catalogs;
	}

	@Override
	public List<MLPSolution> getSolutions(String theCatalogId, ServiceContext theContext) throws ServiceException {
		log.debug("getSolutions with catalog {}", theCatalogId);
		ICommonDataServiceRestClient cdsClient = getClient(theContext);
		String[] catids = new String[] { theCatalogId };
		List<MLPSolution> solutions = allPages("getSolutionsInCatalogs", page -> cdsClient.getSolutionsInCatalogs(catids, page));
		log.debug("getSolutions: solutions count {}", solutions.size());
		return solutions;
	}

	@Override
	public Solution getSolution(String theSolutionId, ServiceContext theContext) throws ServiceException {
		log.trace("getSolution {}", theSolutionId);
		ICommonDataServiceRestClient cdsClient = getClient(theContext, true);
		try {
			Solution solution = (Solution)cdsClient.getSolution(theSolutionId);
			List<MLPSolutionRevision> revisions = getRevisions(theSolutionId, theContext);

			//we can expose this solution only if we can expose at least one revision
			if (revisions == null || revisions.isEmpty())
				return null;

			solution.setRevisions(revisions);
			solution.setPicture(cdsClient.getSolutionPicture(theSolutionId));
			return solution;
		}
		catch (HttpStatusCodeException restx) {
			if (Errors.isCDSNotFound(restx))
				return null;
			else
				throw new ServiceException("Failed to retrieve solution information", restx);
		}
	}

	@Override	
	public Solution putSolution(Solution theSolution, ServiceContext theContext) throws ServiceException {
		
		log.trace("putSolution {}", theSolution);
		ICommonDataServiceRestClient cdsClient = getClient(theContext, true);

		//we handle tags separately
		Set<MLPTag> tags = theSolution.getTags();
		theSolution.setTags(Collections.emptySet());

		try {
			if (theSolution.getCreated() == TimestampedEntity.ORIGIN) {
				theSolution = (Solution)cdsClient.createSolution(theSolution);	
			}
			else {
				cdsClient.updateSolution(theSolution);
			}
			cdsClient.saveSolutionPicture(theSolution.getSolutionId(), theSolution.getPicture());
		}
		catch (HttpStatusCodeException scx) {
			log.error("CDS solution call failed. CDS says " + scx.getResponseBodyAsString(), scx);
			throw new ServiceException("CDS solution call failed. CDS says " + scx.getResponseBodyAsString(), scx);
		}
		catch (Exception x) {
			log.error("Solution handling unexpected failure", x);
			throw new ServiceException("Solution handling unexpected failure", x);
		}

		//tags: best effort approach
		for (MLPTag tag: tags) {
			try {
				cdsClient.addSolutionTag(theSolution.getSolutionId(), tag.getTag());
			}
			catch (HttpStatusCodeException scx) {
				//we ignore and keep trying
				log.error("CDS solution add tag call failed. CDS says " + scx.getResponseBodyAsString(), scx);
			}
		}

		//set back the tags; should we only set back those that succeded ?
		theSolution.setTags(tags);

		return theSolution;
	}

	@Override
	public List<MLPSolutionRevision> getRevisions(String theSolutionId, ServiceContext theContext) throws ServiceException {

		log.trace("getRevisions {}", theSolutionId);
		try {
			List<MLPSolutionRevision> revisions = getClient(theContext).getSolutionRevisions(theSolutionId);
			return revisions;
		} catch (HttpStatusCodeException restx) {
			if (Errors.isCDSNotFound(restx)) {
				return null;
			} else {
				throw new ServiceException("Failed to retrieve solution revision information", restx);
			}
		}
	}


	@Override
	public SolutionRevision getRevision(String theCatalogId, String theSolutionId, String theRevisionId, ServiceContext theContext) throws ServiceException {
		log.trace("getRevision");
		ICommonDataServiceRestClient cdsClient = getClient(theContext, true);
		try {
			SolutionRevision revision = (SolutionRevision)cdsClient.getSolutionRevision(theSolutionId, theRevisionId);
			revision.setArtifacts(getArtifacts(theSolutionId, theRevisionId, theContext));
			if (theCatalogId != null) {
				revision.setDocuments(getDocuments(theCatalogId, theRevisionId, theContext));
				try {
					revision.setRevCatDescription(cdsClient.getRevCatDescription(theRevisionId, theCatalogId));
				}
				catch (HttpStatusCodeException restx) {
					if (!Errors.isCDSNotFound(restx)) {
						throw new ServiceException("Failed to retrieve solution revision description", restx);
					}
				}
			}
			return revision;
		}	
		catch (HttpStatusCodeException restx) {
			if (Errors.isCDSNotFound(restx))
				return null;
			else
				throw new ServiceException("Failed to retrieve solution revision information", restx);
		}
	}

	@Override
	public SolutionRevision putRevision(SolutionRevision theRevision, ServiceContext theContext) throws ServiceException {
		log.trace("putRevision {}", theRevision);
	
		try {
			if (theRevision.getCreated() == TimestampedEntity.ORIGIN) {
				theRevision = (SolutionRevision)getClient(theContext).createSolutionRevision(theRevision);
    	}
			else {
				getClient(theContext).updateSolutionRevision(theRevision);
			}
		}
		catch (HttpStatusCodeException scx) {
			log.error("CDS solution revision call failed. CDS says " + scx.getResponseBodyAsString(), scx);
			throw new ServiceException("CDS solution revision call failed. CDS says " + scx.getResponseBodyAsString(), scx);
		}
		catch (Exception x) {
			log.error("Solution revision handling unexpected failure", x);
			throw new ServiceException("Solution revision handling unexpected failure", x);
		}

		return theRevision;
	}

	@Override
	public List<MLPArtifact> getArtifacts(String theSolutionId, String theRevisionId, ServiceContext theContext) throws ServiceException {

		log.trace("getArtifacts");
		try {
			return getClient(theContext).getSolutionRevisionArtifacts(theSolutionId, theRevisionId);
		}
		catch (HttpStatusCodeException restx) {
			if (Errors.isCDSNotFound(restx))
				return null;
			else
				throw new ServiceException("Failed to retrieve solution revision artifacts information", restx);
		}
	}

	/**
	 * @return catalog artifact representation
	 * @throws ServiceException if failing to retrieve artifact information or retrieve content 
	 */
	@Override
	public Artifact getArtifact(String theArtifactId, ServiceContext theContext) throws ServiceException {

		log.trace("getArtifact");
		try {
			return (Artifact)getClient(theContext).getArtifact(theArtifactId);
		}	
		catch (HttpStatusCodeException restx) {
			if (Errors.isCDSNotFound(restx))
				return null;
			else
				throw new ServiceException("Failed to retrieve solution revision artifact information", restx);
		}
	}

	@Override
	public List<MLPDocument> getDocuments(String theCatalogId, String theRevisionId, ServiceContext theContext) throws ServiceException {
		log.trace("getDocuments");
		try {
			return getClient(theContext).getRevisionCatalogDocuments(theRevisionId, theCatalogId);
		}
		catch (HttpStatusCodeException restx) {
			if (Errors.isCDSNotFound(restx))
				return null;
			else
				throw new ServiceException("Failed to retrieve solution revision documents information", restx);
		}
	}

	@Override
	public Document getDocument(String theDocumentId, ServiceContext theContext) throws ServiceException {
		log.trace("getDocument");
		try {
			return (Document)getClient(theContext).getDocument(theDocumentId);
		}	
		catch (HttpStatusCodeException restx) {
			if (Errors.isCDSNotFound(restx))
				return null;
			else
				throw new ServiceException("Failed to retrieve solution revision document information", restx);
		}
	}

	@Override
	public boolean isCatalogAllowed(String theCatalogId, ServiceContext theContext) {
		return getClient().isPeerAccessToCatalog(getPeerId(theContext), theCatalogId);
	}

	@Override
	public boolean isSolutionAllowed(String theSolutionId, ServiceContext theContext) {
		return getClient().isPeerAccessToSolution(getPeerId(theContext), theSolutionId);
	}

	@Override
	public boolean isRevisionAllowed(String theRevisionId, ServiceContext theContext) {
		try {
			ICommonDataServiceRestClient cdsClient = getClient(theContext, true);
			SolutionRevision revision = (SolutionRevision)cdsClient.getSolutionRevision("ignored", theRevisionId);
			return isSolutionAllowed(revision.getSolutionId(), theContext);
		} catch (Exception e) {
			log.error("Error checking revision access for revision " + theRevisionId + " blocking access", e);
			return false;
		}
	}

	@Override
	public boolean isArtifactAllowed(String theArtifactId, ServiceContext theContext) {
		return true;
	}

	@Override
	public boolean isDocumentAllowed(String theDocumentId, ServiceContext theContext) {
		return true;
	}

	private static String getPeerId(ServiceContext theContext) {
		return theContext.getPeer().getPeerInfo().getPeerId();
	}

	private <T> List<T> allPages(String opname, Function<RestPageRequest, RestPageResponse<T>> fcn) throws ServiceException {
		RestPageRequest request = new RestPageRequest(0, cdsConfig.getPageSize());
		List<T> ret = new ArrayList<>();
		while (true) {
			try {
				RestPageResponse<T> response = fcn.apply(request);
				List<T> returned = response.getContent();
				log.debug("{} returned {}", opname, returned);
				request.setPage(response.getNumber() + 1);
				ret.addAll(returned);
				if (response.isLast()) {
					break;
				}
			} catch (HttpStatusCodeException restx) {
				if (Errors.isCDSNotFound(restx)) {
					break;
				}
				log.debug("{} failed {}: {}", opname, restx, restx.getResponseBodyAsString());
				throw new ServiceException("Failed on " + opname, restx);
			}
		}
		return ret;
	}
}

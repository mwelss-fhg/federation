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

/**
 * 
 */
package org.acumos.federation.gateway.service.impl;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.acumos.cds.AccessTypeCode;
import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPDocument;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.cds.domain.MLPTag;
import org.acumos.cds.transport.RestPageRequest;
import org.acumos.cds.transport.RestPageResponse;
import org.acumos.federation.gateway.cds.AccessType;
import org.acumos.federation.gateway.cds.Artifact;
import org.acumos.federation.gateway.cds.Document;
import org.acumos.federation.gateway.cds.Solution;
import org.acumos.federation.gateway.cds.SolutionRevision;
import org.acumos.federation.gateway.cds.TimestampedEntity;
import org.acumos.federation.gateway.service.CatalogService;
import org.acumos.federation.gateway.service.CatalogServiceConfiguration;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.service.ServiceException;
import org.acumos.federation.gateway.util.Errors;
import org.apache.commons.beanutils.PropertyUtils;
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
public class CatalogServiceImpl extends AbstractServiceImpl
																implements CatalogService {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static final List<String> allATs = new ArrayList<String>();

	static {
		for (AccessType atc: AccessType.values()) {
			allATs.add(atc.code());
		}
	}

	@Autowired
	private CatalogServiceConfiguration config;

	@PostConstruct
	public void initService() {
	}

	@Override
	public List<MLPSolution> getSolutions(Map<String, ?> theSelector, ServiceContext theContext) throws ServiceException {
		log.debug("getSolutions with selector {}", theSelector);

		Map<String, Object> selector = new HashMap<String, Object>(this.config.getSolutionsSelectorDefaults());
		if (theSelector != null)
			selector.putAll(theSelector);
		//it is essential that this gets done at the end as to force all baseSelector criteria (otherwise a submitted accessTypeCode
		//could overwrite the basic one end expose non public solutions ..).
		selector.putAll(this.config.getSolutionsSelector());
		log.debug("getSolutions with full selector {}", selector);

		RestPageRequest pageRequest = new RestPageRequest(0, this.cdsConfig.getPageSize());
		RestPageResponse<MLPSolution> pageResponse = null;
		List<MLPSolution> solutions = new ArrayList<MLPSolution>(),
												pageSolutions = null;
		ICommonDataServiceRestClient cdsClient = getClient(theContext);
		try {
			Predicate<MLPSolution> matcher = ServiceImpl.compileSelector(selector);
			String catid = (String)selector.get(Solution.Fields.catalogId);
			Function<RestPageRequest, RestPageResponse<MLPSolution>> pager = null;
			if (catid != null) {
				pager = page -> cdsClient.getSolutionsInCatalog(catid, page);
			} else {
				boolean active = (Boolean)selector.getOrDefault(Solution.Fields.active, Boolean.TRUE);
				Object o = selector.getOrDefault(Solution.Fields.accessTypeCode, allATs);
				String[] codes = null;
				if (o instanceof String) {
					codes = new String[] { (String)o };
				} else {
					codes = ((List<String>)o).toArray(new String[0]);
				}
				String[] xcodes = codes;
				Instant since = Instant.ofEpochSecond((Long)selector.get(Solution.Fields.modified));
				pager = page -> cdsClient.findSolutionsByDate(active, xcodes, since, page);
			}
			do {
				log.debug("getSolutions page {}", pageResponse);
				pageResponse = pager.apply(pageRequest);
			
				log.debug("getSolutions page response {}", pageResponse);
				//we need to post-process all other selection criteria
				pageSolutions = pageResponse.getContent().stream()
															.filter(matcher)
															.collect(Collectors.toList());
				log.debug("getSolutions page selection {}", pageSolutions);
		
				pageRequest.setPage(pageResponse.getNumber() + 1);
				solutions.addAll(pageSolutions);
			} while (!pageResponse.isLast());
		}
		catch (HttpStatusCodeException restx) {
			if (Errors.isCDSNotFound(restx))
				return Collections.EMPTY_LIST;
			else {
				log.debug("getSolutions failed {}: {}", restx, restx.getResponseBodyAsString());
				throw new ServiceException("Failed to retrieve solutions", restx);
			}
		}

		log.debug("getSolutions: solutions count {}", solutions.size());
		return solutions;
	}

	@Override
	public Solution getSolution(String theSolutionId, ServiceContext theContext) throws ServiceException {

		log.trace("getSolution {}", theSolutionId);
		ICommonDataServiceRestClient cdsClient = getClient(theContext, true);
		try {
			Solution solution = (Solution)cdsClient.getSolution(theSolutionId);
			List<MLPSolutionRevision> revisions = getSolutionRevisions(theSolutionId, theContext);

			//we can expose this solution only if we can expose at least one revision
			if (revisions == null || revisions.isEmpty())
				return null;

			solution.setRevisions(revisions);
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
		theSolution.setTags(Collections.EMPTY_SET);

		try {
			if (theSolution.getCreated() == TimestampedEntity.ORIGIN) {
				theSolution = (Solution)cdsClient.createSolution(theSolution);	
			}
			else {
				cdsClient.updateSolution(theSolution);
			}
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
	public List<MLPSolutionRevision> getSolutionRevisions(String theSolutionId, ServiceContext theContext) throws ServiceException {

		log.trace("getSolutionRevisions {}", theSolutionId);
		try {
			List<MLPSolutionRevision> revisions = getClient(theContext).getSolutionRevisions(theSolutionId);
			//make sure we only expose revisions according to the filter
			if (revisions != null) {
				log.trace("getSolutionRevisions {}: got {} revisions", theSolutionId, revisions.size());
				revisions = 
					revisions.stream()
									 .filter(revision -> this.config.getSolutionRevisionsSelector().entrySet().stream()
																				.allMatch(s -> {
																					try {
																						log.trace("getSolutionRevisions verifying filter: revision property value {} vs filter value {}", PropertyUtils.getProperty(revision, s.getKey()), s.getValue());
																						return PropertyUtils.getProperty(revision, s.getKey()).equals(s.getValue());
																					} 
																					catch (Exception x) { 
																						log.trace("getSolutionRevisions failed to verify filter", x);
																						return false;
																					}
																				})
													)
									 .collect(Collectors.toList());
			}
			return revisions;
		}
		catch (HttpStatusCodeException restx) {
			if (Errors.isCDSNotFound(restx))
				return null;
			else
				throw new ServiceException("Failed to retrieve solution revision information", restx);
		}
	}


	@Override
	public SolutionRevision getSolutionRevision(String theSolutionId, String theRevisionId,
			ServiceContext theContext) throws ServiceException {

		log.trace("getSolutionRevision");
		ICommonDataServiceRestClient cdsClient = getClient(theContext, true);
		try {
			SolutionRevision revision =
					(SolutionRevision)cdsClient.getSolutionRevision(theSolutionId, theRevisionId);
			revision.setArtifacts(getSolutionRevisionArtifacts(theSolutionId, theRevisionId, theContext));
			revision.setDocuments(getSolutionRevisionDocuments(theSolutionId, theRevisionId, theContext));
			try {
				revision.setRevisionDescription(cdsClient.getRevisionDescription(theRevisionId, AccessTypeCode.PB.name()));
			}
			catch (HttpStatusCodeException restx) {
				if (!Errors.isCDSNotFound(restx))
					throw new ServiceException("Failed to retrieve solution revision description", restx);
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
  public SolutionRevision putSolutionRevision(SolutionRevision theRevision, ServiceContext theContext)
																																																				throws ServiceException {
		log.trace("putSolutionRevision {}", theRevision);
	
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
	public List<MLPArtifact> getSolutionRevisionArtifacts(String theSolutionId, String theRevisionId,
			ServiceContext theContext) throws ServiceException {

		log.trace("getSolutionRevisionArtifacts");
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
	public Artifact getSolutionRevisionArtifact(String theArtifactId, ServiceContext theContext) throws ServiceException {

		log.trace("getSolutionRevisionArtifact");
		try {
			//one should check that this belongs to at least one public revision of some solution accessible within the given context ..
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
	public List<MLPDocument> getSolutionRevisionDocuments(String theSolutionId, String theRevisionId, ServiceContext theContext) throws ServiceException {
		log.trace("getSolutionRevisionDocuments");
		try {
			return getClient(theContext).getSolutionRevisionDocuments(theRevisionId, AccessTypeCode.PB.name());
		}
		catch (HttpStatusCodeException restx) {
			if (Errors.isCDSNotFound(restx))
				return null;
			else
				throw new ServiceException("Failed to retrieve solution revision documents information", restx);
		}
	}

	@Override
	public Document getSolutionRevisionDocument(String theDocumentId, ServiceContext theContext) throws ServiceException {
		log.trace("getSolutionRevisionDocument");
		try {
			//one should check that this has a public visibility within at least one revision of some solution accessible within the given context ..
			return (Document)getClient(theContext).getDocument(theDocumentId);
		}	
		catch (HttpStatusCodeException restx) {
			if (Errors.isCDSNotFound(restx))
				return null;
			else
				throw new ServiceException("Failed to retrieve solution revision document information", restx);
		}
	}

}

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
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.acumos.cds.AccessTypeCode;
import org.acumos.cds.ValidationStatusCode;
import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.domain.MLPTag;
import org.acumos.cds.domain.MLPDocument;
import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.cds.transport.RestPageRequest;
import org.acumos.cds.transport.RestPageResponse;
import org.acumos.federation.gateway.cds.Document;
import org.acumos.federation.gateway.cds.Artifact;
import org.acumos.federation.gateway.cds.Solution;
import org.acumos.federation.gateway.cds.SolutionRevision;
import org.acumos.federation.gateway.cds.AccessType;
import org.acumos.federation.gateway.cds.ValidationStatus;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.service.CatalogService;
import org.acumos.federation.gateway.service.CatalogServiceConfiguration;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.service.ServiceException;
import org.acumos.federation.gateway.util.Errors;
import org.apache.commons.beanutils.PropertyUtils;
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

	private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private CatalogServiceConfiguration config;

	@PostConstruct
	public void initService() {
	}

	@Override
	public List<MLPSolution> getSolutions(Map<String, ?> theSelector, ServiceContext theContext) throws ServiceException {
		log.debug(EELFLoggerDelegate.debugLogger, "getSolutions with selector {}", theSelector);

		Map<String, Object> selector = new HashMap<String, Object>(this.config.getSolutionsSelectorDefaults());
		if (theSelector != null)
			selector.putAll(theSelector);
		//it is essential that this gets done at the end as to force all baseSelector criteria (otherwise a submitted accessTypeCode
		//could overwrite the basic one end expose non public solutions ..).
		selector.putAll(this.config.getSolutionsSelector());
		log.debug(EELFLoggerDelegate.debugLogger, "getSolutions with full selector {}", selector);

		RestPageRequest pageRequest = new RestPageRequest(0, this.cdsConfig.getPageSize());
		RestPageResponse<MLPSolution> pageResponse = null;
		List<MLPSolution> solutions = new ArrayList<MLPSolution>(),
											pageSolutions = null;
		ICommonDataServiceRestClient cdsClient = getClient(theContext);
		try {
			do {
				log.debug(EELFLoggerDelegate.debugLogger, "getSolutions page {}", pageResponse);
				pageResponse =
					cdsClient.findSolutionsByDate(
						(Boolean)selector.getOrDefault(Solution.Fields.active, Boolean.TRUE),
						selector.containsKey(Solution.Fields.accessTypeCode) ?
							new String[] {selector.get(Solution.Fields.accessTypeCode).toString()} :
							Arrays.stream(AccessType.values()).map(at -> at.code()).toArray(String[]::new),
						selector.containsKey(Solution.Fields.validationStatusCode) ?
							new String[] {selector.get(Solution.Fields.validationStatusCode).toString()} :
							Arrays.stream(ValidationStatus.values()).map(vs -> vs.code()).toArray(String[]::new),
						new Date((Long)selector.get(Solution.Fields.modified)),
						pageRequest);
			
				log.debug(EELFLoggerDelegate.debugLogger, "getSolutions page response {}", pageResponse);
				//we need to post-process all other selection criteria
				pageSolutions = pageResponse.getContent().stream()
													.filter(solution -> ServiceImpl.isSelectable(solution, selector))
													.collect(Collectors.toList());
				log.debug(EELFLoggerDelegate.debugLogger, "getSolutions page selection {}", pageSolutions);
		
				pageRequest.setPage(pageResponse.getNumber() + 1);
				solutions.addAll(pageSolutions);
			}
			while (!pageResponse.isLast());
		}
		catch (HttpStatusCodeException restx) {
			if (Errors.isCDSNotFound(restx))
				return Collections.EMPTY_LIST;
			else {
				log.debug(EELFLoggerDelegate.debugLogger, "getSolutions failed {}: {}", restx, restx.getResponseBodyAsString());
				throw new ServiceException("Failed to retrieve solutions", restx);
			}
		}

		log.debug(EELFLoggerDelegate.debugLogger, "getSolutions: solutions count {}", solutions.size());
		return solutions;
	}

	@Override
	public Solution getSolution(String theSolutionId, ServiceContext theContext) throws ServiceException {

		log.trace(EELFLoggerDelegate.debugLogger, "getSolution {}", theSolutionId);
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
		
		log.trace(EELFLoggerDelegate.debugLogger, "putSolution {}", theSolution);
		ICommonDataServiceRestClient cdsClient = getClient(theContext, true);

		MLPSolution catalogSolution = null;
		try {
			catalogSolution = cdsClient.getSolution(theSolution.getSolutionId());
		}
		catch (HttpStatusCodeException scx) {
			if (!Errors.isCDSNotFound(scx)) {
				log.error(EELFLoggerDelegate.errorLogger, "Failed to check if solution " + theSolution.getSolutionId() + " exists in catalog. CDS says " + scx.getResponseBodyAsString(), scx);
       	throw new ServiceException("Failed to check if solution " + theSolution.getSolutionId() + " exists in catalog", scx);
			}
		}

		//we handle tags separately
		Set<MLPTag> tags = theSolution.getTags();
		theSolution.setTags(Collections.EMPTY_SET);
		//reset the web stats
		theSolution.setWebStats(null);

		try {
			if (catalogSolution == null) {
	 			log.info(EELFLoggerDelegate.debugLogger, "Solution {} does not exists in catalog, adding", theSolution.getSolutionId());
				catalogSolution = cdsClient.createSolution(theSolution);	
    	}
			else {
	 			log.info(EELFLoggerDelegate.debugLogger, "Solution {} exists in catalog, updating", theSolution.getSolutionId());
				//some basic warnings
				if (!catalogSolution.getUserId().equals(theSolution.getUserId())) {
					// is this solution being updated as part of different/new subscription?
					log.warn(EELFLoggerDelegate.errorLogger, "Updating solution {} triggers a user change", catalogSolution.getSolutionId());
				}

				if (catalogSolution.getSourceId() == null) {
					//this is a local solution that made its way back
					log.info(EELFLoggerDelegate.debugLogger, "Solution {} was originally provisioned locally", catalogSolution.getSolutionId());
					//make sure not to update the user if the solution is local
					theSolution.setUserId(catalogSolution.getUserId());
				}
				else {
					if (!theSolution.getSourceId().equals(catalogSolution.getSourceId())) {
						// we will see this if a solution is available in more than one peer
						log.warn(EELFLoggerDelegate.errorLogger, "Solution {} triggers a source change", catalogSolution.getSolutionId());
					}
				}
				cdsClient.updateSolution(theSolution);
				catalogSolution = theSolution;
			}
		}
		catch (HttpStatusCodeException scx) {
			log.error(EELFLoggerDelegate.errorLogger,	"CDS solution call failed. CDS says " + scx.getResponseBodyAsString(), scx);
			throw new ServiceException("CDS solution call failed. CDS says " + scx.getResponseBodyAsString(), scx);
		}

		//tags: best effort approach
		for (MLPTag tag: tags) {
			try {
				cdsClient.addSolutionTag(catalogSolution.getSolutionId(), tag.getTag());
			}
			catch (HttpStatusCodeException scx) {
				//we ignore and keep trying
				log.error(EELFLoggerDelegate.errorLogger,	"CDS solution add tag call failed. CDS says " + scx.getResponseBodyAsString(), scx);
			}
		}
	
		return Solution.buildFrom(catalogSolution)
									 .withTags(tags) //this is not accurate as somem might have failed
									 .withWebStats(null)
									 .build();
	}

	@Override
	public List<MLPSolutionRevision> getSolutionRevisions(String theSolutionId, ServiceContext theContext) throws ServiceException {

		log.trace(EELFLoggerDelegate.debugLogger, "getSolutionRevisions {}", theSolutionId);
		try {
			List<MLPSolutionRevision> revisions = getClient(theContext).getSolutionRevisions(theSolutionId);
			//make sure we only expose revisions according to the filter
			if (revisions != null) {
				log.trace(EELFLoggerDelegate.debugLogger, "getSolutionRevisions {}: got {} revisions", theSolutionId, revisions.size());
				revisions = 
					revisions.stream()
									 .filter(revision -> this.config.getSolutionRevisionsSelector().entrySet().stream()
																				.allMatch(s -> {
																					try {
																						log.trace(EELFLoggerDelegate.debugLogger, "getSolutionRevisions verifying filter: revision property value {} vs filter value {}", PropertyUtils.getProperty(revision, s.getKey()), s.getValue());
																						return PropertyUtils.getProperty(revision, s.getKey()).equals(s.getValue());
																					} 
																					catch (Exception x) { 
																						log.trace(EELFLoggerDelegate.errorLogger, "getSolutionRevisions failed to verify filter", x);
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

		log.trace(EELFLoggerDelegate.debugLogger, "getSolutionRevision");
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
		log.trace(EELFLoggerDelegate.debugLogger, "putSolutionRevision {}", theRevision);
	
		MLPSolutionRevision catalogRevision = getSolutionRevision(theRevision.getSolutionId(), theRevision.getRevisionId(), theContext);
		try {
			if (catalogRevision == null) {
	 			log.info(EELFLoggerDelegate.debugLogger, "Revision {}/{} does not exists in catalog, adding", theRevision.getSolutionId(), theRevision.getRevisionId());
				catalogRevision = SolutionRevision.buildFrom(getClient(theContext).createSolutionRevision(theRevision))
																					.build();	
    	}
		}
		catch (HttpStatusCodeException scx) {
			log.error(EELFLoggerDelegate.errorLogger,	"CDS solution revision call failed. CDS says " + scx.getResponseBodyAsString(), scx);
			throw new ServiceException("CDS solution revision call failed. CDS says " + scx.getResponseBodyAsString(), scx);
		}

		return (SolutionRevision)catalogRevision;
	}

	@Override
	public List<MLPArtifact> getSolutionRevisionArtifacts(String theSolutionId, String theRevisionId,
			ServiceContext theContext) throws ServiceException {

		log.trace(EELFLoggerDelegate.debugLogger, "getSolutionRevisionArtifacts");
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

		log.trace(EELFLoggerDelegate.debugLogger, "getSolutionRevisionArtifact");
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
		log.trace(EELFLoggerDelegate.debugLogger, "getSolutionRevisionDocuments");
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
		log.trace(EELFLoggerDelegate.debugLogger, "getSolutionRevisionDocument");
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

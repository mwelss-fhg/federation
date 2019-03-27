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

package org.acumos.federation.gateway.adapter;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.domain.MLPCatalog;
import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPPeerSubscription;
import org.acumos.cds.domain.MLPRevCatDescription;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.cds.domain.MLPTag;
import org.acumos.federation.gateway.cds.Artifact;
import org.acumos.federation.gateway.cds.Document;
import org.acumos.federation.gateway.cds.PeerSubscription;
import org.acumos.federation.gateway.cds.Solution;
import org.acumos.federation.gateway.cds.SolutionRevision;
import org.acumos.federation.gateway.cds.TimestampedEntity;
import org.acumos.federation.gateway.common.API;
import org.acumos.federation.gateway.common.Clients;
import org.acumos.federation.gateway.common.FederationClient;
import org.acumos.federation.gateway.common.FederationClient.StreamingResource;
import org.acumos.federation.gateway.common.FederationException;
import org.acumos.federation.gateway.common.JsonResponse;
import org.acumos.federation.gateway.config.GatewayCondition;
import org.acumos.federation.gateway.event.PeerSubscriptionEvent;
import org.acumos.federation.gateway.service.CatalogService;
import org.acumos.federation.gateway.service.CatalogServiceConfiguration;
import org.acumos.federation.gateway.service.ContentService;
import org.acumos.federation.gateway.service.PeerSubscriptionService;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.service.ServiceException;
import org.acumos.federation.gateway.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

import org.omg.CORBA.BooleanHolder;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPDocument;


@Component("peergateway")
@Scope("singleton")
@Conditional({GatewayCondition.class})
public class PeerGateway {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	@Value("${federation.operator}")
	private String defaultUser;
	@Autowired
	@Qualifier("acumos")
	private TaskExecutor taskExecutor;
	@Autowired
	private Clients clients;
	@Autowired
	private ContentService content;
	@Autowired
	private CatalogService catalog;
	@Autowired
	private CatalogServiceConfiguration catalogConfig;
	@Autowired
	private PeerSubscriptionService peerSubscriptionService;


	public PeerGateway() {
		log.trace("PeerGateway::new");
	}

	@PostConstruct
	public void initGateway() {
		log.trace("initPeerGateway");
		/* make sure an operator was specified and that it is a declared user */
		try {
			if (this.clients.getCDSClient().getUser(defaultUser) == null) {
				log.warn("The default federation operator {} is not a known user ID", defaultUser);
			}
		} catch (Exception dx) {
			log.warn("failed to verify default federation operator", dx);
		}
		log.debug("PeerGateway available");
	}

	@PreDestroy
	public void cleanupGateway() {
		log.debug("PeerGateway destroyed");
	}

	protected String getUserId(MLPPeerSubscription theSubscription) {
		String userId = theSubscription.getUserId();
		return userId != null ? userId : defaultUser;
	}

	@EventListener
	public void handlePeerSubscriptionUpdate(PeerSubscriptionEvent theEvent) {
		log.info("received peer subscription update event {}", theEvent);
		taskExecutor.execute(
				new PeerGatewayUpdateTask(theEvent.getPeer(), theEvent.getSubscription()));
	}

	/**
	 * The list of solutions processed here represents the solutions (with respect
	 * to the subscription filter definition) that were reported by the peer as
	 * being updated since the last check.
	 */
	public class PeerGatewayUpdateTask implements Runnable {

		private MLPPeer peer;
		private PeerSubscription sub;
		private FederationClient peerClient;

		public PeerGatewayUpdateTask(MLPPeer thePeer, MLPPeerSubscription theSub) {
			this.peer = thePeer;
			this.sub = new PeerSubscription(theSub);
		}

		public void run() {
			peerClient = clients.getFederationClient(this.peer.getApiUrl());
			if (peerClient == null) {
				log.error("Failed to get client for peer {}", this.peer);
				return;
			}
			Map selector = null;
			try {
				selector = Utils.jsonStringToMap(this.sub.getSelector());
			}
			catch(Exception x) {
				log.error("Failed to parse selector for subscription {}", this.sub);
				return;
			}
			Instant lastProcessed = Instant.now();
			boolean isComplete = true;
			Object catids = selector.get(API.QueryParameters.CATALOG_ID);
			if (catids instanceof String) {
				isComplete &= scanCatalog((String)catids);
			} else if (catids instanceof String[]) {
				for (String catid: (String[]) catids) {
					isComplete &= scanCatalog(catid);
				}
			} else {
				log.error("Selector for subscription {} needs catalog ID(s)", this.sub);
				return;
			}
			log.info("Processing of subscription {} completed succesfully: {}", this.sub, isComplete);
			//only commit the last processed date if we completed succesfully
			if (isComplete) {
				try {
					this.sub.setProcessed(lastProcessed);
					peerSubscriptionService.updatePeerSubscription(this.sub);
				}
				catch (ServiceException sx) {
					log.error("Failed to update subscription information", sx);
				}
			}
		}
		private boolean scanCatalog(String theCatalogId) {

			JsonResponse<List<MLPSolution>> peerSolutionsResponse = null;
			try {
				peerSolutionsResponse = peerClient.getSolutions(theCatalogId);
			}
			catch (FederationException fx) {
				log.error("Processing peer " + this.peer + " subscription " + this.sub.getSubId() + " error.", fx);
				return false;
			}
			List<MLPSolution> peerSolutions = peerSolutionsResponse.getContent();
			Set<String> localSolutions = new HashSet<>();
			try {
				MLPCatalog localCatalog = catalog.getCatalogs().stream().filter(x -> x.getCatalogId().equals(theCatalogId)).findAny().orElse(null);
				if (localCatalog == null) {
					log.info("Subscription local catalog id {} missing: trying to create it", theCatalogId);
					MLPCatalog peerCatalog = peerClient.getCatalogs().getContent().stream().filter(x -> x.getCatalogId().equals(theCatalogId)).findAny().orElse(null);
					localCatalog = clients.getCDSClient().createCatalog(peerCatalog);
				}
				for (MLPSolution solution: catalog.getSolutions(theCatalogId)) {
					localSolutions.add(solution.getSolutionId());
				}
			} catch (FederationException fe) {
				log.error("Failed to retrieve peer catalog " + theCatalogId, fe);
				return false;
			} catch (ServiceException se) {
				log.error("Failed to list solutions in local catalog " + theCatalogId, se);
				return false;
			}
			log.info("Processing peer {} subscription {}, {} yielded solutions {}", this.peer, this.sub.getSubId(), theCatalogId, peerSolutions);
			if (peerSolutions == null) {
				log.warn("No solutions available for peer {} subscription {} in {}", this.peer, this.sub.getSubId(), peerSolutionsResponse);
				peerSolutions = Collections.emptyList();
				//and let it proceed so we end up marking it as processed
			}

			ServiceContext ctx = catalog.selfService();
			boolean isComplete = true;

			for (MLPSolution peerSolution : peerSolutions) {
				log.info("Processing peer solution {}", peerSolution);

				try {
					isComplete &= mapSolution(theCatalogId, peerSolution, !localSolutions.contains(peerSolution.getSolutionId()), ctx);
				}
				catch (Throwable t) {
					log.error("Mapping of acumos solution failed for " + peerSolution, t);
				}
			}
			return isComplete;
		}

		//this should go away once the move to service interface based operations is complete
		//as ugly as they come
		private ICommonDataServiceRestClient getCDSClient() {
			return PeerGateway.this.clients.getCDSClient();
		}

		public Artifact copyArtifact(Artifact peerArtifact) {
			return Artifact.buildFrom(peerArtifact)
								.withUser(getUserId(this.sub))
								.withCreated(TimestampedEntity.ORIGIN)
								.withModified(TimestampedEntity.ORIGIN)
								.build();
		}

		/* we create a new one as nothing is preserved. assumes matching ids. */
		public Artifact copyArtifact(Artifact peerArtifact, Artifact localArtifact) {
			return Artifact.buildFrom(peerArtifact)
								.withId(localArtifact.getArtifactId())
								.withUser(getUserId(this.sub))
								.build();
		}

		public Document copyDocument(Document peerDocument) {
			return Document.buildFrom(peerDocument)
								.withUser(getUserId(this.sub))
								.withCreated(TimestampedEntity.ORIGIN)
								.withModified(TimestampedEntity.ORIGIN)
								.build();
		}

		public Document copyDocument(Document peerDocument, Document localDocument) {
			return Document.buildFrom(peerDocument)
								.withId(localDocument.getDocumentId())
								.withUser(getUserId(this.sub))
								.build();
		}

		private MLPRevCatDescription copyRevCatDescription(MLPRevCatDescription peerDescription) {
			MLPRevCatDescription localDescription = new MLPRevCatDescription(peerDescription);
			localDescription.setCreated(TimestampedEntity.ORIGIN);
			localDescription.setModified(TimestampedEntity.ORIGIN);
			return localDescription;
		}

		private MLPRevCatDescription copyRevCatDescription(MLPRevCatDescription peerDescription, MLPRevCatDescription localDescription) {
			localDescription.setDescription(peerDescription.getDescription());
			return localDescription;
		}

		private void putRevCatDescription(MLPRevCatDescription theDescription) throws ServiceException {

			try {
				if (theDescription.getCreated() == Instant.MIN) {
					getCDSClient().createRevCatDescription(theDescription);
					log.info("Local description created: {}", theDescription);
				}
				else {
					getCDSClient().updateRevCatDescription(theDescription);
				}
			}
			catch (HttpStatusCodeException restx) {
				log.error("Revision description CDS call failed. CDS message is " + restx.getResponseBodyAsString(), restx);
				throw new ServiceException("Revision description CDS call failed.", restx);
			}
			catch (Exception x) {
				log.error("Revision description handling unexpected failure", x);
				throw new ServiceException("Revision description handling unexpected failure", x);
			}
		}

		private Solution copySolution(Solution thePeerSolution) {
			return Solution.buildFrom(thePeerSolution)
								.withCreated(TimestampedEntity.ORIGIN)
								.withModified(TimestampedEntity.ORIGIN)
								.withUser(getUserId(this.sub))
								.withSource(this.peer.getPeerId())
								.withPicture(thePeerSolution.getPicture())
								.resetStats()
								.build();
		}

		private Solution copySolution(Solution thePeerSolution, Solution theLocalSolution) {
			String newUserId = getUserId(this.sub);
			String newSourceId = this.peer.getPeerId();

			//some basic warnings
			if (!theLocalSolution.getUserId().equals(newUserId)) {
				// is this solution being updated as part of different/new subscription?
				log.warn("Updating solution {} triggers a user change", theLocalSolution.getSolutionId());
				//but make the change anyway
				theLocalSolution.setUserId(newUserId);
			}

			if (theLocalSolution.getSourceId() == null) {
				//this is a local solution that made its way back
				log.info("Solution {} was originally provisioned locally, avoiding user update", theLocalSolution.getSolutionId());
			}
			else {
				if (!theLocalSolution.getSourceId().equals(newSourceId)) {
					// we will see this if a solution is available in more than one peer
					log.warn("Solution {} triggers a source change", theLocalSolution.getSolutionId());
					//but make the change anyway
					theLocalSolution.setSourceId(newSourceId);
				}
			}

			theLocalSolution.setPicture(thePeerSolution.getPicture());
			//tags, keep only the delta
			Set<MLPTag> tags = thePeerSolution.getTags();
			tags.removeAll(theLocalSolution.getTags());
			theLocalSolution.setTags(tags);

			return theLocalSolution;
		}

		private boolean hasChanged(Solution thePeerSolution, Solution theLocalSolution) {
			if (!Arrays.equals(theLocalSolution.getPicture(), thePeerSolution.getPicture())) {
				return true;
			}
			if (!theLocalSolution.getTags().containsAll(thePeerSolution.getTags()))
				return false;

			return true;
		}

		/**
		 * Here comes the core process of updating a local solution's related
		 * information with what is available from a peer.
		 *
		 * @param theCatalogId
		 *            the catalog containing the solution for fetching
		 *            revision descriptions and documents
		 * @param theSolution
		 *            the local solution who's related information (revisions and
		 *            artifacts) we are trying to sync
		 * @param addToCatalog
		 *            true if solution is not yet in local catalog
		 * @param theContext
		 *            the context in which we perform the catalog operations
		 * @return true if mapping was succesful, false otherwise
		 * @throws Exception
		 *             any error related to CDS and peer interaction
		 */
		protected boolean mapSolution(String theCatalogId, MLPSolution theSolution, boolean addToCatalog, ServiceContext theContext) throws Exception {

			boolean isComplete = true;
			boolean isSolutionNew = false;
			boolean hasSolutionChanged = false;

			Solution localSolution = null;
			Solution peerSolution = null;

			//retrieve the full representation from the peer
			JsonResponse<MLPSolution> peerSolutionResponse = null;
			try {
				peerSolutionResponse = peerClient.getSolution(theSolution.getSolutionId());
			}
			catch (FederationException fx) {
				log.warn("Failed to retrieve peer solution details for " + theSolution, fx);
				return false;
			}

			peerSolution = (Solution)peerSolutionResponse.getContent();
			if (peerSolution == null) {
				log.warn("No solution details available for {} in {}", theSolution, peerSolutionResponse);
				return false;
			}

			localSolution = catalog.getSolution(peerSolution.getSolutionId());
			if (localSolution == null) {
				localSolution = catalog.putSolution(copySolution(peerSolution), theContext);
				isSolutionNew = true;
			}
			else {
				hasSolutionChanged = hasChanged(peerSolution, localSolution);
			}

			if (addToCatalog) {
				clients.getCDSClient().addSolutionToCatalog(localSolution.getSolutionId(), theCatalogId);
			}
			List<MLPSolutionRevision> peerRevisions = peerSolution.getRevisions();
			Collections.sort(peerRevisions, (arev, brev) -> arev.getModified().compareTo(brev.getModified()));

			// this should not happen as any solution should have at least one
			// revision (but that's an assumption on how on-boarding works)
			if (peerRevisions == null || peerRevisions.isEmpty()) {
				log.warn("No solution revisions were retrieved from the peer");
				return true;
			}

			// check if we have locally the latest revision available on the peer
			List<MLPSolutionRevision> catalogRevisions = localSolution.getRevisions();
			final List<MLPSolutionRevision> localRevisions = catalogRevisions == null ? Collections.emptyList() : catalogRevisions;

			// map peer revisions to local ones; new peer revisions have a null mapping
			Map<MLPSolutionRevision, MLPSolutionRevision> peerToLocalRevisions =
					new LinkedHashMap<>();
			peerRevisions.forEach(peerRevision -> peerToLocalRevisions.put(peerRevision,
					localRevisions.stream()
							.filter(localRevision -> localRevision.getRevisionId().equals(peerRevision.getRevisionId()))
							.findFirst().orElse(null)));

			for (Map.Entry<MLPSolutionRevision, MLPSolutionRevision> revisionEntry : peerToLocalRevisions.entrySet()) {
				MLPSolutionRevision peerRevision = revisionEntry.getKey();
				MLPSolutionRevision localRevision = revisionEntry.getValue();
				boolean isRevisionNew = false;

				//revision related information (artifacts/documents/description/..) is now embedded in the revision details
				//federation api call so one call is all is needed
				JsonResponse<MLPSolutionRevision> peerRevisionResponse = null;
				try {
					peerRevisionResponse = peerClient.getSolutionRevision(peerSolution.getSolutionId(), peerRevision.getRevisionId(), theCatalogId);
				}
				catch (FederationException fx) {
					isComplete = false; //try the next revision but mark the overall processing as incomplete
					continue;
				}

				peerRevision = peerRevisionResponse.getContent();
				if (peerRevision == null) {
					isComplete = false; //try the next revision but mark the overall processing as incomplete
					continue;
				}

				if (localRevision == null) {
					try {
						localRevision = catalog.putRevision(
						    SolutionRevision.buildFrom(peerRevision)
							.withCreated(TimestampedEntity.ORIGIN)
							.withModified(TimestampedEntity.ORIGIN)
							.withUser(getUserId(this.sub))
							.withSource(this.peer.getPeerId())
																	.build(), theContext);
					}
					catch (ServiceException sx) {
						log.error("Failed to put revision " + theSolution.getSolutionId() + "/" + peerRevision.getRevisionId() + " into catalog", sx);
						isComplete = false; //try procecessing the next revision but mark the processing as incomplete
						continue;
					}
					isRevisionNew = true;
				}

				BooleanHolder hasRevChanged = new BooleanHolder(false);
				isComplete &= artifactsHandler.handle(theCatalogId, peerRevision, localRevision, this, hasRevChanged);
				isComplete &= documentsHandler.handle(theCatalogId, peerRevision, localRevision, this, hasRevChanged);
				boolean hasRevisionChanged = hasRevChanged.value;

				MLPRevCatDescription localDescription = ((SolutionRevision)localRevision).getRevCatDescription();
				MLPRevCatDescription peerDescription = ((SolutionRevision)peerRevision).getRevCatDescription();

				if (peerDescription != null) {
					boolean doCatalog = false;

					if (localDescription == null) {
						localDescription = copyRevCatDescription(peerDescription);
						doCatalog = true;
					}
					else {
						//is this a good enough test ?? it implies time sync ..
						if (peerDescription.getModified().isAfter(localDescription.getModified())) {
							localDescription = copyRevCatDescription(peerDescription, localDescription);
							doCatalog = true;
						}
					}

					if (doCatalog) {
						try {
							putRevCatDescription(localDescription);
						}
						catch (ServiceException sx) {
							log.error("Description processing failed",	sx);
							isComplete = false;
						}
						hasRevisionChanged = true;
					}
				} //end revision processing

				if (!isRevisionNew && hasRevisionChanged) {
					try {
						//we do not actually update any properties, just give CDS a chance to update the timestamps as to mark it as updated.
						catalog.putRevision(SolutionRevision.buildFrom(localRevision).build(),
																				theContext);
					}
					catch (ServiceException sx) {
						log.error("Failed to update local revision",	sx);
						isComplete = false;
					}
				}

				hasSolutionChanged |= (isRevisionNew || hasRevisionChanged);
			} //end revisions processing

			if (!isSolutionNew && hasSolutionChanged) {
				try {
					catalog.putSolution(copySolution(peerSolution, localSolution), theContext);
				}
				catch (ServiceException sx) {
					log.error("Failed to update local solution",	sx);
					isComplete = false;
				}
			}

			return isComplete;
		} // mapSolution

		public boolean alwaysUpdateCatalog() {
			return sub.getSubscriptionOptions().alwaysUpdateCatalog();
		}

		public boolean copyDocumentContent(String solutionId, Document document) {
			try (StreamingResource data = peerClient.getDocumentContent(document.getDocumentId())) {
				content.putDocumentContent(solutionId, document, data);
				log.info("Received {} bytes of document content", data.contentLength());
				return true;
			} catch (FederationException fedex) {
				log.error("Failed to retrieve acumos document content", fedex);
			} catch (ServiceException srvex) {
				log.error("Failed to store document content to local repo", srvex);
			} catch (IOException ioex) {
				log.error("Exception receiving document content ", ioex);
			}
			return false;
		}

		public boolean copyArtifactContent(String solutionId, Artifact artifact) {
			try (StreamingResource data = peerClient.getArtifactContent(artifact.getArtifactId())) {
				content.putArtifactContent(solutionId, artifact, data);
				log.info("Received {} bytes of artifact content", data.contentLength());
				return true;
			} catch (FederationException fedex) {
				log.error("Failed to retrieve acumos artifact content", fedex);
			} catch (ServiceException srvex) {
				log.error("Failed to store artifact content to local repo", srvex);
			} catch (IOException ioex) {
				log.error("Exception receiving artifact content ", ioex);
			}
			return false;
		}
	}

	/*
	 * The process for federating Artifacts and Documents is the same,
	 * with just different method names and, in a few cases method
	 * arguments.  These interfaces and the ItemsHandler class are about
	 * factoring out that duplicated code.
	 */

	@FunctionalInterface
	private interface TriFunction<T, U, V, R> {
		R apply(T t, U u, V v);
	}

	@FunctionalInterface
	private interface TriPredicate<T, U, V> {
		boolean test(T t, U u, V v);
	}

	@FunctionalInterface
	private interface QuadConsumer<T, U, V, W> {
		void accept(T t, U u, V v, W w);
	}

	private static class ItemsHandler<T>	{
		// get list of items to process
		private Function<SolutionRevision, List<T>> getList;
		// get ID of an item
		private Function<T, String> getId;
		// get item from local data store
		private BiFunction<ICommonDataServiceRestClient, String, T> localGet;
		// link item to revision/catalog
		private QuadConsumer<ICommonDataServiceRestClient, MLPSolutionRevision, String, String> link;
		// copy item from peer
		private BiFunction<PeerGatewayUpdateTask, T, T> copy;
		// check if peer item updates local item
		private BiPredicate<T, T> changed;
		// merge peer and local items
		private TriFunction<PeerGatewayUpdateTask, T, T, T> merge;
		// transfer content
		private TriPredicate<PeerGatewayUpdateTask, String, T> copybody;
		// create item
		private BiConsumer<ICommonDataServiceRestClient, T> create;
		// update item
		private BiConsumer<ICommonDataServiceRestClient, T> update;

		public ItemsHandler(
		    Function<SolutionRevision, List<T>> getList,
		    Function<T, String> getId,
		    BiFunction<ICommonDataServiceRestClient, String, T> localGet,
		    QuadConsumer<ICommonDataServiceRestClient, MLPSolutionRevision, String, String> link,
		    BiFunction<PeerGatewayUpdateTask, T, T> copy,
		    BiPredicate<T, T> changed,
		    TriFunction<PeerGatewayUpdateTask, T, T, T> merge,
		    TriPredicate<PeerGatewayUpdateTask, String, T> copybody,
		    BiConsumer<ICommonDataServiceRestClient, T> create,
		    BiConsumer<ICommonDataServiceRestClient, T> update) {
			this.getList = getList;
			this.getId = getId;
			this.localGet = localGet;
			this.link = link;
			this.copy = copy;
			this.changed = changed;
			this.merge = merge;
			this.copybody = copybody;
			this.create = create;
			this.update = update;
		}

		public boolean handle(String catalogId, MLPSolutionRevision peer, MLPSolutionRevision local, PeerGatewayUpdateTask task, BooleanHolder updated) {
			ICommonDataServiceRestClient cdsClient = task.getCDSClient();
			HashMap<String, T> localItems = new HashMap();
			for (T item: getList.apply((SolutionRevision)local)) {
				localItems.put(getId.apply(item), item);
			}
			boolean success = true;
			for (T peerItem: getList.apply((SolutionRevision)peer)) {
				String itemId = getId.apply(peerItem);
				T localItem = localItems.get(itemId);
				log.info("Processing peer item {} against local item {}", peerItem, localItem);
				if (localItem == null) {
					localItem = localGet.apply(cdsClient, itemId);
					if (localItem != null) {
						link.accept(cdsClient, local, catalogId, itemId);
					}
				}
				boolean isNew = (localItem == null);
				if (isNew) {
					localItem = copy.apply(task, peerItem);
				} else if (changed.test(peerItem, localItem)) {
					localItem = merge.apply(task, peerItem, localItem);
				} else {
					continue;
				}
				if (!copybody.test(task, local.getSolutionId(), localItem)) {
					success = false;
					if (!task.alwaysUpdateCatalog()) {
						continue;
					}
				}
				if (isNew) {
					create.accept(cdsClient, localItem);
					link.accept(cdsClient, local, catalogId, itemId);
				} else {
					update.accept(cdsClient, localItem);
				}
				updated.value = true;
			}
			return(success);
		}
	}

	private static ItemsHandler<MLPDocument> documentsHandler = new ItemsHandler<MLPDocument>(
	    rev -> rev.getDocuments(),
	    doc -> doc.getDocumentId(),
	    (client, id) -> client.getDocument(id),
	    (client, rev, catid, itemid) -> client.addRevisionCatalogDocument(rev.getRevisionId(), catid, itemid),
	    (task, doc) -> task.copyDocument((Document)doc),
	    (peerdoc, localdoc) -> PeerGateway.hasChanged((Document)peerdoc, (Document)localdoc),
	    (task, peerdoc, localdoc) -> task.copyDocument((Document)peerdoc, (Document)localdoc),
	    (task, solid, doc) -> task.copyDocumentContent(solid, (Document)doc),
	    (client, doc) -> client.createDocument(doc),
	    (client, doc) -> client.updateDocument(doc));
	private static ItemsHandler<MLPArtifact> artifactsHandler = new ItemsHandler<MLPArtifact>(
	    rev -> rev.getArtifacts(),
	    art -> art.getArtifactId(),
	    (client, id) -> client.getArtifact(id),
	    (client, rev, catid, itemid) -> client.addSolutionRevisionArtifact(rev.getSolutionId(), rev.getRevisionId(), itemid),
	    (task, art) -> task.copyArtifact((Artifact)art),
	    (peerart, localart) -> PeerGateway.hasChanged((Artifact)peerart, (Artifact)localart),
	    (task, peerart, localart) -> task.copyArtifact((Artifact)peerart, (Artifact)localart),
	    (task, solid, art) -> task.copyArtifactContent(solid, (Artifact)art),
	    (client, art) -> client.createArtifact(art),
	    (client, art) -> client.updateArtifact(art));

	public static boolean hasChanged(Document thePeerDoc, Document theLocalDoc) {
		if (thePeerDoc.getVersion() != null && theLocalDoc.getVersion() != null) {
			return !thePeerDoc.getVersion().equals(theLocalDoc.getVersion());
		}

		if (thePeerDoc.getSize() != null && theLocalDoc.getSize() != null) {
			return !thePeerDoc.getSize().equals(theLocalDoc.getSize());
		}

		return true;
	}

	public static boolean hasChanged(Artifact thePeerArtifact, Artifact theLocalArtifact) {
		if (thePeerArtifact.getVersion() != null && theLocalArtifact.getVersion() != null) {
			return !thePeerArtifact.getVersion().equals(theLocalArtifact.getVersion());
		}

		if (thePeerArtifact.getSize() != null && theLocalArtifact.getSize() != null) {
			return !thePeerArtifact.getSize().equals(theLocalArtifact.getSize());
		}

		return true;
	}
}

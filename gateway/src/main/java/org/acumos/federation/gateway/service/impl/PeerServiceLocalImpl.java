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

package org.acumos.federation.gateway.service.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPPeerSubscription;
import org.acumos.federation.gateway.cds.PeerSubscription;
import org.acumos.federation.gateway.service.PeerService;
import org.acumos.federation.gateway.service.PeerSubscriptionService;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Service
@ConfigurationProperties(prefix = "peers-local")
public class PeerServiceLocalImpl extends AbstractServiceLocalImpl implements PeerService, PeerSubscriptionService {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private List<FLPPeer> peers;

	@PostConstruct
	public void initPeerService() {
		log.debug("init local peer info service");
		checkResource();
		try {
			watcher.watchOn(this.resource.getURL().toURI(), (uri) -> {
				loadPeersSubscriptionsInfo();
			});
		} catch (IOException | URISyntaxException iox) {
			log.info("Peers subscriptions watcher registration failed for " + this.resource, iox);
		}

		loadPeersSubscriptionsInfo();

		// Done
		log.debug("Local PeerService available");
	}

	private void loadPeersSubscriptionsInfo() {
		log.info("Loading peers subscriptions from " + this.resource);
		synchronized (this) {
			try {
				ObjectReader objectReader = new ObjectMapper()
																					.registerModule(new JavaTimeModule())
																					.reader(FLPPeer.class);
				MappingIterator objectIterator = objectReader.readValues(this.resource.getURL());
				this.peers = objectIterator.readAll();
				log.info("loaded " + this.peers.size() + " peers");
			}
			catch (Exception x) {
				throw new BeanInitializationException("Failed to load solutions catalog from " + this.resource, x);
			}
		}
	}

	@PreDestroy
	public void cleanupPeerService() {
		log.debug("Local peer info service destroyed");
	}

	/** */
	@Override
	public MLPPeer getSelf() {
		MLPPeer self = this.peers.stream().filter(peer -> peer.isSelf()).findFirst().orElse(null);

		return self;
	}

	/** */
	@Override
	public List<MLPPeer> getPeers(ServiceContext theContext) {
		synchronized (this) {
			return this.peers == null ? null
					: this.peers.stream().map(peer -> (MLPPeer) peer).collect(Collectors.toList());
		}
	}

	/** */
	@Override
	public List<MLPPeer> getPeerBySubjectName(final String theSubjectName, ServiceContext theContext) {
		log.info("Looking for peer " + theSubjectName);
		return this.peers.stream().filter(peer -> {
			log.info("Found peer " + peer.getSubjectName());
			return theSubjectName.equals(peer.getSubjectName());
		}).collect(Collectors.toList());
	}

	/** */
	@Override
	public MLPPeer getPeerById(final String thePeerId, ServiceContext theContext) {
		MLPPeer apeer = this.peers.stream().filter(peer -> thePeerId.equals(peer.getPeerId())).findFirst().orElse(null);
		log.info("Local peer info, one peer: " + apeer);
		return apeer;
	}

	/** */
	@Override
	public void registerPeer(MLPPeer thePeer) throws ServiceException {
		log.info("Registered peer {}", thePeer);
		List<MLPPeer> peers = getPeerBySubjectName(thePeer.getSubjectName());
		if (peers.size() > 0) {
			assertPeerRegistration(peers.get(0));
		}
		this.peers.add(new FLPPeer(thePeer));
	}

	/** */
	@Override
	public void unregisterPeer(MLPPeer thePeer) throws ServiceException {
		log.info("Unregistered peer {}", thePeer);
		List<MLPPeer> peers = getPeerBySubjectName(thePeer.getSubjectName());
		if (peers.size() > 0) {
			assertPeerUnregistration(peers.get(0));
			this.peers.remove(peers.get(0));
		}
		else
			throw new ServiceException("No such peer " + thePeer);
	}

	/** */
	@Override
	public List<MLPPeerSubscription> getPeerSubscriptions(final String thePeerId) {
		if (this.peers == null)
			return Collections.EMPTY_LIST;
		FLPPeer peer = this.peers.stream().filter(entry -> thePeerId.equals(entry.getPeerId())).findFirst()
				.orElse(null);
		log.info("Peer " + thePeerId + " subs:" + (peer == null ? "none" : peer.getSubscriptions()));
		return peer == null ? Collections.EMPTY_LIST : (List)peer.getSubscriptions();
	}

	/** */
	@Override
	public PeerSubscription getPeerSubscription(Long theSubId) {
		for (FLPPeer peer : this.peers) {
			for (PeerSubscription peerSub : peer.getSubscriptions()) {
				if (peerSub.getSubId().equals(theSubId))
					return peerSub;
			}
		}
		return null;
	}

	/** */
	@Override
	public void updatePeerSubscription(MLPPeerSubscription theSub) throws ServiceException {
		for (FLPPeer peer : this.peers) {
			for (int i = 0; i < peer.getSubscriptions().size(); i++) {
				PeerSubscription peerSub = peer.getSubscriptions().get(i);
				if (theSub.getSubId().equals(peerSub.getSubId()) &&
						theSub.getPeerId().equals(peerSub.getPeerId())) {
					peer.getSubscriptions().set(i, new PeerSubscription(theSub));
					return;
				}
			}
		}
		throw new ServiceException("No such subscription");
	}

	/** */
	public static class FLPPeer extends MLPPeer {

		@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
		private List<PeerSubscription> subscriptions;

		public FLPPeer() {
		}

		public FLPPeer(MLPPeer theSource) {
			super(theSource);
		}

		// @JsonIgnore
		public List<PeerSubscription> getSubscriptions() {
			return this.subscriptions;
		}

		public void setSubscriptions(List<PeerSubscription> theSubscriptions) {
			this.subscriptions = theSubscriptions;
		}

		public String toString() {
			return super.toString() + ",subscriptions:" + this.subscriptions;
		}
	}

}

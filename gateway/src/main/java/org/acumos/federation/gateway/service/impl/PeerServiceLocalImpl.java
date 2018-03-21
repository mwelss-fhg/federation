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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Date;
import java.util.Collections;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.beans.factory.annotation.Autowired;

import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.service.PeerService;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.service.PeerSubscriptionService;

import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPPeerSubscription;

import org.apache.commons.io.IOUtils;

@Service
@ConfigurationProperties(prefix = "peersLocal")
public class PeerServiceLocalImpl extends AbstractServiceLocalImpl implements PeerService, PeerSubscriptionService {

	private List<FLPPeer> peers;

	@PostConstruct
	public void initPeerService() {
		log.debug(EELFLoggerDelegate.debugLogger, "init local peer info service");
		checkResource();
		try {
			watcher.watchOn(this.resource.getURL().toURI(), (uri) -> {
				loadPeersSubscriptionsInfo();
			});
		} catch (IOException | URISyntaxException iox) {
			log.info(EELFLoggerDelegate.errorLogger,
					"Peers subscriptions watcher registration failed for " + this.resource, iox);
		}

		loadPeersSubscriptionsInfo();

		// Done
		log.debug(EELFLoggerDelegate.debugLogger, "Local PeerService available");
	}

	private void loadPeersSubscriptionsInfo() {
		log.info(EELFLoggerDelegate.debugLogger, "Loading peers subscriptions from " + this.resource);
		synchronized (this) {
			try {
				ObjectReader objectReader = new ObjectMapper().reader(FLPPeer.class);
				MappingIterator objectIterator = objectReader.readValues(this.resource.getURL());
				this.peers = objectIterator.readAll();
				log.info(EELFLoggerDelegate.debugLogger, "loaded " + this.peers.size() + " peers");
			}
			catch (Exception x) {
				throw new BeanInitializationException("Failed to load solutions catalog from " + this.resource, x);
			}
		}
	}

	@PreDestroy
	public void cleanupPeerService() {
		log.debug(EELFLoggerDelegate.debugLogger, "Local peer info service destroyed");
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
		log.info(EELFLoggerDelegate.debugLogger, "Looking for peer " + theSubjectName);
		return this.peers.stream().filter(peer -> {
			log.info(EELFLoggerDelegate.debugLogger, "Found peer " + peer.getSubjectName());
			return theSubjectName.equals(peer.getSubjectName());
		}).collect(Collectors.toList());
	}

	/** */
	@Override
	public MLPPeer getPeerById(final String thePeerId, ServiceContext theContext) {
		MLPPeer apeer = this.peers.stream().filter(peer -> thePeerId.equals(peer.getPeerId())).findFirst().orElse(null);
		log.info(EELFLoggerDelegate.debugLogger, "Local peer info, one peer: " + apeer);
		return apeer;
	}

	/** */
	@Override
	public void registerPeer(MLPPeer mlpPeer) {
		log.info(EELFLoggerDelegate.debugLogger, "Registered peer {}", mlpPeer);
		//this.peers.put(new FLPPeer(mlpPeer));
	}

	/** */
	@Override
	public void unregisterPeer(MLPPeer mlpPeer) {
		throw new UnsupportedOperationException();
	}

	/** */
	@Override
	public List<MLPPeerSubscription> getPeerSubscriptions(final String thePeerId) {
		FLPPeer peer = this.peers.stream().filter(entry -> thePeerId.equals(entry.getPeerId())).findFirst()
				.orElse(null);
		log.info(EELFLoggerDelegate.debugLogger,
				"Peer " + thePeerId + " subs:" + (peer == null ? "none" : peer.getSubscriptions()));
		return peer == Collections.EMPTY_LIST ? null : peer.getSubscriptions();
	}

	/** */
	@Override
	public MLPPeerSubscription getPeerSubscription(Long theSubId) {
		for (FLPPeer peer : this.peers) {
			for (MLPPeerSubscription peerSub : peer.getSubscriptions()) {
				if (peerSub.getSubId().equals(theSubId))
					return peerSub;
			}
		}
		return null;
	}

	/** */
	@Override
	public boolean updatePeerSubscription(MLPPeerSubscription theSub) {
		for (FLPPeer peer : this.peers) {
			for (int i = 0; i < peer.getSubscriptions().size(); i++) {
				MLPPeerSubscription peerSub = peer.getSubscriptions().get(i);
				if (theSub.getSubId().equals(peerSub.getSubId()) &&
						theSub.getPeerId().equals(peerSub.getPeerId())) {
					peer.getSubscriptions().set(i, theSub);
					return true;
				}
			}
		}
		return false;
	}

	/** */
	public static class FLPPeer extends MLPPeer {

		@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
		private List<MLPPeerSubscription> subscriptions;

		// @JsonIgnore
		public List<MLPPeerSubscription> getSubscriptions() {
			return this.subscriptions;
		}

		public void setSubscriptions(List<MLPPeerSubscription> theSubscriptions) {
			this.subscriptions = theSubscriptions;
		}

		public String toString() {
			return super.toString() + ",subscriptions:" + this.subscriptions;
		}
	}

}

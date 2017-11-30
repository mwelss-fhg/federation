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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

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
import org.springframework.context.annotation.Conditional;
import org.springframework.beans.factory.annotation.Autowired;

import org.acumos.federation.gateway.util.LocalWatchService;
import org.acumos.federation.gateway.common.AdapterCondition;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.service.PeerAcumosConfigService;
import org.acumos.federation.gateway.service.PeerAcumosSubscriptionService;

import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPPeerSubscription;

import org.apache.commons.io.IOUtils;

@Service
@ConfigurationProperties(prefix="peersLocal")
@Conditional(AdapterCondition.class)
public class PeerAcumosServiceLocalImpl
												implements PeerAcumosConfigService,
												 					 PeerAcumosSubscriptionService {

	private List<FLPPeer>		peers;
	private URI							sourceUri;
	private WatchService		sourceWatcher = null;

	private final EELFLoggerDelegate logger = EELFLoggerDelegate.getLogger(PeerAcumosServiceLocalImpl.class);

	@Autowired
	private LocalWatchService	watcher;
		
	public void setSourceUri(String theUri) {
		try {
			this.sourceUri = new URI(theUri);
		}
		catch(URISyntaxException urisx) {
			throw new IllegalArgumentException("Cannot set uri", urisx);
		}
	}

	@PostConstruct
	public void initPeerService() {
		logger.debug(EELFLoggerDelegate.debugLogger, "init local peer info service");

		if (this.sourceUri == null) {
			throw new BeanInitializationException("No source uri was configured");
		}

		watcher.watchOn(this.sourceUri,
										(uri) -> { loadPeersSubscriptionsInfo(); });

		loadPeersSubscriptionsInfo();

		// Done
		logger.debug(EELFLoggerDelegate.debugLogger, "Onap available");
	}

	private void loadPeersSubscriptionsInfo() {
		logger.info(EELFLoggerDelegate.debugLogger, "Loading peers subscriptions from " + this.sourceUri);
		synchronized (this) {
	    try {
				ObjectReader objectReader =
    	                        new ObjectMapper().reader(FLPPeer.class);
      	MappingIterator objectIterator =
        	                    objectReader.readValues(this.sourceUri.toURL());
				this.peers = objectIterator.readAll();
				logger.info(EELFLoggerDelegate.debugLogger, "loaded " + this.peers.size() + " peers");
			}
			catch (Exception x) {
      	throw new BeanInitializationException("Failed to load solutions catalog from " + this.sourceUri, x);
			}
		}
	}

	@PreDestroy
	public void cleanupPeerService() {
		logger.debug(EELFLoggerDelegate.debugLogger, "Local peer info service destroyed");
	}


	/** */
	public List<MLPPeer> getPeers() {
		synchronized (this) {
			return this.peers == null ? null : 
																this.peers.stream()
																		.map(peer -> (MLPPeer)peer)
																		.collect(Collectors.toList());
		}
	}
	
	/** */
	public List<MLPPeer> getPeer(final String theSubjectName) {
		logger.info(EELFLoggerDelegate.debugLogger, "Looking for peer " + theSubjectName);
		return 
			this.peers
						.stream()
						.filter(peer -> { 
		logger.info(EELFLoggerDelegate.debugLogger, "Found peer " + peer.getSubjectName());
															return theSubjectName.equals(peer.getSubjectName()); })
						.collect(Collectors.toList());
	}
	
	/** */
	public MLPPeer getOnePeer(final String thePeerId) {
		MLPPeer apeer =
			this.peers
						.stream()
						.filter(peer -> thePeerId.equals(peer.getPeerId()))
						.findFirst()
						.orElse(null);

		logger.debug(EELFLoggerDelegate.errorLogger, "Local peer info, one peer: " + apeer);

		return apeer;
	}
	
	/** */
	public MLPPeer savePeer(MLPPeer mlpPeer) {
		throw new UnsupportedOperationException();
	}
		
	/** */
	public boolean updatePeer(MLPPeer mlpPeer) {
		throw new UnsupportedOperationException();
	}

	/** */
	public boolean deletePeer(MLPPeer mlpPeer) {
		throw new UnsupportedOperationException();
	}

	public List<MLPPeerSubscription> getPeerSubscriptions(final String thePeerId) {
		FLPPeer peer =
			this.peers
							.stream()
							.filter(entry -> thePeerId.equals(entry.getPeerId()))
							.findFirst()
							.orElse(null);
		logger.info(EELFLoggerDelegate.errorLogger, "Peer " + thePeerId + " subs:" + (peer == null ? "none" : peer.getSubscriptions()));
		return peer == Collections.EMPTY_LIST ? null : peer.getSubscriptions();
	}

	/** */
	public MLPPeerSubscription getPeerSubscription(Long theSubId) {
		for (FLPPeer peer: this.peers) {
			for (MLPPeerSubscription peerSub: peer.getSubscriptions()) {
				if (peerSub.getSubId().equals(theSubId))
					return peerSub;
			}
		}	
		return null;
	}

	/** */
	public MLPPeerSubscription savePeerSubscription(MLPPeerSubscription mlpPeerSubscription) {
		throw new UnsupportedOperationException();
	}
	
	/** */
	public boolean updatePeerSubscription(MLPPeerSubscription mlpPeerSubscription) {
		throw new UnsupportedOperationException();
	}

	/** */
	public boolean deletePeerSubscription(MLPPeerSubscription mlpPeerSubscription) {
		throw new UnsupportedOperationException();
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

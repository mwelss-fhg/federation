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

package org.acumos.federation.gateway.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.domain.MLPPeerSubscription;
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.federation.gateway.cds.Solution;
import org.acumos.federation.gateway.common.Clients;
import org.acumos.federation.gateway.common.FederationClient;
import org.acumos.federation.gateway.common.FederationException;
import org.acumos.federation.gateway.event.PeerSubscriptionEvent;
import org.acumos.federation.gateway.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component("test")
@Scope("singleton")
@ConfigurationProperties(prefix = "test")
@Conditional({TestAdapterCondition.class})
public class TestAdapter {

	private final Logger log = LoggerFactory.getLogger(getClass().getName());
	private TaskExecutor taskExecutor;

	private Map<String, Map<String, MLPSolution>> imports = new HashMap<String, Map<String, MLPSolution>>();
	@Autowired
	private Clients clients;

	public TestAdapter() {
	}

	@PostConstruct
	public void initTestAdapter() {
		log.trace("initTestAdapter");

		this.taskExecutor = new ThreadPoolTaskExecutor();
		((ThreadPoolTaskExecutor) this.taskExecutor).setCorePoolSize(1);
		((ThreadPoolTaskExecutor) this.taskExecutor).setMaxPoolSize(1);
		((ThreadPoolTaskExecutor) this.taskExecutor).setQueueCapacity(25);
		((ThreadPoolTaskExecutor) this.taskExecutor).initialize();

		// Done
		log.trace("TestAdapter available");
	}

	@PreDestroy
	public void cleanupTestAdapter() {
		log.trace("TestAdapter destroyed");
	}

	@EventListener
	public void handlePeerSubscriptionUpdate(PeerSubscriptionEvent theEvent) {
		log.info("received peer subscription update event {}", theEvent);
		taskExecutor.execute(new TestTask(theEvent.getPeer(), theEvent.getSubscription()));
	}

	public class TestTask implements Runnable {

		private MLPPeer peer;
		private MLPPeerSubscription sub;

		public TestTask(MLPPeer thePeer, MLPPeerSubscription theSub) {
			this.peer = thePeer;
			this.sub = theSub;
		}

		public void run() {

			Map<String, MLPSolution> peerImports = null;

			synchronized (imports) {
				peerImports = imports.computeIfAbsent(peer.getPeerId(), pid -> new HashMap());
			}

			synchronized (peerImports) {

				List<MLPSolution> peerSolutions = null;
				try {
					peerSolutions = (List)clients.getFederationClient(this.peer.getApiUrl())
						.getSolutions((String)Utils.jsonStringToMap(this.sub.getSelector()).get("catalogId")).getContent();
				}
				catch (FederationException fx) {
					log.warn("Failed to retrieve solutions", fx);
					return;
				}

				log.info("Processing peer {} subscription {} yielded solutions {}", this.peer, this.sub.getSubId(), peerSolutions);

				for (MLPSolution solution : peerSolutions) {

					MLPSolution peerImport = peerImports.get(solution.getSolutionId());
					if (peerImport == null) {
						log.info("New solution {}", solution.getSolutionId());
						peerImports.put(solution.getSolutionId(), solution);
					}
					else {
						log.info("Existing solution {}", solution.getSolutionId());
						if (peerImport.getModified().equals(solution.getModified())) {
							log.info("No updates to solution {}", solution.getSolutionId());
							continue;
						}
						else {
							log.info("Solution {} has updates", solution.getSolutionId());
						}
					}

					FederationClient fedClient = clients.getFederationClient(this.peer.getApiUrl());

					List<MLPSolutionRevision> revisions = null;
					Solution sol = null;
					try {
						sol = (Solution)fedClient.getSolution(solution.getSolutionId()).getContent();
						log.info("retrieved solution {}", solution);
						revisions = (List)sol.getRevisions();
					}
					catch (Exception x) {
						log.error("Failed to retrieve solution", x);
						continue;
					}
					
					log.info("Received {} revisions {}", revisions.size(), revisions);

					for (MLPSolutionRevision revision: revisions) {
						List<MLPArtifact> artifacts = null;
						try {
							artifacts = (List<MLPArtifact>) fedClient.getArtifacts(
																								solution.getSolutionId(),	revision.getRevisionId()).getContent();
						}
						catch (Exception x) {
							log.error("Failed to retrieve artifacts", x);
							continue;
						}
						log.info("Received {} artifacts {}", artifacts.size(), artifacts);

						for (MLPArtifact artifact : artifacts) {
							Resource artifactContent = null;
							try {
								artifactContent = fedClient.getArtifactContent( artifact.getArtifactId());
								log.warn("Received artifact content: "
										+ new BufferedReader(new InputStreamReader(artifactContent.getInputStream()))
												.lines().collect(Collectors.joining("\n")));
							}
							catch (Exception x) {
								log.error("Failed to download artifact", x);
								continue;
							}
						}
					}
				}
			}
		}
	}
}

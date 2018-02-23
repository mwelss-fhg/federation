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
import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPSolutionRevision;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.event.PeerSubscriptionEvent;
import org.acumos.federation.gateway.common.Clients;
import org.acumos.federation.gateway.common.FederationClient;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Conditional;
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

	private final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(getClass().getName());
	private TaskExecutor taskExecutor;

	private Map<String, Map<String, MLPSolution>> imports = new HashMap<String, Map<String, MLPSolution>>();
	@Autowired
	private Clients clients;

	public TestAdapter() {
	}

	@PostConstruct
	public void initTestAdapter() {
		log.trace(EELFLoggerDelegate.debugLogger, "initTestAdapter");

		this.taskExecutor = new ThreadPoolTaskExecutor();
		((ThreadPoolTaskExecutor) this.taskExecutor).setCorePoolSize(1);
		((ThreadPoolTaskExecutor) this.taskExecutor).setMaxPoolSize(1);
		((ThreadPoolTaskExecutor) this.taskExecutor).setQueueCapacity(25);
		((ThreadPoolTaskExecutor) this.taskExecutor).initialize();

		// Done
		log.trace(EELFLoggerDelegate.debugLogger, "TestAdapter available");
	}

	@PreDestroy
	public void cleanupTestAdapter() {
		log.trace(EELFLoggerDelegate.debugLogger, "TestAdapter destroyed");
	}

	@EventListener
	public void handlePeerSubscriptionUpdate(PeerSubscriptionEvent theEvent) {
		log.info(EELFLoggerDelegate.debugLogger, "received peer subscription update event {}", theEvent);
		taskExecutor.execute(new TestTask(theEvent.getPeer(), theEvent.getSolutions()));
	}

	public class TestTask implements Runnable {

		private MLPPeer peer;
		private List<MLPSolution> solutions;

		public TestTask(MLPPeer thePeer, List<MLPSolution> theSolutions) {
			this.peer = thePeer;
			this.solutions = theSolutions;
		}

		public void run() {

			Map<String, MLPSolution> peerImports = null;

			synchronized (imports) {
				peerImports = imports.computeIfAbsent(peer.getPeerId(), pid -> new HashMap());
			}

			synchronized (peerImports) {
				for (MLPSolution solution : this.solutions) {

					MLPSolution peerImport = peerImports.get(solution.getSolutionId());
					if (peerImport == null) {
						log.debug(EELFLoggerDelegate.debugLogger, "New solution");
						peerImports.put(solution.getSolutionId(), solution);
					}
					else {
						log.debug(EELFLoggerDelegate.debugLogger, "Existing solution");
						if (peerImport.getModified().equals(solution.getModified())) {
							log.debug(EELFLoggerDelegate.debugLogger, "No updates");
						}
						else {
							log.debug(EELFLoggerDelegate.debugLogger, "Has updates");
						}
					}

					FederationClient fedClient = clients.getFederationClient(this.peer.getApiUrl());

					List<MLPSolutionRevision> revisions = null;
					try {
						revisions = (List<MLPSolutionRevision>) fedClient.getSolutionRevisions(solution.getSolutionId())
								.getContent();
					}
					catch (Exception x) {
						log.error(EELFLoggerDelegate.errorLogger, "Failed to retrieve revisions", x);
						continue;
					}
					log.debug(EELFLoggerDelegate.debugLogger,
							"Received " + revisions.size() + " revisions " + revisions);

					List<MLPArtifact> artifacts = null;
					try {
						artifacts = (List<MLPArtifact>) fedClient.getArtifacts(solution.getSolutionId(),
								revisions.get(revisions.size() - 1).getRevisionId()).getContent();
					}
					catch (Exception x) {
						log.error(EELFLoggerDelegate.errorLogger, "Failed to retrieve artifacts", x);
						continue;
					}
					log.info(EELFLoggerDelegate.debugLogger,
							"Received " + artifacts.size() + " artifacts " + artifacts);

					for (MLPArtifact artifact : artifacts) {
						Resource artifactContent = null;
						try {
							artifactContent = fedClient.downloadArtifact(artifact.getArtifactId());
							log.warn(EELFLoggerDelegate.debugLogger, "Received artifact content: "
									+ new BufferedReader(new InputStreamReader(artifactContent.getInputStream()))
											.lines().collect(Collectors.joining("\n")));
						}
						catch (Exception x) {
							log.error(EELFLoggerDelegate.errorLogger, "Failed to download artifact", x);
						}
					}
				}
			}
		}
	}
}

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

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.transport.RestPageRequest;
import org.acumos.cds.transport.RestPageResponse;
import org.acumos.federation.gateway.cds.PeerStatus;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.config.FederationInterfaceConfiguration;
import org.acumos.federation.gateway.security.Tools;
import org.acumos.federation.gateway.service.PeerService;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.service.ServiceException;
import org.acumos.federation.gateway.util.MapBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *  CDS based implementation of the hpeer service interface.
 */
@Service
public class PeerServiceImpl extends AbstractServiceImpl implements PeerService {

	private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private FederationInterfaceConfiguration fedIfConfig;

	public PeerServiceImpl() {
	}

	/**
	 * Retrieves from CDS the peer record representing this Acumos gateway.
	 * It can tolerate multiple CDS entries marked as 'self', requires a match between the locally configured identity as
	 * it appears in the federation interface configuration and the subjectName attribute of the CDS entry.
	 */
	@Override
	public MLPPeer getSelf() {

		String selfName = null;
		try {
			selfName = Tools.getNameParts(fedIfConfig.getSubjectName(), "CN").get("CN").toString();
		}
		catch(Exception x) {
			log.warn(EELFLoggerDelegate.errorLogger, "Cannot obtain 'self' name from interface config " + x);
			return null;
		}
		final String subjectName = selfName;
		log.debug(EELFLoggerDelegate.debugLogger, "Expecting 'self' name '{}'", subjectName);

		List<MLPPeer> selfPeers = new ArrayList<MLPPeer>();
		RestPageRequest pageRequest = new RestPageRequest(0, 100);
		RestPageResponse<MLPPeer> pageResponse = null;
		do {
			pageResponse =
				getClient().searchPeers(new MapBuilder().put("isSelf", Boolean.TRUE).build(), false, pageRequest);
			log.debug(EELFLoggerDelegate.errorLogger, "Peers representing 'self': " + pageResponse.getContent());

			selfPeers.addAll(
				pageResponse.getContent().stream()
										.filter(peer -> subjectName.equals(peer.getSubjectName()))
										.collect(Collectors.toList()));

			pageRequest.setPage(pageResponse.getNumber() + 1);
		}
		while (!pageResponse.isLast());

		if (selfPeers.size() != 1) {
			log.warn(EELFLoggerDelegate.errorLogger, "Number of peers representing 'self', i.e. '{}', not 1. Found {}.", subjectName, selfPeers);
			return null;
		}
		return selfPeers.get(0);
	}

	/**
	 * ToDo:
	 */
	@Override
	public List<MLPPeer> getPeers(ServiceContext theContext) {
		log.debug(EELFLoggerDelegate.debugLogger, "getPeers");

		RestPageRequest pageRequest = new RestPageRequest(0, 100);
		RestPageResponse<MLPPeer> pageResponse = null;
		List<MLPPeer> peers = new ArrayList<MLPPeer>(),
									pagePeers = null;
		ICommonDataServiceRestClient cdsClient = getClient();

		do {
			pageResponse = cdsClient.getPeers(pageRequest);
			peers.addAll(pageResponse.getContent());
		
			pageRequest.setPage(pageResponse.getNumber() + 1);
		}
		while (!pageResponse.isLast());

		return peers;
	}

	@Override
	public List<MLPPeer> getPeerBySubjectName(String theSubjectName, ServiceContext theContext) {
		log.debug(EELFLoggerDelegate.debugLogger, "getPeerBySubjectName");
		RestPageResponse<MLPPeer> response = 
			getClient().searchPeers(new MapBuilder().put("subjectName", theSubjectName).build(), false, null);
		if (response.getNumberOfElements() != 1) {
			log.warn(EELFLoggerDelegate.errorLogger, "getPeerBySubjectName returned more then one peer: {}", response.getNumberOfElements());
		}
		return response.getContent();
	}

	@Override
	public MLPPeer getPeerById(String thePeerId, ServiceContext theContext) {
		log.debug(EELFLoggerDelegate.debugLogger, "getPeerById: {}", thePeerId);
		MLPPeer mlpPeer = getClient().getPeer(thePeerId);
		if (mlpPeer != null) {
			log.error(EELFLoggerDelegate.debugLogger, "getPeerById: {}", mlpPeer.toString());
		}
		return mlpPeer;
	}

	@Override
	public void registerPeer(MLPPeer thePeer) throws ServiceException {
		log.debug(EELFLoggerDelegate.debugLogger, "registerPeer");

		String subjectName = thePeer.getSubjectName();
		if (subjectName == null)
			throw new ServiceException("No subject name is available");

		ICommonDataServiceRestClient cdsClient = getClient();
		RestPageResponse<MLPPeer> response = 
			cdsClient.searchPeers(new MapBuilder().put("subjectName", subjectName).build(), false, null);

		if (response.getNumberOfElements() > 0) {
			assertPeerRegistration(response.getContent().get(0));
		}

		log.info(EELFLoggerDelegate.debugLogger, "registerPeer: new peer with subjectName {}, create CDS record",
				thePeer.getSubjectName());
		//enforce
		thePeer.setStatusCode(PeerStatus.Requested.code());

		try {
			cdsClient.createPeer(thePeer);
		}
		catch (Exception x) {
			throw new ServiceException("Failed to create peer", x);
		}
	}

	@Override
	public void unregisterPeer(MLPPeer thePeer) throws ServiceException {
		log.debug(EELFLoggerDelegate.debugLogger, "unregisterPeer");

		String subjectName = thePeer.getSubjectName();
		if (subjectName == null)
			throw new ServiceException("No subject name is available");

		ICommonDataServiceRestClient cdsClient = getClient();
		RestPageResponse<MLPPeer> response = 
			cdsClient.searchPeers(new MapBuilder().put("subjectName", subjectName).build(), false, null);

		if (response.getNumberOfElements() != 1) {
			throw new ServiceException("Search for peer with subjectName '" + subjectName + "' yielded invalid number of items: " + response.getNumberOfElements());
		}

		MLPPeer peer = response.getContent().get(0);
		assertPeerUnregistration(peer);

		//active/inactive peers moved to renounced
		log.info(EELFLoggerDelegate.debugLogger, "unregisterPeer: peer with subjectName {}, update CDS record",
				thePeer.getSubjectName());
		thePeer.setStatusCode(PeerStatus.Renounced.code());

		try {
			cdsClient.updatePeer(thePeer);
		}
		catch (Exception x) {
			throw new ServiceException("Failed to update peer", x);
		}
	}

}

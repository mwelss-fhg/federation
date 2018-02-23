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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.util.MapBuilder;
import org.acumos.federation.gateway.service.PeerService;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.service.ServiceException;
import org.acumos.federation.gateway.cds.PeerStatus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Conditional;

import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.domain.MLPPeer;
import org.acumos.cds.transport.RestPageResponse;

/**
 * 
 *
 */
@Service
public class PeerServiceImpl extends AbstractServiceImpl implements PeerService {

	/**
	 * 
	 */
	public PeerServiceImpl() {
	}

	@Override
	public MLPPeer getSelf() {
		RestPageResponse<MLPPeer> response = 
			getClient().searchPeers(new MapBuilder().put("isSelf", Boolean.TRUE).build(), false, null);
		if (response.getSize() != 1) {
			log.warn(EELFLoggerDelegate.errorLogger, "Number of peers representing 'self' not 1: " + response.getSize());
			return null;
		}
		return response.getContent().get(0);
	}

	/**
	 * ToDo:
	 */
	@Override
	public List<MLPPeer> getPeers(ServiceContext theContext) {
		log.debug(EELFLoggerDelegate.debugLogger, "getPeers");
		ICommonDataServiceRestClient cdsClient = getClient();
		List<MLPPeer> mlpPeers = null;
		/*
		 * cdsClient.searchPeers( new MapBuilder() .put("status", PeerStatus.ACTIVE)
		 * .build(), false);
		 */
		RestPageResponse<MLPPeer> mlpPeersPage = cdsClient.getPeers(null);
		if (mlpPeersPage != null)
			mlpPeers = mlpPeersPage.getContent();
		if (mlpPeers != null) {
			log.debug(EELFLoggerDelegate.debugLogger, "getPeers size:{}", mlpPeers.size());
		}
		return mlpPeers;
	}

	@Override
	public List<MLPPeer> getPeerBySubjectName(String theSubjectName, ServiceContext theContext) {
		log.debug(EELFLoggerDelegate.debugLogger, "getPeerBySubjectName");
		RestPageResponse<MLPPeer> response = 
			getClient().searchPeers(new MapBuilder().put("subjectName", theSubjectName).build(), false, null);
		if (response.getSize() != 1) {
			log.warn(EELFLoggerDelegate.errorLogger, "getPeerBySubjectName returned more then one peer:{}", response.getSize());
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

		if (response.getSize() != 0) {
			//if (response.getSize() == 1) { //should be the only alternative
			MLPPeer peer = response.getContent().get(0);
			PeerStatus status = PeerStatus.forCode(peer.getStatusCode());
			if (null == status) {
				throw new ServiceException("Invalid peer status found: " + peer.getStatusCode());
			}

			if (status == PeerStatus.Requested) {
				throw new ServiceException("Peer registration request is pending");
			}
			else if (status == PeerStatus.Active || status == PeerStatus.Inactive) {
				log.info(EELFLoggerDelegate.applicationLogger, "registering an active/inactive peer: " + peer);
				return;
			}
			else if (status == PeerStatus.Declined) {
				throw new ServiceException("Peer registration request was declined");
			}
			else if (status == PeerStatus.Renounced) {
				throw new ServiceException("Peer unregistration request is pending");
			}
			throw new ServiceException("Peer with subjectName '" + subjectName + "' already exists: " + peer);
		}

		log.error(EELFLoggerDelegate.debugLogger, "registerPeer: new peer with subjectName {}, create CDS record",
				thePeer.getSubjectName());
		//enforce
		thePeer.setStatusCode(PeerStatus.Requested.code());

		try {
			cdsClient.createPeer(thePeer);
		}
		catch (Exception x) {
			throw new ServiceException("Failed to create peer");
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

		if (response.getSize() != 1) {
			throw new ServiceException("Search for peer with subjectName '" + subjectName + "' yielded invalid number of items: " + response);
		}

		MLPPeer peer = response.getContent().get(0);
		PeerStatus status = PeerStatus.forCode(peer.getStatusCode());
		if (null == status) {
			throw new ServiceException("Invalid peer status found: " + peer.getStatusCode());
		}

		if (status == PeerStatus.Requested) {
			throw new ServiceException("Peer registration request is pending");
			//can we simply delete the peer ??
		}
		else if (status == PeerStatus.Declined) {
			throw new ServiceException("Peer registration request was declined");
			//can we simply delete the peer ??
		}
		else if (status == PeerStatus.Renounced) {
			throw new ServiceException("Peer unregistration request is pending");
		}
		//active/inactive peers moved to renounced

		log.error(EELFLoggerDelegate.debugLogger, "unregisterPeer: peer with subjectName {}, update CDS record",
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

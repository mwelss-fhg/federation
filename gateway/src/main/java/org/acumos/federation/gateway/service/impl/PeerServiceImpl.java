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

import org.acumos.federation.gateway.common.GatewayCondition;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.util.MapBuilder;
import org.acumos.federation.gateway.service.PeerService;
import org.acumos.federation.gateway.service.ServiceContext;
import org.acumos.federation.gateway.service.ServiceException;

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
@Conditional(GatewayCondition.class)
public class PeerServiceImpl extends AbstractServiceImpl implements PeerService {

	/**
	 * 
	 */
	public PeerServiceImpl() {
	}

	@Override
	public MLPPeer getSelf() {
		return (MLPPeer) getClient().searchPeers(new MapBuilder().put("isSelf", Boolean.TRUE).build(), false).get(0);
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
		List<MLPPeer> mlpPeers = getClient().searchPeers(new MapBuilder().put("subjectName", theSubjectName).build(),
				false);
		if (mlpPeers != null && mlpPeers.size() > 0) {
			log.debug(EELFLoggerDelegate.debugLogger, "getPeerBySubjectName size:{}", mlpPeers.size());
		}
		return mlpPeers;
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
	public void subscribePeer(MLPPeer thePeer) throws ServiceException {
		log.debug(EELFLoggerDelegate.debugLogger, "subscribePeer");

		String subjectName = thePeer.getSubjectName();
		if (subjectName == null)
			throw new ServiceException("No subject name is available");

		ICommonDataServiceRestClient cdsClient = getClient();
		List<MLPPeer> mlpPeers = cdsClient.searchPeers(new MapBuilder().put("subjectName", subjectName).build(), false);

		if (mlpPeers != null && mlpPeers.size() > 0) {
			throw new ServiceException("Peer with subjectName '" + subjectName + "' already exists: " + mlpPeers);
		}

		log.error(EELFLoggerDelegate.debugLogger, "subscribePeer: new peer with subjectName {}, create CDS record",
				thePeer.getSubjectName());
		// waiting on CDS 1.13
		// thePeer.setStatus(PeerStatus.PENDING);

		try {
			cdsClient.createPeer(thePeer);
		} catch (Exception x) {
			throw new ServiceException("Failed to create peer");
		}
	}

	@Override
	public void unsubscribePeer(MLPPeer thePeer) throws ServiceException {
		log.debug(EELFLoggerDelegate.debugLogger, "unsubscribePeer");

		String subjectName = thePeer.getSubjectName();
		if (subjectName == null)
			throw new ServiceException("No subject name is available");

		ICommonDataServiceRestClient cdsClient = getClient();
		List<MLPPeer> mlpPeers = cdsClient.searchPeers(new MapBuilder().put("subjectName", subjectName).build(), false);

		if (mlpPeers != null && mlpPeers.size() != 1) {
			throw new ServiceException("No peer with subjectName '" + subjectName + "' found: " + mlpPeers);
		}

		log.error(EELFLoggerDelegate.debugLogger, "unsubscribePeer: peer with subjectName {}, update CDS record",
				thePeer.getSubjectName());
		// waiting on CDS 1.13
		// thePeer.setStatus(PeerStatus.PENDING_REMOVE);

		try {
			cdsClient.updatePeer(thePeer);
		} catch (Exception x) {
			throw new ServiceException("Failed to update peer", x);
		}
	}

}

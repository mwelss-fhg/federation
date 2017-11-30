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

import java.util.List;
import java.util.stream.Collectors;

import org.acumos.federation.gateway.common.GatewayCondition;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.service.PeerAcumosSubscriptionService;
import org.acumos.federation.gateway.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Conditional;

import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.domain.MLPPeerSubscription;
import org.acumos.cds.transport.RestPageResponse;

/**
 * 
 *
 */
@Service
@Conditional(GatewayCondition.class)
public class PeerAcumosSubscriptionServiceImpl extends AbstractServiceImpl implements PeerAcumosSubscriptionService {

	private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(PeerAcumosSubscriptionServiceImpl.class);

	@Autowired
	private Environment env;
	
	/**
	 * 
	 */
	public PeerAcumosSubscriptionServiceImpl() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<MLPPeerSubscription> getPeerSubscriptions(String peerId) {
		log.debug(EELFLoggerDelegate.debugLogger, "getPeerSubscriptions:{}", peerId);
		List<MLPPeerSubscription> mlpPeerSubscriptions  = null;
		//Temporary Fix as COmmon Data Service does not handle proper Serialization
		mlpPeerSubscriptions =	getCommonDataClient().getPeerSubscriptions(peerId);
		if(!Utils.isEmptyList(mlpPeerSubscriptions)) {
			//mlpPeerSubscriptions = mlpPeerSubscriptionPaged.getContent();
			mlpPeerSubscriptions = mlpPeerSubscriptions.stream().filter(mlpPeerSubscription -> (mlpPeerSubscription.getPeerId().contains(peerId))).collect(Collectors.toList());
			log.debug(EELFLoggerDelegate.debugLogger, "getPeers size:{}", mlpPeerSubscriptions.size());
		}
		return mlpPeerSubscriptions;
	}

	@Override
	public MLPPeerSubscription getPeerSubscription(Long subId) {
		log.debug(EELFLoggerDelegate.debugLogger, "getPeerSubscription:{}", subId);
		ICommonDataServiceRestClient dataServiceRestClient = getClient();
		MLPPeerSubscription existingMLPPeerSubscription = null;
		existingMLPPeerSubscription = dataServiceRestClient.getPeerSubscription(subId);
		if(existingMLPPeerSubscription != null) {
			log.debug(EELFLoggerDelegate.debugLogger, "getPeerSubscription :{}", existingMLPPeerSubscription.toString());
		}
		return existingMLPPeerSubscription;
	}
	
	@Override
	public MLPPeerSubscription savePeerSubscription(MLPPeerSubscription mlpPeerSubscription) {
		log.debug(EELFLoggerDelegate.debugLogger, "savePeerSubscription");
		ICommonDataServiceRestClient dataServiceRestClient = getClient();
		MLPPeerSubscription mlPeerSubscriptionCreated = null;
		mlPeerSubscriptionCreated = dataServiceRestClient.createPeerSubscription(mlpPeerSubscription);
		if(mlPeerSubscriptionCreated !=null) {
			log.debug(EELFLoggerDelegate.debugLogger, "savePeerSubscription :{}", mlPeerSubscriptionCreated.toString());
		}
		return mlPeerSubscriptionCreated;
	}

	@Override
	public boolean updatePeerSubscription(MLPPeerSubscription mlpPeerSubscription) {
		log.debug(EELFLoggerDelegate.debugLogger, "updatePeerSubscription");
		ICommonDataServiceRestClient dataServiceRestClient = getClient();
		boolean isUpdatedSuccessfully = false;
		MLPPeerSubscription existingMLPPeerSubscription = null;
		try{
			existingMLPPeerSubscription = getPeerSubscription(mlpPeerSubscription.getSubId());
			if(existingMLPPeerSubscription != null) {
				if(mlpPeerSubscription.getPeerId().equalsIgnoreCase(existingMLPPeerSubscription.getPeerId()))
					dataServiceRestClient.updatePeerSubscription(mlpPeerSubscription);
					isUpdatedSuccessfully = true;
			}
		} catch (Exception e) {
			isUpdatedSuccessfully = false;
			log.error(EELFLoggerDelegate.debugLogger, "updatePeer: Exception while deleting the MLPPeerSubscription record:", e);
		}
		return isUpdatedSuccessfully;
	}

	@Override
	public boolean deletePeerSubscription(MLPPeerSubscription mlpPeerSubscription) {
		log.debug(EELFLoggerDelegate.debugLogger, "deletePeerSubscription");
		boolean isDeletedSuccessfully = false;
		ICommonDataServiceRestClient dataServiceRestClient = getClient();
		try {
			dataServiceRestClient.deletePeerSubscription(mlpPeerSubscription.getSubId());
			isDeletedSuccessfully = true;
		} catch (Exception e) {
			isDeletedSuccessfully = false;
			log.error(EELFLoggerDelegate.debugLogger, "deletePeerSubscription: Exception while deleting the MLPPeerSubscription record:", e);
		}
		return isDeletedSuccessfully;
	}
}

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
import java.util.List;

import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.domain.MLPPeerSubscription;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.service.PeerSubscriptionService;
import org.acumos.federation.gateway.service.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * CDS based implementation of PeerSubscriptionService.
 */
@Service
public class PeerSubscriptionServiceImpl extends AbstractServiceImpl implements PeerSubscriptionService {

	private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private Environment env;

	public PeerSubscriptionServiceImpl() {
	}

	@Override
	public List<MLPPeerSubscription> getPeerSubscriptions(String thePeerId) {
		log.debug(EELFLoggerDelegate.debugLogger, "getPeerSubscriptions:{}", thePeerId);
		List<MLPPeerSubscription> peerSubscriptions = getClient().getPeerSubscriptions(thePeerId);
		log.debug(EELFLoggerDelegate.debugLogger, "peer {} subscriptions : {}", thePeerId, peerSubscriptions.size());
		return peerSubscriptions;
	}

	@Override
	public MLPPeerSubscription getPeerSubscription(Long theSubId) {
		log.debug(EELFLoggerDelegate.debugLogger, "getPeerSubscription:{}", theSubId);
		MLPPeerSubscription peerSubscription = getClient().getPeerSubscription(theSubId);
		log.debug(EELFLoggerDelegate.debugLogger, "getPeerSubscription :{}", peerSubscription);
		return peerSubscription;
	}

	@Override
	public void updatePeerSubscription(MLPPeerSubscription theSubscription) throws ServiceException {
		log.debug(EELFLoggerDelegate.debugLogger, "updatePeerSubscription");
		ICommonDataServiceRestClient cdsClient = getClient();
		MLPPeerSubscription existingSubscription = cdsClient.getPeerSubscription(theSubscription.getSubId());
		//this effectively stops one from re-assigning a subscription to another peer.
		if (!theSubscription.getPeerId().equalsIgnoreCase(existingSubscription.getPeerId()))
			throw new ServiceException("Peer id mismatch with existing subscription");
		cdsClient.updatePeerSubscription(theSubscription);
	}

}

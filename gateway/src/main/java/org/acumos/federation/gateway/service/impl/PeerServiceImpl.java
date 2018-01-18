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
import org.acumos.federation.gateway.service.PeerService;
import org.acumos.federation.gateway.service.ServiceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Conditional;

import org.acumos.cds.client.ICommonDataServiceRestClient;
import org.acumos.cds.domain.MLPPeer;

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
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<MLPPeer> getPeers() {
		log.debug(EELFLoggerDelegate.debugLogger, "getPeers");
		ICommonDataServiceRestClient cdsClient = getClient();
		List<MLPPeer> mlpPeers = cdsClient.searchPeers(Collections.EMPTY_MAP,false);
		if(mlpPeers !=null) {
			log.debug(EELFLoggerDelegate.debugLogger, "getPeers size:{}", mlpPeers.size());
		}
		return mlpPeers;
	}

	@Override
	public List<MLPPeer> getPeers(ServiceContext theContext) {
		log.debug(EELFLoggerDelegate.debugLogger, "getPeers(ServiceContext)");
		return getPeers();
	}

	@Override
	public List<MLPPeer> getPeer(String subjectName) {
		log.debug(EELFLoggerDelegate.debugLogger, "savePeer");
		ICommonDataServiceRestClient cdsClient = getClient();
		Map<String, Object> queryParameters = new HashMap<String, Object>();
		queryParameters.put("subjectName", subjectName); //I believe it should be unique
		List<MLPPeer> existingMLPPeers = null;
		existingMLPPeers = cdsClient.searchPeers(queryParameters, false);
		if(existingMLPPeers != null && existingMLPPeers.size() > 0) {
			log.debug(EELFLoggerDelegate.debugLogger, "getPeer size:{}", existingMLPPeers.size());
		}
		return existingMLPPeers;
	}
	
	@Override
	public MLPPeer getOnePeer(String peerId) {
		log.debug(EELFLoggerDelegate.debugLogger, "getPeer: {}", peerId);
		ICommonDataServiceRestClient cdsClient = getClient();
		MLPPeer mlpPeer = cdsClient.getPeer(peerId);
		if(mlpPeer !=null) {
			log.error(EELFLoggerDelegate.debugLogger, "getOnePeer: {}", mlpPeer.toString());
		}
		return mlpPeer;
	}
	
	@Override
	public MLPPeer savePeer(MLPPeer mlpPeer) {
		log.debug(EELFLoggerDelegate.debugLogger, "savePeer");
		ICommonDataServiceRestClient cdsClient = getClient();
		Map<String, Object> queryParameters = new HashMap<String, Object>();
		queryParameters.put("subjectName", mlpPeer.getSubjectName()); //I believe it should be unique
		boolean isPeerExists = false;
		List<MLPPeer> existingMLPPeers = null;
		MLPPeer mlpPeerCreated = null;
		try{
			existingMLPPeers = getPeer(mlpPeer.getSubjectName());
			if(existingMLPPeers != null && existingMLPPeers.size() > 0) {
				isPeerExists = true;
				log.error(EELFLoggerDelegate.debugLogger, "savePeer");
			}
		} catch (Exception e) {
			isPeerExists = false;
			log.error(EELFLoggerDelegate.debugLogger, "savePeer: There is no existing MLPPeer for subjectName:{}, Create a record in DB", mlpPeer.getSubjectName());
		}
		
		if(!isPeerExists) {
			mlpPeerCreated = cdsClient.createPeer(mlpPeer);
			if(mlpPeerCreated !=null) {
				log.debug(EELFLoggerDelegate.debugLogger, "savePeer :{}", mlpPeer.toString());
			}
		}
		return mlpPeerCreated;
	}

	@Override
	public boolean updatePeer(MLPPeer mlpPeer) {
		log.debug(EELFLoggerDelegate.debugLogger, "updatePeer");
		ICommonDataServiceRestClient cdsClient = getClient();
		boolean isUpdatedSuccessfully = false;
		List<MLPPeer> existingMLPPeers = null;
		try{
			existingMLPPeers = getPeer(mlpPeer.getSubjectName());
			if(existingMLPPeers != null && existingMLPPeers.size() > 0) {
				cdsClient.updatePeer(mlpPeer);
				isUpdatedSuccessfully = true;
			}
		} catch (Exception e) {
			isUpdatedSuccessfully = false;
			log.error(EELFLoggerDelegate.debugLogger, "updatePeer: Exception while deleting the MLPPeer record:", e);
		}
		return isUpdatedSuccessfully;
	}

	@Override
	public boolean deletePeer(MLPPeer mlpPeer) {
		log.debug(EELFLoggerDelegate.debugLogger, "deletePeer");
		boolean isDeletedSuccessfully = false;
		ICommonDataServiceRestClient cdsClient = getClient();
		try {
			cdsClient.deletePeer(mlpPeer.getPeerId());
			isDeletedSuccessfully = true;
		} catch (Exception e) {
			isDeletedSuccessfully = false;
			log.error(EELFLoggerDelegate.debugLogger, "deletePeer: Exception while deleting the MLPPeer record:", e);
		}
		return isDeletedSuccessfully;
	}
}

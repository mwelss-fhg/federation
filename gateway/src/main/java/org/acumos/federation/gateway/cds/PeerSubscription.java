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
package org.acumos.federation.gateway.cds;

import org.acumos.cds.domain.MLPPeerSubscription;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 */
public class PeerSubscription extends MLPPeerSubscription {

	private static final SubscriptionOptions defaultOptions = new SubscriptionOptions();
	private static final ObjectMapper mapper = new ObjectMapper();

	private SubscriptionOptions opts;

	public PeerSubscription() {
	}

	public PeerSubscription(MLPPeerSubscription theCDSPeerSub) {
		super(theCDSPeerSub);
		//base class does through property assignments ..
		setOptions(theCDSPeerSub.getOptions());
	}
	
	public PeerSubscription(PeerSubscription thePeerSub) {
		super(thePeerSub);
		this.opts = thePeerSub.getSubscriptionOptions();
	}

	@Override
	public void setOptions(String theOptions) {
		super.setOptions(theOptions);
		if (theOptions == null || theOptions.length() == 0) {
			//should we make the difference between unspecified and defaulted ?
			this.opts = defaultOptions;
		}
		else {
			try {
				this.opts = mapper.readValue(theOptions, SubscriptionOptions.class);
			}
			catch (Exception x) {
				throw new IllegalArgumentException("Falied to read options value " + theOptions, x);
			}
		}		
	}

	@JsonIgnore
	public SubscriptionOptions getSubscriptionOptions() {
		return opts;
	}

	@JsonIgnore
	public void setSubscriptionOptions(SubscriptionOptions theOptions) {
		this.opts = theOptions;
		try {
			super.setOptions(theOptions == null ? null
																					: mapper.writeValueAsString(theOptions));
		}
		catch(JsonProcessingException jpx) {
			throw new IllegalArgumentException("Cannot serialize", jpx);
		}
	}

	public static class SubscriptionOptions {
	
		public CatalogUpdateOptions catalogUpdate = CatalogUpdateOptions.onSuccess;

		public boolean alwaysUpdateCatalog() {
			return this.catalogUpdate.equals(CatalogUpdateOptions.always);
		}
	}

	public static enum CatalogUpdateOptions {
		onSuccess,
		always
	}

	/**
	 * Detect changes in a peer subscription. The 'modified' timestamp is not a reliable test as we
	 * we modify it outselves, so instead we look at the content.
	 * TODO: selector and options are json string, we should compare the actual json structure.
	 * @param theCurrentSub Old sub
	 * @param theNewSub New sub
	 * @return Boolean
	 */
	public static boolean isModified(MLPPeerSubscription theCurrentSub, MLPPeerSubscription theNewSub) {
		boolean res = true;

		String taskSelector = theCurrentSub.getSelector(),
					 peerSelector = theNewSub.getSelector();
		res &= ((taskSelector != null && peerSelector == null) ||
						(taskSelector == null && peerSelector != null) ||
					  (taskSelector != null && peerSelector != null && !taskSelector.equals(peerSelector)));

		String taskOptions = theCurrentSub.getOptions(),
					 peerOptions = theNewSub.getOptions();
		res &= ((taskOptions != null && peerOptions == null) ||
						(taskOptions == null && peerOptions != null) ||
					  (taskOptions != null && peerOptions != null && !taskOptions.equals(peerOptions)));

		Long taskRefresh = theCurrentSub.getRefreshInterval(),
				 peerRefresh = theNewSub.getRefreshInterval();
		res &= ((taskRefresh != null && peerRefresh == null) ||
						(taskRefresh == null && peerRefresh != null) ||
					  (taskRefresh != null && peerRefresh != null && !taskRefresh.equals(peerRefresh)));

		//cannot be null
		res &= !theCurrentSub.getScopeType().equals(theNewSub.getScopeType());
		res &= !theCurrentSub.getUserId().equals(theNewSub.getUserId());
		res &= !theCurrentSub.getPeerId().equals(theNewSub.getPeerId());
		res &= !theCurrentSub.getAccessType().equals(theNewSub.getAccessType());

		return res;
	}


}


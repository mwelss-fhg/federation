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
package org.acumos.federation.gateway.config;

/**
 * 
 *
 */
public class APINames {
	 //Solutions APIs for MarketPlace Catalog
    public static final String PEER_SOLUTIONS = "/solutions";
    public static final String PEER_SOLUTION_DETAILS = "/solutions/{solutionId}";
    
    public static final String PEER_SOLUTION_REVISIONS = "/solutions/{solutionId}/revisions";
    public static final String PEER_SOLUTION_REVISION_DETAILS = "/solutions/{solutionId}/revisions/{revisionId}";
    
    public static final String PEER_SOLUTION_REVISION_ARTIFACTS = "/solutions/{solutionId}/revisions/{revisionId}/artifacts";
    public static final String PEER_ARTIFACT_DETAILS = "/artifacts/{artifactId}";
    public static final String PEER_ARTIFACT_DOWNLOAD = "/artifacts/{artifactId}/download";
    
		public static final String PEER_PEERS = "/peers";
		
		//public static final String PEER_SUBSCRIBE = "/peer/subscribe";
		//public static final String PEER_UNSUBSCRIBE = "/peer/unsubscribe";
}

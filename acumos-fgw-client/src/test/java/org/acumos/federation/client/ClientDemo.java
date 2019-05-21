/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
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
package org.acumos.federation.client;

import java.util.List;

import org.acumos.cds.domain.MLPCatalog;

import org.acumos.federation.client.config.ClientConfig;
import org.acumos.federation.client.config.TlsConfig;
import org.acumos.federation.client.data.Catalog;
import org.acumos.federation.client.FederationClient;
import org.acumos.federation.client.GatewayClient;

/**
 * Demonstrates use of the Federation (public "E5" interface) and
 * Gateway (private internal interface) clients.
 */
public class ClientDemo {
	private static final String peerApiUrl = "https://public.otheracumos.org:9084";
	private static final String internalApiUrl = "https://federation-service:9011";
	private static final String keystore = "keystore.jks";
	private static final String keystorepass = "keystore_pass";
	private static final String firstpeerid = "12345678-1234-1234-1234-1234567890ab";
	private static final String secondpeerid = "cafebebe-cafe-bebe-cafe-bebecafebebe";

	public static void main(String[] args) throws Exception {
		ClientConfig cconf = new ClientConfig();
		cconf.setSsl(new TlsConfig());
		cconf.getSsl().setKeyStore(keystore);
		cconf.getSsl().setKeyStorePassword(keystorepass);
		FederationClient fedclient = new FederationClient(peerApiUrl, cconf);
		System.out.println("Checking connectivity to remote acumos using public E5 interface");
		System.out.println("Response from remote acumos is " + fedclient.ping());
		System.out.println("Listing remote acumos catalogs using public E5 interface");
		for (MLPCatalog mcat: fedclient.getCatalogs()) {
			System.out.println("Catalog " + mcat.getName() + " has " + ((Catalog)mcat).getSize() + " models");
		}
		GatewayClient gwclient = new GatewayClient(internalApiUrl, cconf);
		System.out.println("Verifying first peer access from inside Acumos using private interface");
		System.out.println("Response from peer acumos is " + gwclient.ping(firstpeerid));
		System.out.println("Fetching first peer's catalogs from inside Acumos using private interface");
		for (MLPCatalog mcat: gwclient.getCatalogs(firstpeerid)) {
			System.out.println("Catalog " + mcat.getName() + " has " + ((Catalog)mcat).getSize() + " models");
		}
		System.out.println("Fetching second peer's catalogs from inside Acumos using private interface");
		for (MLPCatalog mcat: gwclient.getCatalogs(secondpeerid)) {
			System.out.println("Catalog " + mcat.getName() + " has " + ((Catalog)mcat).getSize() + " models");
		}
	}
}

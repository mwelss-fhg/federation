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

package org.acumos.federation.gateway.security;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Set of security related utilities
 */
public class Tools {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private Tools() {
	}

	/**
	 * Extract components of an X500 name, as employed in SSL certificates.
	 * @param theName the X500 name in String represention. An IllegalArgumentException is thrown if the argument cannot be parsed accordingly.
	 * @param theParts the X500 name components the client wants to extract
	 * @return a map of requested parts and their values. Only those parts for which an entry was found will be available.
	 */
	public static  Map<String, Object> getNameParts(String theName, String... theParts) {
		log.info(" X500 name: " + theName);
		LdapName x500name = null;
		try {
			x500name = new LdapName(theName);
		}
		catch (InvalidNameException inx) {
			log.warn("Failed to parse name information '{}'", theName);
			throw new IllegalArgumentException(inx);
		}

		Map<String, Object> parts = new HashMap<String, Object>();
		for (Rdn rdn :  x500name.getRdns()) {
			for (String part: theParts) {
				if (part.equalsIgnoreCase(rdn.getType())) {
					parts.put(part, rdn.getValue());
				}
			}
		}
		return parts;
	}
}

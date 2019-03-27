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
package org.acumos.federation.gateway.cds;

import org.acumos.cds.domain.MLPCatalog;

/**
 * Supplements the CDS representation of a catalog with related information: size.
 * Allows federation to pack information passed between peers.
 */
public class Catalog extends MLPCatalog {

	private int	size;

	public Catalog() {
	}

	public Catalog(MLPCatalog theCDSCatalog) {
		super(theCDSCatalog);
	}

	public void setSize(int theSize) {
		size = theSize;
	}

	public int getSize() {
		return size;
	}

	@Override
	public String toString() {
		return super.toString() + ", size=" + size;
	}
}



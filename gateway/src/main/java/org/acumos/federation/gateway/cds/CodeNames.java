/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2017 - 2019 AT&T Intellectual Property & Tech
 * 						Mahindra. All rights reserved.
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

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.acumos.cds.CodeNameType;
import org.acumos.cds.domain.MLPCodeNamePair;
import org.acumos.federation.gateway.service.CodeNamesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The code is written to tolerate early/static references to particular codes by resolving such references only
 * when method invocations occur upon the corresponding CodeName instance.
 * @param <T> Type
 */
public abstract class CodeNames<T extends CodeName> {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static CodeNamesService codes = null;

	protected static Map<CodeNameType, List<MLPCodeNamePair>> codeNamePairs = new HashMap<>();
	protected static Map<Class, List<CodeName>> codeNames = new HashMap<>();

	@Autowired
	public void setCodeNamesService(CodeNamesService theService) {
		if (codes != null && codes != theService) {
			log.warn("Mismatched auto (RE-)wiring. Has " + codes + ". Got " + theService);
		}
		// TODO: last value wins?   if (codes == null)
		codes = theService;
	}	

	protected static List<MLPCodeNamePair> loadCodeNamePairs(CodeNameType theType) {

		List<MLPCodeNamePair> pairs = null;

		if (codes == null)
			throw new IllegalStateException("Service not available");

		try {
			pairs = codes.getCodeNames(theType);
		}
		catch(Exception x) {
			log.error("Failed to load codes for " + theType, x);
			return null;
		}

		log.debug("Loaded codes for {}: {}", theType, pairs);
		codeNamePairs.put(theType, pairs);
		return pairs;
	}

	/* generic return type so derived classes can downcast it accordingly */
	protected static List/*<CodeName>*/ codes(Class theType) {
		return codeNames.get(theType);
	}

	/**
	 * There is weakness in here as it can be called with different code types within the scope of the same container ..
	 * @param <T> Type
	 * @param theCode Code
	 * @param theType Code type
	 * @return Code
	 */	
	protected static <T extends CodeName> T forCode(final String theCode, Class<T> theType) {
		log.info("forCode {}: {}", theType, theCode); 
		synchronized (codeNames) {
			List<CodeName> codes = codeNames.get(theType);
			if (codes == null) {
				codes = new LinkedList<CodeName>();
				codeNames.put(theType, codes);
			}
			//cannot call the CodeName.getCode in here as it will trigger an attempt to load the codes
			CodeName code = codes.stream().filter(c -> ((CodeNameHandler)Proxy.getInvocationHandler(c)).getCode().equals(theCode)).findFirst().orElse(null);	
			if (code == null) {
				code = (CodeName)Proxy.newProxyInstance(CodeNames.class.getClassLoader(), new Class[] { theType }, new CodeNameHandler(theCode));
				codes.add(code);
			}
			return (T)code;
		}
	}

	protected static MLPCodeNamePair getCodeNamePair(CodeNameType theType, String theCode) {
		synchronized (codeNamePairs) {
			List<MLPCodeNamePair> pairs = codeNamePairs.get(theType);
			if (pairs == null) {
				pairs = loadCodeNamePairs(theType);
			}

			return (pairs != null) ? pairs.stream().filter(pair -> pair.getCode().equals(theCode)).findFirst().orElse(null) : null;
		}
	}


	static class CodeNameHandler implements InvocationHandler {

		private String code;
		private MLPCodeNamePair pair;

		private CodeNameHandler(String theCode) {
			this.code = theCode;
		}

		public String getCode() {
			return this.code;
		}

		public Object invoke(Object theProxy, Method theMethod, Object[] theArgs) throws Throwable {
		
			if (this.pair == null) {
				CodeNameType theType = invokeGetType(theProxy);
				this.pair = getCodeNamePair(theType, this.code);
				if (this.pair == null)
					throw new IllegalArgumentException("failed to find code: " + this.code + " in type: " + theType.name());
			}

			if (theMethod.getName().equals("getType")) {
				return invokeGetType(theProxy);
			}
			if (theMethod.getName().equals("getCode")) {
				return this.pair.getCode();
			}
			if (theMethod.getName().equals("getName")) {
				return this.pair.getName();
			}
			if (theMethod.getName().equals("equals")) {
				CodeName other = (CodeName)theArgs[0];
				return this.pair.getCode().equals(other.getCode()) &&
							 this.pair.getName().equals(other.getName()); //we should also test the type ..
			}
			if (theMethod.getName().equals("toString")) {
				return this.pair.getCode().toString();
			}
			throw new IllegalArgumentException("Unexpected CodeName call: " + theMethod);
		}

		private CodeNameType invokeGetType(Object theProxy) throws Throwable {
			Class tgt = theProxy.getClass().getInterfaces()[0];
			Method tgtMethod = tgt.getDeclaredMethod("getType");

			if (tgtMethod == null || !tgtMethod.isDefault())
				throw new RuntimeException("invokeGetType: give a default definition to getType in an actual CodeName");

			Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class
                .getDeclaredConstructor(Class.class, int.class);
			constructor.setAccessible(true);
 
			CodeNameType type = (CodeNameType)
				constructor.newInstance(tgt, MethodHandles.Lookup.PRIVATE)
                .unreflectSpecial(tgtMethod, tgt)
											 .bindTo(theProxy)
											 .invokeWithArguments();

			log.debug("invokeGetType: found type {} for code {}", type, this.code);
			return type;	
		}
	}

}



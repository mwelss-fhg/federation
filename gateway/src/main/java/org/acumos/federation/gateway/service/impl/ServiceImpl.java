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
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.acumos.cds.domain.MLPSolution;
import org.acumos.cds.domain.MLPTag;
import org.acumos.federation.gateway.cds.Solution;
import org.acumos.federation.gateway.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some basic tooling for service implementation.
 * Common functionality to be re-used across service implementations.
 */
public abstract class ServiceImpl {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private ServiceImpl() {
	}


	/**
	 * Returns a predicate equivalent to the logical AND of any non-null argument predicates.
	 * @param preds predicates to combine
	 * @return a predicate computing the logical AND of any non-null arguments or a predicate returning true, if there are none
	 */
	private static Predicate<MLPSolution> and(Predicate<MLPSolution> ... preds) {
		Predicate<MLPSolution> ret = null;
		for (Predicate<MLPSolution> x: preds) {
			if (ret == null) {
				ret = x;
			} else if (x != null) {
				ret = ret.and(x);
			}
		}
		if (ret == null) {
			ret = (arg) -> true;
		}
		return(ret);
	}


	/**
	 * Returns a predicate determining matching against a multi-valued field.
	 * The returned predicate will be called with an MLPSolution.  The
	 * Function specified by field will be invoked on it, to extract a Set
	 * of Strings, which will be tested against the value, in theSelector,
	 * corresponding to key.
	 * If theSelector does not contain key, this just returns null.
	 * Otherwise, if the value is a String, this returns a predicate
	 * computing whether the value is in the extracted Set of Strings.
	 * Otherwise, if the value is a List, this returns a predicate
	 * computing whether any of the values in the List is contained in
	 * the Set of Strings.
	 * @param theSelector a map of field names to expected values
	 * @param key the field name to be handled by this predicate
	 * @param field the Function to extract the field value from the Solution
	 * @param listok whether this field supports a list of values in the selector
	 * @return the predicate for testing the field value
	 * @throws ServiceException if the value of key, in theSelector is neither a String nor a List.
	 */

	private static Predicate<MLPSolution> contains(Map<String, ?> theSelector, String key, Function<MLPSolution, Set<String>> field, boolean listok) throws ServiceException {
		Object o = theSelector.get(key);
		if (o == null) {
			return(null);
		}
		log.trace("using {} based selection {}", key, o);
		if (o instanceof String) {
			String s = (String)o;
			return(arg-> field.apply(arg).contains(s));
		}
		if (listok && o instanceof List) {
			List l = (List)o;
			return(arg -> {
				for (Object val: field.apply(arg)) {
					if (l.contains(val)) {
						return(true);
					}
				}
				return(false);
			});
		}
		log.debug("unknown {} criteria representation {}", key, o.getClass().getName());
		throw new ServiceException("Invalid Selector");
	}


	/**
	 * Returns a predicate determining matching against a field.
	 * The returned predicate will be called with an MLPSolution.  The
	 * Function specified by field will be invoked on it, to extract its
	 * value, which will be tested against the value, in theSelector,
	 * corresponding to key.
	 * If theSelector does not contain key, this just returns null.
	 * Otherwise, if theSelector contains a String, this returns a predicate
	 * computing whether the value equals the extracted value.
	 * Otherwise, if the value is a List, this returns a predicate
	 * computing whether the extracted value is contained in the List.
	 * @param theSelector a map of field names to expected values
	 * @param key the field name to be handled by this predicate
	 * @param field the Function to extract the field value from the Solution
	 * @param listok whether this field supports a list of values in the selector
	 * @return the predicate for testing the field value
	 * @throws ServiceException if the value of key, in theSelector is neither a String nor a List.
	 */

	private static Predicate<MLPSolution> has(Map<String, ?> theSelector, String key, Function<MLPSolution, String> field, boolean listok) throws ServiceException {
		Object o = theSelector.get(key);
		if (o == null) {
			return(null);
		}
		log.trace("using {} based selection {}", key, o);
		if (o instanceof String) {
			String s = (String)o;
			return(arg -> s.equals(field.apply(arg)));
		}
		if (listok && o instanceof List) {
			List l = (List)o;
			return(arg -> l.contains(field.apply(arg)));
		}
		log.debug("unknown {} criteria representation {}", key, o.getClass().getName());
		throw new ServiceException("Invalid Selector");
	}


	/**
	 * Returns a predicate for testing an MLPSolution against a selector.
	 * @param theSelector the criteria to be met in a matching solution
	 * @return a predicate for checking for matching solutions
	 * @throws ServiceException if theSelector is malformed
	 */

	public static Predicate<MLPSolution> compileSelector(Map<String, ?> theSelector) throws ServiceException {
		if (theSelector == null) {
			return(arg -> true);
		}
		log.trace("compileSelector {}", theSelector);
		Boolean ao = (Boolean)theSelector.get(Solution.Fields.active);
		boolean active = ao == null? true: ao.booleanValue();
		Instant since = Instant.ofEpochSecond((Long)theSelector.get(Solution.Fields.modified));
		return(and(
			arg -> arg.isActive() == active,
			arg -> arg.getModified().compareTo(since) >= 0,
			has(theSelector, Solution.Fields.solutionId, arg -> arg.getSolutionId(), false),
			has(theSelector, Solution.Fields.modelTypeCode, arg -> arg.getModelTypeCode(), true),
			has(theSelector, Solution.Fields.toolkitTypeCode, arg -> arg.getToolkitTypeCode(), true),
			contains(theSelector, Solution.Fields.tags, arg -> {
				Set<String> ret = new HashSet<String>();
				for (MLPTag tag: arg.getTags()) {
					ret.add(tag.getTag());
				}
				return(ret);
			}, true),
			has(theSelector, Solution.Fields.name, arg ->arg.getName(), false)
		));
	}
}

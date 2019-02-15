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

package org.acumos.federation.gateway.util;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

public class Futures<T> {

	private Futures() {
	}

	public static <T> Future<T> failedFuture(Throwable theError) {
		return new BasicFuture<T>().cause(theError);
	}

	public static <T> Future<T> succeededFuture(T theResult) {
		return new BasicFuture<T>().result(theResult);
	}

	public static <T> Future<T> future() {
		return new BasicFuture<T>();
	}

	public static <U, V> Future<V> advance(Future<U> theStep, final Function<U, V> theResultFunction) {
		return advance(theStep, theResultFunction, Function.identity());
	}

	public static <U, V> Future<V> advance(Future<U> theStep, final Function<U, V> theResultFunction,
			final Function<Throwable, Throwable> theErrorFunction) {
		final Future<V> adv = new BasicFuture<V>();
		theStep.setHandler(new FutureHandler<U>() {
			public void handle(Future<U> theResult) {
				if (theResult.failed())
					adv.cause(theErrorFunction.apply(theResult.cause()));
				else
					adv.result(theResultFunction.apply(theResult.result()));
			}
		});
		return adv;
	}

	public static class BasicFuture<T> implements Future<T> {

		protected boolean succeeded, failed;

		protected FutureHandler<T> handler;
		protected Throwable cause;
		protected T result;

		protected BasicFuture() {
		}

		public T result() {
			return this.result;
		}

		public Future<T> result(T theResult) {
			this.result = theResult;
			this.succeeded = true;
			this.cause = null;
			this.failed = false;
			callHandler();
			return this;
		}

		public Throwable cause() {
			return this.cause;
		}

		public Future<T> cause(Throwable theCause) {
			this.cause = theCause;
			this.failed = true;
			this.result = null;
			this.succeeded = false;
			callHandler();
			return this;
		}

		public boolean succeeded() {
			return this.succeeded;
		}

		public boolean failed() {
			return this.failed;
		}

		public boolean complete() {
			return this.failed || this.succeeded;
		}

		public Future<T> setHandler(FutureHandler<T> theHandler) {
			this.handler = theHandler;
			callHandler();
			return this;
		}

		public T waitForResult() throws Exception {
			BasicHandler<T> hnd = buildHandler();
			setHandler(hnd);
			hnd.waitForCompletion();
			if (failed())
				throw (Exception) cause();
			else
				return result();
		}

		public Future<T> waitForCompletion() throws InterruptedException {
			BasicHandler<T> hnd = buildHandler();
			setHandler(hnd);
			hnd.waitForCompletion();
			return this;
		}

		protected void callHandler() {
			if (this.handler != null && complete()) {
				this.handler.handle(this);
			}
		}

		protected BasicHandler<T> buildHandler() {
			return new BasicHandler<T>();
		}
	}

	public static class BasicHandler<T> implements FutureHandler<T> {

		protected T result = null;
		protected Throwable error = null;
		protected CountDownLatch latch = null;

		BasicHandler() {
			this(new CountDownLatch(1));
		}

		BasicHandler(CountDownLatch theLatch) {
			this.latch = theLatch;
		}

		public void handle(Future<T> theResult) {
			process(theResult);
			if (this.latch != null) {
				this.latch.countDown();
			}
		}

		protected void process(Future<T> theResult) {
			if (theResult.failed()) {
				this.error = theResult.cause();
			} else {
				this.result = theResult.result();
			}
		}

		public T result(boolean doWait) throws InterruptedException, RuntimeException {
			if (doWait) {
				waitForCompletion();
			}
			if (null == this.error)
				return this.result;

			throw new RuntimeException(this.error);
		}

		public T result() throws InterruptedException, RuntimeException {
			return result(true);
		}

		public BasicHandler<T> waitForCompletion() throws InterruptedException {
			this.latch.await();
			return this;
		}
	}

	public static class Accumulator<T> extends BasicFuture<List<T>> implements Future<List<T>> {

		protected List<Future<T>> futures = new LinkedList<Future<T>>();
		// protected List<T> results = new LinkedList<T>();
		protected BasicHandler<T> handler = null;

		public Accumulator() {
			this.result = new LinkedList<T>();
		}

		public Accumulator<T> add(Future<T> theFuture) {
			System.out.println("Intersection add");

			this.futures.add(theFuture);
			this.result.add(null);
			return this;
		}

		public Accumulator<T> addAll(Accumulator<T> theFutures) {

			System.out.println("Intersection addAll");

			return this;
		}

		public Future<List<T>> accumulate() {
			this.futures = Collections.unmodifiableList(this.futures);
			this.handler = new BasicHandler<T>(new CountDownLatch(this.futures.size())) {
				protected void process(Future<T> theResult) {
					if (theResult.failed()) {
						Accumulator.this.cause = theResult.cause();
					} else {
						Accumulator.this.result.set(Accumulator.this.futures.indexOf(theResult), theResult.result());
					}
					// System.out.println(Accumulator.this.futures.indexOf(theResult) + "
					// completed");
					if (this.latch.getCount() == 1) {
						if (Accumulator.this.cause != null)
							Accumulator.this.cause(Accumulator.this.cause);
						else
							Accumulator.this.result(Accumulator.this.result);
					}
				}
			};
			futures.stream().forEach(f -> f.setHandler(this.handler));

			return this;
		}

	}

}

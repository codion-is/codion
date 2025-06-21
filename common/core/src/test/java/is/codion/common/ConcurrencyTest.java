/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.common;

import is.codion.common.event.Event;
import is.codion.common.state.State;
import is.codion.common.value.Value;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for thread safety of Observable components' listener management.
 * Only tests concurrent add/remove of listeners, as the reactive constructs
 * themselves are designed for single-threaded use on the EDT.
 */
public class ConcurrencyTest {

	private static final int THREAD_COUNT = 20;
	private static final int ITERATIONS = 1000;

	@Test
	@Timeout(15)
	void valueListenerConcurrentAddRemove() throws InterruptedException {
		Value<String> value = Value.nonNull("");
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
		AtomicInteger errors = new AtomicInteger();

		ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

		for (int i = 0; i < THREAD_COUNT; i++) {
			executor.submit(() -> {
				try {
					startLatch.await();

					List<Runnable> listeners = new ArrayList<>();
					for (int j = 0; j < ITERATIONS; j++) {
						Runnable listener = () -> {
							// Listener work
						};
						listeners.add(listener);

						// Add and remove listeners concurrently
						boolean added = value.addListener(listener);
						assertTrue(added, "Listener should be added successfully");

						// Sometimes try to add again (should return false)
						if (j % 10 == 0) {
							assertFalse(value.addListener(listener), "Duplicate listener should not be added");
						}

						boolean removed = value.removeListener(listener);
						assertTrue(removed, "Listener should be removed successfully");

						// Try to remove again (should return false)
						assertFalse(value.removeListener(listener), "Already removed listener should return false");
					}
				}
				catch (Exception e) {
					errors.incrementAndGet();
					e.printStackTrace();
				}
				finally {
					doneLatch.countDown();
				}
			});
		}

		startLatch.countDown();
		assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
		executor.shutdown();

		assertEquals(0, errors.get(), "No errors should occur during concurrent listener operations");
	}

	@Test
	@Timeout(15)
	void stateListenerConcurrentAddRemove() throws InterruptedException {
		State state = State.state();
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
		AtomicInteger errors = new AtomicInteger();

		ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

		for (int i = 0; i < THREAD_COUNT; i++) {
			executor.submit(() -> {
				try {
					startLatch.await();

					List<Runnable> listeners = new ArrayList<>();
					for (int j = 0; j < ITERATIONS; j++) {
						Runnable listener = () -> {
							// Listener work
						};
						listeners.add(listener);

						boolean added = state.addListener(listener);
						assertTrue(added);

						boolean removed = state.removeListener(listener);
						assertTrue(removed);
					}
				}
				catch (Exception e) {
					errors.incrementAndGet();
					e.printStackTrace();
				}
				finally {
					doneLatch.countDown();
				}
			});
		}

		startLatch.countDown();
		assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
		executor.shutdown();

		assertEquals(0, errors.get());
	}

	@Test
	@Timeout(15)
	void eventListenerConcurrentAddRemove() throws InterruptedException {
		Event<Integer> event = Event.event();
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT * 2);
		AtomicInteger errors = new AtomicInteger();

		ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT * 2);

		// Half threads add/remove listeners
		for (int i = 0; i < THREAD_COUNT; i++) {
			executor.submit(() -> {
				try {
					startLatch.await();

					List<Runnable> listeners = new ArrayList<>();
					for (int j = 0; j < ITERATIONS; j++) {
						Runnable listener = () -> {
							// Listener work
						};
						listeners.add(listener);

						boolean added = event.addListener(listener);
						assertTrue(added);
						assertFalse(event.addWeakListener(listener), "Can't add weak if strong exists");

						boolean removed = event.removeListener(listener);
						assertTrue(removed);
					}
				}
				catch (Exception e) {
					errors.incrementAndGet();
					e.printStackTrace();
				}
				finally {
					doneLatch.countDown();
				}
			});
		}

		// Other half add/remove consumers
		for (int i = 0; i < THREAD_COUNT; i++) {
			executor.submit(() -> {
				try {
					startLatch.await();

					List<Consumer<Integer>> consumers = new ArrayList<>();
					for (int j = 0; j < ITERATIONS; j++) {
						Consumer<Integer> consumer = value -> {
							// Consumer work
						};
						consumers.add(consumer);

						boolean added = event.addConsumer(consumer);
						assertTrue(added);
						assertFalse(event.addWeakConsumer(consumer), "Can't add weak if strong exists");

						boolean removed = event.removeConsumer(consumer);
						assertTrue(removed);
					}
				}
				catch (Exception e) {
					errors.incrementAndGet();
					e.printStackTrace();
				}
				finally {
					doneLatch.countDown();
				}
			});
		}

		startLatch.countDown();
		assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
		executor.shutdown();

		assertEquals(0, errors.get());
	}

	@RepeatedTest(5) // Repeat to catch intermittent failures
	@Timeout(15)
	void weakListenerConcurrentAddRemove() throws InterruptedException {
		Value<Integer> value = Value.nonNull(0);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
		AtomicInteger errors = new AtomicInteger();

		ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

		for (int i = 0; i < THREAD_COUNT; i++) {
			executor.submit(() -> {
				try {
					startLatch.await();

					for (int j = 0; j < ITERATIONS / 10; j++) { // Fewer iterations for weak refs
						Runnable listener = () -> {
							// Listener work
						};

						// Test weak listener add/remove
						boolean added = value.addWeakListener(listener);
						assertTrue(added);

						// Can't add strong if weak exists
						assertFalse(value.addListener(listener));

						boolean removed = value.removeListener(listener);
						assertTrue(removed);

						// Now can add strong again
						assertTrue(value.addListener(listener));
						assertTrue(value.removeListener(listener));
					}
				}
				catch (Exception e) {
					errors.incrementAndGet();
					e.printStackTrace();
				}
				finally {
					doneLatch.countDown();
				}
			});
		}

		startLatch.countDown();
		assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
		executor.shutdown();

		assertEquals(0, errors.get());
	}

	@Test
	@Timeout(15)
	void mixedListenerOperations() throws InterruptedException {
		Event<String> event = Event.event();
		State state = State.state();
		Value<Integer> value = Value.nonNull(0);

		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT * 3);
		AtomicInteger errors = new AtomicInteger();

		ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT * 3);

		// Test Event listeners
		for (int i = 0; i < THREAD_COUNT; i++) {
			executor.submit(() -> {
				try {
					startLatch.await();
					testListenerOperations(event::addListener, event::removeListener);
				}
				catch (Exception e) {
					errors.incrementAndGet();
				}
				finally {
					doneLatch.countDown();
				}
			});
		}

		// Test State listeners
		for (int i = 0; i < THREAD_COUNT; i++) {
			executor.submit(() -> {
				try {
					startLatch.await();
					testListenerOperations(state::addListener, state::removeListener);
				}
				catch (Exception e) {
					errors.incrementAndGet();
				}
				finally {
					doneLatch.countDown();
				}
			});
		}

		// Test Value listeners
		for (int i = 0; i < THREAD_COUNT; i++) {
			executor.submit(() -> {
				try {
					startLatch.await();
					testListenerOperations(value::addListener, value::removeListener);
				}
				catch (Exception e) {
					errors.incrementAndGet();
				}
				finally {
					doneLatch.countDown();
				}
			});
		}

		startLatch.countDown();
		assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
		executor.shutdown();

		assertEquals(0, errors.get());
	}

	private void testListenerOperations(
					java.util.function.Function<Runnable, Boolean> addListener,
					java.util.function.Function<Runnable, Boolean> removeListener) {

		for (int i = 0; i < ITERATIONS; i++) {
			Runnable listener = () -> {
				// Listener work
			};

			// Test add
			assertTrue(addListener.apply(listener));
			assertFalse(addListener.apply(listener)); // Duplicate

			// Test remove
			assertTrue(removeListener.apply(listener));
			assertFalse(removeListener.apply(listener)); // Already removed
		}
	}
}
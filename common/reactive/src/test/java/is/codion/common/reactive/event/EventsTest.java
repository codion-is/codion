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
 * Copyright (c) 2009 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.event;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class EventsTest {

	@Test
	void basicEventNotification() {
		Event<Integer> event = Event.event();
		AtomicInteger listenerCounter = new AtomicInteger();
		AtomicInteger consumerCounter = new AtomicInteger();
		AtomicReference<Integer> consumerValue = new AtomicReference<>();

		Runnable listener = listenerCounter::incrementAndGet;
		Consumer<Integer> consumer = value -> {
			consumerCounter.incrementAndGet();
			consumerValue.set(value);
		};

		event.addListener(listener);
		event.addConsumer(consumer);

		// Test run() - notifies both listeners and consumers with null data
		event.run();
		assertEquals(1, listenerCounter.get());
		assertEquals(1, consumerCounter.get());
		assertNull(consumerValue.get());

		// Test accept() - notifies both listeners and consumers with data
		event.accept(42);
		assertEquals(2, listenerCounter.get());
		assertEquals(2, consumerCounter.get());
		assertEquals(42, consumerValue.get());

		// Test removal
		event.removeListener(listener);
		event.removeConsumer(consumer);
		event.run();
		assertEquals(2, listenerCounter.get());
		assertEquals(2, consumerCounter.get());
	}

	@Test
	void listenerNotificationOrder() {
		Event<String> event = Event.event();
		List<Integer> executionOrder = new ArrayList<>();

		event.addListener(() -> executionOrder.add(1));
		event.addListener(() -> executionOrder.add(2));
		event.addListener(() -> executionOrder.add(3));

		event.run();

		assertEquals(Arrays.asList(1, 2, 3), executionOrder);
	}

	@Test
	void consumerNotificationOrder() {
		Event<String> event = Event.event();
		List<String> receivedValues = new ArrayList<>();

		event.addConsumer(value -> receivedValues.add("1:" + value));
		event.addConsumer(value -> receivedValues.add("2:" + value));
		event.addConsumer(value -> receivedValues.add("3:" + value));

		event.accept("test");

		assertEquals(Arrays.asList("1:test", "2:test", "3:test"), receivedValues);
	}

	@Test
	void preventDuplicates() {
		Event<Integer> event = Event.event();
		Runnable listener = () -> {};
		Consumer<Integer> consumer = integer -> {};
		assertTrue(event.addListener(listener));
		assertFalse(event.addListener(listener));
		assertFalse(event.addWeakListener(listener));
		assertTrue(event.addConsumer(consumer));
		assertFalse(event.addConsumer(consumer));
		assertFalse(event.addWeakConsumer(consumer));

		assertTrue(event.removeListener(listener));
		assertFalse(event.removeListener(listener));
		assertTrue(event.addWeakListener(listener));
		assertFalse(event.addListener(listener));

		assertTrue(event.removeConsumer(consumer));
		assertFalse(event.removeConsumer(consumer));
		assertTrue(event.addWeakConsumer(consumer));
		assertFalse(event.addConsumer(consumer));
	}

	@Test
	void eventAsConsumer() {
		// DefaultObserver bug in 0.18.23
		Event<Integer> event = Event.event();
		Event<Integer> consumer = Event.event();

		event.addConsumer(consumer);

		consumer.addConsumer(Assertions::assertNotNull);

		event.accept(1);
	}

	@Test
	void exceptionInListenerPreventsSubsequentListeners() {
		Event<String> event = Event.event();
		AtomicInteger counter = new AtomicInteger();

		event.addListener(() -> counter.incrementAndGet());
		event.addListener(() -> {
			throw new RuntimeException("Test exception");
		});
		event.addListener(() -> counter.incrementAndGet()); // Should not execute

		assertThrows(RuntimeException.class, event::run);
		assertEquals(1, counter.get()); // Only first listener executed
	}

	@Test
	void exceptionInConsumerPreventsSubsequentConsumers() {
		Event<String> event = Event.event();
		AtomicInteger counter = new AtomicInteger();

		event.addConsumer(s -> counter.incrementAndGet());
		event.addConsumer(s -> {
			throw new RuntimeException("Test exception");
		});
		event.addConsumer(s -> counter.incrementAndGet()); // Should not execute

		assertThrows(RuntimeException.class, () -> event.accept("test"));
		assertEquals(1, counter.get()); // Only first consumer executed
	}

	@Test
	void nullDataPropagation() {
		Event<String> event = Event.event();
		AtomicReference<String> receivedValue = new AtomicReference<>("initial");
		AtomicBoolean consumerCalled = new AtomicBoolean(false);

		event.addConsumer(value -> {
			consumerCalled.set(true);
			receivedValue.set(value);
		});

		// Test explicit null
		event.accept(null);
		assertTrue(consumerCalled.get());
		assertNull(receivedValue.get());

		// Reset
		receivedValue.set("reset");
		consumerCalled.set(false);

		// Test run() which passes null to consumers
		event.run();
		assertTrue(consumerCalled.get());
		assertNull(receivedValue.get());
	}

	@Test
	void concurrentListenerAddRemove() throws InterruptedException {
		Event<Integer> event = Event.event();
		int threadCount = 10;
		int operationsPerThread = 1000;

		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(threadCount);
		AtomicInteger errors = new AtomicInteger();

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);

		for (int i = 0; i < threadCount; i++) {
			int threadId = i;
			executor.submit(() -> {
				try {
					startLatch.await();

					List<Runnable> listeners = new ArrayList<>();
					for (int j = 0; j < operationsPerThread; j++) {
						int listenerId = j;
						Runnable listener = () -> {
							// Some work
							String.valueOf(threadId + listenerId);
						};
						listeners.add(listener);

						event.addListener(listener);
						if (j % 2 == 0) {
							event.run(); // Trigger event occasionally
						}
						event.removeListener(listener);
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

		startLatch.countDown(); // Start all threads simultaneously
		assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
		executor.shutdown();

		assertEquals(0, errors.get());
	}

	@Test
	void concurrentEventTrigger() throws InterruptedException {
		Event<Integer> event = Event.event();
		int threadCount = 10;
		int eventsPerThread = 1000;

		AtomicInteger listenerCounter = new AtomicInteger();
		AtomicInteger consumerSum = new AtomicInteger();

		event.addListener(listenerCounter::incrementAndGet);
		event.when(Objects::nonNull).addConsumer(consumerSum::addAndGet);

		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(threadCount);

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);

		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				try {
					startLatch.await();
					for (int j = 0; j < eventsPerThread; j++) {
						if (j % 2 == 0) {
							event.run();
						}
						else {
							event.accept(1);
						}
					}
				}
				catch (Exception e) {
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

		int totalEvents = threadCount * eventsPerThread;
		assertEquals(totalEvents, listenerCounter.get());
		assertEquals(totalEvents / 2, consumerSum.get()); // Half the events used accept(1)
	}

	@Test
	void recursiveEventTrigger() {
		Event<Integer> event = Event.event();
		AtomicInteger depth = new AtomicInteger();
		AtomicInteger maxDepthReached = new AtomicInteger();

		event.addConsumer(value -> {
			int currentDepth = depth.incrementAndGet();
			maxDepthReached.updateAndGet(max -> Math.max(max, currentDepth));

			if (currentDepth < 10) {
				event.accept(value + 1); // Recursive call
			}

			depth.decrementAndGet();
		});

		event.accept(1);

		assertEquals(10, maxDepthReached.get());
		assertEquals(0, depth.get()); // Should return to 0 after recursion completes
	}

	@Test
	void concurrentModificationDuringNotification() throws InterruptedException {
		Event<String> event = Event.event();
		CountDownLatch notificationStarted = new CountDownLatch(1);
		CountDownLatch modificationAttempted = new CountDownLatch(1);
		AtomicBoolean listenerExecuted = new AtomicBoolean();
		AtomicBoolean newListenerExecuted = new AtomicBoolean();
		AtomicBoolean listenerAdded = new AtomicBoolean();

		Runnable newListener = () -> newListenerExecuted.set(true);

		event.addListener(() -> {
			listenerExecuted.set(true);
			notificationStarted.countDown();
			try {
				// Wait for the other thread to attempt modification
				assertTrue(modificationAttempted.await(5, TimeUnit.SECONDS),
								"Modification thread should attempt to add listener");
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});

		Thread notifyThread = new Thread(event::run);
		Thread modifyThread = new Thread(() -> {
			try {
				// Wait for notification to start
				assertTrue(notificationStarted.await(5, TimeUnit.SECONDS),
								"Notification should start");
				// Try to add listener during notification
				listenerAdded.set(event.addListener(newListener));
				modificationAttempted.countDown();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});

		notifyThread.start();
		modifyThread.start();

		assertTrue(join(notifyThread, 10000), "Notify thread should complete");
		assertTrue(join(modifyThread, 10000), "Modify thread should complete");

		assertTrue(listenerExecuted.get(), "Original listener should have executed");
		assertTrue(listenerAdded.get(), "Should be able to add listener during notification");

		// The new listener should not have been called during the first notification
		// because it was added after the snapshot was taken
		assertFalse(newListenerExecuted.get(), "New listener should not execute in first notification");

		// But should be called on the next notification
		event.run();
		assertTrue(newListenerExecuted.get(), "New listener should execute in second notification");
	}

	private boolean join(Thread thread, long timeout) throws InterruptedException {
		thread.join(timeout);
		return !thread.isAlive();
	}
}

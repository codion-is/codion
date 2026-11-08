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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class EventTest {

	@Test
	void basicEventNotification() {
		Event event = Event.event();
		AtomicInteger listenerCounter = new AtomicInteger();
		Runnable listener = listenerCounter::incrementAndGet;

		event.addListener(listener);
		event.run();
		assertEquals(1, listenerCounter.get());

		// Test removal
		event.removeListener(listener);
		event.run();
		assertEquals(1, listenerCounter.get());
	}

	@Test
	void listenerNotificationOrder() {
		Event event = Event.event();
		List<Integer> executionOrder = new ArrayList<>();

		event.addListener(() -> executionOrder.add(1));
		event.addListener(() -> executionOrder.add(2));
		event.addListener(() -> executionOrder.add(3));

		event.run();

		assertEquals(Arrays.asList(1, 2, 3), executionOrder);
	}

	@Test
	void preventDuplicates() {
		Event event = Event.event();
		Runnable listener = () -> {};
		assertTrue(event.addListener(listener));
		assertFalse(event.addListener(listener));
		assertFalse(event.addWeakListener(listener));

		assertTrue(event.removeListener(listener));
		assertFalse(event.removeListener(listener));
		assertTrue(event.addWeakListener(listener));
		assertFalse(event.addListener(listener));
	}

	@Test
	void eventAsListener() {
		Event event = Event.event();
		Event listener = Event.event();
		AtomicInteger counter = new AtomicInteger();

		event.addListener(listener);
		listener.addListener(counter::incrementAndGet);

		event.run();
		assertEquals(1, counter.get());
	}

	@Test
	void exceptionInListenerPreventsSubsequentListeners() {
		Event event = Event.event();
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
	void sensorView() {
		Event event = Event.event();
		AtomicInteger counter = new AtomicInteger();
		// the sensor can not trigger the event, listeners added to it are the event's
		event.sensor().addListener(counter::incrementAndGet);
		event.run();
		assertEquals(1, counter.get());
	}

	@Test
	void concurrentListenerAddRemove() throws InterruptedException {
		Event event = Event.event();
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
	void concurrentModificationDuringNotification() throws InterruptedException {
		Event event = Event.event();
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

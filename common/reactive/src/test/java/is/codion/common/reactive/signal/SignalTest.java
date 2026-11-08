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
package is.codion.common.reactive.signal;

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

public class SignalTest {

	@Test
	void basicSignalNotification() {
		Signal<Integer> signal = Signal.signal();
		AtomicInteger listenerCounter = new AtomicInteger();
		AtomicInteger consumerCounter = new AtomicInteger();
		AtomicReference<Integer> consumerValue = new AtomicReference<>();

		Runnable listener = listenerCounter::incrementAndGet;
		Consumer<Integer> consumer = value -> {
			consumerCounter.incrementAndGet();
			consumerValue.set(value);
		};

		signal.addListener(listener);
		signal.addConsumer(consumer);

		// accept() notifies both listeners and consumers, the consumers with the data
		signal.accept(42);
		assertEquals(1, listenerCounter.get());
		assertEquals(1, consumerCounter.get());
		assertEquals(42, consumerValue.get());

		// Test removal
		signal.removeListener(listener);
		signal.removeConsumer(consumer);
		signal.accept(1);
		assertEquals(1, listenerCounter.get());
		assertEquals(1, consumerCounter.get());
	}

	@Test
	void listenersAndConsumersInOrderAdded() {
		Signal<String> signal = Signal.signal();
		List<String> executionOrder = new ArrayList<>();

		signal.addListener(() -> executionOrder.add("listener 1"));
		signal.addConsumer(value -> executionOrder.add("consumer 1:" + value));
		signal.addListener(() -> executionOrder.add("listener 2"));
		signal.addConsumer(value -> executionOrder.add("consumer 2:" + value));

		signal.accept("test");

		assertEquals(Arrays.asList("listener 1", "consumer 1:test", "listener 2", "consumer 2:test"), executionOrder);
	}

	@Test
	void consumerNotificationOrder() {
		Signal<String> signal = Signal.signal();
		List<String> receivedValues = new ArrayList<>();

		signal.addConsumer(value -> receivedValues.add("1:" + value));
		signal.addConsumer(value -> receivedValues.add("2:" + value));
		signal.addConsumer(value -> receivedValues.add("3:" + value));

		signal.accept("test");

		assertEquals(Arrays.asList("1:test", "2:test", "3:test"), receivedValues);
	}

	@Test
	void preventDuplicates() {
		Signal<Integer> signal = Signal.signal();
		Runnable listener = () -> {};
		Consumer<Integer> consumer = integer -> {};
		assertTrue(signal.addListener(listener));
		assertFalse(signal.addListener(listener));
		assertFalse(signal.addWeakListener(listener));
		assertTrue(signal.addConsumer(consumer));
		assertFalse(signal.addConsumer(consumer));
		assertFalse(signal.addWeakConsumer(consumer));

		assertTrue(signal.removeListener(listener));
		assertFalse(signal.removeListener(listener));
		assertTrue(signal.addWeakListener(listener));
		assertFalse(signal.addListener(listener));

		assertTrue(signal.removeConsumer(consumer));
		assertFalse(signal.removeConsumer(consumer));
		assertTrue(signal.addWeakConsumer(consumer));
		assertFalse(signal.addConsumer(consumer));
	}

	@Test
	void signalAsConsumer() {
		// DefaultObserver bug in 0.18.23
		Signal<Integer> signal = Signal.signal();
		Signal<Integer> consumer = Signal.signal();

		signal.addConsumer(consumer);

		consumer.addConsumer(Assertions::assertNotNull);

		signal.accept(1);
	}

	@Test
	void exceptionInConsumerPreventsSubsequentConsumers() {
		Signal<String> signal = Signal.signal();
		AtomicInteger counter = new AtomicInteger();

		signal.addConsumer(s -> counter.incrementAndGet());
		signal.addConsumer(s -> {
			throw new RuntimeException("Test exception");
		});
		signal.addConsumer(s -> counter.incrementAndGet()); // Should not execute

		assertThrows(RuntimeException.class, () -> signal.accept("test"));
		assertEquals(1, counter.get()); // Only first consumer executed
	}

	@Test
	void nullDataPropagation() {
		Signal<String> signal = Signal.signal();
		AtomicReference<String> receivedValue = new AtomicReference<>("initial");
		AtomicBoolean consumerCalled = new AtomicBoolean(false);

		signal.addConsumer(value -> {
			consumerCalled.set(true);
			receivedValue.set(value);
		});

		signal.accept(null);
		assertTrue(consumerCalled.get());
		assertNull(receivedValue.get());
	}

	@Test
	void observerView() {
		Signal<Integer> signal = Signal.signal();
		AtomicInteger listenerCounter = new AtomicInteger();
		AtomicReference<Integer> consumerValue = new AtomicReference<>();
		// the observer can not trigger the signal, listeners and consumers added to it are the signal's
		signal.observer().addListener(listenerCounter::incrementAndGet);
		signal.observer().addConsumer(consumerValue::set);
		// an observer is its own sensor
		signal.sensor().addListener(listenerCounter::incrementAndGet);
		signal.accept(1);
		assertEquals(2, listenerCounter.get());
		assertEquals(1, consumerValue.get());
	}

	@Test
	void concurrentSignalTrigger() throws InterruptedException {
		Signal<Integer> signal = Signal.signal();
		int threadCount = 10;
		int signalsPerThread = 1000;

		AtomicInteger listenerCounter = new AtomicInteger();
		AtomicInteger consumerSum = new AtomicInteger();

		signal.addListener(listenerCounter::incrementAndGet);
		signal.when(Objects::nonNull).addConsumer(consumerSum::addAndGet);

		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(threadCount);

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);

		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				try {
					startLatch.await();
					for (int j = 0; j < signalsPerThread; j++) {
						if (j % 2 == 0) {
							signal.accept(null);
						}
						else {
							signal.accept(1);
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

		int totalSignals = threadCount * signalsPerThread;
		assertEquals(totalSignals, listenerCounter.get());
		assertEquals(totalSignals / 2, consumerSum.get()); // Half the signals carried 1
	}

	@Test
	void recursiveSignalTrigger() {
		Signal<Integer> signal = Signal.signal();
		AtomicInteger depth = new AtomicInteger();
		AtomicInteger maxDepthReached = new AtomicInteger();

		signal.addConsumer(value -> {
			int currentDepth = depth.incrementAndGet();
			maxDepthReached.updateAndGet(max -> Math.max(max, currentDepth));

			if (currentDepth < 10) {
				signal.accept(value + 1); // Recursive call
			}

			depth.decrementAndGet();
		});

		signal.accept(1);

		assertEquals(10, maxDepthReached.get());
		assertEquals(0, depth.get()); // Should return to 0 after recursion completes
	}
}

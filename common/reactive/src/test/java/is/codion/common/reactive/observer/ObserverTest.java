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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.observer;

import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.common.reactive.value.Value.Notify;

import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public final class ObserverTest {

	@Test
	void observeValue() {
		AtomicInteger oneCounter = new AtomicInteger();
		AtomicInteger twoCounter = new AtomicInteger();
		AtomicInteger nullCounter = new AtomicInteger();
		AtomicReference<Integer> intValue = new AtomicReference<>();
		Value<Integer> value = Value.builder()
						.<Integer>nullable()
						.when(1, oneCounter::incrementAndGet)
						.when(1, intValue::set)
						.when(2, twoCounter::incrementAndGet)
						.when(Objects::isNull, nullCounter::incrementAndGet)
						.when(Objects::isNull, intValue::set)
						.build();

		value.set(1);
		assertEquals(1, intValue.get());
		value.set(2);
		value.clear();
		value.set(null);// no change, no event
		assertNull(intValue.get());
		value.set(1);

		assertEquals(1, intValue.get());
		assertEquals(2, oneCounter.get());
		assertEquals(1, twoCounter.get());
		assertEquals(1, nullCounter.get());

		oneCounter.set(0);
		value = Value.builder()
						.nonNull(0)
						.notify(Notify.SET)
						.build();
		value.when(1).addListener(oneCounter::incrementAndGet);
		value.set(1);
		value.set(1);
		assertEquals(2, oneCounter.get());
	}

	@Test
	void conditionalSubscribesWithItsListeners() {
		Event<Integer> event = Event.event();
		AtomicInteger counter = new AtomicInteger();
		Runnable listener = counter::incrementAndGet;

		//an unlistened conditional leaves nothing on the observer it filters
		Observer<Integer> conditional = event.when(1);
		event.accept(1);
		assertEquals(0, counter.get());

		conditional.addListener(listener);
		event.accept(1);
		event.accept(2);
		assertEquals(1, counter.get());

		//removing the last listener detaches it again
		assertTrue(conditional.removeListener(listener));
		event.accept(1);
		assertEquals(1, counter.get());

		//and adding one re-attaches, the same conditional serving again
		conditional.addListener(listener);
		event.accept(1);
		assertEquals(2, counter.get());

		//a second listener does not attach twice, nor does removing one of two detach
		AtomicInteger second = new AtomicInteger();
		Runnable secondListener = second::incrementAndGet;
		conditional.addListener(secondListener);
		event.accept(1);
		assertEquals(3, counter.get());
		assertEquals(1, second.get());
		conditional.removeListener(secondListener);
		event.accept(1);
		assertEquals(4, counter.get());
	}

	@Test
	void conditionalChainsAttachTransitively() {
		Event<Integer> event = Event.event();
		Observer<Integer> even = event.when(value -> value != null && value % 2 == 0);
		Observer<Integer> evenTens = even.when(value -> value != null && value % 10 == 0);
		AtomicInteger counter = new AtomicInteger();
		Runnable listener = counter::incrementAndGet;

		//nothing listened to, nothing attached at any level
		event.accept(20);
		assertEquals(0, counter.get());

		//one listener at the end of the chain attaches every level, source-ward
		evenTens.addListener(listener);
		event.accept(20);
		assertEquals(1, counter.get());
		event.accept(4);//even, not tens
		event.accept(5);//neither
		assertEquals(1, counter.get());

		//and removing it detaches every level again
		evenTens.removeListener(listener);
		event.accept(20);
		assertEquals(1, counter.get());
	}

	@Test
	void listenerHooksFireOnTheEdgesOnly() {
		//Conditional, the one implementation, attaches idempotently, so a hook firing on every add would
		//go unnoticed through it - the edge semantics are asserted here against the contract itself
		Counting observer = new Counting();
		Runnable one = () -> {};
		Runnable two = () -> {};

		observer.addListener(one);
		assertEquals(1, observer.first);
		observer.addListener(two);
		assertEquals(1, observer.first);

		observer.removeListener(one);
		assertEquals(0, observer.last);
		observer.removeListener(two);
		assertEquals(1, observer.last);

		//and again, from empty
		observer.addListener(one);
		assertEquals(2, observer.first);
		observer.removeListener(one);
		assertEquals(2, observer.last);
	}

	@Test
	void lastListenerFiresWhenWeakListenersAreCollected() {
		//emptied by pruning rather than by the removal, the documented removeWeak*(no-op) cleanup idiom
		Counting observer = new Counting();
		AtomicInteger counter = new AtomicInteger();
		Runnable listener = counter::incrementAndGet;
		observer.addWeakListener(listener);
		assertEquals(1, observer.first);

		WeakReference<Runnable> reference = new WeakReference<>(listener);
		listener = null;
		for (int i = 0; i < 20 && reference.get() != null; i++) {
			System.gc();
		}
		assertNull(reference.get());

		observer.removeWeakListener(counter::incrementAndGet);
		assertEquals(1, observer.last);
	}

	private static final class Counting extends AbstractObserver<String> {

		private int first;
		private int last;

		@Override
		void onFirstListener() {
			first++;
		}

		@Override
		void onLastListener() {
			last++;
		}
	}

	@Test
	void conditionalIsCollectableOnceUnlistened() {
		Event<Integer> event = Event.event();
		Runnable listener = () -> {};
		Observer<Integer> conditional = event.when(1);
		conditional.addListener(listener);
		WeakReference<Observer<Integer>> reference = new WeakReference<>(conditional);

		//while listened to, the event holds it through the consumer it subscribed with
		conditional.removeListener(listener);
		conditional = null;
		for (int i = 0; i < 20 && reference.get() != null; i++) {
			System.gc();
		}
		assertNull(reference.get());
	}

	@Test
	void conditionalArray() {
		AtomicInteger counter = new AtomicInteger();
		int[] one = new int[] {1};
		Value<int[]> value = Value.builder()
						.nonNull(new int[0])
						.when(one, counter::incrementAndGet)
						.build();
		value.set(new int[] {1});
		assertEquals(1, counter.get());
	}

	@Test
	void observeState() {
		AtomicInteger trueCounter = new AtomicInteger();
		AtomicInteger falseCounter = new AtomicInteger();

		State state = State.builder()
						.when(true, trueCounter::incrementAndGet)
						.when(false, falseCounter::incrementAndGet)
						.build();

		state.set(true);
		state.set(true);
		state.set(false);

		assertEquals(1, trueCounter.get());
		assertEquals(1, falseCounter.get());

		state = State.builder()
						.notify(Notify.SET)
						.build();

		trueCounter.set(0);
		falseCounter.set(0);
		state.when(true)
						.addListener(trueCounter::incrementAndGet);
		state.when(false)
						.addListener(falseCounter::incrementAndGet);

		state.set(true);
		state.set(true);
		state.set(false);
		state.set(false);

		assertEquals(2, trueCounter.get());
		assertEquals(2, falseCounter.get());
	}

	@Test
	void changeArrayValueEqualsAndHashCode() {
		//equal array-valued changes (deepEquals) must hash equally
		Change<byte[]> a = Change.change(new byte[] {1, 2, 3}, null);
		Change<byte[]> b = Change.change(new byte[] {1, 2, 3}, null);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		Change<int[]> c = Change.change(new int[] {4}, new int[] {5, 6});
		Change<int[]> d = Change.change(new int[] {4}, new int[] {5, 6});
		assertEquals(c, d);
		assertEquals(c.hashCode(), d.hashCode());
	}
}

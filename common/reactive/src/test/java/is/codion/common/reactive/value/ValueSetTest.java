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
 * Copyright (c) 2013 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.value;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static org.junit.jupiter.api.Assertions.*;

public class ValueSetTest {

	@Test
	void valueSet() {
		ValueSet<Integer> valueSet = ValueSet.valueSet();
		ObservableValueSet<Integer> observer = valueSet.observable();
		assertTrue(observer.isEmpty());
		assertUnmodifiable(observer);

		assertFalse(observer.isNullable());
		assertFalse(observer.isNull());
		assertFalse(observer.optional().isPresent());

		assertTrue(valueSet.add(1));
		assertTrue(observer.optional().isPresent());
		assertFalse(valueSet.add(1));
		assertTrue(valueSet.remove(1));
		assertFalse(valueSet.remove(1));

		Set<Integer> initialValues = new HashSet<>();
		initialValues.add(1);
		initialValues.add(2);

		valueSet = ValueSet.valueSet(initialValues);
		observer = valueSet.observable();
		assertFalse(observer.isEmpty());
		assertEquals(initialValues, observer.get());
		assertUnmodifiable(observer);
		assertTrue(observer.is(initialValues));

		assertFalse(valueSet.add(1));
		assertFalse(valueSet.add(2));
		assertTrue(valueSet.add(3));
		assertTrue(valueSet.remove(1));
		assertTrue(valueSet.remove(2));

		valueSet.set(initialValues);
		assertTrue(valueSet.remove(1));
		assertTrue(valueSet.remove(2));
		assertTrue(valueSet.add(3));

		valueSet.clear();
		assertTrue(observer.isEmpty());
		assertTrue(valueSet.add(3));
		assertFalse(valueSet.removeAll(1, 2));

		assertTrue(valueSet.add(null));
		assertFalse(valueSet.add(null));
		assertTrue(valueSet.remove(null));

		valueSet.clear();
		valueSet.addAll(1, 2);
		assertUnmodifiable(observer);

		valueSet.clear();
		assertTrue(valueSet.add(1));
		assertTrue(valueSet.add(2));

		valueSet.clear();

		Value<Integer> value = valueSet.value();

		value.set(1);
		assertTrue(observer.contains(1));
		value.clear();
		assertTrue(observer.isEmpty());

		valueSet.set(Collections.singleton(2));
		assertEquals(2, value.get());

		valueSet.clear();
		assertNull(value.get());

		assertTrue(valueSet.addAll(1, 2, 3));
		assertEquals(3, observer.size());
		valueSet.forEach(i -> {});
		assertFalse(observer.containsAll(asList(1, 2, 4)));
		assertTrue(observer.containsAll(asList(1, 2, 3)));
		assertFalse(valueSet.addAll(1, 2, 3));
		assertTrue(valueSet.removeAll(1, 2));
		assertFalse(valueSet.removeAll(1, 2));
		assertTrue(valueSet.removeAll(2, 3));
		assertUnmodifiable(observer);
	}

	@Test
	void valueSetEvents() {
		ValueSet<Integer> valueSet = ValueSet.valueSet();
		Value<Integer> value = valueSet.value();

		AtomicInteger valueEventCounter = new AtomicInteger();
		Consumer<Integer> consumer = integer -> valueEventCounter.incrementAndGet();
		value.addConsumer(consumer);

		AtomicInteger valueSetEventCounter = new AtomicInteger();
		Consumer<Set<Integer>> setConsumer = integers -> valueSetEventCounter.incrementAndGet();
		valueSet.addConsumer(setConsumer);

		valueSet.add(1);

		assertEquals(1, valueEventCounter.get());
		assertEquals(1, valueSetEventCounter.get());

		value.set(2);

		assertEquals(2, valueEventCounter.get());
		assertEquals(2, valueSetEventCounter.get());

		valueSet.clear();

		assertEquals(3, valueEventCounter.get());
		assertEquals(3, valueSetEventCounter.get());
	}

	@Test
	void sort() {
		AtomicInteger counter = new AtomicInteger();
		Runnable listener = counter::incrementAndGet;
		ValueSet<Integer> set = ValueSet.<Integer>builder()
						.listener(listener)
						.build();
		set.addAll(1, 2, 3, 4);
		assertEquals(1, counter.get());
		Comparator<Integer> comparator = comparing(Integer::intValue);

		// Already in this order, nothing moved, nothing to announce
		set.sort(comparator);
		assertEquals(1, counter.get());

		// A reorder is invisible to Set equality, so set() does not notify and the sort does
		set.sort(comparator.reversed());
		assertEquals(asList(4, 3, 2, 1), new ArrayList<>(set.get()));
		assertEquals(2, counter.get());
		set.sort(comparator);
		assertEquals(asList(1, 2, 3, 4), new ArrayList<>(set.get()));
		assertEquals(3, counter.get());

		// and an empty set has no order to change
		set.clear();
		assertEquals(4, counter.get());
		set.sort(comparator);
		assertEquals(4, counter.get());
	}

	@Test
	void sortNotifyingOnSet() {
		// set() notifies on every set, the sort then announcing nothing of its own
		AtomicInteger counter = new AtomicInteger();
		ValueSet<Integer> set = ValueSet.<Integer>builder()
						.value(new LinkedHashSet<>(asList(2, 1)))
						.notify(Value.Notify.SET)
						.listener(counter::incrementAndGet)
						.build();
		set.sort(comparing(Integer::intValue));
		assertEquals(asList(1, 2), new ArrayList<>(set.get()));
		assertEquals(1, counter.get());
		set.sort(comparing(Integer::intValue));
		assertEquals(2, counter.get());
	}

	@Test
	void sortLocked() {
		// a reorder is no change by set equality, which the lock lets through, but it is one, as for a list
		ValueSet<Integer> set = ValueSet.valueSet(asList(2, 1));
		set.locked().set(true);
		assertThrows(IllegalStateException.class, () -> set.sort(comparing(Integer::intValue)));
		assertEquals(asList(2, 1), new ArrayList<>(set.get()));
		// the order unchanged
		assertDoesNotThrow(() -> set.sort(comparing(Integer::intValue).reversed()));
	}

	@Test
	void abstractValueSet() {
		Numbers numbers = new Numbers(Value.Notify.CHANGED);
		ObservableValueSet<Integer> observable = numbers.observable();
		assertTrue(observable.isEmpty());
		assertFalse(observable.isNullable());
		AtomicInteger notified = new AtomicInteger();
		numbers.addListener(notified::incrementAndGet);
		//an operation of its own, on the inherited mutators
		numbers.addRange(1, 3);
		assertEquals(new LinkedHashSet<>(asList(1, 2, 3)), numbers.get());
		assertEquals(3, notified.get());
		assertUnmodifiable(observable);
		//notifying on a change only
		numbers.set(new LinkedHashSet<>(asList(1, 2, 3)));
		assertEquals(3, notified.get());
		//the validator added in the constructor
		assertThrows(IllegalArgumentException.class, () -> numbers.add(-1));
		assertEquals(3, numbers.size());
		//the set overload takes a snapshot
		Set<Integer> values = new LinkedHashSet<>(asList(4));
		numbers.set(values);
		values.add(5);
		assertEquals(Collections.singleton(4), numbers.get());
		//locked
		numbers.locked().set(true);
		assertThrows(IllegalStateException.class, () -> numbers.add(6));
		numbers.locked().set(false);
		//sorted
		numbers.set(asList(2, 1));
		numbers.sort(Comparator.naturalOrder());
		assertEquals(asList(1, 2), new ArrayList<>(numbers.get()));
		//linked
		ValueSet<Integer> linked = ValueSet.valueSet();
		numbers.link(linked);
		linked.add(7);
		assertTrue(numbers.contains(7));
		//the first item
		assertEquals(7, numbers.value().get());
	}

	@Test
	void abstractValueSetNotifyingOnSet() {
		AtomicInteger notified = new AtomicInteger();
		Numbers numbers = new Numbers(Value.Notify.SET);
		numbers.addListener(notified::incrementAndGet);
		numbers.set(Collections.emptySet());
		assertEquals(1, notified.get());
	}

	private static final class Numbers extends AbstractValueSet<Integer> {

		private Numbers(Notify notify) {
			super(notify);
			addValidator(values -> values.forEach(value -> {
				if (value < 0) {
					throw new IllegalArgumentException("Negative: " + value);
				}
			}));
		}

		private void addRange(int from, int to) {
			for (int i = from; i <= to; i++) {
				add(i);
			}
		}
	}

	private static void assertUnmodifiable(ObservableValueCollection<Integer, Set<Integer>> observer) {
		assertThrows(UnsupportedOperationException.class, () -> observer.get().remove(1));
	}
}

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

		set.sort(comparator);
		// No change events when sorting
		assertEquals(1, counter.get());
		set.sort(comparator.reversed());
		assertEquals(asList(4, 3, 2, 1), new ArrayList<>(set.get()));
		assertEquals(1, counter.get());
		set.sort(comparator);
		assertEquals(asList(1, 2, 3, 4), new ArrayList<>(set.get()));
		assertEquals(1, counter.get());
	}

	private static void assertUnmodifiable(ObservableValueCollection<Integer, Set<Integer>> observer) {
		assertThrows(UnsupportedOperationException.class, () -> observer.get().remove(1));
	}
}

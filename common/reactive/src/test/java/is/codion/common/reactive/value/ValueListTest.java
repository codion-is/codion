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
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.value;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static org.junit.jupiter.api.Assertions.*;

public class ValueListTest {

	@Test
	void list() {
		ValueList<Integer> values = ValueList.valueList();
		ObservableValueCollection<Integer, List<Integer>> observer = values.observable();
		assertTrue(observer.isEmpty());
		assertUnmodifiable(observer);

		assertFalse(observer.isNullable());
		assertFalse(observer.isNull());
		assertFalse(observer.optional().isPresent());

		assertTrue(values.add(1));
		assertTrue(observer.optional().isPresent());
		assertTrue(values.add(1));
		assertTrue(values.remove(1));
		assertTrue(values.remove(1));

		List<Integer> initialValues = new ArrayList<>();
		initialValues.add(1);
		initialValues.add(2);

		values = ValueList.valueList(initialValues);
		observer = values.observable();
		assertFalse(observer.isEmpty());
		assertEquals(initialValues, observer.get());
		assertUnmodifiable(observer);
		assertTrue(observer.is(initialValues));

		assertTrue(values.add(1));
		assertTrue(values.add(2));
		assertTrue(values.add(3));
		assertTrue(values.remove(1));
		assertTrue(values.remove(2));

		values.set(initialValues);
		assertTrue(values.remove(1));
		assertTrue(values.remove(2));
		assertTrue(values.add(3));

		values.clear();
		assertTrue(observer.isEmpty());
		assertTrue(values.add(3));
		assertFalse(values.removeAll(1, 2));

		assertTrue(values.add(null));
		assertTrue(values.add(null));
		assertTrue(values.remove(null));

		values.clear();
		values.addAll(1, 2);
		assertUnmodifiable(observer);

		values.clear();
		assertTrue(values.add(1));
		assertTrue(values.add(2));

		values.clear();

		Value<Integer> value = values.value();

		value.set(1);
		assertTrue(observer.contains(1));
		value.clear();
		assertTrue(observer.isEmpty());

		values.set(Collections.singleton(2));
		assertEquals(2, value.get());

		values.clear();
		assertNull(value.get());

		assertTrue(values.addAll(1, 2, 3));
		assertEquals(3, observer.size());
		values.forEach(i -> {});
		assertFalse(observer.containsAll(asList(1, 2, 4)));
		assertTrue(observer.containsAll(asList(1, 2, 3)));
		assertTrue(values.addAll(1, 2, 3));
		assertTrue(values.removeAll(1, 2));
		assertFalse(values.removeAll(1, 2));
		assertTrue(values.removeAll(2, 3));
		assertUnmodifiable(observer);
	}

	@Test
	void listEvents() {
		ValueList<Integer> valueList = ValueList.valueList();
		Value<Integer> singleValue = valueList.value();

		AtomicInteger valueEventCounter = new AtomicInteger();
		Runnable listener = valueEventCounter::incrementAndGet;
		singleValue.addListener(listener);

		AtomicInteger valueListEventCounter = new AtomicInteger();
		Runnable setListener = valueListEventCounter::incrementAndGet;
		valueList.addListener(setListener);

		valueList.add(1);

		assertEquals(1, valueEventCounter.get());
		assertEquals(1, valueListEventCounter.get());

		singleValue.set(2);

		assertEquals(2, valueEventCounter.get());
		assertEquals(2, valueListEventCounter.get());

		valueList.clear();

		assertEquals(3, valueEventCounter.get());
		assertEquals(3, valueListEventCounter.get());
	}

	@Test
	void sort() {
		AtomicInteger counter = new AtomicInteger();
		Runnable listener = counter::incrementAndGet;
		ValueList<Integer> list = ValueList.<Integer>builder()
						.listener(listener)
						.build();
		list.addAll(1, 2, 3, 4);
		assertEquals(1, counter.get());
		Comparator<Integer> comparator = comparing(Integer::intValue);

		list.sort(comparator);
		// No change, already sorted
		assertEquals(1, counter.get());
		list.sort(comparator.reversed());
		assertEquals(2, counter.get());
		list.sort(comparator);
		assertEquals(3, counter.get());
		list.clear();
		assertEquals(4, counter.get());
		list.sort(comparator);
		assertEquals(4, counter.get());
	}

	private static void assertUnmodifiable(ObservableValueCollection<Integer, ? extends Collection<Integer>> observer) {
		assertThrows(UnsupportedOperationException.class, () -> observer.get().remove(1));
	}
}

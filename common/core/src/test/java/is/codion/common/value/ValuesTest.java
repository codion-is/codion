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
 * Copyright (c) 2013 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.value;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class ValuesTest {

	@Test
	void set() {
		Values<Integer, Set<Integer>> set = ValueSet.valueSet();
		ObservableValues<Integer, Set<Integer>> observer = set.observable();
		assertTrue(observer.empty());
		assertFalse(observer.notEmpty());
		assertUnmodifiable(observer);

		assertFalse(observer.isNullable());
		assertFalse(observer.isNull());
		assertTrue(observer.optional().isPresent());

		assertTrue(set.add(1));
		assertFalse(set.add(1));
		assertTrue(set.remove(1));
		assertFalse(set.remove(1));

		Set<Integer> initialValues = new HashSet<>();
		initialValues.add(1);
		initialValues.add(2);

		set = ValueSet.<Integer>builder()
						.value(initialValues)
						.build();
		observer = set.observable();
		assertFalse(observer.empty());
		assertTrue(observer.notEmpty());
		assertEquals(initialValues, observer.get());
		assertUnmodifiable(observer);
		assertTrue(observer.isEqualTo(initialValues));

		assertFalse(set.add(1));
		assertFalse(set.add(2));
		assertTrue(set.add(3));
		assertTrue(set.remove(1));
		assertTrue(set.remove(2));

		set.set(initialValues);
		assertTrue(set.remove(1));
		assertTrue(set.remove(2));
		assertTrue(set.add(3));

		set.clear();
		assertTrue(observer.empty());
		assertFalse(observer.notEmpty());
		assertTrue(set.add(3));
		assertFalse(set.removeAll(1, 2));

		assertTrue(set.add(null));
		assertFalse(set.add(null));
		assertTrue(set.remove(null));

		set.clear();
		set.addAll(1, 2);
		assertUnmodifiable(observer);

		set.clear();
		assertTrue(set.add(1));
		assertTrue(set.add(2));

		set.clear();

		Value<Integer> value = set.value();

		value.set(1);
		assertTrue(observer.contains(1));
		value.clear();
		assertTrue(observer.empty());

		set.set(Collections.singleton(2));
		assertEquals(2, value.get());

		set.clear();
		assertNull(value.get());

		assertTrue(set.addAll(1, 2, 3));
		assertEquals(3, observer.size());
		set.forEach(i -> {});
		assertFalse(observer.containsAll(asList(1, 2, 4)));
		assertTrue(observer.containsAll(asList(1, 2, 3)));
		assertFalse(set.addAll(1, 2, 3));
		assertTrue(set.removeAll(1, 2));
		assertFalse(set.removeAll(1, 2));
		assertTrue(set.removeAll(2, 3));
		assertUnmodifiable(observer);

		set.clear();
		set.addAll(1, 2);
		assertFalse(set.set(asList(1, 2)));
	}

	@Test
	void setEvents() {
		Values<Integer, Set<Integer>> set = ValueSet.valueSet();
		Value<Integer> singleValue = set.value();

		AtomicInteger valueEventCounter = new AtomicInteger();
		Consumer<Integer> consumer = integer -> valueEventCounter.incrementAndGet();
		singleValue.addConsumer(consumer);

		AtomicInteger valueSetEventCounter = new AtomicInteger();
		Consumer<Set<Integer>> setConsumer = integers -> valueSetEventCounter.incrementAndGet();
		set.addConsumer(setConsumer);

		set.add(1);

		assertEquals(1, valueEventCounter.get());
		assertEquals(1, valueSetEventCounter.get());

		singleValue.set(2);

		assertEquals(2, valueEventCounter.get());
		assertEquals(2, valueSetEventCounter.get());

		set.clear();

		assertEquals(3, valueEventCounter.get());
		assertEquals(3, valueSetEventCounter.get());
	}

	@Test
	void list() {
		Values<Integer, List<Integer>> list = ValueList.valueList();
		ObservableValues<Integer, List<Integer>> observable = list.observable();
		assertTrue(observable.empty());
		assertFalse(observable.notEmpty());
		assertUnmodifiable(observable);

		assertFalse(observable.isNullable());
		assertFalse(observable.isNull());
		assertTrue(observable.optional().isPresent());

		assertTrue(list.add(1));
		assertTrue(list.add(1));
		assertTrue(list.remove(1));
		assertTrue(list.remove(1));

		List<Integer> initialValues = new ArrayList<>();
		initialValues.add(1);
		initialValues.add(2);

		list = ValueList.<Integer>builder()
						.value(initialValues)
						.build();
		observable = list.observable();
		assertFalse(observable.empty());
		assertTrue(observable.notEmpty());
		assertEquals(initialValues, observable.get());
		assertUnmodifiable(observable);
		assertTrue(observable.isEqualTo(initialValues));

		assertTrue(list.add(1));
		assertTrue(list.add(2));
		assertTrue(list.add(3));
		assertTrue(list.remove(1));
		assertTrue(list.remove(2));

		list.set(initialValues);
		assertTrue(list.remove(1));
		assertTrue(list.remove(2));
		assertTrue(list.add(3));

		list.clear();
		assertTrue(observable.empty());
		assertFalse(observable.notEmpty());
		assertTrue(list.add(3));
		assertFalse(list.removeAll(1, 2));

		assertTrue(list.add(null));
		assertTrue(list.add(null));
		assertTrue(list.remove(null));

		list.clear();
		list.addAll(1, 2);
		assertUnmodifiable(observable);

		list.clear();
		assertTrue(list.add(1));
		assertTrue(list.add(2));

		list.clear();

		Value<Integer> value = list.value();

		value.set(1);
		assertTrue(observable.contains(1));
		value.clear();
		assertTrue(observable.empty());

		list.set(Collections.singleton(2));
		assertEquals(2, value.get());

		list.clear();
		assertNull(value.get());

		assertTrue(list.addAll(1, 2, 3));
		assertEquals(3, observable.size());
		list.forEach(i -> {});
		assertFalse(observable.containsAll(asList(1, 2, 4)));
		assertTrue(observable.containsAll(asList(1, 2, 3)));
		assertTrue(list.addAll(1, 2, 3));
		assertTrue(list.removeAll(1, 2));
		assertFalse(list.removeAll(1, 2));
		assertTrue(list.removeAll(2, 3));
		assertUnmodifiable(observable);

		list.clear();
		list.addAll(1, 2);
		assertFalse(list.set(new LinkedHashSet<>(asList(1, 2))));
		assertTrue(list.set(new LinkedHashSet<>(asList(2, 1))));
	}

	@Test
	void listEvents() {
		Values<Integer, List<Integer>> list = ValueList.valueList();
		Value<Integer> singleValue = list.value();

		AtomicInteger valueEventCounter = new AtomicInteger();
		Consumer<Integer> consumer = integer -> valueEventCounter.incrementAndGet();
		singleValue.addConsumer(consumer);

		AtomicInteger valueListEventCounter = new AtomicInteger();
		Consumer<List<Integer>> setConsumer = integers -> valueListEventCounter.incrementAndGet();
		list.addConsumer(setConsumer);

		list.add(1);

		assertEquals(1, valueEventCounter.get());
		assertEquals(1, valueListEventCounter.get());

		singleValue.set(2);

		assertEquals(2, valueEventCounter.get());
		assertEquals(2, valueListEventCounter.get());

		list.clear();

		assertEquals(3, valueEventCounter.get());
		assertEquals(3, valueListEventCounter.get());
	}

	private static void assertUnmodifiable(ObservableValues<Integer, ? extends Collection<Integer>> observable) {
		assertThrows(UnsupportedOperationException.class, () -> observable.get().remove(1));
	}
}

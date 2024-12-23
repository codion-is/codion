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
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class ValuesTest {

	private final Supplier<Set<Integer>> emptySet = LinkedHashSet::new;
	private final Function<Set<Integer>, Set<Integer>> unmodifiableSet = Collections::unmodifiableSet;

	private final Supplier<List<Integer>> emptyList = ArrayList::new;
	private final Function<List<Integer>, List<Integer>> unmodifiableList = Collections::unmodifiableList;

	@Test
	void set() {
		Values<Integer, Set<Integer>> values = Values.builder(emptySet, unmodifiableSet).build();
		ObservableValues<Integer, Set<Integer>> observer = values.observable();
		assertTrue(observer.empty());
		assertFalse(observer.notEmpty());
		assertUnmodifiable(observer);

		assertFalse(observer.isNullable());
		assertFalse(observer.isNull());
		assertTrue(observer.optional().isPresent());

		assertTrue(values.add(1));
		assertFalse(values.add(1));
		assertTrue(values.remove(1));
		assertFalse(values.remove(1));

		Set<Integer> initialValues = new HashSet<>();
		initialValues.add(1);
		initialValues.add(2);

		values = Values.builder(emptySet, unmodifiableSet)
						.value(initialValues)
						.build();
		observer = values.observable();
		assertFalse(observer.empty());
		assertTrue(observer.notEmpty());
		assertEquals(initialValues, observer.get());
		assertUnmodifiable(observer);
		assertTrue(observer.isEqualTo(initialValues));

		assertFalse(values.add(1));
		assertFalse(values.add(2));
		assertTrue(values.add(3));
		assertTrue(values.remove(1));
		assertTrue(values.remove(2));

		values.set(initialValues);
		assertTrue(values.remove(1));
		assertTrue(values.remove(2));
		assertTrue(values.add(3));

		values.clear();
		assertTrue(observer.empty());
		assertFalse(observer.notEmpty());
		assertTrue(values.add(3));
		assertFalse(values.removeAll(1, 2));

		assertTrue(values.add(null));
		assertFalse(values.add(null));
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
		assertTrue(observer.empty());

		values.set(Collections.singleton(2));
		assertEquals(2, value.get());

		values.clear();
		assertNull(value.get());

		assertTrue(values.addAll(1, 2, 3));
		assertEquals(3, observer.size());
		values.forEach(i -> {});
		assertFalse(observer.containsAll(asList(1, 2, 4)));
		assertTrue(observer.containsAll(asList(1, 2, 3)));
		assertFalse(values.addAll(1, 2, 3));
		assertTrue(values.removeAll(1, 2));
		assertFalse(values.removeAll(1, 2));
		assertTrue(values.removeAll(2, 3));
		assertUnmodifiable(observer);

		values.clear();
		values.addAll(1, 2);
		assertFalse(values.set(asList(1, 2)));
	}

	@Test
	void setEvents() {
		Values<Integer, Set<Integer>> valueSet = Values.builder(emptySet, unmodifiableSet).build();
		Value<Integer> singleValue = valueSet.value();

		AtomicInteger valueEventCounter = new AtomicInteger();
		Consumer<Integer> consumer = integer -> valueEventCounter.incrementAndGet();
		singleValue.addConsumer(consumer);

		AtomicInteger valueSetEventCounter = new AtomicInteger();
		Consumer<Set<Integer>> setConsumer = integers -> valueSetEventCounter.incrementAndGet();
		valueSet.addConsumer(setConsumer);

		valueSet.add(1);

		assertEquals(1, valueEventCounter.get());
		assertEquals(1, valueSetEventCounter.get());

		singleValue.set(2);

		assertEquals(2, valueEventCounter.get());
		assertEquals(2, valueSetEventCounter.get());

		valueSet.clear();

		assertEquals(3, valueEventCounter.get());
		assertEquals(3, valueSetEventCounter.get());
	}

	@Test
	void list() {
		Values<Integer, List<Integer>> values = Values.builder(emptyList, unmodifiableList).build();
		ObservableValues<Integer, List<Integer>> observable = values.observable();
		assertTrue(observable.empty());
		assertFalse(observable.notEmpty());
		assertUnmodifiable(observable);

		assertFalse(observable.isNullable());
		assertFalse(observable.isNull());
		assertTrue(observable.optional().isPresent());

		assertTrue(values.add(1));
		assertTrue(values.add(1));
		assertTrue(values.remove(1));
		assertTrue(values.remove(1));

		List<Integer> initialValues = new ArrayList<>();
		initialValues.add(1);
		initialValues.add(2);

		values = Values.builder(emptyList, unmodifiableList)
						.value(initialValues)
						.build();
		observable = values.observable();
		assertFalse(observable.empty());
		assertTrue(observable.notEmpty());
		assertEquals(initialValues, observable.get());
		assertUnmodifiable(observable);
		assertTrue(observable.isEqualTo(initialValues));

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
		assertTrue(observable.empty());
		assertFalse(observable.notEmpty());
		assertTrue(values.add(3));
		assertFalse(values.removeAll(1, 2));

		assertTrue(values.add(null));
		assertTrue(values.add(null));
		assertTrue(values.remove(null));

		values.clear();
		values.addAll(1, 2);
		assertUnmodifiable(observable);

		values.clear();
		assertTrue(values.add(1));
		assertTrue(values.add(2));

		values.clear();

		Value<Integer> value = values.value();

		value.set(1);
		assertTrue(observable.contains(1));
		value.clear();
		assertTrue(observable.empty());

		values.set(Collections.singleton(2));
		assertEquals(2, value.get());

		values.clear();
		assertNull(value.get());

		assertTrue(values.addAll(1, 2, 3));
		assertEquals(3, observable.size());
		values.forEach(i -> {});
		assertFalse(observable.containsAll(asList(1, 2, 4)));
		assertTrue(observable.containsAll(asList(1, 2, 3)));
		assertTrue(values.addAll(1, 2, 3));
		assertTrue(values.removeAll(1, 2));
		assertFalse(values.removeAll(1, 2));
		assertTrue(values.removeAll(2, 3));
		assertUnmodifiable(observable);

		values.clear();
		values.addAll(1, 2);
		assertFalse(values.set(new LinkedHashSet<>(asList(1, 2))));
		assertTrue(values.set(new LinkedHashSet<>(asList(2, 1))));
	}

	@Test
	void listEvents() {
		Values<Integer, List<Integer>> valueList = Values.builder(emptyList, unmodifiableList).build();
		Value<Integer> singleValue = valueList.value();

		AtomicInteger valueEventCounter = new AtomicInteger();
		Consumer<Integer> consumer = integer -> valueEventCounter.incrementAndGet();
		singleValue.addConsumer(consumer);

		AtomicInteger valueListEventCounter = new AtomicInteger();
		Consumer<List<Integer>> setConsumer = integers -> valueListEventCounter.incrementAndGet();
		valueList.addConsumer(setConsumer);

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

	private static void assertUnmodifiable(ObservableValues<Integer, ? extends Collection<Integer>> observable) {
		assertThrows(UnsupportedOperationException.class, () -> observable.get().remove(1));
	}
}

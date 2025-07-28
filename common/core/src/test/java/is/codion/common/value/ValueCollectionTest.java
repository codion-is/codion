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
 * Copyright (c) 2013 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.value;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class ValueCollectionTest {

	@Test
	@DisplayName("ValueSet basic operations")
	void valueSet_basicOperations_shouldWorkAsExpected() {
		ValueCollection<Integer, Set<Integer>> set = ValueSet.valueSet();
		ObservableValueCollection<Integer, Set<Integer>> observer = set.observable();
		assertTrue(observer.isEmpty());
		assertUnmodifiable(observer);

		assertFalse(observer.isNullable());
		assertFalse(observer.isNull());
		assertTrue(observer.optional().isPresent());

		assertTrue(set.add(1));
		assertFalse(set.add(1));
		assertTrue(set.remove(1));
		assertFalse(set.remove(1));

	}

	@Test
	@DisplayName("ValueSet with initial values")
	void valueSet_withInitialValues_shouldContainValues() {
		Set<Integer> initialValues = new HashSet<>();
		initialValues.add(1);
		initialValues.add(2);

		ValueCollection<Integer, Set<Integer>> set = ValueSet.<Integer>builder()
						.value(initialValues)
						.build();
		ObservableValueCollection<Integer, Set<Integer>> observer = set.observable();
		assertFalse(observer.isEmpty());
		assertEquals(initialValues, observer.get());
		assertUnmodifiable(observer);
		assertTrue(observer.isEqualTo(initialValues));

		assertFalse(set.add(1));
		assertFalse(set.add(2));
		assertTrue(set.add(3));
		assertTrue(set.remove(1));
		assertTrue(set.remove(2));
	}

	@Test
	@DisplayName("ValueSet null handling")
	void valueSet_nullHandling_shouldAllowNullValues() {
		ValueCollection<Integer, Set<Integer>> set = ValueSet.valueSet();
		assertTrue(set.add(null));
		assertFalse(set.add(null));
		assertTrue(set.remove(null));
	}

	@Test
	@DisplayName("ValueSet single value view")
	void valueSet_singleValueView_shouldReflectChanges() {
		ValueCollection<Integer, Set<Integer>> set = ValueSet.valueSet();
		ObservableValueCollection<Integer, Set<Integer>> observer = set.observable();
		Value<Integer> value = set.value();

		value.set(1);
		assertTrue(observer.contains(1));
		value.clear();
		assertTrue(observer.isEmpty());

		set.set(Collections.singleton(2));
		assertEquals(2, value.get());

		set.clear();
		assertNull(value.get());
	}

	@Test
	@DisplayName("ValueSet bulk operations")
	void valueSet_bulkOperations_shouldWorkCorrectly() {
		ValueCollection<Integer, Set<Integer>> set = ValueSet.valueSet();
		ObservableValueCollection<Integer, Set<Integer>> observer = set.observable();

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
	}

	@Test
	void setEvents() {
		ValueCollection<Integer, Set<Integer>> set = ValueSet.valueSet();
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
	@DisplayName("ValueList basic operations")
	void valueList_basicOperations_shouldWorkAsExpected() {
		ValueCollection<Integer, List<Integer>> list = ValueList.valueList();
		ObservableValueCollection<Integer, List<Integer>> observable = list.observable();
		assertTrue(observable.isEmpty());
		assertUnmodifiable(observable);

		assertFalse(observable.isNullable());
		assertFalse(observable.isNull());
		assertTrue(observable.optional().isPresent());

		assertTrue(list.add(1));
		assertTrue(list.add(1));
		assertTrue(list.remove(1));
		assertTrue(list.remove(1));

	}

	@Test
	@DisplayName("ValueList with initial values")
	void valueList_withInitialValues_shouldContainValues() {
		List<Integer> initialValues = new ArrayList<>();
		initialValues.add(1);
		initialValues.add(2);

		ValueCollection<Integer, List<Integer>> list = ValueList.<Integer>builder()
						.value(initialValues)
						.build();
		ObservableValueCollection<Integer, List<Integer>> observable = list.observable();
		assertFalse(observable.isEmpty());
		assertEquals(initialValues, observable.get());
		assertUnmodifiable(observable);
		assertTrue(observable.isEqualTo(initialValues));

		assertTrue(list.add(1));
		assertTrue(list.add(2));
		assertTrue(list.add(3));
		assertTrue(list.remove(1));
		assertTrue(list.remove(2));
	}

	@Test
	@DisplayName("ValueList duplicate and null handling")
	void valueList_duplicatesAndNull_shouldAllowBoth() {
		ValueCollection<Integer, List<Integer>> list = ValueList.valueList();
		// Lists allow duplicates
		assertTrue(list.add(1));
		assertTrue(list.add(1));
		assertTrue(list.remove(1));
		assertTrue(list.remove(1));

		// Lists allow null values
		assertTrue(list.add(null));
		assertTrue(list.add(null));
		assertTrue(list.remove(null));
	}

	@Test
	@DisplayName("ValueList single value view")
	void valueList_singleValueView_shouldReflectChanges() {
		ValueCollection<Integer, List<Integer>> list = ValueList.valueList();
		ObservableValueCollection<Integer, List<Integer>> observable = list.observable();
		Value<Integer> value = list.value();

		value.set(1);
		assertTrue(observable.contains(1));
		value.clear();
		assertTrue(observable.isEmpty());

		list.set(Collections.singleton(2));
		assertEquals(2, value.get());

		list.clear();
		assertNull(value.get());
	}

	@Test
	@DisplayName("ValueList bulk operations")
	void valueList_bulkOperations_shouldWorkCorrectly() {
		ValueCollection<Integer, List<Integer>> list = ValueList.valueList();
		ObservableValueCollection<Integer, List<Integer>> observable = list.observable();

		assertTrue(list.addAll(1, 2, 3));
		assertEquals(3, observable.size());
		list.forEach(i -> {});
		assertFalse(observable.containsAll(asList(1, 2, 4)));
		assertTrue(observable.containsAll(asList(1, 2, 3)));
		assertTrue(list.addAll(1, 2, 3)); // Lists allow duplicates
		assertTrue(list.removeAll(1, 2));
		assertFalse(list.removeAll(1, 2));
		assertTrue(list.removeAll(2, 3));
		assertUnmodifiable(observable);

		list.clear();
		list.addAll(1, 2);
	}

	@Test
	void listEvents() {
		ValueCollection<Integer, List<Integer>> list = ValueList.valueList();
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

	private static void assertUnmodifiable(ObservableValueCollection<Integer, ? extends Collection<Integer>> observable) {
		assertThrows(UnsupportedOperationException.class, () -> observable.get().remove(1));
	}
}

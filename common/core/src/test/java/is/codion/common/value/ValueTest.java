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

import is.codion.common.observable.Observable;
import is.codion.common.value.Value.Notify;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class ValueTest {

	@Test
	void test() {
		Value.builder()
						.nullable(1)
						.notify(Notify.WHEN_SET)
						.build();
		Value.builder()
						.nonNull("NullString")
						.value("Testing")
						.build();
		assertThrows(NullPointerException.class, () -> Value.builder().nonNull(null));
	}

	@Test
	void validator() {
		Value.Validator<Integer> validator = value -> {
			if (value != null && value > 10) {
				throw new IllegalArgumentException();
			}
		};
		Value<Integer> value = Value.builder().nonNull(0).build();
		value.set(11);
		assertThrows(IllegalArgumentException.class, () -> value.addValidator(validator));
		value.set(1);
		assertTrue(value.addValidator(validator));
		assertFalse(value.addValidator(validator));
		value.clear();
		assertEquals(0, value.get());
		assertThrows(IllegalArgumentException.class, () -> value.set(11));
		value.set(2);
		assertEquals(2, value.get());
		assertThrows(IllegalArgumentException.class, () -> value.set(12));
	}

	@Test
	void value() {
		AtomicInteger eventCounter = new AtomicInteger();
		Value<Integer> intValue = Value.builder()
						.nonNull(-1)
						.value(42)
						.build();
		assertFalse(intValue.nullable());
		assertTrue(intValue.optional().isPresent());
		assertTrue(intValue.isEqualTo(42));
		Observable<Integer> observable = intValue.observable();
		assertFalse(observable.nullable());
		assertTrue(observable.optional().isPresent());
		assertTrue(observable.isEqualTo(42));
		Runnable listener = eventCounter::incrementAndGet;
		assertTrue(observable.addListener(listener));
		assertFalse(observable.addListener(listener));
		observable.addConsumer(data -> {
			if (eventCounter.get() != 2) {
				assertNotNull(data);
			}
		});
		assertFalse(intValue.set(42));
		assertEquals(0, eventCounter.get());
		intValue.set(20);
		assertEquals(1, eventCounter.get());
		assertTrue(intValue.isEqualTo(20));
		intValue.clear();
		assertTrue(intValue.isEqualTo(-1));
		assertFalse(intValue.isNull());
		assertTrue(intValue.isNotNull());
		assertTrue(intValue.optional().isPresent());
		assertEquals(-1, intValue.get());
		assertEquals(-1, observable.get());
		assertEquals(2, eventCounter.get());
		intValue.clear();
		assertEquals(-1, intValue.get());
		assertEquals(-1, observable.get());
		assertEquals(2, eventCounter.get());
		intValue.set(42);
		assertEquals(3, eventCounter.get());
		intValue.clear();
		assertEquals(-1, intValue.get());
		assertEquals(-1, observable.get());

		Value<String> stringValue = Value.builder()
						.nonNull("null")
						.build();
		assertFalse(stringValue.nullable());
		assertEquals("null", stringValue.get());
		stringValue.set("test");
		assertEquals("test", stringValue.get());
		stringValue.clear();
		assertEquals("null", stringValue.get());

		Value<String> value = Value.value();
		assertFalse(value.optional().isPresent());
		assertThrows(NoSuchElementException.class, value::getOrThrow);
		value.set("hello");
		assertTrue(value.optional().isPresent());

		assertTrue(observable.removeListener(listener));
		assertFalse(observable.removeListener(listener));
	}

	@Test
	void linkValues() {
		AtomicInteger modelValueEventCounter = new AtomicInteger();
		Value<Integer> modelValue = Value.value(42);
		Value<Integer> uiValue = Value.value();
		uiValue.link(modelValue);

		assertThrows(IllegalStateException.class, () -> uiValue.link(modelValue));

		modelValue.addListener(modelValueEventCounter::incrementAndGet);
		AtomicInteger uiValueEventCounter = new AtomicInteger();
		uiValue.addListener(uiValueEventCounter::incrementAndGet);
		assertEquals(Integer.valueOf(42), uiValue.get());
		assertEquals(0, modelValueEventCounter.get());
		assertEquals(0, uiValueEventCounter.get());

		uiValue.set(20);
		assertEquals(Integer.valueOf(20), modelValue.get());
		assertEquals(1, modelValueEventCounter.get());
		assertEquals(1, uiValueEventCounter.get());

		modelValue.set(22);
		assertEquals(Integer.valueOf(22), uiValue.get());
		assertEquals(2, modelValueEventCounter.get());
		assertEquals(2, uiValueEventCounter.get());

		uiValue.set(22);
		assertEquals(2, modelValueEventCounter.get());
		assertEquals(2, uiValueEventCounter.get());

		uiValue.clear();
		assertNull(modelValue.get());
		assertTrue(modelValue.isNull());
		assertFalse(modelValue.isNotNull());
		assertTrue(modelValue.isEqualTo(null));
		assertNull(uiValue.get());
		assertTrue(uiValue.isNull());
		assertEquals(3, modelValueEventCounter.get());
		assertEquals(3, uiValueEventCounter.get());

		Value<Integer> valueOne = Value.value();
		assertThrows(IllegalArgumentException.class, () -> valueOne.link(valueOne));
	}

	@Test
	void linkValuesReadOnly() {
		AtomicInteger modelValueEventCounter = new AtomicInteger();
		Value<Integer> modelValue = Value.builder()
						.nonNull(0)
						.value(42)
						.build();
		Value<Integer> uiValue = Value.value();
		assertFalse(modelValue.nullable());
		uiValue.link(modelValue.observable());
		modelValue.addListener(modelValueEventCounter::incrementAndGet);
		AtomicInteger uiValueEventCounter = new AtomicInteger();
		uiValue.addListener(uiValueEventCounter::incrementAndGet);
		assertEquals(Integer.valueOf(42), uiValue.get());
		assertEquals(0, modelValueEventCounter.get());
		assertEquals(0, uiValueEventCounter.get());

		uiValue.set(20);
		assertEquals(Integer.valueOf(42), modelValue.get());//read only, no change
		assertEquals(0, modelValueEventCounter.get());
		assertEquals(1, uiValueEventCounter.get());

		modelValue.set(22);
		assertEquals(Integer.valueOf(22), uiValue.get());
		assertEquals(1, modelValueEventCounter.get());
		assertEquals(2, uiValueEventCounter.get());

		uiValue.set(22);
		assertEquals(1, modelValueEventCounter.get());
		assertEquals(2, uiValueEventCounter.get());

		uiValue.clear();
		assertNotNull(modelValue.get());
		assertNull(uiValue.get());
		assertEquals(1, modelValueEventCounter.get());
		assertEquals(3, uiValueEventCounter.get());
	}

	@Test
	void valueLinks() {
		Value<Integer> value1 = Value.value();
		Value<Integer> value2 = Value.value();
		Value<Integer> value3 = Value.value();
		value3.addValidator(value -> {
			if (value != null && value > 4) {
				throw new IllegalArgumentException();
			}
		});
		Value<Integer> value4 = Value.value();

		value1.link(value2);

		assertThrows(IllegalStateException.class, () -> value1.link(value2));//already linked

		value2.link(value3);
		value3.link(value4);

		value1.set(1);
		assertEquals(1, value2.get());
		assertEquals(1, value3.get());
		assertEquals(1, value4.get());

		value4.set(2);
		assertEquals(2, value1.get());
		assertEquals(2, value2.get());
		assertEquals(2, value3.get());

		assertThrows(IllegalStateException.class, () -> value4.link(value1));//cycle

		value3.set(3);
		assertEquals(3, value1.get());
		assertEquals(3, value2.get());
		assertEquals(3, value4.get());

		value2.set(4);
		assertEquals(4, value1.get());
		assertEquals(4, value3.get());
		assertEquals(4, value4.get());

		assertThrows(IllegalArgumentException.class, () -> value1.set(5));
		assertThrows(IllegalArgumentException.class, () -> value2.set(5));
		assertThrows(IllegalArgumentException.class, () -> value3.set(5));
		assertThrows(IllegalArgumentException.class, () -> value4.set(5));
	}

	@Test
	void valueAsConsumer() {
		Value<Integer> value = Value.value();
		Value<Integer> listeningValue = Value.value();

		value.addConsumer(listeningValue::set);
		value.set(1);

		assertEquals(1, listeningValue.get());

		listeningValue.set(2);

		value.set(3);

		assertEquals(3, listeningValue.get());
	}

	@Test
	void unlink() {
		Value<Integer> value = Value.builder()
						.nonNull(0)
						.validator(integer -> {
							if (integer > 2) {
								throw new IllegalArgumentException();
							}
						})
						.build();
		Value<Integer> originalValue = Value.value(1);

		value.link(originalValue);
		assertEquals(originalValue.get(), value.get());

		assertThrows(IllegalArgumentException.class, () -> originalValue.set(3));

		value.unlink(originalValue);

		originalValue.set(3);
		assertNotEquals(originalValue.get(), value.get());
		assertEquals(1, value.get());

		assertThrows(IllegalStateException.class, () -> value.unlink(originalValue));

		Observable<Integer> originalObservable = originalValue.observable();

		assertThrows(IllegalArgumentException.class, () -> value.link(originalObservable));

		originalValue.set(2);

		value.link(originalObservable);
		assertEquals(originalValue.get(), value.get());

		assertThrows(IllegalArgumentException.class, () -> originalValue.set(3));

		value.unlink(originalObservable);
		assertThrows(IllegalStateException.class, () -> value.unlink(originalObservable));

		originalValue.set(3);

		assertNotEquals(originalValue.get(), value.get());
		assertEquals(2, value.get());
	}

	@Test
	void weakListeners() {
		Value<Integer> value = Value.value();
		Observable<Integer> observer = value.observable();
		Runnable listener = () -> {};
		Consumer<Integer> consumer = integer -> {};
		observer.addWeakListener(listener);
		observer.addWeakListener(listener);
		observer.addWeakConsumer(consumer);
		observer.addWeakConsumer(consumer);
		value.set(1);
		observer.removeWeakListener(listener);
		observer.removeWeakConsumer(consumer);
	}

	@Test
	void setEqual() {
		class Test {

			final int value;
			final String text;

			Test(int value, String text) {
				this.value = value;
				this.text = text;
			}

			@Override
			public boolean equals(Object obj) {
				return obj instanceof Test && ((Test) obj).text.equals(text);
			}
		}
		Test test1 = new Test(1, "Hello");
		Test test2 = new Test(2, "Hello");
		Value<Test> value = Value.value(test1);
		value.addListener(() -> {
			throw new RuntimeException("Change event should not have been triggered");
		});
		value.set(test2);
		assertSame(test2, value.get());
	}

	@Test
	void map() {
		Value<Integer> value = Value.value(0);
		Function<Integer, Integer> increment = currentValue -> currentValue + 1;
		value.map(increment);
		assertEquals(1, value.get());
		value.map(currentValue -> null);
		assertTrue(value.isNull());
		value.map(currentValue -> 42);
		assertTrue(value.isNotNull());
	}
}

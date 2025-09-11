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

import is.codion.common.observer.Observable;
import is.codion.common.value.Value.Notify;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

public class ValueTest {

	private static final String NULL_STRING = "null";
	private static final String TEST_STRING = "test";
	private static final String HELLO_STRING = "hello";
	private static final int VALUE_42 = 42;
	private static final int VALUE_20 = 20;
	private static final int VALUE_22 = 22;
	private static final int VALUE_NEGATIVE_1 = -1;

	@Test
	void test() {
		Value.builder()
						.nullable(1)
						.notify(Notify.SET)
						.build();
		Value.builder()
						.nonNull("NullString")
						.value("Testing")
						.build();
		assertThrows(NullPointerException.class, () -> Value.nonNull(null));
	}

	@Test
	void validator() {
		Value.Validator<Integer> validator = value -> {
			if (value != null && value > 10) {
				throw new IllegalArgumentException();
			}
		};
		Value<Integer> value = Value.nonNull(0);
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
	void valueNonNullBehavior() {
		AtomicInteger eventCounter = new AtomicInteger();
		Value<Integer> intValue = Value.builder()
						.nonNull(VALUE_NEGATIVE_1)
						.value(VALUE_42)
						.build();
		assertFalse(intValue.isNullable());
		assertTrue(intValue.optional().isPresent());
		assertTrue(intValue.is(VALUE_42));
		Observable<Integer> observable = intValue.observable();
		assertFalse(observable.isNullable());
		assertTrue(observable.optional().isPresent());
		assertTrue(observable.is(VALUE_42));
		Runnable listener = eventCounter::incrementAndGet;
		assertTrue(observable.addListener(listener));
		assertFalse(observable.addListener(listener));
		observable.addConsumer(data -> {
			if (eventCounter.get() != 2) {
				assertNotNull(data);
			}
		});
		assertEquals(0, eventCounter.get());
		intValue.set(VALUE_20);
		assertEquals(1, eventCounter.get());
		assertTrue(intValue.is(VALUE_20));
		intValue.clear();
		assertTrue(intValue.is(VALUE_NEGATIVE_1));
		assertFalse(intValue.isNull());
		assertTrue(intValue.optional().isPresent());
		assertEquals(VALUE_NEGATIVE_1, intValue.get());
		assertEquals(VALUE_NEGATIVE_1, observable.get());
		assertEquals(2, eventCounter.get());
		intValue.clear();
		assertEquals(VALUE_NEGATIVE_1, intValue.get());
		assertEquals(VALUE_NEGATIVE_1, observable.get());
		assertEquals(2, eventCounter.get());
		intValue.set(VALUE_42);
		assertEquals(3, eventCounter.get());
		intValue.clear();
		assertEquals(VALUE_NEGATIVE_1, intValue.get());
		assertEquals(VALUE_NEGATIVE_1, observable.get());

		assertTrue(observable.removeListener(listener));
		assertFalse(observable.removeListener(listener));
	}

	@Test
	void valueStringNonNull() {
		Value<String> stringValue = Value.nonNull(NULL_STRING);
		assertFalse(stringValue.isNullable());
		assertEquals(NULL_STRING, stringValue.get());
		stringValue.set(TEST_STRING);
		assertEquals(TEST_STRING, stringValue.get());
		stringValue.clear();
		assertEquals(NULL_STRING, stringValue.get());
	}

	@Test
	void valueNullableOptional() {
		Value<String> value = Value.nullable();
		assertFalse(value.optional().isPresent());
		assertThrows(NoSuchElementException.class, value::getOrThrow);
		value.set(HELLO_STRING);
		assertTrue(value.optional().isPresent());
	}

	@Test
	void linkValues() {
		AtomicInteger modelValueEventCounter = new AtomicInteger();
		Value<Integer> modelValue = Value.nullable(VALUE_42);
		Value<Integer> uiValue = Value.nullable();
		uiValue.link(modelValue);

		assertThrows(IllegalStateException.class, () -> uiValue.link(modelValue));

		modelValue.addListener(modelValueEventCounter::incrementAndGet);
		AtomicInteger uiValueEventCounter = new AtomicInteger();
		uiValue.addListener(uiValueEventCounter::incrementAndGet);
		assertEquals(Integer.valueOf(VALUE_42), uiValue.get());
		assertEquals(0, modelValueEventCounter.get());
		assertEquals(0, uiValueEventCounter.get());

		uiValue.set(VALUE_20);
		assertEquals(Integer.valueOf(VALUE_20), modelValue.get());
		assertEquals(1, modelValueEventCounter.get());
		assertEquals(1, uiValueEventCounter.get());

		modelValue.set(VALUE_22);
		assertEquals(Integer.valueOf(VALUE_22), uiValue.get());
		assertEquals(2, modelValueEventCounter.get());
		assertEquals(2, uiValueEventCounter.get());

		uiValue.set(VALUE_22);
		assertEquals(2, modelValueEventCounter.get());
		assertEquals(2, uiValueEventCounter.get());

		uiValue.clear();
		assertNull(modelValue.get());
		assertTrue(modelValue.isNull());
		assertTrue(modelValue.is(null));
		assertNull(uiValue.get());
		assertTrue(uiValue.isNull());
		assertEquals(3, modelValueEventCounter.get());
		assertEquals(3, uiValueEventCounter.get());

		Value<Integer> valueOne = Value.nullable();
		assertThrows(IllegalArgumentException.class, () -> valueOne.link(valueOne));
	}

	@Test
	void linkValuesReadOnly() {
		AtomicInteger modelValueEventCounter = new AtomicInteger();
		Value<Integer> modelValue = Value.builder()
						.nonNull(0)
						.value(VALUE_42)
						.build();
		Value<Integer> uiValue = Value.nullable();
		assertFalse(modelValue.isNullable());
		uiValue.link(modelValue.observable());
		modelValue.addListener(modelValueEventCounter::incrementAndGet);
		AtomicInteger uiValueEventCounter = new AtomicInteger();
		uiValue.addListener(uiValueEventCounter::incrementAndGet);
		assertEquals(Integer.valueOf(VALUE_42), uiValue.get());
		assertEquals(0, modelValueEventCounter.get());
		assertEquals(0, uiValueEventCounter.get());

		uiValue.set(VALUE_20);
		assertEquals(Integer.valueOf(VALUE_42), modelValue.get());//read only, no change
		assertEquals(0, modelValueEventCounter.get());
		assertEquals(1, uiValueEventCounter.get());

		modelValue.set(VALUE_22);
		assertEquals(Integer.valueOf(VALUE_22), uiValue.get());
		assertEquals(1, modelValueEventCounter.get());
		assertEquals(2, uiValueEventCounter.get());

		uiValue.set(VALUE_22);
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
		Value<Integer> value1 = Value.nullable();
		Value<Integer> value2 = Value.nullable();
		Value<Integer> value3 = Value.nullable();
		value3.addValidator(value -> {
			if (value != null && value > 4) {
				throw new IllegalArgumentException();
			}
		});
		Value<Integer> value4 = Value.nullable();

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
		Value<Integer> value = Value.nullable();
		Value<Integer> listeningValue = Value.nullable();

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
		Value<Integer> originalValue = Value.nullable(1);

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
		Value<Integer> value = Value.nullable();
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
		Value<Test> value = Value.nullable(test1);
		value.addListener(() -> {
			throw new RuntimeException("Change event should not have been triggered");
		});
		value.set(test2);
		assertSame(test2, value.get());
	}

	@Test
	void map() {
		Value<Integer> value = Value.nullable(0);
		UnaryOperator<Integer> increment = currentValue -> currentValue + 1;
		value.map(increment);
		assertEquals(1, value.get());
		value.map(currentValue -> null);
		assertTrue(value.isNull());
		value.map(currentValue -> VALUE_42);
		assertFalse(value.isNull());
	}

	@Test
	void initialNullValue() {
		Value<Integer> value = new AbstractValue<Integer>(0) {
			Integer value;

			@Override
			protected Integer getValue() {
				return value;
			}

			@Override
			protected void setValue(Integer value) {
				this.value = value;
			}
		};
		assertNotNull(value.get());
	}
}

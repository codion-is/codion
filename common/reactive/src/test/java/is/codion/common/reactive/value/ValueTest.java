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

import is.codion.common.reactive.observer.Change;
import is.codion.common.reactive.observer.Observable;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.value.Value.Notify;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static is.codion.common.reactive.observer.Change.change;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
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
		Value<String> nonNull = Value.builder()
						.nonNull("NullString")
						.value("Testing")
						.build();
		nonNull.clear();
		assertEquals("NullString", nonNull.get());
		assertThrows(NullPointerException.class, () -> Value.nonNull(null));
		Value<Integer> value = Value.builder()
						.nonNull(0)
						.locked(true)
						.build();
		value.set(null);
		value.set(0);
		assertThrows(IllegalStateException.class, () -> value.set(1));
		value.locked().set(false);
		value.set(1);

		Value<Integer> value2 = Value.builder()
						.nonNull(1)
						.build();
		value2.link(value);
		assertEquals(1, value2.get());
		value.locked().set(true);
		assertThrows(IllegalStateException.class, () -> value2.set(2));
		assertEquals(1, value.get());
	}

	@Test
	void changeListener() {
		AtomicInteger counter = new AtomicInteger();
		Value<Integer> value = Value.nonNull(0);
		value.set(1);
		value.changed().addListener(counter::incrementAndGet);
		value.set(2);
		assertEquals(1, counter.get());
		value.set(2);
		assertEquals(1, counter.get());
		value.clear();
		assertEquals(2, counter.get());
		value.clear();
		assertEquals(2, counter.get());
		value.set(4);
		assertEquals(3, counter.get());
		value.set(4);
		assertEquals(3, counter.get());
	}

	@Test
	void changeConsumer() {
		List<Change<Integer>> changes = asList(
						change(null, 0),
						change(0, 1),
						change(1, 3),
						change(3, 5),
						change(5, null));
		AtomicInteger counter = new AtomicInteger();
		Value<Integer> value = Value.nullable();
		value.changed().addConsumer(change ->
						assertEquals(changes.get(counter.getAndIncrement()), change));
		value.set(0);
		value.set(1);
		value.set(3);
		value.set(3);
		value.set(5);
		value.clear();
		value.set(null);
		assertEquals(5, counter.get());
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

		assertThrows(IllegalArgumentException.class, () -> Value.builder()
						.nonNull(3)
						.validator(integer -> {
							if (integer > 2) {
								throw new IllegalArgumentException();
							}
						})
						.build());
	}

	@Test
	void setNullValue() {
		Value<Integer> value = new AbstractValue<>(0, Notify.CHANGED) {
			private Integer value;

			@Override
			protected Integer getValue() {
				return value;
			}

			@Override
			protected void setValue(Integer value) {
				this.value = value;
			}
		};
		AtomicInteger counter = new AtomicInteger();
		value.addListener(counter::incrementAndGet);
		assertEquals(0, value.get());
		value.set(0);// Should not trigger a change event
		assertEquals(0, counter.get());
		value.set(1);
		assertEquals(1, counter.get());
		value.clear();
		assertEquals(2, counter.get());
		value.set(0);// Should not trigger a change event
		assertEquals(2, counter.get());
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
	void arrayValue() {
		int[] one = new int[] {1};
		Value<int[]> value = Value.builder()
						.nonNull(new int[0])
						.build();
		value.set(new int[] {1});
		assertTrue(value.is(one));
	}

	@Test
	void arrayValueChange() {
		AtomicInteger counter = new AtomicInteger();
		int[] one = new int[] {1};
		int[] two = new int[] {2};
		Change<int[]> oneTwo = change(one, two);
		Value<int[]> value = Value.nonNull(new int[] {1});
		value.changed().when(oneTwo).addListener(counter::incrementAndGet);
		value.set(new int[] {2});
		assertEquals(1, counter.get());
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
	void linkChainDoesNotFalselyDetectCycle() {
		Value<Integer> value1 = Value.nullable();
		Value<Integer> value2 = Value.nullable();
		Value<Integer> value3 = Value.nullable();
		value1.link(value2);//value1 mirrors value2
		//linking value3 to value1, which is itself already linked, is a legitimate chain (value3 <- value1 <- value2), not a cycle
		assertDoesNotThrow(() -> value3.link(value1));
		value2.set(7);
		assertEquals(7, value1.get());
		assertEquals(7, value3.get());
		//a genuine transitive cycle is still detected
		assertThrows(IllegalStateException.class, () -> value2.link(value3));
	}

	@Test
	void linkValidationDoesNotCompound() {
		//one set validates both ends twice each, and that is the design: the bridge runs the far end's
		//validators before anything is written, so a value one end rejects moves neither, and the write
		//then propagates as a real set, which validates the far end again on its own terms.
		//A validator may carry side effects, so the bridge skips the partner bridge that would walk
		//straight back into the validators the set is already running - without that it would be three
		Value<Integer> linked = Value.nullable();
		Value<Integer> original = Value.nullable();
		AtomicInteger linkedValidations = new AtomicInteger();
		AtomicInteger originalValidations = new AtomicInteger();
		linked.addValidator(value -> linkedValidations.incrementAndGet());
		original.addValidator(value -> originalValidations.incrementAndGet());
		linked.link(original);
		linkedValidations.set(0);
		originalValidations.set(0);

		linked.set(1);
		assertEquals(2, linkedValidations.get());
		assertEquals(2, originalValidations.get());

		original.set(2);
		assertEquals(4, linkedValidations.get());
		assertEquals(4, originalValidations.get());
	}

	@Test
	void linkDiamond() {
		//a diamond: d mirrors both b and c, which both mirror a. Two shapes meet here that a chain does not
		//reach - the cycle walk arrives at a by two paths, and so does the validator bridge, which used to
		//recurse between the two until the stack gave out
		Value<Integer> a = Value.nullable();
		Value<Integer> b = Value.nullable();
		Value<Integer> c = Value.nullable();
		Value<Integer> d = Value.nullable();
		b.link(a);
		c.link(a);
		d.link(b);
		assertDoesNotThrow(() -> d.link(c));

		Value<Integer> e = Value.nullable();
		assertDoesNotThrow(() -> e.link(d));
		a.set(7);
		assertEquals(7, e.get());

		//every validator in the graph still runs, from either end
		a.addValidator(value -> {
			if (value != null && value == 13) {
				throw new IllegalArgumentException("no 13");
			}
		});
		assertThrows(IllegalArgumentException.class, () -> e.set(13));
		assertThrows(IllegalArgumentException.class, () -> a.set(13));
		assertEquals(7, a.get());
		assertEquals(7, e.get());

		//and a genuine cycle back into the diamond is still caught
		assertThrows(IllegalStateException.class, () -> a.link(e));
	}

	@Test
	void collectionValuesAreLinkedLikeAnyOther() {
		//ValueList and ValueSet extend BaseValue without going through AbstractValue, and the link guards
		//used to test for AbstractValue - so collection values got neither cycle detection nor validator
		//bridging, and a rejected write left the pair diverged
		ValueSet<String> original = ValueSet.valueSet();
		ValueSet<String> linked = ValueSet.valueSet();
		original.addValidator(values -> {
			if (values != null && values.contains("x")) {
				throw new IllegalArgumentException("no x");
			}
		});
		linked.link(original);

		//the far end's validator refuses before either end is written
		assertThrows(IllegalArgumentException.class, () -> linked.set(singleton("x")));
		assertTrue(linked.get().isEmpty());
		assertTrue(original.get().isEmpty());
		assertEquals(linked.get(), original.get());

		//and they still behave as one value
		linked.add("a");
		assertEquals(singleton("a"), original.get());
		original.add("b");
		assertEquals(new HashSet<>(asList("a", "b")), linked.get());

		//cycles are detected here too
		ValueSet<String> one = ValueSet.valueSet();
		ValueSet<String> two = ValueSet.valueSet();
		two.link(one);
		assertThrows(IllegalStateException.class, () -> one.link(two));
	}

	@Test
	void linkForeignValue() {
		//a Value implemented outside the framework keeps its validators to itself, so the bridge goes
		//through the public validate() gate rather than reaching in - the invariant it exists for holds
		//either way: neither end is written when the other refuses
		ForeignValue<String> foreign = new ForeignValue<>();
		foreign.addValidator(value -> {
			if ("x".equals(value)) {
				throw new IllegalArgumentException("no x");
			}
		});
		Value<String> value = Value.nullable();
		assertDoesNotThrow(() -> value.link(foreign));

		//they behave as one value
		value.set("a");
		assertEquals("a", foreign.get());
		foreign.set("b");
		assertEquals("b", value.get());

		//and the far end refuses before either moves
		assertThrows(IllegalArgumentException.class, () -> value.set("x"));
		assertEquals("b", value.get());
		assertEquals("b", foreign.get());
	}

	/**
	 * A {@link Value} from outside the framework: it implements the interface without extending
	 * {@link AbstractValue}, so none of the link machinery can reach into it. Delegation rather than a
	 * from-scratch implementation, so it behaves like a real one.
	 */
	private static final class ForeignValue<T> implements Value<T> {

		private final Value<T> delegate = Value.nullable();

		@Override
		public @Nullable T get() {
			return delegate.get();
		}

		@Override
		public boolean isNullable() {
			return delegate.isNullable();
		}

		@Override
		public Observer<T> observer() {
			return delegate.observer();
		}

		@Override
		public void set(@Nullable T value) {
			delegate.set(value);
		}

		@Override
		public void clear() {
			delegate.clear();
		}

		@Override
		public Observable<T> observable() {
			return delegate.observable();
		}

		@Override
		public Locked locked() {
			return delegate.locked();
		}

		@Override
		public void link(Value<T> originalValue) {
			delegate.link(originalValue);
		}

		@Override
		public void unlink(Value<T> originalValue) {
			delegate.unlink(originalValue);
		}

		@Override
		public void link(Observable<T> observable) {
			delegate.link(observable);
		}

		@Override
		public void unlink(Observable<T> observable) {
			delegate.unlink(observable);
		}

		@Override
		public boolean addValidator(Validator<? super T> validator) {
			return delegate.addValidator(validator);
		}

		@Override
		public boolean removeValidator(Validator<? super T> validator) {
			return delegate.removeValidator(validator);
		}

		@Override
		public void validate(@Nullable T value) {
			delegate.validate(value);
		}
	}

	@Test
	void linkObservableRejectsDuplicate() {
		Value<Integer> value = Value.nullable();
		Value<Integer> source = Value.nullable();
		Observable<Integer> observable = source.observable();
		value.link(observable);
		//re-linking the same observable must be rejected rather than silently leaking a feeder
		assertThrows(IllegalStateException.class, () -> value.link(observable));
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
	void update() {
		Value<Integer> value = Value.nullable(0);
		UnaryOperator<Integer> increment = currentValue -> currentValue + 1;
		value.update(increment);
		assertEquals(1, value.get());
		value.update(currentValue -> null);
		assertTrue(value.isNull());
		value.update(currentValue -> VALUE_42);
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

	@Test
	void arraysEqual() {
		// Test byte arrays
		AtomicInteger byteArrayCounter = new AtomicInteger();
		Value<byte[]> byteArrayValue = Value.nullable(new byte[] {1, 2, 3});
		byteArrayValue.addListener(byteArrayCounter::incrementAndGet);

		// Setting to equal array should not trigger notification (Notify.CHANGED is default)
		byteArrayValue.set(new byte[] {1, 2, 3});
		assertEquals(0, byteArrayCounter.get(), "Equal byte arrays should not trigger notification");

		// Setting to different array should trigger notification
		byteArrayValue.set(new byte[] {1, 2, 4});
		assertEquals(1, byteArrayCounter.get(), "Different byte arrays should trigger notification");

		// Test int arrays
		AtomicInteger intArrayCounter = new AtomicInteger();
		Value<int[]> intArrayValue = Value.nullable(new int[] {10, 20, 30});
		intArrayValue.addListener(intArrayCounter::incrementAndGet);

		intArrayValue.set(new int[] {10, 20, 30});
		assertEquals(0, intArrayCounter.get(), "Equal int arrays should not trigger notification");

		intArrayValue.set(new int[] {10, 20, 31});
		assertEquals(1, intArrayCounter.get(), "Different int arrays should trigger notification");

		// Test Object arrays
		AtomicInteger objectArrayCounter = new AtomicInteger();
		Value<String[]> stringArrayValue = Value.nullable(new String[] {"a", "b", "c"});
		stringArrayValue.addListener(objectArrayCounter::incrementAndGet);

		stringArrayValue.set(new String[] {"a", "b", "c"});
		assertEquals(0, objectArrayCounter.get(), "Equal String arrays should not trigger notification");

		stringArrayValue.set(new String[] {"a", "b", "d"});
		assertEquals(1, objectArrayCounter.get(), "Different String arrays should trigger notification");

		// Test multi-dimensional arrays
		AtomicInteger multiDimCounter = new AtomicInteger();
		Value<int[][]> multiDimValue = Value.nullable(new int[][] {{1, 2}, {3, 4}});
		multiDimValue.addListener(multiDimCounter::incrementAndGet);

		multiDimValue.set(new int[][] {{1, 2}, {3, 4}});
		assertEquals(0, multiDimCounter.get(), "Equal multi-dimensional arrays should not trigger notification");

		multiDimValue.set(new int[][] {{1, 2}, {3, 5}});
		assertEquals(1, multiDimCounter.get(), "Different multi-dimensional arrays should trigger notification");

		// Test null array handling
		byteArrayValue.set(null);
		assertEquals(2, byteArrayCounter.get(), "Setting to null should trigger notification");

		byteArrayValue.set(null);
		assertEquals(2, byteArrayCounter.get(), "Setting null to null should not trigger notification");

		// Test with Notify.SET (should always notify, even for equal arrays)
		Value<byte[]> alwaysNotifyValue = Value.builder()
						.nullable(new byte[] {1, 2, 3})
						.notify(Notify.SET)
						.build();
		AtomicInteger setCounter = new AtomicInteger();
		alwaysNotifyValue.addListener(setCounter::incrementAndGet);

		alwaysNotifyValue.set(new byte[] {1, 2, 3});
		assertEquals(1, setCounter.get(), "Notify.SET should notify even for equal arrays");

		alwaysNotifyValue.set(new byte[] {1, 2, 3});
		assertEquals(2, setCounter.get(), "Notify.SET should notify every time");
	}

	@Test
	void builderListenerOrder() {
		List<Object> notified = new ArrayList<>();
		Runnable firstListener = () -> notified.add(1);
		Consumer<Integer> firstConsumer = value -> notified.add(2);
		Runnable weakListener = () -> notified.add(3);
		Runnable oneListener = () -> notified.add(4);
		Consumer<Integer> weakConsumer = value -> notified.add(5);
		Runnable secondListener = () -> notified.add(6);
		Consumer<Integer> oneConsumer = value -> notified.add(7);
		Consumer<Integer> secondConsumer = value -> notified.add(8);
		Value.builder()
						.nonNull(0)
						.listener(firstListener)
						.consumer(firstConsumer)
						.weakListener(weakListener)
						.when(1, oneListener)
						.weakConsumer(weakConsumer)
						.listener(secondListener)
						.when(1, oneConsumer)
						.consumer(secondConsumer)
						.build()
						.set(1);
		assertEquals(asList(1, 2, 3, 4, 5, 6, 7, 8), notified);
	}
}

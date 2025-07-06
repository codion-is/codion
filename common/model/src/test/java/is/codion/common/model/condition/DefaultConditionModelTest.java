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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.model.condition;

import is.codion.common.Operator;
import is.codion.common.model.condition.ConditionModel.Operands;
import is.codion.common.model.condition.ConditionModel.Wildcard;
import is.codion.common.value.Value;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultConditionModelTest {

	private static final Integer TEST_VALUE_1 = 1;
	private static final Integer TEST_VALUE_2 = 2;

	final AtomicInteger equalCounter = new AtomicInteger();
	final AtomicInteger inCounter = new AtomicInteger();
	final AtomicInteger upperCounter = new AtomicInteger();
	final AtomicInteger lowerCounter = new AtomicInteger();
	final AtomicInteger conditionChangedCounter = new AtomicInteger();
	final AtomicInteger operatorCounter = new AtomicInteger();
	final AtomicInteger enabledCounter = new AtomicInteger();

	final Consumer<String> equalConsumer = value -> equalCounter.incrementAndGet();
	final Consumer<Set<String>> inConsumer = value -> inCounter.incrementAndGet();
	final Consumer<String> upperConsumer = value -> upperCounter.incrementAndGet();
	final Consumer<String> lowerConsumer = value -> lowerCounter.incrementAndGet();
	final Runnable conditionChangedListener = conditionChangedCounter::incrementAndGet;
	final Consumer<Operator> operatorConsumer = data -> operatorCounter.incrementAndGet();
	final Runnable enabledListener = enabledCounter::incrementAndGet;

	@Test
	void testOperands() {
		ConditionModel<String> model = ConditionModel.builder()
						.valueClass(String.class)
						.autoEnable(false)
						.build();
		model.caseSensitive().set(false);
		model.operands().wildcard().set(Wildcard.NONE);

		Operands<String> operands = model.operands();
		operands.equal().addConsumer(equalConsumer);
		operands.in().addConsumer(inConsumer);
		operands.upper().addConsumer(upperConsumer);
		operands.lower().addConsumer(lowerConsumer);
		model.changed().addListener(conditionChangedListener);

		operands.upper().set("hello");
		assertEquals(1, conditionChangedCounter.get());
		assertFalse(model.enabled().get());
		assertEquals(1, upperCounter.get());
		assertEquals("hello", operands.upper().get());
		operands.lower().set("hello");
		assertEquals(2, conditionChangedCounter.get());
		assertEquals(1, lowerCounter.get());
		assertEquals("hello", operands.lower().get());

		operands.equal().set("test");
		assertEquals(1, equalCounter.get());
		assertEquals("test", operands.equal().get());

		operands.wildcard().set(Wildcard.PREFIX_AND_POSTFIX);
		assertEquals("%test%", operands.equalWithWildcards());

		operands.wildcard().set(Wildcard.PREFIX);
		assertEquals("%test", operands.equalWithWildcards());

		operands.wildcard().set(Wildcard.POSTFIX);
		assertEquals("test%", operands.equalWithWildcards());

		operands.wildcard().set(Wildcard.NONE);
		assertEquals("test", operands.equalWithWildcards());

		model.clear();

		operands.equal().removeConsumer(equalConsumer);
		operands.in().removeConsumer(inConsumer);
		operands.upper().removeConsumer(upperConsumer);
		operands.lower().removeConsumer(lowerConsumer);
		model.changed().removeListener(conditionChangedListener);
	}

	@Test
	void testMisc() {
		ConditionModel<String> model = ConditionModel.builder()
						.valueClass(String.class).build();
		model.operands().wildcard().set(Wildcard.PREFIX_AND_POSTFIX);
		model.set().equalTo("upper");
		assertEquals("%upper%", model.operands().equalWithWildcards());

		assertThrows(NullPointerException.class, () -> model.set().in((Collection<String>) null));
		assertThrows(NullPointerException.class, () -> model.set().in("test", null));
	}

	@Test
	void testOperator() {
		assertThrows(IllegalArgumentException.class, () -> ConditionModel.builder()
						.valueClass(String.class)
						.operators(Arrays.asList(Operator.EQUAL, Operator.NOT_BETWEEN))
						.operator(Operator.IN));
		assertThrows(IllegalArgumentException.class, () -> ConditionModel.builder()
						.valueClass(String.class)
						.operator(Operator.IN)
						.operators(Arrays.asList(Operator.EQUAL, Operator.NOT_BETWEEN)));

		ConditionModel<String> model = ConditionModel.builder()
						.valueClass(String.class)
						.operators(Arrays.asList(Operator.EQUAL, Operator.NOT_EQUAL, Operator.LESS_THAN_OR_EQUAL, Operator.NOT_BETWEEN))
						.build();
		model.operator().addConsumer(operatorConsumer);
		assertEquals(Operator.EQUAL, model.operator().get());
		model.operator().set(Operator.LESS_THAN_OR_EQUAL);
		assertEquals(1, operatorCounter.get());
		assertEquals(Operator.LESS_THAN_OR_EQUAL, model.operator().get());
		model.operator().clear();
		assertEquals(Operator.EQUAL, model.operator().get());
		model.operator().set(Operator.NOT_BETWEEN);
		assertEquals(3, operatorCounter.get());
		model.operator().removeConsumer(operatorConsumer);

		assertThrows(IllegalArgumentException.class, () -> model.operator().set(Operator.BETWEEN));

		assertThrows(IllegalArgumentException.class, () -> model.operator().set(Operator.BETWEEN));
	}

	@Test
	void test() {
		ConditionModel<String> model = ConditionModel.builder()
						.valueClass(String.class).build();
		assertTrue(model.autoEnable().get());
		model.operands().equal().set("test");
		assertTrue(model.enabled().get());
		model.caseSensitive().set(false);
		assertFalse(model.caseSensitive().get());
		assertEquals(String.class, model.valueClass());

		model.operands().wildcard().set(Wildcard.PREFIX_AND_POSTFIX);
		assertEquals(Wildcard.PREFIX_AND_POSTFIX, model.operands().wildcard().get());

		model.enabled().addListener(enabledListener);
		model.enabled().set(false);
		assertEquals(1, enabledCounter.get());
		model.enabled().set(true);
		assertEquals(2, enabledCounter.get());

		model.enabled().removeListener(enabledListener);

		model.locked().set(true);
		assertTrue(model.locked().get());
		assertTrue(model.locked().get());
	}

	@Test
	void lockedModelPreventsModification() {
		ConditionModel<String> model = ConditionModel.builder()
						.valueClass(String.class).build();
		model.locked().set(true);

		// Verify all modification attempts throw IllegalStateException when locked
		assertThrows(IllegalStateException.class, () -> model.operands().upper().set("test"));
		assertThrows(IllegalStateException.class, () -> model.operands().lower().set("test"));
		assertThrows(IllegalStateException.class, () -> model.operands().equal().set("test"));
		assertThrows(IllegalStateException.class, () -> model.operands().in().set(Collections.singletonList("test")));
		assertThrows(IllegalStateException.class, () -> model.enabled().set(true));
		assertThrows(IllegalStateException.class, () -> model.operator().set(Operator.NOT_EQUAL));
	}

	@Test
	void multiConditionString() {
		ConditionModel<String> condition = ConditionModel.builder()
						.valueClass(String.class).build();
		condition.caseSensitive().set(false);
		condition.operands().wildcard().set(Wildcard.NONE);

		Collection<String> strings = asList("abc", "def");
		condition.operands().in().set(strings);

		assertTrue(condition.operands().in().get().containsAll(strings));
	}

	@Test
	void autoEnable_equal_enablesWithEqualValue() {
		ConditionModel<Integer> condition = ConditionModel.builder()
						.valueClass(Integer.class)
						.build();
		condition.operator().set(Operator.EQUAL);
		Operands<Integer> operands = condition.operands();

		assertFalse(condition.enabled().get());
		operands.equal().set(TEST_VALUE_1);
		assertTrue(condition.enabled().get());
		operands.equal().set(null);
		assertFalse(condition.enabled().get());

		// Setting other operands should not affect auto-enable for EQUAL
		condition.enabled().set(false);
		operands.upper().set(TEST_VALUE_1);
		operands.lower().set(TEST_VALUE_1);
		assertFalse(condition.enabled().get());
	}

	@Test
	void autoEnable_notEqual_enablesWithEqualValue() {
		ConditionModel<Integer> condition = ConditionModel.builder()
						.valueClass(Integer.class)
						.build();
		condition.operator().set(Operator.NOT_EQUAL);
		Operands<Integer> operands = condition.operands();

		assertFalse(condition.enabled().get());
		operands.equal().set(TEST_VALUE_1);
		assertTrue(condition.enabled().get());
		operands.equal().set(null);
		assertFalse(condition.enabled().get());

		// Setting other operands should not affect auto-enable for NOT_EQUAL
		condition.enabled().set(false);
		operands.upper().set(TEST_VALUE_1);
		operands.lower().set(TEST_VALUE_1);
		assertFalse(condition.enabled().get());
	}

	@Test
	void autoEnable_lessThan_enablesWithUpperValue() {
		ConditionModel<Integer> condition = ConditionModel.builder()
						.valueClass(Integer.class)
						.build();
		condition.operator().set(Operator.LESS_THAN);
		Operands<Integer> operands = condition.operands();

		assertFalse(condition.enabled().get());
		operands.upper().set(TEST_VALUE_1);
		assertTrue(condition.enabled().get());
		operands.upper().set(null);
		assertFalse(condition.enabled().get());

		// Setting lower operand should not affect auto-enable for LESS_THAN
		operands.lower().set(TEST_VALUE_1);
		assertFalse(condition.enabled().get());
	}

	@Test
	void autoEnable_lessThanOrEqual_enablesWithUpperValue() {
		ConditionModel<Integer> condition = ConditionModel.builder()
						.valueClass(Integer.class)
						.build();
		condition.operator().set(Operator.LESS_THAN_OR_EQUAL);
		Operands<Integer> operands = condition.operands();

		assertFalse(condition.enabled().get());
		operands.upper().set(TEST_VALUE_1);
		assertTrue(condition.enabled().get());
		operands.upper().set(null);
		assertFalse(condition.enabled().get());

		// Setting lower operand should not affect auto-enable for LESS_THAN_OR_EQUAL
		operands.lower().set(TEST_VALUE_1);
		assertFalse(condition.enabled().get());
	}

	@Test
	void autoEnable_greaterThan_enablesWithLowerValue() {
		ConditionModel<Integer> condition = ConditionModel.builder()
						.valueClass(Integer.class)
						.build();
		condition.operator().set(Operator.GREATER_THAN);
		Operands<Integer> operands = condition.operands();

		assertFalse(condition.enabled().get());
		operands.lower().set(TEST_VALUE_1);
		assertTrue(condition.enabled().get());
		operands.lower().set(null);
		assertFalse(condition.enabled().get());

		// Setting upper operand should not affect auto-enable for GREATER_THAN
		operands.upper().set(TEST_VALUE_1);
		assertFalse(condition.enabled().get());
	}

	@Test
	void autoEnable_greaterThanOrEqual_enablesWithLowerValue() {
		ConditionModel<Integer> condition = ConditionModel.builder()
						.valueClass(Integer.class)
						.build();
		condition.operator().set(Operator.GREATER_THAN_OR_EQUAL);
		Operands<Integer> operands = condition.operands();

		assertFalse(condition.enabled().get());
		operands.lower().set(TEST_VALUE_1);
		assertTrue(condition.enabled().get());
		operands.lower().set(null);
		assertFalse(condition.enabled().get());

		// Setting upper operand should not affect auto-enable for GREATER_THAN_OR_EQUAL
		operands.upper().set(TEST_VALUE_1);
		assertFalse(condition.enabled().get());
	}

	@Test
	void autoEnable_between_enablesWithBothBounds() {
		ConditionModel<Integer> condition = ConditionModel.builder()
						.valueClass(Integer.class)
						.build();
		condition.operator().set(Operator.BETWEEN);
		Operands<Integer> operands = condition.operands();

		assertFalse(condition.enabled().get());

		// Equal operand should not affect auto-enable for BETWEEN
		operands.equal().set(TEST_VALUE_1);
		assertFalse(condition.enabled().get());
		operands.equal().set(null);

		// Single bound is not sufficient
		operands.lower().set(TEST_VALUE_1);
		assertFalse(condition.enabled().get());

		// Both bounds required for auto-enable
		operands.upper().set(TEST_VALUE_2);
		assertTrue(condition.enabled().get());

		// Removing either bound disables
		operands.lower().set(null);
		assertFalse(condition.enabled().get());
		operands.lower().set(TEST_VALUE_1);
		assertTrue(condition.enabled().get());
	}

	@Test
	void autoEnable_betweenExclusive_enablesWithBothBounds() {
		ConditionModel<Integer> condition = ConditionModel.builder()
						.valueClass(Integer.class)
						.build();
		condition.operator().set(Operator.BETWEEN_EXCLUSIVE);
		Operands<Integer> operands = condition.operands();

		assertFalse(condition.enabled().get());

		// Equal operand should not affect auto-enable for BETWEEN_EXCLUSIVE
		operands.equal().set(TEST_VALUE_1);
		assertFalse(condition.enabled().get());
		operands.equal().set(null);

		// Single bound is not sufficient
		operands.lower().set(TEST_VALUE_1);
		assertFalse(condition.enabled().get());

		// Both bounds required for auto-enable
		operands.upper().set(TEST_VALUE_2);
		assertTrue(condition.enabled().get());

		// Removing either bound disables
		operands.lower().set(null);
		assertFalse(condition.enabled().get());
		operands.lower().set(TEST_VALUE_1);
		assertTrue(condition.enabled().get());
	}

	@Test
	void autoEnable_notBetween_enablesWithBothBounds() {
		ConditionModel<Integer> condition = ConditionModel.builder()
						.valueClass(Integer.class)
						.build();
		condition.operator().set(Operator.NOT_BETWEEN);
		Operands<Integer> operands = condition.operands();

		assertFalse(condition.enabled().get());

		// Equal operand should not affect auto-enable for NOT_BETWEEN
		operands.equal().set(TEST_VALUE_1);
		assertFalse(condition.enabled().get());
		operands.equal().set(null);

		// Single bound is not sufficient
		operands.lower().set(TEST_VALUE_1);
		assertFalse(condition.enabled().get());

		// Both bounds required for auto-enable
		operands.upper().set(TEST_VALUE_2);
		assertTrue(condition.enabled().get());

		// Removing either bound disables
		operands.lower().set(null);
		assertFalse(condition.enabled().get());
		operands.lower().set(TEST_VALUE_1);
		assertTrue(condition.enabled().get());
	}

	@Test
	void autoEnable_notBetweenExclusive_enablesWithBothBounds() {
		ConditionModel<Integer> condition = ConditionModel.builder()
						.valueClass(Integer.class)
						.build();
		condition.operator().set(Operator.NOT_BETWEEN_EXCLUSIVE);
		Operands<Integer> operands = condition.operands();

		assertFalse(condition.enabled().get());

		// Equal operand should not affect auto-enable for NOT_BETWEEN_EXCLUSIVE
		operands.equal().set(TEST_VALUE_1);
		assertFalse(condition.enabled().get());
		operands.equal().set(null);

		// Single bound is not sufficient
		operands.lower().set(TEST_VALUE_1);
		assertFalse(condition.enabled().get());

		// Both bounds required for auto-enable
		operands.upper().set(TEST_VALUE_2);
		assertTrue(condition.enabled().get());

		// Removing either bound disables
		operands.lower().set(null);
		assertFalse(condition.enabled().get());
		operands.lower().set(TEST_VALUE_1);
		assertTrue(condition.enabled().get());
	}

	@Test
	void noOperators() {
		assertThrows(IllegalArgumentException.class, () -> ConditionModel.builder()
						.valueClass(String.class)
						.operators(emptyList()));
	}

	@Test
	void includeInteger() {
		ConditionModel<Integer> condition = ConditionModel.builder()
						.valueClass(Integer.class)
						.autoEnable(false)
						.operator(Operator.EQUAL)
						.build();
		condition.enabled().set(true);

		Operands<Integer> operands = condition.operands();
		operands.equal().set(null);
		assertTrue(condition.accepts(null));
		assertFalse(condition.accepts(1));

		condition.operator().set(Operator.NOT_EQUAL);
		assertFalse(condition.accepts(null));
		assertTrue(condition.accepts(1));

		condition.operator().set(Operator.EQUAL);

		operands.equal().set(10);
		assertFalse(condition.accepts(null));
		assertFalse(condition.accepts(9));
		assertTrue(condition.accepts(10));
		assertFalse(condition.accepts(11));

		condition.operator().set(Operator.NOT_EQUAL);
		assertTrue(condition.accepts(null));
		assertTrue(condition.accepts(9));
		assertFalse(condition.accepts(10));
		assertTrue(condition.accepts(11));

		operands.lower().set(10);
		condition.operator().set(Operator.GREATER_THAN_OR_EQUAL);
		assertFalse(condition.accepts(null));
		assertFalse(condition.accepts(9));
		assertTrue(condition.accepts(10));
		assertTrue(condition.accepts(11));
		condition.operator().set(Operator.GREATER_THAN);
		assertFalse(condition.accepts(null));
		assertFalse(condition.accepts(9));
		assertFalse(condition.accepts(10));
		assertTrue(condition.accepts(11));

		operands.upper().set(10);
		condition.operator().set(Operator.LESS_THAN_OR_EQUAL);
		assertFalse(condition.accepts(null));
		assertTrue(condition.accepts(9));
		assertTrue(condition.accepts(10));
		assertFalse(condition.accepts(11));
		condition.operator().set(Operator.LESS_THAN);
		assertFalse(condition.accepts(null));
		assertTrue(condition.accepts(9));
		assertFalse(condition.accepts(10));
		assertFalse(condition.accepts(11));

		operands.lower().set(6);
		condition.operator().set(Operator.BETWEEN);
		assertFalse(condition.accepts(null));
		assertTrue(condition.accepts(6));
		assertTrue(condition.accepts(7));
		assertTrue(condition.accepts(9));
		assertTrue(condition.accepts(10));
		assertFalse(condition.accepts(11));
		assertFalse(condition.accepts(5));
		condition.operator().set(Operator.BETWEEN_EXCLUSIVE);
		assertFalse(condition.accepts(null));
		assertFalse(condition.accepts(6));
		assertTrue(condition.accepts(7));
		assertTrue(condition.accepts(9));
		assertFalse(condition.accepts(10));
		assertFalse(condition.accepts(11));
		assertFalse(condition.accepts(5));

		condition.operator().set(Operator.NOT_BETWEEN);
		assertFalse(condition.accepts(null));
		assertTrue(condition.accepts(6));
		assertFalse(condition.accepts(7));
		assertFalse(condition.accepts(9));
		assertTrue(condition.accepts(10));
		assertTrue(condition.accepts(11));
		assertTrue(condition.accepts(5));
		condition.operator().set(Operator.NOT_BETWEEN_EXCLUSIVE);
		assertFalse(condition.accepts(null));
		assertFalse(condition.accepts(6));
		assertFalse(condition.accepts(7));
		assertFalse(condition.accepts(9));
		assertFalse(condition.accepts(10));
		assertTrue(condition.accepts(11));
		assertTrue(condition.accepts(5));

		operands.upper().set(null);
		operands.lower().set(null);
		condition.operator().set(Operator.BETWEEN);
		assertTrue(condition.accepts(1));
		assertTrue(condition.accepts(8));
		assertTrue(condition.accepts(11));
		condition.operator().set(Operator.BETWEEN_EXCLUSIVE);
		assertTrue(condition.accepts(1));
		assertTrue(condition.accepts(8));
		assertTrue(condition.accepts(11));
		condition.operator().set(Operator.NOT_BETWEEN);
		assertTrue(condition.accepts(1));
		assertTrue(condition.accepts(8));
		assertTrue(condition.accepts(11));
		condition.operator().set(Operator.NOT_BETWEEN_EXCLUSIVE);
		assertTrue(condition.accepts(1));
		assertTrue(condition.accepts(8));
		assertTrue(condition.accepts(11));

		assertTrue(condition.accepts(null));
		assertTrue(condition.accepts(5));
		assertTrue(condition.accepts(6));
		assertTrue(condition.accepts(7));

		condition.operator().set(Operator.IN);
		operands.in().set(asList(1, 2, 3));
		assertTrue(condition.accepts(1));
		assertTrue(condition.accepts(2));
		assertTrue(condition.accepts(3));
		assertFalse(condition.accepts(4));
		condition.operator().set(Operator.NOT_IN);
		assertFalse(condition.accepts(1));
		assertFalse(condition.accepts(2));
		assertFalse(condition.accepts(3));
		assertTrue(condition.accepts(4));
	}

	@Test
	void acceptsString() {
		ConditionModel<String> condition = ConditionModel.builder()
						.valueClass(String.class)
						.autoEnable(false)
						.build();
		condition.enabled().set(true);

		condition.operator().set(Operator.EQUAL);
		Operands<String> operands = condition.operands();
		operands.equal().set("hello");
		assertTrue(condition.accepts("hello"));
		operands.equal().set("hell%");
		assertTrue(condition.accepts("hello"));
		assertFalse(condition.accepts("helo"));
		operands.equal().set("%ell%");
		assertTrue(condition.accepts("hello"));
		assertFalse(condition.accepts("helo"));

		condition.caseSensitive().set(false);
		assertTrue(condition.accepts("HELlo"));
		assertFalse(condition.accepts("heLo"));
		assertFalse(condition.accepts(null));

		operands.equal().set("%");
		assertTrue(condition.accepts("hello"));
		assertTrue(condition.accepts("helo"));

		condition.caseSensitive().set(true);
		operands.equal().set("hello");
		condition.operator().set(Operator.NOT_EQUAL);
		assertFalse(condition.accepts("hello"));
		operands.equal().set("hell%");
		assertFalse(condition.accepts("hello"));
		assertTrue(condition.accepts("helo"));
		operands.equal().set("%ell%");
		assertFalse(condition.accepts("hello"));
		assertTrue(condition.accepts("helo"));

		condition.caseSensitive().set(false);
		assertFalse(condition.accepts("HELlo"));
		assertTrue(condition.accepts("heLo"));
		assertTrue(condition.accepts(null));

		operands.equal().set("%");
		assertFalse(condition.accepts("hello"));
		assertFalse(condition.accepts("helo"));

		operands.equal().set("hell");
		condition.operator().set(Operator.EQUAL);
		condition.operands().wildcard().set(Wildcard.NONE);
		assertTrue(condition.accepts("HELl"));
		assertTrue(condition.accepts("hElL"));
		condition.operator().set(Operator.NOT_EQUAL);
		assertFalse(condition.accepts("HELl"));
		assertFalse(condition.accepts("hElL"));

		condition.operator().set(Operator.EQUAL);
		condition.operands().wildcard().set(Wildcard.PREFIX_AND_POSTFIX);
		assertTrue(condition.accepts("hELlo"));
		assertTrue(condition.accepts("rhElLoo"));
		condition.operator().set(Operator.NOT_EQUAL);
		assertFalse(condition.accepts("hELlo"));
		assertFalse(condition.accepts("rhElLoo"));
	}

	@Test
	void acceptCharacter() {
		ConditionModel<Character> condition = ConditionModel.builder()
						.valueClass(Character.class)
						.build();
		condition.autoEnable().set(false);
		condition.enabled().set(true);

		condition.caseSensitive().set(true);
		condition.operator().set(Operator.EQUAL);
		condition.operands().equal().set('h');
		assertTrue(condition.accepts('h'));
		assertFalse(condition.accepts('H'));

		condition.caseSensitive().set(false);
		assertTrue(condition.accepts('H'));
	}

	@Nested
	@DisplayName("Set operations")
	class SetOperationsTest {

		private ConditionModel<Integer> condition;

		@BeforeEach
		void setUp() {
			condition = ConditionModel.builder()
							.valueClass(Integer.class)
							.autoEnable(false)
							.build();
		}

		@Nested
		@DisplayName("Null operations")
		class NullOperationsTest {

			@Test
			@DisplayName("isNull operation")
			void set_isNull_enablesCondition() {
				boolean changed = condition.set().isNull();

				assertFalse(changed);
				assertTrue(condition.enabled().get());
			}

			@Test
			@DisplayName("isNotNull operation")
			void set_isNotNull_enablesCondition() {
				boolean changed = condition.set().isNotNull();

				assertTrue(changed);
				assertTrue(condition.enabled().get());
			}
		}

		@Nested
		@DisplayName("Equality operations")
		class EqualityOperationsTest {

			@Test
			@DisplayName("equalTo with value enables condition")
			void set_equalToWithValue_enablesCondition() {
				boolean changed = condition.set().equalTo(TEST_VALUE_1);

				assertTrue(changed);
				assertTrue(condition.enabled().get());
			}

			@Test
			@DisplayName("equalTo with null disables condition")
			void set_equalToWithNull_disablesCondition() {
				// First set a non-null value to ensure state change
				condition.set().equalTo(TEST_VALUE_1);

				boolean changed = condition.set().equalTo(null);

				assertTrue(changed);
				assertFalse(condition.enabled().get());
			}

			@Test
			@DisplayName("notEqualTo with value enables condition")
			void set_notEqualToWithValue_enablesCondition() {
				boolean changed = condition.set().notEqualTo(TEST_VALUE_1);

				assertTrue(changed);
				assertTrue(condition.enabled().get());
			}

			@Test
			@DisplayName("notEqualTo with null disables condition")
			void set_notEqualToWithNull_disablesCondition() {
				// First set a non-null value to ensure state change
				condition.set().notEqualTo(TEST_VALUE_1);

				boolean changed = condition.set().notEqualTo(null);

				assertTrue(changed);
				assertFalse(condition.enabled().get());
			}
		}

		@Nested
		@DisplayName("Comparison operations")
		class ComparisonOperationsTest {

			@Test
			@DisplayName("greaterThan with value enables condition")
			void set_greaterThanWithValue_enablesCondition() {
				boolean changed = condition.set().greaterThan(TEST_VALUE_1);

				assertTrue(changed);
				assertTrue(condition.enabled().get());
			}

			@Test
			@DisplayName("greaterThan with null disables condition")
			void set_greaterThanWithNull_disablesCondition() {
				// First set a non-null value to ensure state change
				condition.set().greaterThan(TEST_VALUE_1);

				boolean changed = condition.set().greaterThan(null);

				assertTrue(changed);
				assertFalse(condition.enabled().get());
			}

			@Test
			@DisplayName("greaterThanOrEqualTo with value enables condition")
			void set_greaterThanOrEqualToWithValue_enablesCondition() {
				boolean changed = condition.set().greaterThanOrEqualTo(TEST_VALUE_1);

				assertTrue(changed);
				assertTrue(condition.enabled().get());
			}

			@Test
			@DisplayName("greaterThanOrEqualTo with null disables condition")
			void set_greaterThanOrEqualToWithNull_disablesCondition() {
				// First set a non-null value to ensure state change
				condition.set().greaterThanOrEqualTo(TEST_VALUE_1);

				boolean changed = condition.set().greaterThanOrEqualTo(null);

				assertTrue(changed);
				assertFalse(condition.enabled().get());
			}

			@Test
			@DisplayName("lessThan with value enables condition")
			void set_lessThanWithValue_enablesCondition() {
				boolean changed = condition.set().lessThan(TEST_VALUE_1);

				assertTrue(changed);
				assertTrue(condition.enabled().get());
			}

			@Test
			@DisplayName("lessThan with null disables condition")
			void set_lessThanWithNull_disablesCondition() {
				// First set a non-null value to ensure state change
				condition.set().lessThan(TEST_VALUE_1);

				boolean changed = condition.set().lessThan(null);

				assertTrue(changed);
				assertFalse(condition.enabled().get());
			}

			@Test
			@DisplayName("lessThanOrEqualTo with value enables condition")
			void set_lessThanOrEqualToWithValue_enablesCondition() {
				boolean changed = condition.set().lessThanOrEqualTo(TEST_VALUE_1);

				assertTrue(changed);
				assertTrue(condition.enabled().get());
			}

			@Test
			@DisplayName("lessThanOrEqualTo with null disables condition")
			void set_lessThanOrEqualToWithNull_disablesCondition() {
				// First set a non-null value to ensure state change
				condition.set().lessThanOrEqualTo(TEST_VALUE_1);

				boolean changed = condition.set().lessThanOrEqualTo(null);

				assertTrue(changed);
				assertFalse(condition.enabled().get());
			}
		}

		@Nested
		@DisplayName("Collection operations")
		class CollectionOperationsTest {

			@Test
			@DisplayName("in with values enables condition")
			void set_inWithValues_enablesCondition() {
				boolean changed = condition.set().in(TEST_VALUE_1, TEST_VALUE_2);

				assertTrue(changed);
				assertTrue(condition.enabled().get());
			}

			@Test
			@DisplayName("in with empty list disables condition")
			void set_inWithEmptyList_disablesCondition() {
				boolean changed = condition.set().in(emptyList());

				assertTrue(changed);
				assertFalse(condition.enabled().get());
			}

			@Test
			@DisplayName("notIn with values enables condition")
			void set_notInWithValues_enablesCondition() {
				boolean changed = condition.set().notIn(TEST_VALUE_1, TEST_VALUE_2);

				assertTrue(changed);
				assertTrue(condition.enabled().get());
			}

			@Test
			@DisplayName("notIn with empty list disables condition")
			void set_notInWithEmptyList_disablesCondition() {
				boolean changed = condition.set().notIn(emptyList());

				assertTrue(changed);
				assertFalse(condition.enabled().get());
			}
		}

		@Nested
		@DisplayName("Range operations")
		class RangeOperationsTest {

			@Test
			@DisplayName("betweenExclusive with both values enables condition")
			void set_betweenExclusiveWithBothValues_enablesCondition() {
				boolean changed = condition.set().betweenExclusive(TEST_VALUE_1, TEST_VALUE_2);

				assertTrue(changed);
				assertTrue(condition.enabled().get());
			}

			@Test
			@DisplayName("betweenExclusive with lower null disables condition")
			void set_betweenExclusiveWithLowerNull_disablesCondition() {
				// First set valid range to ensure state change
				condition.set().betweenExclusive(TEST_VALUE_1, TEST_VALUE_2);

				boolean changed = condition.set().betweenExclusive(TEST_VALUE_1, null);

				assertTrue(changed);
				assertFalse(condition.enabled().get());
			}

			@Test
			@DisplayName("betweenExclusive with upper null disables condition")
			void set_betweenExclusiveWithUpperNull_disablesCondition() {
				// First set valid range to ensure state change
				condition.set().betweenExclusive(TEST_VALUE_1, TEST_VALUE_2);

				boolean changed = condition.set().betweenExclusive(null, TEST_VALUE_2);

				assertTrue(changed);
				assertFalse(condition.enabled().get());
			}

			@Test
			@DisplayName("betweenExclusive with both null disables condition")
			void set_betweenExclusiveWithBothNull_disablesCondition() {
				// First set valid range to ensure state change
				condition.set().betweenExclusive(TEST_VALUE_1, TEST_VALUE_2);

				boolean changed = condition.set().betweenExclusive(null, null);

				assertTrue(changed);
				assertFalse(condition.enabled().get());
			}

			@Test
			@DisplayName("notBetweenExclusive with both values enables condition")
			void set_notBetweenExclusiveWithBothValues_enablesCondition() {
				boolean changed = condition.set().notBetweenExclusive(TEST_VALUE_1, TEST_VALUE_2);

				assertTrue(changed);
				assertTrue(condition.enabled().get());
			}

			@Test
			@DisplayName("notBetweenExclusive with lower null disables condition")
			void set_notBetweenExclusiveWithLowerNull_disablesCondition() {
				// First set valid range to ensure state change
				condition.set().notBetweenExclusive(TEST_VALUE_1, TEST_VALUE_2);

				boolean changed = condition.set().notBetweenExclusive(TEST_VALUE_1, null);

				assertTrue(changed);
				assertFalse(condition.enabled().get());
			}

			@Test
			@DisplayName("notBetweenExclusive with upper null disables condition")
			void set_notBetweenExclusiveWithUpperNull_disablesCondition() {
				// First set valid range to ensure state change
				condition.set().notBetweenExclusive(TEST_VALUE_1, TEST_VALUE_2);

				boolean changed = condition.set().notBetweenExclusive(null, TEST_VALUE_2);

				assertTrue(changed);
				assertFalse(condition.enabled().get());
			}

			@Test
			@DisplayName("notBetweenExclusive with both null disables condition")
			void set_notBetweenExclusiveWithBothNull_disablesCondition() {
				// First set valid range to ensure state change
				condition.set().notBetweenExclusive(TEST_VALUE_1, TEST_VALUE_2);

				boolean changed = condition.set().notBetweenExclusive(null, null);

				assertTrue(changed);
				assertFalse(condition.enabled().get());
			}

			@Test
			@DisplayName("between with both values enables condition")
			void set_betweenWithBothValues_enablesCondition() {
				boolean changed = condition.set().between(TEST_VALUE_1, TEST_VALUE_2);

				assertTrue(changed);
				assertTrue(condition.enabled().get());
			}

			@Test
			@DisplayName("between with lower null disables condition")
			void set_betweenWithLowerNull_disablesCondition() {
				// First set valid range to ensure state change
				condition.set().between(TEST_VALUE_1, TEST_VALUE_2);

				boolean changed = condition.set().between(TEST_VALUE_1, null);

				assertTrue(changed);
				assertFalse(condition.enabled().get());
			}

			@Test
			@DisplayName("between with upper null disables condition")
			void set_betweenWithUpperNull_disablesCondition() {
				// First set valid range to ensure state change
				condition.set().between(TEST_VALUE_1, TEST_VALUE_2);

				boolean changed = condition.set().between(null, TEST_VALUE_2);

				assertTrue(changed);
				assertFalse(condition.enabled().get());
			}

			@Test
			@DisplayName("between with both null disables condition")
			void set_betweenWithBothNull_disablesCondition() {
				// First set valid range to ensure state change
				condition.set().between(TEST_VALUE_1, TEST_VALUE_2);

				boolean changed = condition.set().between(null, null);

				assertTrue(changed);
				assertFalse(condition.enabled().get());
			}

			@Test
			@DisplayName("notBetween with both values enables condition")
			void set_notBetweenWithBothValues_enablesCondition() {
				boolean changed = condition.set().notBetween(TEST_VALUE_1, TEST_VALUE_2);

				assertTrue(changed);
				assertTrue(condition.enabled().get());
			}

			@Test
			@DisplayName("notBetween with lower null disables condition")
			void set_notBetweenWithLowerNull_disablesCondition() {
				// First set valid range to ensure state change
				condition.set().notBetween(TEST_VALUE_1, TEST_VALUE_2);

				boolean changed = condition.set().notBetween(TEST_VALUE_1, null);

				assertTrue(changed);
				assertFalse(condition.enabled().get());
			}

			@Test
			@DisplayName("notBetween with upper null disables condition")
			void set_notBetweenWithUpperNull_disablesCondition() {
				// First set valid range to ensure state change
				condition.set().notBetween(TEST_VALUE_1, TEST_VALUE_2);

				boolean changed = condition.set().notBetween(null, TEST_VALUE_2);

				assertTrue(changed);
				assertFalse(condition.enabled().get());
			}

			@Test
			@DisplayName("notBetween with both null disables condition")
			void set_notBetweenWithBothNull_disablesCondition() {
				// First set valid range to ensure state change
				condition.set().notBetween(TEST_VALUE_1, TEST_VALUE_2);

				boolean changed = condition.set().notBetween(null, null);

				assertTrue(changed);
				assertFalse(condition.enabled().get());
			}
		}
	}

	@Test
	void clearNonNull() {
		ConditionModel<Boolean> conditionModel = ConditionModel.builder()
						.valueClass(Boolean.class)
						.operands(new Operands<Boolean>() {
							@Override
							public Value<Boolean> equal() {
								return Value.nonNull(false);
							}
						})
						.build();
		conditionModel.operands().equal().set(true);
		assertTrue(conditionModel.enabled().get());
		conditionModel.clear();
		assertFalse(conditionModel.enabled().get());
	}
}

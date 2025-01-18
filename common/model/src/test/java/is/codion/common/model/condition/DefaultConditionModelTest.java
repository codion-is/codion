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
		ConditionModel<String> model = ConditionModel.builder(String.class)
						.autoEnable(false)
						.build();
		model.caseSensitive().set(false);
		model.wildcard().set(Wildcard.NONE);

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

		model.wildcard().set(Wildcard.PREFIX_AND_POSTFIX);
		assertEquals("%test%", operands.equal().get());

		model.wildcard().set(Wildcard.PREFIX);
		assertEquals("%test", operands.equal().get());

		model.wildcard().set(Wildcard.POSTFIX);
		assertEquals("test%", operands.equal().get());

		model.wildcard().set(Wildcard.NONE);
		assertEquals("test", operands.equal().get());

		model.clear();

		operands.equal().removeConsumer(equalConsumer);
		operands.in().removeConsumer(inConsumer);
		operands.upper().removeConsumer(upperConsumer);
		operands.lower().removeConsumer(lowerConsumer);
		model.changed().removeListener(conditionChangedListener);
	}

	@Test
	void testMisc() {
		ConditionModel<String> model = ConditionModel.builder(String.class).build();
		model.wildcard().set(Wildcard.PREFIX_AND_POSTFIX);
		model.set().equalTo("upper");
		assertEquals("%upper%", model.operands().equal().get());

		assertThrows(NullPointerException.class, () -> model.set().in((Collection<String>) null));
		assertThrows(NullPointerException.class, () -> model.set().in("test", null));
	}

	@Test
	void testOperator() {
		assertThrows(IllegalArgumentException.class, () -> ConditionModel.builder(String.class)
						.operators(Arrays.asList(Operator.EQUAL, Operator.NOT_BETWEEN))
						.operator(Operator.IN));
		assertThrows(IllegalArgumentException.class, () -> ConditionModel.builder(String.class)
						.operator(Operator.IN)
						.operators(Arrays.asList(Operator.EQUAL, Operator.NOT_BETWEEN)));

		ConditionModel<String> model = ConditionModel.builder(String.class)
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
		ConditionModel<String> model = ConditionModel.builder(String.class).build();
		assertTrue(model.autoEnable().get());
		model.operands().equal().set("test");
		assertTrue(model.enabled().get());
		model.caseSensitive().set(false);
		assertFalse(model.caseSensitive().get());
		assertEquals(String.class, model.valueClass());

		model.wildcard().set(Wildcard.PREFIX_AND_POSTFIX);
		assertEquals(Wildcard.PREFIX_AND_POSTFIX, model.wildcard().get());

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
	void setUpperLocked() {
		ConditionModel<String> model = ConditionModel.builder(String.class).build();
		model.locked().set(true);
		assertThrows(IllegalStateException.class, () -> model.operands().upper().set("test"));
	}

	@Test
	void setLowerLocked() {
		ConditionModel<String> model = ConditionModel.builder(String.class).build();
		model.locked().set(true);
		assertThrows(IllegalStateException.class, () -> model.operands().lower().set("test"));
	}

	@Test
	void setEqualOperandLocked() {
		ConditionModel<String> model = ConditionModel.builder(String.class).build();
		model.locked().set(true);
		assertThrows(IllegalStateException.class, () -> model.operands().equal().set("test"));
	}

	@Test
	void setInOperandsLocked() {
		ConditionModel<String> model = ConditionModel.builder(String.class).build();
		model.locked().set(true);
		assertThrows(IllegalStateException.class, () -> model.operands().in().set(Collections.singletonList("test")));
	}

	@Test
	void setEnabledLocked() {
		ConditionModel<String> model = ConditionModel.builder(String.class).build();
		model.locked().set(true);
		assertThrows(IllegalStateException.class, () -> model.enabled().set(true));
	}

	@Test
	void setOperatorLocked() {
		ConditionModel<String> model = ConditionModel.builder(String.class).build();
		model.locked().set(true);
		assertThrows(IllegalStateException.class, () -> model.operator().set(Operator.NOT_EQUAL));
	}

	@Test
	void multiConditionString() {
		ConditionModel<String> condition = ConditionModel.builder(String.class).build();
		condition.caseSensitive().set(false);
		condition.wildcard().set(Wildcard.NONE);

		Collection<String> strings = asList("abc", "def");
		condition.operands().in().set(strings);

		assertTrue(condition.operands().in().get().containsAll(strings));
	}

	@Test
	void autoEnable() {
		ConditionModel<Integer> condition = ConditionModel.builder(Integer.class).build();

		condition.operator().set(Operator.EQUAL);
		assertFalse(condition.enabled().get());
		Operands<Integer> operands = condition.operands();
		operands.equal().set(1);
		assertTrue(condition.enabled().get());
		operands.equal().set(null);
		assertFalse(condition.enabled().get());
		condition.enabled().set(false);
		operands.upper().set(1);
		operands.lower().set(1);
		assertFalse(condition.enabled().get());
		operands.upper().set(null);
		operands.lower().set(null);

		condition.operator().set(Operator.NOT_EQUAL);
		assertFalse(condition.enabled().get());
		operands.equal().set(1);
		assertTrue(condition.enabled().get());
		operands.equal().set(null);
		assertFalse(condition.enabled().get());
		condition.enabled().set(false);
		operands.upper().set(1);
		operands.lower().set(1);
		assertFalse(condition.enabled().get());
		operands.upper().set(null);
		operands.lower().set(null);

		condition.operator().set(Operator.LESS_THAN);
		assertFalse(condition.enabled().get());
		operands.upper().set(1);
		assertTrue(condition.enabled().get());
		operands.upper().set(null);
		assertFalse(condition.enabled().get());
		operands.lower().set(1);
		assertFalse(condition.enabled().get());
		operands.lower().set(null);

		condition.operator().set(Operator.LESS_THAN_OR_EQUAL);
		assertFalse(condition.enabled().get());
		operands.upper().set(1);
		assertTrue(condition.enabled().get());
		operands.upper().set(null);
		assertFalse(condition.enabled().get());
		operands.lower().set(1);
		assertFalse(condition.enabled().get());
		operands.lower().set(null);

		condition.operator().set(Operator.GREATER_THAN);
		assertFalse(condition.enabled().get());
		operands.lower().set(1);
		assertTrue(condition.enabled().get());
		operands.lower().set(null);
		assertFalse(condition.enabled().get());
		operands.upper().set(1);
		assertFalse(condition.enabled().get());
		operands.upper().set(null);

		condition.operator().set(Operator.GREATER_THAN_OR_EQUAL);
		assertFalse(condition.enabled().get());
		operands.lower().set(1);
		assertTrue(condition.enabled().get());
		operands.lower().set(null);
		assertFalse(condition.enabled().get());
		operands.upper().set(1);
		assertFalse(condition.enabled().get());
		operands.upper().set(null);

		condition.operator().set(Operator.BETWEEN);
		assertFalse(condition.enabled().get());
		operands.equal().set(1);
		assertFalse(condition.enabled().get());
		operands.equal().set(null);
		operands.lower().set(1);
		assertFalse(condition.enabled().get());
		operands.upper().set(1);
		assertTrue(condition.enabled().get());
		operands.lower().set(null);
		assertFalse(condition.enabled().get());
		operands.lower().set(1);
		assertTrue(condition.enabled().get());
		operands.lower().set(null);
		operands.upper().set(null);

		condition.operator().set(Operator.BETWEEN_EXCLUSIVE);
		assertFalse(condition.enabled().get());
		operands.equal().set(1);
		assertFalse(condition.enabled().get());
		operands.equal().set(null);
		operands.lower().set(1);
		assertFalse(condition.enabled().get());
		operands.upper().set(1);
		assertTrue(condition.enabled().get());
		operands.lower().set(null);
		assertFalse(condition.enabled().get());
		operands.lower().set(1);
		assertTrue(condition.enabled().get());
		operands.lower().set(null);
		operands.upper().set(null);

		condition.operator().set(Operator.NOT_BETWEEN);
		assertFalse(condition.enabled().get());
		operands.equal().set(1);
		assertFalse(condition.enabled().get());
		operands.equal().set(null);
		operands.lower().set(1);
		assertFalse(condition.enabled().get());
		operands.upper().set(1);
		assertTrue(condition.enabled().get());
		operands.lower().set(null);
		assertFalse(condition.enabled().get());
		operands.lower().set(1);
		assertTrue(condition.enabled().get());
		operands.lower().set(null);
		operands.upper().set(null);

		condition.operator().set(Operator.NOT_BETWEEN_EXCLUSIVE);
		assertFalse(condition.enabled().get());
		operands.equal().set(1);
		assertFalse(condition.enabled().get());
		operands.equal().set(null);
		operands.lower().set(1);
		assertFalse(condition.enabled().get());
		operands.upper().set(1);
		assertTrue(condition.enabled().get());
		operands.lower().set(null);
		assertFalse(condition.enabled().get());
		operands.lower().set(1);
		assertTrue(condition.enabled().get());
		operands.lower().set(null);
		operands.upper().set(null);
	}

	@Test
	void noOperators() {
		assertThrows(IllegalArgumentException.class, () -> ConditionModel.builder(String.class)
						.operators(emptyList()));
	}

	@Test
	void includeInteger() {
		ConditionModel<Integer> condition = ConditionModel.builder(Integer.class)
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
		ConditionModel<String> condition = ConditionModel.builder(String.class)
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
		condition.wildcard().set(Wildcard.NONE);
		assertTrue(condition.accepts("HELl"));
		assertTrue(condition.accepts("hElL"));
		condition.operator().set(Operator.NOT_EQUAL);
		assertFalse(condition.accepts("HELl"));
		assertFalse(condition.accepts("hElL"));
	}

	@Test
	void acceptCharacter() {
		ConditionModel<Character> condition = ConditionModel.builder(Character.class).build();
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

	@Test
	void set() {
		ConditionModel<Integer> condition = ConditionModel.builder(Integer.class)
						.autoEnable(false)
						.build();

		//is null
		boolean changed = condition.set().isNull();
		assertFalse(changed);
		assertTrue(condition.enabled().get());

		//is not null
		changed = condition.set().isNotNull();
		assertTrue(changed);
		assertTrue(condition.enabled().get());

		//equal
		changed = condition.set().equalTo(5);
		assertTrue(changed);
		assertTrue(condition.enabled().get());

		changed = condition.set().equalTo(null);
		assertTrue(changed);
		assertFalse(condition.enabled().get());

		// not equal
		changed = condition.set().notEqualTo(5);
		assertTrue(changed);
		assertTrue(condition.enabled().get());

		changed = condition.set().notEqualTo(null);
		assertTrue(changed);
		assertFalse(condition.enabled().get());

		//greater than
		changed = condition.set().greaterThan(5);
		assertTrue(changed);
		assertTrue(condition.enabled().get());

		changed = condition.set().greaterThan(null);
		assertTrue(changed);
		assertFalse(condition.enabled().get());

		//greater than or equal
		changed = condition.set().greaterThanOrEqualTo(5);
		assertTrue(changed);
		assertTrue(condition.enabled().get());

		changed = condition.set().greaterThanOrEqualTo(null);
		assertTrue(changed);
		assertFalse(condition.enabled().get());

		//less than
		changed = condition.set().lessThan(5);
		assertTrue(changed);
		assertTrue(condition.enabled().get());

		changed = condition.set().lessThan(null);
		assertTrue(changed);
		assertFalse(condition.enabled().get());

		//less than or equal
		changed = condition.set().lessThanOrEqualTo(5);
		assertTrue(changed);
		assertTrue(condition.enabled().get());

		changed = condition.set().lessThanOrEqualTo(null);
		assertTrue(changed);
		assertFalse(condition.enabled().get());

		//in
		changed = condition.set().in(5, 6);
		assertTrue(changed);
		assertTrue(condition.enabled().get());

		changed = condition.set().in(emptyList());
		assertTrue(changed);
		assertFalse(condition.enabled().get());

		//not in
		changed = condition.set().notIn(5, 6);
		assertTrue(changed);
		assertTrue(condition.enabled().get());

		changed = condition.set().notIn(emptyList());
		assertTrue(changed);
		assertFalse(condition.enabled().get());

		//between exclusive
		changed = condition.set().betweenExclusive(5, 6);
		assertTrue(changed);
		assertTrue(condition.enabled().get());

		changed = condition.set().betweenExclusive(5, null);
		assertTrue(changed);
		assertFalse(condition.enabled().get());

		changed = condition.set().betweenExclusive(null, 6);
		assertTrue(changed);
		assertFalse(condition.enabled().get());

		changed = condition.set().betweenExclusive(null, null);
		assertTrue(changed);
		assertFalse(condition.enabled().get());

		//not between exclusive
		changed = condition.set().notBetweenExclusive(5, 6);
		assertTrue(changed);
		assertTrue(condition.enabled().get());

		changed = condition.set().notBetweenExclusive(5, null);
		assertTrue(changed);
		assertFalse(condition.enabled().get());

		changed = condition.set().notBetweenExclusive(null, 6);
		assertTrue(changed);
		assertFalse(condition.enabled().get());

		changed = condition.set().notBetweenExclusive(null, null);
		assertTrue(changed);
		assertFalse(condition.enabled().get());

		//between
		changed = condition.set().between(5, 6);
		assertTrue(changed);
		assertTrue(condition.enabled().get());

		changed = condition.set().between(5, null);
		assertTrue(changed);
		assertFalse(condition.enabled().get());

		changed = condition.set().between(null, 6);
		assertTrue(changed);
		assertFalse(condition.enabled().get());

		changed = condition.set().between(null, null);
		assertTrue(changed);
		assertFalse(condition.enabled().get());

		//not between
		changed = condition.set().notBetween(5, 6);
		assertTrue(changed);
		assertTrue(condition.enabled().get());

		changed = condition.set().notBetween(5, null);
		assertTrue(changed);
		assertFalse(condition.enabled().get());

		changed = condition.set().notBetween(null, 6);
		assertTrue(changed);
		assertFalse(condition.enabled().get());

		changed = condition.set().notBetween(null, null);
		assertTrue(changed);
		assertFalse(condition.enabled().get());
	}
}

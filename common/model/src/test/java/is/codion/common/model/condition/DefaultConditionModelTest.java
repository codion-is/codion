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
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.model.condition;

import is.codion.common.Operator;
import is.codion.common.model.condition.ConditionModel.AutomaticWildcard;
import is.codion.common.model.condition.ConditionModel.Operands;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultConditionModelTest {

	final AtomicInteger equalCounter = new AtomicInteger();
	final AtomicInteger inCounter = new AtomicInteger();
	final AtomicInteger upperBoundCounter = new AtomicInteger();
	final AtomicInteger lowerBoundCounter = new AtomicInteger();
	final AtomicInteger conditionChangedCounter = new AtomicInteger();
	final AtomicInteger operatorCounter = new AtomicInteger();
	final AtomicInteger enabledCounter = new AtomicInteger();

	final Consumer<String> equalConsumer = value -> equalCounter.incrementAndGet();
	final Consumer<Set<String>> inConsumer = value -> inCounter.incrementAndGet();
	final Consumer<String> upperBoundConsumer = value -> upperBoundCounter.incrementAndGet();
	final Consumer<String> lowerBoundConsumer = value -> lowerBoundCounter.incrementAndGet();
	final Runnable conditionChangedListener = conditionChangedCounter::incrementAndGet;
	final Consumer<Operator> operatorConsumer = data -> operatorCounter.incrementAndGet();
	final Runnable enabledListener = enabledCounter::incrementAndGet;

	@Test
	void testOperands() {
		ConditionModel<String, String> model = ConditionModel.builder("test", String.class).build();
		model.caseSensitive().set(false);
		model.automaticWildcard().set(AutomaticWildcard.NONE);

		model.autoEnable().set(false);
		Operands<String> operands = model.operands();
		operands.equal().addConsumer(equalConsumer);
		operands.in().addConsumer(inConsumer);
		operands.upperBound().addConsumer(upperBoundConsumer);
		operands.lowerBound().addConsumer(lowerBoundConsumer);
		model.changed().addListener(conditionChangedListener);

		operands.upperBound().set("hello");
		assertEquals(1, conditionChangedCounter.get());
		assertFalse(model.enabled().get());
		assertEquals(1, upperBoundCounter.get());
		assertEquals("hello", operands.upperBound().get());
		operands.lowerBound().set("hello");
		assertEquals(2, conditionChangedCounter.get());
		assertEquals(1, lowerBoundCounter.get());
		assertEquals("hello", operands.lowerBound().get());

		operands.equal().set("test");
		assertEquals(1, equalCounter.get());
		assertEquals("test", operands.equal().get());

		model.automaticWildcard().set(AutomaticWildcard.PREFIX_AND_POSTFIX);
		assertEquals("%test%", operands.equal().get());

		model.automaticWildcard().set(AutomaticWildcard.PREFIX);
		assertEquals("%test", operands.equal().get());

		model.automaticWildcard().set(AutomaticWildcard.POSTFIX);
		assertEquals("test%", operands.equal().get());

		model.automaticWildcard().set(AutomaticWildcard.NONE);
		assertEquals("test", operands.equal().get());

		model.clear();

		operands.equal().removeConsumer(equalConsumer);
		operands.in().removeConsumer(inConsumer);
		operands.upperBound().removeConsumer(upperBoundConsumer);
		operands.lowerBound().removeConsumer(lowerBoundConsumer);
		model.changed().removeListener(conditionChangedListener);
	}

	@Test
	void testMisc() {
		ConditionModel<String, String> model = ConditionModel.builder("test", String.class).build();
		assertEquals("test", model.identifier());

		model.operator().set(Operator.EQUAL);
		model.automaticWildcard().set(AutomaticWildcard.PREFIX_AND_POSTFIX);
		model.operands().equal().set("upper");
		assertEquals("%upper%", model.operands().equal().get());
	}

	@Test
	void testOperator() {
		assertThrows(IllegalArgumentException.class, () -> ConditionModel.builder("test", String.class)
						.operators(Arrays.asList(Operator.EQUAL, Operator.NOT_BETWEEN))
						.operator(Operator.IN));
		assertThrows(IllegalArgumentException.class, () -> ConditionModel.builder("test", String.class)
						.operator(Operator.IN)
						.operators(Arrays.asList(Operator.EQUAL, Operator.NOT_BETWEEN)));

		ConditionModel<String, String> model = ConditionModel.builder("test", String.class)
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
		ConditionModel<String, String> model = ConditionModel.builder("test", String.class).build();
		assertTrue(model.autoEnable().get());
		model.operands().equal().set("test");
		assertTrue(model.enabled().get());
		model.caseSensitive().set(false);
		assertFalse(model.caseSensitive().get());
		assertEquals("test", model.identifier());
		assertEquals(String.class, model.valueClass());

		model.automaticWildcard().set(AutomaticWildcard.PREFIX_AND_POSTFIX);
		assertEquals(AutomaticWildcard.PREFIX_AND_POSTFIX, model.automaticWildcard().get());

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
	void setUpperBoundLocked() {
		ConditionModel<String, String> model = ConditionModel.builder("test", String.class).build();
		model.locked().set(true);
		assertThrows(IllegalStateException.class, () -> model.operands().upperBound().set("test"));
	}

	@Test
	void setLowerBoundLocked() {
		ConditionModel<String, String> model = ConditionModel.builder("test", String.class).build();
		model.locked().set(true);
		assertThrows(IllegalStateException.class, () -> model.operands().lowerBound().set("test"));
	}

	@Test
	void setEqualOperandLocked() {
		ConditionModel<String, String> model = ConditionModel.builder("test", String.class).build();
		model.locked().set(true);
		assertThrows(IllegalStateException.class, () -> model.operands().equal().set("test"));
	}

	@Test
	void setInOperandsLocked() {
		ConditionModel<String, String> model = ConditionModel.builder("test", String.class).build();
		model.locked().set(true);
		assertThrows(IllegalStateException.class, () -> model.operands().in().set(Collections.singletonList("test")));
	}

	@Test
	void setEnabledLocked() {
		ConditionModel<String, String> model = ConditionModel.builder("test", String.class).build();
		model.locked().set(true);
		assertThrows(IllegalStateException.class, () -> model.enabled().set(true));
	}

	@Test
	void setOperatorLocked() {
		ConditionModel<String, String> model = ConditionModel.builder("test", String.class).build();
		model.locked().set(true);
		assertThrows(IllegalStateException.class, () -> model.operator().set(Operator.NOT_EQUAL));
		assertThrows(IllegalStateException.class, () -> model.operator().set(Operator.NOT_EQUAL));
	}

	@Test
	void multiConditionString() {
		ConditionModel<String, String> condition = ConditionModel.builder("test", String.class).build();
		condition.caseSensitive().set(false);
		condition.automaticWildcard().set(AutomaticWildcard.NONE);

		Collection<String> strings = asList("abc", "def");
		condition.operands().in().set(strings);

		assertTrue(condition.operands().in().get().containsAll(strings));
	}

	@Test
	void autoEnable() {
		ConditionModel<String, Integer> condition = ConditionModel.builder("test", Integer.class).build();

		condition.operator().set(Operator.EQUAL);
		assertFalse(condition.enabled().get());
		Operands<Integer> operands = condition.operands();
		operands.equal().set(1);
		assertTrue(condition.enabled().get());
		operands.equal().set(null);
		assertFalse(condition.enabled().get());
		condition.enabled().set(false);
		operands.upperBound().set(1);
		operands.lowerBound().set(1);
		assertFalse(condition.enabled().get());
		operands.upperBound().set(null);
		operands.lowerBound().set(null);

		condition.operator().set(Operator.NOT_EQUAL);
		assertFalse(condition.enabled().get());
		operands.equal().set(1);
		assertTrue(condition.enabled().get());
		operands.equal().set(null);
		assertFalse(condition.enabled().get());
		condition.enabled().set(false);
		operands.upperBound().set(1);
		operands.lowerBound().set(1);
		assertFalse(condition.enabled().get());
		operands.upperBound().set(null);
		operands.lowerBound().set(null);

		condition.operator().set(Operator.LESS_THAN);
		assertFalse(condition.enabled().get());
		operands.upperBound().set(1);
		assertTrue(condition.enabled().get());
		operands.upperBound().set(null);
		assertFalse(condition.enabled().get());
		operands.lowerBound().set(1);
		assertFalse(condition.enabled().get());
		operands.lowerBound().set(null);

		condition.operator().set(Operator.LESS_THAN_OR_EQUAL);
		assertFalse(condition.enabled().get());
		operands.upperBound().set(1);
		assertTrue(condition.enabled().get());
		operands.upperBound().set(null);
		assertFalse(condition.enabled().get());
		operands.lowerBound().set(1);
		assertFalse(condition.enabled().get());
		operands.lowerBound().set(null);

		condition.operator().set(Operator.GREATER_THAN);
		assertFalse(condition.enabled().get());
		operands.lowerBound().set(1);
		assertTrue(condition.enabled().get());
		operands.lowerBound().set(null);
		assertFalse(condition.enabled().get());
		operands.upperBound().set(1);
		assertFalse(condition.enabled().get());
		operands.upperBound().set(null);

		condition.operator().set(Operator.GREATER_THAN_OR_EQUAL);
		assertFalse(condition.enabled().get());
		operands.lowerBound().set(1);
		assertTrue(condition.enabled().get());
		operands.lowerBound().set(null);
		assertFalse(condition.enabled().get());
		operands.upperBound().set(1);
		assertFalse(condition.enabled().get());
		operands.upperBound().set(null);

		condition.operator().set(Operator.BETWEEN);
		assertFalse(condition.enabled().get());
		operands.equal().set(1);
		assertFalse(condition.enabled().get());
		operands.equal().set(null);
		operands.lowerBound().set(1);
		assertFalse(condition.enabled().get());
		operands.upperBound().set(1);
		assertTrue(condition.enabled().get());
		operands.lowerBound().set(null);
		assertFalse(condition.enabled().get());
		operands.lowerBound().set(1);
		assertTrue(condition.enabled().get());
		operands.lowerBound().set(null);
		operands.upperBound().set(null);

		condition.operator().set(Operator.BETWEEN_EXCLUSIVE);
		assertFalse(condition.enabled().get());
		operands.equal().set(1);
		assertFalse(condition.enabled().get());
		operands.equal().set(null);
		operands.lowerBound().set(1);
		assertFalse(condition.enabled().get());
		operands.upperBound().set(1);
		assertTrue(condition.enabled().get());
		operands.lowerBound().set(null);
		assertFalse(condition.enabled().get());
		operands.lowerBound().set(1);
		assertTrue(condition.enabled().get());
		operands.lowerBound().set(null);
		operands.upperBound().set(null);

		condition.operator().set(Operator.NOT_BETWEEN);
		assertFalse(condition.enabled().get());
		operands.equal().set(1);
		assertFalse(condition.enabled().get());
		operands.equal().set(null);
		operands.lowerBound().set(1);
		assertFalse(condition.enabled().get());
		operands.upperBound().set(1);
		assertTrue(condition.enabled().get());
		operands.lowerBound().set(null);
		assertFalse(condition.enabled().get());
		operands.lowerBound().set(1);
		assertTrue(condition.enabled().get());
		operands.lowerBound().set(null);
		operands.upperBound().set(null);

		condition.operator().set(Operator.NOT_BETWEEN_EXCLUSIVE);
		assertFalse(condition.enabled().get());
		operands.equal().set(1);
		assertFalse(condition.enabled().get());
		operands.equal().set(null);
		operands.lowerBound().set(1);
		assertFalse(condition.enabled().get());
		operands.upperBound().set(1);
		assertTrue(condition.enabled().get());
		operands.lowerBound().set(null);
		assertFalse(condition.enabled().get());
		operands.lowerBound().set(1);
		assertTrue(condition.enabled().get());
		operands.lowerBound().set(null);
		operands.upperBound().set(null);
	}

	@Test
	void noOperators() {
		assertThrows(IllegalArgumentException.class, () -> ConditionModel.builder("test", String.class)
						.operators(Collections.emptyList()));
	}

	@Test
	void includeInteger() {
		ConditionModel<String, Integer> condition = ConditionModel.builder("test", Integer.class).build();
		condition.autoEnable().set(false);
		condition.enabled().set(true);
		condition.operator().set(Operator.EQUAL);

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

		operands.lowerBound().set(10);
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

		operands.upperBound().set(10);
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

		operands.lowerBound().set(6);
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

		operands.upperBound().set(null);
		operands.lowerBound().set(null);
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
		ConditionModel<String, String> condition = ConditionModel.builder("test", String.class).build();
		condition.autoEnable().set(false);
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
		condition.automaticWildcard().set(AutomaticWildcard.NONE);
		assertTrue(condition.accepts("HELl"));
		assertTrue(condition.accepts("hElL"));
		condition.operator().set(Operator.NOT_EQUAL);
		assertFalse(condition.accepts("HELl"));
		assertFalse(condition.accepts("hElL"));
	}

	@Test
	void acceptCharacter() {
		ConditionModel<String, Character> condition = ConditionModel.builder("test", Character.class).build();
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
}

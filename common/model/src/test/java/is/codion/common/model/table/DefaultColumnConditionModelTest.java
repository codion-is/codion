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
package is.codion.common.model.table;

import is.codion.common.Operator;
import is.codion.common.model.table.ColumnConditionModel.AutomaticWildcard;
import is.codion.common.model.table.ColumnConditionModel.Operand;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultColumnConditionModelTest {

	final AtomicInteger equalToCounter = new AtomicInteger();
	final AtomicInteger inCounter = new AtomicInteger();
	final AtomicInteger upperBoundCounter = new AtomicInteger();
	final AtomicInteger lowerBoundCounter = new AtomicInteger();
	final AtomicInteger conditionChangedCounter = new AtomicInteger();
	final AtomicInteger operatorCounter = new AtomicInteger();
	final AtomicInteger enabledCounter = new AtomicInteger();

	final Consumer<String> equalToConsumer = value -> equalToCounter.incrementAndGet();
	final Consumer<Set<String>> inConsumer = value -> inCounter.incrementAndGet();
	final Consumer<String> upperBoundConsumer = value -> upperBoundCounter.incrementAndGet();
	final Consumer<String> lowerBoundConsumer = value -> lowerBoundCounter.incrementAndGet();
	final Runnable conditionChangedListener = conditionChangedCounter::incrementAndGet;
	final Consumer<Operator> operatorConsumer = data -> operatorCounter.incrementAndGet();
	final Runnable enabledListener = enabledCounter::incrementAndGet;

	@Test
	void testSetBounds() {
		ColumnConditionModel<String, String> model = ColumnConditionModel.builder("test", String.class).build();
		model.caseSensitive().set(false);
		model.automaticWildcard().set(AutomaticWildcard.NONE);

		model.autoEnable().set(false);
		Operand<String> operand = model.operand();
		operand.equal().addConsumer(equalToConsumer);
		operand.in().addConsumer(inConsumer);
		operand.upperBound().addConsumer(upperBoundConsumer);
		operand.lowerBound().addConsumer(lowerBoundConsumer);
		model.conditionChangedEvent().addListener(conditionChangedListener);

		operand.upperBound().set("hello");
		assertEquals(1, conditionChangedCounter.get());
		assertFalse(model.enabled().get());
		assertEquals(1, upperBoundCounter.get());
		assertEquals("hello", operand.upperBound().get());
		operand.lowerBound().set("hello");
		assertEquals(2, conditionChangedCounter.get());
		assertEquals(1, lowerBoundCounter.get());
		assertEquals("hello", operand.lowerBound().get());

		operand.equal().set("test");
		assertEquals(1, equalToCounter.get());
		assertEquals("test", operand.equal().get());

		model.automaticWildcard().set(AutomaticWildcard.PREFIX_AND_POSTFIX);
		assertEquals("%test%", operand.equal().get());

		model.automaticWildcard().set(AutomaticWildcard.PREFIX);
		assertEquals("%test", operand.equal().get());

		model.automaticWildcard().set(AutomaticWildcard.POSTFIX);
		assertEquals("test%", operand.equal().get());

		model.automaticWildcard().set(AutomaticWildcard.NONE);
		assertEquals("test", operand.equal().get());

		model.clear();

		operand.equal().removeConsumer(equalToConsumer);
		operand.in().removeConsumer(inConsumer);
		operand.upperBound().removeConsumer(upperBoundConsumer);
		operand.lowerBound().removeConsumer(lowerBoundConsumer);
		model.conditionChangedEvent().removeListener(conditionChangedListener);
	}

	@Test
	void testMisc() {
		ColumnConditionModel<String, String> model = ColumnConditionModel.builder("test", String.class).build();
		assertEquals("test", model.identifier());

		model.operator().set(Operator.EQUAL);
		model.automaticWildcard().set(AutomaticWildcard.PREFIX_AND_POSTFIX);
		model.operand().equal().set("upper");
		assertEquals("%upper%", model.operand().equal().get());
	}

	@Test
	void testOperator() {
		assertThrows(IllegalArgumentException.class, () -> ColumnConditionModel.builder("test", String.class)
						.operators(Arrays.asList(Operator.EQUAL, Operator.NOT_BETWEEN))
						.operator(Operator.IN));
		assertThrows(IllegalArgumentException.class, () -> ColumnConditionModel.builder("test", String.class)
						.operator(Operator.IN)
						.operators(Arrays.asList(Operator.EQUAL, Operator.NOT_BETWEEN)));

		ColumnConditionModel<String, String> model = ColumnConditionModel.builder("test", String.class)
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
		ColumnConditionModel<String, String> model = ColumnConditionModel.builder("test", String.class).build();
		assertTrue(model.autoEnable().get());
		model.operand().equal().set("test");
		assertTrue(model.enabled().get());
		model.caseSensitive().set(false);
		assertFalse(model.caseSensitive().get());
		assertEquals("test", model.identifier());
		assertEquals(String.class, model.columnClass());

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
		ColumnConditionModel<String, String> model = ColumnConditionModel.builder("test", String.class).build();
		model.locked().set(true);
		assertThrows(IllegalStateException.class, () -> model.operand().upperBound().set("test"));
	}

	@Test
	void setLowerBoundLocked() {
		ColumnConditionModel<String, String> model = ColumnConditionModel.builder("test", String.class).build();
		model.locked().set(true);
		assertThrows(IllegalStateException.class, () -> model.operand().lowerBound().set("test"));
	}

	@Test
	void setEqualValueLocked() {
		ColumnConditionModel<String, String> model = ColumnConditionModel.builder("test", String.class).build();
		model.locked().set(true);
		assertThrows(IllegalStateException.class, () -> model.operand().equal().set("test"));
	}

	@Test
	void setInValuesLocked() {
		ColumnConditionModel<String, String> model = ColumnConditionModel.builder("test", String.class).build();
		model.locked().set(true);
		assertThrows(IllegalStateException.class, () -> model.operand().in().set(Collections.singletonList("test")));
	}

	@Test
	void setEnabledLocked() {
		ColumnConditionModel<String, String> model = ColumnConditionModel.builder("test", String.class).build();
		model.locked().set(true);
		assertThrows(IllegalStateException.class, () -> model.enabled().set(true));
	}

	@Test
	void setOperatorLocked() {
		ColumnConditionModel<String, String> model = ColumnConditionModel.builder("test", String.class).build();
		model.locked().set(true);
		assertThrows(IllegalStateException.class, () -> model.operator().set(Operator.NOT_EQUAL));
		assertThrows(IllegalStateException.class, () -> model.operator().set(Operator.NOT_EQUAL));
	}

	@Test
	void multiConditionString() {
		ColumnConditionModel<String, String> conditionModel = ColumnConditionModel.builder("test", String.class).build();
		conditionModel.caseSensitive().set(false);
		conditionModel.automaticWildcard().set(AutomaticWildcard.NONE);

		Collection<String> strings = asList("abc", "def");
		conditionModel.operand().in().set(strings);

		assertTrue(conditionModel.operand().in().get().containsAll(strings));
	}

	@Test
	void autoEnable() {
		ColumnConditionModel<String, Integer> conditionModel = ColumnConditionModel.builder("test", Integer.class).build();

		conditionModel.operator().set(Operator.EQUAL);
		assertFalse(conditionModel.enabled().get());
		Operand<Integer> operand = conditionModel.operand();
		operand.equal().set(1);
		assertTrue(conditionModel.enabled().get());
		operand.equal().set(null);
		assertFalse(conditionModel.enabled().get());
		conditionModel.enabled().set(false);
		operand.upperBound().set(1);
		operand.lowerBound().set(1);
		assertFalse(conditionModel.enabled().get());
		operand.upperBound().set(null);
		operand.lowerBound().set(null);

		conditionModel.operator().set(Operator.NOT_EQUAL);
		assertFalse(conditionModel.enabled().get());
		operand.equal().set(1);
		assertTrue(conditionModel.enabled().get());
		operand.equal().set(null);
		assertFalse(conditionModel.enabled().get());
		conditionModel.enabled().set(false);
		operand.upperBound().set(1);
		operand.lowerBound().set(1);
		assertFalse(conditionModel.enabled().get());
		operand.upperBound().set(null);
		operand.lowerBound().set(null);

		conditionModel.operator().set(Operator.LESS_THAN);
		assertFalse(conditionModel.enabled().get());
		operand.upperBound().set(1);
		assertTrue(conditionModel.enabled().get());
		operand.upperBound().set(null);
		assertFalse(conditionModel.enabled().get());
		operand.lowerBound().set(1);
		assertFalse(conditionModel.enabled().get());
		operand.lowerBound().set(null);

		conditionModel.operator().set(Operator.LESS_THAN_OR_EQUAL);
		assertFalse(conditionModel.enabled().get());
		operand.upperBound().set(1);
		assertTrue(conditionModel.enabled().get());
		operand.upperBound().set(null);
		assertFalse(conditionModel.enabled().get());
		operand.lowerBound().set(1);
		assertFalse(conditionModel.enabled().get());
		operand.lowerBound().set(null);

		conditionModel.operator().set(Operator.GREATER_THAN);
		assertFalse(conditionModel.enabled().get());
		operand.lowerBound().set(1);
		assertTrue(conditionModel.enabled().get());
		operand.lowerBound().set(null);
		assertFalse(conditionModel.enabled().get());
		operand.upperBound().set(1);
		assertFalse(conditionModel.enabled().get());
		operand.upperBound().set(null);

		conditionModel.operator().set(Operator.GREATER_THAN_OR_EQUAL);
		assertFalse(conditionModel.enabled().get());
		operand.lowerBound().set(1);
		assertTrue(conditionModel.enabled().get());
		operand.lowerBound().set(null);
		assertFalse(conditionModel.enabled().get());
		operand.upperBound().set(1);
		assertFalse(conditionModel.enabled().get());
		operand.upperBound().set(null);

		conditionModel.operator().set(Operator.BETWEEN);
		assertFalse(conditionModel.enabled().get());
		operand.equal().set(1);
		assertFalse(conditionModel.enabled().get());
		operand.equal().set(null);
		operand.lowerBound().set(1);
		assertFalse(conditionModel.enabled().get());
		operand.upperBound().set(1);
		assertTrue(conditionModel.enabled().get());
		operand.lowerBound().set(null);
		assertFalse(conditionModel.enabled().get());
		operand.lowerBound().set(1);
		assertTrue(conditionModel.enabled().get());
		operand.lowerBound().set(null);
		operand.upperBound().set(null);

		conditionModel.operator().set(Operator.BETWEEN_EXCLUSIVE);
		assertFalse(conditionModel.enabled().get());
		operand.equal().set(1);
		assertFalse(conditionModel.enabled().get());
		operand.equal().set(null);
		operand.lowerBound().set(1);
		assertFalse(conditionModel.enabled().get());
		operand.upperBound().set(1);
		assertTrue(conditionModel.enabled().get());
		operand.lowerBound().set(null);
		assertFalse(conditionModel.enabled().get());
		operand.lowerBound().set(1);
		assertTrue(conditionModel.enabled().get());
		operand.lowerBound().set(null);
		operand.upperBound().set(null);

		conditionModel.operator().set(Operator.NOT_BETWEEN);
		assertFalse(conditionModel.enabled().get());
		operand.equal().set(1);
		assertFalse(conditionModel.enabled().get());
		operand.equal().set(null);
		operand.lowerBound().set(1);
		assertFalse(conditionModel.enabled().get());
		operand.upperBound().set(1);
		assertTrue(conditionModel.enabled().get());
		operand.lowerBound().set(null);
		assertFalse(conditionModel.enabled().get());
		operand.lowerBound().set(1);
		assertTrue(conditionModel.enabled().get());
		operand.lowerBound().set(null);
		operand.upperBound().set(null);

		conditionModel.operator().set(Operator.NOT_BETWEEN_EXCLUSIVE);
		assertFalse(conditionModel.enabled().get());
		operand.equal().set(1);
		assertFalse(conditionModel.enabled().get());
		operand.equal().set(null);
		operand.lowerBound().set(1);
		assertFalse(conditionModel.enabled().get());
		operand.upperBound().set(1);
		assertTrue(conditionModel.enabled().get());
		operand.lowerBound().set(null);
		assertFalse(conditionModel.enabled().get());
		operand.lowerBound().set(1);
		assertTrue(conditionModel.enabled().get());
		operand.lowerBound().set(null);
		operand.upperBound().set(null);
	}

	@Test
	void noOperators() {
		assertThrows(IllegalArgumentException.class, () -> ColumnConditionModel.builder("test", String.class)
						.operators(Collections.emptyList()));
	}

	@Test
	void includeInteger() {
		ColumnConditionModel<String, Integer> conditionModel = ColumnConditionModel.builder("test", Integer.class).build();
		conditionModel.autoEnable().set(false);
		conditionModel.enabled().set(true);
		conditionModel.operator().set(Operator.EQUAL);

		Operand<Integer> operand = conditionModel.operand();
		operand.equal().set(null);
		assertTrue(conditionModel.accepts(null));
		assertFalse(conditionModel.accepts(1));

		conditionModel.operator().set(Operator.NOT_EQUAL);
		assertFalse(conditionModel.accepts(null));
		assertTrue(conditionModel.accepts(1));

		conditionModel.operator().set(Operator.EQUAL);

		operand.equal().set(10);
		assertFalse(conditionModel.accepts(null));
		assertFalse(conditionModel.accepts(9));
		assertTrue(conditionModel.accepts(10));
		assertFalse(conditionModel.accepts(11));

		conditionModel.operator().set(Operator.NOT_EQUAL);
		assertTrue(conditionModel.accepts(null));
		assertTrue(conditionModel.accepts(9));
		assertFalse(conditionModel.accepts(10));
		assertTrue(conditionModel.accepts(11));

		operand.lowerBound().set(10);
		conditionModel.operator().set(Operator.GREATER_THAN_OR_EQUAL);
		assertFalse(conditionModel.accepts(null));
		assertFalse(conditionModel.accepts(9));
		assertTrue(conditionModel.accepts(10));
		assertTrue(conditionModel.accepts(11));
		conditionModel.operator().set(Operator.GREATER_THAN);
		assertFalse(conditionModel.accepts(null));
		assertFalse(conditionModel.accepts(9));
		assertFalse(conditionModel.accepts(10));
		assertTrue(conditionModel.accepts(11));

		operand.upperBound().set(10);
		conditionModel.operator().set(Operator.LESS_THAN_OR_EQUAL);
		assertFalse(conditionModel.accepts(null));
		assertTrue(conditionModel.accepts(9));
		assertTrue(conditionModel.accepts(10));
		assertFalse(conditionModel.accepts(11));
		conditionModel.operator().set(Operator.LESS_THAN);
		assertFalse(conditionModel.accepts(null));
		assertTrue(conditionModel.accepts(9));
		assertFalse(conditionModel.accepts(10));
		assertFalse(conditionModel.accepts(11));

		operand.lowerBound().set(6);
		conditionModel.operator().set(Operator.BETWEEN);
		assertFalse(conditionModel.accepts(null));
		assertTrue(conditionModel.accepts(6));
		assertTrue(conditionModel.accepts(7));
		assertTrue(conditionModel.accepts(9));
		assertTrue(conditionModel.accepts(10));
		assertFalse(conditionModel.accepts(11));
		assertFalse(conditionModel.accepts(5));
		conditionModel.operator().set(Operator.BETWEEN_EXCLUSIVE);
		assertFalse(conditionModel.accepts(null));
		assertFalse(conditionModel.accepts(6));
		assertTrue(conditionModel.accepts(7));
		assertTrue(conditionModel.accepts(9));
		assertFalse(conditionModel.accepts(10));
		assertFalse(conditionModel.accepts(11));
		assertFalse(conditionModel.accepts(5));

		conditionModel.operator().set(Operator.NOT_BETWEEN);
		assertFalse(conditionModel.accepts(null));
		assertTrue(conditionModel.accepts(6));
		assertFalse(conditionModel.accepts(7));
		assertFalse(conditionModel.accepts(9));
		assertTrue(conditionModel.accepts(10));
		assertTrue(conditionModel.accepts(11));
		assertTrue(conditionModel.accepts(5));
		conditionModel.operator().set(Operator.NOT_BETWEEN_EXCLUSIVE);
		assertFalse(conditionModel.accepts(null));
		assertFalse(conditionModel.accepts(6));
		assertFalse(conditionModel.accepts(7));
		assertFalse(conditionModel.accepts(9));
		assertFalse(conditionModel.accepts(10));
		assertTrue(conditionModel.accepts(11));
		assertTrue(conditionModel.accepts(5));

		operand.upperBound().set(null);
		operand.lowerBound().set(null);
		conditionModel.operator().set(Operator.BETWEEN);
		assertTrue(conditionModel.accepts(1));
		assertTrue(conditionModel.accepts(8));
		assertTrue(conditionModel.accepts(11));
		conditionModel.operator().set(Operator.BETWEEN_EXCLUSIVE);
		assertTrue(conditionModel.accepts(1));
		assertTrue(conditionModel.accepts(8));
		assertTrue(conditionModel.accepts(11));
		conditionModel.operator().set(Operator.NOT_BETWEEN);
		assertTrue(conditionModel.accepts(1));
		assertTrue(conditionModel.accepts(8));
		assertTrue(conditionModel.accepts(11));
		conditionModel.operator().set(Operator.NOT_BETWEEN_EXCLUSIVE);
		assertTrue(conditionModel.accepts(1));
		assertTrue(conditionModel.accepts(8));
		assertTrue(conditionModel.accepts(11));

		assertTrue(conditionModel.accepts(null));
		assertTrue(conditionModel.accepts(5));
		assertTrue(conditionModel.accepts(6));
		assertTrue(conditionModel.accepts(7));

		conditionModel.operator().set(Operator.IN);
		operand.in().set(asList(1, 2, 3));
		assertTrue(conditionModel.accepts(1));
		assertTrue(conditionModel.accepts(2));
		assertTrue(conditionModel.accepts(3));
		assertFalse(conditionModel.accepts(4));
		conditionModel.operator().set(Operator.NOT_IN);
		assertFalse(conditionModel.accepts(1));
		assertFalse(conditionModel.accepts(2));
		assertFalse(conditionModel.accepts(3));
		assertTrue(conditionModel.accepts(4));
	}

	@Test
	void acceptsString() {
		ColumnConditionModel<String, String> conditionModel = ColumnConditionModel.builder("test", String.class).build();
		conditionModel.autoEnable().set(false);
		conditionModel.enabled().set(true);

		conditionModel.operator().set(Operator.EQUAL);
		Operand<String> operand = conditionModel.operand();
		operand.equal().set("hello");
		assertTrue(conditionModel.accepts("hello"));
		operand.equal().set("hell%");
		assertTrue(conditionModel.accepts("hello"));
		assertFalse(conditionModel.accepts("helo"));
		operand.equal().set("%ell%");
		assertTrue(conditionModel.accepts("hello"));
		assertFalse(conditionModel.accepts("helo"));

		conditionModel.caseSensitive().set(false);
		assertTrue(conditionModel.accepts("HELlo"));
		assertFalse(conditionModel.accepts("heLo"));
		assertFalse(conditionModel.accepts(null));

		operand.equal().set("%");
		assertTrue(conditionModel.accepts("hello"));
		assertTrue(conditionModel.accepts("helo"));

		conditionModel.caseSensitive().set(true);
		operand.equal().set("hello");
		conditionModel.operator().set(Operator.NOT_EQUAL);
		assertFalse(conditionModel.accepts("hello"));
		operand.equal().set("hell%");
		assertFalse(conditionModel.accepts("hello"));
		assertTrue(conditionModel.accepts("helo"));
		operand.equal().set("%ell%");
		assertFalse(conditionModel.accepts("hello"));
		assertTrue(conditionModel.accepts("helo"));

		conditionModel.caseSensitive().set(false);
		assertFalse(conditionModel.accepts("HELlo"));
		assertTrue(conditionModel.accepts("heLo"));
		assertTrue(conditionModel.accepts(null));

		operand.equal().set("%");
		assertFalse(conditionModel.accepts("hello"));
		assertFalse(conditionModel.accepts("helo"));

		operand.equal().set("hell");
		conditionModel.operator().set(Operator.EQUAL);
		conditionModel.automaticWildcard().set(AutomaticWildcard.NONE);
		assertTrue(conditionModel.accepts("HELl"));
		assertTrue(conditionModel.accepts("hElL"));
		conditionModel.operator().set(Operator.NOT_EQUAL);
		assertFalse(conditionModel.accepts("HELl"));
		assertFalse(conditionModel.accepts("hElL"));
	}

	@Test
	void acceptCharacter() {
		ColumnConditionModel<String, Character> conditionModel = ColumnConditionModel.builder("test", Character.class).build();
		conditionModel.autoEnable().set(false);
		conditionModel.enabled().set(true);

		conditionModel.caseSensitive().set(true);
		conditionModel.operator().set(Operator.EQUAL);
		conditionModel.operand().equal().set('h');
		assertTrue(conditionModel.accepts('h'));
		assertFalse(conditionModel.accepts('H'));

		conditionModel.caseSensitive().set(false);
		assertTrue(conditionModel.accepts('H'));
	}
}

/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import is.codion.common.Operator;
import is.codion.common.Text;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.model.table.ColumnConditionModel.AutomaticWildcard;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultColumnConditionModelTest {

  final AtomicInteger equalToCounter = new AtomicInteger();
  final AtomicInteger upperBoundCounter = new AtomicInteger();
  final AtomicInteger lowerBoundCounter = new AtomicInteger();
  final AtomicInteger conditionChangedCounter = new AtomicInteger();
  final AtomicInteger operatorCounter = new AtomicInteger();
  final AtomicInteger enabledCounter = new AtomicInteger();

  final EventDataListener<String> equalToListener = value -> equalToCounter.incrementAndGet();
  final EventDataListener<String> upperBoundListener = value -> upperBoundCounter.incrementAndGet();
  final EventDataListener<String> lowerBoundListener = value -> lowerBoundCounter.incrementAndGet();
  final EventListener conditionChangedListener = conditionChangedCounter::incrementAndGet;
  final EventDataListener<Operator> operatorListener = data -> operatorCounter.incrementAndGet();
  final EventListener enabledListener = enabledCounter::incrementAndGet;

  @Test
  void testSetBounds() {
    ColumnConditionModel<String, String> model = ColumnConditionModel.builder("test", String.class).build();
    model.caseSensitiveState().set(false);
    model.automaticWildcardValue().set(AutomaticWildcard.NONE);

    model.autoEnableState().set(false);
    model.equalValueSet().value().addDataListener(equalToListener);
    model.upperBoundValue().addDataListener(upperBoundListener);
    model.lowerBoundValue().addDataListener(lowerBoundListener);
    model.addChangeListener(conditionChangedListener);

    model.setUpperBound("hello");
    assertEquals(1, conditionChangedCounter.get());
    assertFalse(model.isEnabled());
    assertEquals(1, upperBoundCounter.get());
    assertEquals("hello", model.getUpperBound());
    model.setLowerBound("hello");
    assertEquals(2, conditionChangedCounter.get());
    assertEquals(1, lowerBoundCounter.get());
    assertEquals("hello", model.getLowerBound());

    model.setEqualValue("test");
    assertEquals(1, equalToCounter.get());
    assertEquals("test", model.getEqualValue());

    model.automaticWildcardValue().set(AutomaticWildcard.PREFIX_AND_POSTFIX);
    assertEquals("%test%", model.getEqualValues().iterator().next());
    assertEquals("%test%", model.getEqualValue());

    model.automaticWildcardValue().set(AutomaticWildcard.PREFIX);
    assertEquals("%test", model.getEqualValues().iterator().next());
    assertEquals("%test", model.getEqualValue());

    model.automaticWildcardValue().set(AutomaticWildcard.POSTFIX);
    assertEquals("test%", model.getEqualValues().iterator().next());
    assertEquals("test%", model.getEqualValue());

    model.automaticWildcardValue().set(AutomaticWildcard.NONE);
    assertEquals("test", model.getEqualValues().iterator().next());
    assertEquals("test", model.getEqualValue());

    model.clear();

    model.equalValueSet().value().removeDataListener(equalToListener);
    model.upperBoundValue().removeDataListener(upperBoundListener);
    model.lowerBoundValue().removeDataListener(lowerBoundListener);
    model.removeChangeListener(conditionChangedListener);
  }

  @Test
  void testMisc() {
    ColumnConditionModel<String, String> model = ColumnConditionModel.builder("test", String.class).build();
    assertEquals("test", model.columnIdentifier());

    model.setOperator(Operator.EQUAL);
    model.automaticWildcardValue().set(AutomaticWildcard.PREFIX_AND_POSTFIX);
    model.setEqualValue("upper");
    assertEquals("%upper%", model.getEqualValue());
  }

  @Test
  void testOperator() {
    ColumnConditionModel<String, String> model = ColumnConditionModel.builder("test", String.class)
            .operators(Arrays.asList(Operator.EQUAL, Operator.NOT_EQUAL, Operator.LESS_THAN_OR_EQUAL, Operator.NOT_BETWEEN))
            .build();
    model.operatorValue().addDataListener(operatorListener);
    assertEquals(Operator.EQUAL, model.getOperator());
    model.setOperator(Operator.LESS_THAN_OR_EQUAL);
    assertEquals(1, operatorCounter.get());
    assertEquals(Operator.LESS_THAN_OR_EQUAL, model.getOperator());
    assertThrows(NullPointerException.class, () -> model.setOperator(null));
    model.setOperator(Operator.NOT_BETWEEN);
    assertEquals(2, operatorCounter.get());
    model.operatorValue().removeDataListener(operatorListener);

    assertThrows(IllegalArgumentException.class, () -> model.setOperator(Operator.BETWEEN));

    assertThrows(IllegalArgumentException.class, () -> model.operatorValue().set(Operator.BETWEEN));
  }

  @Test
  void test() throws Exception {
    ColumnConditionModel<String, String> model = ColumnConditionModel.builder("test", String.class).build();
    assertTrue(model.autoEnableState().get());
    model.setEqualValue("test");
    assertTrue(model.isEnabled());
    model.caseSensitiveState().set(false);
    assertFalse(model.caseSensitiveState().get());
    assertEquals("test", model.columnIdentifier());
    assertEquals(String.class, model.columnClass());
    assertEquals(Text.WILDCARD_CHARACTER.get(), model.wildcard());

    model.automaticWildcardValue().set(AutomaticWildcard.PREFIX_AND_POSTFIX);
    assertEquals(AutomaticWildcard.PREFIX_AND_POSTFIX, model.automaticWildcardValue().get());

    model.enabledState().addListener(enabledListener);
    model.setEnabled(false);
    assertEquals(1, enabledCounter.get());
    model.setEnabled(true);
    assertEquals(2, enabledCounter.get());

    model.enabledState().removeListener(enabledListener);

    model.setLocked(true);
    assertTrue(model.isLocked());
    assertTrue(model.lockedObserver().get());
  }

  @Test
  void setUpperBoundLocked() {
    ColumnConditionModel<String, String> model = ColumnConditionModel.builder("test", String.class).build();
    model.setLocked(true);
    assertThrows(IllegalStateException.class, () -> model.setUpperBound("test"));
  }

  @Test
  void setLowerBoundLocked() {
    ColumnConditionModel<String, String> model = ColumnConditionModel.builder("test", String.class).build();
    model.setLocked(true);
    assertThrows(IllegalStateException.class, () -> model.setLowerBound("test"));
  }

  @Test
  void setEqualValueLocked() {
    ColumnConditionModel<String, String> model = ColumnConditionModel.builder("test", String.class).build();
    model.setLocked(true);
    assertThrows(IllegalStateException.class, () -> model.setEqualValue("test"));
  }

  @Test
  void setEqualValuesLocked() {
    ColumnConditionModel<String, String> model = ColumnConditionModel.builder("test", String.class).build();
    model.setLocked(true);
    assertThrows(IllegalStateException.class, () -> model.setEqualValues(Collections.singletonList("test")));
  }

  @Test
  void setEnabledLocked() {
    ColumnConditionModel<String, String> model = ColumnConditionModel.builder("test", String.class).build();
    model.setLocked(true);
    assertThrows(IllegalStateException.class, () -> model.setEnabled(true));
  }

  @Test
  void setOperatorLocked() {
    ColumnConditionModel<String, String> model = ColumnConditionModel.builder("test", String.class).build();
    model.setLocked(true);
    assertThrows(IllegalStateException.class, () -> model.setOperator(Operator.NOT_EQUAL));
    assertThrows(IllegalStateException.class, () -> model.operatorValue().set(Operator.NOT_EQUAL));
  }

  @Test
  void multiConditionString() {
    ColumnConditionModel<String, String> conditionModel = ColumnConditionModel.builder("test", String.class).build();
    conditionModel.caseSensitiveState().set(false);
    conditionModel.automaticWildcardValue().set(AutomaticWildcard.NONE);

    Collection<String> strings = asList("abc", "def");
    conditionModel.setEqualValues(strings);

    assertTrue(conditionModel.getEqualValues().containsAll(strings));
  }

  @Test
  void autoEnable() {
    ColumnConditionModel<String, Integer> conditionModel = ColumnConditionModel.builder("test", Integer.class).build();

    conditionModel.setOperator(Operator.EQUAL);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setEqualValue(1);
    assertTrue(conditionModel.isEnabled());
    conditionModel.setEqualValue(null);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setEnabled(false);
    conditionModel.setUpperBound(1);
    conditionModel.setLowerBound(1);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setUpperBound(null);
    conditionModel.setLowerBound(null);

    conditionModel.setOperator(Operator.NOT_EQUAL);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setEqualValue(1);
    assertTrue(conditionModel.isEnabled());
    conditionModel.setEqualValue(null);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setEnabled(false);
    conditionModel.setUpperBound(1);
    conditionModel.setLowerBound(1);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setUpperBound(null);
    conditionModel.setLowerBound(null);

    conditionModel.setOperator(Operator.LESS_THAN);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setUpperBound(1);
    assertTrue(conditionModel.isEnabled());
    conditionModel.setUpperBound(null);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setLowerBound(1);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setLowerBound(null);

    conditionModel.setOperator(Operator.LESS_THAN_OR_EQUAL);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setUpperBound(1);
    assertTrue(conditionModel.isEnabled());
    conditionModel.setUpperBound(null);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setLowerBound(1);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setLowerBound(null);

    conditionModel.setOperator(Operator.GREATER_THAN);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setLowerBound(1);
    assertTrue(conditionModel.isEnabled());
    conditionModel.setLowerBound(null);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setUpperBound(1);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setUpperBound(null);

    conditionModel.setOperator(Operator.GREATER_THAN_OR_EQUAL);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setLowerBound(1);
    assertTrue(conditionModel.isEnabled());
    conditionModel.setLowerBound(null);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setUpperBound(1);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setUpperBound(null);

    conditionModel.setOperator(Operator.BETWEEN);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setEqualValue(1);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setEqualValue(null);
    conditionModel.setLowerBound(1);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setUpperBound(1);
    assertTrue(conditionModel.isEnabled());
    conditionModel.setLowerBound(null);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setLowerBound(1);
    assertTrue(conditionModel.isEnabled());
    conditionModel.setLowerBound(null);
    conditionModel.setUpperBound(null);

    conditionModel.setOperator(Operator.BETWEEN_EXCLUSIVE);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setEqualValue(1);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setEqualValue(null);
    conditionModel.setLowerBound(1);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setUpperBound(1);
    assertTrue(conditionModel.isEnabled());
    conditionModel.setLowerBound(null);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setLowerBound(1);
    assertTrue(conditionModel.isEnabled());
    conditionModel.setLowerBound(null);
    conditionModel.setUpperBound(null);

    conditionModel.setOperator(Operator.NOT_BETWEEN);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setEqualValue(1);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setEqualValue(null);
    conditionModel.setLowerBound(1);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setUpperBound(1);
    assertTrue(conditionModel.isEnabled());
    conditionModel.setLowerBound(null);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setLowerBound(1);
    assertTrue(conditionModel.isEnabled());
    conditionModel.setLowerBound(null);
    conditionModel.setUpperBound(null);

    conditionModel.setOperator(Operator.NOT_BETWEEN_EXCLUSIVE);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setEqualValue(1);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setEqualValue(null);
    conditionModel.setLowerBound(1);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setUpperBound(1);
    assertTrue(conditionModel.isEnabled());
    conditionModel.setLowerBound(null);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setLowerBound(1);
    assertTrue(conditionModel.isEnabled());
    conditionModel.setLowerBound(null);
    conditionModel.setUpperBound(null);
  }

  @Test
  void noOperators() {
    assertThrows(IllegalArgumentException.class, () -> ColumnConditionModel.builder("test", String.class)
            .operators(Collections.emptyList()));
  }

  @Test
  void includeInteger() {
    ColumnConditionModel<String, Integer> conditionModel = ColumnConditionModel.builder("test", Integer.class).build();
    conditionModel.autoEnableState().set(false);
    conditionModel.setEnabled(true);
    conditionModel.setOperator(Operator.EQUAL);

    conditionModel.setEqualValue(null);
    assertTrue(conditionModel.accepts(null));
    assertFalse(conditionModel.accepts(1));

    conditionModel.setOperator(Operator.NOT_EQUAL);
    assertFalse(conditionModel.accepts(null));
    assertTrue(conditionModel.accepts(1));

    conditionModel.setOperator(Operator.EQUAL);

    conditionModel.setEqualValue(10);
    assertFalse(conditionModel.accepts(null));
    assertFalse(conditionModel.accepts(9));
    assertTrue(conditionModel.accepts(10));
    assertFalse(conditionModel.accepts(11));

    conditionModel.setOperator(Operator.NOT_EQUAL);
    assertTrue(conditionModel.accepts(null));
    assertTrue(conditionModel.accepts(9));
    assertFalse(conditionModel.accepts(10));
    assertTrue(conditionModel.accepts(11));

    conditionModel.setLowerBound(10);
    conditionModel.setOperator(Operator.GREATER_THAN_OR_EQUAL);
    assertFalse(conditionModel.accepts(null));
    assertFalse(conditionModel.accepts(9));
    assertTrue(conditionModel.accepts(10));
    assertTrue(conditionModel.accepts(11));
    conditionModel.setOperator(Operator.GREATER_THAN);
    assertFalse(conditionModel.accepts(null));
    assertFalse(conditionModel.accepts(9));
    assertFalse(conditionModel.accepts(10));
    assertTrue(conditionModel.accepts(11));

    conditionModel.setUpperBound(10);
    conditionModel.setOperator(Operator.LESS_THAN_OR_EQUAL);
    assertFalse(conditionModel.accepts(null));
    assertTrue(conditionModel.accepts(9));
    assertTrue(conditionModel.accepts(10));
    assertFalse(conditionModel.accepts(11));
    conditionModel.setOperator(Operator.LESS_THAN);
    assertFalse(conditionModel.accepts(null));
    assertTrue(conditionModel.accepts(9));
    assertFalse(conditionModel.accepts(10));
    assertFalse(conditionModel.accepts(11));

    conditionModel.setLowerBound(6);
    conditionModel.setOperator(Operator.BETWEEN);
    assertFalse(conditionModel.accepts(null));
    assertTrue(conditionModel.accepts(6));
    assertTrue(conditionModel.accepts(7));
    assertTrue(conditionModel.accepts(9));
    assertTrue(conditionModel.accepts(10));
    assertFalse(conditionModel.accepts(11));
    assertFalse(conditionModel.accepts(5));
    conditionModel.setOperator(Operator.BETWEEN_EXCLUSIVE);
    assertFalse(conditionModel.accepts(null));
    assertFalse(conditionModel.accepts(6));
    assertTrue(conditionModel.accepts(7));
    assertTrue(conditionModel.accepts(9));
    assertFalse(conditionModel.accepts(10));
    assertFalse(conditionModel.accepts(11));
    assertFalse(conditionModel.accepts(5));

    conditionModel.setOperator(Operator.NOT_BETWEEN);
    assertFalse(conditionModel.accepts(null));
    assertTrue(conditionModel.accepts(6));
    assertFalse(conditionModel.accepts(7));
    assertFalse(conditionModel.accepts(9));
    assertTrue(conditionModel.accepts(10));
    assertTrue(conditionModel.accepts(11));
    assertTrue(conditionModel.accepts(5));
    conditionModel.setOperator(Operator.NOT_BETWEEN_EXCLUSIVE);
    assertFalse(conditionModel.accepts(null));
    assertFalse(conditionModel.accepts(6));
    assertFalse(conditionModel.accepts(7));
    assertFalse(conditionModel.accepts(9));
    assertFalse(conditionModel.accepts(10));
    assertTrue(conditionModel.accepts(11));
    assertTrue(conditionModel.accepts(5));

    conditionModel.setUpperBound(null);
    conditionModel.setLowerBound(null);
    conditionModel.setOperator(Operator.BETWEEN);
    assertTrue(conditionModel.accepts(1));
    assertTrue(conditionModel.accepts(8));
    assertTrue(conditionModel.accepts(11));
    conditionModel.setOperator(Operator.BETWEEN_EXCLUSIVE);
    assertTrue(conditionModel.accepts(1));
    assertTrue(conditionModel.accepts(8));
    assertTrue(conditionModel.accepts(11));
    conditionModel.setOperator(Operator.NOT_BETWEEN);
    assertTrue(conditionModel.accepts(1));
    assertTrue(conditionModel.accepts(8));
    assertTrue(conditionModel.accepts(11));
    conditionModel.setOperator(Operator.NOT_BETWEEN_EXCLUSIVE);
    assertTrue(conditionModel.accepts(1));
    assertTrue(conditionModel.accepts(8));
    assertTrue(conditionModel.accepts(11));

    assertTrue(conditionModel.accepts(null));
    assertTrue(conditionModel.accepts(5));
    assertTrue(conditionModel.accepts(6));
    assertTrue(conditionModel.accepts(7));
  }

  @Test
  void includeString() {
    ColumnConditionModel<String, String> conditionModel = ColumnConditionModel.builder("test", String.class).build();
    conditionModel.autoEnableState().set(false);
    conditionModel.setEnabled(true);

    conditionModel.setOperator(Operator.EQUAL);
    conditionModel.setEqualValue("hello");
    assertTrue(conditionModel.accepts("hello"));
    conditionModel.setEqualValue("hell%");
    assertTrue(conditionModel.accepts("hello"));
    assertFalse(conditionModel.accepts("helo"));
    conditionModel.setEqualValue("%ell%");
    assertTrue(conditionModel.accepts("hello"));
    assertFalse(conditionModel.accepts("helo"));

    conditionModel.caseSensitiveState().set(false);
    assertTrue(conditionModel.accepts("HELlo"));
    assertFalse(conditionModel.accepts("heLo"));
    assertFalse(conditionModel.accepts(null));

    conditionModel.setEqualValue("%");
    assertTrue(conditionModel.accepts("hello"));
    assertTrue(conditionModel.accepts("helo"));

    conditionModel.caseSensitiveState().set(true);
    conditionModel.setEqualValue("hello");
    conditionModel.setOperator(Operator.NOT_EQUAL);
    assertFalse(conditionModel.accepts("hello"));
    conditionModel.setEqualValue("hell%");
    assertFalse(conditionModel.accepts("hello"));
    assertTrue(conditionModel.accepts("helo"));
    conditionModel.setEqualValue("%ell%");
    assertFalse(conditionModel.accepts("hello"));
    assertTrue(conditionModel.accepts("helo"));

    conditionModel.caseSensitiveState().set(false);
    assertFalse(conditionModel.accepts("HELlo"));
    assertTrue(conditionModel.accepts("heLo"));
    assertTrue(conditionModel.accepts(null));

    conditionModel.setEqualValue("%");
    assertFalse(conditionModel.accepts("hello"));
    assertFalse(conditionModel.accepts("helo"));
  }
}

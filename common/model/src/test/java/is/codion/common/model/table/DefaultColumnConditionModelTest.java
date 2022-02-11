/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import is.codion.common.Operator;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.model.table.ColumnConditionModel.AutomaticWildcard;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultColumnConditionModelTest {

  private static final List<Operator> ALL_OPERATORS = asList(Operator.values());

  final AtomicInteger equalToCounter = new AtomicInteger();
  final AtomicInteger upperBoundCounter = new AtomicInteger();
  final AtomicInteger lowerBoundCounter = new AtomicInteger();
  final AtomicInteger conditionChangedCounter = new AtomicInteger();
  final AtomicInteger operatorCounter = new AtomicInteger();
  final AtomicInteger enabledCounter = new AtomicInteger();
  final AtomicInteger clearCounter = new AtomicInteger();

  final EventListener equalToListener = equalToCounter::incrementAndGet;
  final EventListener upperBoundListener = upperBoundCounter::incrementAndGet;
  final EventListener lowerBoundListener = lowerBoundCounter::incrementAndGet;
  final EventListener conditionChangedListener = conditionChangedCounter::incrementAndGet;
  final EventDataListener<Operator> operatorListener = data -> operatorCounter.incrementAndGet();
  final EventListener enabledListener = enabledCounter::incrementAndGet;
  final EventListener clearListener = clearCounter::incrementAndGet;

  @Test
  void testSetBounds() {
    final DefaultColumnConditionModel<String, String> model =
            new DefaultColumnConditionModel<>("test", String.class, Arrays.asList(Operator.values()), "%");
    model.setAutoEnable(false);
    assertFalse(model.isAutoEnable());
    model.addEqualsValueListener(equalToListener);
    model.addUpperBoundListener(upperBoundListener);
    model.addLowerBoundListener(lowerBoundListener);
    model.addConditionChangedListener(conditionChangedListener);
    model.addClearedListener(clearListener);

    model.setUpperBound("hello");
    assertEquals(1, conditionChangedCounter.get());
    assertFalse(model.isEnabled());
    assertEquals(1, upperBoundCounter.get());
    assertEquals("hello", model.getUpperBound());
    model.setLowerBound("hello");
    assertEquals(2, conditionChangedCounter.get());
    assertEquals(1, lowerBoundCounter.get());
    assertEquals("hello", model.getLowerBound());

    model.setAutomaticWildcard(AutomaticWildcard.PREFIX_AND_POSTFIX);
    assertEquals("%hello%", model.getUpperBound());
    assertEquals("%hello%", model.getLowerBound());
    model.setAutomaticWildcard(AutomaticWildcard.NONE);

    model.setEqualValue("test");
    assertEquals(1, equalToCounter.get());
    assertEquals("test", model.getEqualValue());

    model.setAutomaticWildcard(AutomaticWildcard.PREFIX_AND_POSTFIX);
    assertEquals("%test%", model.getEqualValues().iterator().next());

    model.clearCondition();
    assertEquals(1, clearCounter.get());

    model.removeEqualsValueListener(equalToListener);
    model.removeUpperBoundListener(upperBoundListener);
    model.removeLowerBoundListener(lowerBoundListener);
    model.removeConditionChangedListener(conditionChangedListener);
    model.removeClearedListener(clearListener);
  }

  @Test
  void testMisc() {
    final ColumnConditionModel<String, String> model = new DefaultColumnConditionModel<>("test", String.class, ALL_OPERATORS, "%");
    assertEquals("test", model.getColumnIdentifier());

    model.setOperator(Operator.EQUAL);
    model.setAutomaticWildcard(AutomaticWildcard.PREFIX_AND_POSTFIX);
    model.setEqualValue("upper");
    assertEquals("%upper%", model.getEqualValue());
  }

  @Test
  void testOperator() {
    final DefaultColumnConditionModel<String, String> model = new DefaultColumnConditionModel<>("test",
            String.class, Arrays.asList(Operator.EQUAL, Operator.NOT_EQUAL, Operator.LESS_THAN_OR_EQUAL, Operator.NOT_BETWEEN), "%");
    model.addOperatorListener(operatorListener);
    assertEquals(Operator.EQUAL, model.getOperator());
    model.setOperator(Operator.LESS_THAN_OR_EQUAL);
    assertEquals(1, operatorCounter.get());
    assertEquals(Operator.LESS_THAN_OR_EQUAL, model.getOperator());
    try {
      model.setOperator(null);
      fail();
    }
    catch (final NullPointerException ignored) {/*ignored*/}
    model.setOperator(Operator.NOT_BETWEEN);
    assertEquals(2, operatorCounter.get());
    model.removeOperatorListener(operatorListener);

    assertThrows(IllegalArgumentException.class, () -> model.setOperator(Operator.BETWEEN));

    assertThrows(IllegalArgumentException.class, () -> model.getOperatorValue().set(Operator.BETWEEN));
  }

  @Test
  void test() throws Exception {
    final DefaultColumnConditionModel<String, String> model = new DefaultColumnConditionModel<>("test", String.class, ALL_OPERATORS, "%");
    assertTrue(model.isAutoEnable());
    model.setEqualValue("test");
    assertTrue(model.isEnabled());
    model.setCaseSensitive(false);
    assertFalse(model.isCaseSensitive());
    assertEquals("test", model.getColumnIdentifier());
    assertEquals(String.class, model.getTypeClass());
    assertEquals("%", model.getWildcard());

    model.setWildcard("#");
    assertEquals("#", model.getWildcard());

    model.setAutomaticWildcard(AutomaticWildcard.PREFIX_AND_POSTFIX);
    assertEquals(AutomaticWildcard.PREFIX_AND_POSTFIX, model.getAutomaticWildcard());

    model.addEnabledListener(enabledListener);
    model.setEnabled(false);
    assertEquals(1, enabledCounter.get());
    model.setEnabled(true);
    assertEquals(2, enabledCounter.get());

    model.removeEnabledListener(enabledListener);

    model.setLocked(true);
    assertTrue(model.isLocked());
    assertTrue(model.getLockedObserver().get());
  }

  @Test
  void setUpperBoundLocked() {
    final DefaultColumnConditionModel<String, String> model = new DefaultColumnConditionModel<>("test", String.class, ALL_OPERATORS, "%");
    model.setLocked(true);
    assertThrows(IllegalStateException.class, () -> model.setUpperBound("test"));
  }

  @Test
  void setLowerBoundLocked() {
    final DefaultColumnConditionModel<String, String> model = new DefaultColumnConditionModel<>("test", String.class, ALL_OPERATORS, "%");
    model.setLocked(true);
    assertThrows(IllegalStateException.class, () -> model.setLowerBound("test"));
  }

  @Test
  void setEqualValueLocked() {
    final DefaultColumnConditionModel<String, String> model = new DefaultColumnConditionModel<>("test", String.class, ALL_OPERATORS, "%");
    model.setLocked(true);
    assertThrows(IllegalStateException.class, () -> model.setEqualValue("test"));
  }

  @Test
  void setEqualValuesLocked() {
    final DefaultColumnConditionModel<String, String> model = new DefaultColumnConditionModel<>("test", String.class, ALL_OPERATORS, "%");
    model.setLocked(true);
    assertThrows(IllegalStateException.class, () -> model.setEqualValues(Collections.singletonList("test")));
  }

  @Test
  void setEnabledLocked() {
    final DefaultColumnConditionModel<String, String> model = new DefaultColumnConditionModel<>("test", String.class, ALL_OPERATORS, "%");
    model.setLocked(true);
    assertThrows(IllegalStateException.class, () -> model.setEnabled(true));
  }

  @Test
  void setOperatorLocked() {
    final DefaultColumnConditionModel<String, String> model = new DefaultColumnConditionModel<>("test", String.class, ALL_OPERATORS, "%");
    model.setLocked(true);
    assertThrows(IllegalStateException.class, () -> model.setOperator(Operator.NOT_EQUAL));
    assertThrows(IllegalStateException.class, () -> model.getOperatorValue().set(Operator.NOT_EQUAL));
  }

  @Test
  void multiConditionString() {
    final DefaultColumnConditionModel<String, String> conditionModel = new DefaultColumnConditionModel<>("test", String.class, ALL_OPERATORS, "%");

    final Collection<String> strings = asList("abc", "def");
    conditionModel.setEqualValues(strings);

    assertTrue(conditionModel.getEqualValues().containsAll(strings));
  }

  @Test
  void autoEnable() {
    final DefaultColumnConditionModel<String, Integer> conditionModel = new DefaultColumnConditionModel<>("Test", Integer.class, ALL_OPERATORS, null);

    conditionModel.setOperator(Operator.EQUAL);
    assertFalse(conditionModel.isEnabled());
    conditionModel.setEqualValue(1);
    assertTrue(conditionModel.isEnabled());
    conditionModel.setEqualValue(null);
    //null values are ok when EQUAL or NOT_EQUAL
    assertTrue(conditionModel.isEnabled());
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
    //null values are ok when EQUAL or NOT_EQUAL
    assertTrue(conditionModel.isEnabled());
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
    assertThrows(IllegalArgumentException.class, () -> new DefaultColumnConditionModel<>("test", String.class, Collections.emptyList(), "%"));
  }
}

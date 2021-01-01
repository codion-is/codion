/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import is.codion.common.db.Operator;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;

import org.junit.jupiter.api.Test;

import java.util.Collection;
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
  final AtomicInteger clearCounter = new AtomicInteger();

  final EventListener equalToListener = equalToCounter::incrementAndGet;
  final EventListener upperBoundListener = upperBoundCounter::incrementAndGet;
  final EventListener lowerBoundListener = lowerBoundCounter::incrementAndGet;
  final EventListener conditionChangedListener = conditionChangedCounter::incrementAndGet;
  final EventDataListener<Operator> operatorListener = data -> operatorCounter.incrementAndGet();
  final EventListener enabledListener = enabledCounter::incrementAndGet;
  final EventListener clearListener = clearCounter::incrementAndGet;

  @Test
  public void testSetBounds() {
    final DefaultColumnConditionModel<String, String, String> model = new DefaultColumnConditionModel<>("test", String.class, "%");
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

    model.setAutomaticWildcard(ColumnConditionModel.AutomaticWildcard.PREFIX_AND_POSTFIX);
    assertEquals("%hello%", model.getUpperBound());
    assertEquals("%hello%", model.getLowerBound());
    model.setAutomaticWildcard(ColumnConditionModel.AutomaticWildcard.NONE);

    model.setEqualValue("test");
    assertEquals(1, equalToCounter.get());
    assertEquals("test", model.getEqualValue());

    model.setAutomaticWildcard(ColumnConditionModel.AutomaticWildcard.PREFIX_AND_POSTFIX);
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
  public void testMisc() {
    final ColumnConditionModel<String, String, String> model = new DefaultColumnConditionModel<>("test", String.class, "%");
    assertEquals("test", model.getColumnIdentifier());

    model.setOperator(Operator.EQUAL);
    model.setAutomaticWildcard(ColumnConditionModel.AutomaticWildcard.PREFIX_AND_POSTFIX);
    model.setEqualValue("upper");
    assertEquals("%upper%", model.getEqualValue());
  }

  @Test
  public void testOperator() {
    final DefaultColumnConditionModel<String, String, String> model = new DefaultColumnConditionModel<>("test", String.class, "%");
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
  }

  @Test
  public void test() throws Exception {
    final DefaultColumnConditionModel<String, String, String> model = new DefaultColumnConditionModel<>("test", String.class, "%");
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

    model.setAutomaticWildcard(ColumnConditionModel.AutomaticWildcard.PREFIX_AND_POSTFIX);
    assertEquals(ColumnConditionModel.AutomaticWildcard.PREFIX_AND_POSTFIX, model.getAutomaticWildcard());

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
  public void setUpperBoundLocked() {
    final DefaultColumnConditionModel<String, String, String> model = new DefaultColumnConditionModel<>("test", String.class, "%");
    model.setLocked(true);
    assertThrows(IllegalStateException.class, () -> model.setUpperBound("test"));
  }

  @Test
  public void setLowerBoundLocked() {
    final DefaultColumnConditionModel<String, String, String> model = new DefaultColumnConditionModel<>("test", String.class, "%");
    model.setLocked(true);
    assertThrows(IllegalStateException.class, () -> model.setLowerBound("test"));
  }

  @Test
  public void setEnabledLocked() {
    final DefaultColumnConditionModel<String, String, String> model = new DefaultColumnConditionModel<>("test", String.class, "%");
    model.setLocked(true);
    assertThrows(IllegalStateException.class, () -> model.setEnabled(true));
  }

  @Test
  public void setOperatorLocked() {
    final DefaultColumnConditionModel<String, String, String> model = new DefaultColumnConditionModel<>("test", String.class, "%");
    model.setLocked(true);
    assertThrows(IllegalStateException.class, () -> model.setOperator(Operator.NOT_EQUAL));
  }

  @Test
  public void include() {
    final DefaultColumnConditionModel<String, String, Integer> conditionModel = new DefaultColumnConditionModel<>("test", Integer.class, "%");
    conditionModel.setEqualValue(10);
    conditionModel.setOperator(Operator.EQUAL);
    assertFalse(conditionModel.include(9));
    assertTrue(conditionModel.include(10));
    assertFalse(conditionModel.include(11));

    conditionModel.setOperator(Operator.NOT_EQUAL);
    assertTrue(conditionModel.include(9));
    assertFalse(conditionModel.include(10));
    assertTrue(conditionModel.include(11));

    conditionModel.setLowerBound(10);
    conditionModel.setOperator(Operator.GREATER_THAN_OR_EQUAL);
    assertFalse(conditionModel.include(9));
    assertTrue(conditionModel.include(10));
    assertTrue(conditionModel.include(11));
    conditionModel.setOperator(Operator.GREATER_THAN);
    assertFalse(conditionModel.include(9));
    assertFalse(conditionModel.include(10));
    assertTrue(conditionModel.include(11));

    conditionModel.setUpperBound(10);
    conditionModel.setOperator(Operator.LESS_THAN_OR_EQUAL);
    assertTrue(conditionModel.include(9));
    assertTrue(conditionModel.include(10));
    assertFalse(conditionModel.include(11));
    conditionModel.setOperator(Operator.LESS_THAN);
    assertTrue(conditionModel.include(9));
    assertFalse(conditionModel.include(10));
    assertFalse(conditionModel.include(11));

    conditionModel.setLowerBound(6);
    conditionModel.setOperator(Operator.BETWEEN);
    assertTrue(conditionModel.include(6));
    assertTrue(conditionModel.include(7));
    assertTrue(conditionModel.include(9));
    assertTrue(conditionModel.include(10));
    assertFalse(conditionModel.include(11));
    assertFalse(conditionModel.include(5));
    conditionModel.setOperator(Operator.BETWEEN_EXCLUSIVE);
    assertFalse(conditionModel.include(6));
    assertTrue(conditionModel.include(7));
    assertTrue(conditionModel.include(9));
    assertFalse(conditionModel.include(10));
    assertFalse(conditionModel.include(11));
    assertFalse(conditionModel.include(5));

    conditionModel.setOperator(Operator.NOT_BETWEEN);
    assertTrue(conditionModel.include(6));
    assertFalse(conditionModel.include(7));
    assertFalse(conditionModel.include(9));
    assertTrue(conditionModel.include(10));
    assertTrue(conditionModel.include(11));
    assertTrue(conditionModel.include(5));
    conditionModel.setOperator(Operator.NOT_BETWEEN_EXCLUSIVE);
    assertFalse(conditionModel.include(6));
    assertFalse(conditionModel.include(7));
    assertFalse(conditionModel.include(9));
    assertFalse(conditionModel.include(10));
    assertTrue(conditionModel.include(11));
    assertTrue(conditionModel.include(5));

    conditionModel.setEnabled(false);
    assertTrue(conditionModel.include(5));
    assertTrue(conditionModel.include(6));
    assertTrue(conditionModel.include(7));
  }

  @Test
  public void multiConditionString() {
    final DefaultColumnConditionModel<String, String, String> conditionModel = new DefaultColumnConditionModel<>("test", String.class, "%");

    final Collection<String> strings = asList("abc", "def");
    conditionModel.setEqualValues(strings);

    assertTrue(conditionModel.getEqualValues().containsAll(strings));
  }
}

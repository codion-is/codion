/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.table;

import org.jminor.common.db.ConditionType;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.EventListener;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultColumnConditionModelTest {
  final AtomicInteger upperBoundCounter = new AtomicInteger();
  final AtomicInteger lowerBoundCounter = new AtomicInteger();
  final AtomicInteger conditionStateCounter = new AtomicInteger();
  final AtomicInteger conditionTypeCounter = new AtomicInteger();
  final AtomicInteger enabledCounter = new AtomicInteger();
  final AtomicInteger clearCounter = new AtomicInteger();

  final EventListener upperBoundListener = upperBoundCounter::incrementAndGet;
  final EventListener lowerBoundListener = lowerBoundCounter::incrementAndGet;
  final EventListener conditionStateListener = conditionStateCounter::incrementAndGet;
  final EventDataListener<ConditionType> conditionTypeListener = data -> conditionTypeCounter.incrementAndGet();
  final EventListener enabledListener = enabledCounter::incrementAndGet;
  final EventListener clearListener = clearCounter::incrementAndGet;

  @Test
  public void testSetBounds() {
    final DefaultColumnConditionModel<String> model = new DefaultColumnConditionModel<>("test", String.class, "%");
    model.setAutoEnable(false);
    assertFalse(model.isAutoEnable());
    model.addUpperBoundListener(upperBoundListener);
    model.addLowerBoundListener(lowerBoundListener);
    model.addConditionStateListener(conditionStateListener);
    model.addClearedListener(clearListener);

    model.setUpperBound("hello");
    assertEquals(1, conditionStateCounter.get());
    assertFalse(model.isEnabled());
    assertEquals(1, upperBoundCounter.get());
    assertEquals("hello", model.getUpperBound());
    model.setLowerBound("hello");
    assertEquals(2, conditionStateCounter.get());
    assertEquals(1, lowerBoundCounter.get());
    assertEquals("hello", model.getLowerBound());

    model.setAutomaticWildcard(ColumnConditionModel.AutomaticWildcard.PREFIX_AND_POSTFIX);
    assertEquals("%hello%", model.getUpperBound());
    assertEquals("%hello%", model.getLowerBound());
    model.setAutomaticWildcard(ColumnConditionModel.AutomaticWildcard.NONE);

    model.setLikeValue("test");
    assertEquals(2, upperBoundCounter.get());
    assertEquals("test", model.getUpperBound());

    model.clearCondition();
    assertEquals(1, clearCounter.get());

    model.removeUpperBoundListener(upperBoundListener);
    model.removeLowerBoundListener(lowerBoundListener);
    model.removeConditionStateListener(conditionStateListener);
    model.removeClearedListener(clearListener);
  }

  @Test
  public void testMisc() {
    final ColumnConditionModel model = new DefaultColumnConditionModel("test", String.class, "%");
    assertEquals("test", model.getColumnIdentifier());
    model.setConditionType(ConditionType.LIKE);
    assertFalse(model.isLowerBoundRequired());
    model.setConditionType(ConditionType.GREATER_THAN);
    model.setConditionType(ConditionType.LESS_THAN);

    model.setConditionType(ConditionType.WITHIN_RANGE);
    assertTrue(model.isLowerBoundRequired());

    model.setConditionType(ConditionType.LIKE);
    model.setAutomaticWildcard(ColumnConditionModel.AutomaticWildcard.PREFIX_AND_POSTFIX);
    model.setUpperBound("upper");
    assertEquals("%upper%", model.getUpperBound());
  }

  @Test
  public void testConditionType() {
    final DefaultColumnConditionModel<String> model = new DefaultColumnConditionModel<>("test", String.class, "%");
    model.addConditionTypeListener(conditionTypeListener);
    assertEquals(ConditionType.LIKE, model.getConditionType());
    model.setConditionType(ConditionType.LESS_THAN);
    assertEquals(1, conditionTypeCounter.get());
    assertEquals(ConditionType.LESS_THAN, model.getConditionType());
    try {
      model.setConditionType(null);
      fail();
    }
    catch (final NullPointerException ignored) {/*ignored*/}
    model.setConditionType(ConditionType.OUTSIDE_RANGE);
    assertEquals(2, conditionTypeCounter.get());
    model.removeConditionTypeListener(conditionTypeListener);
  }

  @Test
  public void test() throws Exception {
    final DefaultColumnConditionModel<String> model = new DefaultColumnConditionModel<>("test", String.class, "%");
    assertTrue(model.isAutoEnable());
    model.setUpperBound("test");
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

    assertThrows(IllegalArgumentException.class, () -> model.setLowerBound(1));
    assertThrows(IllegalArgumentException.class, () -> model.setUpperBound(1d));
    assertThrows(IllegalArgumentException.class, () -> model.setLowerBound(asList("2", 1)));
    assertThrows(IllegalArgumentException.class, () -> model.setUpperBound(asList("1", true)));
  }

  @Test
  public void setUpperBoundLocked() {
    final DefaultColumnConditionModel<String> model = new DefaultColumnConditionModel<>("test", String.class, "%");
    model.setLocked(true);
    assertThrows(IllegalStateException.class, () -> model.setUpperBound("test"));
  }

  @Test
  public void setLowerBoundLocked() {
    final DefaultColumnConditionModel<String> model = new DefaultColumnConditionModel<>("test", String.class, "%");
    model.setLocked(true);
    assertThrows(IllegalStateException.class, () -> model.setLowerBound("test"));
  }

  @Test
  public void setEnabledLocked() {
    final DefaultColumnConditionModel<String> model = new DefaultColumnConditionModel<>("test", String.class, "%");
    model.setLocked(true);
    assertThrows(IllegalStateException.class, () -> model.setEnabled(true));
  }

  @Test
  public void setConditionTypeLocked() {
    final DefaultColumnConditionModel<String> model = new DefaultColumnConditionModel<>("test", String.class, "%");
    model.setLocked(true);
    assertThrows(IllegalStateException.class, () -> model.setConditionType(ConditionType.NOT_LIKE));
  }

  @Test
  public void include() {
    final DefaultColumnConditionModel<String> conditionModel = new DefaultColumnConditionModel<>("test", Integer.class, "%");
    conditionModel.setUpperBound(10);
    conditionModel.setConditionType(ConditionType.LIKE);
    assertFalse(conditionModel.include(9));
    assertTrue(conditionModel.include(10));
    assertFalse(conditionModel.include(11));

    conditionModel.setConditionType(ConditionType.NOT_LIKE);
    assertTrue(conditionModel.include(9));
    assertFalse(conditionModel.include(10));
    assertTrue(conditionModel.include(11));

    conditionModel.setConditionType(ConditionType.GREATER_THAN);
    assertFalse(conditionModel.include(9));
    assertTrue(conditionModel.include(10));
    assertTrue(conditionModel.include(11));

    conditionModel.setConditionType(ConditionType.LESS_THAN);
    assertTrue(conditionModel.include(9));
    assertTrue(conditionModel.include(10));
    assertFalse(conditionModel.include(11));

    conditionModel.setConditionType(ConditionType.WITHIN_RANGE);
    conditionModel.setLowerBound(6);
    assertTrue(conditionModel.include(6));
    assertTrue(conditionModel.include(7));
    assertTrue(conditionModel.include(9));
    assertTrue(conditionModel.include(10));
    assertFalse(conditionModel.include(11));
    assertFalse(conditionModel.include(5));

    conditionModel.setConditionType(ConditionType.OUTSIDE_RANGE);
    assertTrue(conditionModel.include(6));
    assertFalse(conditionModel.include(7));
    assertFalse(conditionModel.include(9));
    assertTrue(conditionModel.include(10));
    assertTrue(conditionModel.include(11));
    assertTrue(conditionModel.include(5));

    conditionModel.setEnabled(false);
    assertTrue(conditionModel.include(5));
    assertTrue(conditionModel.include(6));
    assertTrue(conditionModel.include(7));
  }

  @Test
  public void multiConditionString() {
    final DefaultColumnConditionModel<String> conditionModel = new DefaultColumnConditionModel<>("test", String.class, "%");

    final Collection<String> strings = asList("abc", "def");
    conditionModel.setUpperBound(strings);

    assertEquals(strings, conditionModel.getUpperBound());
  }
}

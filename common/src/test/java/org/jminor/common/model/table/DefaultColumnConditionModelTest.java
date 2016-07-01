/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.table;

import org.jminor.common.EventInfoListener;
import org.jminor.common.EventListener;
import org.jminor.common.model.ConditionType;

import org.junit.Test;

import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class DefaultColumnConditionModelTest {
  final AtomicInteger upperBoundCounter = new AtomicInteger();
  final AtomicInteger lowerBoundCounter = new AtomicInteger();
  final AtomicInteger searchStateCounter = new AtomicInteger();
  final AtomicInteger searchTypeCounter = new AtomicInteger();
  final AtomicInteger enabledCounter = new AtomicInteger();
  final AtomicInteger clearCounter = new AtomicInteger();

  final EventListener upperBoundListener = upperBoundCounter::incrementAndGet;
  final EventListener lowerBoundListener = lowerBoundCounter::incrementAndGet;
  final EventListener conditionStateListener = searchStateCounter::incrementAndGet;
  final EventInfoListener<ConditionType> searchTypeListener = info -> searchTypeCounter.incrementAndGet();
  final EventListener enabledListener = enabledCounter::incrementAndGet;
  final EventListener clearListener = clearCounter::incrementAndGet;

  @Test
  public void testSetBounds() {
    final DefaultColumnConditionModel<String> model = new DefaultColumnConditionModel<>("test", Types.VARCHAR, "%");
    model.setAutoEnable(false);
    assertFalse(model.isAutoEnable());
    model.addUpperBoundListener(upperBoundListener);
    model.addLowerBoundListener(lowerBoundListener);
    model.addConditionStateListener(conditionStateListener);
    model.addClearedListener(clearListener);

    model.setUpperBound("hello");
    assertEquals(1, searchStateCounter.get());
    assertFalse(model.isEnabled());
    assertEquals(1, upperBoundCounter.get());
    assertEquals("hello", model.getUpperBound());
    model.setLowerBound("hello");
    assertEquals(2, searchStateCounter.get());
    assertEquals(1, lowerBoundCounter.get());
    assertEquals("hello", model.getLowerBound());

    model.setAutomaticWildcard(true);
    assertEquals("%hello%", model.getUpperBound());
    assertEquals("%hello%", model.getLowerBound());
    model.setAutomaticWildcard(false);

    model.setLikeValue("test");
    assertEquals(2, upperBoundCounter.get());
    assertEquals("test", model.getUpperBound());

    model.setUpperBound(2.2);
    model.setUpperBound(1);
    model.setUpperBound(false);
    model.setUpperBound('c');
    model.setUpperBound(new Date());
    model.setUpperBound(new Timestamp(System.currentTimeMillis()));
    model.setUpperBound(new Object());
    model.setUpperBound(true);

    model.setLowerBound(2.2);
    model.setLowerBound(1);
    model.setLowerBound(false);
    model.setLowerBound('c');
    model.setLowerBound(new Date());
    model.setLowerBound(new Timestamp(System.currentTimeMillis()));
    model.setLowerBound(new Object());
    model.setLowerBound(true);

    model.clearCondition();
    assertEquals(1, clearCounter.get());

    model.removeUpperBoundListener(upperBoundListener);
    model.removeLowerBoundListener(lowerBoundListener);
    model.removeConditionStateListener(conditionStateListener);
    model.removeClearedListener(clearListener);
  }

  @Test
  public void testSearchType() {
    final DefaultColumnConditionModel<String> model = new DefaultColumnConditionModel<>("test", Types.VARCHAR, "%");
    model.addSearchTypeListener(searchTypeListener);
    assertEquals(ConditionType.LIKE, model.getSearchType());
    model.setSearchType(ConditionType.LESS_THAN);
    assertEquals(1, searchTypeCounter.get());
    assertEquals(ConditionType.LESS_THAN, model.getSearchType());
    try {
      model.setSearchType(null);
      fail();
    }
    catch (final NullPointerException ignored) {/*ignored*/}
    model.setSearchType(ConditionType.OUTSIDE_RANGE);
    assertEquals(2, searchTypeCounter.get());
    model.removeSearchTypeListener(searchTypeListener);
  }

  @Test
  public void test() throws Exception {
    final DefaultColumnConditionModel<String> model = new DefaultColumnConditionModel<>("test", Types.VARCHAR, "%");
    assertTrue(model.isAutoEnable());
    model.setUpperBound("test");
    assertTrue(model.isEnabled());
    model.setCaseSensitive(false);
    assertFalse(model.isCaseSensitive());
    assertEquals("test", model.getColumnIdentifier());
    assertEquals(Types.VARCHAR, model.getType());
    assertEquals("%", model.getWildcard());

    model.setWildcard("#");
    assertEquals("#", model.getWildcard());

    model.setAutomaticWildcard(true);
    assertTrue(model.isAutomaticWildcard());

    model.addEnabledListener(enabledListener);
    model.setEnabled(false);
    assertEquals(1, enabledCounter.get());
    model.setEnabled(true);
    assertEquals(2, enabledCounter.get());

    model.removeEnabledListener(enabledListener);

    model.setLocked(true);
    assertTrue(model.isLocked());
    assertTrue(model.getLockedObserver().isActive());
  }

  @Test(expected = IllegalStateException.class)
  public void setUpperBoundLocked() {
    final DefaultColumnConditionModel<String> model = new DefaultColumnConditionModel<>("test", Types.VARCHAR, "%");
    model.setLocked(true);
    model.setUpperBound("test");
  }

  @Test(expected = IllegalStateException.class)
  public void setLowerBoundLocked() {
    final DefaultColumnConditionModel<String> model = new DefaultColumnConditionModel<>("test", Types.VARCHAR, "%");
    model.setLocked(true);
    model.setLowerBound("test");
  }

  @Test(expected = IllegalStateException.class)
  public void setEnabledLocked() {
    final DefaultColumnConditionModel<String> model = new DefaultColumnConditionModel<>("test", Types.VARCHAR, "%");
    model.setLocked(true);
    model.setEnabled(true);
  }

  @Test(expected = IllegalStateException.class)
  public void setSearchTypeLocked() {
    final DefaultColumnConditionModel<String> model = new DefaultColumnConditionModel<>("test", Types.VARCHAR, "%");
    model.setLocked(true);
    model.setSearchType(ConditionType.NOT_LIKE);
  }

  @Test
  public void include() {
    final DefaultColumnConditionModel<String> conditionModel = new DefaultColumnConditionModel<>("test", Types.INTEGER, "%");
    conditionModel.setUpperBound(10);
    conditionModel.setSearchType(ConditionType.LIKE);
    assertFalse(conditionModel.include(9));
    assertTrue(conditionModel.include(10));
    assertFalse(conditionModel.include(11));

    conditionModel.setSearchType(ConditionType.NOT_LIKE);
    assertTrue(conditionModel.include(9));
    assertFalse(conditionModel.include(10));
    assertTrue(conditionModel.include(11));

    conditionModel.setSearchType(ConditionType.GREATER_THAN);
    assertFalse(conditionModel.include(9));
    assertTrue(conditionModel.include(10));
    assertTrue(conditionModel.include(11));

    conditionModel.setSearchType(ConditionType.LESS_THAN);
    assertTrue(conditionModel.include(9));
    assertTrue(conditionModel.include(10));
    assertFalse(conditionModel.include(11));

    conditionModel.setSearchType(ConditionType.WITHIN_RANGE);
    conditionModel.setLowerBound(6);
    assertTrue(conditionModel.include(6));
    assertTrue(conditionModel.include(7));
    assertTrue(conditionModel.include(9));
    assertTrue(conditionModel.include(10));
    assertFalse(conditionModel.include(11));
    assertFalse(conditionModel.include(5));

    conditionModel.setSearchType(ConditionType.OUTSIDE_RANGE);
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
}

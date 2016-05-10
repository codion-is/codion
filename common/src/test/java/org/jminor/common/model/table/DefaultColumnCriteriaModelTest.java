/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.table;

import org.jminor.common.EventInfoListener;
import org.jminor.common.EventListener;
import org.jminor.common.model.SearchType;

import org.junit.Test;

import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class DefaultColumnCriteriaModelTest {
  final AtomicInteger upperBoundCounter = new AtomicInteger();
  final AtomicInteger lowerBoundCounter = new AtomicInteger();
  final AtomicInteger searchStateCounter = new AtomicInteger();
  final AtomicInteger searchTypeCounter = new AtomicInteger();
  final AtomicInteger enabledCounter = new AtomicInteger();
  final AtomicInteger clearCounter = new AtomicInteger();

  final EventListener upperBoundListener = new EventListener() {
    @Override
    public void eventOccurred() {
      upperBoundCounter.incrementAndGet();
    }
  };
  final EventListener lowerBoundListener = new EventListener() {
    @Override
    public void eventOccurred() {
      lowerBoundCounter.incrementAndGet();
    }
  };
  final EventListener criteriaStateListener = new EventListener() {
    @Override
    public void eventOccurred() {
      searchStateCounter.incrementAndGet();
    }
  };
  final EventInfoListener<SearchType> searchTypeListener = new EventInfoListener<SearchType>() {
    @Override
    public void eventOccurred(final SearchType info) {
      searchTypeCounter.incrementAndGet();
    }
  };
  final EventListener enabledListener = new EventListener() {
    @Override
    public void eventOccurred() {
      enabledCounter.incrementAndGet();
    }
  };
  final EventListener clearListener = new EventListener() {
    @Override
    public void eventOccurred() {
      clearCounter.incrementAndGet();
    }
  };

  @Test
  public void testSetBounds() {
    final DefaultColumnCriteriaModel<String> model = new DefaultColumnCriteriaModel<>("test", Types.VARCHAR, "%");
    model.setAutoEnable(false);
    assertFalse(model.isAutoEnable());
    model.addUpperBoundListener(upperBoundListener);
    model.addLowerBoundListener(lowerBoundListener);
    model.addCriteriaStateListener(criteriaStateListener);
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

    model.clearCriteria();
    assertEquals(1, clearCounter.get());

    model.removeUpperBoundListener(upperBoundListener);
    model.removeLowerBoundListener(lowerBoundListener);
    model.removeCriteriaStateListener(criteriaStateListener);
    model.removeClearedListener(clearListener);
  }

  @Test
  public void testSearchType() {
    final DefaultColumnCriteriaModel<String> model = new DefaultColumnCriteriaModel<>("test", Types.VARCHAR, "%");
    model.addSearchTypeListener(searchTypeListener);
    assertEquals(SearchType.LIKE, model.getSearchType());
    model.setSearchType(SearchType.LESS_THAN);
    assertEquals(1, searchTypeCounter.get());
    assertEquals(SearchType.LESS_THAN, model.getSearchType());
    try {
      model.setSearchType(null);
      fail();
    }
    catch (final IllegalArgumentException ignored) {/*ignored*/}
    model.setSearchType(SearchType.OUTSIDE_RANGE);
    assertEquals(2, searchTypeCounter.get());
    model.removeSearchTypeListener(searchTypeListener);
  }

  @Test
  public void test() throws Exception {
    final DefaultColumnCriteriaModel<String> model = new DefaultColumnCriteriaModel<>("test", Types.VARCHAR, "%");
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
    final DefaultColumnCriteriaModel<String> model = new DefaultColumnCriteriaModel<>("test", Types.VARCHAR, "%");
    model.setLocked(true);
    model.setUpperBound("test");
  }

  @Test(expected = IllegalStateException.class)
  public void setLowerBoundLocked() {
    final DefaultColumnCriteriaModel<String> model = new DefaultColumnCriteriaModel<>("test", Types.VARCHAR, "%");
    model.setLocked(true);
    model.setLowerBound("test");
  }

  @Test(expected = IllegalStateException.class)
  public void setEnabledLocked() {
    final DefaultColumnCriteriaModel<String> model = new DefaultColumnCriteriaModel<>("test", Types.VARCHAR, "%");
    model.setLocked(true);
    model.setEnabled(true);
  }

  @Test(expected = IllegalStateException.class)
  public void setSearchTypeLocked() {
    final DefaultColumnCriteriaModel<String> model = new DefaultColumnCriteriaModel<>("test", Types.VARCHAR, "%");
    model.setLocked(true);
    model.setSearchType(SearchType.NOT_LIKE);
  }

  @Test
  public void include() {
    final DefaultColumnCriteriaModel<String> criteriaModel = new DefaultColumnCriteriaModel<>("test", Types.INTEGER, "%");
    criteriaModel.setUpperBound(10);
    criteriaModel.setSearchType(SearchType.LIKE);
    assertFalse(criteriaModel.include(9));
    assertTrue(criteriaModel.include(10));
    assertFalse(criteriaModel.include(11));

    criteriaModel.setSearchType(SearchType.NOT_LIKE);
    assertTrue(criteriaModel.include(9));
    assertFalse(criteriaModel.include(10));
    assertTrue(criteriaModel.include(11));

    criteriaModel.setSearchType(SearchType.GREATER_THAN);
    assertFalse(criteriaModel.include(9));
    assertTrue(criteriaModel.include(10));
    assertTrue(criteriaModel.include(11));

    criteriaModel.setSearchType(SearchType.LESS_THAN);
    assertTrue(criteriaModel.include(9));
    assertTrue(criteriaModel.include(10));
    assertFalse(criteriaModel.include(11));

    criteriaModel.setSearchType(SearchType.WITHIN_RANGE);
    criteriaModel.setLowerBound(6);
    assertTrue(criteriaModel.include(6));
    assertTrue(criteriaModel.include(7));
    assertTrue(criteriaModel.include(9));
    assertTrue(criteriaModel.include(10));
    assertFalse(criteriaModel.include(11));
    assertFalse(criteriaModel.include(5));

    criteriaModel.setSearchType(SearchType.OUTSIDE_RANGE);
    assertTrue(criteriaModel.include(6));
    assertFalse(criteriaModel.include(7));
    assertFalse(criteriaModel.include(9));
    assertTrue(criteriaModel.include(10));
    assertTrue(criteriaModel.include(11));
    assertTrue(criteriaModel.include(5));

    criteriaModel.setEnabled(false);
    assertTrue(criteriaModel.include(5));
    assertTrue(criteriaModel.include(6));
    assertTrue(criteriaModel.include(7));
  }
}

/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.table;

import org.jminor.common.model.EventInfoListener;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.SearchType;

import org.junit.Test;

import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class DefaultColumnSearchModelTest {
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
  final EventListener searchStateListener = new EventListener() {
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
    final DefaultColumnSearchModel<String> model = new DefaultColumnSearchModel<>("test", Types.VARCHAR, "%");
    model.setAutoEnable(false);
    assertFalse(model.isAutoEnable());
    model.addUpperBoundListener(upperBoundListener);
    model.addLowerBoundListener(lowerBoundListener);
    model.addSearchStateListener(searchStateListener);
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
    model.setUpperBound(Boolean.valueOf(true));

    model.setLowerBound(2.2);
    model.setLowerBound(1);
    model.setLowerBound(false);
    model.setLowerBound('c');
    model.setLowerBound(new Date());
    model.setLowerBound(new Timestamp(System.currentTimeMillis()));
    model.setLowerBound(new Object());
    model.setLowerBound(Boolean.valueOf(true));

    model.clearSearch();
    assertEquals(1, clearCounter.get());

    model.removeUpperBoundListener(upperBoundListener);
    model.removeLowerBoundListener(lowerBoundListener);
    model.removeSearchStateListener(searchStateListener);
    model.removeClearedListener(clearListener);
  }

  @Test
  public void testSearchType() {
    final DefaultColumnSearchModel<String> model = new DefaultColumnSearchModel<>("test", Types.VARCHAR, "%");
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
    final DefaultColumnSearchModel<String> model = new DefaultColumnSearchModel<>("test", Types.VARCHAR, "%");
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
    final DefaultColumnSearchModel<String> model = new DefaultColumnSearchModel<>("test", Types.VARCHAR, "%");
    model.setLocked(true);
    model.setUpperBound("test");
  }

  @Test(expected = IllegalStateException.class)
  public void setLowerBoundLocked() {
    final DefaultColumnSearchModel<String> model = new DefaultColumnSearchModel<>("test", Types.VARCHAR, "%");
    model.setLocked(true);
    model.setLowerBound("test");
  }

  @Test(expected = IllegalStateException.class)
  public void setEnabledLocked() {
    final DefaultColumnSearchModel<String> model = new DefaultColumnSearchModel<>("test", Types.VARCHAR, "%");
    model.setLocked(true);
    model.setEnabled(true);
  }

  @Test(expected = IllegalStateException.class)
  public void setSearchTypeLocked() {
    final DefaultColumnSearchModel<String> model = new DefaultColumnSearchModel<>("test", Types.VARCHAR, "%");
    model.setLocked(true);
    model.setSearchType(SearchType.NOT_LIKE);
  }

  @Test
  public void include() {
    final DefaultColumnSearchModel<String> searchModel = new DefaultColumnSearchModel<>("test", Types.INTEGER, "%");
    searchModel.setUpperBound(10);
    searchModel.setSearchType(SearchType.LIKE);
    assertFalse(searchModel.include(9));
    assertTrue(searchModel.include(10));
    assertFalse(searchModel.include(11));

    searchModel.setSearchType(SearchType.NOT_LIKE);
    assertTrue(searchModel.include(9));
    assertFalse(searchModel.include(10));
    assertTrue(searchModel.include(11));

    searchModel.setSearchType(SearchType.GREATER_THAN);
    assertFalse(searchModel.include(9));
    assertTrue(searchModel.include(10));
    assertTrue(searchModel.include(11));

    searchModel.setSearchType(SearchType.LESS_THAN);
    assertTrue(searchModel.include(9));
    assertTrue(searchModel.include(10));
    assertFalse(searchModel.include(11));

    searchModel.setSearchType(SearchType.WITHIN_RANGE);
    searchModel.setLowerBound(6);
    assertTrue(searchModel.include(6));
    assertTrue(searchModel.include(7));
    assertTrue(searchModel.include(9));
    assertTrue(searchModel.include(10));
    assertFalse(searchModel.include(11));
    assertFalse(searchModel.include(5));

    searchModel.setSearchType(SearchType.OUTSIDE_RANGE);
    assertTrue(searchModel.include(6));
    assertFalse(searchModel.include(7));
    assertFalse(searchModel.include(9));
    assertTrue(searchModel.include(10));
    assertTrue(searchModel.include(11));
    assertTrue(searchModel.include(5));

    searchModel.setEnabled(false);
    assertTrue(searchModel.include(5));
    assertTrue(searchModel.include(6));
    assertTrue(searchModel.include(7));
  }
}

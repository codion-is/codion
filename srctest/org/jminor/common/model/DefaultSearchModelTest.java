package org.jminor.common.model;

import static org.junit.Assert.*;
import org.junit.Test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

public class DefaultSearchModelTest {
  final Collection<Object> upperBoundCounter = new ArrayList<Object>();
  final Collection<Object> lowerBoundCounter = new ArrayList<Object>();
  final Collection<Object> searchStateCounter = new ArrayList<Object>();
  final Collection<Object> searchTypeCounter = new ArrayList<Object>();
  final Collection<Object> enabledCounter = new ArrayList<Object>();
  final Collection<Object> clearCounter = new ArrayList<Object>();

  final ActionListener upperBoundListener = new ActionListener() {
    public void actionPerformed(final ActionEvent e) {
      upperBoundCounter.add(new Object());
    }
  };
  final ActionListener lowerBoundListener = new ActionListener() {
    public void actionPerformed(final ActionEvent e) {
      lowerBoundCounter.add(new Object());
    }
  };
  final ActionListener searchStateListener = new ActionListener() {
    public void actionPerformed(final ActionEvent e) {
      searchStateCounter.add(new Object());
    }
  };
  final ActionListener searchTypeListener = new ActionListener() {
    public void actionPerformed(final ActionEvent e) {
      searchTypeCounter.add(new Object());
    }
  };
  final ActionListener enabledListener = new ActionListener() {
    public void actionPerformed(final ActionEvent e) {
      enabledCounter.add(new Object());
    }
  };
  final ActionListener clearListener = new ActionListener() {
    public void actionPerformed(final ActionEvent e) {
      clearCounter.add(new Object());
    }
  };

  @Test
  public void testSetBounds() {
    final DefaultColumnSearchModel<String> model = new DefaultColumnSearchModel<String>("test", Types.VARCHAR, "%");
    model.setAutoEnable(false);
    assertFalse(model.isAutoEnable());
    model.addUpperBoundListener(upperBoundListener);
    model.addLowerBoundListener(lowerBoundListener);
    model.addSearchStateListener(searchStateListener);
    model.addClearedListener(clearListener);

    model.setUpperBound("hello");
    assertEquals(1, searchStateCounter.size());
    assertFalse(model.isEnabled());
    assertEquals(1, upperBoundCounter.size());
    assertEquals("hello", model.getUpperBound());
    model.setLowerBound("hello");
    assertEquals(2, searchStateCounter.size());
    assertEquals(1, lowerBoundCounter.size());
    assertEquals("hello", model.getLowerBound());

    model.setAutomaticWildcard(true);
    assertEquals("%hello%", model.getUpperBound());
    assertEquals("%hello%", model.getLowerBound());
    model.setAutomaticWildcard(false);

    model.setLikeValue("test");
    assertEquals(2, upperBoundCounter.size());
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
    assertEquals(1, clearCounter.size());

    model.removeUpperBoundListener(upperBoundListener);
    model.removeLowerBoundListener(lowerBoundListener);
    model.removeSearchStateListener(searchStateListener);
    model.removeClearedListener(clearListener);
  }

  @Test
  public void testSearchType() {
    final DefaultColumnSearchModel<String> model = new DefaultColumnSearchModel<String>("test", Types.VARCHAR, "%");
    model.addSearchTypeListener(searchTypeListener);
    assertEquals(SearchType.LIKE, model.getSearchType());
    model.setSearchType(SearchType.AT_LEAST);
    assertEquals(1, searchTypeCounter.size());
    assertEquals(SearchType.AT_LEAST, model.getSearchType());
    try {
      model.setSearchType(null);
      fail();
    }
    catch (IllegalArgumentException e) {}
    model.setSearchType(SearchType.OUTSIDE_RANGE);
    assertEquals(2, searchTypeCounter.size());
    model.removeSearchTypeListener(searchTypeListener);
  }

  @Test
  public void test() throws Exception {
    final DefaultColumnSearchModel<String> model = new DefaultColumnSearchModel<String>("test", Types.VARCHAR, "%");
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
    assertEquals(1, enabledCounter.size());
    model.setEnabled(true);
    assertEquals(2, enabledCounter.size());

    model.removeEnabledListener(enabledListener);

    model.setLocked(true);
    assertTrue(model.isLocked());
    assertTrue(model.getLockedState().isActive());

    try {
      model.setUpperBound("test");
      fail("Should not be able to set upper bound in a locked search model");
    }
    catch (IllegalStateException e) {}
    try {
      model.setLowerBound("test");
      fail("Should not be able to set lower bound in a locked search model");
    }
    catch (IllegalStateException e) {}
    try {
      model.setEnabled(true);
      fail("Should not be able to set search enabled in a locked search model");
    }
    catch (IllegalStateException e) {}
    try {
      model.setSearchType(SearchType.NOT_LIKE);
      fail("Should not be able to set search type in a locked search model");
    }
    catch (IllegalStateException e) {}
  }
}

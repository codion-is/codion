/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.model.combobox;

import org.jminor.common.EventInfoListener;
import org.jminor.common.EventListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class SwingFilteredComboBoxModelTest {

  private SwingFilteredComboBoxModel<String> testModel;

  private static final String NULL = "null";
  private static final String ANNA = "anna";
  private static final String KALLI = "kalli";
  private static final String SIGGI = "siggi";
  private static final String TOMAS = "tomas";
  private static final String BJORN = "björn";

  private final ListDataListener listDataListener = new ListDataListener() {
    @Override
    public void intervalAdded(final ListDataEvent e) {}
    @Override
    public void intervalRemoved(final ListDataEvent e) {}
    @Override
    public void contentsChanged(final ListDataEvent e) {}
  };

  @Test
  public void testRefreshClear() {
    testModel.refresh();
    assertEquals(5, testModel.getVisibleItems().size());
    testModel.clear();
    assertEquals(0, testModel.getSize());
    assertTrue(testModel.isCleared());
  }

  @Test
  public void testDataListeners() {
    testModel.addListDataListener(listDataListener);
    testModel.removeListDataListener(listDataListener);
  }

  @Test
  public void testSorting() {
    assertEquals(ANNA + " should be at index 1, got " + testModel.getElementAt(1), ANNA, testModel.getElementAt(1));
    assertEquals(BJORN + " should be at index 2, got " + testModel.getElementAt(2), BJORN, testModel.getElementAt(2));
    assertEquals(KALLI + " should be at index 3, got " + testModel.getElementAt(3), KALLI, testModel.getElementAt(3));
    assertEquals(SIGGI + " should be at index 4, got " + testModel.getElementAt(4), SIGGI, testModel.getElementAt(4));
    assertEquals(TOMAS + " should be at index 5, got " + testModel.getElementAt(5), TOMAS, testModel.getElementAt(5));

    final Comparator<String> comparator = testModel.getSortComparator();
    testModel.setSortComparator(null);
    assertNull(testModel.getSortComparator());
    final List<String> names = new ArrayList<>();
    names.add(ANNA);
    names.add(KALLI);
    names.add(SIGGI);
    names.add(TOMAS);
    names.add(BJORN);
    testModel.setContents(names);

    assertEquals(ANNA + " should be at index 1, got " + testModel.getElementAt(1), ANNA, testModel.getElementAt(1));
    assertEquals(KALLI + " should be at index 2, got " + testModel.getElementAt(2), KALLI, testModel.getElementAt(2));
    assertEquals(SIGGI + " should be at index 3, got " + testModel.getElementAt(3), SIGGI, testModel.getElementAt(3));
    assertEquals(TOMAS + " should be at index 4, got " + testModel.getElementAt(4), TOMAS, testModel.getElementAt(4));
    assertEquals(BJORN + " should be at index 5, got " + testModel.getElementAt(5), BJORN, testModel.getElementAt(5));

    testModel.setSortComparator(comparator);
    names.remove(SIGGI);
    testModel.setContents(names);
    testModel.addItem(SIGGI);

    assertEquals(ANNA + " should be at index 1, got " + testModel.getElementAt(1), ANNA, testModel.getElementAt(1));
    assertEquals(BJORN + " should be at index 2, got " + testModel.getElementAt(2), BJORN, testModel.getElementAt(2));
    assertEquals(KALLI + " should be at index 3, got " + testModel.getElementAt(3), KALLI, testModel.getElementAt(3));
    assertEquals(SIGGI + " should be at index 4, got " + testModel.getElementAt(4), SIGGI, testModel.getElementAt(4));
    assertEquals(TOMAS + " should be at index 5, got " + testModel.getElementAt(5), TOMAS, testModel.getElementAt(5));

    testModel.setSortComparator((o1, o2) -> {
      if (o1 == null) {
        return -1;
      }
      if (o2 == null) {
        return 1;
      }
      return o2.compareTo(o1);
    });
    assertNotNull(testModel.getSortComparator());

    assertEquals(TOMAS + " should be at index 1, got " + testModel.getElementAt(1), TOMAS, testModel.getElementAt(1));
    assertEquals(SIGGI + " should be at index 2, got " + testModel.getElementAt(2), SIGGI, testModel.getElementAt(2));
    assertEquals(KALLI + " should be at index 3, got " + testModel.getElementAt(3), KALLI, testModel.getElementAt(3));
    assertEquals(BJORN + " should be at index 4, got " + testModel.getElementAt(4), BJORN, testModel.getElementAt(4));
    assertEquals(ANNA + " should be at index 5, got " + testModel.getElementAt(5), ANNA, testModel.getElementAt(5));
  }

  @Test
  public void testSelection() {
    final AtomicInteger selectionChangedCounter = new AtomicInteger();
    final EventInfoListener<String> selectionListener = info -> selectionChangedCounter.incrementAndGet();
    testModel.addSelectionListener(selectionListener);
    testModel.setSelectedItem(BJORN);
    assertEquals(1, selectionChangedCounter.get());
    assertEquals(BJORN, testModel.getSelectedItem());
    assertEquals(BJORN, testModel.getSelectedValue());
    assertFalse(testModel.isSelectionEmpty());
    assertFalse(testModel.isNullValueSelected());
    testModel.setSelectedItem(null);
    assertTrue(testModel.isSelectionEmpty());
    assertEquals(2, selectionChangedCounter.get());
    assertEquals(NULL, testModel.getSelectedItem());
    assertTrue(testModel.isNullValueSelected());
    assertTrue(testModel.isSelectionEmpty());
    assertNull(testModel.getSelectedValue());
    testModel.setSelectedItem(SIGGI);
    testModel.clear();
    assertEquals(4, selectionChangedCounter.get());
    testModel.removeSelectionListener(selectionListener);
  }

  @Test
  public void filterWithSelection() {
    testModel.setSelectedItem(BJORN);
    testModel.setFilterCondition(item -> !item.equals(BJORN));
    assertEquals(NULL, testModel.getSelectedItem());
    assertNull(testModel.getSelectedValue());

    testModel.setFilterCondition(null);
    testModel.setFilterSelectedItem(false);
    assertFalse(testModel.isFilterSelectedItem());
    testModel.setSelectedItem(BJORN);
    testModel.setFilterCondition(item -> !item.equals(BJORN));
    assertNotNull(testModel.getSelectedItem());
    assertEquals(BJORN, testModel.getSelectedValue());
  }

  @Test
  public void setFilterCondition() {
    final AtomicInteger filteringEndedCounter = new AtomicInteger();
    final EventListener filteringEndedListener = filteringEndedCounter::incrementAndGet;
    testModel.addListDataListener(listDataListener);
    testModel.addFilteringListener(filteringEndedListener);

    testModel.setFilterCondition(item -> false);
    assertEquals(1, filteringEndedCounter.get());
    assertEquals("The model should only include the null value item", 1, testModel.getSize());
    testModel.setFilterCondition(item -> true);
    assertEquals(2, filteringEndedCounter.get());
    assertEquals("The model should be full", 6, testModel.getSize());
    testModel.setFilterCondition(item -> !item.equals(ANNA));
    assertEquals("The model should contain 5 items", 5, testModel.getSize());
    assertTrue("The model should not contain '" + ANNA + "'", !testModel.isVisible(ANNA));
    assertTrue(testModel.isFiltered(ANNA));
    testModel.setFilterCondition(item -> item.equals(ANNA));
    assertEquals("The model should only contain 2 items", 2, testModel.getSize());
    assertTrue("The model should only contain '" + ANNA + "'", testModel.isVisible(ANNA));

    assertEquals(4, testModel.getFilteredItems().size());
    assertEquals(1, testModel.getVisibleItems().size());
    assertEquals(4, testModel.getFilteredItemCount());
    assertEquals(2, testModel.getVisibleItemCount());
    assertEquals(5, testModel.getAllItems().size());

    testModel.addItem(BJORN);
    assertEquals(5, testModel.getFilteredItemCount());

    assertFalse(testModel.contains(BJORN, false));
    assertTrue(testModel.contains(BJORN, true));

    testModel.removeListDataListener(listDataListener);
    testModel.removeFilteringListener(filteringEndedListener);
  }

  @Test
  public void removeItem() {
    //remove filtered item
    testModel.setFilterCondition(item -> !item.equals(BJORN));
    testModel.removeItem(BJORN);
    testModel.setFilterCondition(null);
    assertFalse(BJORN + " should no longer be in the model", testModel.isVisible(BJORN));

    //remove visible item
    testModel.removeItem(KALLI);
    assertFalse(KALLI + " should no longer be in the model", testModel.isVisible(KALLI));
  }

  @Test
  public void addItem() {
    testModel.clear();
    //add filtered item
    testModel.setFilterCondition(item -> !item.equals(BJORN));
    testModel.addItem(BJORN);
    assertFalse(BJORN + " should be filtered", testModel.isVisible(BJORN));

    //add visible item
    testModel.addItem(KALLI);
    assertTrue(KALLI + " should not be filtered", testModel.isVisible(KALLI));

    testModel.setFilterCondition(null);
    assertTrue(BJORN + " should not be filtered", testModel.isVisible(BJORN));
  }

  @Test
  public void setNullValueString() throws Exception {
    assertTrue(testModel.isVisible(null));
    testModel.refresh();
    assertEquals(5, testModel.getVisibleItems().size());
    assertTrue(testModel.getNullValue().equals(NULL));
    testModel.setSelectedItem(null);
    assertEquals(testModel.getSelectedItem(), NULL);
    assertTrue(testModel.isNullValueSelected());
    assertNull(testModel.getSelectedValue());
    testModel.setSelectedItem(NULL);
    assertEquals(NULL, testModel.getElementAt(0));
    assertEquals(ANNA, testModel.getElementAt(1));
  }

  @Before
  public void setUp() throws Exception {
    testModel = new SwingFilteredComboBoxModel<>();
    testModel.setNullValue(NULL);
    final List<String> names = new ArrayList<>();
    names.add(ANNA);
    names.add(KALLI);
    names.add(SIGGI);
    names.add(TOMAS);
    names.add(BJORN);
    testModel.setContents(names);
  }

  @After
  public void tearDown() throws Exception {
    testModel = null;
  }
}

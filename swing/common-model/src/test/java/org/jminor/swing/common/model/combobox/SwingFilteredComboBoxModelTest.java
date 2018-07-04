/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.model.combobox;

import org.jminor.common.EventDataListener;
import org.jminor.common.EventListener;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

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
    assertEquals(ANNA, testModel.getElementAt(1));
    assertEquals(BJORN, testModel.getElementAt(2));
    assertEquals(KALLI, testModel.getElementAt(3));
    assertEquals(SIGGI, testModel.getElementAt(4));
    assertEquals(TOMAS, testModel.getElementAt(5));

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

    assertEquals(ANNA, testModel.getElementAt(1));
    assertEquals(KALLI, testModel.getElementAt(2));
    assertEquals(SIGGI, testModel.getElementAt(3));
    assertEquals(TOMAS, testModel.getElementAt(4));
    assertEquals(BJORN, testModel.getElementAt(5));

    testModel.setSortComparator(comparator);
    names.remove(SIGGI);
    testModel.setContents(names);
    testModel.addItem(SIGGI);

    assertEquals(ANNA, testModel.getElementAt(1));
    assertEquals(BJORN, testModel.getElementAt(2));
    assertEquals(KALLI, testModel.getElementAt(3));
    assertEquals(SIGGI, testModel.getElementAt(4));
    assertEquals(TOMAS, testModel.getElementAt(5));

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

    assertEquals(TOMAS, testModel.getElementAt(1));
    assertEquals(SIGGI, testModel.getElementAt(2));
    assertEquals(KALLI, testModel.getElementAt(3));
    assertEquals(BJORN, testModel.getElementAt(4));
    assertEquals(ANNA, testModel.getElementAt(5));
  }

  @Test
  public void testSelection() {
    final AtomicInteger selectionChangedCounter = new AtomicInteger();
    final EventDataListener<String> selectionListener = selectedItem -> selectionChangedCounter.incrementAndGet();
    testModel.addSelectionListener(selectionListener);
    testModel.setSelectedItem(BJORN);
    assertEquals(1, selectionChangedCounter.get());
    testModel.setSelectedItem(null);
    assertEquals(2, selectionChangedCounter.get());
    testModel.setSelectedItem(NULL);
    assertEquals(2, selectionChangedCounter.get());
    testModel.setSelectedItem(BJORN);
    assertEquals(3, selectionChangedCounter.get());
    assertEquals(BJORN, testModel.getSelectedItem());
    assertEquals(BJORN, testModel.getSelectedValue());
    assertFalse(testModel.isSelectionEmpty());
    assertFalse(testModel.isNullValueSelected());
    testModel.setSelectedItem(null);
    assertTrue(testModel.isSelectionEmpty());
    assertEquals(4, selectionChangedCounter.get());
    assertEquals(NULL, testModel.getSelectedItem());
    assertTrue(testModel.isNullValueSelected());
    assertTrue(testModel.isSelectionEmpty());
    assertNull(testModel.getSelectedValue());
    testModel.setSelectedItem(SIGGI);
    testModel.clear();
    assertEquals(6, selectionChangedCounter.get());
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
    assertEquals(1, testModel.getSize());
    testModel.setFilterCondition(item -> true);
    assertEquals(2, filteringEndedCounter.get());
    assertEquals(6, testModel.getSize());
    testModel.setFilterCondition(item -> !item.equals(ANNA));
    assertEquals(5, testModel.getSize());
    assertTrue(!testModel.isVisible(ANNA));
    assertTrue(testModel.isFiltered(ANNA));
    testModel.setFilterCondition(item -> item.equals(ANNA));
    assertEquals(2, testModel.getSize());
    assertTrue(testModel.isVisible(ANNA));

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
    assertFalse(testModel.isVisible(BJORN));

    //remove visible item
    testModel.removeItem(KALLI);
    assertFalse(testModel.isVisible(KALLI));
  }

  @Test
  public void addItem() {
    testModel.clear();
    //add filtered item
    testModel.setFilterCondition(item -> !item.equals(BJORN));
    testModel.addItem(BJORN);
    assertFalse(testModel.isVisible(BJORN));

    //add visible item
    testModel.addItem(KALLI);
    assertTrue(testModel.isVisible(KALLI));

    testModel.setFilterCondition(null);
    assertTrue(testModel.isVisible(BJORN));
  }

  @Test
  public void setNullValueString() throws Exception {
    assertTrue(testModel.isVisible(null));
    testModel.refresh();
    assertEquals(5, testModel.getVisibleItems().size());
    assertEquals(testModel.getNullValue(), NULL);
    testModel.setSelectedItem(null);
    assertEquals(testModel.getSelectedItem(), NULL);
    assertTrue(testModel.isNullValueSelected());
    assertNull(testModel.getSelectedValue());
    testModel.setSelectedItem(NULL);
    assertEquals(NULL, testModel.getElementAt(0));
    assertEquals(ANNA, testModel.getElementAt(1));
  }

  @Test
  public void setContentsSelectedItem() {
    class Data {
      final int id;
      String data;

      Data(final int id, final String data) {
        this.id = id;
        this.data = data;
      }

      @Override
      public boolean equals(final Object o) {
        if (this == o) {
          return true;
        }
        if (o == null || getClass() != o.getClass()) {
          return false;
        }

        return id == ((Data) o).id;
      }
    }
    List<Data> contents = Arrays.asList(new Data(1, "1"), new Data(2, "2"), new Data(3, "3"));

    final SwingFilteredComboBoxModel<Data> model = new SwingFilteredComboBoxModel();
    model.setContents(contents);
    model.setSelectedItem(contents.get(1));
    assertEquals("2", model.getSelectedValue().data);

    contents = Arrays.asList(new Data(1, "1"), new Data(2, "22"), new Data(3, "3"));

    model.setContents(contents);
    assertEquals("22", model.getSelectedValue().data);
  }

  @BeforeEach
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

  @AfterEach
  public void tearDown() throws Exception {
    testModel = null;
  }
}

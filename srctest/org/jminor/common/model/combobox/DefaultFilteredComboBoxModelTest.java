/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.combobox;

import org.jminor.common.model.FilterCriteria;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

public class DefaultFilteredComboBoxModelTest {

  private FilteredComboBoxModel<String> testModel;

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
  }

  @Test
  public void testSelection() {
    final Collection<Object> selectionChangedCounter = new ArrayList<Object>();
    final ActionListener selectionListener = new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        selectionChangedCounter.add(new Object());
      }
    };
    testModel.addSelectionListener(selectionListener);
    testModel.setSelectedItem(BJORN);
    assertEquals(1, selectionChangedCounter.size());
    assertEquals(BJORN, testModel.getSelectedItem());
    assertEquals(BJORN, testModel.getSelectedValue());
    testModel.setSelectedItem(null);
    assertEquals(2, selectionChangedCounter.size());
    assertEquals(NULL, testModel.getSelectedItem());
    assertNull(testModel.getSelectedValue());
    testModel.removeSelectionListener(selectionListener);
  }

  @Test
  public void filterWithSelection() {
    testModel.setSelectedItem(BJORN);
    testModel.setFilterCriteria(new FilterCriteria<String>() {
      @Override
      public boolean include(final String item) {
        return !item.equals(BJORN);
      }
    });
    assertEquals(NULL, testModel.getSelectedItem());
    assertNull(testModel.getSelectedValue());

    testModel.setFilterCriteria(null);
    testModel.setFilterSelectedItem(false);
    testModel.setSelectedItem(BJORN);
    testModel.setFilterCriteria(new FilterCriteria<String>() {
      @Override
      public boolean include(final String item) {
        return !item.equals(BJORN);
      }
    });
    assertNotNull(testModel.getSelectedItem());
    assertEquals(BJORN, testModel.getSelectedValue());
  }

  @Test
  public void setFilterCriteria() {
    final Collection<Object> filteringEndedCounter = new ArrayList<Object>();
    final ActionListener filteringEndedListener = new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        filteringEndedCounter.add(new Object());
      }
    };
    testModel.addListDataListener(listDataListener);
    testModel.addFilteringListener(filteringEndedListener);

    testModel.setFilterCriteria(new FilterCriteria<String>() {
      @Override
      public boolean include(final String item) {
        return false;
      }
    });
    assertEquals(1, filteringEndedCounter.size());
    assertEquals("The model should only include the null value item", 1, testModel.getSize());
    testModel.setFilterCriteria(new FilterCriteria<String>() {
      @Override
      public boolean include(final String item) {
        return true;
      }
    });
    assertEquals(2, filteringEndedCounter.size());
    assertEquals("The model should be full", 6, testModel.getSize());
    testModel.setFilterCriteria(new FilterCriteria<String>() {
      @Override
      public boolean include(final String item) {
        return !item.equals(ANNA);
      }
    });
    assertEquals("The model should contain 5 items", 5, testModel.getSize());
    assertTrue("The model should not contain '" + ANNA + "'", !testModel.isVisible(ANNA));
    assertTrue(testModel.isFiltered(ANNA));
    testModel.setFilterCriteria(new FilterCriteria<String>() {
      @Override
      public boolean include(final String item) {
        return item.equals(ANNA);
      }
    });
    assertEquals("The model should only contain 2 items", 2, testModel.getSize());
    assertTrue("The model should only contain '" + ANNA + "'", testModel.isVisible(ANNA));

    assertEquals(4, testModel.getFilteredItems().size());
    assertEquals(1, testModel.getVisibleItems().size());
    assertEquals(4, testModel.getFilteredItemCount());
    assertEquals(2, testModel.getVisibleItemCount());
    assertEquals(6, testModel.getAllItems().size());

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
    testModel.setFilterCriteria(new FilterCriteria<String>() {
      @Override
      public boolean include(final String item) {
        return !item.equals(BJORN);
      }
    });
    testModel.removeItem(BJORN);
    testModel.setFilterCriteria(null);
    assertFalse(BJORN + " should no longer be in the model", testModel.isVisible(BJORN));

    //remove visible item
    testModel.removeItem(KALLI);
    assertFalse(KALLI + " should no longer be in the model", testModel.isVisible(KALLI));
  }

  @Test
  public void setNullValueString() throws Exception {
    assertTrue(testModel.isVisible(null));
    testModel.refresh();
    assertEquals(5, testModel.getVisibleItems().size());
    assertTrue(testModel.getNullValueString().equals(NULL));
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
    testModel = new DefaultFilteredComboBoxModel<String>();
    testModel.setNullValueString(NULL);
    final List<String> names = new ArrayList<String>();
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

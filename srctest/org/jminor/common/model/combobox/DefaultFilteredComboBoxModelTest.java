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

  private static final String ANNA = "anna";
  private static final String KALLI = "kalli";
  private static final String SIGGI = "siggi";
  private static final String TOMAS = "tomas";
  private static final String BJORN = "björn";

  private final ListDataListener listDataListener = new ListDataListener() {
    public void intervalAdded(final ListDataEvent e) {}
    public void intervalRemoved(final ListDataEvent e) {}
    public void contentsChanged(final ListDataEvent e) {}
  };

  @Test
  public void testRefreshClear() {
    testModel.refresh();
    assertEquals(Integer.valueOf(5), (Integer) testModel.getVisibleItems().size());
    testModel.clear();
    assertTrue(testModel.getSize() == 0);
    assertTrue(testModel.isCleared());
  }

  @Test
  public void testDataListeners() {
    testModel.addListDataListener(listDataListener);
    testModel.removeListDataListener(listDataListener);
  }

  @Test
  public void testSorting() {
    assertTrue(ANNA + " should be at index 0, got " + testModel.getElementAt(0), testModel.getElementAt(0).equals(ANNA));
    assertTrue(BJORN + " should be at index 1, got " + testModel.getElementAt(1), testModel.getElementAt(1).equals(BJORN));
    assertTrue(KALLI + " should be at index 2, got " + testModel.getElementAt(2), testModel.getElementAt(2).equals(KALLI));
    assertTrue(SIGGI + " should be at index 3, got " + testModel.getElementAt(3), testModel.getElementAt(3).equals(SIGGI));
    assertTrue(TOMAS + " should be at index 4, got " + testModel.getElementAt(4), testModel.getElementAt(4).equals(TOMAS));
  }

  @Test
  public void testSelection() {
    final Collection<Object> selectionChangedCounter = new ArrayList<Object>();
    final ActionListener selectionListener = new ActionListener() {
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
    assertNull(testModel.getSelectedItem());
    assertNull(testModel.getSelectedValue());
    testModel.removeSelectionListener(selectionListener);
  }

  @Test
  public void filterWithSelection() {
    testModel.setSelectedItem(BJORN);
    testModel.setFilterCriteria(new FilterCriteria<String>() {
      public boolean include(final String item) {
        return !item.equals(BJORN);
      }
    });
    assertNull(testModel.getSelectedItem());

    testModel.setFilterCriteria(null);
    testModel.setFilterSelectedItem(false);
    testModel.setSelectedItem(BJORN);
    testModel.setFilterCriteria(new FilterCriteria<String>() {
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
      public void actionPerformed(final ActionEvent e) {
        filteringEndedCounter.add(new Object());
      }
    };
    testModel.addListDataListener(listDataListener);
    testModel.addFilteringListener(filteringEndedListener);

    testModel.setFilterCriteria(new FilterCriteria<String>() {
      public boolean include(final String item) {
        return false;
      }
    });
    assertEquals(1, filteringEndedCounter.size());
    assertTrue("The model should be empty", testModel.getSize() == 0);
    testModel.setFilterCriteria(new FilterCriteria<String>() {
      public boolean include(final String item) {
        return true;
      }
    });
    assertEquals(2, filteringEndedCounter.size());
    assertTrue("The model should be full", testModel.getSize() == 5);
    testModel.setFilterCriteria(new FilterCriteria<String>() {
      public boolean include(final String item) {
        return !item.equals(ANNA);
      }
    });
    assertTrue("The model should contain 4 items", testModel.getSize() == 4);
    assertTrue("The model should not contain '" + ANNA + "'", !testModel.isVisible(ANNA));
    assertTrue(testModel.isFiltered(ANNA));
    testModel.setFilterCriteria(new FilterCriteria<String>() {
      public boolean include(final String item) {
        return item.equals(ANNA);
      }
    });
    assertTrue("The model should only contain 1 item", testModel.getSize() == 1);
    assertTrue("The model should only contain '" + ANNA + "'", testModel.isVisible(ANNA));

    assertTrue(testModel.getFilteredItems().size() == 4);
    assertTrue(testModel.getVisibleItems().size() == 1);
    assertTrue(testModel.getFilteredItemCount() == 4);
    assertTrue(testModel.getVisibleItemCount() == 1);
    assertTrue(testModel.getAllItems().size() == 5);

    testModel.addItem(BJORN);
    assertTrue(testModel.getFilteredItemCount() == 5);

    assertFalse(testModel.contains(BJORN, false));
    assertTrue(testModel.contains(BJORN, true));

    testModel.removeListDataListener(listDataListener);
    testModel.removeFilteringListener(filteringEndedListener);
  }

  @Test
  public void removeItem() {
    //remove filtered item
    testModel.setFilterCriteria(new FilterCriteria<String>() {
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
    testModel.addItem(null);
    final String nullValueString = "nullValueString";
    testModel.setNullValueString(nullValueString);
    assertTrue(testModel.isVisible(null));
    testModel.refresh();
    assertEquals(Integer.valueOf(4), (Integer) testModel.getVisibleItems().size());//sublist 1 ... due to nullValueString
    assertTrue(testModel.getNullValueString().equals(nullValueString));
    testModel.setSelectedItem(null);
    assertEquals(testModel.getSelectedItem(), nullValueString);
    assertTrue(testModel.isNullValueSelected());
    assertNull(testModel.getSelectedValue());
    testModel.setSelectedItem(nullValueString);
    assertEquals(nullValueString, testModel.getElementAt(0));
    assertEquals(ANNA, testModel.getElementAt(1));
  }

  @Before
  public void setUp() throws Exception {
    testModel = new DefaultFilteredComboBoxModel<String>();
    testModel.setContents(initContents());
  }

  @After
  public void tearDown() throws Exception {
    testModel = null;
  }

  private List<String> initContents() {
    final List<String> names = new ArrayList<String>();
    names.add(ANNA);
    names.add(KALLI);
    names.add(SIGGI);
    names.add(TOMAS);
    names.add(BJORN);

    return names;
  }
}

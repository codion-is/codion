/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.combobox;

import org.jminor.common.model.FilterCriteria;

import junit.framework.TestCase;

import java.util.Vector;

public class FilteredComboBoxModelTest extends TestCase {

  private FilteredComboBoxModel testModel;

  private static final String ANNA = "anna";
  private static final String KALLI = "kalli";
  private static final String SIGGI = "siggi";
  private static final String TOMAS = "tomas";
  private static final String BJORN = "björn";

  public void testSelection() {
    testModel.setEmptyStringIsNull(false);
    testModel.setSelectedItem("");
    assertFalse(testModel.isNullValueItemSelected());
    testModel.setEmptyStringIsNull(true);
    assertTrue(testModel.isNullValueItemSelected());
  }

  public void testSort() {
    testModel.setSortContents(true);
    assertTrue(ANNA + " should be at index 0, got " + testModel.getElementAt(0), testModel.getElementAt(0).equals(ANNA));
    assertTrue(BJORN + " should be at index 1, got " + testModel.getElementAt(1), testModel.getElementAt(1).equals(BJORN));
    assertTrue(KALLI + " should be at index 2, got " + testModel.getElementAt(2), testModel.getElementAt(2).equals(KALLI));
    assertTrue(SIGGI + " should be at index 3, got " + testModel.getElementAt(3), testModel.getElementAt(3).equals(SIGGI));
    assertTrue(TOMAS + " should be at index 4, got " + testModel.getElementAt(4), testModel.getElementAt(4).equals(TOMAS));
  }

  public void testFiltering() {
    testModel.setFilterCriteria(new FilterCriteria() {
      public boolean include(Object item) {
        return false;
      }
    });
    assertTrue("The model should be empty", testModel.getSize() == 0);
    testModel.setFilterCriteria(new FilterCriteria() {
      public boolean include(Object item) {
        return true;
      }
    });
    assertTrue("The model should be full", testModel.getSize() == 5);
    testModel.setFilterCriteria(new FilterCriteria() {
      public boolean include(Object item) {
        return !item.equals(ANNA);
      }
    });
    assertTrue("The model should contain 4 items", testModel.getSize() == 4);
    assertTrue("The model should not contain '" + ANNA + "'", !modelContains(ANNA));
    testModel.setFilterCriteria(new FilterCriteria() {
      public boolean include(Object item) {
        return item.equals(ANNA);
      }
    });
    assertTrue("The model should only contain 1 item", testModel.getSize() == 1);
    assertTrue("The mopel should only contain '" + ANNA + "'", modelContains(ANNA));
  }

  public void testRemove() {
    //remove filtered item
    testModel.setFilterCriteria(new FilterCriteria() {
      public boolean include(Object item) {
        return !item.equals(BJORN);
      }
    });
    testModel.removeItem(BJORN);
    testModel.setFilterCriteria(null);
    assertFalse(BJORN + " should no longer be in the model", modelContains(BJORN));

    //remove visible item
    testModel.removeItem(KALLI);
    assertFalse(KALLI + " should no longer be in the model", modelContains(KALLI));
  }

  /** {@inheritDoc} */
  @Override
  protected void setUp() throws Exception {
    testModel = new FilteredComboBoxModel();
    testModel.setContents(initContents());
  }

  /** {@inheritDoc} */
  @Override
  protected void tearDown() throws Exception {
    testModel = null;
  }

  private boolean modelContains(final String s) {
    for (int i = 0; i < testModel.getSize(); i++)
      if (testModel.getElementAt(i).equals(s))
        return true;

    return false;
  }

  private Vector<String> initContents() {
    final Vector<String> names = new Vector<String>();
    names.add(ANNA);
    names.add(KALLI);
    names.add(SIGGI);
    names.add(TOMAS);
    names.add(BJORN);

    return names;
  }
}

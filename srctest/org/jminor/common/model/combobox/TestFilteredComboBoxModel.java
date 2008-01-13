/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.combobox;

import org.jminor.common.model.IFilterCriteria;

import junit.framework.TestCase;

import java.util.Vector;

public class TestFilteredComboBoxModel extends TestCase {

  private FilteredComboBoxModel testModel;

  private static final String ANNA = "anna";
  private static final String KALLI = "kalli";
  private static final String SIGGI = "siggi";
  private static final String TOMAS = "tomas";
  private static final String BJORN = "björn";

  public TestFilteredComboBoxModel(String name) {
    super(name);
  }

  public void testSort() {
    testModel.setSortContents(true);
    assertTrue(ANNA + " a að vera nr. 0", testModel.getElementAt(0).equals(ANNA));
    assertTrue(BJORN + " a að vera nr. 1", testModel.getElementAt(1).equals(BJORN));
    assertTrue(KALLI + " a að vera nr. 2", testModel.getElementAt(2).equals(KALLI));
    assertTrue(SIGGI + " a að vera nr. 3", testModel.getElementAt(3).equals(SIGGI));
    assertTrue(TOMAS + " a að vera nr. 4", testModel.getElementAt(4).equals(TOMAS));
  }

  public void testFiltering() {
    testModel.setFilterCriteria(new IFilterCriteria() {
      public boolean include(Object item) {
        return false;
      }
    });
    assertTrue("Modelið a að vera tomt", testModel.getSize() == 0);
    testModel.setFilterCriteria(new IFilterCriteria() {
      public boolean include(Object item) {
        return true;
      }
    });
    assertTrue("Modelið a að vera fullt", testModel.getSize() == 5);
    testModel.setFilterCriteria(new IFilterCriteria() {
      public boolean include(Object item) {
        return !item.equals(ANNA);
      }
    });
    assertTrue("Modelið a að innihalda 4 stk", testModel.getSize() == 4);
    assertTrue("Modelið a ekki að innihalda '" + ANNA + "'", !modelContains(ANNA));
    testModel.setFilterCriteria(new IFilterCriteria() {
      public boolean include(Object item) {
        return item.equals(ANNA);
      }
    });
    assertTrue("Mödelið a að innihalda 1 stk", testModel.getSize() == 1);
    assertTrue("Mödelið a einungis að innihalda '" + ANNA + "'", modelContains(ANNA));
  }

  public void testRemove() {
    //fjarlÃ¦gja filteraÃ° item
    testModel.setFilterCriteria(new IFilterCriteria() {
      public boolean include(Object item) {
        return !item.equals(BJORN);
      }
    });
    testModel.removeItem(BJORN);
    testModel.setFilterCriteria(null);
    assertFalse(BJORN + " a ekki að vera lengur i modelinu", modelContains(BJORN));

    //fjarlÃ¦gja sÃ½nilegt item
    testModel.removeItem(KALLI);
    assertFalse(KALLI + " a ekki að vera lengur i modelinu", modelContains(KALLI));
  }

  /** {@inheritDoc} */
  protected void setUp() throws Exception {
    testModel = new FilteredComboBoxModel();
    testModel.setContents(initContents());
  }

  /** {@inheritDoc} */
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
    final Vector<String> ret = new Vector<String>();
    ret.add(ANNA);
    ret.add(KALLI);
    ret.add(SIGGI);
    ret.add(TOMAS);
    ret.add(BJORN);

    return ret;
  }
}

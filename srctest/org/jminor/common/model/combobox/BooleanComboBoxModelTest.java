/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.combobox;

import org.jminor.common.model.Item;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class BooleanComboBoxModelTest {

  @Test
  public void test() throws Exception {
    final BooleanComboBoxModel model = new BooleanComboBoxModel();

    model.setSelectedItem(false);
    assertEquals("BooleanComboBoxModel should accept false", false, ((Item) model.getSelectedItem()).getItem());
    model.setSelectedItem(true);
    assertEquals("BooleanComboBoxModel should accept true", true, ((Item) model.getSelectedItem()).getItem());
    model.setSelectedItem(null);
    assertNull("BooleanComboBoxModel should accept null", ((Item) model.getSelectedItem()).getItem());
  }
}

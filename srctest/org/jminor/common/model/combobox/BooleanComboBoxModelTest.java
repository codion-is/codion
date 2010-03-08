/*
 * Copyright (c) 2009, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.combobox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class BooleanComboBoxModelTest {

  @Test
  public void test() throws Exception {
    final BooleanComboBoxModel model = new BooleanComboBoxModel();

    model.setSelectedItem(false);
    assertEquals("BooleanComboBoxModel should accept false", false, ((ItemComboBoxModel.Item) model.getSelectedItem()).getItem());
    model.setSelectedItem(true);
    assertEquals("BooleanComboBoxModel should accept true", true, ((ItemComboBoxModel.Item) model.getSelectedItem()).getItem());
    model.setSelectedItem(null);
    assertNull("BooleanComboBoxModel should accept null", ((ItemComboBoxModel.Item) model.getSelectedItem()).getItem());
  }
}

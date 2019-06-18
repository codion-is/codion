/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.model.combobox;

import org.jminor.common.Item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class BooleanComboBoxModelTest {

  @Test
  public void test() throws Exception {
    final BooleanComboBoxModel model = new BooleanComboBoxModel();

    model.setSelectedItem(false);
    assertEquals(false, ((Item) model.getSelectedItem()).getValue());
    model.setSelectedItem(true);
    assertEquals(true, ((Item) model.getSelectedItem()).getValue());
    model.setSelectedItem(null);
    assertNull(((Item) model.getSelectedItem()).getValue());
  }
}

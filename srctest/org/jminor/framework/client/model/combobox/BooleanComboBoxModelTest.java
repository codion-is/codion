package org.jminor.framework.client.model.combobox;

import org.jminor.framework.model.Type;

import junit.framework.TestCase;

public class BooleanComboBoxModelTest extends TestCase {

  public void test() throws Exception {
    final BooleanComboBoxModel model = new BooleanComboBoxModel();

    model.setSelectedItem(false);
    assertEquals("BooleanComboBoxModel should accept false", Type.Boolean.FALSE, model.getSelectedItem());
    model.setSelectedItem(true);
    assertEquals("BooleanComboBoxModel should accept true", Type.Boolean.TRUE, model.getSelectedItem());

    model.setSelectedItem(null);
    assertEquals("BooleanComboBoxModel should accept null", Type.Boolean.NULL, model.getSelectedItem());
  }
}

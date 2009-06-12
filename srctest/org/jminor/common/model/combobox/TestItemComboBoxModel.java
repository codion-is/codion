package org.jminor.common.model.combobox;

import junit.framework.TestCase;

public class TestItemComboBoxModel extends TestCase {

  public TestItemComboBoxModel() {
    super("TestItemComboBoxModel");
  }

  public void test() throws Exception {
    final ItemComboBoxModel.Item[] items = new ItemComboBoxModel.Item[] {
            new ItemComboBoxModel.Item<Integer>(1, "One"),
            new ItemComboBoxModel.Item<Integer>(2, "Two"),
            new ItemComboBoxModel.Item<Integer>(3, "Three"),
            new ItemComboBoxModel.Item<Integer>(4, "Four")};
    final ItemComboBoxModel model = new ItemComboBoxModel(items);

    assertEquals("The item representing 1 should be at index 0", 0, model.getIndexOfItem(1));
    assertEquals("The item representing 2 should be at index 1", 1, model.getIndexOfItem(2));
    assertEquals("The item representing 3 should be at index 2", 2, model.getIndexOfItem(3));
    assertEquals("The item representing 4 should be at index 3", 3, model.getIndexOfItem(4));

    model.setSelectedItem(1);
    assertTrue("The item representing 1 should be selected", model.getSelectedItem().equals(items[0]));
    model.setSelectedItem(2);
    assertTrue("The item representing 2 should be selected", model.getSelectedItem().equals(items[1]));
    model.setSelectedItem(4);
    assertTrue("The item representing 4 should be selected", model.getSelectedItem().equals(items[3]));
  }
}

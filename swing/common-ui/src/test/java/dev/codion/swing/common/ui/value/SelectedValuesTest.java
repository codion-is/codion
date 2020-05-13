/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.common.ui.value;

import dev.codion.common.event.Event;
import dev.codion.common.event.Events;
import dev.codion.common.item.Item;
import dev.codion.common.value.Value;
import dev.codion.common.value.Values;
import dev.codion.swing.common.model.combobox.ItemComboBoxModel;

import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import java.util.List;

import static java.util.Arrays.asList;
import static dev.codion.common.item.Items.item;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SelectedValuesTest {

  private String selectedItem;
  private final Event selectedItemChangedEvent = Events.event();

  @Test
  public void selectedItemValueLink() throws Exception {
    final JComboBox box = new JComboBox(new String[] {"b", "d", "s"});
    Values.propertyValue(this, "selectedItem", String.class, selectedItemChangedEvent).link(SelectedValues.selectedValue(box));
    assertNull(selectedItem);
    setSelectedItem("s");
    assertEquals("s", box.getSelectedItem());
    box.setSelectedItem("d");
    assertEquals("d", selectedItem);
  }

  @Test
  public void selectedItemValue() {
    final List<Item<String>> items = asList(item(null), item("one"),
            item("two"), item("three"), item("four"));
    ComponentValue<String, JComboBox<Item<String>>> componentValue = SelectedValues.selectedItemValue("two", items);
    ItemComboBoxModel<String> boxModel = (ItemComboBoxModel<String>) componentValue.getComponent().getModel();
    assertEquals(5, boxModel.getSize());
    assertEquals("two", componentValue.get());

    componentValue = SelectedValues.selectedItemValue(null, items);
    boxModel = (ItemComboBoxModel<String>) componentValue.getComponent().getModel();
    assertEquals(5, boxModel.getSize());
    assertNull(componentValue.get());
  }

  @Test
  public void selectedValue() {
    final JComboBox box = new JComboBox(new String[] {null, "one", "two", "three"});
    final Value<Object> value = SelectedValues.selectedValue(box);

    assertNull(value.get());
    box.setSelectedIndex(1);
    assertEquals("one", box.getSelectedItem());
    box.setSelectedIndex(2);
    assertEquals("two", box.getSelectedItem());
    box.setSelectedIndex(3);
    assertEquals("three", box.getSelectedItem());
    box.setSelectedIndex(0);
    assertNull(value.get());

    value.set("two");
    assertEquals("two", box.getSelectedItem());
  }

  public String getSelectedItem() {
    return selectedItem;
  }

  public void setSelectedItem(final String selectedItem) {
    this.selectedItem = selectedItem;
    selectedItemChangedEvent.onEvent();
  }
}

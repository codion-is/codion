/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.common.event.Event;
import is.codion.common.event.Events;
import is.codion.common.item.Item;
import is.codion.common.value.Value;
import is.codion.common.value.Values;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;

import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import java.util.List;

import static is.codion.common.item.Items.item;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SelectedValuesTest {

  private String selectedItem;
  private final Event<String> selectedItemChangedEvent = Events.event();

  @Test
  public void selectedItemValueLinkValidate() throws Exception {
    final Value<String> originalValue = Values.value("b");
    originalValue.setValidator(value -> {
      if (value != null && value.equals("s")) {
        throw new IllegalArgumentException();
      }
    });
    final JComboBox<String> box = new JComboBox<>(new String[] {"b", "d", "s"});
    SelectedValues.selectedValue(box).link(originalValue);

    assertEquals("b", box.getSelectedItem());
    box.setSelectedItem("d");
    assertEquals("d", box.getSelectedItem());
    box.setSelectedItem("s");
    assertEquals("d", box.getSelectedItem());
  }

  @Test
  public void selectedItemValueLink() throws Exception {
    final JComboBox<String> box = new JComboBox<>(new String[] {"b", "d", "s"});
    SelectedValues.selectedValue(box).link(Values.propertyValue(this, "selectedItem", String.class, selectedItemChangedEvent));
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
    final JComboBox<String> box = new JComboBox<>(new String[] {null, "one", "two", "three"});
    final Value<String> value = SelectedValues.selectedValue(box);

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

/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.junit.jupiter.api.Test;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import java.util.List;

import static is.codion.common.item.Item.item;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class SelectedValuesTest {

  @Test
  void selectedItemValueLinkValidate() {
    Value<String> originalValue = Value.value("b");
    originalValue.addValidator(value -> {
      if (value != null && value.equals("s")) {
        throw new IllegalArgumentException();
      }
    });
    ComponentValue<String, JComboBox<String>> componentValue = Components.comboBox(new DefaultComboBoxModel<>(new String[] {"b", "d", "s"}), originalValue)
            .buildValue();
    JComboBox<String> box = componentValue.component();

    assertEquals("b", box.getSelectedItem());
    box.setSelectedItem("d");
    assertEquals("d", box.getSelectedItem());
    assertThrows(IllegalArgumentException.class, () -> box.setSelectedItem("s"));
    assertEquals("d", box.getSelectedItem());
  }

  @Test
  void selectedItemValueLink() {
    Value<String> value = Value.value();
    ComponentValue<String, JComboBox<String>> componentValue = Components.comboBox(new DefaultComboBoxModel<>(new String[] {"b", "d", "s"}),
                    value)
            .buildValue();
    JComboBox<String> box = componentValue.component();

    assertNull(value.get());
    value.set("s");
    assertEquals("s", box.getSelectedItem());
    box.setSelectedItem("d");
    assertEquals("d", value.get());
  }

  @Test
  void selectedItemValue() {
    List<Item<String>> items = asList(item(null), item("one"),
            item("two"), item("three"), item("four"));
    ItemComboBoxModel<String> boxModel = ItemComboBoxModel.itemComboBoxModel(items);
    boxModel.setSelectedItem("two");
    ComponentValue<String, JComboBox<Item<String>>> componentValue = Components.itemComboBox(boxModel)
            .buildValue();
    assertEquals(5, boxModel.getSize());
    assertEquals("two", componentValue.get());

    boxModel = ItemComboBoxModel.itemComboBoxModel(items);
    componentValue = Components.itemComboBox(boxModel)
            .buildValue();
    assertEquals(5, boxModel.getSize());
    assertNull(componentValue.get());
  }

  @Test
  void selectedValue() {
    ComponentValue<String, JComboBox<String>> value = Components.comboBox(new DefaultComboBoxModel<>(new String[] {null, "one", "two", "three"}))
            .buildValue();
    JComboBox<String> box = value.component();

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
}

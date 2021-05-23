/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;
import is.codion.common.value.Value;
import is.codion.swing.common.model.combobox.BooleanComboBoxModel;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;
import is.codion.swing.common.ui.textfield.TextInputPanel;
import is.codion.swing.common.ui.value.UpdateOn;

import org.junit.jupiter.api.Test;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.util.List;

import static is.codion.common.item.Item.item;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public final class ComponentBuildersTest {

  @Test
  public void createCheckBox() {
    final Value<Boolean> value = Value.value(true, false);
    final JCheckBox box = ComponentBuilders.checkBoxBuilder(value)
            .transferFocusOnEnter(true).build();
    assertTrue(box.isSelected());
    assertTrue(value.get());

    box.doClick();

    assertFalse(box.isSelected());
    assertFalse(value.get());

    value.set(true);
    assertTrue(box.isSelected());
  }

  @Test
  public void createNullableCheckBox() {
    final Value<Boolean> value = Value.value(true);
    final NullableCheckBox box = (NullableCheckBox) ComponentBuilders.checkBoxBuilder(value)
            .transferFocusOnEnter(true).nullable(true).build();
    assertTrue(box.isSelected());
    assertTrue(value.get());

    box.getMouseListeners()[1].mouseClicked(null);

    assertNull(box.getState());
    assertNull(value.get());

    value.set(false);
    assertFalse(box.isSelected());
  }

  @Test
  public void createBooleanComboBox() {
    final Value<Boolean> value = Value.value(true);
    final BooleanComboBoxModel boxModel = (BooleanComboBoxModel)
            ComponentBuilders.booleanComboBoxBuilder(value)
                    .transferFocusOnEnter(true).build().getModel();
    assertTrue(boxModel.getSelectedValue().getValue());
    boxModel.setSelectedItem(null);
    assertNull(value.get());

    value.set(false);
    assertFalse(boxModel.getSelectedValue().getValue());
  }

  @Test
  public void createValueListComboBox() {
    final List<Item<Integer>> items = asList(item(0, "0"), item(1, "1"),
          item(2, "2"), item(3, "3"));
    final Value<Integer> value = Value.value();
    final JComboBox<Item<Integer>> comboBox = ComponentBuilders.valueListComboBoxBuilder(value, items)
            .transferFocusOnEnter(true)
            .nullable(true).build();
    final ItemComboBoxModel<Integer> model = (ItemComboBoxModel<Integer>) comboBox.getModel();
    assertEquals(0, model.indexOf(null));
    assertTrue(model.containsItem(Item.item(null)));

    assertNull(value.get());
    comboBox.setSelectedItem(1);
    assertEquals(1, value.get());
    comboBox.setSelectedItem(2);
    assertEquals(2, value.get());
    comboBox.setSelectedItem(3);
    assertEquals(3, value.get());
    comboBox.setSelectedItem(4);//does not exist
    assertEquals(3, value.get());
  }

  @Test
  public void createComboBox() {
    final DefaultComboBoxModel<Integer> boxModel = new DefaultComboBoxModel<>(new Integer[] {0, 1, 2, 3});
    final Value<Integer> value = Value.value();
    final JComboBox<Integer> box = ComponentBuilders.comboBoxBuilder(value, Integer.class, boxModel)
            .transferFocusOnEnter(true).build();

    assertNull(value.get());
    box.setSelectedItem(1);
    assertEquals(1, value.get());
    box.setSelectedItem(2);
    assertEquals(2, value.get());
    box.setSelectedItem(3);
    assertEquals(3, value.get());
    box.setSelectedItem(4);//does not exist
    assertEquals(3, value.get());
  }

  @Test
  public void createTextField() {
    final Value<String> value = Value.value();
    final JTextField field = ComponentBuilders.textFieldBuilder(value, String.class)
            .columns(10).upperCase().selectAllOnFocusGained().build();
    field.setText("hello");
    assertEquals("HELLO", value.get());
  }

  @Test
  public void createTextArea() {
    final Value<String> value = Value.value();
    final JTextArea textArea = ComponentBuilders.textAreaBuilder(value)
            .transferFocusOnEnter(true).rows(4).columns(2).updateOn(UpdateOn.KEYSTROKE).lineWrap(true).wrapStyleWord(true).build();
    textArea.setText("hello");
    assertEquals("hello", value.get());
  }

  @Test
  public void createTextInputPanel() {
    final Value<String> value = Value.value();
    final TextInputPanel inputPanel = ComponentBuilders.textInputPanelBuilder(value)
            .transferFocusOnEnter(true).columns(10).buttonFocusable(true).updateOn(UpdateOn.KEYSTROKE).build();
    inputPanel.setText("hello");
    assertEquals("hello", value.get());
  }

  @Test
  public void createFormattedTextField() {
    final Value<String> value = Value.value();
    final JFormattedTextField field = ComponentBuilders.formattedTextFieldBuilder(value)
            .formatMaskString("##:##").valueContainsLiterals(true).columns(6).updateOn(UpdateOn.KEYSTROKE)
            .focusLostBehaviour(JFormattedTextField.COMMIT).build();
    field.setText("1234");
    assertEquals("12:34", value.get());
  }
}

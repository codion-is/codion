/*
 * Copyright (c) 2013 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.item.Item;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.swing.common.model.component.combobox.FilteredComboBoxModel;
import is.codion.swing.common.model.component.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.component.button.NullableCheckBox;
import is.codion.swing.common.ui.component.combobox.Completion;
import is.codion.swing.common.ui.component.text.TextInputPanel;
import is.codion.swing.common.ui.component.text.UpdateOn;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.TestDomain;
import is.codion.swing.framework.ui.TestDomain.Detail;
import is.codion.swing.framework.ui.TestDomain.Detail.EnumType;
import is.codion.swing.framework.ui.TestDomain.Master;

import org.junit.jupiter.api.Test;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import static org.junit.jupiter.api.Assertions.*;

public final class EntityComponentsTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domain(new TestDomain())
          .user(UNIT_TEST_USER)
          .build();

  private final SwingEntityEditModel editModel = new SwingEntityEditModel(Detail.TYPE, CONNECTION_PROVIDER);
  private final EntityComponents entityComponents = new EntityComponents(editModel.entityDefinition());

  @Test
  void checkBox() {
    editModel.setDefaultValues();
    ComponentValue<Boolean, JCheckBox> componentValue =
            entityComponents.checkBox(Detail.BOOLEAN)
                    .transferFocusOnEnter(true)
                    .linkedValue(editModel.value(Detail.BOOLEAN))
                    .buildValue();
    JCheckBox box = componentValue.component();
    assertTrue(box.isSelected());//default value is true
    assertTrue(editModel.get(Detail.BOOLEAN));

    box.doClick();

    assertFalse(box.isSelected());
    assertFalse(editModel.get(Detail.BOOLEAN));

    editModel.put(Detail.BOOLEAN, true);
    assertTrue(box.isSelected());
  }

  @Test
  void toggleButton() {
    editModel.setDefaultValues();
    ComponentValue<Boolean, JToggleButton> componentValue =
            entityComponents.toggleButton(Detail.BOOLEAN)
                    .transferFocusOnEnter(true)
                    .linkedValue(editModel.value(Detail.BOOLEAN))
                    .buildValue();
    JToggleButton box = componentValue.component();
    assertTrue(box.isSelected());//default value is true
    assertTrue(editModel.get(Detail.BOOLEAN));

    box.doClick();

    assertFalse(box.isSelected());
    assertFalse(editModel.get(Detail.BOOLEAN));

    editModel.put(Detail.BOOLEAN, true);
    assertTrue(box.isSelected());
  }

  @Test
  void nullableCheckBox() {
    editModel.setDefaultValues();
    ComponentValue<Boolean, JCheckBox> componentValue =
            entityComponents.checkBox(Detail.BOOLEAN_NULLABLE)
                    .transferFocusOnEnter(true)
                    .nullable(true)
                    .linkedValue(editModel.value(Detail.BOOLEAN_NULLABLE))
                    .buildValue();
    NullableCheckBox box = (NullableCheckBox) componentValue.component();
    assertTrue(box.isSelected());//default value is true
    assertTrue(editModel.get(Detail.BOOLEAN_NULLABLE));

    box.getMouseListeners()[1].mouseClicked(null);

    assertNull(box.getState());
    assertNull(editModel.get(Detail.BOOLEAN_NULLABLE));

    editModel.put(Detail.BOOLEAN_NULLABLE, false);
    assertFalse(box.isSelected());
  }

  @Test
  void booleanComboBox() {
    editModel.setDefaultValues();
    editModel.put(Detail.BOOLEAN, true);
    ComponentValue<Boolean, JComboBox<Item<Boolean>>> componentValue =
            entityComponents.booleanComboBox(Detail.BOOLEAN)
                    .transferFocusOnEnter(true)
                    .linkedValue(editModel.value(Detail.BOOLEAN))
                    .buildValue();
    ItemComboBoxModel<Boolean> boxModel = (ItemComboBoxModel<Boolean>) componentValue.component().getModel();
    assertTrue(boxModel.selectedValue().get());
    boxModel.setSelectedItem(null);
    assertNull(editModel.get(Detail.BOOLEAN));

    editModel.put(Detail.BOOLEAN, false);
    assertFalse(boxModel.selectedValue().get());
  }

  @Test
  void itemComboBox() {
    ComponentValue<Integer, JComboBox<Item<Integer>>> componentValue =
            entityComponents.itemComboBox(Detail.INT_VALUE_LIST)
                    .transferFocusOnEnter(true)
                    .linkedValue(editModel.value(Detail.INT_VALUE_LIST))
                    .buildValue();
    JComboBox<Item<Integer>> comboBox = componentValue.component();

    ItemComboBoxModel<Integer> model = (ItemComboBoxModel<Integer>) comboBox.getModel();
    assertEquals(0, model.indexOf(null));
    assertTrue(model.containsItem(Item.item(null)));

    assertNull(editModel.get(Detail.INT_VALUE_LIST));
    comboBox.setSelectedItem(1);
    assertEquals(1, editModel.get(Detail.INT_VALUE_LIST));
    comboBox.setSelectedItem(2);
    assertEquals(2, editModel.get(Detail.INT_VALUE_LIST));
    comboBox.setSelectedItem(3);
    assertEquals(3, editModel.get(Detail.INT_VALUE_LIST));
    comboBox.setSelectedItem(4);//does not exist
    assertEquals(3, editModel.get(Detail.INT_VALUE_LIST));
  }

  @Test
  void nullableUnsortedItemComboBox() {
    ComponentValue<Integer, JComboBox<Item<Integer>>> componentValue =
            entityComponents.itemComboBox(Detail.INT_VALUE_LIST)
                    .sorted(false)
                    .buildValue();
    ItemComboBoxModel<Integer> model = (ItemComboBoxModel<Integer>) componentValue.component().getModel();

    //null item should be first, regardless of sorting
    assertEquals(0, model.visibleItems().indexOf(Item.item(null)));
  }

  @Test
  void comboBox() {
    DefaultComboBoxModel<Integer> boxModel = new DefaultComboBoxModel<>(new Integer[] {0, 1, 2, 3});
    ComponentValue<Integer, JComboBox<Integer>> componentValue =
            entityComponents.comboBox(Detail.INT, boxModel)
                    .completionMode(Completion.Mode.NONE)//otherwise a non-existing element can be selected, last test fails
                    .transferFocusOnEnter(true)
                    .linkedValue(editModel.value(Detail.INT))
                    .buildValue();
    JComboBox<Integer> box = componentValue.component();

    assertNull(editModel.get(Detail.INT));
    box.setSelectedItem(1);
    assertEquals(1, editModel.get(Detail.INT));
    box.setSelectedItem(2);
    assertEquals(2, editModel.get(Detail.INT));
    box.setSelectedItem(3);
    assertEquals(3, editModel.get(Detail.INT));
    box.setSelectedItem(4);//does not exist
    assertEquals(3, editModel.get(Detail.INT));
  }

  @Test
  void enumComboBox() {
    JComboBox<?> comboBox = (JComboBox<?>) entityComponents.component(Detail.ENUM_TYPE).build();
    FilteredComboBoxModel<EnumType> comboBoxModel = (FilteredComboBoxModel<EnumType>) comboBox.getModel();
    comboBoxModel.refresh();
    assertEquals(4, comboBoxModel.getSize());
    for (EnumType enumType : EnumType.values()) {
      assertTrue(comboBoxModel.containsItem(enumType));
    }
  }

  @Test
  void textField() {
    ComponentValue<String, JTextField> componentValue =
            entityComponents.textField(Detail.STRING)
                    .columns(10)
                    .upperCase(true)
                    .selectAllOnFocusGained(true)
                    .linkedValue(editModel.value(Detail.STRING))
                    .buildValue();
    JTextField field = componentValue.component();
    field.setText("hello");
    assertEquals("HELLO", editModel.get(Detail.STRING));

    entityComponents.textField(Detail.DATE)
            .linkedValue(editModel.value(Detail.DATE))
            .buildValue();
    entityComponents.textField(Detail.TIME)
            .linkedValue(editModel.value(Detail.TIME))
            .buildValue();
    entityComponents.textField(Detail.TIMESTAMP)
            .linkedValue(editModel.value(Detail.TIMESTAMP))
            .buildValue();
    entityComponents.textField(Detail.OFFSET)
            .linkedValue(editModel.value(Detail.OFFSET))
            .buildValue();
  }

  @Test
  void textArea() {
    ComponentValue<String, JTextArea> componentValue =
            entityComponents.textArea(Detail.STRING)
                    .transferFocusOnEnter(true)
                    .rowsColumns(4, 2)
                    .updateOn(UpdateOn.KEYSTROKE)
                    .lineWrap(true)
                    .wrapStyleWord(true)
                    .linkedValue(editModel.value(Detail.STRING))
                    .buildValue();
    JTextArea textArea = componentValue.component();
    textArea.setText("hello");
    assertEquals("hello", editModel.get(Detail.STRING));
  }

  @Test
  void textInputPanel() {
    ComponentValue<String, TextInputPanel> componentValue =
            entityComponents.textInputPanel(Detail.STRING)
                    .transferFocusOnEnter(true)
                    .columns(10)
                    .buttonFocusable(true)
                    .updateOn(UpdateOn.KEYSTROKE)
                    .linkedValue(editModel.value(Detail.STRING))
                    .buildValue();
    TextInputPanel inputPanel = componentValue.component();
    inputPanel.setText("hello");
    assertEquals("hello", editModel.get(Detail.STRING));
  }

  @Test
  void maskedTextField() {
    ComponentValue<String, JFormattedTextField> componentValue =
            entityComponents.maskedTextField(Detail.STRING)
                    .mask("##:##")
                    .valueContainsLiteralCharacters(true)
                    .columns(6)
                    .commitsOnValidEdit(true)
                    .focusLostBehaviour(JFormattedTextField.COMMIT)
                    .linkedValue(editModel.value(Detail.STRING))
                    .buildValue();
    JFormattedTextField field = componentValue.component();
    field.setText("1234");
    assertEquals("12:34", editModel.get(Detail.STRING));
  }

  @Test
  void foreignKeyLabel() {
    ComponentValue<Entity, JLabel> componentValue =
            entityComponents.foreignKeyLabel(Detail.MASTER_FK)
                    .linkedValue(editModel.value(Detail.MASTER_FK))
                    .buildValue();
    JLabel field = componentValue.component();
    Entity entity = editModel.entities().builder(Master.TYPE).with(Master.NAME, "name").build();
    editModel.put(Detail.MASTER_FK, entity);
    assertEquals("name", field.getText());
  }

  @Test
  void foreignKeySearchField() {
    entityComponents.foreignKeySearchField(Detail.MASTER_FK, editModel.foreignKeySearchModel(Detail.MASTER_FK))
            .columns(20)
            .upperCase(true)
            .lowerCase(false)
            .searchHintEnabled(true)
            .buildValue();
  }

  @Test
  void foreignKeyComboBox() {
    entityComponents.foreignKeyComboBox(Detail.MASTER_FK, editModel.foreignKeyComboBoxModel(Detail.MASTER_FK))
            .linkedValue(editModel.value(Detail.MASTER_FK))
            .buildValue();
  }

  @Test
  void integerSpinner() {
    Value<Integer> value = Value.value();
    ComponentValue<Integer, JSpinner> componentValue =
            entityComponents.integerSpinner(Detail.INT)
                    .linkedValue(value)
                    .buildValue();
    JSpinner spinner = componentValue.component();
    value.set(100);
    assertEquals(100, componentValue.get());
    spinner.setValue(42);
    assertEquals(42, value.get());
  }

  @Test
  void doubleSpinner() {
    Value<Double> value = Value.value();
    ComponentValue<Double, JSpinner> componentValue =
            entityComponents.doubleSpinner(Detail.DOUBLE)
                    .linkedValue(value)
                    .buildValue();
    JSpinner spinner = componentValue.component();
    value.set(100d);
    assertEquals(100d, componentValue.get());
    spinner.setValue(42d);
    assertEquals(42d, value.get());
  }

  @Test
  void slider() {
    Value<Integer> value = Value.value();
    ComponentValue<Integer, JSlider> componentValue =
            entityComponents.slider(Detail.INT)
                    .linkedValue(value)
                    .buildValue();
    JSlider slider = componentValue.component();
    value.set(100);
    assertEquals(100, slider.getValue());
    slider.setValue(42);
    assertEquals(42, value.get());
  }

  @Test
  void component() {
    EntityDefinition definition = CONNECTION_PROVIDER.entities().definition(Detail.TYPE);
    definition.columnDefinitions()
            .forEach(columnDefinition -> entityComponents.component(columnDefinition.attribute()).build());

    assertThrows(IllegalArgumentException.class, () -> entityComponents.component(Detail.MASTER_FK));
  }
}

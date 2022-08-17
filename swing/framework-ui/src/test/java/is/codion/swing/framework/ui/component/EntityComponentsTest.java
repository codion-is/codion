/*
 * Copyright (c) 2013 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.item.Item;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.swing.common.model.component.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.common.ui.component.button.NullableCheckBox;
import is.codion.swing.common.ui.component.combobox.Completion;
import is.codion.swing.common.ui.component.text.TextInputPanel;
import is.codion.swing.common.ui.component.text.UpdateOn;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.TestDomain;
import is.codion.swing.framework.ui.TestDomain.Detail;
import is.codion.swing.framework.ui.TestDomain.Master;

import org.junit.jupiter.api.Test;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import static org.junit.jupiter.api.Assertions.*;

public final class EntityComponentsTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domainClassName(TestDomain.class.getName())
          .user(UNIT_TEST_USER)
          .build();

  private final SwingEntityEditModel editModel = new SwingEntityEditModel(Detail.TYPE, CONNECTION_PROVIDER);
  private final EntityComponents inputComponents = new EntityComponents(editModel.entityDefinition());

  @Test
  void createCheckBox() {
    editModel.setDefaultValues();
    ComponentValue<Boolean, JCheckBox> componentValue =
            inputComponents.checkBox(Detail.BOOLEAN)
                    .transferFocusOnEnter(true)
                    .linkedValue(editModel.value(Detail.BOOLEAN))
                    .buildComponentValue();
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
  void createToggleButton() {
    editModel.setDefaultValues();
    ComponentValue<Boolean, JToggleButton> componentValue =
            inputComponents.toggleButton(Detail.BOOLEAN)
                    .transferFocusOnEnter(true)
                    .linkedValue(editModel.value(Detail.BOOLEAN))
                    .buildComponentValue();
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
  void createNullableCheckBox() {
    editModel.setDefaultValues();
    ComponentValue<Boolean, JCheckBox> componentValue =
            inputComponents.checkBox(Detail.BOOLEAN_NULLABLE)
                    .transferFocusOnEnter(true)
                    .nullable(true)
                    .linkedValue(editModel.value(Detail.BOOLEAN_NULLABLE))
                    .buildComponentValue();
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
  void createBooleanComboBox() {
    editModel.setDefaultValues();
    editModel.put(Detail.BOOLEAN, true);
    ComponentValue<Boolean, JComboBox<Item<Boolean>>> componentValue =
            inputComponents.booleanComboBox(Detail.BOOLEAN)
                    .transferFocusOnEnter(true)
                    .linkedValue(editModel.value(Detail.BOOLEAN))
                    .buildComponentValue();
    ItemComboBoxModel<Boolean> boxModel = (ItemComboBoxModel<Boolean>) componentValue.component().getModel();
    assertTrue(boxModel.selectedValue().value());
    boxModel.setSelectedItem(null);
    assertNull(editModel.get(Detail.BOOLEAN));

    editModel.put(Detail.BOOLEAN, false);
    assertFalse(boxModel.selectedValue().value());
  }

  @Test
  void createItemComboBox() {
    ComponentValue<Integer, JComboBox<Item<Integer>>> componentValue =
            inputComponents.itemComboBox(Detail.INT_VALUE_LIST)
                    .transferFocusOnEnter(true)
                    .linkedValue(editModel.value(Detail.INT_VALUE_LIST))
                    .buildComponentValue();
    JComboBox<Item<Integer>> box = componentValue.component();

    assertNull(editModel.get(Detail.INT_VALUE_LIST));
    box.setSelectedItem(1);
    assertEquals(1, editModel.get(Detail.INT_VALUE_LIST));
    box.setSelectedItem(2);
    assertEquals(2, editModel.get(Detail.INT_VALUE_LIST));
    box.setSelectedItem(3);
    assertEquals(3, editModel.get(Detail.INT_VALUE_LIST));
    box.setSelectedItem(4);//does not exist
    assertEquals(3, editModel.get(Detail.INT_VALUE_LIST));
  }

  @Test
  void createNullableUnsortedItemComboBox() {
    ComponentValue<Integer, JComboBox<Item<Integer>>> componentValue =
            inputComponents.itemComboBox(Detail.INT_VALUE_LIST)
                    .sorted(false)
                    .buildComponentValue();
    ItemComboBoxModel<Integer> model = (ItemComboBoxModel<Integer>) componentValue.component().getModel();

    //null item should be first, regardless of sorting
    assertEquals(0, model.items().indexOf(Item.item(null)));
  }

  @Test
  void createComboBox() {
    DefaultComboBoxModel<Integer> boxModel = new DefaultComboBoxModel<>(new Integer[] {0, 1, 2, 3});
    ComponentValue<Integer, JComboBox<Integer>> componentValue =
            inputComponents.comboBox(Detail.INT, boxModel)
                    .completionMode(Completion.Mode.NONE)//otherwise a non-existing element can be selected, last test fails
                    .transferFocusOnEnter(true)
                    .linkedValue(editModel.value(Detail.INT))
                    .buildComponentValue();
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
  void createTextField() {
    ComponentValue<String, JTextField> componentValue =
            inputComponents.textField(Detail.STRING)
                    .columns(10)
                    .upperCase(true)
                    .selectAllOnFocusGained(true)
                    .linkedValue(editModel.value(Detail.STRING))
                    .buildComponentValue();
    JTextField field = componentValue.component();
    field.setText("hello");
    assertEquals("HELLO", editModel.get(Detail.STRING));

    inputComponents.textField(Detail.DATE)
            .linkedValue(editModel.value(Detail.DATE))
            .buildComponentValue();
    inputComponents.textField(Detail.TIME)
            .linkedValue(editModel.value(Detail.TIME))
            .buildComponentValue();
    inputComponents.textField(Detail.TIMESTAMP)
            .linkedValue(editModel.value(Detail.TIMESTAMP))
            .buildComponentValue();
    inputComponents.textField(Detail.OFFSET)
            .linkedValue(editModel.value(Detail.OFFSET))
            .buildComponentValue();
  }

  @Test
  void createTextArea() {
    ComponentValue<String, JTextArea> componentValue =
            inputComponents.textArea(Detail.STRING)
                    .transferFocusOnEnter(true)
                    .rowsColumns(4, 2)
                    .updateOn(UpdateOn.KEYSTROKE)
                    .lineWrap(true)
                    .wrapStyleWord(true)
                    .linkedValue(editModel.value(Detail.STRING))
                    .buildComponentValue();
    JTextArea textArea = componentValue.component();
    textArea.setText("hello");
    assertEquals("hello", editModel.get(Detail.STRING));
  }

  @Test
  void createTextInputPanel() {
    ComponentValue<String, TextInputPanel> componentValue =
            inputComponents.textInputPanel(Detail.STRING)
                    .transferFocusOnEnter(true)
                    .columns(10)
                    .buttonFocusable(true)
                    .updateOn(UpdateOn.KEYSTROKE)
                    .linkedValue(editModel.value(Detail.STRING))
                    .buildComponentValue();
    TextInputPanel inputPanel = componentValue.component();
    inputPanel.setText("hello");
    assertEquals("hello", editModel.get(Detail.STRING));
  }

  @Test
  void createMaskedTextField() {
    ComponentValue<String, JFormattedTextField> componentValue =
            inputComponents.maskedTextField(Detail.STRING)
                    .mask("##:##")
                    .valueContainsLiteralCharacters(true)
                    .columns(6)
                    .commitsOnValidEdit(true)
                    .focusLostBehaviour(JFormattedTextField.COMMIT)
                    .linkedValue(editModel.value(Detail.STRING))
                    .buildComponentValue();
    JFormattedTextField field = componentValue.component();
    field.setText("1234");
    assertEquals("12:34", editModel.get(Detail.STRING));
  }

  @Test
  void createForeignKeyLabel() {
    ComponentValue<Entity, JLabel> componentValue =
            inputComponents.foreignKeyLabel(Detail.MASTER_FK)
                    .linkedValue(editModel.value(Detail.MASTER_FK))
                    .buildComponentValue();
    JLabel field = componentValue.component();
    Entity entity = editModel.entities().builder(Master.TYPE).with(Master.NAME, "name").build();
    editModel.put(Detail.MASTER_FK, entity);
    assertEquals("name", field.getText());
  }

  @Test
  void createForeignKeySearchField() {
    inputComponents.foreignKeySearchField(Detail.MASTER_FK, editModel.foreignKeySearchModel(Detail.MASTER_FK))
            .columns(20)
            .upperCase(true)
            .lowerCase(false)
            .searchHintEnabled(true)
            .buildComponentValue();
  }

  @Test
  void createForeignKeyComboBox() {
    inputComponents.foreignKeyComboBox(Detail.MASTER_FK, editModel.foreignKeyComboBoxModel(Detail.MASTER_FK))
            .linkedValue(editModel.value(Detail.MASTER_FK))
            .buildComponentValue();
  }

  @Test
  void itemComboBox() {
    Value<Integer> value = Value.value();
    ComponentValue<Integer, JComboBox<Item<Integer>>> componentValue =
            inputComponents.itemComboBox(Detail.INT_VALUE_LIST)
                    .linkedValue(value)
                    .buildComponentValue();
    JComboBox<Item<Integer>> comboBox = componentValue.component();
    ItemComboBoxModel<Integer> model = (ItemComboBoxModel<Integer>) comboBox.getModel();
    assertEquals(0, model.indexOf(null));
    assertTrue(model.containsItem(Item.item(null)));
  }

  @Test
  void inputComponent() {
    EntityDefinition definition = CONNECTION_PROVIDER.entities().definition(Detail.TYPE);
    definition.columnProperties()
            .forEach(property -> inputComponents.component(property.attribute()).build());

    assertThrows(IllegalArgumentException.class, () -> inputComponents.component(Detail.MASTER_FK));
  }
}

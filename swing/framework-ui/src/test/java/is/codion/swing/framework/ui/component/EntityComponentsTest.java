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

  private final SwingEntityEditModel editModel = new SwingEntityEditModel(TestDomain.T_DETAIL, CONNECTION_PROVIDER);
  private final EntityComponents inputComponents = new EntityComponents(editModel.entityDefinition());

  @Test
  void createCheckBox() {
    editModel.setDefaultValues();
    ComponentValue<Boolean, JCheckBox> componentValue =
            inputComponents.checkBox(TestDomain.DETAIL_BOOLEAN)
                    .transferFocusOnEnter(true)
                    .linkedValue(editModel.value(TestDomain.DETAIL_BOOLEAN))
                    .buildComponentValue();
    JCheckBox box = componentValue.getComponent();
    assertTrue(box.isSelected());//default value is true
    assertTrue(editModel.get(TestDomain.DETAIL_BOOLEAN));

    box.doClick();

    assertFalse(box.isSelected());
    assertFalse(editModel.get(TestDomain.DETAIL_BOOLEAN));

    editModel.put(TestDomain.DETAIL_BOOLEAN, true);
    assertTrue(box.isSelected());
  }

  @Test
  void createToggleButton() {
    editModel.setDefaultValues();
    ComponentValue<Boolean, JToggleButton> componentValue =
            inputComponents.toggleButton(TestDomain.DETAIL_BOOLEAN)
                    .transferFocusOnEnter(true)
                    .linkedValue(editModel.value(TestDomain.DETAIL_BOOLEAN))
                    .buildComponentValue();
    JToggleButton box = componentValue.getComponent();
    assertTrue(box.isSelected());//default value is true
    assertTrue(editModel.get(TestDomain.DETAIL_BOOLEAN));

    box.doClick();

    assertFalse(box.isSelected());
    assertFalse(editModel.get(TestDomain.DETAIL_BOOLEAN));

    editModel.put(TestDomain.DETAIL_BOOLEAN, true);
    assertTrue(box.isSelected());
  }

  @Test
  void createNullableCheckBox() {
    editModel.setDefaultValues();
    ComponentValue<Boolean, JCheckBox> componentValue =
            inputComponents.checkBox(TestDomain.DETAIL_BOOLEAN_NULLABLE)
                    .transferFocusOnEnter(true)
                    .nullable(true)
                    .linkedValue(editModel.value(TestDomain.DETAIL_BOOLEAN_NULLABLE))
                    .buildComponentValue();
    NullableCheckBox box = (NullableCheckBox) componentValue.getComponent();
    assertTrue(box.isSelected());//default value is true
    assertTrue(editModel.get(TestDomain.DETAIL_BOOLEAN_NULLABLE));

    box.getMouseListeners()[1].mouseClicked(null);

    assertNull(box.getState());
    assertNull(editModel.get(TestDomain.DETAIL_BOOLEAN_NULLABLE));

    editModel.put(TestDomain.DETAIL_BOOLEAN_NULLABLE, false);
    assertFalse(box.isSelected());
  }

  @Test
  void createBooleanComboBox() {
    editModel.setDefaultValues();
    editModel.put(TestDomain.DETAIL_BOOLEAN, true);
    ComponentValue<Boolean, JComboBox<Item<Boolean>>> componentValue =
            inputComponents.booleanComboBox(TestDomain.DETAIL_BOOLEAN)
                    .transferFocusOnEnter(true)
                    .linkedValue(editModel.value(TestDomain.DETAIL_BOOLEAN))
                    .buildComponentValue();
    ItemComboBoxModel<Boolean> boxModel = (ItemComboBoxModel<Boolean>) componentValue.getComponent().getModel();
    assertTrue(boxModel.selectedValue().value());
    boxModel.setSelectedItem(null);
    assertNull(editModel.get(TestDomain.DETAIL_BOOLEAN));

    editModel.put(TestDomain.DETAIL_BOOLEAN, false);
    assertFalse(boxModel.selectedValue().value());
  }

  @Test
  void createItemComboBox() {
    ComponentValue<Integer, JComboBox<Item<Integer>>> componentValue =
            inputComponents.itemComboBox(TestDomain.DETAIL_INT_VALUE_LIST)
                    .transferFocusOnEnter(true)
                    .linkedValue(editModel.value(TestDomain.DETAIL_INT_VALUE_LIST))
                    .buildComponentValue();
    JComboBox<Item<Integer>> box = componentValue.getComponent();

    assertNull(editModel.get(TestDomain.DETAIL_INT_VALUE_LIST));
    box.setSelectedItem(1);
    assertEquals(1, editModel.get(TestDomain.DETAIL_INT_VALUE_LIST));
    box.setSelectedItem(2);
    assertEquals(2, editModel.get(TestDomain.DETAIL_INT_VALUE_LIST));
    box.setSelectedItem(3);
    assertEquals(3, editModel.get(TestDomain.DETAIL_INT_VALUE_LIST));
    box.setSelectedItem(4);//does not exist
    assertEquals(3, editModel.get(TestDomain.DETAIL_INT_VALUE_LIST));
  }

  @Test
  void createNullableUnsortedItemComboBox() {
    ComponentValue<Integer, JComboBox<Item<Integer>>> componentValue =
            inputComponents.itemComboBox(TestDomain.DETAIL_INT_VALUE_LIST)
                    .sorted(false)
                    .buildComponentValue();
    ItemComboBoxModel<Integer> model = (ItemComboBoxModel<Integer>) componentValue.getComponent().getModel();

    //null item should be first, regardless of sorting
    assertEquals(0, model.items().indexOf(Item.item(null)));
  }

  @Test
  void createComboBox() {
    DefaultComboBoxModel<Integer> boxModel = new DefaultComboBoxModel<>(new Integer[] {0, 1, 2, 3});
    ComponentValue<Integer, JComboBox<Integer>> componentValue =
            inputComponents.comboBox(TestDomain.DETAIL_INT, boxModel)
                    .completionMode(Completion.Mode.NONE)//otherwise a non-existing element can be selected, last test fails
                    .transferFocusOnEnter(true)
                    .linkedValue(editModel.value(TestDomain.DETAIL_INT))
                    .buildComponentValue();
    JComboBox<Integer> box = componentValue.getComponent();

    assertNull(editModel.get(TestDomain.DETAIL_INT));
    box.setSelectedItem(1);
    assertEquals(1, editModel.get(TestDomain.DETAIL_INT));
    box.setSelectedItem(2);
    assertEquals(2, editModel.get(TestDomain.DETAIL_INT));
    box.setSelectedItem(3);
    assertEquals(3, editModel.get(TestDomain.DETAIL_INT));
    box.setSelectedItem(4);//does not exist
    assertEquals(3, editModel.get(TestDomain.DETAIL_INT));
  }

  @Test
  void createTextField() {
    ComponentValue<String, JTextField> componentValue =
            inputComponents.textField(TestDomain.DETAIL_STRING)
                    .columns(10)
                    .upperCase(true)
                    .selectAllOnFocusGained(true)
                    .linkedValue(editModel.value(TestDomain.DETAIL_STRING))
                    .buildComponentValue();
    JTextField field = componentValue.getComponent();
    field.setText("hello");
    assertEquals("HELLO", editModel.get(TestDomain.DETAIL_STRING));

    inputComponents.textField(TestDomain.DETAIL_DATE)
            .linkedValue(editModel.value(TestDomain.DETAIL_DATE))
            .buildComponentValue();
    inputComponents.textField(TestDomain.DETAIL_TIME)
            .linkedValue(editModel.value(TestDomain.DETAIL_TIME))
            .buildComponentValue();
    inputComponents.textField(TestDomain.DETAIL_TIMESTAMP)
            .linkedValue(editModel.value(TestDomain.DETAIL_TIMESTAMP))
            .buildComponentValue();
    inputComponents.textField(TestDomain.DETAIL_OFFSET)
            .linkedValue(editModel.value(TestDomain.DETAIL_OFFSET))
            .buildComponentValue();
  }

  @Test
  void createTextArea() {
    ComponentValue<String, JTextArea> componentValue =
            inputComponents.textArea(TestDomain.DETAIL_STRING)
                    .transferFocusOnEnter(true)
                    .rowsColumns(4, 2)
                    .updateOn(UpdateOn.KEYSTROKE)
                    .lineWrap(true)
                    .wrapStyleWord(true)
                    .linkedValue(editModel.value(TestDomain.DETAIL_STRING))
                    .buildComponentValue();
    JTextArea textArea = componentValue.getComponent();
    textArea.setText("hello");
    assertEquals("hello", editModel.get(TestDomain.DETAIL_STRING));
  }

  @Test
  void createTextInputPanel() {
    ComponentValue<String, TextInputPanel> componentValue =
            inputComponents.textInputPanel(TestDomain.DETAIL_STRING)
                    .transferFocusOnEnter(true)
                    .columns(10)
                    .buttonFocusable(true)
                    .updateOn(UpdateOn.KEYSTROKE)
                    .linkedValue(editModel.value(TestDomain.DETAIL_STRING))
                    .buildComponentValue();
    TextInputPanel inputPanel = componentValue.getComponent();
    inputPanel.setText("hello");
    assertEquals("hello", editModel.get(TestDomain.DETAIL_STRING));
  }

  @Test
  void createMaskedTextField() {
    ComponentValue<String, JFormattedTextField> componentValue =
            inputComponents.maskedTextField(TestDomain.DETAIL_STRING)
                    .mask("##:##")
                    .valueContainsLiteralCharacters(true)
                    .columns(6)
                    .commitsOnValidEdit(true)
                    .focusLostBehaviour(JFormattedTextField.COMMIT)
                    .linkedValue(editModel.value(TestDomain.DETAIL_STRING))
                    .buildComponentValue();
    JFormattedTextField field = componentValue.getComponent();
    field.setText("1234");
    assertEquals("12:34", editModel.get(TestDomain.DETAIL_STRING));
  }

  @Test
  void createForeignKeyLabel() {
    ComponentValue<Entity, JLabel> componentValue =
            inputComponents.foreignKeyLabel(TestDomain.DETAIL_MASTER_FK)
                    .linkedValue(editModel.value(TestDomain.DETAIL_MASTER_FK))
                    .buildComponentValue();
    JLabel field = componentValue.getComponent();
    Entity entity = editModel.entities().builder(TestDomain.T_MASTER).with(TestDomain.MASTER_NAME, "name").build();
    editModel.put(TestDomain.DETAIL_MASTER_FK, entity);
    assertEquals("name", field.getText());
  }

  @Test
  void createForeignKeySearchField() {
    inputComponents.foreignKeySearchField(TestDomain.DETAIL_MASTER_FK, editModel.getForeignKeySearchModel(TestDomain.DETAIL_MASTER_FK))
            .columns(20)
            .upperCase(true)
            .lowerCase(false)
            .searchHintEnabled(true)
            .buildComponentValue();
  }

  @Test
  void createForeignKeyComboBox() {
    inputComponents.foreignKeyComboBox(TestDomain.DETAIL_MASTER_FK, editModel.getForeignKeyComboBoxModel(TestDomain.DETAIL_MASTER_FK))
            .linkedValue(editModel.value(TestDomain.DETAIL_MASTER_FK))
            .buildComponentValue();
  }

  @Test
  void itemComboBox() {
    Value<Integer> value = Value.value();
    ComponentValue<Integer, JComboBox<Item<Integer>>> componentValue =
            inputComponents.itemComboBox(TestDomain.DETAIL_INT_VALUE_LIST)
                    .linkedValue(value)
                    .buildComponentValue();
    JComboBox<Item<Integer>> comboBox = componentValue.getComponent();
    ItemComboBoxModel<Integer> model = (ItemComboBoxModel<Integer>) comboBox.getModel();
    assertEquals(0, model.indexOf(null));
    assertTrue(model.containsItem(Item.item(null)));
  }

  @Test
  void inputComponent() {
    EntityDefinition definition = CONNECTION_PROVIDER.entities().definition(TestDomain.T_DETAIL);
    definition.columnProperties()
            .forEach(property -> inputComponents.component(property.attribute()).build());

    assertThrows(IllegalArgumentException.class, () -> inputComponents.component(TestDomain.DETAIL_MASTER_FK));
  }
}

/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.item.Item;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntityEditModel;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.common.ui.textfield.TextInputPanel;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.UpdateOn;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.TestDomain;

import org.junit.jupiter.api.Test;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import static org.junit.jupiter.api.Assertions.*;

public final class EntityComponentBuildersTest {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          DatabaseFactory.getDatabase()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  private final EntityEditModel editModel = new SwingEntityEditModel(TestDomain.T_DETAIL, CONNECTION_PROVIDER);
  private final EntityComponentBuilders inputComponents = new EntityComponentBuilders(editModel.getEntityDefinition());

  @Test
  public void createCheckBox() {
    editModel.setDefaultValues();
    final ComponentValue<Boolean, JCheckBox> componentValue =
            inputComponents.checkBox(TestDomain.DETAIL_BOOLEAN)
            .transferFocusOnEnter(true).buildComponentValue();
    componentValue.link(editModel.value(TestDomain.DETAIL_BOOLEAN));
    final JCheckBox box = componentValue.getComponent();
    assertTrue(box.isSelected());//default value is true
    assertTrue(editModel.get(TestDomain.DETAIL_BOOLEAN));

    box.doClick();

    assertFalse(box.isSelected());
    assertFalse(editModel.get(TestDomain.DETAIL_BOOLEAN));

    editModel.put(TestDomain.DETAIL_BOOLEAN, true);
    assertTrue(box.isSelected());
  }

  @Test
  public void createToggleButton() {
    editModel.setDefaultValues();
    final ComponentValue<Boolean, JToggleButton> componentValue =
            inputComponents.toggleButton(TestDomain.DETAIL_BOOLEAN)
            .transferFocusOnEnter(true).buildComponentValue();
    componentValue.link(editModel.value(TestDomain.DETAIL_BOOLEAN));
    final JToggleButton box = componentValue.getComponent();
    assertTrue(box.isSelected());//default value is true
    assertTrue(editModel.get(TestDomain.DETAIL_BOOLEAN));

    box.doClick();

    assertFalse(box.isSelected());
    assertFalse(editModel.get(TestDomain.DETAIL_BOOLEAN));

    editModel.put(TestDomain.DETAIL_BOOLEAN, true);
    assertTrue(box.isSelected());
  }

  @Test
  public void createNullableCheckBox() {
    editModel.setDefaultValues();
    final ComponentValue<Boolean, JCheckBox> componentValue =
            inputComponents.checkBox(TestDomain.DETAIL_BOOLEAN_NULLABLE)
            .transferFocusOnEnter(true).nullable(true).buildComponentValue();
    componentValue.link(editModel.value(TestDomain.DETAIL_BOOLEAN_NULLABLE));
    final NullableCheckBox box = (NullableCheckBox) componentValue.getComponent();
    assertTrue(box.isSelected());//default value is true
    assertTrue(editModel.get(TestDomain.DETAIL_BOOLEAN_NULLABLE));

    box.getMouseListeners()[1].mouseClicked(null);

    assertNull(box.getState());
    assertNull(editModel.get(TestDomain.DETAIL_BOOLEAN_NULLABLE));

    editModel.put(TestDomain.DETAIL_BOOLEAN_NULLABLE, false);
    assertFalse(box.isSelected());
  }

  @Test
  public void createBooleanComboBox() {
    editModel.setDefaultValues();
    editModel.put(TestDomain.DETAIL_BOOLEAN, true);
    final ComponentValue<Boolean, SteppedComboBox<Item<Boolean>>> componentValue =
            inputComponents.booleanComboBox(TestDomain.DETAIL_BOOLEAN)
            .transferFocusOnEnter(true).buildComponentValue();
    componentValue.link(editModel.value(TestDomain.DETAIL_BOOLEAN));
    final ItemComboBoxModel<Boolean> boxModel = (ItemComboBoxModel<Boolean>) componentValue.getComponent().getModel();
    assertTrue(boxModel.getSelectedValue().getValue());
    boxModel.setSelectedItem(null);
    assertNull(editModel.get(TestDomain.DETAIL_BOOLEAN));

    editModel.put(TestDomain.DETAIL_BOOLEAN, false);
    assertFalse(boxModel.getSelectedValue().getValue());
  }

  @Test
  public void createItemComboBox() {
    final ComponentValue<Integer, SteppedComboBox<Item<Integer>>> componentValue =
            inputComponents.itemComboBox(TestDomain.DETAIL_INT_VALUE_LIST).transferFocusOnEnter(true)
            .buildComponentValue();
    componentValue.link(editModel.value(TestDomain.DETAIL_INT_VALUE_LIST));
    final JComboBox<Item<Integer>> box = componentValue.getComponent();

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
  public void createComboBox() {
    final DefaultComboBoxModel<Integer> boxModel = new DefaultComboBoxModel<>(new Integer[] {0, 1, 2, 3});
    final ComponentValue<Integer, SteppedComboBox<Integer>> componentValue =
            inputComponents.comboBox(TestDomain.DETAIL_INT, boxModel)
                    .completionMode(Completion.Mode.NONE)//otherwise a non-existing element can be selected, last test fails
            .transferFocusOnEnter(true).buildComponentValue();
    componentValue.link(editModel.value(TestDomain.DETAIL_INT));
    final JComboBox<Integer> box = componentValue.getComponent();

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
  public void createTextField() {
    final ComponentValue<String, JTextField> componentValue =
            inputComponents.textField(TestDomain.DETAIL_STRING)
            .columns(10).upperCase().selectAllOnFocusGained().buildComponentValue();
    componentValue.link(editModel.value(TestDomain.DETAIL_STRING));
    final JTextField field = componentValue.getComponent();
    field.setText("hello");
    assertEquals("HELLO", editModel.get(TestDomain.DETAIL_STRING));
  }

  @Test
  public void createTextArea() {
    final ComponentValue<String, JTextArea> componentValue =
            inputComponents.textArea(TestDomain.DETAIL_STRING)
            .transferFocusOnEnter(true).rowsColumns(4, 2).updateOn(UpdateOn.KEYSTROKE).lineWrap(true).wrapStyleWord(true)
            .buildComponentValue();
    componentValue.link(editModel.value(TestDomain.DETAIL_STRING));
    final JTextArea textArea = componentValue.getComponent();
    textArea.setText("hello");
    assertEquals("hello", editModel.get(TestDomain.DETAIL_STRING));
  }

  @Test
  public void createTextInputPanel() {
    final ComponentValue<String, TextInputPanel> componentValue =
            inputComponents.textInputPanel(TestDomain.DETAIL_STRING)
            .transferFocusOnEnter(true).columns(10).buttonFocusable(true).updateOn(UpdateOn.KEYSTROKE)
            .buildComponentValue();
    componentValue.link(editModel.value(TestDomain.DETAIL_STRING));
    final TextInputPanel inputPanel = componentValue.getComponent();
    inputPanel.setText("hello");
    assertEquals("hello", editModel.get(TestDomain.DETAIL_STRING));
  }

  @Test
  public void createFormattedTextField() {
    final ComponentValue<String, JFormattedTextField> componentValue =
            inputComponents.formattedTextField(TestDomain.DETAIL_STRING)
            .formatMask("##:##").valueContainsLiterals(true).columns(6).updateOn(UpdateOn.KEYSTROKE)
            .focusLostBehaviour(JFormattedTextField.COMMIT).buildComponentValue();
    componentValue.link(editModel.value(TestDomain.DETAIL_STRING));
    final JFormattedTextField field = componentValue.getComponent();
    field.setText("1234");
    assertEquals("12:34", editModel.get(TestDomain.DETAIL_STRING));
  }

  @Test
  public void createForeignKeyField() {
    final ComponentValue<Entity, JTextField> componentValue =
            inputComponents.foreignKeyField(TestDomain.DETAIL_MASTER_FK)
            .columns(10).buildComponentValue();
    componentValue.link(editModel.value(TestDomain.DETAIL_MASTER_FK));
    final JTextField field = componentValue.getComponent();
    final Entity entity = editModel.getEntities().builder(TestDomain.T_MASTER).with(TestDomain.MASTER_NAME, "name").build();
    editModel.put(TestDomain.DETAIL_MASTER_FK, entity);
    assertEquals("name", field.getText());
  }

  @Test
  public void itemComboBox() {
    final Value<Integer> value = Value.value();
    final ComponentValue<Integer, SteppedComboBox<Item<Integer>>> componentValue =
            inputComponents.itemComboBox(TestDomain.DETAIL_INT_VALUE_LIST).buildComponentValue();
    componentValue.link(value);
    final SteppedComboBox<Item<Integer>> comboBox = componentValue.getComponent();
    final ItemComboBoxModel<Integer> model = (ItemComboBoxModel<Integer>) comboBox.getModel();
    assertEquals(0, model.indexOf(null));
    assertTrue(model.containsItem(Item.item(null)));
  }
}

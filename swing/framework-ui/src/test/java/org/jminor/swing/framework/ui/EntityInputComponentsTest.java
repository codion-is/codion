package org.jminor.swing.framework.ui;

import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.property.ValueListProperty;
import org.jminor.framework.model.EntityEditModel;
import org.jminor.swing.common.model.combobox.BooleanComboBoxModel;
import org.jminor.swing.common.ui.checkbox.NullableCheckBox;
import org.jminor.swing.framework.model.SwingEntityEditModel;

import org.junit.jupiter.api.Test;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import static org.junit.jupiter.api.Assertions.*;

public class EntityInputComponentsTest {

  private static final Domain DOMAIN = new TestDomain();

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  private final EntityEditModel editModel = new SwingEntityEditModel(TestDomain.T_DETAIL, CONNECTION_PROVIDER);

  @Test
  public void createLabel() {
    final JLabel label = EntityInputComponents.createLabel(DOMAIN.getDefinition(TestDomain.T_DETAIL).getProperty(TestDomain.DETAIL_STRING));
    assertEquals(DOMAIN.getDefinition(TestDomain.T_DETAIL).getProperty(TestDomain.DETAIL_STRING).getCaption(), label.getText());
  }

  @Test
  public void createNullableCheckBoxNonNullableBooleanProperty() {
    assertThrows(IllegalArgumentException.class, () ->
            EntityInputComponents.createNullableCheckBox(DOMAIN.getDefinition(TestDomain.T_DETAIL).getProperty(TestDomain.DETAIL_BOOLEAN),
                    editModel.value(TestDomain.DETAIL_BOOLEAN), null, true));
  }

  @Test
  public void createNullableCheckBoxNonBooleanProperty() {
    assertThrows(IllegalArgumentException.class, () ->
            EntityInputComponents.createNullableCheckBox(DOMAIN.getDefinition(TestDomain.T_DETAIL).getProperty(TestDomain.DETAIL_TIMESTAMP),
                    editModel.value(TestDomain.DETAIL_TIMESTAMP), null, true));
  }

  @Test
  public void createCheckBoxNonBooleanProperty() {
    assertThrows(IllegalArgumentException.class, () ->
            EntityInputComponents.createCheckBox(DOMAIN.getDefinition(TestDomain.T_DETAIL).getProperty(TestDomain.DETAIL_TIMESTAMP),
                    editModel.value(TestDomain.DETAIL_TIMESTAMP)));
  }

  @Test
  public void createCheckBox() {
    //set default values
    editModel.setEntity(null);
    final JCheckBox box = EntityInputComponents.createCheckBox(DOMAIN.getDefinition(TestDomain.T_DETAIL).getProperty(
            TestDomain.DETAIL_BOOLEAN), editModel.value(TestDomain.DETAIL_BOOLEAN));
    assertTrue(box.isSelected());//default value is true
    assertTrue((Boolean) editModel.get(TestDomain.DETAIL_BOOLEAN));

    box.doClick();

    assertFalse(box.isSelected());
    assertFalse((Boolean) editModel.get(TestDomain.DETAIL_BOOLEAN));

    editModel.put(TestDomain.DETAIL_BOOLEAN, true);
    assertTrue(box.isSelected());
  }

  @Test
  public void createNullableCheckBox() {
    //set default values
    editModel.setEntity(null);
    final NullableCheckBox box = EntityInputComponents.createNullableCheckBox(DOMAIN.getDefinition(TestDomain.T_DETAIL).getProperty(
            TestDomain.DETAIL_BOOLEAN_NULLABLE), editModel.value(TestDomain.DETAIL_BOOLEAN_NULLABLE), null, false);
    assertTrue(box.isSelected());//default value is true
    assertTrue((Boolean) editModel.get(TestDomain.DETAIL_BOOLEAN_NULLABLE));

    box.getMouseListeners()[0].mouseClicked(null);

    assertNull(box.getState());
    assertNull(editModel.get(TestDomain.DETAIL_BOOLEAN_NULLABLE));

    editModel.put(TestDomain.DETAIL_BOOLEAN_NULLABLE, false);
    assertFalse(box.isSelected());
  }

  @Test
  public void createBooleanComboBox() {
    //set default values
    editModel.setEntity(null);
    final BooleanComboBoxModel boxModel = (BooleanComboBoxModel)
            EntityInputComponents.createBooleanComboBox(DOMAIN.getDefinition(TestDomain.T_DETAIL).getProperty(
                    TestDomain.DETAIL_BOOLEAN), editModel.value(TestDomain.DETAIL_BOOLEAN)).getModel();
    assertTrue(boxModel.getSelectedValue().getValue());//default value is true
    boxModel.setSelectedItem(null);
    assertNull(editModel.get(TestDomain.DETAIL_BOOLEAN));

    editModel.put(TestDomain.DETAIL_BOOLEAN, false);
    assertFalse(boxModel.getSelectedValue().getValue());
  }

  @Test
  public void createValueListComboBox() {
    final JComboBox box = EntityInputComponents.createValueListComboBox((ValueListProperty)
            DOMAIN.getDefinition(TestDomain.T_DETAIL).getProperty(TestDomain.DETAIL_INT_VALUE_LIST),
            editModel.value(TestDomain.DETAIL_INT_VALUE_LIST));

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
    final DefaultComboBoxModel boxModel = new DefaultComboBoxModel<>(new Object[] {0, 1, 2, 3});
    final JComboBox box = EntityInputComponents.createComboBox(DOMAIN.getDefinition(TestDomain.T_DETAIL).getProperty(
            TestDomain.DETAIL_INT), editModel.value(TestDomain.DETAIL_INT), boxModel, null);

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

}

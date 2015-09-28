/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.swing.model.combobox.BooleanComboBoxModel;
import org.jminor.common.swing.ui.UiUtil;
import org.jminor.common.swing.ui.checkbox.TristateCheckBox;
import org.jminor.framework.client.model.DefaultEntityEditModel;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.db.EntityConnectionProvidersTest;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.TestDomain;

import org.junit.Test;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import java.awt.Dimension;

import static org.junit.Assert.*;

public class EntityUiUtilTest {

  @Test
  public void createLabel() {
    TestDomain.init();
    final JLabel lbl = EntityUiUtil.createLabel(Entities.getProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_STRING));
    assertEquals(Entities.getProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_STRING).getCaption(), lbl.getText());
  }

  @Test(expected = IllegalArgumentException.class)
  public void createTristateCheckBoxNonNullableBooleanProperty() {
    TestDomain.init();
    final EntityEditModel editModel = new DefaultEntityEditModel(TestDomain.T_DETAIL, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    EntityUiUtil.createTristateCheckBox(Entities.getProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_BOOLEAN), editModel, null, true);
  }

  @Test(expected = IllegalArgumentException.class)
  public void createTristateCheckBoxNonBooleanProperty() {
    TestDomain.init();
    final EntityEditModel editModel = new DefaultEntityEditModel(TestDomain.T_DETAIL, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    EntityUiUtil.createTristateCheckBox(Entities.getProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_TIMESTAMP), editModel, null, true);
  }

  @Test(expected = IllegalArgumentException.class)
  public void createCheckBoxNonBooleanProperty() {
    TestDomain.init();
    final EntityEditModel editModel = new DefaultEntityEditModel(TestDomain.T_DETAIL, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    EntityUiUtil.createCheckBox(Entities.getProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_TIMESTAMP), editModel);
  }

  @Test
  public void createCheckBox() {
    TestDomain.init();
    final EntityEditModel editModel = new DefaultEntityEditModel(TestDomain.T_DETAIL, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    final JCheckBox box = EntityUiUtil.createCheckBox(Entities.getProperty(TestDomain.T_DETAIL,
            TestDomain.DETAIL_BOOLEAN), editModel);
    assertTrue(box.isSelected());//default value is true
    assertTrue((Boolean) editModel.getValue(TestDomain.DETAIL_BOOLEAN));

    box.doClick();

    assertFalse(box.isSelected());
    assertFalse((Boolean) editModel.getValue(TestDomain.DETAIL_BOOLEAN));

    editModel.setValue(TestDomain.DETAIL_BOOLEAN, true);
    assertTrue(box.isSelected());
  }

  @Test
  public void createTristateCheckBox() {
    TestDomain.init();
    final EntityEditModel editModel = new DefaultEntityEditModel(TestDomain.T_DETAIL, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    final TristateCheckBox box = EntityUiUtil.createTristateCheckBox(Entities.getProperty(TestDomain.T_DETAIL,
            TestDomain.DETAIL_BOOLEAN_NULLABLE), editModel, null, false);
    assertTrue(box.isSelected());//default value is true
    assertTrue((Boolean) editModel.getValue(TestDomain.DETAIL_BOOLEAN_NULLABLE));

    box.getMouseListeners()[0].mousePressed(null);

    assertTrue(box.isIndeterminate());
    assertNull(editModel.getValue(TestDomain.DETAIL_BOOLEAN_NULLABLE));

    editModel.setValue(TestDomain.DETAIL_BOOLEAN_NULLABLE, false);
    assertFalse(box.isSelected());
  }

  @Test
  public void createBooleanComboBox() {
    TestDomain.init();
    final EntityEditModel editModel = new DefaultEntityEditModel(TestDomain.T_DETAIL, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    final BooleanComboBoxModel boxModel = (BooleanComboBoxModel) EntityUiUtil.createBooleanComboBox(Entities.getProperty(TestDomain.T_DETAIL,
            TestDomain.DETAIL_BOOLEAN), editModel).getModel();
    assertTrue(boxModel.getSelectedValue().getItem());
    boxModel.setSelectedItem(null);
    assertNull(editModel.getValue(TestDomain.DETAIL_BOOLEAN));

    editModel.setValue(TestDomain.DETAIL_BOOLEAN, false);
    assertFalse(boxModel.getSelectedValue().getItem());
  }

  @Test
  public void createValueListComboBox() {
    TestDomain.init();
    final EntityEditModel editModel = new DefaultEntityEditModel(TestDomain.T_DETAIL, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    final JComboBox box = EntityUiUtil.createValueListComboBox((Property.ValueListProperty) Entities.getProperty(TestDomain.T_DETAIL,
            TestDomain.DETAIL_INT_VALUE_LIST), editModel);

    assertNull(editModel.getValue(TestDomain.DETAIL_INT_VALUE_LIST));
    box.setSelectedItem(1);
    assertEquals(1, editModel.getValue(TestDomain.DETAIL_INT_VALUE_LIST));
    box.setSelectedItem(2);
    assertEquals(2, editModel.getValue(TestDomain.DETAIL_INT_VALUE_LIST));
    box.setSelectedItem(3);
    assertEquals(3, editModel.getValue(TestDomain.DETAIL_INT_VALUE_LIST));
    box.setSelectedItem(4);//does not exist
    assertEquals(3, editModel.getValue(TestDomain.DETAIL_INT_VALUE_LIST));
  }

  @Test
  public void createComboBox() {
    TestDomain.init();
    final DefaultComboBoxModel boxModel = new DefaultComboBoxModel<>(new Object[] {0, 1, 2, 3});
    final EntityEditModel editModel = new DefaultEntityEditModel(TestDomain.T_DETAIL, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    final JComboBox box = EntityUiUtil.createComboBox(Entities.getProperty(TestDomain.T_DETAIL,
            TestDomain.DETAIL_INT), editModel, boxModel, null);

    assertNull(editModel.getValue(TestDomain.DETAIL_INT));
    box.setSelectedItem(1);
    assertEquals(1, editModel.getValue(TestDomain.DETAIL_INT));
    box.setSelectedItem(2);
    assertEquals(2, editModel.getValue(TestDomain.DETAIL_INT));
    box.setSelectedItem(3);
    assertEquals(3, editModel.getValue(TestDomain.DETAIL_INT));
    box.setSelectedItem(4);//does not exist
    assertEquals(3, editModel.getValue(TestDomain.DETAIL_INT));
  }

  @Test
  public void setPreferredWidth() {
    final JComboBox box = new JComboBox();
    box.setPreferredSize(new Dimension(10, 10));
    UiUtil.setPreferredWidth(box, 42);
    assertEquals(10, box.getPreferredSize().height);
    assertEquals(42, box.getPreferredSize().width);
  }
}

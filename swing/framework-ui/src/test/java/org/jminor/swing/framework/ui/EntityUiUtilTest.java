/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.EntityEditModel;
import org.jminor.swing.common.model.combobox.BooleanComboBoxModel;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.checkbox.TristateCheckBox;
import org.jminor.swing.framework.model.SwingEntityEditModel;

import org.junit.Test;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import java.awt.Dimension;

import static org.junit.Assert.*;

public final class EntityUiUtilTest {

  private static final Entities ENTITIES = new TestDomain();

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(ENTITIES, new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray()), Databases.getInstance());

  private final EntityEditModel editModel = new SwingEntityEditModel(TestDomain.T_DETAIL, CONNECTION_PROVIDER);

  @Test
  public void createLabel() {
    final JLabel lbl = EntityUiUtil.createLabel(ENTITIES.getProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_STRING));
    assertEquals(ENTITIES.getProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_STRING).getCaption(), lbl.getText());
  }

  @Test(expected = IllegalArgumentException.class)
  public void createTristateCheckBoxNonNullableBooleanProperty() {
    EntityUiUtil.createTristateCheckBox(ENTITIES.getProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_BOOLEAN), editModel, null, true);
  }

  @Test(expected = IllegalArgumentException.class)
  public void createTristateCheckBoxNonBooleanProperty() {
    EntityUiUtil.createTristateCheckBox(ENTITIES.getProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_TIMESTAMP), editModel, null, true);
  }

  @Test(expected = IllegalArgumentException.class)
  public void createCheckBoxNonBooleanProperty() {
    EntityUiUtil.createCheckBox(ENTITIES.getProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_TIMESTAMP), editModel);
  }

  @Test
  public void createCheckBox() {
    //set default values
    editModel.setEntity(null);
    final JCheckBox box = EntityUiUtil.createCheckBox(ENTITIES.getProperty(TestDomain.T_DETAIL,
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
    //set default values
    editModel.setEntity(null);
    final TristateCheckBox box = EntityUiUtil.createTristateCheckBox(ENTITIES.getProperty(TestDomain.T_DETAIL,
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
    //set default values
    editModel.setEntity(null);
    final BooleanComboBoxModel boxModel = (BooleanComboBoxModel) EntityUiUtil.createBooleanComboBox(ENTITIES.getProperty(TestDomain.T_DETAIL,
            TestDomain.DETAIL_BOOLEAN), editModel).getModel();
    assertTrue(boxModel.getSelectedValue().getItem());//default value is true
    boxModel.setSelectedItem(null);
    assertNull(editModel.getValue(TestDomain.DETAIL_BOOLEAN));

    editModel.setValue(TestDomain.DETAIL_BOOLEAN, false);
    assertFalse(boxModel.getSelectedValue().getItem());
  }

  @Test
  public void createValueListComboBox() {
    final JComboBox box = EntityUiUtil.createValueListComboBox((Property.ValueListProperty) ENTITIES.getProperty(TestDomain.T_DETAIL,
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
    final DefaultComboBoxModel boxModel = new DefaultComboBoxModel<>(new Object[] {0, 1, 2, 3});
    final JComboBox box = EntityUiUtil.createComboBox(ENTITIES.getProperty(TestDomain.T_DETAIL,
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

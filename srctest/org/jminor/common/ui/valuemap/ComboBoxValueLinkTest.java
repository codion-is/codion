/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.valuemap.EditModelValue;
import org.jminor.common.ui.ValueLinks;
import org.jminor.framework.client.model.DefaultEntityEditModel;
import org.jminor.framework.client.model.EntityComboBoxModel;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.ui.EntityComboBox;
import org.jminor.framework.db.EntityConnectionImplTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ComboBoxValueLinkTest {

  @Ignore
  @Test
  public void test() throws Exception {
    final EntityEditModel model = new DefaultEntityEditModel(EmpDept.T_EMPLOYEE, EntityConnectionImplTest.CONNECTION_PROVIDER);
    final Property.ForeignKeyProperty fkProperty = Entities.getForeignKeyProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_DEPARTMENT_FK);
    final EntityComboBoxModel comboBoxModel = model.createEntityComboBoxModel(fkProperty);
    final EntityComboBox comboBox = new EntityComboBox(comboBoxModel);
    comboBoxModel.refresh();
    ValueLinks.selectedItemValueLink(comboBox, new EditModelValue<String, Object>(model, EmpDept.EMPLOYEE_DEPARTMENT_FK));
    assertTrue(comboBox.getSelectedItem() == null);
    Entity department = model.getConnectionProvider().getConnection().selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "SALES");
    model.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, department);
    assertEquals(comboBox.getSelectedItem(), department);
    department = model.getConnectionProvider().getConnection().selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "OPERATIONS");
    comboBox.setSelectedItem(department);
    assertEquals(model.getValue(fkProperty.getPropertyID()), department);
  }
}

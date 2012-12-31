/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.valuemap.EditModelValue;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.control.ValueLink;
import org.jminor.framework.client.model.DefaultEntityEditModel;
import org.jminor.framework.client.model.DefaultEntityLookupModel;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityLookupModel;
import org.jminor.framework.db.EntityConnectionImplTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LookupValueLinkTest {

  private final EntityEditModel model;

  public LookupValueLinkTest() {
    model = new DefaultEntityEditModel(EmpDept.T_EMPLOYEE, EntityConnectionImplTest.CONNECTION_PROVIDER);
  }

  @Test
  public void test() throws Exception {
    final Property.ForeignKeyProperty fkProperty = Entities.getForeignKeyProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_DEPARTMENT_FK);
    final EntityLookupModel lookupModel = new DefaultEntityLookupModel(fkProperty.getReferencedEntityID(), EntityConnectionImplTest.CONNECTION_PROVIDER,
            Entities.getColumnProperties(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME));
    new ValueLink<Object>(new EditModelValue<String, Object>(model, EmpDept.EMPLOYEE_DEPARTMENT_FK),
            new EntityUiUtil.LookupUIValue(lookupModel), LinkType.READ_WRITE);
    assertTrue(lookupModel.getSelectedEntities().size() == 0);
    Entity department = model.getConnectionProvider().getConnection().selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "SALES");
    model.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, department);
    assertEquals(lookupModel.getSelectedEntities().size(), 1);
    assertEquals(lookupModel.getSelectedEntities().iterator().next(), department);
    department = model.getConnectionProvider().getConnection().selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "OPERATIONS");
    lookupModel.setSelectedEntity(department);
    assertEquals(model.getValue(fkProperty.getPropertyID()), department);
  }
}
/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.framework.client.model.DefaultEntityEditModel;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityLookupModel;
import org.jminor.framework.db.EntityConnectionImplTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.util.Arrays;

public class LookupValueLinkTest {

  private EntityEditModel model;

  public LookupValueLinkTest() {
    model = new DefaultEntityEditModel(EmpDept.T_EMPLOYEE, EntityConnectionImplTest.DB_PROVIDER);
  }

  @Test
  public void test() throws Exception {
    final Property.ForeignKeyProperty fkProperty = Entities.getForeignKeyProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_DEPARTMENT_FK);
    final Property.ColumnProperty deptName = (Property.ColumnProperty) Entities.getProperty(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME);
    final EntityLookupModel lookupModel = model.createEntityLookupModel(fkProperty.getReferencedEntityID(), Arrays.asList(deptName), null);
    new EntityUiUtil.LookupValueLink(lookupModel, model, fkProperty.getPropertyID());
    assertTrue(lookupModel.getSelectedEntities().size() == 0);
    Entity department = model.getConnectionProvider().getConnection().selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "SALES");
    model.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, department);
    assertEquals(lookupModel.getSelectedEntities().size(), 1);
    assertEquals(lookupModel.getSelectedEntities().get(0), department);
    department = model.getConnectionProvider().getConnection().selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "OPERATIONS");
    lookupModel.setSelectedEntity(department);
    assertEquals(model.getValue(fkProperty.getPropertyID()), department);
  }
}
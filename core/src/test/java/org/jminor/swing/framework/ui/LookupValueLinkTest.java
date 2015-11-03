/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.framework.db.EntityConnectionProvidersTest;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.TestDomain;
import org.jminor.swing.framework.model.DefaultEntityEditModel;
import org.jminor.swing.framework.model.EntityEditModel;
import org.jminor.swing.framework.model.EntityLookupModel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LookupValueLinkTest {

  private final EntityEditModel model;

  public LookupValueLinkTest() {
    model = new DefaultEntityEditModel(TestDomain.T_EMP, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
  }

  @Test
  public void test() throws Exception {
    final Property.ForeignKeyProperty fkProperty = Entities.getForeignKeyProperty(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT_FK);
    final EntityLookupModel lookupModel = EntityUiUtil.createForeignKeyLookupField(fkProperty, model, TestDomain.DEPARTMENT_NAME).getModel();
    assertTrue(lookupModel.getSelectedEntities().size() == 0);
    Entity department = model.getConnectionProvider().getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");
    model.setValue(TestDomain.EMP_DEPARTMENT_FK, department);
    assertEquals(lookupModel.getSelectedEntities().size(), 1);
    assertEquals(lookupModel.getSelectedEntities().iterator().next(), department);
    department = model.getConnectionProvider().getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "OPERATIONS");
    lookupModel.setSelectedEntity(department);
    assertEquals(model.getValue(fkProperty.getPropertyID()), department);
  }
}
/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.model.testing.TestDomain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class SwingEntityModelProviderTest {

  private static final Entities ENTITIES = new TestDomain();

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(ENTITIES, new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray()), Databases.getInstance());

  @Test
  public void testDetailModelProvider() {
    final SwingEntityModelProvider departmentModelProvider = new SwingEntityModelProvider(TestDomain.T_DEPARTMENT)
            .setEditModelClass(DepartmentEditModel.class)
            .setTableModelClass(DepartmentTableModel.class);
    final SwingEntityModelProvider employeeModelProvider = new SwingEntityModelProvider(TestDomain.T_EMP);

    departmentModelProvider.addDetailModelProvider(employeeModelProvider);

    assertEquals(DepartmentEditModel.class, departmentModelProvider.getEditModelClass());
    assertEquals(DepartmentTableModel.class, departmentModelProvider.getTableModelClass());

    final SwingEntityModel departmentModel = departmentModelProvider.createModel(CONNECTION_PROVIDER, false);
    assertTrue(departmentModel.getEditModel() instanceof DepartmentEditModel);
    assertTrue(departmentModel.getTableModel() instanceof DepartmentTableModel);
    assertTrue(departmentModel.containsDetailModel(TestDomain.T_EMP));
  }

  static final class DepartmentEditModel extends SwingEntityEditModel {

    public DepartmentEditModel(final EntityConnectionProvider connectionProvider) {
      super(TestDomain.T_DEPARTMENT, connectionProvider);
    }
  }

  static final class DepartmentTableModel extends SwingEntityTableModel {

    public DepartmentTableModel(final EntityConnectionProvider connectionProvider) {
      super(TestDomain.T_DEPARTMENT, connectionProvider);
    }
  }
}

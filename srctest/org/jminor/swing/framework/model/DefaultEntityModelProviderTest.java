/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.EntityConnectionProvidersTest;
import org.jminor.framework.domain.TestDomain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DefaultEntityModelProviderTest {

  public DefaultEntityModelProviderTest() {
    TestDomain.init();
  }

  @Test
  public void testDetailModelProvider() {
    final EntityModelProvider departmentModelProvider = new DefaultEntityModelProvider(TestDomain.T_DEPARTMENT)
            .setEditModelClass(DepartmentEditModel.class)
            .setTableModelClass(DepartmentTableModel.class);
    final EntityModelProvider employeeModelProvider = new DefaultEntityModelProvider(TestDomain.T_EMP);

    departmentModelProvider.addDetailModelProvider(employeeModelProvider);

    assertEquals(DepartmentEditModel.class, departmentModelProvider.getEditModelClass());
    assertEquals(DepartmentTableModel.class, departmentModelProvider.getTableModelClass());

    final EntityModel departmentModel = departmentModelProvider.createModel(EntityConnectionProvidersTest.CONNECTION_PROVIDER, false);
    assertTrue(departmentModel.getEditModel() instanceof DepartmentEditModel);
    assertTrue(departmentModel.getTableModel() instanceof DepartmentTableModel);
    assertTrue(departmentModel.containsDetailModel(TestDomain.T_EMP));
  }

  static final class DepartmentEditModel extends DefaultEntityEditModel {

    public DepartmentEditModel(final EntityConnectionProvider connectionProvider) {
      super(TestDomain.T_DEPARTMENT, connectionProvider);
    }
  }

  static final class DepartmentTableModel extends DefaultEntityTableModel {

    public DepartmentTableModel(final EntityConnectionProvider connectionProvider) {
      super(TestDomain.T_DEPARTMENT, connectionProvider);
    }
  }
}

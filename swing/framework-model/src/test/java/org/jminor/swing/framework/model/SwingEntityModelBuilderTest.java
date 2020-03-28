/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.db.Databases;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.model.TestDomain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SwingEntityModelBuilderTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  @Test
  public void testDetailModelBuilder() {
    final SwingEntityModelBuilder departmentModelBuilder = new SwingEntityModelBuilder(TestDomain.T_DEPARTMENT)
            .setEditModelClass(DepartmentEditModel.class)
            .setTableModelClass(DepartmentTableModel.class);
    final SwingEntityModelBuilder employeeModelBuilder = new SwingEntityModelBuilder(TestDomain.T_EMP);

    departmentModelBuilder.addDetailModelBuilder(employeeModelBuilder);

    assertEquals(DepartmentEditModel.class, departmentModelBuilder.getEditModelClass());
    assertEquals(DepartmentTableModel.class, departmentModelBuilder.getTableModelClass());

    final SwingEntityModel departmentModel = departmentModelBuilder.createModel(CONNECTION_PROVIDER);
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

/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.framework.model;

import dev.codion.common.db.database.Databases;
import dev.codion.common.user.User;
import dev.codion.common.user.Users;
import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.db.local.LocalEntityConnectionProvider;
import dev.codion.framework.model.tests.TestDomain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class SwingEntityModelBuilderTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  @Test
  public void setModelClass() {
    assertThrows(IllegalStateException.class, () -> new SwingEntityModelBuilder(TestDomain.T_DEPARTMENT)
            .setEditModelClass(DepartmentEditModel.class).setModelClass(SwingEntityModel.class));
    assertThrows(IllegalStateException.class, () -> new SwingEntityModelBuilder(TestDomain.T_DEPARTMENT)
            .setTableModelClass(DepartmentTableModel.class).setModelClass(SwingEntityModel.class));

    assertThrows(IllegalStateException.class, () -> new SwingEntityModelBuilder(TestDomain.T_DEPARTMENT)
            .setModelClass(SwingEntityModel.class).setEditModelClass(DepartmentEditModel.class));
    assertThrows(IllegalStateException.class, () -> new SwingEntityModelBuilder(TestDomain.T_DEPARTMENT)
            .setModelClass(SwingEntityModel.class).setTableModelClass(DepartmentTableModel.class));
  }

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

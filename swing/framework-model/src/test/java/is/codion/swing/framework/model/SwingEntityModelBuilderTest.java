/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.db.database.Databases;
import is.codion.common.user.User;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.model.tests.TestDomain;

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
            .editModelClass(DepartmentEditModel.class).modelClass(SwingEntityModel.class));
    assertThrows(IllegalStateException.class, () -> new SwingEntityModelBuilder(TestDomain.T_DEPARTMENT)
            .tableModelClass(DepartmentTableModel.class).modelClass(SwingEntityModel.class));

    assertThrows(IllegalStateException.class, () -> new SwingEntityModelBuilder(TestDomain.T_DEPARTMENT)
            .modelClass(SwingEntityModel.class).editModelClass(DepartmentEditModel.class));
    assertThrows(IllegalStateException.class, () -> new SwingEntityModelBuilder(TestDomain.T_DEPARTMENT)
            .modelClass(SwingEntityModel.class).tableModelClass(DepartmentTableModel.class));
  }

  @Test
  public void testDetailModelBuilder() {
    final SwingEntityModelBuilder departmentModelBuilder = new SwingEntityModelBuilder(TestDomain.T_DEPARTMENT)
            .editModelClass(DepartmentEditModel.class)
            .tableModelClass(DepartmentTableModel.class);
    final SwingEntityModelBuilder employeeModelBuilder = new SwingEntityModelBuilder(TestDomain.T_EMP);

    departmentModelBuilder.detailModelBuilder(employeeModelBuilder);

    assertEquals(DepartmentEditModel.class, departmentModelBuilder.getEditModelClass());
    assertEquals(DepartmentTableModel.class, departmentModelBuilder.getTableModelClass());

    final SwingEntityModel departmentModel = departmentModelBuilder.buildModel(CONNECTION_PROVIDER);
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

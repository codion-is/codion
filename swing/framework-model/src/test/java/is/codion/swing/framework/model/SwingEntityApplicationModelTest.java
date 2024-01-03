/*
 * Copyright (c) 2016 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.model.test.AbstractEntityApplicationModelTest;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;

public final class SwingEntityApplicationModelTest
        extends AbstractEntityApplicationModelTest<SwingEntityModel, SwingEntityEditModel, SwingEntityTableModel> {

  @Override
  protected SwingEntityModel createDepartmentModel() {
    return new DeptModel(connectionProvider());
  }

  private static class DeptModel extends SwingEntityModel {
    private DeptModel(EntityConnectionProvider connectionProvider) {
      super(Department.TYPE, connectionProvider);
      addDetailModel(new SwingEntityModel(Employee.TYPE, connectionProvider));
    }
  }
}

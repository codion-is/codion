/*
 * Copyright (c) 2018 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.framework.model.test.AbstractEntityApplicationModelTest;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;

public final class DefaultEntityApplicationModelTest extends AbstractEntityApplicationModelTest<DefaultEntityModelTest.TestEntityModel,
        DefaultEntityModelTest.TestEntityEditModel, DefaultEntityModelTest.TestEntityTableModel> {

  @Override
  protected DefaultEntityModelTest.TestEntityModel createDepartmentModel() {
    DefaultEntityModelTest.TestEntityModel deptModel = new DefaultEntityModelTest.TestEntityModel(
            new DefaultEntityModelTest.TestEntityEditModel(Department.TYPE, connectionProvider()));
    DefaultEntityModelTest.TestEntityModel empModel = new DefaultEntityModelTest.TestEntityModel(
            new DefaultEntityModelTest.TestEntityEditModel(Employee.TYPE, connectionProvider()));
    deptModel.addDetailModel(empModel).setActive(true);

    return deptModel;
  }
}

/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.employees.testing.scenarios;

import is.codion.common.model.loadtest.LoadTest.Scenario.Performer;
import is.codion.framework.demos.employees.domain.Employees.Department;
import is.codion.framework.demos.employees.model.EmployeesAppModel;
import is.codion.swing.framework.model.SwingEntityModel;

import static is.codion.framework.domain.entity.test.EntityTestUtil.createRandomEntity;

// tag::loadTest[]
public final class InsertDepartment implements Performer<EmployeesAppModel> {

  @Override
  public void perform(EmployeesAppModel application) throws Exception {
    SwingEntityModel departmentModel = application.entityModel(Department.TYPE);
    departmentModel.editModel().set(createRandomEntity(application.entities(), Department.TYPE, null));
    departmentModel.editModel().insert();
  }
}
// end::loadTest[]
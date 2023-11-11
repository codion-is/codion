/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.employees.testing.scenarios;

import is.codion.framework.demos.employees.domain.Employees;
import is.codion.framework.demos.employees.model.EmployeesAppModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.tools.loadtest.AbstractEntityUsageScenario;

import static is.codion.framework.domain.entity.test.EntityTestUtil.createRandomEntity;

// tag::loadTest[]
public final class InsertDepartment extends AbstractEntityUsageScenario<EmployeesAppModel> {

  @Override
  protected void perform(EmployeesAppModel application) throws Exception {
    SwingEntityModel departmentModel = application.entityModel(Employees.Department.TYPE);
    departmentModel.editModel().set(createRandomEntity(application.entities(),
            Employees.Department.TYPE, null));
    departmentModel.editModel().insert();
  }
}
// end::loadTest[]
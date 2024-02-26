/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.employees.testing.scenarios;

import is.codion.common.model.loadtest.LoadTest.Scenario.Performer;
import is.codion.framework.demos.employees.domain.Employees.Department;
import is.codion.framework.demos.employees.model.EmployeesAppModel;

import static is.codion.swing.framework.model.tools.loadtest.EntityLoadTestUtil.selectRandomRow;

// tag::loadTest[]
public final class SelectDepartment implements Performer<EmployeesAppModel> {

  @Override
  public void perform(EmployeesAppModel application) {
    selectRandomRow(application.entityModel(Department.TYPE).tableModel());
  }
}
// end::loadTest[]
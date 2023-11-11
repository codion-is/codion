/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.employees.testing.scenarios;

import is.codion.framework.demos.employees.domain.Employees;
import is.codion.framework.demos.employees.model.EmployeesAppModel;
import is.codion.swing.framework.model.tools.loadtest.AbstractEntityUsageScenario;

import static is.codion.swing.framework.model.tools.loadtest.EntityLoadTestModel.selectRandomRow;

// tag::loadTest[]
public final class SelectDepartment extends AbstractEntityUsageScenario<EmployeesAppModel> {

  @Override
  protected void perform(EmployeesAppModel application) {
    selectRandomRow(application.entityModel(Employees.Department.TYPE).tableModel());
  }

  @Override
  public int defaultWeight() {
    return 10;
  }
}
// end::loadTest[]
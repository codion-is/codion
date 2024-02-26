/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.employees.testing.scenarios;

import is.codion.framework.demos.employees.domain.Employees.Department;
import is.codion.framework.demos.employees.model.EmployeesAppModel;
import is.codion.swing.framework.model.tools.loadtest.AbstractEntityScenario;

// tag::loadTest[]
public final class SelectDepartment extends AbstractEntityScenario<EmployeesAppModel> {

  @Override
  protected void perform(EmployeesAppModel application) {
    selectRandomRow(application.entityModel(Department.TYPE).tableModel());
  }

  @Override
  public int defaultWeight() {
    return 10;
  }
}
// end::loadTest[]
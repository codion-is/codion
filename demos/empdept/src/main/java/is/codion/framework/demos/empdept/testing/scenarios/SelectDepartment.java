/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.testing.scenarios;

import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.framework.demos.empdept.model.EmpDeptAppModel;
import is.codion.swing.framework.tools.loadtest.AbstractEntityUsageScenario;

import static is.codion.swing.framework.tools.loadtest.EntityLoadTestModel.selectRandomRow;

// tag::loadTest[]
public final class SelectDepartment extends AbstractEntityUsageScenario<EmpDeptAppModel> {

  @Override
  protected void perform(EmpDeptAppModel application) {
    selectRandomRow(application.entityModel(Department.TYPE).tableModel());
  }

  @Override
  public int defaultWeight() {
    return 10;
  }
}
// end::loadTest[]
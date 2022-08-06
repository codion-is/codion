/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.testing.scenarios;

import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.framework.demos.empdept.ui.EmpDeptAppPanel;
import is.codion.swing.framework.tools.loadtest.AbstractEntityUsageScenario;

import static is.codion.swing.framework.tools.loadtest.EntityLoadTestModel.selectRandomRow;

// tag::loadTest[]
public final class SelectDepartment extends AbstractEntityUsageScenario<EmpDeptAppPanel.EmpDeptApplicationModel> {

  @Override
  protected void perform(EmpDeptAppPanel.EmpDeptApplicationModel application) {
    selectRandomRow(application.entityModel(Department.TYPE).tableModel());
  }

  @Override
  public int getDefaultWeight() {
    return 10;
  }
}
// end::loadTest[]
/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.testing.scenarios;

import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.framework.demos.empdept.ui.EmpDeptAppPanel;
import is.codion.swing.common.tools.loadtest.ScenarioException;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.tools.loadtest.AbstractEntityUsageScenario;

import static is.codion.framework.domain.entity.test.EntityTestUnit.createRandomEntity;

// tag::loadTest[]
public final class InsertDepartment extends AbstractEntityUsageScenario<EmpDeptAppPanel.EmpDeptApplicationModel> {

  @Override
  protected void perform(final EmpDeptAppPanel.EmpDeptApplicationModel application) throws ScenarioException {
    try {
      final SwingEntityModel departmentModel = application.getEntityModel(Department.TYPE);
      departmentModel.getEditModel().setEntity(createRandomEntity(application.getEntities(),
              Department.TYPE, null));
      departmentModel.getEditModel().insert();
    }
    catch (final Exception e) {
      throw new ScenarioException(e);
    }
  }
}
// end::loadTest[]
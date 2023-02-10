/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.testing.scenarios;

import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.framework.demos.empdept.model.EmpDeptAppModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.tools.loadtest.AbstractEntityUsageScenario;

import static is.codion.framework.domain.entity.test.EntityTestUtil.createRandomEntity;

// tag::loadTest[]
public final class InsertDepartment extends AbstractEntityUsageScenario<EmpDeptAppModel> {

  @Override
  protected void perform(EmpDeptAppModel application) throws Exception {
    SwingEntityModel departmentModel = application.entityModel(Department.TYPE);
    departmentModel.editModel().setEntity(createRandomEntity(application.entities(),
            Department.TYPE, null));
    departmentModel.editModel().insert();
  }
}
// end::loadTest[]
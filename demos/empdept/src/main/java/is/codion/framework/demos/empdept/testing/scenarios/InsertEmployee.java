/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.testing.scenarios;

import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.framework.demos.empdept.model.EmpDeptAppModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.tools.loadtest.AbstractEntityUsageScenario;

import java.util.HashMap;
import java.util.Map;

import static is.codion.framework.domain.entity.test.EntityTestUtil.createRandomEntity;
import static is.codion.swing.framework.model.tools.loadtest.EntityLoadTestModel.selectRandomRow;

// tag::loadTest[]
public final class InsertEmployee extends AbstractEntityUsageScenario<EmpDeptAppModel> {

  @Override
  protected void perform(EmpDeptAppModel application) throws Exception {
    SwingEntityModel departmentModel = application.entityModel(Department.TYPE);
    selectRandomRow(departmentModel.tableModel());
    SwingEntityModel employeeModel = departmentModel.detailModel(Employee.TYPE);
    Map<ForeignKey, Entity> foreignKeyEntities = new HashMap<>();
    foreignKeyEntities.put(Employee.DEPARTMENT_FK, departmentModel.tableModel().selectionModel().getSelectedItem());
    employeeModel.editModel().setEntity(createRandomEntity(application.entities(), Employee.TYPE, foreignKeyEntities));
    employeeModel.editModel().insert();
  }

  @Override
  public int defaultWeight() {
    return 3;
  }
}
// end::loadTest[]
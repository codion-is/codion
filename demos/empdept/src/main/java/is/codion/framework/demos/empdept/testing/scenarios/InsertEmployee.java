/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.testing.scenarios;

import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.framework.demos.empdept.ui.EmpDeptAppPanel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.tools.loadtest.AbstractEntityUsageScenario;

import java.util.HashMap;
import java.util.Map;

import static is.codion.framework.domain.entity.test.EntityTestUtil.createRandomEntity;
import static is.codion.swing.framework.tools.loadtest.EntityLoadTestModel.selectRandomRow;

// tag::loadTest[]
public final class InsertEmployee extends AbstractEntityUsageScenario<EmpDeptAppPanel.EmpDeptApplicationModel> {

  @Override
  protected void perform(EmpDeptAppPanel.EmpDeptApplicationModel application) throws Exception {
    SwingEntityModel departmentModel = application.getEntityModel(Department.TYPE);
    selectRandomRow(departmentModel.getTableModel());
    SwingEntityModel employeeModel = departmentModel.getDetailModel(Employee.TYPE);
    Map<ForeignKey, Entity> foreignKeyEntities = new HashMap<>();
    foreignKeyEntities.put(Employee.DEPARTMENT_FK, departmentModel.getTableModel().getSelectionModel().getSelectedItem());
    employeeModel.getEditModel().setEntity(createRandomEntity(application.getEntities(), Employee.TYPE, foreignKeyEntities));
    employeeModel.getEditModel().insert();
  }

  @Override
  public int getDefaultWeight() {
    return 3;
  }
}
// end::loadTest[]
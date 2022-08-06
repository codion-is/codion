/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.testing.scenarios;

import is.codion.framework.db.EntityConnection;
import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.framework.demos.empdept.ui.EmpDeptAppPanel;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.tools.loadtest.AbstractEntityUsageScenario;

import java.util.Random;

import static is.codion.framework.domain.entity.test.EntityTestUtil.randomize;
import static is.codion.swing.framework.tools.loadtest.EntityLoadTestModel.selectRandomRow;

// tag::loadTest[]
public final class UpdateEmployee extends AbstractEntityUsageScenario<EmpDeptAppPanel.EmpDeptApplicationModel> {

  private final Random random = new Random();

  @Override
  protected void perform(EmpDeptAppPanel.EmpDeptApplicationModel application) throws Exception {
    SwingEntityModel departmentModel = application.entityModel(Department.TYPE);
    selectRandomRow(departmentModel.tableModel());
    SwingEntityModel employeeModel = departmentModel.detailModel(Employee.TYPE);
    if (employeeModel.tableModel().getRowCount() > 0) {
      EntityConnection connection = employeeModel.connectionProvider().connection();
      connection.beginTransaction();
      try {
        selectRandomRow(employeeModel.tableModel());
        Entity selected = employeeModel.tableModel().selectionModel().getSelectedItem();
        randomize(application.entities(), selected, null);
        employeeModel.editModel().setEntity(selected);
        employeeModel.editModel().update();
        selectRandomRow(employeeModel.tableModel());
        selected = employeeModel.tableModel().selectionModel().getSelectedItem();
        randomize(application.entities(), selected, null);
        employeeModel.editModel().setEntity(selected);
        employeeModel.editModel().update();
      }
      finally {
        if (random.nextBoolean()) {
          connection.rollbackTransaction();
        }
        else {
          connection.commitTransaction();
        }
      }
    }
  }

  @Override
  public int getDefaultWeight() {
    return 5;
  }
}
// end::loadTest[]
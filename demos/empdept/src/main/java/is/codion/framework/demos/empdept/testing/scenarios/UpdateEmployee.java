/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.testing.scenarios;

import is.codion.framework.db.EntityConnection;
import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.framework.demos.empdept.ui.EmpDeptAppPanel;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.tools.loadtest.ScenarioException;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.tools.loadtest.AbstractEntityUsageScenario;

import java.util.Random;

import static is.codion.framework.domain.entity.test.EntityTestUtil.randomize;
import static is.codion.swing.framework.tools.loadtest.EntityLoadTestModel.selectRandomRow;

// tag::loadTest[]
public final class UpdateEmployee extends AbstractEntityUsageScenario<EmpDeptAppPanel.EmpDeptApplicationModel> {

  private final Random random = new Random();

  @Override
  protected void perform(final EmpDeptAppPanel.EmpDeptApplicationModel application) throws ScenarioException {
    try {
      final SwingEntityModel departmentModel = application.getEntityModel(Department.TYPE);
      selectRandomRow(departmentModel.getTableModel());
      final SwingEntityModel employeeModel = departmentModel.getDetailModel(Employee.TYPE);
      if (employeeModel.getTableModel().getRowCount() > 0) {
        final EntityConnection connection = employeeModel.getConnectionProvider().getConnection();
        connection.beginTransaction();
        try {
          selectRandomRow(employeeModel.getTableModel());
          Entity selected = employeeModel.getTableModel().getSelectionModel().getSelectedItem();
          randomize(application.getEntities(), selected, null);
          employeeModel.getEditModel().setEntity(selected);
          employeeModel.getEditModel().update();
          selectRandomRow(employeeModel.getTableModel());
          selected = employeeModel.getTableModel().getSelectionModel().getSelectedItem();
          randomize(application.getEntities(), selected, null);
          employeeModel.getEditModel().setEntity(selected);
          employeeModel.getEditModel().update();
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
    catch (final Exception e) {
      throw new ScenarioException(e);
    }
  }

  @Override
  public int getDefaultWeight() {
    return 5;
  }
}
// end::loadTest[]
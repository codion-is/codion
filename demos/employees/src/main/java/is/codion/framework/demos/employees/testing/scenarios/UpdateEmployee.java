/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.employees.testing.scenarios;

import is.codion.framework.db.EntityConnection;
import is.codion.framework.demos.employees.domain.Employees.Department;
import is.codion.framework.demos.employees.domain.Employees.Employee;
import is.codion.framework.demos.employees.model.EmployeesAppModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.tools.loadtest.AbstractEntityPerformer;

import java.util.Random;

import static is.codion.framework.domain.entity.test.EntityTestUtil.randomize;

// tag::loadTest[]
public final class UpdateEmployee extends AbstractEntityPerformer<EmployeesAppModel> {

  private final Random random = new Random();

  @Override
  public void perform(EmployeesAppModel application) throws Exception {
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
        employeeModel.editModel().set(selected);
        employeeModel.editModel().update();
        selectRandomRow(employeeModel.tableModel());
        selected = employeeModel.tableModel().selectionModel().getSelectedItem();
        randomize(application.entities(), selected, null);
        employeeModel.editModel().set(selected);
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
}
// end::loadTest[]
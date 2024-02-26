/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.employees.testing.scenarios;

import is.codion.common.model.loadtest.LoadTest.Scenario.Performer;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.demos.employees.domain.Employees.Department;
import is.codion.framework.demos.employees.domain.Employees.Employee;
import is.codion.framework.demos.employees.model.EmployeesAppModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityModel;

import java.util.Random;

import static is.codion.framework.domain.entity.test.EntityTestUtil.randomize;
import static is.codion.swing.framework.model.tools.loadtest.EntityLoadTestUtil.selectRandomRow;

// tag::loadTest[]
public final class UpdateEmployee implements Performer<EmployeesAppModel> {

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
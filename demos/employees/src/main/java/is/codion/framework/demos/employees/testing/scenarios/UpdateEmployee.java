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
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.employees.testing.scenarios;

import is.codion.framework.db.EntityConnection;
import is.codion.framework.demos.employees.domain.Employees;
import is.codion.framework.demos.employees.model.EmployeesAppModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.tools.loadtest.AbstractEntityUsageScenario;

import java.util.Random;

import static is.codion.framework.domain.entity.test.EntityTestUtil.randomize;
import static is.codion.swing.framework.model.tools.loadtest.EntityLoadTestModel.selectRandomRow;

// tag::loadTest[]
public final class UpdateEmployee extends AbstractEntityUsageScenario<EmployeesAppModel> {

  private final Random random = new Random();

  @Override
  protected void perform(EmployeesAppModel application) throws Exception {
    SwingEntityModel departmentModel = application.entityModel(Employees.Department.TYPE);
    selectRandomRow(departmentModel.tableModel());
    SwingEntityModel employeeModel = departmentModel.detailModel(Employees.Employee.TYPE);
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

  @Override
  public int defaultWeight() {
    return 5;
  }
}
// end::loadTest[]
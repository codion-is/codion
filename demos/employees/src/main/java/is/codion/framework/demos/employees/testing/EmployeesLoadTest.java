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
package is.codion.framework.demos.employees.testing;

import is.codion.common.model.loadtest.LoadTest;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.employees.domain.Employees;
import is.codion.framework.demos.employees.domain.Employees.Department;
import is.codion.framework.demos.employees.domain.Employees.Employee;
import is.codion.framework.demos.employees.model.EmployeesAppModel;
import is.codion.framework.demos.employees.testing.scenarios.InsertDepartment;
import is.codion.framework.demos.employees.testing.scenarios.InsertEmployee;
import is.codion.framework.demos.employees.testing.scenarios.LoginLogout;
import is.codion.framework.demos.employees.testing.scenarios.SelectDepartment;
import is.codion.framework.demos.employees.testing.scenarios.UpdateEmployee;
import is.codion.swing.common.model.tools.loadtest.LoadTestModel;
import is.codion.swing.common.ui.tools.loadtest.LoadTestPanel;
import is.codion.swing.framework.model.SwingEntityModel;

import java.util.function.Function;

import static java.util.Arrays.asList;

// tag::loadTest[]
public final class EmployeesLoadTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final class EmployeesAppModelFactory
          implements Function<User, EmployeesAppModel> {

    @Override
    public EmployeesAppModel apply(User user) {
      EmployeesAppModel applicationModel =
              new EmployeesAppModel(EntityConnectionProvider.builder()
                      .domainType(Employees.DOMAIN)
                      .clientTypeId(EmployeesLoadTest.class.getSimpleName())
                      .user(user)
                      .build());

      SwingEntityModel model = applicationModel.entityModel(Department.TYPE);
      model.detailModelLink(model.detailModel(Employee.TYPE)).active().set(true);
      try {
        model.tableModel().refresh();
      }
      catch (Exception ignored) {/*ignored*/}

      return applicationModel;
    }
  }

  public static void main(String[] args) {
    LoadTest<EmployeesAppModel> loadTest =
            LoadTest.builder(new EmployeesAppModelFactory(),
                            application -> application.connectionProvider().close())
                    .user(UNIT_TEST_USER)
                    .usageScenarios(asList(new InsertDepartment(), new InsertEmployee(), new LoginLogout(),
                            new SelectDepartment(), new UpdateEmployee()))
                    .titleFactory(model -> "Employees LoadTest - " + EntityConnectionProvider.CLIENT_CONNECTION_TYPE.get())
                    .build();
    new LoadTestPanel<>(LoadTestModel.loadTestModel(loadTest)).run();
  }
}
// end::loadTest[]
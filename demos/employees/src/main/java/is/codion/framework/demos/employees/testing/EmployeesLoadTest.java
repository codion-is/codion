/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
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
import is.codion.swing.framework.model.SwingEntityModel;

import java.util.function.Function;

import static is.codion.common.model.loadtest.LoadTest.Scenario.scenario;
import static is.codion.swing.common.model.tools.loadtest.LoadTestModel.loadTestModel;
import static is.codion.swing.common.ui.tools.loadtest.LoadTestPanel.loadTestPanel;
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
                    .scenarios(asList(
                            scenario(new InsertDepartment(), 1),
                            scenario(new InsertEmployee(), 3),
                            scenario(new LoginLogout(), 4),
                            scenario(new SelectDepartment(), 10),
                            scenario(new UpdateEmployee(), 5)))
                    .titleFactory(model -> "Employees LoadTest - " + EntityConnectionProvider.CLIENT_CONNECTION_TYPE.get())
                    .build();
    loadTestPanel(loadTestModel(loadTest)).run();
  }
}
// end::loadTest[]
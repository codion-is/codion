/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.employees.testing;

import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.employees.domain.Employees;
import is.codion.framework.demos.employees.model.EmployeesAppModel;
import is.codion.framework.demos.employees.testing.scenarios.InsertDepartment;
import is.codion.framework.demos.employees.testing.scenarios.InsertEmployee;
import is.codion.framework.demos.employees.testing.scenarios.LoginLogout;
import is.codion.framework.demos.employees.testing.scenarios.SelectDepartment;
import is.codion.framework.demos.employees.testing.scenarios.UpdateEmployee;
import is.codion.swing.common.ui.tools.loadtest.LoadTestPanel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.tools.loadtest.EntityLoadTestModel;

import static java.util.Arrays.asList;

// tag::loadTest[]
public final class EmployeesLoadTest extends EntityLoadTestModel<EmployeesAppModel> {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  public EmployeesLoadTest() {
    super(UNIT_TEST_USER, asList(new InsertDepartment(), new InsertEmployee(), new LoginLogout(),
            new SelectDepartment(), new UpdateEmployee()));
  }

  @Override
  protected EmployeesAppModel createApplication(User user) throws CancelException {
    EmployeesAppModel applicationModel =
            new EmployeesAppModel(EntityConnectionProvider.builder()
                    .domainType(Employees.DOMAIN)
                    .clientTypeId(EmployeesLoadTest.class.getSimpleName())
                    .user(user)
                    .build());

    SwingEntityModel model = applicationModel.entityModel(Employees.Department.TYPE);
    model.detailModelLink(model.detailModel(Employees.Employee.TYPE)).active().set(true);
    try {
      model.tableModel().refresh();
    }
    catch (Exception ignored) {/*ignored*/}

    return applicationModel;
  }

  public static void main(String[] args) {
    new LoadTestPanel<>(new EmployeesLoadTest().loadTestModel()).run();
  }
}
// end::loadTest[]
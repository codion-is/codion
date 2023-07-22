/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.testing;

import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.empdept.domain.EmpDept;
import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.framework.demos.empdept.model.EmpDeptAppModel;
import is.codion.framework.demos.empdept.testing.scenarios.InsertDepartment;
import is.codion.framework.demos.empdept.testing.scenarios.InsertEmployee;
import is.codion.framework.demos.empdept.testing.scenarios.LoginLogout;
import is.codion.framework.demos.empdept.testing.scenarios.SelectDepartment;
import is.codion.framework.demos.empdept.testing.scenarios.UpdateEmployee;
import is.codion.swing.common.ui.tools.loadtest.LoadTestPanel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.tools.loadtest.EntityLoadTestModel;

import static java.util.Arrays.asList;

// tag::loadTest[]
public final class EmpDeptLoadTest extends EntityLoadTestModel<EmpDeptAppModel> {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  public EmpDeptLoadTest() {
    super(UNIT_TEST_USER, asList(new InsertDepartment(), new InsertEmployee(), new LoginLogout(),
            new SelectDepartment(), new UpdateEmployee()));
  }

  @Override
  protected EmpDeptAppModel createApplication() throws CancelException {
    EmpDeptAppModel applicationModel =
            new EmpDeptAppModel(EntityConnectionProvider.builder()
                    .domainType(EmpDept.DOMAIN)
                    .clientTypeId(EmpDeptLoadTest.class.getSimpleName())
                    .user(getUser())
                    .build());

    SwingEntityModel model = applicationModel.entityModel(Department.TYPE);
    model.detailModelLink(model.detailModel(Employee.TYPE)).setActive(true);
    try {
      model.tableModel().refresh();
    }
    catch (Exception ignored) {/*ignored*/}

    return applicationModel;
  }

  public static void main(String[] args) {
    new LoadTestPanel<>(new EmpDeptLoadTest()).run();
  }
}
// end::loadTest[]
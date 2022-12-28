/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.testing;

import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.empdept.domain.EmpDept;
import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.framework.demos.empdept.testing.scenarios.InsertDepartment;
import is.codion.framework.demos.empdept.testing.scenarios.InsertEmployee;
import is.codion.framework.demos.empdept.testing.scenarios.LoginLogout;
import is.codion.framework.demos.empdept.testing.scenarios.SelectDepartment;
import is.codion.framework.demos.empdept.testing.scenarios.UpdateEmployee;
import is.codion.framework.demos.empdept.ui.EmpDeptAppPanel;
import is.codion.swing.common.tools.ui.loadtest.LoadTestPanel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.tools.loadtest.EntityLoadTestModel;

import javax.swing.SwingUtilities;

import static java.util.Arrays.asList;

// tag::loadTest[]
public final class EmpDeptLoadTest extends EntityLoadTestModel<EmpDeptAppPanel.EmpDeptApplicationModel> {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  public EmpDeptLoadTest() {
    super(UNIT_TEST_USER, asList(new InsertDepartment(), new InsertEmployee(), new LoginLogout(),
            new SelectDepartment(), new UpdateEmployee()));
  }

  @Override
  protected EmpDeptAppPanel.EmpDeptApplicationModel createApplication() throws CancelException {
    EmpDeptAppPanel.EmpDeptApplicationModel applicationModel =
            new EmpDeptAppPanel.EmpDeptApplicationModel(EntityConnectionProvider.builder()
                    .domainClassName(EmpDept.class.getName())
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

  public static void main(String[] args) throws Exception {
    SwingUtilities.invokeLater(new Runner());
  }

  private static final class Runner implements Runnable {
    @Override
    public void run() {
      try {
        new LoadTestPanel<>(new EmpDeptLoadTest()).showFrame();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
// end::loadTest[]
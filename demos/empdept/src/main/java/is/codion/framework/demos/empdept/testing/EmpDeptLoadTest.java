/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.testing;

import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnectionProviders;
import is.codion.framework.demos.empdept.domain.EmpDept;
import is.codion.framework.demos.empdept.ui.EmpDeptAppPanel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.test.EntityTestUnit;
import is.codion.framework.domain.identity.Identity;
import is.codion.framework.model.EntityApplicationModel;
import is.codion.framework.model.EntityModel;
import is.codion.swing.common.tools.loadtest.ScenarioException;
import is.codion.swing.common.tools.ui.loadtest.LoadTestPanel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.tools.loadtest.EntityLoadTestModel;

import javax.swing.SwingUtilities;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static java.util.Arrays.asList;

// tag::loadTest[]
public final class EmpDeptLoadTest extends EntityLoadTestModel {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  public EmpDeptLoadTest() {
    super(UNIT_TEST_USER, asList(new InsertDepartment(), new InsertEmployee(), new LoginLogout(),
            new SelectDepartment(), new UpdateEmployee()));
  }

  @Override
  protected EntityApplicationModel initializeApplication() throws CancelException {
    final EntityApplicationModel applicationModel = new EmpDeptAppPanel.EmpDeptApplicationModel(
            EntityConnectionProviders.connectionProvider().setDomainClassName(EmpDept.class.getName())
                    .setClientTypeId(EmpDeptLoadTest.class.getSimpleName())
                    .setUser(getUser()));
    final EntityModel deptModel = new SwingEntityModel(EmpDept.T_DEPARTMENT, applicationModel.getConnectionProvider());
    deptModel.addDetailModel(new SwingEntityModel(EmpDept.T_EMPLOYEE, applicationModel.getConnectionProvider()));
    applicationModel.addEntityModel(deptModel);

    final EntityModel model = applicationModel.getEntityModel(EmpDept.T_DEPARTMENT);
    model.addLinkedDetailModel(model.getDetailModel(EmpDept.T_EMPLOYEE));
    try {
      model.refresh();
    }
    catch (final Exception ignored) {/*ignored*/}

    return applicationModel;
  }

  private static final class SelectDepartment extends AbstractEntityUsageScenario<EmpDeptAppPanel.EmpDeptApplicationModel> {
    @Override
    protected void perform(final EmpDeptAppPanel.EmpDeptApplicationModel application) {
      selectRandomRow(application.getEntityModel(EmpDept.T_DEPARTMENT).getTableModel());
    }
    @Override
    public int getDefaultWeight() {
      return 10;
    }
  }

  private static final class UpdateEmployee extends AbstractEntityUsageScenario<EmpDeptAppPanel.EmpDeptApplicationModel> {

    private final Random random = new Random();

    @Override
    protected void perform(final EmpDeptAppPanel.EmpDeptApplicationModel application) throws ScenarioException {
      try {
        final SwingEntityModel departmentModel = application.getEntityModel(EmpDept.T_DEPARTMENT);
        selectRandomRow(departmentModel.getTableModel());
        final SwingEntityModel employeeModel = departmentModel.getDetailModel(EmpDept.T_EMPLOYEE);
        if (employeeModel.getTableModel().getRowCount() > 0) {
          employeeModel.getConnectionProvider().getConnection().beginTransaction();
          try {
            selectRandomRow(employeeModel.getTableModel());
            Entity selected = employeeModel.getTableModel().getSelectionModel().getSelectedItem();
            EntityTestUnit.randomize(application.getEntities(), selected, null);
            employeeModel.getEditModel().setEntity(selected);
            employeeModel.getEditModel().update();
            selectRandomRow(employeeModel.getTableModel());
            selected = employeeModel.getTableModel().getSelectionModel().getSelectedItem();
            EntityTestUnit.randomize(application.getEntities(), selected, null);
            employeeModel.getEditModel().setEntity(selected);
            employeeModel.getEditModel().update();
          }
          finally {
            if (random.nextBoolean()) {
              employeeModel.getConnectionProvider().getConnection().rollbackTransaction();
            }
            else {
              employeeModel.getConnectionProvider().getConnection().commitTransaction();
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

  private static final class InsertEmployee extends AbstractEntityUsageScenario<EmpDeptAppPanel.EmpDeptApplicationModel> {
    @Override
    protected void perform(final EmpDeptAppPanel.EmpDeptApplicationModel application) throws ScenarioException {
      try {
        final SwingEntityModel departmentModel = application.getEntityModel(EmpDept.T_DEPARTMENT);
        selectRandomRow(departmentModel.getTableModel());
        final SwingEntityModel employeeModel = departmentModel.getDetailModel(EmpDept.T_EMPLOYEE);
        final Map<Identity, Entity> references = new HashMap<>();
        references.put(EmpDept.T_DEPARTMENT, departmentModel.getTableModel().getSelectionModel().getSelectedItem());
        employeeModel.getEditModel().setEntity(EntityTestUnit.createRandomEntity(application.getEntities(), EmpDept.T_EMPLOYEE, references));
        employeeModel.getEditModel().insert();
      }
      catch (final Exception e) {
        throw new ScenarioException(e);
      }
    }
    @Override
    public int getDefaultWeight() {
      return 3;
    }
  }

  private static final class InsertDepartment extends AbstractEntityUsageScenario<EmpDeptAppPanel.EmpDeptApplicationModel> {
    @Override
    protected void perform(final EmpDeptAppPanel.EmpDeptApplicationModel application) throws ScenarioException {
      try {
        final SwingEntityModel departmentModel = application.getEntityModel(EmpDept.T_DEPARTMENT);
        departmentModel.getEditModel().setEntity(EntityTestUnit.createRandomEntity(application.getEntities(), EmpDept.T_DEPARTMENT, null));
        departmentModel.getEditModel().insert();
      }
      catch (final Exception e) {
        throw new ScenarioException(e);
      }
    }
  }

  private static final class LoginLogout extends AbstractEntityUsageScenario<EmpDeptAppPanel.EmpDeptApplicationModel> {
    final Random random = new Random();
    @Override
    protected void perform(final EmpDeptAppPanel.EmpDeptApplicationModel application) {
      try {
        application.getConnectionProvider().disconnect();
        Thread.sleep(random.nextInt(1500));
        application.getConnectionProvider().getConnection();
      }
      catch (final InterruptedException ignored) {/*ignored*/}
    }
    @Override
    public int getDefaultWeight() {
      return 4;
    }
  }

  public static void main(final String[] args) throws Exception {
    SwingUtilities.invokeLater(new Runner());
  }

  private static final class Runner implements Runnable {
    @Override
    public void run() {
      try {
        new LoadTestPanel(new EmpDeptLoadTest()).showFrame();
      }
      catch (final Exception e) {
        e.printStackTrace();
      }
    }
  }
}
// end::loadTest[]
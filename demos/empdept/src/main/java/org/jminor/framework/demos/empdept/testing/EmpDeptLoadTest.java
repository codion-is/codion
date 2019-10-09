/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.testing;

import org.jminor.common.User;
import org.jminor.common.model.CancelException;
import org.jminor.framework.db.EntityConnectionProviders;
import org.jminor.framework.demos.empdept.client.ui.EmpDeptAppPanel;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.test.EntityTestUnit;
import org.jminor.framework.model.EntityApplicationModel;
import org.jminor.framework.model.EntityModel;
import org.jminor.swing.common.tools.ui.LoadTestPanel;
import org.jminor.swing.framework.model.SwingEntityModel;
import org.jminor.swing.framework.tools.EntityLoadTestModel;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static java.util.Arrays.asList;

public final class EmpDeptLoadTest extends EntityLoadTestModel {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

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
    protected void performScenario(final EmpDeptAppPanel.EmpDeptApplicationModel application) {
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
    protected void performScenario(final EmpDeptAppPanel.EmpDeptApplicationModel application) throws ScenarioException {
      try {
        final SwingEntityModel departmentModel = application.getEntityModel(EmpDept.T_DEPARTMENT);
        selectRandomRow(departmentModel.getTableModel());
        final SwingEntityModel employeeModel = departmentModel.getDetailModel(EmpDept.T_EMPLOYEE);
        if (employeeModel.getTableModel().getRowCount() > 0) {
          employeeModel.getConnectionProvider().getConnection().beginTransaction();
          try {
            selectRandomRow(employeeModel.getTableModel());
            Entity selected = employeeModel.getTableModel().getSelectionModel().getSelectedItem();
            EntityTestUnit.randomize(application.getDomain(), selected, null);
            employeeModel.getEditModel().setEntity(selected);
            employeeModel.getEditModel().update();
            selectRandomRow(employeeModel.getTableModel());
            selected = employeeModel.getTableModel().getSelectionModel().getSelectedItem();
            EntityTestUnit.randomize(application.getDomain(), selected, null);
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
    protected void performScenario(final EmpDeptAppPanel.EmpDeptApplicationModel application) throws ScenarioException {
      try {
        final SwingEntityModel departmentModel = application.getEntityModel(EmpDept.T_DEPARTMENT);
        selectRandomRow(departmentModel.getTableModel());
        final SwingEntityModel employeeModel = departmentModel.getDetailModel(EmpDept.T_EMPLOYEE);
        final Map<String, Entity> references = new HashMap<>();
        references.put(EmpDept.T_DEPARTMENT, departmentModel.getTableModel().getSelectionModel().getSelectedItem());
        employeeModel.getEditModel().setEntity(EntityTestUnit.createRandomEntity(application.getDomain(), EmpDept.T_EMPLOYEE, references));
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
    protected void performScenario(final EmpDeptAppPanel.EmpDeptApplicationModel application) throws ScenarioException {
      try {
        final SwingEntityModel departmentModel = application.getEntityModel(EmpDept.T_DEPARTMENT);
        departmentModel.getEditModel().setEntity(EntityTestUnit.createRandomEntity(application.getDomain(), EmpDept.T_DEPARTMENT, null));
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
    protected void performScenario(final EmpDeptAppPanel.EmpDeptApplicationModel application) {
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
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        new LoadTestPanel(new EmpDeptLoadTest()).showFrame();
      }
      catch (final Exception e) {
        e.printStackTrace();
      }
    }
  }
}

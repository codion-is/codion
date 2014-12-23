/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.testing;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.common.model.tools.ScenarioException;
import org.jminor.common.ui.tools.LoadTestPanel;
import org.jminor.framework.client.model.DefaultEntityApplicationModel;
import org.jminor.framework.client.model.DefaultEntityModel;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.provider.EntityConnectionProviders;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.tools.testing.EntityLoadTestModel;
import org.jminor.framework.tools.testing.EntityTestUnit;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@SuppressWarnings({"UnusedDeclaration"})
public final class EmpDeptLoadTest extends EntityLoadTestModel {

  public EmpDeptLoadTest() {
    super(User.UNIT_TEST_USER, Arrays.asList(new InsertDepartment(), new InsertEmployee(), new LoginLogout(),
            new SelectDepartment(), new UpdateEmployee()));
  }

  @Override
  protected EntityApplicationModel initializeApplication() throws CancelException {
    final EntityApplicationModel applicationModel = new DefaultEntityApplicationModel(
            EntityConnectionProviders.createConnectionProvider(getUser(), EmpDeptLoadTest.class.getSimpleName())) {
      @Override
      protected void loadDomainModel() {
        EmpDept.init();
      }
    };
    final EntityModel deptModel = new DefaultEntityModel(EmpDept.T_DEPARTMENT, applicationModel.getConnectionProvider());
    deptModel.addDetailModel(new DefaultEntityModel(EmpDept.T_EMPLOYEE, applicationModel.getConnectionProvider()));
    applicationModel.addEntityModel(deptModel);

    final EntityModel model = applicationModel.getEntityModel(EmpDept.T_DEPARTMENT);
    model.addLinkedDetailModel(model.getDetailModel(EmpDept.T_EMPLOYEE));
    try {
      model.refresh();
    }
    catch (final Exception ignored) {}

    return applicationModel;
  }

  private static final class SelectDepartment extends AbstractEntityUsageScenario {
    @Override
    protected void performScenario(final EntityApplicationModel application) {
      selectRandomRow(application.getEntityModel(EmpDept.T_DEPARTMENT).getTableModel());
    }
    @Override
    public int getDefaultWeight() {
      return 10;
    }
  }

  private static final class UpdateEmployee extends AbstractEntityUsageScenario {

    private final Random random = new Random();

    @Override
    protected void performScenario(final EntityApplicationModel application) throws ScenarioException {
      try {
        final EntityModel departmentModel = application.getEntityModel(EmpDept.T_DEPARTMENT);
        selectRandomRow(departmentModel.getTableModel());
        final EntityModel employeeModel = departmentModel.getDetailModel(EmpDept.T_EMPLOYEE);
        if (employeeModel.getTableModel().getRowCount() > 0) {
          employeeModel.getConnectionProvider().getConnection().beginTransaction();
          try {
            selectRandomRow(employeeModel.getTableModel());
            Entity selected = employeeModel.getTableModel().getSelectionModel().getSelectedItem();
            EntityTestUnit.randomize(selected, false, null);
            employeeModel.getEditModel().setEntity(selected);
            employeeModel.getEditModel().update();
            selectRandomRow(employeeModel.getTableModel());
            selected = employeeModel.getTableModel().getSelectionModel().getSelectedItem();
            EntityTestUnit.randomize(selected, false, null);
            employeeModel.getEditModel().setEntity(selected);
            employeeModel.getEditModel().update();
          }
          finally {
            if (random.nextDouble() < 0.5) {
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

  private static final class InsertEmployee extends EntityLoadTestModel.AbstractEntityUsageScenario {
    @Override
    protected void performScenario(final EntityApplicationModel application) throws ScenarioException {
      try {
        final EntityModel departmentModel = application.getEntityModel(EmpDept.T_DEPARTMENT);
        selectRandomRow(departmentModel.getTableModel());
        final EntityModel employeeModel = departmentModel.getDetailModel(EmpDept.T_EMPLOYEE);
        final Map<String, Entity> references = new HashMap<>();
        references.put(EmpDept.T_DEPARTMENT, departmentModel.getTableModel().getSelectionModel().getSelectedItem());
        employeeModel.getEditModel().setEntity(EntityTestUnit.createRandomEntity(EmpDept.T_EMPLOYEE, references));
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

  private static final class InsertDepartment extends AbstractEntityUsageScenario {
    @Override
    protected void performScenario(final EntityApplicationModel application) throws ScenarioException {
      try {
        final EntityModel departmentModel = application.getEntityModel(EmpDept.T_DEPARTMENT);
        departmentModel.getEditModel().setEntity(EntityTestUnit.createRandomEntity(EmpDept.T_DEPARTMENT, null));
        departmentModel.getEditModel().insert();
      }
      catch (final Exception e) {
        throw new ScenarioException(e);
      }
    }

    @Override
    public int getDefaultWeight() {
      return 1;
    }
  }

  private static final class LoginLogout extends AbstractEntityUsageScenario {
    final Random random = new Random();
    @Override
    protected void performScenario(final EntityApplicationModel application) {
      try {
        application.getConnectionProvider().disconnect();
        Thread.sleep(random.nextInt(1500));
        application.getConnectionProvider().getConnection();
      }
      catch (final InterruptedException ignored) {}
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

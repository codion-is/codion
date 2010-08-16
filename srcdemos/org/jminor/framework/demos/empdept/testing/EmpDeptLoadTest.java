/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.testing;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.common.ui.LoadTestPanel;
import org.jminor.framework.client.model.DefaultEntityApplicationModel;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.provider.EntityDbProviderFactory;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.tools.testing.EntityLoadTestModel;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * User: Bjorn Darri
 * Date: 30.11.2007
 * Time: 03:33:10
 */
@SuppressWarnings({"UnusedDeclaration"})
public final class EmpDeptLoadTest extends EntityLoadTestModel {

  public EmpDeptLoadTest() {
    super(User.UNIT_TEST_USER, new InsertDepartment(), new InsertEmployee(), new LoginLogout(),
            new SelectDepartment(), new UpdateEmployee());
  }

  @Override
  protected void loadDomainModel() {
    new EmpDept();
  }

  @Override
  protected EntityApplicationModel initializeApplication() throws CancelException {
    final EntityApplicationModel applicationModel = new DefaultEntityApplicationModel(
            EntityDbProviderFactory.createEntityDbProvider(getUser(), EmpDeptLoadTest.class.getSimpleName())) {
      @Override
      protected void loadDomainModel() {
        new EmpDept();
      }
    };

    final EntityModel model = applicationModel.getMainApplicationModel(EmpDept.T_DEPARTMENT);
    model.setLinkedDetailModels(model.getDetailModel(EmpDept.T_EMPLOYEE));
    model.refresh();

    return applicationModel;
  }

  private static final class SelectDepartment extends UsageScenario {
    @Override
    protected void performScenario(final Object application) throws ScenarioException {
      selectRandomRow(((EntityApplicationModel) application).getMainApplicationModel(EmpDept.T_DEPARTMENT).getTableModel());
    }
    @Override
    protected int getDefaultWeight() {
      return 10;
    }
  }

  private static final class UpdateEmployee extends UsageScenario {
    @Override
      protected void performScenario(final Object application) throws ScenarioException {
      try {
        final EntityModel departmentModel = ((EntityApplicationModel) application).getMainApplicationModel(EmpDept.T_DEPARTMENT);
        selectRandomRow(departmentModel.getTableModel());
        final EntityModel employeeModel = departmentModel.getDetailModel(EmpDept.T_EMPLOYEE);
        if (employeeModel.getTableModel().getRowCount() > 0) {
          selectRandomRow(employeeModel.getTableModel());
          final Entity selected = employeeModel.getTableModel().getSelectedItem();
          EntityUtil.randomize(selected, false, null);
          employeeModel.getEditModel().setEntity(selected);
          employeeModel.getEditModel().update();
        }
      }
      catch (Exception e) {
        throw new ScenarioException(e);
      }
    }
      @Override
      protected int getDefaultWeight() {
        return 5;
      }
  }

  private static final class InsertEmployee extends UsageScenario {
    @Override
    protected void performScenario(final Object application) throws ScenarioException {
      try {
        final EntityModel departmentModel = ((EntityApplicationModel) application).getMainApplicationModel(EmpDept.T_DEPARTMENT);
        selectRandomRow(departmentModel.getTableModel());
        final EntityModel employeeModel = departmentModel.getDetailModel(EmpDept.T_EMPLOYEE);
        final Map<String, Entity> references = new HashMap<String, Entity>();
        references.put(EmpDept.T_DEPARTMENT, departmentModel.getTableModel().getSelectedItem());
        employeeModel.getEditModel().setValueMap(EntityUtil.createRandomEntity(EmpDept.T_EMPLOYEE, references));
        employeeModel.getEditModel().insert();
      }
      catch (Exception e) {
        throw new ScenarioException(e);
      }
    }
    @Override
    protected int getDefaultWeight() {
      return 3;
    }
  }

  private static final class InsertDepartment extends UsageScenario {
    @Override
      protected void performScenario(final Object application) throws ScenarioException {
      try {
        final EntityModel departmentModel = ((EntityApplicationModel) application).getMainApplicationModel(EmpDept.T_DEPARTMENT);
        departmentModel.getEditModel().setValueMap(EntityUtil.createRandomEntity(EmpDept.T_DEPARTMENT, null));
        departmentModel.getEditModel().insert();
      }
      catch (Exception e) {
        throw new ScenarioException(e);
      }
    }
      @Override
      protected int getDefaultWeight() {
        return 1;
      }
  }

  private static final class LoginLogout extends UsageScenario {
    final Random random = new Random();
    @Override
      protected void performScenario(final Object application) throws ScenarioException {
      try {
        ((EntityApplicationModel) application).getDbProvider().disconnect();
        Thread.sleep(random.nextInt(1500));
        ((EntityApplicationModel) application).getDbProvider().getEntityDb();
      }
      catch (InterruptedException e) {/**/}
    }
      @Override
      protected int getDefaultWeight() {
        return 4;
      }
  }

  public static void main(final String[] args) throws Exception {
    SwingUtilities.invokeLater(new Runner());
  }

  private static final class Runner implements Runnable {
    public void run() {
      try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        new LoadTestPanel(new EmpDeptLoadTest()).showFrame();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}

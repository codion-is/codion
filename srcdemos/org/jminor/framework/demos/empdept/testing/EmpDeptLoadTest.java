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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Bjorn Darri
 * Date: 30.11.2007
 * Time: 03:33:10
 */
@SuppressWarnings({"UnusedDeclaration"})
public class EmpDeptLoadTest extends EntityLoadTestModel {

  public EmpDeptLoadTest() {
    super(User.UNIT_TEST_USER);
  }

  @Override
  protected Collection<UsageScenario> initializeUsageScenarios() {
    final UsageScenario selectDepartment = new UsageScenario("selectDepartment") {
      @Override
      protected void performScenario(final Object application) throws Exception {
        selectRandomRow(((EntityApplicationModel) application).getMainApplicationModel(EmpDept.T_DEPARTMENT).getTableModel());
      }
      @Override
      protected int getDefaultWeight() {
        return 10;
      }
    };
    final UsageScenario updateEmployee = new UsageScenario("updateEmployee") {
      @Override
      protected void performScenario(final Object application) throws Exception {
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
      @Override
      protected int getDefaultWeight() {
        return 5;
      }
    };
    final UsageScenario insertEmployee = new UsageScenario("insertEmployee") {
      @Override
      protected void performScenario(final Object application) throws Exception {
        final EntityModel departmentModel = ((EntityApplicationModel) application).getMainApplicationModel(EmpDept.T_DEPARTMENT);
        selectRandomRow(departmentModel.getTableModel());
        final EntityModel employeeModel = departmentModel.getDetailModel(EmpDept.T_EMPLOYEE);
        final Map<String, Entity> references = new HashMap<String, Entity>();
        references.put(EmpDept.T_DEPARTMENT, departmentModel.getTableModel().getSelectedItem());
        employeeModel.getEditModel().setValueMap(EntityUtil.createRandomEntity(EmpDept.T_EMPLOYEE, references));
        employeeModel.getEditModel().insert();
      }
      @Override
      protected int getDefaultWeight() {
        return 3;
      }
    };
    final UsageScenario insertDepartment = new UsageScenario("insertDepartment") {
      @Override
      protected void performScenario(final Object application) throws Exception {
        final EntityModel departmentModel = ((EntityApplicationModel) application).getMainApplicationModel(EmpDept.T_DEPARTMENT);
        departmentModel.getEditModel().setValueMap(EntityUtil.createRandomEntity(EmpDept.T_DEPARTMENT, null));
        departmentModel.getEditModel().insert();
      }
      @Override
      protected int getDefaultWeight() {
        return 1;
      }
    };
    final UsageScenario logoutLogin = new UsageScenario("logoutLogin") {
      @Override
      protected void performScenario(final Object application) throws Exception {
        ((EntityApplicationModel) application).getDbProvider().disconnect();
        think();
        ((EntityApplicationModel) application).getDbProvider().getEntityDb();
      }
      @Override
      protected int getDefaultWeight() {
        return 4;
      }
    };

    return Arrays.asList(insertDepartment, insertEmployee, updateEmployee, selectDepartment, logoutLogin);
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

  public static void main(String[] args) throws Exception {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
          new LoadTestPanel(new EmpDeptLoadTest()).showFrame();
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }
}

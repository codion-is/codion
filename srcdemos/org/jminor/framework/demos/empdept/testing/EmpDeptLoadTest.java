/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.testing;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.common.ui.LoadTestPanel;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.demos.empdept.beans.DepartmentModel;
import org.jminor.framework.demos.empdept.beans.EmployeeModel;
import org.jminor.framework.demos.empdept.client.EmpDeptAppModel;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.server.provider.EntityDbRemoteProvider;
import org.jminor.framework.tools.testing.EntityLoadTestModel;

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
      protected void performScenario(final Object applicationModel) throws Exception {
        selectRandomRow(((EntityApplicationModel) applicationModel).getMainApplicationModel(DepartmentModel.class).getTableModel());
      }
      @Override
      protected int getDefaultWeight() {
        return 10;
      }
    };
    final UsageScenario updateEmployee = new UsageScenario("updateEmployee") {
      @Override
      protected void performScenario(final Object applicationModel) throws Exception {
        final EntityModel departmentModel = ((EntityApplicationModel) applicationModel).getMainApplicationModel(DepartmentModel.class);
        selectRandomRow(departmentModel.getTableModel());
        final EntityModel employeeModel = departmentModel.getDetailModel(EmployeeModel.class);
        if (employeeModel.getTableModel().getRowCount() > 0) {
          selectRandomRow(employeeModel.getTableModel());
          if (random.nextDouble() < 0.5)
            employeeModel.getEditModel().setValue(EmpDept.EMPLOYEE_COMMISSION, 100 + random.nextDouble() * 1900);
          else
            employeeModel.getEditModel().setValue(EmpDept.EMPLOYEE_SALARY, 1000 + random.nextDouble() * 9000);

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
      protected void performScenario(final Object applicationModel) throws Exception {
        final EntityModel departmentModel = ((EntityApplicationModel) applicationModel).getMainApplicationModel(DepartmentModel.class);
        selectRandomRow(departmentModel.getTableModel());
        final EntityModel employeeModel = departmentModel.getDetailModel(EmployeeModel.class);
        final Map<String, Entity> references = new HashMap<String, Entity>();
        references.put(EmpDept.T_DEPARTMENT, departmentModel.getTableModel().getSelectedEntity());
        employeeModel.getEditModel().setEntity(EntityUtil.createRandomEntity(EmpDept.T_EMPLOYEE, references));
        employeeModel.getEditModel().insert();
      }
      @Override
      protected int getDefaultWeight() {
        return 3;
      }
    };
    final UsageScenario insertDepartment = new UsageScenario("insertDepartment") {
      @Override
      protected void performScenario(final Object applicationModel) throws Exception {
        final EntityModel departmentModel = ((EntityApplicationModel) applicationModel).getMainApplicationModel(DepartmentModel.class);
        departmentModel.getEditModel().setEntity(EntityUtil.createRandomEntity(EmpDept.T_DEPARTMENT, null));
        departmentModel.getEditModel().insert();
      }
      @Override
      protected int getDefaultWeight() {
        return 1;
      }
    };
    final UsageScenario logoutLogin = new UsageScenario("logoutLogin") {
      @Override
      protected void performScenario(final Object applicationModel) throws Exception {
        ((EntityApplicationModel) applicationModel).getDbProvider().disconnect();
        think();
        ((EntityApplicationModel) applicationModel).getDbProvider().getEntityDb();
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
    final EntityApplicationModel applicationModel =
            new EmpDeptAppModel(new EntityDbRemoteProvider(getUser(), "scott@"+new Object(), getClass().getSimpleName()));

    final EntityModel model = applicationModel.getMainApplicationModels().iterator().next();
    model.setLinkedDetailModel(model.getDetailModels().get(0));
    model.refresh();

    return applicationModel;
  }

  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    new LoadTestPanel(new EmpDeptLoadTest()).showFrame();
  }
}

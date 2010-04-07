/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.profiling;

import org.jminor.common.db.User;
import org.jminor.common.model.CancelException;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.db.provider.EntityDbProviderFactory;
import org.jminor.framework.demos.empdept.beans.DepartmentModel;
import org.jminor.framework.demos.empdept.beans.EmployeeModel;
import org.jminor.framework.demos.empdept.client.EmpDeptAppModel;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.server.provider.EntityDbRemoteProvider;
import org.jminor.framework.tools.profiling.ProfilingModel;
import org.jminor.framework.tools.profiling.ui.ProfilingPanel;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Bjorn Darri
 * Date: 30.11.2007
 * Time: 03:33:10
 */
@SuppressWarnings({"UnusedDeclaration"})
public class EmpDeptProfiling extends ProfilingModel {

  public EmpDeptProfiling() {
    super(new User("scott", "tiger"));
  }

  @Override
  protected Collection<UsageScenario> initializeUsageScenarios() {
    final UsageScenario selectDepartment = new UsageScenario("selectDepartment") {
      @Override
      protected void performScenario(final EntityApplicationModel applicationModel) throws Exception {
        selectRandomRow(applicationModel.getMainApplicationModel(DepartmentModel.class).getTableModel());
      }
      @Override
      protected int getDefaultWeight() {
        return 10;
      }
    };
    final UsageScenario updateEmployee = new UsageScenario("updateEmployee") {
      @Override
      protected void performScenario(final EntityApplicationModel applicationModel) throws Exception {
        final EntityModel departmentModel = applicationModel.getMainApplicationModel(DepartmentModel.class);
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
      protected void performScenario(final EntityApplicationModel applicationModel) throws Exception {
        final EntityModel departmentModel = applicationModel.getMainApplicationModel(DepartmentModel.class);
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
      protected void performScenario(final EntityApplicationModel applicationModel) throws Exception {
        final EntityModel departmentModel = applicationModel.getMainApplicationModel(DepartmentModel.class);
        departmentModel.getEditModel().setEntity(EntityUtil.createRandomEntity(EmpDept.T_DEPARTMENT, null));
        departmentModel.getEditModel().insert();
      }
      @Override
      protected int getDefaultWeight() {
        return 1;
      }
    };

    return Arrays.asList(insertDepartment, insertEmployee, updateEmployee, selectDepartment);
  }

  @Override
  protected void loadDomainModel() {
    new EmpDept();
  }

  @Override
  protected EntityApplicationModel initializeApplicationModel() throws CancelException {
    final EntityApplicationModel applicationModel =
            new EmpDeptAppModel(new EntityDbRemoteProvider(getUser(), "scott@"+new Object(), getClass().getSimpleName()));

    final EntityModel model = applicationModel.getMainApplicationModels().iterator().next();
    model.setLinkedDetailModel(model.getDetailModels().get(0));
    model.refresh();

    return applicationModel;
  }

  private void loadTestRMIServer() {
    try {
      final EntityDbRemoteProvider dbProvider =
              new EntityDbRemoteProvider(new User("scott", "tiger"), "scott@tiger"+System.currentTimeMillis(), getClass().getSimpleName());

      for (int i = 0; i < 100; i++)
        dbProvider.getEntityDb().selectAll(EmpDept.T_EMPLOYEE);

      dbProvider.getEntityDb().disconnect();
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException();
    }
  }

  private void testSelect() {
    try {
      JOptionPane.showMessageDialog(null, "Start");

      final User user = new User("scott", "tiger");
      final EntityDbProvider db = EntityDbProviderFactory.createEntityDbProvider(user, user.toString());
      db.getEntityDb().selectAll(EmpDept.T_DEPARTMENT);
      db.getEntityDb().selectAll(EmpDept.T_EMPLOYEE);
      JOptionPane.showMessageDialog(null, "Exit");
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void testInsert() {
    EntityDbProvider db = null;
    try {
      JOptionPane.showMessageDialog(null, "Start");

      final User user = new User("scott", "tiger");
      db = EntityDbProviderFactory.createEntityDbProvider(user, user.toString());
      db.getEntityDb().beginTransaction();
      db.getEntityDb().insert(Arrays.asList(
              getEmployee(null, "One", "none", null, new Date(), 1234.123, 13, null),
              getEmployee(null, "Two", "none", null, new Date(), 1234.123, 13, null),
              getEmployee(null, "Three", "none", null, new Date(), 1234.123, 13, null),
              getEmployee(null, "Four", "none", null, new Date(), 1234.123, 13, null),
              getEmployee(null, "Five", "none", null, new Date(), 1234.123, 13, null),
              getEmployee(null, "Six", "none", null, new Date(), 1234.123, 13, null),
              getEmployee(null, "Seven", "none", null, new Date(), 1234.123, 13, null),
              getEmployee(null, "Eight", "none", null, new Date(), 1234.123, 13, null),
              getEmployee(null, "Nine", "none", null, new Date(), 1234.123, 13, null),
              getEmployee(null, "Ten", "none", null, new Date(), 1234.123, 13, null)));
      JOptionPane.showMessageDialog(null, "Exit");
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      try {
        if (db != null)
          db.getEntityDb().rollbackTransaction();
      }
      catch (Exception e) {/**/}
    }
  }

  private Entity getEmployee(final Integer id, final String name, final String job, final Entity manager,
                             final Date hiredate, final double salary, final double commission, final Entity department) {
    final Entity employee = new Entity(EmpDept.T_EMPLOYEE);
    employee.setValue(EmpDept.EMPLOYEE_ID, id);
    employee.setValue(EmpDept.EMPLOYEE_NAME, name);
    employee.setValue(EmpDept.EMPLOYEE_JOB, job);
    employee.setValue(EmpDept.EMPLOYEE_MGR_FK, manager);
    employee.setValue(EmpDept.EMPLOYEE_HIREDATE, hiredate);
    employee.setValue(EmpDept.EMPLOYEE_SALARY, salary);
    employee.setValue(EmpDept.EMPLOYEE_COMMISSION, commission);
    employee.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, department);

    return employee;
  }

  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    new ProfilingPanel(new EmpDeptProfiling());
  }
}

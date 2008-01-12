/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.framework.demos.empdept.profiling;

import org.jminor.common.Constants;
import org.jminor.common.db.User;
import org.jminor.common.model.UserException;
import org.jminor.framework.client.dbprovider.EntityDbProviderFactory;
import org.jminor.framework.client.dbprovider.IEntityDbProvider;
import org.jminor.framework.client.dbprovider.RMIEntityDbProvider;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.demos.empdept.client.EmpDeptAppModel;
import org.jminor.framework.demos.empdept.model.EmpDept;
import org.jminor.framework.model.Entity;
import org.jminor.framework.profiling.Profiling;
import org.jminor.framework.profiling.ui.ProfilingPanel;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * User: Björn Darri
 * Date: 30.11.2007
 * Time: 03:33:10
 */
@SuppressWarnings({"UnusedDeclaration"})
public class EmpDeptProfiling extends Profiling {

  /** Constructs a new EmpDeptProfiling.*/
  public EmpDeptProfiling() {
    super(new User("scott", "tiger"));
  }

  /** {@inheritDoc} */
  protected void loadDbModel() {
    new EmpDept();
  }

  /** {@inheritDoc} */
  protected void performWork(final EntityApplicationModel applicationModel) {
    final EntityModel model = applicationModel.getMainApplicationModels().values().iterator().next();
    try {
      model.getTableModel().setSelectedItemIndexes(new int[0]);
      model.forceRefresh();
      selectRandomRow(model);
    }
    catch (UserException e) {
      e.printStackTrace();
    }
  }

  /** {@inheritDoc} */
  protected EntityApplicationModel initializeApplicationModel() throws UserException {
    final EntityApplicationModel applicationModel =
            new EmpDeptAppModel(new RMIEntityDbProvider(getUser(), "scott@"+new Object(), getClass().getSimpleName()));

    final EntityModel model = applicationModel.getMainApplicationModels().values().iterator().next();
    model.setLinkedDetailModel(model.getDetailModels().get(0));

    return applicationModel;
  }

  private void updateEmployeeSalary(EntityModel model) {
    try {
      model.uiSetValue(EmpDept.EMPLOYEE_SALARY, random.nextDouble()*3000);
      model.update();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void updateEmployeeCommission(EntityModel model) {
    try {
      model.uiSetValue(EmpDept.EMPLOYEE_SALARY, random.nextDouble()*1500);
      model.update();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void updateDepartmentName(EntityModel model) {
    try {
      final String objString = new Object().toString();
      model.uiSetValue(EmpDept.DEPARTMENT_NAME, objString.substring(objString.indexOf("@"), objString.length()-1));
      model.update();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void loadTestRMIServer() {
    try {
      final RMIEntityDbProvider dbProvider =
              new RMIEntityDbProvider(new User("scott", "tiger"), "scott@tiger"+System.currentTimeMillis(), getClass().getSimpleName());

      for (int i = 0; i < 100; i++)
        dbProvider.getEntityDb().selectAll(EmpDept.T_EMPLOYEE);

      dbProvider.getEntityDb().logout();
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException();
    }
  }

  private void testLoadEntities() {
    final List<Entity> ret = new ArrayList<Entity>();
    for (int i = 0; i < 10000; i++) {
      ret.add(getEmployee(Constants.INT_NULL_VALUE, "One", "none", null, new Date(), 1234.123, 13, null));
    }
  }

  private void testSelect() {
    try {
      JOptionPane.showMessageDialog(null, "Start");

      final User user = new User("scott", "tiger");
      final IEntityDbProvider db = EntityDbProviderFactory.createEntityDbProvider(user, user.toString());
      db.getEntityDb().selectAll(EmpDept.T_DEPARTMENT);
      db.getEntityDb().selectAll(EmpDept.T_EMPLOYEE);
      JOptionPane.showMessageDialog(null, "Exit");
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void testInsert() {
    IEntityDbProvider db = null;
    try {
      JOptionPane.showMessageDialog(null, "Start");

      final User user = new User("scott", "tiger");
      db = EntityDbProviderFactory.createEntityDbProvider(user, user.toString());
      db.getEntityDb().startTransaction();
      db.getEntityDb().insert(Arrays.asList(
              getEmployee(Constants.INT_NULL_VALUE, "One", "none", null, new Date(), 1234.123, 13, null),
              getEmployee(Constants.INT_NULL_VALUE, "Two", "none", null, new Date(), 1234.123, 13, null),
              getEmployee(Constants.INT_NULL_VALUE, "Three", "none", null, new Date(), 1234.123, 13, null),
              getEmployee(Constants.INT_NULL_VALUE, "Four", "none", null, new Date(), 1234.123, 13, null),
              getEmployee(Constants.INT_NULL_VALUE, "Five", "none", null, new Date(), 1234.123, 13, null),
              getEmployee(Constants.INT_NULL_VALUE, "Six", "none", null, new Date(), 1234.123, 13, null),
              getEmployee(Constants.INT_NULL_VALUE, "Seven", "none", null, new Date(), 1234.123, 13, null),
              getEmployee(Constants.INT_NULL_VALUE, "Eight", "none", null, new Date(), 1234.123, 13, null),
              getEmployee(Constants.INT_NULL_VALUE, "Nine", "none", null, new Date(), 1234.123, 13, null),
              getEmployee(Constants.INT_NULL_VALUE, "Ten", "none", null, new Date(), 1234.123, 13, null)));
      JOptionPane.showMessageDialog(null, "Exit");
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      try {
        if (db != null)
          db.getEntityDb().endTransaction(true);
      }
      catch (Exception e) {/**/}
    }
  }

  private Entity getEmployee(final int id, final String name, final String job, final Entity manager,
                             final Date hiredate, final double salary, final double commission, final Entity department) {
    final Entity ret = new Entity(EmpDept.T_EMPLOYEE);
    ret.setValue(EmpDept.EMPLOYEE_ID, id);
    ret.setValue(EmpDept.EMPLOYEE_NAME, name);
    ret.setValue(EmpDept.EMPLOYEE_JOB, job);
    ret.setValue(EmpDept.EMPLOYEE_MGR_REF, manager);
    ret.setValue(EmpDept.EMPLOYEE_HIREDATE, hiredate);
    ret.setValue(EmpDept.EMPLOYEE_SALARY, salary);
    ret.setValue(EmpDept.EMPLOYEE_COMMISSION, commission);
    ret.setValue(EmpDept.EMPLOYEE_DEPARTMENT_REF, department);

    return ret;
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

/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.Database;
import org.jminor.common.db.User;
import org.jminor.common.db.dbms.H2Database;
import org.jminor.common.model.SearchType;
import org.jminor.framework.db.EntityDb;
import org.jminor.framework.db.criteria.EntityCriteria;
import org.jminor.framework.db.criteria.PropertyCriteria;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.db.provider.EntityDbProviderFactory;
import org.jminor.framework.demos.empdept.beans.DepartmentModel;
import org.jminor.framework.demos.empdept.beans.EmployeeModel;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityKey;
import org.jminor.framework.domain.EntityRepository;

import junit.framework.TestCase;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

public class EntityModelTest extends TestCase {

  private EmployeeModel employeeModel;
  private DepartmentModel departmentModel;

  static {
    new EmpDept();
  }

  public void testDetailModel() throws Exception {
    assertTrue("DepartmentModel should contain EmployeeModel detail", departmentModel.containsDetailModel(EmployeeModel.class));
    assertEquals("Only one detail model should be in DepartmentModel", 1, departmentModel.getDetailModels().size());
    departmentModel.setLinkedDetailModel(departmentModel.getDetailModels().get(0));
    assertTrue("EmployeeModel should be the linked detail model in DepartmentModel",
            departmentModel.getLinkedDetailModel().getClass().equals(EmployeeModel.class));
    final EntityDb db = departmentModel.getDbProvider().getEntityDb();
    final Entity department = db.selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "SALES");
    final List<Entity> employees = db.selectMany(new EntityCriteria(EmpDept.T_EMPLOYEE,
            new PropertyCriteria(EntityRepository.getProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_DEPARTMENT_FK),
                    SearchType.LIKE, department)));
    assertTrue("Number of employees for department should not be 0", employees.size() > 0);
    departmentModel.getTableModel().setQueryFilteredByMaster(true);
    departmentModel.getTableModel().setSelectedEntity(department);
    final List<Entity> employeesFromDetailModel =
            departmentModel.getDetailModel(EmployeeModel.class).getTableModel().getAllEntities();
    assertTrue("Filtered list should contain all employees for department", containsAll(employees, employeesFromDetailModel));
  }

  public void testSelection() throws Exception {
    employeeModel.refresh();
    assertTrue(employeeModel.isActiveEntityNull());
    assertFalse(employeeModel.getActiveEntityModifiedState().isActive());
    employeeModel.getTableModel().setSelectedItemIndex(0);
    assertFalse("Active entity is null after an entity is selected", employeeModel.isActiveEntityNull());
    assertTrue("Active entity is not equal to the selected entity",
            employeeModel.getActiveEntityCopy().propertyValuesEqual(employeeModel.getTableModel().getEntityAtViewIndex(0)));
    assertFalse(employeeModel.getActiveEntityModifiedState().isActive());
    employeeModel.getTableModel().getSelectionModel().clearSelection();
    assertTrue("Active entity is not null after selection is cleared", employeeModel.isActiveEntityNull());
    assertFalse(employeeModel.getActiveEntityModifiedState().isActive());
  }

  public void testEdit() throws Exception {
    //changes to property values in a selected entity should be reverted when it's deselected
    employeeModel.getTableModel().refresh();
    employeeModel.getTableModel().setSelectedItemIndex(0);
    assertFalse(employeeModel.getActiveEntityModifiedState().isActive());
    final Double originalCommission = (Double) employeeModel.getValue(EmpDept.EMPLOYEE_COMMISSION);
    final double commission = 66.7;
    final Timestamp originalHiredate = (Timestamp) employeeModel.getValue(EmpDept.EMPLOYEE_HIREDATE);
    final Timestamp hiredate = new Timestamp(System.currentTimeMillis());
    final String originalName = (String) employeeModel.getValue(EmpDept.EMPLOYEE_NAME);
    final String name = "Mr. Mr";

    employeeModel.setValue(EmpDept.EMPLOYEE_COMMISSION, commission);
    assertTrue(employeeModel.getActiveEntityModifiedState().isActive());
    employeeModel.setValue(EmpDept.EMPLOYEE_HIREDATE, hiredate);
    employeeModel.setValue(EmpDept.EMPLOYEE_NAME, name);

    assertEquals("Commission does not fit", employeeModel.getValue(EmpDept.EMPLOYEE_COMMISSION), commission);
    assertEquals("Hiredate does not fit", employeeModel.getValue(EmpDept.EMPLOYEE_HIREDATE), hiredate);
    assertEquals("Name does not fit", employeeModel.getValue(EmpDept.EMPLOYEE_NAME), name);

    employeeModel.setValue(EmpDept.EMPLOYEE_COMMISSION, originalCommission);
    assertTrue(employeeModel.getActiveEntityModifiedState().isActive());
    employeeModel.setValue(EmpDept.EMPLOYEE_HIREDATE, originalHiredate);
    assertTrue(employeeModel.getActiveEntityModifiedState().isActive());
    employeeModel.setValue(EmpDept.EMPLOYEE_NAME, originalName);
    assertFalse(employeeModel.getActiveEntityModifiedState().isActive());

    employeeModel.getTableModel().getSelectionModel().clearSelection();
    assertTrue("Active entity is not null after selection is cleared", employeeModel.getActiveEntityCopy().isNull());
    employeeModel.getTableModel().setSelectedItemIndex(0);
    assertTrue("Active entity is null after selection is made", !employeeModel.getActiveEntityCopy().isNull());
    employeeModel.clear();
    assertTrue("Active entity is not null after model is cleared", employeeModel.getActiveEntityCopy().isNull());
  }

  public void testStrictEditMode() throws Exception {
    if (!Database.get().supportsNoWait())
      return;

    final EntityDbProvider dbProvider = EntityDbProviderFactory.createEntityDbProvider(
          new User("scott", "tiger"), "EntityModelTest");

    if (Database.get() instanceof H2Database)
      dbProvider.getEntityDb().executeStatement("SET LOCK_TIMEOUT 100");

    departmentModel.getTableModel().setQueryFilteredByMaster(false);
    departmentModel.refresh();
    departmentModel.setUseSelectForUpdate(true);

    //select entity and change a value
    departmentModel.getTableModel().setSelectedItemIndex(0);
    final EntityKey primaryKey = departmentModel.getActiveEntityCopy().getPrimaryKey();
    final Object originalValue = departmentModel.getValue(EmpDept.DEPARTMENT_LOCATION);
    departmentModel.uiSetValue(EmpDept.DEPARTMENT_LOCATION, "None really");
    //assert row is locked
    try {
      dbProvider.getEntityDb().selectForUpdate(Arrays.asList(primaryKey));
      fail("Row should be locked after modification");
    }
    catch (Exception e) {}

    //revert value to original
    departmentModel.uiSetValue(EmpDept.DEPARTMENT_LOCATION, originalValue);
    //assert row is not locked, and then unlock it
    try {
      dbProvider.getEntityDb().selectForUpdate(Arrays.asList(primaryKey));
      dbProvider.getEntityDb().endTransaction(false);
    }
    catch (Exception e) {
      fail("Row should not be locked after value has been reverted");
    }

    //change value
    departmentModel.uiSetValue(EmpDept.DEPARTMENT_LOCATION, "Hello world");
    //assert row is locked
    try {
      dbProvider.getEntityDb().selectForUpdate(Arrays.asList(primaryKey));
      fail("Row should be locked after modification");
    }
    catch (Exception e) {}

    //do update
    departmentModel.update();
    //assert row is not locked
    try {
      dbProvider.getEntityDb().selectForUpdate(Arrays.asList(primaryKey));
      dbProvider.getEntityDb().endTransaction(false);
    }
    catch (Exception e) {
      fail("Row should not be locked after update");
    }

    departmentModel.uiSetValue(EmpDept.DEPARTMENT_LOCATION, "None really");
    //assert row is locked
    try {
      dbProvider.getEntityDb().selectForUpdate(Arrays.asList(primaryKey));
      fail("Row should be locked after modification");
    }
    catch (Exception e) {}

    departmentModel.getTableModel().setSelectedItemIndex(1);

    try {
      dbProvider.getEntityDb().selectForUpdate(Arrays.asList(primaryKey));
      dbProvider.getEntityDb().endTransaction(false);
    }
    catch (Exception e) {
      fail("Row should not be locked after another has been selected");
    }

    //clean up by resetting the value
    departmentModel.getTableModel().setSelectedItemIndex(0);
    departmentModel.uiSetValue(EmpDept.DEPARTMENT_LOCATION, originalValue);
    departmentModel.update();
  }

  @Override
  protected void setUp() throws Exception {
    final EntityDbProvider dbProvider = EntityDbProviderFactory.createEntityDbProvider(
          new User("scott", "tiger"), "EntityModelTest");
    departmentModel = new DepartmentModel(dbProvider);
    employeeModel = (EmployeeModel) departmentModel.getDetailModel(EmployeeModel.class);
  }

  @Override
  protected void tearDown() throws Exception {
    departmentModel.getDbProvider().logout();
  }

  private boolean containsAll(List<Entity> employees, List<Entity> employeesFromModel) {
    for (final Entity entity : employeesFromModel)
      if (!employees.contains(entity))
        return false;

    return true;
  }
}
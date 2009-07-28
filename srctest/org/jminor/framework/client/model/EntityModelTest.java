/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.User;
import org.jminor.common.model.SearchType;
import org.jminor.framework.db.EntityDbLocalProvider;
import org.jminor.framework.db.IEntityDb;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.db.criteria.EntityCriteria;
import org.jminor.framework.db.criteria.PropertyCriteria;
import org.jminor.framework.demos.empdept.beans.DepartmentModel;
import org.jminor.framework.demos.empdept.beans.EmployeeModel;
import org.jminor.framework.demos.empdept.model.EmpDept;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityRepository;

import junit.framework.TestCase;

import java.sql.Timestamp;
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
    final IEntityDb db = departmentModel.getDbProvider().getEntityDb();
    final Entity department = db.selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "SALES");
    final List<Entity> employees = db.selectMany(new EntityCriteria(EmpDept.T_EMPLOYEE,
            new PropertyCriteria(EntityRepository.get().getProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_DEPARTMENT_REF),
                    SearchType.LIKE, department)));
    assertTrue("Number of employees for department should not be 0", employees.size() > 0);
    departmentModel.getTableModel().setFilterQueryByMaster(true);
    departmentModel.getTableModel().setSelectedEntity(department);
    final List<Entity> employeesFromDetailModel =
            departmentModel.getDetailModel(EmployeeModel.class).getTableModel().getAllEntities();
    assertTrue("Filtered list should contain all employees for department", containsAll(employees, employeesFromDetailModel));
  }

  private boolean containsAll(List<Entity> employees, List<Entity> employeesFromModel) {
    for (final Entity entity : employeesFromModel)
      if (!employees.contains(entity))
        return false;

    return true;
  }

  public void testSelection() throws Exception {
    employeeModel.refresh();
    assertTrue(employeeModel.isActiveEntityNull());
    assertFalse(employeeModel.getActiveEntityModifiedState().isActive());
    employeeModel.getTableModel().setSelectedItemIdx(0);
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
    employeeModel.getTableModel().setSelectedItemIdx(0);
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
    employeeModel.getTableModel().setSelectedItemIdx(0);
    assertTrue("Active entity is null after selection is made", !employeeModel.getActiveEntityCopy().isNull());
    employeeModel.clear();
    assertTrue("Active entity is not null after model is cleared", employeeModel.getActiveEntityCopy().isNull());
  }

  @Override
  protected void setUp() throws Exception {
    final IEntityDbProvider dbProvider = new EntityDbLocalProvider(new User("scott", "tiger"));
    employeeModel = new EmployeeModel(dbProvider);
    departmentModel = new DepartmentModel(dbProvider);
  }

  @Override
  protected void tearDown() throws Exception {
    employeeModel.getDbProvider().logout();
  }
}
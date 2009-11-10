/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.SearchType;
import org.jminor.framework.db.EntityDb;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.db.criteria.CriteriaUtil;
import org.jminor.framework.demos.empdept.beans.DepartmentModel;
import org.jminor.framework.demos.empdept.beans.EmployeeModel;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;

import junit.framework.TestCase;

import java.util.List;

public class EntityModelTest extends TestCase {
  private DepartmentModel departmentModel;

  public void testDetailModel() throws Exception {
    assertTrue("DepartmentModel should contain EmployeeModel detail", departmentModel.containsDetailModel(EmployeeModel.class));
    assertEquals("Only one detail model should be in DepartmentModel", 1, departmentModel.getDetailModels().size());
    departmentModel.setLinkedDetailModel(departmentModel.getDetailModels().get(0));
    assertTrue("EmployeeModel should be the linked detail model in DepartmentModel",
            departmentModel.getLinkedDetailModel().getClass().equals(EmployeeModel.class));
    final EntityDb db = departmentModel.getDbProvider().getEntityDb();
    final Entity department = db.selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "SALES");
    final List<Entity> employees = db.selectMany(CriteriaUtil.selectCriteria(EmpDept.T_EMPLOYEE,
            EmpDept.EMPLOYEE_DEPARTMENT_FK, SearchType.LIKE, department));
    assertTrue("Number of employees for department should not be 0", employees.size() > 0);
    departmentModel.getTableModel().setQueryFilteredByMaster(true);
    departmentModel.getTableModel().setSelectedEntity(department);
    final List<Entity> employeesFromDetailModel =
            departmentModel.getDetailModel(EmpDept.T_EMPLOYEE).getTableModel().getAllEntities();
    assertTrue("Filtered list should contain all employees for department", containsAll(employees, employeesFromDetailModel));
  }

  @Override
  protected void setUp() throws Exception {
    departmentModel = new DepartmentModel(EntityDbConnectionTest.dbProvider);
  }

  private boolean containsAll(List<Entity> employees, List<Entity> employeesFromModel) {
    for (final Entity entity : employeesFromModel)
      if (!employees.contains(entity))
        return false;

    return true;
  }
}
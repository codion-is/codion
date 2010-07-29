/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.SearchType;
import org.jminor.framework.db.EntityDb;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public final class DefaultEntityModelTest {

  private DefaultEntityModel departmentModel;
  private int eventCount = 0;

  private static class EmpModel extends DefaultEntityModel {
    private EmpModel(final EntityDbProvider dbProvider) {
      super(new DefaultEntityEditModel(EmpDept.T_EMPLOYEE, dbProvider),
              new DefaultEntityTableModel(EmpDept.T_EMPLOYEE, dbProvider));
    }
  }

  @Test
  public void testDetailModels() {
    departmentModel.addDetailModels(new EmpModel(departmentModel.getDbProvider()));
    assertTrue(departmentModel.containsDetailModel(EmpModel.class));
    final EntityModel empModel = departmentModel.getDetailModel(EmpModel.class);
    assertNotNull(empModel);
    departmentModel.setLinkedDetailModels(empModel);
    assertTrue(departmentModel.getLinkedDetailModels().contains(empModel));
  }

  @Test
  public void testConstructor() {
    try {
      new DefaultEntityModel(null, EntityDbConnectionTest.DB_PROVIDER);
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      new DefaultEntityModel(EmpDept.T_EMPLOYEE, null);
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      new DefaultEntityModel((DefaultEntityEditModel) null, null);
      fail();
    }
    catch (IllegalArgumentException e) {}
    new DefaultEntityModel(new DefaultEntityEditModel(EmpDept.T_DEPARTMENT, EntityDbConnectionTest.DB_PROVIDER));
    new DefaultEntityModel(new DefaultEntityTableModel(EmpDept.T_DEPARTMENT, EntityDbConnectionTest.DB_PROVIDER));
    new DefaultEntityModel(new DefaultEntityEditModel(EmpDept.T_DEPARTMENT, EntityDbConnectionTest.DB_PROVIDER), true);
  }

  @Test
  public void test() throws Exception {
    departmentModel.setCascadeRefresh(true);
    assertTrue(departmentModel.isCascadeRefresh());
    assertNull(departmentModel.getMasterModel());
    assertNotNull(departmentModel.getEditModel());
    assertNotNull(departmentModel.getTableModel());
    assertTrue(departmentModel.containsTableModel());
    assertNotNull(departmentModel.eventLinkedDetailModelsChanged());

    departmentModel.eventRefreshStarted().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        eventCount++;
      }
    });
    departmentModel.eventRefreshDone().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        eventCount++;
      }
    });
    departmentModel.refresh();
    assertTrue(departmentModel.getDetailModel(EmpDept.T_EMPLOYEE).getTableModel().getRowCount() > 0);
    assertEquals(2, eventCount);

    try {
      departmentModel.getDbProvider().getEntityDb().beginTransaction();
      final Entity department = departmentModel.getDbProvider().getEntityDb().selectSingle(
              EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "OPERATIONS");
      departmentModel.getTableModel().setSelectedItem(department);
      final EntityModel employeeModel = departmentModel.getDetailModel(EmpDept.T_EMPLOYEE);
      final EntityComboBoxModel comboBoxModel = employeeModel.getEditModel().initializeEntityComboBoxModel(EmpDept.EMPLOYEE_DEPARTMENT_FK);
      comboBoxModel.refresh();
      comboBoxModel.setSelectedItem(department);
      departmentModel.getTableModel().deleteSelected();
      assertEquals(3, employeeModel.getEditModel().getEntityComboBoxModel(EmpDept.EMPLOYEE_DEPARTMENT_FK).getSize());
      assertNotNull(employeeModel.getEditModel().getEntityComboBoxModel(EmpDept.EMPLOYEE_DEPARTMENT_FK).getSelectedEntity());
    }
    finally {
      departmentModel.getDbProvider().getEntityDb().rollbackTransaction();
    }
  }

  @Test
  public void detailModel() throws Exception {
    departmentModel.getDetailModel(EmpDept.T_EMPLOYEE);
    assertTrue("DepartmentModel should contain Employee detail", departmentModel.containsDetailModel(EmpDept.T_EMPLOYEE));
    assertEquals("Only one detail model should be in DepartmentModel", 1, departmentModel.getDetailModels().size());
    departmentModel.setLinkedDetailModels(departmentModel.getDetailModels().iterator().next());
    assertTrue(departmentModel.getLinkedDetailModels().size() == 1);
    assertTrue("Employee model should be the linked detail model in DepartmentModel",
            departmentModel.getLinkedDetailModels().contains(departmentModel.getDetailModel(EmpDept.T_EMPLOYEE)));
    assertNotNull(departmentModel.getDetailModel(EmpDept.T_EMPLOYEE));
    departmentModel.refresh();
    departmentModel.refreshDetailModels();
    assertTrue(departmentModel.getDetailModel(EmpDept.T_EMPLOYEE).getTableModel().getRowCount() > 0);

    final EntityDb db = departmentModel.getDbProvider().getEntityDb();
    final Entity department = db.selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "SALES");
    final List<Entity> salesEmployees = db.selectMany(EntityCriteriaUtil.selectCriteria(EmpDept.T_EMPLOYEE,
            EmpDept.EMPLOYEE_DEPARTMENT_FK, SearchType.LIKE, department));
    assertTrue("Number of employees for department should not be 0", salesEmployees.size() > 0);
    departmentModel.getDetailModel(EmpDept.T_EMPLOYEE).getTableModel().setDetailModel(true);
    departmentModel.getTableModel().setSelectedItem(department);
    final List<Entity> employeesFromDetailModel =
            departmentModel.getDetailModel(EmpDept.T_EMPLOYEE).getTableModel().getAllItems();
    assertTrue("Filtered list should contain all employees for department", containsAll(salesEmployees, employeesFromDetailModel));
  }

  @Before
  public void setUp() throws Exception {
    departmentModel = new DefaultEntityModel(EmpDept.T_DEPARTMENT, EntityDbConnectionTest.DB_PROVIDER);
    departmentModel.getDetailModel(EmpDept.T_EMPLOYEE).getTableModel().setQueryCriteriaRequired(false);
  }

  private boolean containsAll(List<Entity> employees, List<Entity> employeesFromModel) {
    for (final Entity entity : employeesFromModel) {
      if (!employees.contains(entity)) {
        return false;
      }
    }

    return true;
  }
}
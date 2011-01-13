/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnectionImplTest;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.db.provider.EntityConnectionProvider;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;

import org.junit.Before;
import org.junit.Test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public final class DefaultEntityModelTest {

  private DefaultEntityModel departmentModel;
  private int eventCount = 0;

  public static class EmpModel extends DefaultEntityModel {
    public EmpModel(final EntityConnectionProvider connectionProvider) {
      super(new DefaultEntityEditModel(EmpDept.T_EMPLOYEE, connectionProvider),
              new DefaultEntityTableModel(EmpDept.T_EMPLOYEE, connectionProvider));
    }
  }

  @Test
  public void testDetailModels() throws CancelException, DatabaseException, ValidationException {
    assertTrue(departmentModel.containsDetailModel(EmpModel.class));
    final EntityModel employeeModel = departmentModel.getDetailModel(EmpModel.class);
    assertNotNull(employeeModel);
    departmentModel.setLinkedDetailModels(employeeModel);
    assertTrue(departmentModel.getLinkedDetailModels().contains(employeeModel));
    departmentModel.refresh();
    final EntityEditModel employeeEditModel = employeeModel.getEditModel();
    final EntityComboBoxModel departmentsComboBoxModel = employeeEditModel.initializeEntityComboBoxModel(Entities.getForeignKeyProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_DEPARTMENT_FK));
    departmentsComboBoxModel.refresh();
    final Entity.Key primaryKey = Entities.key(EmpDept.T_DEPARTMENT);
    primaryKey.setValue(EmpDept.DEPARTMENT_ID, 40);//operations, no employees
    final List<Entity.Key> keys = new ArrayList<Entity.Key>();
    keys.add(primaryKey);
    departmentModel.getTableModel().setSelectedByPrimaryKeys(keys);
    final Entity operations = departmentModel.getTableModel().getSelectedItem();
    try {
      departmentModel.getConnectionProvider().getConnection().beginTransaction();
      departmentModel.getEditModel().delete();
      assertFalse(departmentsComboBoxModel.contains(operations, true));
      departmentModel.getEditModel().setValue(EmpDept.DEPARTMENT_ID, 99);
      departmentModel.getEditModel().setValue(EmpDept.DEPARTMENT_NAME, "nameit");
      final Entity.Key insertedKey = departmentModel.getEditModel().insert().get(0);
      final Entity inserted = departmentModel.getConnectionProvider().getConnection().selectSingle(insertedKey);
      assertTrue(departmentsComboBoxModel.contains(inserted, true));
      departmentModel.getTableModel().setSelectedByPrimaryKeys(Arrays.asList(insertedKey));
      departmentModel.getEditModel().setValue(EmpDept.DEPARTMENT_NAME, "nameitagain");
      departmentModel.getEditModel().update();
      assertEquals("nameitagain", departmentsComboBoxModel.getEntity(insertedKey).getValue(EmpDept.DEPARTMENT_NAME));

      primaryKey.setValue(EmpDept.DEPARTMENT_ID, 20);//research
      departmentModel.getTableModel().setSelectedByPrimaryKeys(keys);
      departmentModel.getEditModel().setValue(EmpDept.DEPARTMENT_NAME, "NewName");
      departmentModel.getEditModel().update();

      for (final Entity employee : employeeModel.getTableModel().getAllItems()) {
        final Entity dept = employee.getForeignKeyValue(EmpDept.EMPLOYEE_DEPARTMENT_FK);
        assertEquals("NewName", dept.getValue(EmpDept.DEPARTMENT_NAME));
      }
    }
    finally {
      departmentModel.getConnectionProvider().getConnection().rollbackTransaction();
    }
  }

  @Test
  public void clear() {
    departmentModel.refresh();
    assertTrue(departmentModel.getTableModel().getRowCount() > 0);

    final EntityModel employeeModel = departmentModel.getDetailModel(EmpModel.class);
    employeeModel.refresh();
    assertTrue(employeeModel.getTableModel().getRowCount() > 0);

    departmentModel.clearDetailModels();
    assertTrue(employeeModel.getTableModel().getRowCount() == 0);

    departmentModel.clear();
    assertTrue(departmentModel.getTableModel().getRowCount() == 0);
  }

  @Test
  public void constructor() {
    try {
      new DefaultEntityModel(null, EntityConnectionImplTest.DB_PROVIDER);
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
    new DefaultEntityModel(new DefaultEntityEditModel(EmpDept.T_DEPARTMENT, EntityConnectionImplTest.DB_PROVIDER));
    new DefaultEntityModel(new DefaultEntityTableModel(EmpDept.T_DEPARTMENT, EntityConnectionImplTest.DB_PROVIDER));
    new DefaultEntityModel(new DefaultEntityEditModel(EmpDept.T_DEPARTMENT, EntityConnectionImplTest.DB_PROVIDER), true);
  }

  @Test
  public void test() throws Exception {
    assertNull(departmentModel.getMasterModel());
    assertNotNull(departmentModel.getEditModel());
    assertNotNull(departmentModel.getTableModel());
    assertTrue(departmentModel.containsTableModel());

    final ActionListener linkedListener = new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
      }
    };
    final ActionListener listener = new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        eventCount++;
      }
    };
    departmentModel.addLinkedDetailModelsListener(linkedListener);
    departmentModel.addBeforeRefreshListener(listener);
    departmentModel.addAfterRefreshListener(listener);
    departmentModel.refresh();
    assertEquals(2, eventCount);

    try {
      departmentModel.getConnectionProvider().getConnection().beginTransaction();
      final Entity department = departmentModel.getConnectionProvider().getConnection().selectSingle(
              EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "OPERATIONS");
      departmentModel.getTableModel().setSelectedItem(department);
      final EntityModel employeeModel = departmentModel.getDetailModel(EmpDept.T_EMPLOYEE);
      final EntityComboBoxModel deptComboBoxModel = employeeModel.getEditModel().initializeEntityComboBoxModel(EmpDept.EMPLOYEE_DEPARTMENT_FK);
      deptComboBoxModel.refresh();
      deptComboBoxModel.setSelectedItem(department);
      departmentModel.getTableModel().deleteSelected();
      assertEquals(3, employeeModel.getEditModel().getEntityComboBoxModel(EmpDept.EMPLOYEE_DEPARTMENT_FK).getSize());
      assertNotNull(employeeModel.getEditModel().getEntityComboBoxModel(EmpDept.EMPLOYEE_DEPARTMENT_FK).getSelectedValue());
    }
    finally {
      departmentModel.getConnectionProvider().getConnection().rollbackTransaction();
    }
    departmentModel.removeBeforeRefreshListener(listener);
    departmentModel.removeAfterRefreshListener(listener);
    departmentModel.removeLinkedDetailModelsListener(linkedListener);
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

    final EntityConnection db = departmentModel.getConnectionProvider().getConnection();
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
    departmentModel = new DefaultEntityModel(EmpDept.T_DEPARTMENT, EntityConnectionImplTest.DB_PROVIDER);
    final EntityModel employeeModel = new EmpModel(departmentModel.getConnectionProvider());
    employeeModel.getTableModel().setQueryCriteriaRequired(false);
    departmentModel.addDetailModel(employeeModel);
  }

  private static boolean containsAll(final List<Entity> employees, final List<Entity> employeesFromModel) {
    for (final Entity entity : employeesFromModel) {
      if (!employees.contains(entity)) {
        return false;
      }
    }

    return true;
  }
}
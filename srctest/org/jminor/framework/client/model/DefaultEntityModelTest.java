/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.EventAdapter;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.valuemap.ValueMapValue;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.control.ValueLinks;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnectionImplTest;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.db.provider.EntityConnectionProvider;
import org.jminor.framework.demos.empdept.beans.EmployeeEditModel;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;

import org.junit.Before;
import org.junit.Test;

import javax.swing.JComboBox;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public final class DefaultEntityModelTest {

  private DefaultEntityModel departmentModel;
  private int eventCount = 0;

  public static class EmpModel extends DefaultEntityModel {
    public EmpModel(final EntityConnectionProvider connectionProvider) {
      super(new EmployeeEditModel(connectionProvider));
      getEditModel().initializeEntityComboBoxModel(EmpDept.EMPLOYEE_DEPARTMENT_FK).refresh();
      getEditModel().initializeEntityComboBoxModel(EmpDept.EMPLOYEE_MGR_FK).refresh();
    }
  }

  @Test
  public void isModified() {
    //here we're basically testing for the entity in the edit model being modified after
    //being set when selected in the table model, this usually happens when combo box models
    //are being filtered on property value change, see EmployeeEditModel.bindEvents()
    final EntityModel employeeModel = departmentModel.getDetailModel(EmpDept.T_EMPLOYEE);
    final EntityEditModel employeeEditModel = employeeModel.getEditModel();
    final EntityTableModel employeeTableModel = employeeModel.getTableModel();
    ValueLinks.selectedItemValueLink(new JComboBox(employeeEditModel.getEntityComboBoxModel(EmpDept.EMPLOYEE_MGR_FK)),
            new ValueMapValue<String, Object>(employeeEditModel, EmpDept.EMPLOYEE_MGR_FK), LinkType.READ_WRITE);
    employeeTableModel.refresh();
    for (final Entity employee : employeeTableModel.getAllItems()) {
      employeeTableModel.setSelectedItem(employee);
      assertFalse(employeeEditModel.isModified());
    }
  }

  @Test
  public void testDetailModels() throws CancelException, DatabaseException, ValidationException {
    assertTrue(departmentModel.containsDetailModel(EmpModel.class));
    assertNull(departmentModel.getDetailModel(DefaultEntityModel.class));
    assertFalse(departmentModel.containsDetailModel("undefined"));
    assertFalse(departmentModel.containsDetailModel(DefaultEntityModel.class));
    final EntityModel employeeModel = departmentModel.getDetailModel(EmpModel.class);
    assertNotNull(employeeModel);
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
      final Entity inserted = departmentModel.getEditModel().insert().get(0);
      assertTrue(departmentsComboBoxModel.contains(inserted, true));
      departmentModel.getTableModel().setSelectedItem(inserted);
      departmentModel.getEditModel().setValue(EmpDept.DEPARTMENT_NAME, "nameitagain");
      departmentModel.getEditModel().update();
      assertEquals("nameitagain", departmentsComboBoxModel.getEntity(inserted.getPrimaryKey()).getValue(EmpDept.DEPARTMENT_NAME));

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

  @Test(expected = IllegalArgumentException.class)
  public void getDetailModelNotFound() {
    departmentModel.getDetailModel("undefined");
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
    new DefaultEntityModel(new DefaultEntityEditModel(EmpDept.T_DEPARTMENT, EntityConnectionImplTest.CONNECTION_PROVIDER));
    new DefaultEntityModel(new DefaultEntityTableModel(EmpDept.T_DEPARTMENT, EntityConnectionImplTest.CONNECTION_PROVIDER));
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullEntityID() {
    new DefaultEntityModel(null, EntityConnectionImplTest.CONNECTION_PROVIDER);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullConnectionProvider() {
    new DefaultEntityModel(EmpDept.T_EMPLOYEE, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullEditModelNullTableModel() {
    new DefaultEntityModel((DefaultEntityEditModel) null, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorTableModelEntityIDMismatch() {
    final EntityEditModel editModel = new DefaultEntityEditModel(EmpDept.T_DEPARTMENT, EntityConnectionImplTest.CONNECTION_PROVIDER);
    final EntityTableModel tableModel = new DefaultEntityTableModel(EmpDept.T_EMPLOYEE, EntityConnectionImplTest.CONNECTION_PROVIDER);
    new DefaultEntityModel(editModel, tableModel);
  }

  @Test
  public void test() throws Exception {
    assertNull(departmentModel.getMasterModel());
    assertNotNull(departmentModel.getEditModel());
    assertNotNull(departmentModel.getTableModel());
    assertTrue(departmentModel.containsTableModel());

    final EventListener linkedListener = new EventAdapter() {
      @Override
      public void eventOccurred() {}
    };
    final EventListener listener = new EventAdapter() {
      @Override
      public void eventOccurred() {
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
    assertTrue(departmentModel.getLinkedDetailModels().size() == 1);
    assertTrue("Employee model should be the linked detail model in DepartmentModel",
            departmentModel.getLinkedDetailModels().contains(departmentModel.getDetailModel(EmpDept.T_EMPLOYEE)));
    assertNotNull(departmentModel.getDetailModel(EmpDept.T_EMPLOYEE));
    departmentModel.refresh();
    departmentModel.refreshDetailModels();
    assertTrue(departmentModel.getDetailModel(EmpDept.T_EMPLOYEE).getTableModel().getRowCount() > 0);

    final EntityConnection connection = departmentModel.getConnectionProvider().getConnection();
    final Entity department = connection.selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "SALES");
    final List<Entity> salesEmployees = connection.selectMany(EntityCriteriaUtil.selectCriteria(EmpDept.T_EMPLOYEE,
            EmpDept.EMPLOYEE_DEPARTMENT_FK, SearchType.LIKE, department));
    assertTrue("Number of employees for department should not be 0", salesEmployees.size() > 0);
    departmentModel.getTableModel().setSelectedItem(department);
    final List<Entity> employeesFromDetailModel =
            departmentModel.getDetailModel(EmpDept.T_EMPLOYEE).getTableModel().getAllItems();
    assertTrue("Filtered list should contain all employees for department", containsAll(salesEmployees, employeesFromDetailModel));
  }

  @Test(expected = IllegalArgumentException.class)
  public void addSameDetailModelTwice() {
    departmentModel = new DefaultEntityModel(EmpDept.T_DEPARTMENT, EntityConnectionImplTest.CONNECTION_PROVIDER);
    final EntityModel employeeModel = new EmpModel(departmentModel.getConnectionProvider());
    departmentModel.addDetailModels(employeeModel, employeeModel);
  }

  @Test(expected = IllegalArgumentException.class)
  public void addDetailModelDetailModelAlreadyHasMasterModel() {
    departmentModel = new DefaultEntityModel(EmpDept.T_DEPARTMENT, EntityConnectionImplTest.CONNECTION_PROVIDER);
    final EntityModel employeeModel = new EmpModel(departmentModel.getConnectionProvider());
    employeeModel.setMasterModel(departmentModel);
    departmentModel.addDetailModel(employeeModel);
  }

  @Test(expected = IllegalStateException.class)
  public void setMasterModel() {
    departmentModel = new DefaultEntityModel(EmpDept.T_DEPARTMENT, EntityConnectionImplTest.CONNECTION_PROVIDER);
    final EntityModel employeeModel = new EmpModel(departmentModel.getConnectionProvider());
    employeeModel.setMasterModel(departmentModel);
    employeeModel.setMasterModel(new DefaultEntityModel(EmpDept.T_DEPARTMENT, EntityConnectionImplTest.CONNECTION_PROVIDER));
  }

  @Test
  public void addRemoveLinkedDetailModel() {
    departmentModel.removeLinkedDetailModel(departmentModel.getDetailModel(EmpDept.T_EMPLOYEE));
    assertTrue(departmentModel.getLinkedDetailModels().isEmpty());
    departmentModel.addLinkedDetailModel(departmentModel.getDetailModel(EmpDept.T_EMPLOYEE));
    assertFalse(departmentModel.getLinkedDetailModels().isEmpty());
    assertTrue(departmentModel.getLinkedDetailModels().contains(departmentModel.getDetailModel(EmpDept.T_EMPLOYEE)));
  }

  @Before
  public void setUp() throws Exception {
    departmentModel = new DefaultEntityModel(EmpDept.T_DEPARTMENT, EntityConnectionImplTest.CONNECTION_PROVIDER);
    final EntityModel employeeModel = new EmpModel(departmentModel.getConnectionProvider());
    departmentModel.addDetailModel(employeeModel);
    departmentModel.setDetailModelForeignKey(employeeModel, EmpDept.EMPLOYEE_DEPARTMENT_FK);
    departmentModel.addLinkedDetailModel(employeeModel);
    employeeModel.getTableModel().setQueryCriteriaRequired(false);
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
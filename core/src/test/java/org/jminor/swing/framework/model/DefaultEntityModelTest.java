/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.FilterCriteria;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.EditModelValues;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.EntityConnectionProvidersTest;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.TestDomain;
import org.jminor.swing.common.ui.ValueLinks;

import org.junit.Before;
import org.junit.Test;

import javax.swing.JComboBox;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public final class DefaultEntityModelTest {

  private DefaultEntityModel departmentModel;
  private int eventCount = 0;

  public static class EmpModel extends DefaultEntityModel {
    public EmpModel(final EntityConnectionProvider connectionProvider) {
      super(new DefaultEntityEditModel(TestDomain.T_EMP, connectionProvider));
      getEditModel().getForeignKeyComboBoxModel(TestDomain.EMP_DEPARTMENT_FK).refresh();
      getEditModel().getForeignKeyComboBoxModel(TestDomain.EMP_MGR_FK).refresh();
    }
  }

  @Test
  public void isModified() {
    //here we're basically testing for the entity in the edit model being modified after
    //being set when selected in the table model, this usually happens when combo box models
    //are being filtered on property value change, see EmployeeEditModel.bindEvents()
    final EntityModel employeeModel = departmentModel.getDetailModel(TestDomain.T_EMP);
    final EntityEditModel employeeEditModel = employeeModel.getEditModel();
    final EntityTableModel employeeTableModel = employeeModel.getTableModel();
    ValueLinks.selectedItemValueLink(new JComboBox<>(employeeEditModel.getForeignKeyComboBoxModel(TestDomain.EMP_MGR_FK)),
            EditModelValues.<Entity>value(employeeEditModel, TestDomain.EMP_MGR_FK));
    employeeTableModel.refresh();
    for (final Entity employee : employeeTableModel.getAllItems()) {
      employeeTableModel.getSelectionModel().setSelectedItem(employee);
      assertFalse(employeeEditModel.isModified());
    }
  }

  @Test
  public void testUpdatePrimaryKey() throws DatabaseException, ValidationException {
    departmentModel.refresh();
    final EntityEditModel deptEditModel = departmentModel.getEditModel();
    final EntityTableModel deptTableModel = departmentModel.getTableModel();
    final Entity.Key operationsKey = Entities.key(TestDomain.T_DEPARTMENT);
    operationsKey.setValue(TestDomain.DEPARTMENT_ID, 40);//operations
    deptTableModel.setSelectedByPrimaryKeys(Collections.singletonList(operationsKey));

    deptEditModel.setValue(TestDomain.DEPARTMENT_ID, 80);
    deptEditModel.update();

    assertFalse(deptTableModel.getSelectionModel().isSelectionEmpty());
    Entity operations = deptTableModel.getSelectionModel().getSelectedItem();
    assertEquals(80, operations.getValue(TestDomain.DEPARTMENT_ID));

    deptTableModel.setFilterCriteria(new FilterCriteria<Entity>() {
      @Override
      public boolean include(final Entity item) {
        return !Util.equal(80, item.getValue(TestDomain.DEPARTMENT_ID));
      }
    });

    deptEditModel.setEntity(operations);
    deptEditModel.setValue(TestDomain.DEPARTMENT_ID, 40);
    deptEditModel.update();

    operations = deptTableModel.getFilteredItems().get(0);
    assertEquals(40, operations.getValue(TestDomain.DEPARTMENT_ID));
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
    final EntityComboBoxModel departmentsComboBoxModel = employeeEditModel.getForeignKeyComboBoxModel(Entities.getForeignKeyProperty(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT_FK));
    departmentsComboBoxModel.refresh();
    final Entity.Key primaryKey = Entities.key(TestDomain.T_DEPARTMENT);
    primaryKey.setValue(TestDomain.DEPARTMENT_ID, 40);//operations, no employees
    final List<Entity.Key> keys = new ArrayList<>();
    keys.add(primaryKey);
    departmentModel.getTableModel().setSelectedByPrimaryKeys(keys);
    final Entity operations = departmentModel.getTableModel().getSelectionModel().getSelectedItem();
    try {
      departmentModel.getConnectionProvider().getConnection().beginTransaction();
      departmentModel.getEditModel().delete();
      assertFalse(departmentsComboBoxModel.contains(operations, true));
      departmentModel.getEditModel().setValue(TestDomain.DEPARTMENT_ID, 99);
      departmentModel.getEditModel().setValue(TestDomain.DEPARTMENT_NAME, "nameit");
      final Entity inserted = departmentModel.getEditModel().insert().get(0);
      assertTrue(departmentsComboBoxModel.contains(inserted, true));
      departmentModel.getTableModel().getSelectionModel().setSelectedItem(inserted);
      departmentModel.getEditModel().setValue(TestDomain.DEPARTMENT_NAME, "nameitagain");
      departmentModel.getEditModel().update();
      assertEquals("nameitagain", departmentsComboBoxModel.getEntity(inserted.getPrimaryKey()).getValue(TestDomain.DEPARTMENT_NAME));

      primaryKey.setValue(TestDomain.DEPARTMENT_ID, 20);//research
      departmentModel.getTableModel().setSelectedByPrimaryKeys(keys);
      departmentModel.getEditModel().setValue(TestDomain.DEPARTMENT_NAME, "NewName");
      departmentModel.getEditModel().update();

      for (final Entity employee : employeeModel.getTableModel().getAllItems()) {
        final Entity dept = employee.getForeignKeyValue(TestDomain.EMP_DEPARTMENT_FK);
        assertEquals("NewName", dept.getValue(TestDomain.DEPARTMENT_NAME));
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
    new DefaultEntityModel(new DefaultEntityEditModel(TestDomain.T_DEPARTMENT, EntityConnectionProvidersTest.CONNECTION_PROVIDER));
    new DefaultEntityModel(new DefaultEntityTableModel(TestDomain.T_DEPARTMENT, EntityConnectionProvidersTest.CONNECTION_PROVIDER));
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullEntityID() {
    new DefaultEntityModel(null, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullConnectionProvider() {
    new DefaultEntityModel(TestDomain.T_EMP, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullEditModelNullTableModel() {
    new DefaultEntityModel((DefaultEntityEditModel) null, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorTableModelEntityIDMismatch() {
    final EntityEditModel editModel = new DefaultEntityEditModel(TestDomain.T_DEPARTMENT, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    final EntityTableModel tableModel = new DefaultEntityTableModel(TestDomain.T_EMP, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    new DefaultEntityModel(editModel, tableModel);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorTableModelEditModelMismatch() {
    final EntityEditModel editModel = new DefaultEntityEditModel(TestDomain.T_DEPARTMENT, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    final EntityEditModel editModel2 = new DefaultEntityEditModel(TestDomain.T_DEPARTMENT, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    final EntityTableModel tableModel = new DefaultEntityTableModel(TestDomain.T_DEPARTMENT, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    tableModel.setEditModel(editModel);
    new DefaultEntityModel(editModel2, tableModel);
  }

  @Test
  public void clearEditModelClearTableSelection() {
    departmentModel.refresh();
    departmentModel.getTableModel().getSelectionModel().setSelectedIndexes(Arrays.asList(1,2,3));
    assertFalse(departmentModel.getTableModel().getSelectionModel().isSelectionEmpty());
    assertFalse(departmentModel.getEditModel().isEntityNew());
    departmentModel.getEditModel().setEntity(null);
    assertTrue(departmentModel.getTableModel().getSelectionModel().isSelectionEmpty());
  }

  @Test
  public void test() throws Exception {
    assertNull(departmentModel.getMasterModel());
    assertNotNull(departmentModel.getEditModel());
    assertNotNull(departmentModel.getTableModel());
    assertTrue(departmentModel.containsTableModel());

    final EventListener linkedListener = new EventListener() {
      @Override
      public void eventOccurred() {}
    };
    final EventListener listener = new EventListener() {
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
              TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "OPERATIONS");
      departmentModel.getTableModel().getSelectionModel().setSelectedItem(department);
      final EntityModel employeeModel = departmentModel.getDetailModel(TestDomain.T_EMP);
      final EntityComboBoxModel deptComboBoxModel = employeeModel.getEditModel().getForeignKeyComboBoxModel(TestDomain.EMP_DEPARTMENT_FK);
      deptComboBoxModel.refresh();
      deptComboBoxModel.setSelectedItem(department);
      departmentModel.getTableModel().deleteSelected();
      assertEquals(3, employeeModel.getEditModel().getForeignKeyComboBoxModel(TestDomain.EMP_DEPARTMENT_FK).getSize());
      assertNotNull(employeeModel.getEditModel().getForeignKeyComboBoxModel(TestDomain.EMP_DEPARTMENT_FK).getSelectedValue());
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
    departmentModel.getDetailModel(TestDomain.T_EMP);
    assertTrue("DepartmentModel should contain Employee detail", departmentModel.containsDetailModel(TestDomain.T_EMP));
    assertEquals("Only one detail model should be in DepartmentModel", 1, departmentModel.getDetailModels().size());
    assertTrue(departmentModel.getLinkedDetailModels().size() == 1);
    assertTrue("Employee model should be the linked detail model in DepartmentModel",
            departmentModel.getLinkedDetailModels().contains(departmentModel.getDetailModel(TestDomain.T_EMP)));
    assertNotNull(departmentModel.getDetailModel(TestDomain.T_EMP));
    departmentModel.refresh();
    departmentModel.refreshDetailModels();
    assertTrue(departmentModel.getDetailModel(TestDomain.T_EMP).getTableModel().getRowCount() > 0);

    final EntityConnection connection = departmentModel.getConnectionProvider().getConnection();
    final Entity department = connection.selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");
    final List<Entity> salesEmployees = connection.selectMany(EntityCriteriaUtil.selectCriteria(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, SearchType.LIKE, department));
    assertTrue("Number of employees for department should not be 0", salesEmployees.size() > 0);
    departmentModel.getTableModel().getSelectionModel().setSelectedItem(department);
    final List<Entity> employeesFromDetailModel =
            departmentModel.getDetailModel(TestDomain.T_EMP).getTableModel().getAllItems();
    assertTrue("Filtered list should contain all employees for department", containsAll(salesEmployees, employeesFromDetailModel));
  }

  @Test(expected = IllegalArgumentException.class)
  public void addSameDetailModelTwice() {
    departmentModel = new DefaultEntityModel(TestDomain.T_DEPARTMENT, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    final EntityModel employeeModel = new EmpModel(departmentModel.getConnectionProvider());
    departmentModel.addDetailModels(employeeModel, employeeModel);
  }

  @Test(expected = IllegalArgumentException.class)
  public void addDetailModelDetailModelAlreadyHasMasterModel() {
    departmentModel = new DefaultEntityModel(TestDomain.T_DEPARTMENT, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    final EntityModel employeeModel = new EmpModel(departmentModel.getConnectionProvider());
    employeeModel.setMasterModel(departmentModel);
    departmentModel.addDetailModel(employeeModel);
  }

  @Test(expected = IllegalStateException.class)
  public void setMasterModel() {
    departmentModel = new DefaultEntityModel(TestDomain.T_DEPARTMENT, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    final EntityModel employeeModel = new EmpModel(departmentModel.getConnectionProvider());
    employeeModel.setMasterModel(departmentModel);
    employeeModel.setMasterModel(new DefaultEntityModel(TestDomain.T_DEPARTMENT, EntityConnectionProvidersTest.CONNECTION_PROVIDER));
  }

  @Test
  public void addRemoveLinkedDetailModel() {
    departmentModel.removeLinkedDetailModel(departmentModel.getDetailModel(TestDomain.T_EMP));
    assertTrue(departmentModel.getLinkedDetailModels().isEmpty());
    departmentModel.addLinkedDetailModel(departmentModel.getDetailModel(TestDomain.T_EMP));
    assertFalse(departmentModel.getLinkedDetailModels().isEmpty());
    assertTrue(departmentModel.getLinkedDetailModels().contains(departmentModel.getDetailModel(TestDomain.T_EMP)));
  }

  @Test
  public void filterOnMasterInsert() throws DatabaseException, ValidationException {
    final EntityModel employeeModel = departmentModel.getDetailModel(TestDomain.T_EMP);
    employeeModel.setFilterOnMasterInsert(true);
    assertTrue(employeeModel.isFilterOnMasterInsert());
    final EntityEditModel editModel = departmentModel.getEditModel();
    editModel.setValue(TestDomain.DEPARTMENT_ID, 100);
    editModel.setValue(TestDomain.DEPARTMENT_NAME, "Name");
    editModel.setValue(TestDomain.DEPARTMENT_LOCATION, "Loc");
    final List<Entity> inserted = editModel.insert();
    final Collection filter = (Collection) employeeModel.getTableModel().getCriteriaModel().getPropertyCriteriaModel(TestDomain.EMP_DEPARTMENT_FK).getUpperBound();
    assertEquals(inserted.get(0), filter.iterator().next());
    editModel.delete();
  }

  @Before
  public void setUp() throws Exception {
    departmentModel = new DefaultEntityModel(TestDomain.T_DEPARTMENT, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    final EntityModel employeeModel = new EmpModel(departmentModel.getConnectionProvider());
    departmentModel.addDetailModel(employeeModel);
    departmentModel.setDetailModelForeignKey(employeeModel, TestDomain.EMP_DEPARTMENT_FK);
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
/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.FilterCriteria;
import org.jminor.common.model.FilteredModel;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.TestDomain;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public abstract class AbstractEntityModelTest<Model extends DefaultEntityModel<Model, EditModel, TableModel>,
        EditModel extends DefaultEntityEditModel, TableModel extends EntityTableModel<EditModel>> {

  static {
    TestDomain.init();
  }

  protected final Model departmentModel = createDepartmentModel();
  private int eventCount = 0;

  protected abstract Model createDepartmentModel();

  protected abstract Model createDepartmentModelWithoutDetailModel();

  protected abstract EditModel createDepartmentEditModel();

  protected abstract TableModel createEmployeeTableModel();

  protected abstract TableModel createDepartmentTableModel();

  protected abstract Model createEmployeeModel();

  @Test
  public void testUpdatePrimaryKey() throws DatabaseException, ValidationException {
    departmentModel.refresh();
    final EntityEditModel deptEditModel = departmentModel.getEditModel();
    final TableModel deptTableModel = departmentModel.getTableModel();
    final Entity.Key operationsKey = Entities.key(TestDomain.T_DEPARTMENT);
    operationsKey.put(TestDomain.DEPARTMENT_ID, 40);//operations
    deptTableModel.setSelectedByKey(Collections.singletonList(operationsKey));

    assertFalse(deptTableModel.getSelectionModel().isSelectionEmpty());
    deptEditModel.setValue(TestDomain.DEPARTMENT_ID, 80);
    assertFalse(deptTableModel.getSelectionModel().isSelectionEmpty());
    deptEditModel.update();

    assertFalse(deptTableModel.getSelectionModel().isSelectionEmpty());
    final Entity operations = deptTableModel.getSelectionModel().getSelectedItem();
    assertEquals(80, operations.get(TestDomain.DEPARTMENT_ID));

    ((FilteredModel<Entity>) deptTableModel).setFilterCriteria(new FilterCriteria<Entity>() {
      @Override
      public boolean include(final Entity item) {
        return !Util.equal(80, item.get(TestDomain.DEPARTMENT_ID));
      }
    });

    deptEditModel.setEntity(operations);
    deptEditModel.setValue(TestDomain.DEPARTMENT_ID, 40);
    deptEditModel.update();

    assertTrue(deptTableModel.getFilteredItems().isEmpty());
  }

  @Test
  public void testDetailModels() throws CancelException, DatabaseException, ValidationException {
    //todo
  }

  @Test(expected = IllegalArgumentException.class)
  public void getDetailModelNotFound() {
    departmentModel.getDetailModel("undefined");
  }

  @Test
  public void clear() {
    departmentModel.refresh();
    assertTrue(departmentModel.getTableModel().getRowCount() > 0);

    final Model employeeModel = departmentModel.getDetailModel(TestDomain.T_EMP);
    employeeModel.refresh();
    assertTrue(employeeModel.getTableModel().getRowCount() > 0);

    departmentModel.clearDetailModels();
    assertTrue(employeeModel.getTableModel().getRowCount() == 0);

    departmentModel.clear();
    assertTrue(departmentModel.getTableModel().getRowCount() == 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullEditModelNullTableModel() {
    new DefaultEntityModel((DefaultEntityEditModel) null, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorTableModelEntityIDMismatch() {
    final EditModel editModel = createDepartmentEditModel();
    final TableModel tableModel = createEmployeeTableModel();
    new DefaultEntityModel(editModel, tableModel);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorTableModelEditModelMismatch() {
    final DefaultEntityEditModel editModel = createDepartmentEditModel();
    final DefaultEntityEditModel editModel2 = createDepartmentEditModel();
    final EntityTableModel tableModel = createDepartmentTableModel();
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
    final DefaultEntityModel model = createDepartmentModelWithoutDetailModel();
    final DefaultEntityModel employeeModel = createEmployeeModel();
    model.addDetailModels(employeeModel, employeeModel);
  }

  @Test(expected = IllegalArgumentException.class)
  public void addDetailModelDetailModelAlreadyHasMasterModel() {
    final DefaultEntityModel model = createDepartmentModelWithoutDetailModel();
    final DefaultEntityModel employeeModel = createEmployeeModel();
    employeeModel.setMasterModel(model);
    model.addDetailModel(employeeModel);
  }

  @Test(expected = IllegalStateException.class)
  public void setMasterModel() {
    final DefaultEntityModel model = createDepartmentModelWithoutDetailModel();
    final DefaultEntityModel employeeModel = createEmployeeModel();
    employeeModel.setMasterModel(model);
    employeeModel.setMasterModel(departmentModel);
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
    final Entity upperBoundEntity;
    final Object upperBound = employeeModel.getTableModel().getCriteriaModel().getPropertyCriteriaModel(TestDomain.EMP_DEPARTMENT_FK).getUpperBound();
    if (upperBound instanceof Collection) {
      upperBoundEntity = (Entity) ((Collection) upperBound).iterator().next();
    }
    else {
      upperBoundEntity = (Entity) upperBound;
    }
    assertEquals(inserted.get(0), upperBoundEntity);
    editModel.delete();
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
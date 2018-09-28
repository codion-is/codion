/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.EventListener;
import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.valuemap.exception.ValidationException;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A base class for testing {@link EntityModel} subclasses.
 * @param <Model> the {@link EntityModel} type
 * @param <EditModel> the {@link EntityEditModel} type
 * @param <TableModel> the {@link EntityTableModel} type
 */
public abstract class AbstractEntityModelTest<Model extends DefaultEntityModel<Model, EditModel, TableModel>,
        EditModel extends DefaultEntityEditModel, TableModel extends EntityTableModel<EditModel>> {

  protected static final Entities ENTITIES = new TestDomain();

  protected static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  protected static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(ENTITIES, UNIT_TEST_USER, Databases.getInstance());

  protected final Model departmentModel = createDepartmentModel();
  private int eventCount = 0;

  @Test
  public void testUpdatePrimaryKey() throws DatabaseException, ValidationException {
    if (!departmentModel.containsTableModel()) {
      return;
    }
    departmentModel.refresh();
    final EntityEditModel deptEditModel = departmentModel.getEditModel();
    final TableModel deptTableModel = departmentModel.getTableModel();
    final Entity.Key operationsKey = ENTITIES.key(TestDomain.T_DEPARTMENT);
    operationsKey.put(TestDomain.DEPARTMENT_ID, 40);//operations
    deptTableModel.setSelectedByKey(Collections.singletonList(operationsKey));

    assertFalse(deptTableModel.getSelectionModel().isSelectionEmpty());
    deptEditModel.setValue(TestDomain.DEPARTMENT_ID, 80);
    assertFalse(deptTableModel.getSelectionModel().isSelectionEmpty());
    deptEditModel.update();

    assertFalse(deptTableModel.getSelectionModel().isSelectionEmpty());
    final Entity operations = deptTableModel.getSelectionModel().getSelectedItem();
    assertEquals(80, operations.get(TestDomain.DEPARTMENT_ID));

    deptTableModel.setFilterCondition(item ->
            !Objects.equals(80, item.get(TestDomain.DEPARTMENT_ID)));

    deptEditModel.setEntity(operations);
    deptEditModel.setValue(TestDomain.DEPARTMENT_ID, 40);
    deptEditModel.update();

    deptTableModel.filterContents();

    assertTrue(deptTableModel.getFilteredItems().isEmpty());
  }

  @Test
  public void testDetailModels() throws DatabaseException, ValidationException {
    //todo
  }

  @Test
  public void getDetailModelNotFound() {
    assertThrows(IllegalArgumentException.class, () -> departmentModel.getDetailModel("undefined"));
  }

  @Test
  public void clear() {
    if (!departmentModel.containsTableModel()) {
      return;
    }
    departmentModel.refresh();
    assertTrue(departmentModel.getTableModel().getRowCount() > 0);

    final Model employeeModel = departmentModel.getDetailModel(TestDomain.T_EMP);
    employeeModel.refresh();
    assertTrue(employeeModel.getTableModel().getRowCount() > 0);

    departmentModel.clearDetailModels();
    assertEquals(0, employeeModel.getTableModel().getRowCount());

    departmentModel.clear();
    assertEquals(0, departmentModel.getTableModel().getRowCount());
  }

  @Test
  public void constructorNullEditModelNullTableModel() {
    assertThrows(NullPointerException.class, () -> new DefaultEntityModel((DefaultEntityEditModel) null, null));
  }

  @Test
  public void constructorTableModelEntityIDMismatch() {
    if (!departmentModel.containsTableModel()) {
      return;
    }
    final EditModel editModel = createDepartmentEditModel();
    final TableModel tableModel = createEmployeeTableModel();
    assertThrows(IllegalArgumentException.class, () -> new DefaultEntityModel(editModel, tableModel));
  }

  @Test
  public void constructorTableModelEditModelMismatch() {
    if (!departmentModel.containsTableModel()) {
      return;
    }
    final DefaultEntityEditModel editModel = createDepartmentEditModel();
    final DefaultEntityEditModel editModel2 = createDepartmentEditModel();
    final EntityTableModel tableModel = createDepartmentTableModel();
    tableModel.setEditModel(editModel);
    assertThrows(IllegalArgumentException.class, () -> new DefaultEntityModel(editModel2, tableModel));
  }

  @Test
  public void clearEditModelClearTableSelection() {
    if (!departmentModel.containsTableModel()) {
      return;
    }
    departmentModel.refresh();
    departmentModel.getTableModel().getSelectionModel().setSelectedIndexes(Arrays.asList(1, 2, 3));
    assertFalse(departmentModel.getTableModel().getSelectionModel().isSelectionEmpty());
    assertFalse(departmentModel.getEditModel().isEntityNew());
    departmentModel.getEditModel().setEntity(null);
    assertTrue(departmentModel.getTableModel().getSelectionModel().isSelectionEmpty());
  }

  @Test
  public void test() throws Exception {
    assertNull(departmentModel.getMasterModel());
    assertNotNull(departmentModel.getEditModel());

    final EventListener linkedListener = () -> {};
    final EventListener listener = () -> eventCount++;
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
    departmentModel.getDetailModel((Class<? extends Model>) departmentModel.getDetailModel(TestDomain.T_EMP).getClass());
    assertTrue(departmentModel.containsDetailModel(TestDomain.T_EMP));
    assertTrue(departmentModel.containsDetailModel(departmentModel.getDetailModel(TestDomain.T_EMP)));
    assertTrue(departmentModel.containsDetailModel((Class<? extends Model>) departmentModel.getDetailModel(TestDomain.T_EMP).getClass()));
    assertEquals(1, departmentModel.getDetailModels().size(), "Only one detail model should be in DepartmentModel");
    assertEquals(1, departmentModel.getLinkedDetailModels().size());

    departmentModel.getDetailModel(TestDomain.T_EMP);

    assertTrue(departmentModel.getLinkedDetailModels().contains(departmentModel.getDetailModel(TestDomain.T_EMP)));
    assertNotNull(departmentModel.getDetailModel(TestDomain.T_EMP));
    departmentModel.refresh();
    departmentModel.refreshDetailModels();

    if (!departmentModel.containsTableModel()) {
      return;
    }

    assertTrue(departmentModel.getDetailModel(TestDomain.T_EMP).getTableModel().getRowCount() > 0);

    final EntityConnection connection = departmentModel.getConnectionProvider().getConnection();
    final Entity department = connection.selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");
    final List<Entity> salesEmployees = connection.selectMany(new EntityConditions(ENTITIES).selectCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, Condition.Type.LIKE, department));
    assertTrue(!salesEmployees.isEmpty(), "Number of employees for department should not be 0");
    departmentModel.getTableModel().getSelectionModel().setSelectedItem(department);
    final List<Entity> employeesFromDetailModel =
            departmentModel.getDetailModel(TestDomain.T_EMP).getTableModel().getAllItems();
    assertTrue(salesEmployees.containsAll(employeesFromDetailModel), "Filtered list should contain all employees for department");
  }

  @Test
  public void addSameDetailModelTwice() {
    final DefaultEntityModel model = createDepartmentModelWithoutDetailModel();
    final DefaultEntityModel employeeModel = createEmployeeModel();
    assertThrows(IllegalArgumentException.class, () -> model.addDetailModels(employeeModel, employeeModel));
  }

  @Test
  public void addDetailModelDetailModelAlreadyHasMasterModel() {
    final DefaultEntityModel model = createDepartmentModelWithoutDetailModel();
    final DefaultEntityModel employeeModel = createEmployeeModel();
    employeeModel.setMasterModel(model);
    assertThrows(IllegalArgumentException.class, () -> model.addDetailModel(employeeModel));
  }

  @Test
  public void setMasterModel() {
    final DefaultEntityModel model = createDepartmentModelWithoutDetailModel();
    final DefaultEntityModel employeeModel = createEmployeeModel();
    employeeModel.setMasterModel(model);
    assertThrows(IllegalStateException.class, () -> employeeModel.setMasterModel(departmentModel));
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
    if (!departmentModel.containsTableModel()) {
      return;
    }
    final EntityModel employeeModel = departmentModel.getDetailModel(TestDomain.T_EMP);
    employeeModel.setFilterOnMasterInsert(true);
    assertTrue(employeeModel.isFilterOnMasterInsert());
    final EntityEditModel editModel = departmentModel.getEditModel();
    editModel.setValue(TestDomain.DEPARTMENT_ID, 100);
    editModel.setValue(TestDomain.DEPARTMENT_NAME, "Name");
    editModel.setValue(TestDomain.DEPARTMENT_LOCATION, "Loc");
    final List<Entity> inserted = editModel.insert();
    final Entity upperBoundEntity;
    final Object upperBound = employeeModel.getTableModel().getConditionModel().getPropertyConditionModel(TestDomain.EMP_DEPARTMENT_FK).getUpperBound();
    if (upperBound instanceof Collection) {
      upperBoundEntity = (Entity) ((Collection) upperBound).iterator().next();
    }
    else {
      upperBoundEntity = (Entity) upperBound;
    }
    assertEquals(inserted.get(0), upperBoundEntity);
    editModel.delete();
  }

  @Test
  public void insertDifferentTypes() throws DatabaseException, ValidationException {
    if (!departmentModel.containsTableModel()) {
      return;
    }
    final Entity dept = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    dept.put(TestDomain.DEPARTMENT_ID, -42);
    dept.put(TestDomain.DEPARTMENT_NAME, "Name");
    dept.put(TestDomain.DEPARTMENT_LOCATION, "Loc");

    final Entity emp = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_EMP, TestDomain.EMP_ID, 8);
    emp.clearKeyValues();
    emp.put(TestDomain.EMP_NAME, "NewName");

    final EntityModel model = createDepartmentModelWithoutDetailModel();
    model.getEditModel().insert(Arrays.asList(dept, emp));
    assertTrue(model.getTableModel().contains(dept, true));
    assertFalse(model.getTableModel().contains(emp, true));

    model.getEditModel().delete(Arrays.asList(dept, emp));

    assertFalse(model.getTableModel().contains(dept, true));
  }

  /**
   * @return a EntityModel based on the department entity
   * @see TestDomain#T_DEPARTMENT
   */
  protected abstract Model createDepartmentModel();

  /**
   * @return a EntityModel based on the department entity, without detail models
   * @see TestDomain#T_DEPARTMENT
   */
  protected abstract Model createDepartmentModelWithoutDetailModel();

  /**
   * @return a EntityEditModel based on the department entity
   * @see TestDomain#T_DEPARTMENT
   */
  protected abstract EditModel createDepartmentEditModel();

  /**
   * @return a EntityTableModel based on the employee entity
   * @see TestDomain#T_EMP
   */
  protected abstract TableModel createEmployeeTableModel();

  /**
   * @return a EntityTableModel based on the department entity
   * @see TestDomain#T_DEPARTMENT
   */
  protected abstract TableModel createDepartmentTableModel();

  /**
   * @return a EntityModel based on the employee entity
   * @see TestDomain#T_EMP
   */
  protected abstract Model createEmployeeModel();
}
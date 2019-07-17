/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.valuemap.exception.ValidationException;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A base class for testing {@link EntityTableModel} subclasses.
 * @param <EditModel> the {@link EntityEditModel} type
 * @param <TableModel> the {@link EntityTableModel} type
 */
public abstract class AbstractEntityTableModelTest<EditModel extends EntityEditModel, TableModel extends EntityTableModel<EditModel>> {

  protected static final Domain DOMAIN = new TestDomain();

  protected static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  protected static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setUser(UNIT_TEST_USER).setDomainClassName(TestDomain.class.getName());

  protected final List<Entity> testEntities = initTestEntities();

  protected final TableModel testModel = createTestTableModel();

  @Test
  public void setSelectedByKey() {
    final TableModel tableModel = createEmployeeTableModelWithoutEditModel();
    tableModel.refresh();

    final Entity.Key pk1 = DOMAIN.key(TestDomain.T_EMP);
    pk1.put(TestDomain.EMP_ID, 1);
    final Entity.Key pk2 = DOMAIN.key(TestDomain.T_EMP);
    pk2.put(TestDomain.EMP_ID, 2);

    tableModel.setSelectedByKey(Collections.singletonList(pk1));
    final Entity selectedPK1 = tableModel.getSelectionModel().getSelectedItem();
    assertEquals(pk1, selectedPK1.getKey());
    assertEquals(1, tableModel.getSelectionModel().getSelectionCount());

    tableModel.setSelectedByKey(Collections.singletonList(pk2));
    final Entity selectedPK2 = tableModel.getSelectionModel().getSelectedItem();
    assertEquals(pk2, selectedPK2.getKey());
    assertEquals(1, tableModel.getSelectionModel().getSelectionCount());

    final List<Entity.Key> keys = Arrays.asList(pk1, pk2);
    tableModel.setSelectedByKey(keys);
    final List<Entity> selectedItems = tableModel.getSelectionModel().getSelectedItems();
    for (final Entity selected : selectedItems) {
      assertTrue(keys.contains(selected.getKey()));
    }
    assertEquals(2, tableModel.getSelectionModel().getSelectionCount());
  }

  @Test
  public void getSelectedEntitiesIterator() {
    final TableModel tableModel = createEmployeeTableModelWithoutEditModel();
    tableModel.refresh();

    tableModel.getSelectionModel().setSelectedIndexes(Arrays.asList(0, 3, 5));
    final Iterator<Entity> iterator = tableModel.getSelectedEntitiesIterator();
    assertEquals(tableModel.getAllItems().get(0), iterator.next());
    assertEquals(tableModel.getAllItems().get(3), iterator.next());
    assertEquals(tableModel.getAllItems().get(5), iterator.next());
  }

  @Test
  public void updateNoEditModel() {
    final TableModel tableModel = createEmployeeTableModelWithoutEditModel();
    assertThrows(IllegalStateException.class, () -> tableModel.update(new ArrayList<>()));
  }

  @Test
  public void deleteSelectedNoEditModel() {
    final TableModel tableModel = createEmployeeTableModelWithoutEditModel();
    tableModel.refresh();
    tableModel.getSelectionModel().setSelectedIndex(0);
    assertThrows(IllegalStateException.class, tableModel::deleteSelected);
  }

  @Test
  public void addOnInsert() throws DatabaseException, ValidationException {
    final TableModel deptModel = createDepartmentTableModel();
    deptModel.refresh();

    deptModel.setInsertAction(EntityTableModel.InsertAction.ADD_BOTTOM);
    final Entity dept = DOMAIN.entity(TestDomain.T_DEPARTMENT);
    dept.put(TestDomain.DEPARTMENT_ID, -10);
    dept.put(TestDomain.DEPARTMENT_LOCATION, "Nowhere1");
    dept.put(TestDomain.DEPARTMENT_NAME, "HELLO");
    final int count = deptModel.getRowCount();
    deptModel.getEditModel().insert(Collections.singletonList(dept));
    assertEquals(count + 1, deptModel.getRowCount());
    assertEquals(dept, deptModel.getAllItems().get(deptModel.getRowCount() - 1));

    deptModel.setInsertAction(EntityTableModel.InsertAction.ADD_TOP_SORTED);
    final Entity dept2 = DOMAIN.entity(TestDomain.T_DEPARTMENT);
    dept2.put(TestDomain.DEPARTMENT_ID, -20);
    dept2.put(TestDomain.DEPARTMENT_LOCATION, "Nowhere2");
    dept2.put(TestDomain.DEPARTMENT_NAME, "NONAME");
    deptModel.getEditModel().insert(Collections.singletonList(dept2));
    assertEquals(count + 2, deptModel.getRowCount());
    assertEquals(dept2, deptModel.getAllItems().get(2));

    deptModel.setInsertAction(EntityTableModel.InsertAction.DO_NOTHING);
    final Entity dept3 = DOMAIN.entity(TestDomain.T_DEPARTMENT);
    dept3.put(TestDomain.DEPARTMENT_ID, -30);
    dept3.put(TestDomain.DEPARTMENT_LOCATION, "Nowhere3");
    dept3.put(TestDomain.DEPARTMENT_NAME, "NONAME2");
    deptModel.getEditModel().insert(Collections.singletonList(dept3));
    assertEquals(count + 2, deptModel.getRowCount());

    deptModel.refresh();
    assertEquals(count + 3, deptModel.getRowCount());

    deptModel.getEditModel().delete(Arrays.asList(dept, dept2, dept3));
  }

  @Test
  public void removeOnDelete() throws DatabaseException {
    final TableModel tableModel = createEmployeeTableModel();
    tableModel.refresh();

    final Entity.Key pk1 = DOMAIN.key(TestDomain.T_EMP);
    pk1.put(TestDomain.EMP_ID, 1);
    final Entity.Key pk2 = DOMAIN.key(TestDomain.T_EMP);
    pk2.put(TestDomain.EMP_ID, 2);
    try {
      tableModel.getConnectionProvider().getConnection().beginTransaction();
      tableModel.setSelectedByKey(Collections.singletonList(pk1));
      tableModel.getSelectionModel().setSelectedIndex(0);
      Entity selected = tableModel.getSelectionModel().getSelectedItem();
      tableModel.setRemoveEntitiesOnDelete(true);
      assertTrue(tableModel.isRemoveEntitiesOnDelete());
      tableModel.deleteSelected();
      assertFalse(tableModel.contains(selected, false));

      tableModel.setSelectedByKey(Collections.singletonList(pk2));
      selected = tableModel.getSelectionModel().getSelectedItem();
      tableModel.setRemoveEntitiesOnDelete(false);
      assertFalse(tableModel.isRemoveEntitiesOnDelete());
      tableModel.deleteSelected();
      assertTrue(tableModel.contains(selected, false));
    }
    finally {
      tableModel.getConnectionProvider().getConnection().rollbackTransaction();
    }
  }

  @Test
  public void getEntityByKey() {
    final TableModel tableModel = createEmployeeTableModelWithoutEditModel();
    tableModel.refresh();

    final Entity.Key pk1 = DOMAIN.key(TestDomain.T_EMP);
    pk1.put(TestDomain.EMP_ID, 1);
    assertNotNull(tableModel.getEntityByKey(pk1));

    final Entity.Key pk2 = DOMAIN.key(TestDomain.T_EMP);
    pk2.put(TestDomain.EMP_ID, -66);
    assertNull(tableModel.getEntityByKey(pk2));
  }

  @Test
  public void setEditModelNullValue() {
    final TableModel tableModel = createEmployeeTableModelWithoutEditModel();
    assertThrows(NullPointerException.class, () -> tableModel.setEditModel(null));
  }

  @Test
  public void setEditModelWrongEntityId() {
    final TableModel tableModel = createEmployeeTableModelWithoutEditModel();
    assertThrows(IllegalArgumentException.class, () -> tableModel.setEditModel(createDepartmentEditModel()));
  }

  @Test
  public void setEditModelAlreadySet() {
    assertTrue(testModel.hasEditModel());
    assertThrows(IllegalStateException.class, () -> testModel.setEditModel(createDetailEditModel()));
  }

  @Test
  public void setAndGetEditModel() {
    final TableModel tableModel = createDetailTableModel();
    final EditModel editModel = createDetailEditModel();
    assertFalse(tableModel.hasEditModel());
    tableModel.setEditModel(editModel);
    assertTrue(tableModel.hasEditModel());
    assertEquals(editModel, tableModel.getEditModel());
  }

  @Test
  public void getEditModelNoEditModelSet() {
    final TableModel tableModel = createDetailTableModel();
    assertThrows(IllegalStateException.class, tableModel::getEditModel);
  }

  @Test
  public void isUpdateAllowed() {
    final TableModel tableModel = createDetailTableModel();
    final EditModel editModel = createDetailEditModel();
    assertFalse(tableModel.isUpdateAllowed());
    tableModel.setEditModel(editModel);
    assertTrue(tableModel.isUpdateAllowed());
    editModel.setUpdateAllowed(false);
    assertFalse(tableModel.isUpdateAllowed());
  }

  @Test
  public void isDeleteAllowed() {
    final TableModel tableModel = createDetailTableModel();
    final EditModel editModel = createDetailEditModel();
    assertFalse(tableModel.isDeleteAllowed());
    tableModel.setEditModel(editModel);
    assertTrue(tableModel.isDeleteAllowed());
    editModel.setDeleteAllowed(false);
    assertFalse(tableModel.isDeleteAllowed());
  }

  @Test
  public void getEntityId() {
    assertEquals(TestDomain.T_DETAIL, testModel.getEntityId());
  }

  @Test
  public void deleteNotAllowed() {
    testModel.getEditModel().setDeleteAllowed(false);
    assertFalse(testModel.isDeleteAllowed());
    testModel.refresh();
    testModel.getSelectionModel().setSelectedIndexes(Collections.singletonList(0));
    assertThrows(IllegalStateException.class, testModel::deleteSelected);
  }

  @Test
  public void updateNotAllowed() {
    testModel.getEditModel().setUpdateAllowed(false);
    assertFalse(testModel.isUpdateAllowed());
    testModel.refresh();
    testModel.getSelectionModel().setSelectedIndexes(Collections.singletonList(0));
    final Entity entity = testModel.getSelectionModel().getSelectedItem();
    entity.put(TestDomain.DETAIL_STRING, "hello");
    assertThrows(IllegalStateException.class, () -> testModel.update(Collections.singletonList(entity)));
  }

  @Test
  public void batchUpdateNotAllowed() {
    testModel.setBatchUpdateAllowed(false);
    assertFalse(testModel.isBatchUpdateAllowed());
    testModel.refresh();
    testModel.getSelectionModel().setSelectedIndexes(Arrays.asList(0, 1));
    final List<Entity> entities = testModel.getSelectionModel().getSelectedItems();
    Entities.put(TestDomain.DETAIL_STRING, "hello", entities);
    assertThrows(IllegalStateException.class, () -> testModel.update(entities));
  }

  @Test
  public void testTheRest() {
    assertNotNull(testModel.getConnectionProvider());
    testModel.getQueryConditionRequiredState().setActive(false);
    assertFalse(testModel.getQueryConditionRequiredState().isActive());
    testModel.getQueryConditionRequiredState().setActive(true);
    assertTrue(testModel.getQueryConditionRequiredState().isActive());
    testModel.setQueryConfigurationAllowed(false);
    assertFalse(testModel.isQueryConfigurationAllowed());
    testModel.setQueryConfigurationAllowed(true);
    assertTrue(testModel.isQueryConfigurationAllowed());
    testModel.setFetchCount(10);
    assertEquals(10, testModel.getFetchCount());
    assertNotNull(testModel.getEditModel());
    assertFalse(testModel.isReadOnly());
    testModel.refresh();
  }

  @Test
  public void getEntitiesByKey() {
    testModel.refresh();
    Entity tmpEnt = DOMAIN.entity(TestDomain.T_DETAIL);
    tmpEnt.put(TestDomain.DETAIL_ID, 3L);
    assertEquals("c", testModel.getEntityByKey(tmpEnt.getKey()).get(TestDomain.DETAIL_STRING));
    final List<Entity.Key> keys = new ArrayList<>();
    keys.add(tmpEnt.getKey());
    tmpEnt = DOMAIN.entity(TestDomain.T_DETAIL);
    tmpEnt.put(TestDomain.DETAIL_ID, 2L);
    keys.add(tmpEnt.getKey());
    tmpEnt = DOMAIN.entity(TestDomain.T_DETAIL);
    tmpEnt.put(TestDomain.DETAIL_ID, 1L);
    keys.add(tmpEnt.getKey());

    final Collection<Entity> entities = testModel.getEntitiesByKey(keys);
    assertEquals(3, entities.size());
  }

  @Test
  public void noVisibleColumns() {
    assertThrows(IllegalStateException.class, this::createMasterTableModel);
  }

  @Test
  public void getTableDataAsDelimitedString() {
    final TableModel deptModel = createDepartmentTableModel();
    deptModel.setColumns(TestDomain.DEPARTMENT_ID, TestDomain.DEPARTMENT_NAME, TestDomain.DEPARTMENT_LOCATION);
    deptModel.refresh();
    final String expected = "deptno\tdname\tloc\n" +
            "10\tACCOUNTING\tNEW YORK\n" +
            "40\tOPERATIONS\tBOSTON\n" +
            "20\tRESEARCH\tDALLAS\n" +
            "30\tSALES\tCHICAGO";
    assertEquals(expected, deptModel.getTableDataAsDelimitedString('\t'));
  }

  @Test
  public void setColumns() {
    final TableModel empModel = createEmployeeTableModel();
    empModel.setColumns(TestDomain.EMP_COMMISSION, TestDomain.EMP_DEPARTMENT_FK, TestDomain.EMP_HIREDATE);
  }

  /**
   * @return a static EntityTableModel using {@link #testEntities} with an edit model
   * @see TestDomain#T_DETAIL
   */
  protected abstract TableModel createTestTableModel();

  /**
   * @return a EntityTableModel based on the master entity
   * @see TestDomain#T_MASTER
   */
  protected abstract TableModel createMasterTableModel();

  protected abstract TableModel createEmployeeTableModelWithoutEditModel();

  protected abstract TableModel createDepartmentTableModel();

  protected abstract TableModel createEmployeeTableModel();

  protected abstract EditModel createDepartmentEditModel();

  protected abstract TableModel createDetailTableModel();

  protected abstract EditModel createDetailEditModel();

  private static List<Entity> initTestEntities() {
    final List<Entity> testEntities = new ArrayList<>(5);
    final String[] stringValues = new String[] {"a", "b", "c", "d", "e"};
    for (int i = 0; i < 5; i++) {
      final Entity entity = DOMAIN.entity(TestDomain.T_DETAIL);
      entity.put(TestDomain.DETAIL_ID, (long) i + 1);
      entity.put(TestDomain.DETAIL_INT, i + 1);
      entity.put(TestDomain.DETAIL_STRING, stringValues[i]);
      testEntities.add(entity);
    }

    return testEntities;
  }
}
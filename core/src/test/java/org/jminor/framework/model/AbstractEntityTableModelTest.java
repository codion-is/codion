/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.db.EntityConnectionProvidersTest;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.TestDomain;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public abstract class AbstractEntityTableModelTest<T extends EntityTableModel> {

  protected final T testModel = createTestTableModel();

  static {
    TestDomain.init();
  }

  protected abstract T createTestTableModel();

  protected abstract T createMasterTableModel();

  protected abstract T createEmployeeTableModelWithoutEditModel();

  protected abstract T createDepartmentTableModel();

  protected abstract T createEmployeeTableModel();

  protected abstract EntityEditModel createDepartmentEditModel();

  protected abstract T createDetailTableModel();

  protected abstract EntityEditModel createDetailEditModel();

  @Test
  public void setSelectedByKey() {
    final T tableModel = createEmployeeTableModelWithoutEditModel();
    tableModel.refresh();

    final Entity.Key pk1 = Entities.key(TestDomain.T_EMP);
    pk1.put(TestDomain.EMP_ID, 1);
    final Entity.Key pk2 = Entities.key(TestDomain.T_EMP);
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
    final T tableModel = createEmployeeTableModelWithoutEditModel();
    tableModel.refresh();

    tableModel.getSelectionModel().setSelectedIndexes(Arrays.asList(0, 3, 5));
    final Iterator<Entity> iterator = tableModel.getSelectedEntitiesIterator();
    assertEquals(tableModel.getAllItems().get(0), iterator.next());
    assertEquals(tableModel.getAllItems().get(3), iterator.next());
    assertEquals(tableModel.getAllItems().get(5), iterator.next());
  }

  @Test(expected = IllegalStateException.class)
  public void updateNoEditModel() throws CancelException, ValidationException, DatabaseException {
    final T tableModel = createEmployeeTableModelWithoutEditModel();
    tableModel.update(new ArrayList<>());
  }

  @Test(expected = IllegalStateException.class)
  public void deleteSelectedNoEditModel() throws CancelException, DatabaseException {
    final T tableModel = createEmployeeTableModelWithoutEditModel();
    tableModel.refresh();
    tableModel.getSelectionModel().setSelectedIndex(0);
    tableModel.deleteSelected();
  }

  @Test
  public void addOnInsert() throws CancelException, DatabaseException, ValidationException {
    final T deptModel = createDepartmentTableModel();
    deptModel.refresh();

    deptModel.setInsertAction(EntityTableModel.InsertAction.ADD_BOTTOM);
    final Entity dept = Entities.entity(TestDomain.T_DEPARTMENT);
    dept.put(TestDomain.DEPARTMENT_ID, -10);
    dept.put(TestDomain.DEPARTMENT_LOCATION, "Nowhere");
    dept.put(TestDomain.DEPARTMENT_NAME, "Noname");
    final int count = deptModel.getRowCount();
    deptModel.getEditModel().insert(Collections.singletonList(dept));
    assertEquals(count + 1, deptModel.getRowCount());
    assertEquals(dept, deptModel.getAllItems().get(deptModel.getRowCount() - 1));

    deptModel.setInsertAction(EntityTableModel.InsertAction.DO_NOTHING);
    final Entity dept2 = Entities.entity(TestDomain.T_DEPARTMENT);
    dept2.put(TestDomain.DEPARTMENT_ID, -20);
    dept2.put(TestDomain.DEPARTMENT_LOCATION, "Nowhere2");
    dept2.put(TestDomain.DEPARTMENT_NAME, "Noname2");
    deptModel.getEditModel().insert(Collections.singletonList(dept2));
    assertEquals(count + 1, deptModel.getRowCount());

    deptModel.refresh();
    assertEquals(count + 2, deptModel.getRowCount());

    deptModel.getEditModel().delete(Arrays.asList(dept, dept2));
  }

  @Test
  public void removeOnDelete() throws CancelException, DatabaseException {
    final EntityTableModel tableModel = createEmployeeTableModel();
    tableModel.refresh();

    final Entity.Key pk1 = Entities.key(TestDomain.T_EMP);
    pk1.put(TestDomain.EMP_ID, 1);
    final Entity.Key pk2 = Entities.key(TestDomain.T_EMP);
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
    final EntityTableModel tableModel = createEmployeeTableModelWithoutEditModel();
    tableModel.refresh();

    final Entity.Key pk1 = Entities.key(TestDomain.T_EMP);
    pk1.put(TestDomain.EMP_ID, 1);
    assertNotNull(tableModel.getEntityByKey(pk1));

    final Entity.Key pk2 = Entities.key(TestDomain.T_EMP);
    pk2.put(TestDomain.EMP_ID, -66);
    assertNull(tableModel.getEntityByKey(pk2));
  }

  @Test(expected = IllegalArgumentException.class)
  public void setEditModelNullValue() {
    final EntityTableModel tableModel = createEmployeeTableModelWithoutEditModel();
    tableModel.setEditModel(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setEditModelWrongEntityID() {
    final EntityTableModel tableModel = createEmployeeTableModelWithoutEditModel();
    tableModel.setEditModel(createDepartmentEditModel());
  }

  @Test(expected = IllegalStateException.class)
  public void setEditModelAlreadySet() {
    assertTrue(testModel.hasEditModel());
    testModel.setEditModel(createDetailEditModel());
  }

  @Test
  public void setAndGetEditModel() {
    final EntityTableModel tableModel = createDetailTableModel();
    final EntityEditModel editModel = createDetailEditModel();
    assertFalse(tableModel.hasEditModel());
    tableModel.setEditModel(editModel);
    assertTrue(tableModel.hasEditModel());
    assertEquals(editModel, tableModel.getEditModel());
  }

  @Test(expected = IllegalStateException.class)
  public void getEditModelNoEditModelSet() {
    final EntityTableModel tableModel = createDetailTableModel();
    tableModel.getEditModel();
  }

  @Test
  public void isUpdateAllowed() {
    final EntityTableModel tableModel = createDetailTableModel();
    final EntityEditModel editModel = createDetailEditModel();
    assertFalse(tableModel.isUpdateAllowed());
    tableModel.setEditModel(editModel);
    assertTrue(tableModel.isUpdateAllowed());
    editModel.setUpdateAllowed(false);
    assertFalse(tableModel.isUpdateAllowed());
  }

  @Test
  public void isDeleteAllowed() {
    final EntityTableModel tableModel = createDetailTableModel();
    final EntityEditModel editModel = createDetailEditModel();
    assertFalse(tableModel.isDeleteAllowed());
    tableModel.setEditModel(editModel);
    assertTrue(tableModel.isDeleteAllowed());
    editModel.setDeleteAllowed(false);
    assertFalse(tableModel.isDeleteAllowed());
  }

  @Test
  public void getEntityID() {
    assertEquals(TestDomain.T_DETAIL, testModel.getEntityID());
  }

  @Test(expected = IllegalStateException.class)
  public void deleteNotAllowed() throws CancelException, DatabaseException {
    testModel.getEditModel().setDeleteAllowed(false);
    assertFalse(testModel.isDeleteAllowed());
    testModel.refresh();
    testModel.getSelectionModel().setSelectedIndex(0);
    testModel.deleteSelected();
  }

  @Test(expected = IllegalStateException.class)
  public void updateNotAllowed() throws DatabaseException, CancelException, ValidationException {
    testModel.getEditModel().setUpdateAllowed(false);
    assertFalse(testModel.isUpdateAllowed());
    testModel.refresh();
    testModel.getSelectionModel().setSelectedIndex(0);
    final Entity entity = testModel.getSelectionModel().getSelectedItem();
    entity.put(TestDomain.DETAIL_STRING, "hello");
    testModel.update(Collections.singletonList(entity));
  }

  @Test(expected = IllegalStateException.class)
  public void batchUpdateNotAllowed() throws DatabaseException, CancelException, ValidationException {
    testModel.setBatchUpdateAllowed(false);
    assertFalse(testModel.isBatchUpdateAllowed());
    testModel.refresh();
    testModel.getSelectionModel().setSelectedIndexes(Arrays.asList(0, 1));
    final List<Entity> entities = testModel.getSelectionModel().getSelectedItems();
    EntityUtil.put(TestDomain.DETAIL_STRING, "hello", entities);
    testModel.update(entities);
  }

  @Test
  public void testTheRest() {
    assertEquals(EntityConnectionProvidersTest.CONNECTION_PROVIDER, testModel.getConnectionProvider());
    testModel.setQueryCriteriaRequired(false);
    assertFalse(testModel.isQueryCriteriaRequired());
    testModel.setQueryCriteriaRequired(true);
    assertTrue(testModel.isQueryCriteriaRequired());
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
  public void getEntitiesByPropertyValue() {
    testModel.refresh();
    final Map<String, Object> propValues = new HashMap<>();
    propValues.put(TestDomain.DETAIL_STRING, "b");
    assertEquals(1, testModel.getEntitiesByPropertyValue(propValues).size());
    propValues.put(TestDomain.DETAIL_STRING, "zz");
    assertTrue(testModel.getEntitiesByPropertyValue(propValues).isEmpty());
  }

  @Test
  public void getEntitiesByKey() {
    testModel.refresh();
    Entity tmpEnt = Entities.entity(TestDomain.T_DETAIL);
    tmpEnt.put(TestDomain.DETAIL_ID, 3l);
    assertEquals("c", testModel.getEntityByKey(tmpEnt.getKey()).get(TestDomain.DETAIL_STRING));
    final List<Entity.Key> keys = new ArrayList<>();
    keys.add(tmpEnt.getKey());
    tmpEnt = Entities.entity(TestDomain.T_DETAIL);
    tmpEnt.put(TestDomain.DETAIL_ID, 2l);
    keys.add(tmpEnt.getKey());
    tmpEnt = Entities.entity(TestDomain.T_DETAIL);
    tmpEnt.put(TestDomain.DETAIL_ID, 1l);
    keys.add(tmpEnt.getKey());

    final Collection<Entity> entities = testModel.getEntitiesByKey(keys);
    assertEquals(3, entities.size());
  }

  @Test(expected = IllegalStateException.class)
  public void noVisibleColumns() {
    createMasterTableModel();
  }
}
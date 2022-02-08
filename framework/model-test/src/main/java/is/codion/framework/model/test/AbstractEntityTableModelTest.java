/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model.test;

import is.codion.common.Operator;
import is.codion.common.Util;
import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.model.EntityEditModel;
import is.codion.framework.model.EntityTableModel;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A base class for testing {@link EntityTableModel} subclasses.
 * @param <EditModel> the {@link EntityEditModel} type
 * @param <TableModel> the {@link EntityTableModel} type
 */
public abstract class AbstractEntityTableModelTest<EditModel extends EntityEditModel, TableModel extends EntityTableModel<EditModel>> {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          DatabaseFactory.getDatabase()).setUser(UNIT_TEST_USER).setDomainClassName(TestDomain.class.getName());

  private final EntityConnectionProvider connectionProvider;

  protected final List<Entity> testEntities = initTestEntities(CONNECTION_PROVIDER.getEntities());

  protected final TableModel testModel;

  protected AbstractEntityTableModelTest() {
    connectionProvider = CONNECTION_PROVIDER;
    testModel = createTestTableModel();
  }

  @Test
  public void setSelectedByKey() {
    final TableModel tableModel = createEmployeeTableModel();
    tableModel.refresh();

    final List<Key> keys = tableModel.getEntities().primaryKeys(TestDomain.T_EMP, 1, 2);
    final Key pk1 = keys.get(0);
    final Key pk2 = keys.get(1);

    tableModel.setSelectedByKey(singletonList(pk1));
    final Entity selectedPK1 = tableModel.getSelectionModel().getSelectedItem();
    assertEquals(pk1, selectedPK1.getPrimaryKey());
    assertEquals(1, tableModel.getSelectionModel().getSelectionCount());

    tableModel.setSelectedByKey(singletonList(pk2));
    final Entity selectedPK2 = tableModel.getSelectionModel().getSelectedItem();
    assertEquals(pk2, selectedPK2.getPrimaryKey());
    assertEquals(1, tableModel.getSelectionModel().getSelectionCount());

    tableModel.setSelectedByKey(keys);
    final List<Entity> selectedItems = tableModel.getSelectionModel().getSelectedItems();
    for (final Entity selected : selectedItems) {
      assertTrue(keys.contains(selected.getPrimaryKey()));
    }
    assertEquals(2, tableModel.getSelectionModel().getSelectionCount());
  }

  @Test
  public void getSelectedEntitiesIterator() {
    final TableModel tableModel = createEmployeeTableModel();
    tableModel.refresh();

    tableModel.getSelectionModel().setSelectedIndexes(asList(0, 3, 5));
    final Iterator<Entity> iterator = tableModel.getSelectedEntitiesIterator();
    assertEquals(tableModel.getItems().get(0), iterator.next());
    assertEquals(tableModel.getItems().get(3), iterator.next());
    assertEquals(tableModel.getItems().get(5), iterator.next());
  }

  @Test
  public void addOnInsert() throws DatabaseException, ValidationException {
    final TableModel deptModel = createDepartmentTableModel();
    deptModel.refresh();

    final Entities entities = deptModel.getEntities();
    deptModel.setInsertAction(EntityTableModel.InsertAction.ADD_BOTTOM);
    final Entity dept = entities.builder(TestDomain.T_DEPARTMENT)
            .with(TestDomain.DEPARTMENT_ID, -10)
            .with(TestDomain.DEPARTMENT_LOCATION, "Nowhere1")
            .with(TestDomain.DEPARTMENT_NAME, "HELLO")
            .build();
    final int count = deptModel.getRowCount();
    deptModel.getEditModel().insert(singletonList(dept));
    assertEquals(count + 1, deptModel.getRowCount());
    assertEquals(dept, deptModel.getItems().get(deptModel.getRowCount() - 1));

    deptModel.setInsertAction(EntityTableModel.InsertAction.ADD_TOP_SORTED);
    final Entity dept2 = entities.builder(TestDomain.T_DEPARTMENT)
            .with(TestDomain.DEPARTMENT_ID, -20)
            .with(TestDomain.DEPARTMENT_LOCATION, "Nowhere2")
            .with(TestDomain.DEPARTMENT_NAME, "NONAME")
            .build();
    deptModel.getEditModel().insert(singletonList(dept2));
    assertEquals(count + 2, deptModel.getRowCount());
    assertEquals(dept2, deptModel.getItems().get(2));

    deptModel.setInsertAction(EntityTableModel.InsertAction.DO_NOTHING);
    final Entity dept3 = entities.builder(TestDomain.T_DEPARTMENT)
            .with(TestDomain.DEPARTMENT_ID, -30)
            .with(TestDomain.DEPARTMENT_LOCATION, "Nowhere3")
            .with(TestDomain.DEPARTMENT_NAME, "NONAME2")
            .build();
    deptModel.getEditModel().insert(singletonList(dept3));
    assertEquals(count + 2, deptModel.getRowCount());

    deptModel.refresh();
    assertEquals(count + 3, deptModel.getRowCount());

    deptModel.getEditModel().delete(asList(dept, dept2, dept3));
  }

  @Test
  public void removeOnDelete() throws DatabaseException {
    final TableModel tableModel = createEmployeeTableModel();
    tableModel.refresh();

    final Entities entities = tableModel.getEntities();
    final Key pk1 = entities.primaryKey(TestDomain.T_EMP, 1);
    final Key pk2 = entities.primaryKey(TestDomain.T_EMP, 2);
    tableModel.getConnectionProvider().getConnection().beginTransaction();
    try {
      tableModel.setSelectedByKey(singletonList(pk1));
      tableModel.getSelectionModel().setSelectedIndex(0);
      Entity selected = tableModel.getSelectionModel().getSelectedItem();
      tableModel.setRemoveEntitiesOnDelete(true);
      assertTrue(tableModel.isRemoveEntitiesOnDelete());
      tableModel.deleteSelected();
      assertFalse(tableModel.containsItem(selected));

      tableModel.setSelectedByKey(singletonList(pk2));
      selected = tableModel.getSelectionModel().getSelectedItem();
      tableModel.setRemoveEntitiesOnDelete(false);
      assertFalse(tableModel.isRemoveEntitiesOnDelete());
      tableModel.deleteSelected();
      assertTrue(tableModel.containsItem(selected));
    }
    finally {
      tableModel.getConnectionProvider().getConnection().rollbackTransaction();
    }
  }

  @Test
  public void getEntityByKey() {
    final TableModel tableModel = createEmployeeTableModel();
    tableModel.refresh();

    final Entities entities = tableModel.getEntities();
    final Key pk1 = entities.primaryKey(TestDomain.T_EMP, 1);
    assertNotNull(tableModel.getEntityByKey(pk1));

    final Key pk2 = entities.primaryKey(TestDomain.T_EMP, -66);
    assertNull(tableModel.getEntityByKey(pk2));
  }

  @Test
  public void isUpdateEnabled() {
    final TableModel tableModel = createDetailTableModel();
    assertTrue(tableModel.isUpdateEnabled());
    tableModel.getEditModel().setUpdateEnabled(false);
    assertFalse(tableModel.isUpdateEnabled());
  }

  @Test
  public void isDeleteEnabled() {
    final TableModel tableModel = createDetailTableModel();
    assertTrue(tableModel.isDeleteEnabled());
    tableModel.getEditModel().setDeleteEnabled(false);
    assertFalse(tableModel.isDeleteEnabled());
  }

  @Test
  public void getEntityType() {
    assertEquals(TestDomain.T_DETAIL, testModel.getEntityType());
  }

  @Test
  public void deleteNotEnabled() {
    testModel.getEditModel().setDeleteEnabled(false);
    assertFalse(testModel.isDeleteEnabled());
    testModel.refresh();
    testModel.getSelectionModel().setSelectedIndexes(singletonList(0));
    assertThrows(IllegalStateException.class, testModel::deleteSelected);
  }

  @Test
  public void updateNotEnabled() {
    testModel.getEditModel().setUpdateEnabled(false);
    assertFalse(testModel.isUpdateEnabled());
    testModel.refresh();
    testModel.getSelectionModel().setSelectedIndexes(singletonList(0));
    final Entity entity = testModel.getSelectionModel().getSelectedItem();
    entity.put(TestDomain.DETAIL_STRING, "hello");
    assertThrows(IllegalStateException.class, () -> testModel.update(singletonList(entity)));
  }

  @Test
  public void batchUpdateNotEnabled() {
    testModel.setBatchUpdateEnabled(false);
    assertFalse(testModel.isBatchUpdateEnabled());
    testModel.refresh();
    testModel.getSelectionModel().setSelectedIndexes(asList(0, 1));
    final List<Entity> entities = testModel.getSelectionModel().getSelectedItems();
    Entity.put(TestDomain.DETAIL_STRING, "hello", entities);
    assertThrows(IllegalStateException.class, () -> testModel.update(entities));
  }

  @Test
  public void testTheRest() {
    assertNotNull(testModel.getConnectionProvider());
    testModel.getQueryConditionRequiredState().set(false);
    assertFalse(testModel.getQueryConditionRequiredState().get());
    testModel.getQueryConditionRequiredState().set(true);
    assertTrue(testModel.getQueryConditionRequiredState().get());
    testModel.setLimit(10);
    assertEquals(10, testModel.getLimit());
    assertNotNull(testModel.getEditModel());
    assertFalse(testModel.isReadOnly());
    testModel.refresh();
  }

  @Test
  public void getEntitiesByKey() {
    testModel.refresh();
    final Entities entities = testModel.getEntities();
    Entity tmpEnt = entities.builder(TestDomain.T_DETAIL)
            .with(TestDomain.DETAIL_ID, 3L)
            .build();
    assertEquals("c", testModel.getEntityByKey(tmpEnt.getPrimaryKey()).get(TestDomain.DETAIL_STRING));
    final List<Key> keys = new ArrayList<>();
    keys.add(tmpEnt.getPrimaryKey());
    tmpEnt = entities.builder(TestDomain.T_DETAIL)
            .with(TestDomain.DETAIL_ID, 2L)
            .build();
    keys.add(tmpEnt.getPrimaryKey());
    tmpEnt = entities.builder(TestDomain.T_DETAIL)
            .with(TestDomain.DETAIL_ID, 1L)
            .build();
    keys.add(tmpEnt.getPrimaryKey());

    final Collection<Entity> entitiesByKey = testModel.getEntitiesByKey(keys);
    assertEquals(3, entitiesByKey.size());
  }

  @Test
  public void queryRowCountLimit() {
    final TableModel tableModel = createEmployeeTableModel();
    final Value<Throwable> exceptionValue = Value.value();
    tableModel.addRefreshFailedListener(exceptionValue::set);
    tableModel.setQueryRowCountLimit(6);
    tableModel.refresh();
    assertTrue(exceptionValue.isNotNull());
    assertEquals(IllegalStateException.class, exceptionValue.get().getClass());
    exceptionValue.set(null);
    final ColumnConditionModel<?, Double> commissionConditionModel =
            tableModel.getTableConditionModel().getConditionModel(TestDomain.EMP_COMMISSION);
    commissionConditionModel.setOperator(Operator.EQUAL);
    commissionConditionModel.setEnabled(true);
    tableModel.refresh();
    assertTrue(exceptionValue.isNull());
    commissionConditionModel.setEnabled(false);
    tableModel.refresh();
    assertTrue(exceptionValue.isNotNull());
    assertEquals(IllegalStateException.class, exceptionValue.get().getClass());
    exceptionValue.set(null);
    tableModel.setQueryRowCountLimit(-1);
    tableModel.refresh();
    assertTrue(exceptionValue.isNull());
  }

  @Test
  public void noVisibleColumns() {
    assertThrows(IllegalArgumentException.class, this::createMasterTableModel);
  }

  @Test
  public void getTableDataAsDelimitedString() {
    final TableModel deptModel = createDepartmentTableModel();
    deptModel.setColumns(TestDomain.DEPARTMENT_ID, TestDomain.DEPARTMENT_NAME, TestDomain.DEPARTMENT_LOCATION);
    deptModel.refresh();
    final String newline = Util.LINE_SEPARATOR;
    final String expected = "deptno\tdname\tloc" + newline +
            "10\tACCOUNTING\tNEW YORK" + newline +
            "40\tOPERATIONS\tBOSTON" + newline +
            "20\tRESEARCH\tDALLAS" + newline +
            "30\tSALES\tCHICAGO";
    assertEquals(expected, deptModel.getTableDataAsDelimitedString('\t'));
  }

  @Test
  public void setColumns() {
    final TableModel empModel = createEmployeeTableModel();
    empModel.setColumns(TestDomain.EMP_COMMISSION, TestDomain.EMP_DEPARTMENT_FK, TestDomain.EMP_HIREDATE);
  }

  protected final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  /**
   * @return a EntityTableModel using {@link #testEntities} with an edit model
   * @see TestDomain#T_DETAIL
   */
  protected abstract TableModel createTestTableModel();

  /**
   * @return a EntityTableModel based on the master entity
   * @see TestDomain#T_MASTER
   */
  protected abstract TableModel createMasterTableModel();

  protected abstract TableModel createDepartmentTableModel();

  protected abstract TableModel createEmployeeTableModel();

  protected abstract EditModel createDepartmentEditModel();

  protected abstract TableModel createDetailTableModel();

  protected abstract EditModel createDetailEditModel();

  private static List<Entity> initTestEntities(final Entities entities) {
    final List<Entity> testEntities = new ArrayList<>(5);
    final String[] stringValues = new String[] {"a", "b", "c", "d", "e"};
    for (int i = 0; i < 5; i++) {
      testEntities.add(entities.builder(TestDomain.T_DETAIL)
              .with(TestDomain.DETAIL_ID, (long) i + 1)
              .with(TestDomain.DETAIL_INT, i + 1)
              .with(TestDomain.DETAIL_STRING, stringValues[i])
              .build());
    }

    return testEntities;
  }
}
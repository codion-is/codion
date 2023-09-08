/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model.test;

import is.codion.common.Operator;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.model.EntityEditModel;
import is.codion.framework.model.EntityTableModel;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Detail;
import is.codion.framework.model.test.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .user(UNIT_TEST_USER)
          .domain(new TestDomain())
          .build();

  private final EntityConnectionProvider connectionProvider;

  protected final List<Entity> testEntities = initTestEntities(CONNECTION_PROVIDER.entities());

  protected final TableModel testModel;

  protected AbstractEntityTableModelTest() {
    connectionProvider = CONNECTION_PROVIDER;
    testModel = createTestTableModel();
  }

  @Test
  public void selectEntitiesByKey() {
    TableModel tableModel = createEmployeeTableModel();
    tableModel.refresh();

    List<Entity.Key> keys = tableModel.entities().primaryKeys(Employee.TYPE, 1, 2);
    Entity.Key pk1 = keys.get(0);
    Entity.Key pk2 = keys.get(1);

    tableModel.selectEntitiesByKey(singletonList(pk1));
    Entity selectedPK1 = tableModel.selectionModel().getSelectedItem();
    assertEquals(pk1, selectedPK1.primaryKey());
    assertEquals(1, tableModel.selectionModel().selectionCount());

    tableModel.selectEntitiesByKey(singletonList(pk2));
    Entity selectedPK2 = tableModel.selectionModel().getSelectedItem();
    assertEquals(pk2, selectedPK2.primaryKey());
    assertEquals(1, tableModel.selectionModel().selectionCount());

    tableModel.selectEntitiesByKey(keys);
    List<Entity> selectedItems = tableModel.selectionModel().getSelectedItems();
    for (Entity selected : selectedItems) {
      assertTrue(keys.contains(selected.primaryKey()));
    }
    assertEquals(2, tableModel.selectionModel().selectionCount());
  }

  @Test
  public void selectedEntitiesIterator() {
    TableModel tableModel = createEmployeeTableModel();
    tableModel.refresh();

    tableModel.selectionModel().setSelectedIndexes(asList(0, 3, 5));
    Iterator<Entity> iterator = tableModel.selectionModel().getSelectedItems().iterator();
    assertEquals(tableModel.visibleItems().get(0), iterator.next());
    assertEquals(tableModel.visibleItems().get(3), iterator.next());
    assertEquals(tableModel.visibleItems().get(5), iterator.next());
  }

  @Test
  public void addOnInsert() throws DatabaseException, ValidationException {
    TableModel deptModel = createDepartmentTableModel();
    deptModel.refresh();

    Entities entities = deptModel.entities();
    deptModel.setOnInsert(EntityTableModel.OnInsert.ADD_BOTTOM);
    Entity dept = entities.builder(Department.TYPE)
            .with(Department.ID, -10)
            .with(Department.LOCATION, "Nowhere1")
            .with(Department.NAME, "HELLO")
            .build();
    int count = deptModel.getRowCount();
    deptModel.editModel().insert(singletonList(dept));
    assertEquals(count + 1, deptModel.getRowCount());
    assertEquals(dept, deptModel.visibleItems().get(deptModel.getRowCount() - 1));

    deptModel.setOnInsert(EntityTableModel.OnInsert.ADD_TOP_SORTED);
    Entity dept2 = entities.builder(Department.TYPE)
            .with(Department.ID, -20)
            .with(Department.LOCATION, "Nowhere2")
            .with(Department.NAME, "NONAME")
            .build();
    deptModel.editModel().insert(singletonList(dept2));
    assertEquals(count + 2, deptModel.getRowCount());
    assertEquals(dept2, deptModel.visibleItems().get(2));

    deptModel.setOnInsert(EntityTableModel.OnInsert.DO_NOTHING);
    Entity dept3 = entities.builder(Department.TYPE)
            .with(Department.ID, -30)
            .with(Department.LOCATION, "Nowhere3")
            .with(Department.NAME, "NONAME2")
            .build();
    deptModel.editModel().insert(singletonList(dept3));
    assertEquals(count + 2, deptModel.getRowCount());

    deptModel.refresh();
    assertEquals(count + 3, deptModel.getRowCount());

    deptModel.editModel().delete(asList(dept, dept2, dept3));
  }

  @Test
  public void removeDeletedEntities() throws DatabaseException {
    TableModel tableModel = createEmployeeTableModel();
    tableModel.refresh();

    Entities entities = tableModel.entities();
    Entity.Key pk1 = entities.primaryKey(Employee.TYPE, 1);
    Entity.Key pk2 = entities.primaryKey(Employee.TYPE, 2);
    tableModel.connectionProvider().connection().beginTransaction();
    try {
      tableModel.selectEntitiesByKey(singletonList(pk1));
      tableModel.selectionModel().setSelectedIndex(0);
      Entity selected = tableModel.selectionModel().getSelectedItem();
      tableModel.setRemoveDeletedEntities(true);
      assertTrue(tableModel.isRemoveDeletedEntities());
      tableModel.deleteSelected();
      assertFalse(tableModel.containsItem(selected));

      tableModel.selectEntitiesByKey(singletonList(pk2));
      selected = tableModel.selectionModel().getSelectedItem();
      tableModel.setRemoveDeletedEntities(false);
      assertFalse(tableModel.isRemoveDeletedEntities());
      tableModel.deleteSelected();
      assertTrue(tableModel.containsItem(selected));
    }
    finally {
      tableModel.connectionProvider().connection().rollbackTransaction();
    }
  }

  @Test
  public void entityByKey() {
    TableModel tableModel = createEmployeeTableModel();
    tableModel.refresh();

    Entities entities = tableModel.entities();
    Entity.Key pk1 = entities.primaryKey(Employee.TYPE, 1);
    assertNotNull(tableModel.entityByKey(pk1));

    Entity.Key pk2 = entities.primaryKey(Employee.TYPE, -66);
    assertNull(tableModel.entityByKey(pk2));
  }

  @Test
  public void entityType() {
    assertEquals(Detail.TYPE, testModel.entityType());
  }

  @Test
  public void deleteNotEnabled() {
    testModel.editModel().deleteEnabled().set(false);
    testModel.refresh();
    testModel.selectionModel().setSelectedIndexes(singletonList(0));
    assertThrows(IllegalStateException.class, testModel::deleteSelected);
  }

  @Test
  public void updateNotEnabled() {
    testModel.editModel().updateEnabled().set(false);
    testModel.refresh();
    testModel.selectionModel().setSelectedIndexes(singletonList(0));
    Entity entity = testModel.selectionModel().getSelectedItem();
    entity.put(Detail.STRING, "hello");
    assertThrows(IllegalStateException.class, () -> testModel.update(singletonList(entity)));
  }

  @Test
  public void multipleEntityUpdateEnabled() {
    testModel.setMultipleEntityUpdateEnabled(false);
    assertFalse(testModel.isMultipleEntityUpdateEnabled());
    testModel.refresh();
    testModel.selectionModel().setSelectedIndexes(asList(0, 1));
    List<Entity> entities = testModel.selectionModel().getSelectedItems();
    Entity.put(Detail.STRING, "hello", entities);
    assertThrows(IllegalStateException.class, () -> testModel.update(entities));
  }

  @Test
  public void testTheRest() {
    assertNotNull(testModel.connectionProvider());
    testModel.conditionRequired().set(false);
    assertFalse(testModel.conditionRequired().get());
    testModel.conditionRequired().set(true);
    assertTrue(testModel.conditionRequired().get());
    testModel.setLimit(10);
    assertEquals(10, testModel.getLimit());
    assertNotNull(testModel.editModel());
    assertFalse(testModel.editModel().isReadOnly());
    testModel.refresh();
  }

  @Test
  public void entitiesByKey() {
    testModel.refresh();
    Entities entities = testModel.entities();
    Entity tmpEnt = entities.builder(Detail.TYPE)
            .with(Detail.ID, 3L)
            .build();
    assertEquals("c", testModel.entityByKey(tmpEnt.primaryKey()).get(Detail.STRING));
    List<Entity.Key> keys = new ArrayList<>();
    keys.add(tmpEnt.primaryKey());
    tmpEnt = entities.builder(Detail.TYPE)
            .with(Detail.ID, 2L)
            .build();
    keys.add(tmpEnt.primaryKey());
    tmpEnt = entities.builder(Detail.TYPE)
            .with(Detail.ID, 1L)
            .build();
    keys.add(tmpEnt.primaryKey());

    Collection<Entity> entitiesByKey = testModel.entitiesByKey(keys);
    assertEquals(3, entitiesByKey.size());
  }

  @Test
  public void limit() {
    TableModel tableModel = createEmployeeTableModel();
    tableModel.setLimit(6);
    tableModel.refresh();
    assertEquals(6, tableModel.getRowCount());
    ColumnConditionModel<?, Double> commissionConditionModel =
            tableModel.conditionModel().attributeModel(Employee.COMMISSION);
    commissionConditionModel.operator().set(Operator.EQUAL);
    commissionConditionModel.enabled().set(true);
    tableModel.refresh();
    commissionConditionModel.enabled().set(false);
    tableModel.refresh();
    assertEquals(6, tableModel.getRowCount());
    tableModel.setLimit(-1);
    tableModel.refresh();
    assertEquals(16, tableModel.getRowCount());
  }

  @Test
  public void setColumns() {
    TableModel empModel = createEmployeeTableModel();
    empModel.setVisibleColumns(Employee.COMMISSION, Employee.DEPARTMENT_FK, Employee.HIREDATE);
  }

  @Test
  public void conditionChangedListener() {
    TableModel empModel = createEmployeeTableModel();
    AtomicInteger counter = new AtomicInteger();
    Runnable conditionChangedListener = counter::incrementAndGet;
    empModel.conditionChanged().addListener(conditionChangedListener);
    ColumnConditionModel<? extends Attribute<Double>, Double> commissionModel =
            empModel.conditionModel().attributeModel(Employee.COMMISSION);
    commissionModel.enabled().set(true);
    assertEquals(1, counter.get());
    commissionModel.enabled().set(false);
    assertEquals(2, counter.get());
    commissionModel.operator().set(Operator.GREATER_THAN_OR_EQUAL);
    commissionModel.setLowerBound(1200d);
    //automatically set enabled when upper bound is set
    assertEquals(3, counter.get());
    empModel.conditionChanged().removeListener(conditionChangedListener);
  }

  @Test
  public void testSearchState() {
    TableModel empModel = createEmployeeTableModel();
    assertFalse(empModel.conditionChanged().get());
    ColumnConditionModel<? extends Attribute<String>, String> jobModel =
            empModel.conditionModel().attributeModel(Employee.JOB);
    jobModel.setEqualValue("job");
    assertTrue(empModel.conditionChanged().get());
    jobModel.enabled().set(false);
    assertFalse(empModel.conditionChanged().get());
    jobModel.enabled().set(true);
    assertTrue(empModel.conditionChanged().get());
    empModel.refresh();
    assertFalse(empModel.conditionChanged().get());
  }

  protected final EntityConnectionProvider connectionProvider() {
    return connectionProvider;
  }

  /**
   * @return a EntityTableModel using {@link #testEntities} with an edit model
   * @see Detail#TYPE
   */
  protected abstract TableModel createTestTableModel();

  /**
   * @return a EntityTableModel based on the master entity
   * @see TestDomain.Master#TYPE
   */
  protected abstract TableModel createMasterTableModel();

  protected abstract TableModel createDepartmentTableModel();

  protected abstract TableModel createEmployeeTableModel();

  protected abstract EditModel createDepartmentEditModel();

  protected abstract TableModel createDetailTableModel();

  protected abstract EditModel createDetailEditModel();

  private static List<Entity> initTestEntities(Entities entities) {
    List<Entity> testEntities = new ArrayList<>(5);
    String[] stringValues = new String[] {"a", "b", "c", "d", "e"};
    for (int i = 0; i < 5; i++) {
      testEntities.add(entities.builder(Detail.TYPE)
              .with(Detail.ID, (long) i + 1)
              .with(Detail.INT, i + 1)
              .with(Detail.STRING, stringValues[i])
              .build());
    }

    return testEntities;
  }
}
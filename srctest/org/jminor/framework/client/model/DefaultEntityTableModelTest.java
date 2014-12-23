/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.Util;
import org.jminor.common.model.table.ColumnSearchModel;
import org.jminor.common.model.table.SortingDirective;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.db.DefaultEntityConnectionTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityTestDomain;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;

import org.junit.Test;

import javax.swing.table.TableColumn;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public final class DefaultEntityTableModelTest {

  private final DefaultEntityTableModel testModel = new EntityTableModelTmp();

  static {
    EntityTestDomain.init();
    EmpDept.init();
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullSearchModel() {
    new DefaultEntityTableModel(EmpDept.T_EMPLOYEE, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void nonMatchingSearchModelEntityID() {
    final EntityTableSearchModel searchModel = new DefaultEntityTableSearchModel(EmpDept.T_DEPARTMENT, null);
    new DefaultEntityTableModel(EmpDept.T_EMPLOYEE, null, null, searchModel);
  }

  @Test
  public void setSelectedByPrimaryKeys() {
    final DefaultEntityTableModel tableModel = new DefaultEntityTableModel(EmpDept.T_EMPLOYEE, testModel.getConnectionProvider());
    tableModel.refresh();

    final Entity.Key pk1 = Entities.key(EmpDept.T_EMPLOYEE);
    pk1.setValue(EmpDept.EMPLOYEE_ID, 1);
    final Entity.Key pk2 = Entities.key(EmpDept.T_EMPLOYEE);
    pk2.setValue(EmpDept.EMPLOYEE_ID, 2);

    tableModel.setSelectedByPrimaryKeys(Arrays.asList(pk1));
    final Entity selectedPK1 = tableModel.getSelectionModel().getSelectedItem();
    assertEquals(pk1, selectedPK1.getPrimaryKey());
    assertEquals(1, tableModel.getSelectionModel().getSelectionCount());

    tableModel.setSelectedByPrimaryKeys(Arrays.asList(pk2));
    final Entity selectedPK2 = tableModel.getSelectionModel().getSelectedItem();
    assertEquals(pk2, selectedPK2.getPrimaryKey());
    assertEquals(1, tableModel.getSelectionModel().getSelectionCount());

    final List<Entity.Key> keys = Arrays.asList(pk1, pk2);
    tableModel.setSelectedByPrimaryKeys(keys);
    final List<Entity> selectedItems = tableModel.getSelectionModel().getSelectedItems();
    for (final Entity selected : selectedItems) {
      assertTrue(keys.contains(selected.getPrimaryKey()));
    }
    assertEquals(2, tableModel.getSelectionModel().getSelectionCount());
  }

  @Test
  public void getSelectedEntitiesIterator() {
    final DefaultEntityTableModel tableModel = new DefaultEntityTableModel(EmpDept.T_EMPLOYEE, testModel.getConnectionProvider());
    tableModel.refresh();

    tableModel.getSelectionModel().setSelectedIndexes(Arrays.asList(0, 3, 5));
    final Iterator<Entity> iterator = tableModel.getSelectedEntitiesIterator();
    assertEquals(tableModel.getItemAt(0), iterator.next());
    assertEquals(tableModel.getItemAt(3), iterator.next());
    assertEquals(tableModel.getItemAt(5), iterator.next());
  }

  @Test(expected = IllegalStateException.class)
  public void updateNoEditModel() throws CancelException, ValidationException, DatabaseException {
    final DefaultEntityTableModel tableModel = new DefaultEntityTableModel(EmpDept.T_EMPLOYEE, testModel.getConnectionProvider());
    tableModel.update(new ArrayList<Entity>());
  }

  @Test(expected = IllegalStateException.class)
  public void deleteSelectedNoEditModel() throws CancelException, DatabaseException {
    final DefaultEntityTableModel tableModel = new DefaultEntityTableModel(EmpDept.T_EMPLOYEE, testModel.getConnectionProvider());
    tableModel.refresh();
    tableModel.getSelectionModel().setSelectedIndex(0);
    tableModel.deleteSelected();
  }

  @Test
  public void addOnInsert() throws CancelException, DatabaseException, ValidationException {
    final DefaultEntityTableModel deptModel = new DefaultEntityTableModel(EmpDept.T_DEPARTMENT, testModel.getConnectionProvider());
    deptModel.setEditModel(new DefaultEntityEditModel(EmpDept.T_DEPARTMENT, testModel.getConnectionProvider()));
    deptModel.refresh();

    deptModel.setAddEntitiesOnInsert(true);
    final Entity dept = Entities.entity(EmpDept.T_DEPARTMENT);
    dept.setValue(EmpDept.DEPARTMENT_ID, -10);
    dept.setValue(EmpDept.DEPARTMENT_LOCATION, "Nowhere");
    dept.setValue(EmpDept.DEPARTMENT_NAME, "Noname");
    final int count = deptModel.getRowCount();
    deptModel.getEditModel().insert(Arrays.asList(dept));
    assertEquals(count + 1, deptModel.getRowCount());

    deptModel.setAddEntitiesOnInsert(false);
    final Entity dept2 = Entities.entity(EmpDept.T_DEPARTMENT);
    dept2.setValue(EmpDept.DEPARTMENT_ID, -20);
    dept2.setValue(EmpDept.DEPARTMENT_LOCATION, "Nowhere2");
    dept2.setValue(EmpDept.DEPARTMENT_NAME, "Noname2");
    deptModel.getEditModel().insert(Arrays.asList(dept2));
    assertEquals(count + 1, deptModel.getRowCount());

    deptModel.refresh();
    assertEquals(count + 2, deptModel.getRowCount());

    deptModel.getEditModel().delete(Arrays.asList(dept, dept2));
  }

  @Test
  public void removeOnDelete() throws CancelException, DatabaseException {
    final DefaultEntityTableModel tableModel = new DefaultEntityTableModel(EmpDept.T_EMPLOYEE, testModel.getConnectionProvider());
    tableModel.setEditModel(new DefaultEntityEditModel(EmpDept.T_EMPLOYEE, testModel.getConnectionProvider()));
    tableModel.refresh();

    final Entity.Key pk1 = Entities.key(EmpDept.T_EMPLOYEE);
    pk1.setValue(EmpDept.EMPLOYEE_ID, 1);
    final Entity.Key pk2 = Entities.key(EmpDept.T_EMPLOYEE);
    pk2.setValue(EmpDept.EMPLOYEE_ID, 2);
    try {
      tableModel.getConnectionProvider().getConnection().beginTransaction();
      tableModel.setSelectedByPrimaryKeys(Arrays.asList(pk1));
      tableModel.getSelectionModel().setSelectedIndex(0);
      Entity selected = tableModel.getSelectionModel().getSelectedItem();
      tableModel.setRemoveEntitiesOnDelete(true);
      assertTrue(tableModel.isRemoveEntitiesOnDelete());
      tableModel.deleteSelected();
      assertFalse(tableModel.contains(selected, false));

      tableModel.setSelectedByPrimaryKeys(Arrays.asList(pk2));
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
  public void getEntityByPrimaryKey() {
    final DefaultEntityTableModel tableModel = new DefaultEntityTableModel(EmpDept.T_EMPLOYEE, testModel.getConnectionProvider());
    tableModel.refresh();

    final Entity.Key pk1 = Entities.key(EmpDept.T_EMPLOYEE);
    pk1.setValue(EmpDept.EMPLOYEE_ID, 1);
    assertNotNull(tableModel.getEntityByPrimaryKey(pk1));

    final Entity.Key pk2 = Entities.key(EmpDept.T_EMPLOYEE);
    pk2.setValue(EmpDept.EMPLOYEE_ID, -66);
    assertNull(tableModel.getEntityByPrimaryKey(pk2));
  }

  @Test
  public void indexOf() {
    final DefaultEntityTableModel tableModel = new DefaultEntityTableModel(EmpDept.T_EMPLOYEE, testModel.getConnectionProvider());
    tableModel.refresh();
    tableModel.setSortingDirective(EmpDept.EMPLOYEE_NAME, SortingDirective.ASCENDING, false);
    assertEquals(SortingDirective.ASCENDING, tableModel.getSortingDirective(EmpDept.EMPLOYEE_NAME));

    final Entity.Key pk1 = Entities.key(EmpDept.T_EMPLOYEE);
    pk1.setValue(EmpDept.EMPLOYEE_ID, 10);//ADAMS
    assertEquals(0, tableModel.indexOf(pk1));

    final Entity.Key pk2 = Entities.key(EmpDept.T_EMPLOYEE);
    pk2.setValue(EmpDept.EMPLOYEE_ID, -66);
    assertEquals(-1, tableModel.indexOf(pk2));
  }

  @Test(expected = IllegalArgumentException.class)
  public void setEditModelNullValue() {
    final EntityTableModel tableModel = new DefaultEntityTableModel(EntityTestDomain.T_DETAIL, DefaultEntityConnectionTest.CONNECTION_PROVIDER);
    tableModel.setEditModel(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setEditModelWrongEntityID() {
    final EntityTableModel tableModel = new DefaultEntityTableModel(EntityTestDomain.T_DETAIL, DefaultEntityConnectionTest.CONNECTION_PROVIDER);
    final EntityEditModel editModel = new DefaultEntityEditModel(EntityTestDomain.T_MASTER, DefaultEntityConnectionTest.CONNECTION_PROVIDER);
    tableModel.setEditModel(editModel);
  }

  @Test(expected = IllegalStateException.class)
  public void setEditModelAlreadySet() {
    assertTrue(testModel.hasEditModel());
    final EntityEditModel editModel = new DefaultEntityEditModel(EntityTestDomain.T_DETAIL, DefaultEntityConnectionTest.CONNECTION_PROVIDER);
    testModel.setEditModel(editModel);
  }

  @Test
  public void setAndGetEditModel() {
    final EntityTableModel tableModel = new DefaultEntityTableModel(EntityTestDomain.T_DETAIL, DefaultEntityConnectionTest.CONNECTION_PROVIDER);
    final EntityEditModel editModel = new DefaultEntityEditModel(EntityTestDomain.T_DETAIL, DefaultEntityConnectionTest.CONNECTION_PROVIDER);
    assertFalse(tableModel.hasEditModel());
    tableModel.setEditModel(editModel);
    assertTrue(tableModel.hasEditModel());
    assertEquals(editModel, tableModel.getEditModel());
  }

  @Test(expected = IllegalStateException.class)
  public void getEditModelNoEditModelSet() {
    final EntityTableModel tableModel = new DefaultEntityTableModel(EntityTestDomain.T_DETAIL, DefaultEntityConnectionTest.CONNECTION_PROVIDER);
    tableModel.getEditModel();
  }

  @Test
  public void isUpdateAllowed() {
    final EntityTableModel tableModel = new DefaultEntityTableModel(EntityTestDomain.T_DETAIL, DefaultEntityConnectionTest.CONNECTION_PROVIDER);
    final EntityEditModel editModel = new DefaultEntityEditModel(EntityTestDomain.T_DETAIL, DefaultEntityConnectionTest.CONNECTION_PROVIDER);
    assertFalse(tableModel.isUpdateAllowed());
    tableModel.setEditModel(editModel);
    assertTrue(tableModel.isUpdateAllowed());
    editModel.setUpdateAllowed(false);
    assertFalse(tableModel.isUpdateAllowed());
  }

  @Test
  public void isDeleteAllowed() {
    final EntityTableModel tableModel = new DefaultEntityTableModel(EntityTestDomain.T_DETAIL, DefaultEntityConnectionTest.CONNECTION_PROVIDER);
    final EntityEditModel editModel = new DefaultEntityEditModel(EntityTestDomain.T_DETAIL, DefaultEntityConnectionTest.CONNECTION_PROVIDER);
    assertFalse(tableModel.isDeleteAllowed());
    tableModel.setEditModel(editModel);
    assertTrue(tableModel.isDeleteAllowed());
    editModel.setDeleteAllowed(false);
    assertFalse(tableModel.isDeleteAllowed());
  }

  @Test
  public void getEntityID() {
    assertEquals(EntityTestDomain.T_DETAIL, testModel.getEntityID());
  }

  @Test
  public void getValueAt() {
    testModel.refresh();
    assertEquals(1, testModel.getValueAt(0, 0));
    assertEquals(2, testModel.getValueAt(1, 0));
    assertEquals(3, testModel.getValueAt(2, 0));
    assertEquals(4, testModel.getValueAt(3, 0));
    assertEquals(5, testModel.getValueAt(4, 0));
    assertEquals("a", testModel.getValueAt(0, 2));
    assertEquals("b", testModel.getValueAt(1, 2));
    assertEquals("c", testModel.getValueAt(2, 2));
    assertEquals("d", testModel.getValueAt(3, 2));
    assertEquals("e", testModel.getValueAt(4, 2));
  }

  @Test
  public void testFiltering() {
    testModel.refresh();
    final ColumnSearchModel<Property> filterModel = testModel.getSearchModel().getPropertyFilterModel(EntityTestDomain.DETAIL_STRING);
    filterModel.setLikeValue("a");
    testModel.filterContents();
  }

  @Test
  public void getPropertyColumnIndex() {
    assertEquals(0, testModel.getPropertyColumnIndex(EntityTestDomain.DETAIL_INT));
    assertEquals(1, testModel.getPropertyColumnIndex(EntityTestDomain.DETAIL_DOUBLE));
    assertEquals(2, testModel.getPropertyColumnIndex(EntityTestDomain.DETAIL_STRING));
    assertEquals(3, testModel.getPropertyColumnIndex(EntityTestDomain.DETAIL_DATE));
    assertEquals(4, testModel.getPropertyColumnIndex(EntityTestDomain.DETAIL_TIMESTAMP));
    assertEquals(5, testModel.getPropertyColumnIndex(EntityTestDomain.DETAIL_BOOLEAN));
    assertEquals(6, testModel.getPropertyColumnIndex(EntityTestDomain.DETAIL_BOOLEAN_NULLABLE));
    assertEquals(7, testModel.getPropertyColumnIndex(EntityTestDomain.DETAIL_ENTITY_FK));
    assertEquals(8, testModel.getPropertyColumnIndex(EntityTestDomain.DETAIL_MASTER_NAME));
    assertEquals(9, testModel.getPropertyColumnIndex(EntityTestDomain.DETAIL_MASTER_CODE));
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
    entity.setValue(EntityTestDomain.DETAIL_STRING, "hello");
    testModel.update(Arrays.asList(entity));
  }

  @Test(expected = IllegalStateException.class)
  public void batchUpdateNotAllowed() throws DatabaseException, CancelException, ValidationException {
    testModel.setBatchUpdateAllowed(false);
    assertFalse(testModel.isBatchUpdateAllowed());
    testModel.refresh();
    testModel.getSelectionModel().setSelectedIndexes(Arrays.asList(0, 1));
    final List<Entity> entities = testModel.getSelectionModel().getSelectedItems();
    EntityUtil.setPropertyValue(EntityTestDomain.DETAIL_STRING, "hello", entities);
    testModel.update(entities);
  }

  @Test
  public void testTheRest() {
    assertEquals(DefaultEntityConnectionTest.CONNECTION_PROVIDER, testModel.getConnectionProvider());
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
    assertFalse(testModel.isCellEditable(0, 0));
  }

  @Test
  public void getColumnClass() {
    assertEquals(Integer.class, testModel.getColumnClass(0));
    assertEquals(Double.class, testModel.getColumnClass(1));
    assertEquals(String.class, testModel.getColumnClass(2));
    assertEquals(Date.class, testModel.getColumnClass(3));
    assertEquals(Timestamp.class, testModel.getColumnClass(4));
    assertEquals(Boolean.class, testModel.getColumnClass(5));
    assertEquals(Boolean.class, testModel.getColumnClass(6));
    assertEquals(Object.class, testModel.getColumnClass(7));
  }

  @Test
  public void columnModel() {
    final Property property = Entities.getProperty(EntityTestDomain.T_DETAIL, EntityTestDomain.DETAIL_STRING);
    final TableColumn column = testModel.getColumnModel().getTableColumn(property);
    assertEquals(property, column.getIdentifier());
  }

  @Test
  public void getValues() {
    testModel.refresh();
    final Property property = Entities.getProperty(EntityTestDomain.T_DETAIL, EntityTestDomain.DETAIL_STRING);
    final Collection values = testModel.getValues(property, false);
    assertEquals(5, values.size());
    assertTrue(values.contains("a"));
    assertTrue(values.contains("b"));
    assertTrue(values.contains("c"));
    assertTrue(values.contains("d"));
    assertTrue(values.contains("e"));
    assertFalse(values.contains("zz"));
  }

  @Test
  public void testSortComparator() {
    final Property masterFKProperty = Entities.getProperty(EntityTestDomain.T_DETAIL, EntityTestDomain.DETAIL_ENTITY_FK);
    final Comparator comparator = ((DefaultEntityTableModel.DefaultEntityTableSortModel) testModel.getSortModel()).initializeColumnComparator(masterFKProperty);
    assertEquals(comparator, Entities.getComparator(EntityTestDomain.T_MASTER));
  }

  @Test
  public void getEntitiesByPropertyValues() {
    testModel.refresh();
    final Map<String, Object> propValues = new HashMap<>();
    propValues.put(EntityTestDomain.DETAIL_STRING, "b");
    assertEquals(1, testModel.getEntitiesByPropertyValues(propValues).size());
    propValues.put(EntityTestDomain.DETAIL_STRING, "zz");
    assertTrue(testModel.getEntitiesByPropertyValues(propValues).isEmpty());
  }

  @Test
  public void getEntitiesByPrimaryKeys() {
    testModel.refresh();
    Entity tmpEnt = Entities.entity(EntityTestDomain.T_DETAIL);
    tmpEnt.setValue(EntityTestDomain.DETAIL_ID, 3);
    assertEquals("c", testModel.getEntityByPrimaryKey(tmpEnt.getPrimaryKey()).getValue(EntityTestDomain.DETAIL_STRING));
    final List<Entity.Key> keys = new ArrayList<>();
    keys.add(tmpEnt.getPrimaryKey());
    tmpEnt = Entities.entity(EntityTestDomain.T_DETAIL);
    tmpEnt.setValue(EntityTestDomain.DETAIL_ID, 2);
    keys.add(tmpEnt.getPrimaryKey());
    tmpEnt = Entities.entity(EntityTestDomain.T_DETAIL);
    tmpEnt.setValue(EntityTestDomain.DETAIL_ID, 1);
    keys.add(tmpEnt.getPrimaryKey());

    final Collection<Entity> entities = testModel.getEntitiesByPrimaryKeys(keys);
    assertEquals(3, entities.size());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void setValueAt() {
    testModel.setValueAt("hello", 0, 0);
  }

  @Test(expected = IllegalStateException.class)
  public void noVisibleColumns() {
    new DefaultEntityTableModel(EntityTestDomain.T_MASTER, DefaultEntityConnectionTest.CONNECTION_PROVIDER);
  }

  @Test
  public void preferences() throws Exception {
    testModel.clearPreferences();

    final EntityTableModelTmp tableModel = new EntityTableModelTmp();
    assertTrue(tableModel.getColumnModel().isColumnVisible(Entities.getColumnProperty(EntityTestDomain.T_DETAIL, EntityTestDomain.DETAIL_STRING)));

    tableModel.getColumnModel().setColumnVisible(Entities.getColumnProperty(EntityTestDomain.T_DETAIL, EntityTestDomain.DETAIL_STRING), false);
    tableModel.getColumnModel().moveColumn(1, 0);//double to 0, int to 1
    TableColumn column = tableModel.getColumnModel().getColumn(3);
    column.setWidth(150);//timestamp
    column = tableModel.getColumnModel().getColumn(5);
    column.setWidth(170);//entity_ref

    tableModel.savePreferences();

    final EntityTableModelTmp model = new EntityTableModelTmp();
    assertFalse(model.getColumnModel().isColumnVisible(Entities.getColumnProperty(EntityTestDomain.T_DETAIL, EntityTestDomain.DETAIL_STRING)));
    assertTrue(model.getPropertyColumnIndex(EntityTestDomain.DETAIL_DOUBLE) == 0);
    assertTrue(model.getPropertyColumnIndex(EntityTestDomain.DETAIL_INT) == 1);
    column = model.getColumnModel().getColumn(3);
    assertEquals(150, column.getPreferredWidth());
    column = model.getColumnModel().getColumn(5);
    assertEquals(170, column.getPreferredWidth());

    model.clearPreferences();
    Util.flushUserPreferences();
  }

  public static final class EntityTableModelTmp extends DefaultEntityTableModel {

    private final Entity[] entities = initTestEntities(new Entity[5]);

    public EntityTableModelTmp() {
      super(EntityTestDomain.T_DETAIL, DefaultEntityConnectionTest.CONNECTION_PROVIDER);
      setEditModel(new DefaultEntityEditModel(EntityTestDomain.T_DETAIL, DefaultEntityConnectionTest.CONNECTION_PROVIDER));
    }
    @Override
    protected List<Entity> performQuery(final Criteria criteria) {
      return Arrays.asList(entities);
    }
  }

  private static Entity[] initTestEntities(final Entity[] testEntities) {
    final String[] stringValues = new String[]{"a", "b", "c", "d", "e"};
    for (int i = 0; i < testEntities.length; i++) {
      testEntities[i] = Entities.entity(EntityTestDomain.T_DETAIL);
      testEntities[i].setValue(EntityTestDomain.DETAIL_ID, i+1);
      testEntities[i].setValue(EntityTestDomain.DETAIL_INT, i+1);
      testEntities[i].setValue(EntityTestDomain.DETAIL_STRING, stringValues[i]);
    }

    return testEntities;
  }
}
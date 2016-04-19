/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.Util;
import org.jminor.common.model.table.ColumnCriteriaModel;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.db.EntityConnectionProvidersTest;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.TestDomain;
import org.jminor.framework.model.DefaultEntityTableCriteriaModel;
import org.jminor.framework.model.EntityEditModel;
import org.jminor.framework.model.EntityTableCriteriaModel;
import org.jminor.framework.model.EntityTableModel;
import org.jminor.swing.common.model.table.SortingDirective;

import org.junit.Test;

import javax.swing.table.TableColumn;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
    TestDomain.init();
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullCriteriaModel() {
    new DefaultEntityTableModel(TestDomain.T_EMP, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void nonMatchingCriteriaModelEntityID() {
    final EntityTableCriteriaModel criteriaModel = new DefaultEntityTableCriteriaModel(TestDomain.T_DEPARTMENT, null,
            new DefaultPropertyFilterModelProvider(), new DefaultPropertyCriteriaModelProvider());
    new DefaultEntityTableModel(TestDomain.T_EMP, null, null, criteriaModel);
  }

  @Test
  public void setSelectedByKey() {
    final DefaultEntityTableModel tableModel = new DefaultEntityTableModel(TestDomain.T_EMP, testModel.getConnectionProvider());
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
    final DefaultEntityTableModel tableModel = new DefaultEntityTableModel(TestDomain.T_EMP, testModel.getConnectionProvider());
    tableModel.refresh();

    tableModel.getSelectionModel().setSelectedIndexes(Arrays.asList(0, 3, 5));
    final Iterator<Entity> iterator = tableModel.getSelectedEntitiesIterator();
    assertEquals(tableModel.getItemAt(0), iterator.next());
    assertEquals(tableModel.getItemAt(3), iterator.next());
    assertEquals(tableModel.getItemAt(5), iterator.next());
  }

  @Test(expected = IllegalStateException.class)
  public void updateNoEditModel() throws CancelException, ValidationException, DatabaseException {
    final DefaultEntityTableModel tableModel = new DefaultEntityTableModel(TestDomain.T_EMP, testModel.getConnectionProvider());
    tableModel.update(new ArrayList<Entity>());
  }

  @Test(expected = IllegalStateException.class)
  public void deleteSelectedNoEditModel() throws CancelException, DatabaseException {
    final DefaultEntityTableModel tableModel = new DefaultEntityTableModel(TestDomain.T_EMP, testModel.getConnectionProvider());
    tableModel.refresh();
    tableModel.getSelectionModel().setSelectedIndex(0);
    tableModel.deleteSelected();
  }

  @Test
  public void addOnInsert() throws CancelException, DatabaseException, ValidationException {
    final DefaultEntityTableModel deptModel = new DefaultEntityTableModel(TestDomain.T_DEPARTMENT, testModel.getConnectionProvider());
    deptModel.setEditModel(new SwingEntityEditModel(TestDomain.T_DEPARTMENT, testModel.getConnectionProvider()));
    deptModel.refresh();

    deptModel.setInsertAction(EntityTableModel.InsertAction.ADD_BOTTOM);
    final Entity dept = Entities.entity(TestDomain.T_DEPARTMENT);
    dept.put(TestDomain.DEPARTMENT_ID, -10);
    dept.put(TestDomain.DEPARTMENT_LOCATION, "Nowhere");
    dept.put(TestDomain.DEPARTMENT_NAME, "Noname");
    final int count = deptModel.getRowCount();
    deptModel.getEditModel().insert(Collections.singletonList(dept));
    assertEquals(count + 1, deptModel.getRowCount());
    assertEquals(dept, deptModel.getItemAt(deptModel.getRowCount() - 1));

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
    final DefaultEntityTableModel tableModel = new DefaultEntityTableModel(TestDomain.T_EMP, testModel.getConnectionProvider());
    tableModel.setEditModel(new SwingEntityEditModel(TestDomain.T_EMP, testModel.getConnectionProvider()));
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
    final DefaultEntityTableModel tableModel = new DefaultEntityTableModel(TestDomain.T_EMP, testModel.getConnectionProvider());
    tableModel.refresh();

    final Entity.Key pk1 = Entities.key(TestDomain.T_EMP);
    pk1.put(TestDomain.EMP_ID, 1);
    assertNotNull(tableModel.getEntityByKey(pk1));

    final Entity.Key pk2 = Entities.key(TestDomain.T_EMP);
    pk2.put(TestDomain.EMP_ID, -66);
    assertNull(tableModel.getEntityByKey(pk2));
  }

  @Test
  public void indexOf() {
    final DefaultEntityTableModel tableModel = new DefaultEntityTableModel(TestDomain.T_EMP, testModel.getConnectionProvider());
    tableModel.refresh();
    tableModel.getSortModel().setSortingDirective(Entities.getProperty(TestDomain.T_EMP, TestDomain.EMP_NAME),
            SortingDirective.ASCENDING, false);
    assertEquals(SortingDirective.ASCENDING, tableModel.getSortModel()
            .getSortingDirective(Entities.getProperty(TestDomain.T_EMP, TestDomain.EMP_NAME)));

    final Entity.Key pk1 = Entities.key(TestDomain.T_EMP);
    pk1.put(TestDomain.EMP_ID, 10);//ADAMS
    assertEquals(0, tableModel.indexOf(pk1));

    final Entity.Key pk2 = Entities.key(TestDomain.T_EMP);
    pk2.put(TestDomain.EMP_ID, -66);
    assertEquals(-1, tableModel.indexOf(pk2));
  }

  @Test(expected = IllegalArgumentException.class)
  public void setEditModelNullValue() {
    final EntityTableModel tableModel = new DefaultEntityTableModel(TestDomain.T_DETAIL, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    tableModel.setEditModel(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setEditModelWrongEntityID() {
    final EntityTableModel tableModel = new DefaultEntityTableModel(TestDomain.T_DETAIL, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    final EntityEditModel editModel = new SwingEntityEditModel(TestDomain.T_MASTER,
            EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    tableModel.setEditModel(editModel);
  }

  @Test(expected = IllegalStateException.class)
  public void setEditModelAlreadySet() {
    assertTrue(testModel.hasEditModel());
    final EntityEditModel editModel = new SwingEntityEditModel(TestDomain.T_DETAIL,
            EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    testModel.setEditModel(editModel);
  }

  @Test
  public void setAndGetEditModel() {
    final EntityTableModel tableModel = new DefaultEntityTableModel(TestDomain.T_DETAIL, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    final EntityEditModel editModel = new SwingEntityEditModel(TestDomain.T_DETAIL,
            EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    assertFalse(tableModel.hasEditModel());
    tableModel.setEditModel(editModel);
    assertTrue(tableModel.hasEditModel());
    assertEquals(editModel, tableModel.getEditModel());
  }

  @Test(expected = IllegalStateException.class)
  public void getEditModelNoEditModelSet() {
    final EntityTableModel tableModel = new DefaultEntityTableModel(TestDomain.T_DETAIL, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    tableModel.getEditModel();
  }

  @Test
  public void isUpdateAllowed() {
    final EntityTableModel tableModel = new DefaultEntityTableModel(TestDomain.T_DETAIL,
            EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    final EntityEditModel editModel = new SwingEntityEditModel(TestDomain.T_DETAIL,
            EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    assertFalse(tableModel.isUpdateAllowed());
    tableModel.setEditModel(editModel);
    assertTrue(tableModel.isUpdateAllowed());
    editModel.setUpdateAllowed(false);
    assertFalse(tableModel.isUpdateAllowed());
  }

  @Test
  public void isDeleteAllowed() {
    final EntityTableModel tableModel = new DefaultEntityTableModel(TestDomain.T_DETAIL,
            EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    final EntityEditModel editModel = new SwingEntityEditModel(TestDomain.T_DETAIL,
            EntityConnectionProvidersTest.CONNECTION_PROVIDER);
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
    final ColumnCriteriaModel<Property> filterModel = testModel.getCriteriaModel().getPropertyFilterModel(TestDomain.DETAIL_STRING);
    filterModel.setLikeValue("a");
    testModel.filterContents();
  }

  @Test
  public void getPropertyColumnIndex() {
    assertEquals(0, testModel.getPropertyColumnIndex(TestDomain.DETAIL_INT));
    assertEquals(1, testModel.getPropertyColumnIndex(TestDomain.DETAIL_DOUBLE));
    assertEquals(2, testModel.getPropertyColumnIndex(TestDomain.DETAIL_STRING));
    assertEquals(3, testModel.getPropertyColumnIndex(TestDomain.DETAIL_DATE));
    assertEquals(4, testModel.getPropertyColumnIndex(TestDomain.DETAIL_TIMESTAMP));
    assertEquals(5, testModel.getPropertyColumnIndex(TestDomain.DETAIL_BOOLEAN));
    assertEquals(6, testModel.getPropertyColumnIndex(TestDomain.DETAIL_BOOLEAN_NULLABLE));
    assertEquals(7, testModel.getPropertyColumnIndex(TestDomain.DETAIL_ENTITY_FK));
    assertEquals(8, testModel.getPropertyColumnIndex(TestDomain.DETAIL_MASTER_NAME));
    assertEquals(9, testModel.getPropertyColumnIndex(TestDomain.DETAIL_MASTER_CODE));
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
    final Property property = Entities.getProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_STRING);
    final TableColumn column = testModel.getColumnModel().getTableColumn(property);
    assertEquals(property, column.getIdentifier());
  }

  @Test
  public void getValues() {
    testModel.refresh();
    final Property property = Entities.getProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_STRING);
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
    final Property masterFKProperty = Entities.getProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_ENTITY_FK);
    final Comparator comparator = ((DefaultEntityTableModel.DefaultEntityTableSortModel) testModel.getSortModel()).initializeColumnComparator(masterFKProperty);
    assertEquals(comparator, Entities.getComparator(TestDomain.T_MASTER));
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

  @Test(expected = UnsupportedOperationException.class)
  public void setValueAt() {
    testModel.setValueAt("hello", 0, 0);
  }

  @Test(expected = IllegalStateException.class)
  public void noVisibleColumns() {
    new DefaultEntityTableModel(TestDomain.T_MASTER, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
  }

  @Test
  public void preferences() throws Exception {
    testModel.clearPreferences();

    final EntityTableModelTmp tableModel = new EntityTableModelTmp();
    assertTrue(tableModel.getColumnModel().isColumnVisible(Entities.getColumnProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_STRING)));

    tableModel.getColumnModel().setColumnVisible(Entities.getColumnProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_STRING), false);
    tableModel.getColumnModel().moveColumn(1, 0);//double to 0, int to 1
    TableColumn column = tableModel.getColumnModel().getColumn(3);
    column.setWidth(150);//timestamp
    column = tableModel.getColumnModel().getColumn(5);
    column.setWidth(170);//entity_ref

    tableModel.savePreferences();

    final EntityTableModelTmp model = new EntityTableModelTmp();
    assertFalse(model.getColumnModel().isColumnVisible(Entities.getColumnProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_STRING)));
    assertTrue(model.getPropertyColumnIndex(TestDomain.DETAIL_DOUBLE) == 0);
    assertTrue(model.getPropertyColumnIndex(TestDomain.DETAIL_INT) == 1);
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
      super(TestDomain.T_DETAIL, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
      setEditModel(new SwingEntityEditModel(TestDomain.T_DETAIL, EntityConnectionProvidersTest.CONNECTION_PROVIDER));
    }
    @Override
    protected List<Entity> performQuery(final Criteria criteria) {
      return Arrays.asList(entities);
    }
  }

  private static Entity[] initTestEntities(final Entity[] testEntities) {
    final String[] stringValues = new String[]{"a", "b", "c", "d", "e"};
    for (int i = 0; i < testEntities.length; i++) {
      testEntities[i] = Entities.entity(TestDomain.T_DETAIL);
      testEntities[i].put(TestDomain.DETAIL_ID, (long) i+1);
      testEntities[i].put(TestDomain.DETAIL_INT, i+1);
      testEntities[i].put(TestDomain.DETAIL_STRING, stringValues[i]);
    }

    return testEntities;
  }
}
/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.ColumnSearchModel;
import org.jminor.common.model.reports.ReportDataWrapper;
import org.jminor.framework.db.EntityConnectionImplTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityTestDomain;
import org.jminor.framework.domain.Property;

import org.junit.Test;

import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public final class DefaultEntityTableModelTest {

  private static final Entity[] testEntities;

  private final DefaultEntityTableModel testModel = new EntityTableModelTmp();

  static {
    EntityTestDomain.init();
    EmpDept.init();
    testEntities = initTestEntities(new Entity[5]);
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullSearchModel() {
    new DefaultEntityTableModel(EmpDept.T_EMPLOYEE, null, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void nonMatchingSearchModelEntityID() {
    final EntityTableSearchModel searchModel = new DefaultEntityTableSearchModel(EmpDept.T_DEPARTMENT, null);
    new DefaultEntityTableModel(EmpDept.T_EMPLOYEE, null, searchModel);
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
    final Entity selectedPK1 = tableModel.getSelectedItem();
    assertEquals(pk1, selectedPK1.getPrimaryKey());
    assertEquals(1, tableModel.getSelectionCount());

    tableModel.setSelectedByPrimaryKeys(Arrays.asList(pk2));
    final Entity selectedPK2 = tableModel.getSelectedItem();
    assertEquals(pk2, selectedPK2.getPrimaryKey());
    assertEquals(1, tableModel.getSelectionCount());

    final List<Entity.Key> keys = Arrays.asList(pk1, pk2);
    tableModel.setSelectedByPrimaryKeys(keys);
    final List<Entity> selectedItems = tableModel.getSelectedItems();
    for (final Entity selected : selectedItems) {
      assertTrue(keys.contains(selected.getPrimaryKey()));
    }
    assertEquals(2, tableModel.getSelectionCount());
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
      tableModel.setSelectedItemIndex(0);
      Entity selected = tableModel.getSelectedItem();
      tableModel.setRemoveItemsOnDelete(true);
      tableModel.deleteSelected();
      assertFalse(tableModel.contains(selected, false));

      tableModel.setSelectedByPrimaryKeys(Arrays.asList(pk2));
      selected = tableModel.getSelectedItem();
      tableModel.setRemoveItemsOnDelete(false);
      tableModel.deleteSelected();
      assertTrue(tableModel.contains(selected, false));
    }
    finally {
      tableModel.getConnectionProvider().getConnection().rollbackTransaction();
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void setEditModelNullValue() {
    final EntityTableModel tableModel = new DefaultEntityTableModel(EntityTestDomain.T_DETAIL, EntityConnectionImplTest.DB_PROVIDER);
    tableModel.setEditModel(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setEditModelWrongEntityID() {
    final EntityTableModel tableModel = new DefaultEntityTableModel(EntityTestDomain.T_DETAIL, EntityConnectionImplTest.DB_PROVIDER);
    final EntityEditModel editModel = new DefaultEntityEditModel(EntityTestDomain.T_MASTER, EntityConnectionImplTest.DB_PROVIDER);
    tableModel.setEditModel(editModel);
  }

  @Test(expected = IllegalStateException.class)
  public void setEditModelAlreadySet() {
    assertTrue(testModel.hasEditModel());
    final EntityEditModel editModel = new DefaultEntityEditModel(EntityTestDomain.T_DETAIL, EntityConnectionImplTest.DB_PROVIDER);
    testModel.setEditModel(editModel);
  }

  @Test
  public void setAndGetEditModel() {
    final EntityTableModel tableModel = new DefaultEntityTableModel(EntityTestDomain.T_DETAIL, EntityConnectionImplTest.DB_PROVIDER);
    final EntityEditModel editModel = new DefaultEntityEditModel(EntityTestDomain.T_DETAIL, EntityConnectionImplTest.DB_PROVIDER);
    assertFalse(tableModel.hasEditModel());
    tableModel.setEditModel(editModel);
    assertTrue(tableModel.hasEditModel());
    assertEquals(editModel, tableModel.getEditModel());
  }

  @Test(expected = IllegalStateException.class)
  public void getEditModelNoEditModelSet() {
    final EntityTableModel tableModel = new DefaultEntityTableModel(EntityTestDomain.T_DETAIL, EntityConnectionImplTest.DB_PROVIDER);
    tableModel.getEditModel();
  }

  @Test
  public void isUpdateAllowed() {
    final EntityTableModel tableModel = new DefaultEntityTableModel(EntityTestDomain.T_DETAIL, EntityConnectionImplTest.DB_PROVIDER);
    final EntityEditModel editModel = new DefaultEntityEditModel(EntityTestDomain.T_DETAIL, EntityConnectionImplTest.DB_PROVIDER);
    assertFalse(tableModel.isUpdateAllowed());
    tableModel.setEditModel(editModel);
    assertTrue(tableModel.isUpdateAllowed());
    editModel.setUpdateAllowed(false);
    assertFalse(tableModel.isUpdateAllowed());
  }

  @Test
  public void isDeleteAllowed() {
    final EntityTableModel tableModel = new DefaultEntityTableModel(EntityTestDomain.T_DETAIL, EntityConnectionImplTest.DB_PROVIDER);
    final EntityEditModel editModel = new DefaultEntityEditModel(EntityTestDomain.T_DETAIL, EntityConnectionImplTest.DB_PROVIDER);
    assertFalse(tableModel.isDeleteAllowed());
    tableModel.setEditModel(editModel);
    assertTrue(tableModel.isDeleteAllowed());
    editModel.setDeleteAllowed(false);
    assertFalse(tableModel.isDeleteAllowed());
  }

  @Test
  public void settersAndGetters() {
    assertEquals(EntityConnectionImplTest.DB_PROVIDER, testModel.getConnectionProvider());
    assertEquals(EntityTestDomain.T_DETAIL, testModel.getEntityID());
    testModel.setDetailModel(false);
    assertFalse(testModel.isDetailModel());
    testModel.setQueryCriteriaRequired(false);
    assertFalse(testModel.isQueryCriteriaRequired());
    testModel.setQueryConfigurationAllowed(false);
    assertFalse(testModel.isQueryConfigurationAllowed());
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
    assertEquals(6, testModel.getPropertyColumnIndex(EntityTestDomain.DETAIL_ENTITY_FK));
    assertEquals(7, testModel.getPropertyColumnIndex(EntityTestDomain.DETAIL_MASTER_NAME));
    assertEquals(8, testModel.getPropertyColumnIndex(EntityTestDomain.DETAIL_MASTER_CODE));
  }

  @Test
  public void testTheRest() {
    final List<Property> columnProperties = testModel.getTableColumnProperties();
    assertEquals(testModel.getColumnCount(), columnProperties.size());
    testModel.setQueryConfigurationAllowed(false);
    assertFalse(testModel.isQueryConfigurationAllowed());
    testModel.setFetchCount(10);
    assertEquals(10, testModel.getFetchCount());
    assertFalse(testModel.isDetailModel());
    assertNotNull(testModel.getEditModel());
    assertFalse(testModel.isReadOnly());
    testModel.setBatchUpdateAllowed(true).setQueryConfigurationAllowed(true);
    assertTrue(testModel.isBatchUpdateAllowed());
    assertTrue(testModel.isDeleteAllowed());
    assertTrue(testModel.isQueryConfigurationAllowed());
    testModel.refresh();
    assertFalse(testModel.isCellEditable(0,0));

    final ReportDataWrapper wrapper = new ReportDataWrapper() {
      public Object getDataSource() {
        return null;
      }
    };
    testModel.setReportDataSource(wrapper);
    assertNotNull(testModel.getReportDataSource());

    assertEquals(Integer.class, testModel.getColumnClass(0));

    final Property property = Entities.getProperty(EntityTestDomain.T_DETAIL, EntityTestDomain.DETAIL_STRING);
    final TableColumn column = testModel.getTableColumn(property);
    assertEquals(property, column.getIdentifier());

    final Collection<Object> values = testModel.getValues(property, false);
    assertEquals(5, values.size());
    assertTrue(values.contains("a"));
    assertTrue(values.contains("b"));
    assertTrue(values.contains("c"));
    assertTrue(values.contains("d"));
    assertTrue(values.contains("e"));

    Entity tmpEnt = Entities.entity(EntityTestDomain.T_DETAIL);
    tmpEnt.setValue(EntityTestDomain.DETAIL_ID, 3);
    assertEquals("c", testModel.getEntityByPrimaryKey(tmpEnt.getPrimaryKey()).getValue(EntityTestDomain.DETAIL_STRING));
    final List<Entity.Key> keys = new ArrayList<Entity.Key>();
    keys.add(tmpEnt.getPrimaryKey());
    tmpEnt = Entities.entity(EntityTestDomain.T_DETAIL);
    tmpEnt.setValue(EntityTestDomain.DETAIL_ID, 2);
    keys.add(tmpEnt.getPrimaryKey());
    tmpEnt = Entities.entity(EntityTestDomain.T_DETAIL);
    tmpEnt.setValue(EntityTestDomain.DETAIL_ID, 1);
    keys.add(tmpEnt.getPrimaryKey());

    final List<Entity> entities = testModel.getEntitiesByPrimaryKeys(keys);
    assertEquals(3, entities.size());

    final Map<String, Object> propValues = new HashMap<String, Object>();
    propValues.put(EntityTestDomain.DETAIL_STRING, "b");
    final Collection<Entity> byPropertyValues = testModel.getEntitiesByPropertyValues(propValues);
    assertEquals(1, byPropertyValues.size());

    try {
      testModel.setValueAt("hello", 0, 0);
      fail();
    }
    catch (Exception e) {}
  }

  public static final class EntityTableModelTmp extends DefaultEntityTableModel {
    public EntityTableModelTmp() {
      super(EntityTestDomain.T_DETAIL, EntityConnectionImplTest.DB_PROVIDER);
      setEditModel(new DefaultEntityEditModel(EntityTestDomain.T_DETAIL, EntityConnectionImplTest.DB_PROVIDER));
    }
    @Override
    protected List<Entity> performQuery(final Criteria criteria) {
      return Arrays.asList(testEntities);
    }
  }

  private static Entity[] initTestEntities(final Entity[] testEntities) {
    for (int i = 0; i < testEntities.length; i++) {
      testEntities[i] = Entities.entity(EntityTestDomain.T_DETAIL);
      testEntities[i].setValue(EntityTestDomain.DETAIL_ID, i+1);
      testEntities[i].setValue(EntityTestDomain.DETAIL_INT, i+1);
      testEntities[i].setValue(EntityTestDomain.DETAIL_STRING, new String[]{"a", "b", "c", "d", "e"}[i]);
    }

    return testEntities;
  }
}
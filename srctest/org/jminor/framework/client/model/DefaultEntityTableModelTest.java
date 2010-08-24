/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.model.ColumnSearchModel;
import org.jminor.common.model.reports.ReportDataWrapper;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityTestDomain;
import org.jminor.framework.domain.Property;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DefaultEntityTableModelTest {

  private static final Entity[] testEntities;

  private final DefaultEntityTableModel testModel = new EntityTableModelTmp();

  static {
    new EntityTestDomain();
    testEntities = initTestEntities(new Entity[5]);
  }

  @Test
  public void testFiltering() {
    testModel.refresh();
    final ColumnSearchModel<Property> filterModel = testModel.getSearchModel().getPropertyFilterModel(EntityTestDomain.DETAIL_STRING);
    filterModel.setLikeValue("a");
    testModel.filterContents();
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
    assertNotNull(testModel.getAllowDeleteState());
    assertNotNull(testModel.getAllowMultipleUpdateState());
    assertFalse(testModel.isReadOnly());
    testModel.setMultipleUpdateAllowed(true).setQueryConfigurationAllowed(true).setDeleteAllowed(true);
    assertTrue(testModel.isMultipleUpdateAllowed());
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

    Entity tmpEnt = Entities.entityInstance(EntityTestDomain.T_DETAIL);
    tmpEnt.setValue(EntityTestDomain.DETAIL_ID, 3);
    assertEquals("c", testModel.getEntityByPrimaryKey(tmpEnt.getPrimaryKey()).getValue(EntityTestDomain.DETAIL_STRING));
    final List<Entity.Key> keys = new ArrayList<Entity.Key>();
    keys.add(tmpEnt.getPrimaryKey());
    tmpEnt = Entities.entityInstance(EntityTestDomain.T_DETAIL);
    tmpEnt.setValue(EntityTestDomain.DETAIL_ID, 2);
    keys.add(tmpEnt.getPrimaryKey());
    tmpEnt = Entities.entityInstance(EntityTestDomain.T_DETAIL);
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
      super(EntityTestDomain.T_DETAIL, EntityDbConnectionTest.DB_PROVIDER);
      setEditModel(new DefaultEntityEditModel(EntityTestDomain.T_DETAIL, EntityDbConnectionTest.DB_PROVIDER));
    }
    @Override
    protected List<Entity> performQuery(final Criteria criteria) {
      return Arrays.asList(testEntities);
    }
  }

  private static Entity[] initTestEntities(final Entity[] testEntities) {
    for (int i = 0; i < testEntities.length; i++) {
      testEntities[i] = Entities.entityInstance(EntityTestDomain.T_DETAIL);
      testEntities[i].setValue(EntityTestDomain.DETAIL_ID, i+1);
      testEntities[i].setValue(EntityTestDomain.DETAIL_STRING, new String[]{"a", "b", "c", "d", "e"}[i]);
    }

    return testEntities;
  }
}
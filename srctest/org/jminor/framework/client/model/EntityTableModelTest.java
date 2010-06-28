/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.model.TableSorter;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.EntityTestDomain;
import org.jminor.framework.domain.Property;

import static org.junit.Assert.*;
import org.junit.Test;

import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityTableModelTest {

  private static final Entity[] testEntities;

  private final EntityTableModel testModel = new EntityTableModelTmp();

  static {
    new EntityTestDomain();
    testEntities = initTestEntities(new Entity[5]);
  }

  private static Entity[] initTestEntities(final Entity[] testEntities) {
    for (int i = 0; i < testEntities.length; i++) {
      testEntities[i] = new Entity(EntityTestDomain.T_DETAIL);
      testEntities[i].setValue(EntityTestDomain.DETAIL_ID, i+1);
      testEntities[i].setValue(EntityTestDomain.DETAIL_STRING, new String[]{"a", "b", "c", "d", "e"}[i]);
    }

    return testEntities;
  }

  @Test
  public void testBasics() {
    final List<Property> columnProperties = testModel.getTableColumnProperties();
    assertEquals(testModel.getColumnCount(), columnProperties.size());
    assertTrue(testModel.isQueryConfigurationAllowed());
    testModel.refresh();
    testModel.setSortingStatus(EntityTestDomain.DETAIL_STRING, TableSorter.DESCENDING);
    assertEquals("e", testModel.getItemAtViewIndex(0).getValue(EntityTestDomain.DETAIL_STRING));
    testModel.setSelectedItemIndex(2);
    assertEquals(2, testModel.getSelectedIndex());
    testModel.moveSelectionDown();
    assertEquals(3, testModel.getSelectedIndex());
    testModel.moveSelectionUp();
    testModel.moveSelectionUp();
    assertEquals(1, testModel.getSelectedIndex());
    testModel.selectAll();
    assertEquals(5, testModel.getSelectedItems().size());
    testModel.clearSelection();
    assertEquals(0, testModel.getSelectedItems().size());
    assertFalse(testModel.isCellEditable(0,0));

    final Property property = EntityRepository.getProperty(EntityTestDomain.T_DETAIL, EntityTestDomain.DETAIL_STRING);
    final TableColumn column = testModel.getTableColumn(property);
    assertEquals(property, column.getIdentifier());

    final Collection<Object> values = testModel.getValues(property, false);
    assertEquals(5, values.size());
    assertTrue(values.contains("a"));
    assertTrue(values.contains("b"));
    assertTrue(values.contains("c"));
    assertTrue(values.contains("d"));
    assertTrue(values.contains("e"));

    Entity tmpEnt = new Entity(EntityTestDomain.T_DETAIL);
    tmpEnt.setValue(EntityTestDomain.DETAIL_ID, 3);
    assertEquals("c", testModel.getEntityByPrimaryKey(tmpEnt.getPrimaryKey()).getValue(EntityTestDomain.DETAIL_STRING));
    final List<Entity.Key> keys = new ArrayList<Entity.Key>();
    keys.add(tmpEnt.getPrimaryKey());
    tmpEnt = new Entity(EntityTestDomain.T_DETAIL);
    tmpEnt.setValue(EntityTestDomain.DETAIL_ID, 2);
    keys.add(tmpEnt.getPrimaryKey());
    tmpEnt = new Entity(EntityTestDomain.T_DETAIL);
    tmpEnt.setValue(EntityTestDomain.DETAIL_ID, 1);
    keys.add(tmpEnt.getPrimaryKey());

    final List<Entity> entities = testModel.getEntitiesByPrimaryKeys(keys);
    assertEquals(3, entities.size());

    final Map<String, Object> propValues = new HashMap<String, Object>();
    propValues.put(EntityTestDomain.DETAIL_STRING, "b");
    final Collection<Entity> byPropertyValues = testModel.getEntitiesByPropertyValues(propValues);
    assertEquals(1, byPropertyValues.size());
  }

  @Test
  public void entityTableModel() throws Exception {
    testModel.refresh();
    assertTrue("Model should contain all entities", tableModelContainsAll(testEntities, false, testModel));

    //test filters
    testModel.getSearchModel().getPropertyFilterModel(EntityTestDomain.DETAIL_STRING).setLikeValue("a");
    assertTrue("filter should be enabled", testModel.getSearchModel().getPropertyFilterModel(EntityTestDomain.DETAIL_STRING).isSearchEnabled());
    assertEquals("4 entities should be hidden", 4, testModel.getHiddenItemCount());
    assertFalse("Model should not contain all entities",
            tableModelContainsAll(testEntities, false, testModel));
    assertTrue("Model should contain all entities, including hidden",
            tableModelContainsAll(testEntities, true, testModel));
    testModel.getSearchModel().getPropertyFilterModel(EntityTestDomain.DETAIL_STRING).setSearchEnabled(false);
    assertFalse("filter should not be enabled", testModel.getSearchModel().getPropertyFilterModel(EntityTestDomain.DETAIL_STRING).isSearchEnabled());

    assertTrue("Model should contain all entities", tableModelContainsAll(testEntities, false, testModel));

    testModel.getSearchModel().getPropertyFilterModel(EntityTestDomain.DETAIL_STRING).setLikeValue("t"); // ekki til
    assertTrue("filter should be enabled", testModel.getSearchModel().getPropertyFilterModel(EntityTestDomain.DETAIL_STRING).isSearchEnabled());
    assertEquals("all 5 entities should be hidden", 5, testModel.getHiddenItemCount());
    assertFalse("Model should not contain all entities",
            tableModelContainsAll(testEntities, false, testModel));
    assertTrue("Model should contain all entities, including hidden",
            tableModelContainsAll(testEntities, true, testModel));
    testModel.getSearchModel().getPropertyFilterModel(EntityTestDomain.DETAIL_STRING).setSearchEnabled(false);
    assertTrue("Model should contain all entities", tableModelContainsAll(testEntities, false, testModel));
    assertFalse("filter should not be enabled", testModel.getSearchModel().getPropertyFilterModel(EntityTestDomain.DETAIL_STRING).isSearchEnabled());

    //test selection
    testModel.setSelectedItem(testEntities[0]);
    assertEquals("selected item should fit", testEntities[0], testModel.getSelectedItem());
    assertEquals("current index should fit", 0, testModel.getSelectedIndex());
    testModel.addSelectedItemIndex(1);
    assertEquals("selected item should fit", testEntities[0], testModel.getSelectedItem());
    assertEquals("selected indexes should fit", Arrays.asList(0, 1), testModel.getSelectedViewIndexes());
    assertEquals("current index should fit", 0, testModel.getSelectedIndex());
    testModel.addSelectedItemIndex(4);
    assertEquals("selected indexes should fit", Arrays.asList(0, 1, 4), testModel.getSelectedViewIndexes());
    testModel.getSelectionModel().removeIndexInterval(1, 4);
    assertEquals("selected indexes should fit", Arrays.asList(0), testModel.getSelectedViewIndexes());
    assertEquals("current index should fit", 0, testModel.getSelectedIndex());
    testModel.getSelectionModel().clearSelection();
    assertEquals("selected indexes should fit", new ArrayList<Integer>(), testModel.getSelectedViewIndexes());
    assertEquals("current index should fit", -1, testModel.getSelectionModel().getMinSelectionIndex());
    testModel.addSelectedItemIndexes(Arrays.asList(0, 3, 4));
    assertEquals("selected indexes should fit", Arrays.asList(0, 3, 4), testModel.getSelectedViewIndexes());
    assertEquals("current index should fit", 0, testModel.getSelectionModel().getMinSelectionIndex());
    testModel.getSelectionModel().removeSelectionInterval(0, 0);
    assertEquals("current index should fit", 3, testModel.getSelectionModel().getMinSelectionIndex());
    testModel.getSelectionModel().removeSelectionInterval(3, 3);
    assertEquals("current index should fit", 4, testModel.getSelectionModel().getMinSelectionIndex());

    testModel.addSelectedItemIndexes(Arrays.asList(0, 3, 4));
    assertEquals("current index should fit", 0, testModel.getSelectionModel().getMinSelectionIndex());
    testModel.getSelectionModel().removeSelectionInterval(3, 3);
    assertEquals("current index should fit", 0, testModel.getSelectionModel().getMinSelectionIndex());
    testModel.getSelectionModel().clearSelection();
    assertEquals("current index should fit", -1, testModel.getSelectionModel().getMinSelectionIndex());

    testModel.addSelectedItemIndexes(Arrays.asList(0, 1, 2, 3, 4));
    assertEquals("current index should fit", 0, testModel.getSelectionModel().getMinSelectionIndex());
    testModel.getSelectionModel().removeSelectionInterval(0, 0);
    assertEquals("current index should fit", 1, testModel.getSelectionModel().getMinSelectionIndex());
    testModel.getSelectionModel().removeSelectionInterval(1, 1);
    assertEquals("current index should fit", 2, testModel.getSelectionModel().getMinSelectionIndex());
    testModel.getSelectionModel().removeSelectionInterval(2, 2);
    assertEquals("current index should fit", 3, testModel.getSelectionModel().getMinSelectionIndex());
    testModel.getSelectionModel().removeSelectionInterval(3, 3);
    assertEquals("current index should fit", 4, testModel.getSelectionModel().getMinSelectionIndex());
    testModel.getSelectionModel().removeSelectionInterval(4, 4);
    assertEquals("current index should fit", -1, testModel.getSelectionModel().getMinSelectionIndex());

    //test selection and filtering together
    testModel.addSelectedItemIndexes(Arrays.asList(3));
    assertEquals("current index should fit", 3, testModel.getSelectionModel().getMinSelectionIndex());

    testModel.getSearchModel().getPropertyFilterModel(EntityTestDomain.DETAIL_STRING).setLikeValue("d");
    assertEquals("current index should fit", 0,
            testModel.getSelectionModel().getMinSelectionIndex());
    assertEquals("selected indexes should fit", Arrays.asList(0), testModel.getSelectedViewIndexes());
    testModel.getSearchModel().getPropertyFilterModel(EntityTestDomain.DETAIL_STRING).setSearchEnabled(false);
    assertEquals("current index should fit", 0,
            testModel.getSelectionModel().getMinSelectionIndex());
    assertEquals("selected item should fit", testEntities[3], testModel.getSelectedItem());

    //test selection and sorting together
    testModel.setSelectedItemIndexes(Arrays.asList(3));
    assertEquals("current index should fit", 3, testModel.getSelectionModel().getMinSelectionIndex());
    assertEquals("current selected item should fit", testEntities[2], testModel.getSelectedItem());

    testModel.setSortingStatus(2, TableSorter.ASCENDING);
    assertEquals("current selected item should fit", testEntities[2], testModel.getSelectedItem());
    assertEquals("current index should fit", 2,
            testModel.getSelectionModel().getMinSelectionIndex());

    testModel.setSelectedItemIndexes(Arrays.asList(0));
    assertEquals("current selected item should fit", testEntities[0], testModel.getSelectedItem());
    testModel.setSortingStatus(2, TableSorter.DESCENDING);
    assertEquals("current index should fit", 4,
            testModel.getSelectionModel().getMinSelectionIndex());

    assertEquals("selected indexes should fit", Arrays.asList(4), testModel.getSelectedViewIndexes());
    assertEquals("current selected item should fit", testEntities[0], testModel.getSelectedItem());
    assertEquals("current index should fit", 4,
            testModel.getSelectionModel().getMinSelectionIndex());
    assertEquals("selected item should fit", testEntities[0], testModel.getSelectedItem());
  }

  private boolean tableModelContainsAll(final Entity[] entities, final boolean includeFiltered,
                                        final EntityTableModel model) {
    for (final Entity entity : entities) {
      if (!model.contains(entity, includeFiltered)) {
        return false;
      }
    }

    return true;
  }

  public static class EntityTableModelTmp extends DefaultEntityTableModel {
    public EntityTableModelTmp() {
      super(EntityTestDomain.T_DETAIL, EntityDbConnectionTest.DB_PROVIDER);
    }

    @Override
    public void refresh() {
      clear();
      addItems(performQuery(null), false);
    }

    @Override
    protected List<Entity> performQuery(final Criteria criteria) {
      return Arrays.asList(testEntities);
    }

    protected boolean isRefreshRequired() {
      return true;
    }
  }
}
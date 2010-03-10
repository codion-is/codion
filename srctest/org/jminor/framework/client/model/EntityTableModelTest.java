/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.Criteria;
import org.jminor.common.model.IntArray;
import org.jminor.common.model.table.TableSorter;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityTestDomain;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

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
  public void entityTableModel() throws Exception {
    testModel.refresh();
    assertTrue("Model should contain all entities", tableModelContainsAll(testEntities, false, testModel));

    //test filters
    testModel.getSearchModel().getPropertyFilterModel(EntityTestDomain.DETAIL_STRING).setLikeValue("a");
    assertTrue("filter should be enabled", testModel.getSearchModel().getPropertyFilterModel(EntityTestDomain.DETAIL_STRING).isSearchEnabled());
    assertEquals("4 entities should be hidden", 4, testModel.getHiddenCount());
    assertFalse("Model should not contain all entities",
            tableModelContainsAll(testEntities, false, testModel));
    assertTrue("Model should contain all entities, including hidden",
            tableModelContainsAll(testEntities, true, testModel));
    testModel.getSearchModel().getPropertyFilterModel(EntityTestDomain.DETAIL_STRING).setSearchEnabled(false);
    assertFalse("filter should not be enabled", testModel.getSearchModel().getPropertyFilterModel(EntityTestDomain.DETAIL_STRING).isSearchEnabled());

    assertTrue("Model should contain all entities", tableModelContainsAll(testEntities, false, testModel));

    testModel.getSearchModel().getPropertyFilterModel(EntityTestDomain.DETAIL_STRING).setLikeValue("t"); // ekki til
    assertTrue("filter should be enabled", testModel.getSearchModel().getPropertyFilterModel(EntityTestDomain.DETAIL_STRING).isSearchEnabled());
    assertEquals("all 5 entities should be hidden", 5, testModel.getHiddenCount());
    assertFalse("Model should not contain all entities",
            tableModelContainsAll(testEntities, false, testModel));
    assertTrue("Model should contain all entities, including hidden",
            tableModelContainsAll(testEntities, true, testModel));
    testModel.getSearchModel().getPropertyFilterModel(EntityTestDomain.DETAIL_STRING).setSearchEnabled(false);
    assertTrue("Model should contain all entities", tableModelContainsAll(testEntities, false, testModel));
    assertFalse("filter should not be enabled", testModel.getSearchModel().getPropertyFilterModel(EntityTestDomain.DETAIL_STRING).isSearchEnabled());

    //test selection
    testModel.setSelectedEntity(testEntities[0]);
    assertEquals("selected item should fit", testEntities[0], testModel.getSelectedEntity());
    assertEquals("current index should fit", 0, testModel.getSelectedIndex());
    testModel.addSelectedItemIndex(1);
    assertEquals("selected item should fit", testEntities[0], testModel.getSelectedEntity());
    assertEquals("selected indexes should fit", Arrays.asList(0, 1), testModel.getSelectedViewIndexes());
    assertEquals("current index should fit", 0, testModel.getSelectedIndex());
    testModel.addSelectedItemIndex(4);
    assertEquals("selected indexes should fit", Arrays.asList(0, 1, 4), testModel.getSelectedViewIndexes());
    testModel.getSelectionModel().removeIndexInterval(1, 4);
    assertEquals("selected indexes should fit", Arrays.asList(0), testModel.getSelectedViewIndexes());
    assertEquals("current index should fit", 0, testModel.getSelectedIndex());
    testModel.getSelectionModel().clearSelection();
    assertEquals("selected indexes should fit", new IntArray(), testModel.getSelectedViewIndexes());
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
    assertEquals("selected item should fit", testEntities[3], testModel.getSelectedEntity());

    //test selection and sorting together
    testModel.setSelectedItemIndexes(Arrays.asList(3));
    assertEquals("current index should fit", 3, testModel.getSelectionModel().getMinSelectionIndex());
    assertEquals("current selected item should fit", testEntities[2], testModel.getSelectedEntity());

    testModel.getTableSorter().setSortingStatus(2, TableSorter.ASCENDING);
    assertEquals("current selected item should fit", testEntities[2], testModel.getSelectedEntity());
    assertEquals("current index should fit", 2,
            testModel.getSelectionModel().getMinSelectionIndex());

    testModel.setSelectedItemIndexes(Arrays.asList(0));
    assertEquals("current selected item should fit", testEntities[0], testModel.getSelectedEntity());
    testModel.getTableSorter().setSortingStatus(2, TableSorter.DESCENDING);
    assertEquals("current index should fit", 4,
            testModel.getSelectionModel().getMinSelectionIndex());

    assertEquals("selected indexes should fit", Arrays.asList(4), testModel.getSelectedViewIndexes());
    assertEquals("current selected item should fit", testEntities[0], testModel.getSelectedEntity());
    assertEquals("current index should fit", 4,
            testModel.getSelectionModel().getMinSelectionIndex());
    assertEquals("selected item should fit", testEntities[0], testModel.getSelectedEntity());
  }

  private boolean tableModelContainsAll(final Entity[] entities, final boolean includeFiltered,
                                        final EntityTableModel model) {
    for (final Entity entity : entities) {
      if (!model.contains(entity, includeFiltered))
        return false;
    }

    return true;
  }

  public static class EntityTableModelTmp extends EntityTableModel {
    public EntityTableModelTmp() {
      super(EntityTestDomain.T_DETAIL, EntityDbConnectionTest.dbProvider);
    }

    @Override
    public void refresh() {
      clear();
      addEntities(performQuery(null), false);
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
/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.IntArray;
import org.jminor.common.model.UserException;
import org.jminor.common.model.table.TableSorter;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.ModelTestDomain;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;

public class TestEntityTableModel extends TestCase {

  private static final Entity[] testEntities;

  private final EntityTableModel testModel = new EntityTableModelTmp();

  static {
    new ModelTestDomain();
    testEntities = initTestEntities(new Entity[5]);
  }
  
  private static Entity[] initTestEntities(final Entity[] testEntities) {
    for (int i = 0; i < testEntities.length; i++) {
      testEntities[i] = new Entity(ModelTestDomain.T_TEST_DETAIL);
      testEntities[i].setValue(ModelTestDomain.TEST_DETAIL_ID, i+1);
      testEntities[i].setValue(ModelTestDomain.TEST_DETAIL_STRING, new String[]{"a", "b", "c", "d", "e"}[i]);
    }

    return testEntities;
  }

  public TestEntityTableModel(final String name) {
    super(name);
  }

  public void testEntityTableModel() throws Exception {
    testModel.refresh();
    assertTrue("Model should contain all entities", tableModelContainsAll(testEntities, false, testModel));

    //test filters
    testModel.getPropertyFilterModel(ModelTestDomain.TEST_DETAIL_STRING).setLikeValue("a");
    assertTrue("filter should be enabled", testModel.getPropertyFilterModel(ModelTestDomain.TEST_DETAIL_STRING).isSearchEnabled());
    assertEquals("4 entities should be filtered", 4, testModel.getFilteredCount());
    assertFalse("Model should not contain all entities",
            tableModelContainsAll(testEntities, false, testModel));
    assertTrue("Model should contain all entities, including filtered",
            tableModelContainsAll(testEntities, true, testModel));
    testModel.getPropertyFilterModel(ModelTestDomain.TEST_DETAIL_STRING).setSearchEnabled(false);
    assertFalse("filter should not be enabled", testModel.getPropertyFilterModel(ModelTestDomain.TEST_DETAIL_STRING).isSearchEnabled());

    assertTrue("Model should contain all entities", tableModelContainsAll(testEntities, false, testModel));

    testModel.getPropertyFilterModel(ModelTestDomain.TEST_DETAIL_STRING).setLikeValue("t"); // ekki til
    assertTrue("filter should be enabled", testModel.getPropertyFilterModel(ModelTestDomain.TEST_DETAIL_STRING).isSearchEnabled());
    assertEquals("all 5 entities should be filtered", 5, testModel.getFilteredCount());
    assertFalse("Model should not contain all entities",
            tableModelContainsAll(testEntities, false, testModel));
    assertTrue("Model should contain all entities, including filtered",
            tableModelContainsAll(testEntities, true, testModel));
    testModel.getPropertyFilterModel(ModelTestDomain.TEST_DETAIL_STRING).setSearchEnabled(false);
    assertTrue("Model should contain all entities", tableModelContainsAll(testEntities, false, testModel));
    assertFalse("filter should not be enabled", testModel.getPropertyFilterModel(ModelTestDomain.TEST_DETAIL_STRING).isSearchEnabled());

    //test selection
    testModel.setSelectedEntity(testEntities[0]);
    assertEquals("selected item should fit", testEntities[0], testModel.getSelectedEntity());
    assertEquals("current index should fit", 0, testModel.getSelectedIndex());
    testModel.addSelectedItemIdx(1);
    assertEquals("selected item should fit", testEntities[0], testModel.getSelectedEntity());
    assertEquals("selected indexes should fit", new IntArray(new int[]{0, 1}), new IntArray(testModel.getSelectedViewIndexes()));
    assertEquals("current index should fit", 0, testModel.getSelectedIndex());
    testModel.addSelectedItemIdx(4);
    assertEquals("selected indexes should fit", new IntArray(new int[]{0, 1, 4}), new IntArray(testModel.getSelectedViewIndexes()));
    testModel.getSelectionModel().removeIndexInterval(1, 4);
    assertEquals("selected indexes should fit", new IntArray(new int[]{0}), new IntArray(testModel.getSelectedViewIndexes()));
    assertEquals("current index should fit", 0, testModel.getSelectedIndex());
    testModel.getSelectionModel().clearSelection();
    assertEquals("selected indexes should fit", new IntArray(new int[]{}), new IntArray(testModel.getSelectedViewIndexes()));
    assertEquals("current index should fit", -1, testModel.getSelectionModel().getMinSelectionIndex());
    testModel.addSelectedItemIndexes(new int[]{0, 3, 4});
    assertEquals("selected indexes should fit", new IntArray(new int[]{0, 3, 4}), new IntArray(testModel.getSelectedViewIndexes()));
    assertEquals("current index should fit", 0, testModel.getSelectionModel().getMinSelectionIndex());
    testModel.getSelectionModel().removeSelectionInterval(0, 0);
    assertEquals("current index should fit", 3, testModel.getSelectionModel().getMinSelectionIndex());
    testModel.getSelectionModel().removeSelectionInterval(3, 3);
    assertEquals("current index should fit", 4, testModel.getSelectionModel().getMinSelectionIndex());

    testModel.addSelectedItemIndexes(new int[]{0, 3, 4});
    assertEquals("current index should fit", 0, testModel.getSelectionModel().getMinSelectionIndex());
    testModel.getSelectionModel().removeSelectionInterval(3, 3);
    assertEquals("current index should fit", 0, testModel.getSelectionModel().getMinSelectionIndex());
    testModel.getSelectionModel().clearSelection();
    assertEquals("current index should fit", -1, testModel.getSelectionModel().getMinSelectionIndex());

    testModel.addSelectedItemIndexes(new int[]{0, 1, 2, 3, 4});
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
    testModel.addSelectedItemIndexes(new int[]{3});
    assertEquals("current index should fit", 3, testModel.getSelectionModel().getMinSelectionIndex());

    testModel.getPropertyFilterModel(ModelTestDomain.TEST_DETAIL_STRING).setLikeValue("d");
    assertEquals("current index should fit", 0,
            testModel.getSelectionModel().getMinSelectionIndex());
    assertEquals("selected indexes should fit", new IntArray(new int[]{0}), new IntArray(testModel.getSelectedViewIndexes()));
    testModel.getPropertyFilterModel(ModelTestDomain.TEST_DETAIL_STRING).setSearchEnabled(false);
    assertEquals("current index should fit", 0,
            testModel.getSelectionModel().getMinSelectionIndex());
    assertEquals("selected item should fit", testEntities[3], testModel.getSelectedEntity());

    //test selection and sorting together
    testModel.setSelectedItemIndexes(new int[]{3});
    assertEquals("current index should fit", 3, testModel.getSelectionModel().getMinSelectionIndex());
    assertEquals("current selected item should fit", testEntities[2], testModel.getSelectedEntity());

    testModel.getTableSorter().setSortingStatus(2, TableSorter.ASCENDING);
    assertEquals("current selected item should fit", testEntities[2], testModel.getSelectedEntity());
    assertEquals("current index should fit", 2,
            testModel.getSelectionModel().getMinSelectionIndex());

    testModel.setSelectedItemIndexes(new int[]{0});
    assertEquals("current selected item should fit", testEntities[0], testModel.getSelectedEntity());
    testModel.getTableSorter().setSortingStatus(2, TableSorter.DESCENDING);
    assertEquals("current index should fit", 4,
            testModel.getSelectionModel().getMinSelectionIndex());

    assertEquals("selected indexes should fit", new IntArray(new int[]{4}), new IntArray(testModel.getSelectedViewIndexes()));
    assertEquals("current selected item should fit", testEntities[0], testModel.getSelectedEntity());
    assertEquals("current index should fit", 4,
            testModel.getSelectionModel().getMinSelectionIndex());
    assertEquals("selected item should fit", testEntities[0], testModel.getSelectedEntity());
  }

  public boolean tableModelContainsAll(final Entity[] entities, final boolean includeFiltered,
                                       final EntityTableModel model) {
    for (final Entity entity : entities) {
      if (!model.contains(entity, includeFiltered))
        return false;
    }

    return true;
  }

  public static class EntityTableModelTmp extends EntityTableModel {
    public EntityTableModelTmp() {
      super(null, ModelTestDomain.T_TEST_DETAIL);
    }

    public synchronized void refresh() throws UserException {
      removeAll();
      addEntities(getAllEntitiesFromDb(), false);
    }

    protected List<Entity> getAllEntitiesFromDb() {
      return Arrays.asList(testEntities);
    }

    protected boolean isRefreshRequired() {
      return true;
    }
  }
}
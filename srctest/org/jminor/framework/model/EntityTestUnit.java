/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.db.DbException;

import java.util.Arrays;
import java.util.List;

public abstract class EntityTestUnit extends AbstractEntityTestUnit {

  public EntityTestUnit(final String name, final AbstractEntityTestFixture fixture, final String entityID) {
    super(name, fixture, entityID);
  }

  public void testInsert() throws Exception {
    try {
      final Entity testEntity = createTestEntities().get(0);
      final Entity tmp = getDbConnection().selectSingle(testEntity.getPrimaryKey());
      assertEquals(testEntity, tmp);
    }
    catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  public void testSelect() throws Exception {
    try {
      final Entity testEntity = createTestEntities().get(0);

      final Entity etmp = getDbConnection().selectSingle(testEntity.getPrimaryKey());
      assertEquals(testEntity, etmp);

      List<Entity> allEntities = getDbConnection().selectAll(getEntityID());
      boolean found = false;
      for (Entity entity : allEntities) {
        if (testEntity.getPrimaryKey().equals(entity.getPrimaryKey()))
          found = true;
      }
      assertTrue(found);
      allEntities = getDbConnection().selectMany(Arrays.asList(testEntity.getPrimaryKey()));
      found = false;
      for (Entity entity : allEntities) {
        if (testEntity.getPrimaryKey().equals(entity.getPrimaryKey()))
          found = true;
      }
      assertTrue(found);
    }
    catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  public void testUpdate() throws Exception {
    try {
      final Entity testEntity = createTestEntities().get(0);

      modifyEntity(testEntity);
      getDbConnection().update(Arrays.asList(testEntity));

      final Entity tmp = getDbConnection().selectSingle(testEntity.getPrimaryKey());
      assertEquals(testEntity, tmp);
    }
    catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  public void testDelete() throws Exception {
    try {
      final List<Entity> testEntities = createTestEntities();

      for (int i = testEntities.size() - 1; i >= 0; i--) {
        //in case the entities depend on each other via a foreign key
        //they must be deleted in the opposite order of creation
        getDbConnection().delete(Arrays.asList(testEntities.get(i)));
      }

      for (final Entity testEntity : testEntities) {
        boolean caught = false;
        try {
          getDbConnection().selectSingle(testEntity.getPrimaryKey());
        }
        catch (DbException e) {
          caught = true;
        }
        assertTrue(caught);
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  /**
   * This method should leave the entity modified, so that is can be used for the update test
   * @param testEntity the entity to modify
   */
  protected abstract void modifyEntity(Entity testEntity);
}

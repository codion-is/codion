/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.db.DbException;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public abstract class EntityTestUnit extends TestCase {

  protected final EntityTestFixture fixture;
  protected final String entityID;
  private final HashMap<String, Entity> referencedEntities = new HashMap<String, Entity>();

  public EntityTestUnit(final String name, final EntityTestFixture fixture, final String entityID) {
    super(name);
    this.fixture = fixture;
    this.entityID = entityID;
  }

  public void testInsert() throws Exception {
    try {
      final Entity testEntity = createTestEntities().get(0);
      final Entity tmp = fixture.getDbConnection().selectSingle(testEntity.getPrimaryKey());
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

      final Entity etmp = fixture.getDbConnection().selectSingle(testEntity.getPrimaryKey());
      assertEquals(testEntity, etmp);

      List<Entity> allEntities = fixture.getDbConnection().selectAll(getEntityID());
      boolean found = false;
      for (Entity entity : allEntities) {
        if (testEntity.getPrimaryKey().equals(entity.getPrimaryKey()))
          found = true;
      }
      assertTrue(found);
      allEntities = fixture.getDbConnection().selectMany(Arrays.asList(testEntity.getPrimaryKey()));
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
      fixture.getDbConnection().update(Arrays.asList(testEntity));

      final Entity tmp = fixture.getDbConnection().selectSingle(testEntity.getPrimaryKey());
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
        fixture.getDbConnection().delete(Arrays.asList(testEntities.get(i)));
      }

      for (final Entity testEntity : testEntities) {
        boolean caught = false;
        try {
          fixture.getDbConnection().selectSingle(testEntity.getPrimaryKey());
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
   * This method should leave the entity modified, so that it can be used for the update test
   * @param testEntity the entity to modify
   */
  protected abstract void modifyEntity(Entity testEntity);

  /**
   * @return Value for property 'entityID'.
   */
  protected String getEntityID() {
    return this.entityID;
  }

  protected abstract List<Entity> initializeTestEntities();

  /**
   * @return Value for property 'referenceEntities'.
   */
  protected HashMap<String, Entity> getReferencedEntities() {
    return this.referencedEntities;
  }

  protected List<Entity> createTestEntities() throws Exception {
    return fixture.getDbConnection().selectMany(fixture.getDbConnection().insert(initializeTestEntities()));
  }

  /**
   * @return Value for property 'referenceEntityIDs'.
   */
  protected Collection<String> getReferenceEntityIDs() {
    return new ArrayList<String>(0);
  }

  /** {@inheritDoc} */
  protected void setUp() throws Exception {
    super.setUp();
    try { // in case the last test case did not end gracefully, with an exception that is
      if (fixture.getDbConnection().isTransactionOpen())
        fixture.getDbConnection().endTransaction(true);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    try {
      fixture.getDbConnection().startTransaction();
      referencedEntities.putAll(fixture.initializeReferenceEntities(entityID));
    }
    catch (Exception e) { //this exception will cause the test case not to be run,
      fixture.getDbConnection().endTransaction(true); //so we must end the transaction manually
      throw e;
    }
  }

  /** {@inheritDoc} */
  protected void tearDown() throws Exception {
    super.tearDown();
    referencedEntities.clear();
    try {
      fixture.getDbConnection().endTransaction(true);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}

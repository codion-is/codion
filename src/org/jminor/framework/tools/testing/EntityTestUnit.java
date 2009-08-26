/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.tools.testing;

import org.jminor.common.db.DbException;
import org.jminor.common.db.RecordNotFoundException;
import org.jminor.common.db.User;
import org.jminor.common.model.UserCancelException;
import org.jminor.common.model.UserException;
import org.jminor.common.model.Util;
import org.jminor.common.ui.LoginPanel;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityDbProviderFactory;
import org.jminor.framework.db.IEntityDb;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityKey;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public abstract class EntityTestUnit extends TestCase {

  private IEntityDb dbConnection;
  private final HashMap<String, Entity> referencedEntities = new HashMap<String, Entity>();

  public EntityTestUnit() {
    try {
      loadDomainModel();
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  protected void setUp() throws Exception {
    dbConnection = initializeDbConnection();
  }

  /** {@inheritDoc} */
  @Override
  protected void tearDown() throws Exception {
    if (dbConnection != null)
      dbConnection.logout();
  }

  /**
   * @return the IEntityDb connection this test case should use
   * @throws UserException in case of an exception
   * @throws UserCancelException in case the login in was cancelled
   */
  protected IEntityDb initializeDbConnection() throws UserException, UserCancelException {
    return EntityDbProviderFactory.createEntityDbProvider(getTestUser(), getClass().getSimpleName()).getEntityDb();
  }

  /**
   * Returns the database user to use when running the tests, this default implementation
   * prompts for the user/password information, usually overridden
   * @return the db user to use when running the test
   * @throws org.jminor.common.model.UserCancelException in case the user cancels the login
   */
  protected User getTestUser() throws UserCancelException {
    return LoginPanel.getUser(null, new User(Configuration.getDefaultUsername(getClass().getName()), null));
  }

  protected IEntityDb getDbConnection() {
    return dbConnection;
  }

  protected Entity getReferenceEntity(final String entityID) {
    final Entity ret = referencedEntities.get(entityID);
    if (ret == null)
      throw new RuntimeException("No reference entity available of type " + entityID);

    return ret;
  }

  protected void setReferenceEntity(final String entityID, final Entity entity) throws Exception {
    if (!entity.is(entityID))
      throw new IllegalArgumentException("Reference entity type mismatch: " + entityID + " - " + entity.getEntityID());

    referencedEntities.put(entityID, initialize(entity));
  }

  protected void testEntity(final String entityID) throws Exception {
    try {
      getDbConnection().beginTransaction();
      initializeReferenceEntities(addAllReferencedEntityIDs(entityID, new HashSet<String>()));
      final Entity initialEntity = initializeTestEntity(entityID);
      if (initialEntity == null)
        throw new Exception("No test entity provided " + entityID);

      final Entity testEntity = testInsert(initialEntity);
      testSelect(testEntity);
      testUpdate(testEntity);
      testDelete(testEntity);
    }
    finally {
      referencedEntities.clear();
      getDbConnection().endTransaction(false);
    }
  }

  protected Entity testInsert(final Entity testEntity) throws Exception {
    try {
      final List<EntityKey> keys = getDbConnection().insert(Arrays.asList(testEntity));
      try {
        return getDbConnection().selectSingle(keys.get(0));
      }
      catch (RecordNotFoundException e) {
        fail("Inserted entity of type " + testEntity.getEntityID() + " not returned by select after insert");
        throw e;
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  protected void testSelect(final Entity testEntity) throws Exception {
    try {
      final Entity etmp = getDbConnection().selectSingle(testEntity.getPrimaryKey());
      assertTrue("Entity of type " + testEntity.getEntityID() + " failed select comparison",
              testEntity.propertyValuesEqual(etmp));

      final List<Entity> allEntities = getDbConnection().selectMany(Arrays.asList(testEntity.getPrimaryKey()));
      boolean found = false;
      for (final Entity entity : allEntities) {
        if (testEntity.getPrimaryKey().equals(entity.getPrimaryKey()))
          found = true;
      }
      assertTrue("Entity of type " + testEntity.getEntityID() + " was not found when selecting by primary key", found);
    }
    catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  protected void testUpdate(final Entity testEntity) throws Exception {
    try {
      modifyEntity(testEntity);
      if (!testEntity.isModified())
        return;

      getDbConnection().update(Arrays.asList(testEntity));

      final Entity tmp = getDbConnection().selectSingle(testEntity.getPrimaryKey());
      assertEquals("Primary keys of entity and its updated counterpart should be equal",
              testEntity.getPrimaryKey(), tmp.getPrimaryKey());
      for (final Property property : EntityRepository.getProperties(testEntity.getEntityID()).values()) {
        if (!property.isSelectOnly() && property.isUpdatable()) {
          final Object beforeUpdate = testEntity.getRawValue(property.propertyID);
          final Object afterUpdate = tmp.getRawValue(property.propertyID);
          assertTrue("Values of property " + property + " should be equal after update ["
                  + beforeUpdate + (beforeUpdate != null ? (" (" + beforeUpdate.getClass() + ")") : "") + ", "
                  + afterUpdate + (afterUpdate != null ? (" (" + afterUpdate.getClass() + ")") : "") + "]",
                  Util.equal(beforeUpdate, afterUpdate));
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  protected void testDelete(final Entity testEntity) throws Exception {
    try {
      getDbConnection().delete(Arrays.asList(testEntity));

      boolean caught = false;
      try {
        getDbConnection().selectSingle(testEntity.getPrimaryKey());
      }
      catch (DbException e) {
        caught = true;
      }
      assertTrue("Entity of type " + testEntity.getEntityID() + " failed delete test", caught);
    }
    catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  /**
   * This method should load the domain model, for example by instantiating the domain model
   * class or simply loading it by name
   */
  protected abstract void loadDomainModel();

  /**
   * This method should return an instance of the entity specified by <code>entityID</code>
   * @param entityID the entityID for which to initialize an entity instance
   * @return an entity
   */
  protected abstract Entity initializeTestEntity(final String entityID);

  /**
   * This method should return <code>testEntity</code> in a modified state
   * @param testEntity the entity to modify
   */
  protected abstract void modifyEntity(final Entity testEntity);

  /**
   * This method should initialize instances of entities specified by the entityIDs found in the
   * <code>entityIDs</code> Collection and map them to their respective enitityIDs via the setReferenceEntity method
   * @param referenceEntityIDs the IDs of the entities that should be initialized
   * @throws Exception in case of an exception
   * @see #setReferenceEntity(String, org.jminor.framework.domain.Entity)
   */
  protected abstract void initializeReferenceEntities(final Collection<String> referenceEntityIDs) throws Exception;

  /**
   * Initializes the given entity, that is, performes an insert on it in case it doesn't
   * already exist in the database, returns the same entity
   * @param entity the entity to initialize
   * @return the entity
   * @throws Exception in case of an exception
   */
  private Entity initialize(final Entity entity) throws Exception {
    try {
      final List<Entity> entities = getDbConnection().selectMany(Arrays.asList(entity.getPrimaryKey()));
      if (entities.size() > 0)
        return entities.get(0);

      return getDbConnection().selectSingle(getDbConnection().insert(Arrays.asList(entity)).get(0));
    }
    catch (DbException e) {
      System.out.println(e.getStatement());
      e.printStackTrace();
      throw e;
    }
  }

  private Collection<String> addAllReferencedEntityIDs(final String entityID, final Collection<String> container) {
    for (final Property.ForeignKeyProperty foreignKeyProperty : EntityRepository.getForeignKeyProperties(entityID)) {
      final String referenceEntityID = foreignKeyProperty.referenceEntityID;
      if (referenceEntityID != null) {
        if (!container.contains(referenceEntityID)) {
          container.add(referenceEntityID);
          addAllReferencedEntityIDs(referenceEntityID, container);
        }
      }
    }
    return container;
  }
}

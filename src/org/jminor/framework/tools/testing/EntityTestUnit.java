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
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.db.provider.EntityDbProviderFactory;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * A class for unit testing domain entities
 */
public abstract class EntityTestUnit extends TestCase {

  private EntityDbProvider dbConnectionProvider;
  private final HashMap<String, Entity> referencedEntities = new HashMap<String, Entity>();

  public EntityTestUnit() {
    loadDomainModel();
  }

  /** {@inheritDoc} */
  @Override
  protected void setUp() throws Exception {
    dbConnectionProvider = initializeDbConnectionProvider();
  }

  /** {@inheritDoc} */
  @Override
  protected void tearDown() throws Exception {
    if (dbConnectionProvider != null)
      dbConnectionProvider.disconnect();
  }

  /**
   * @return the EntityDbProvider instance this test case should use
   * @throws UserException in case of an exception
   * @throws UserCancelException in case the login was cancelled
   */
  protected EntityDbProvider initializeDbConnectionProvider() throws UserException, UserCancelException {
    return EntityDbProviderFactory.createEntityDbProvider(getTestUser(), getClass().getSimpleName());
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

  /**
   * @return the EntityDb instance used by this EntityTestUnit
   */
  protected EntityDbProvider getDbConnectionProvider() {
    return dbConnectionProvider;
  }

  /**
   * @param entityID the entityID of the the reference entity to retrieve
   * @return the entity mapped to the given entityID
   * @see #setReferenceEntity(String, org.jminor.framework.domain.Entity)
   */
  protected Entity getReferenceEntity(final String entityID) {
    final Entity ret = referencedEntities.get(entityID);
    if (ret == null)
      throw new RuntimeException("No reference entity available of type " + entityID);

    return ret;
  }

  /**
   * Maps the given reference entity to the given entityID
   * @param entityID the entityID
   * @param entity the reference entity to map to the given entityID
   * @throws Exception in case of an exception
   * @see #getReferenceEntity(String)
   */
  protected void setReferenceEntity(final String entityID, final Entity entity) throws Exception {
    if (!entity.is(entityID))
      throw new IllegalArgumentException("Reference entity type mismatch: " + entityID + " - " + entity.getEntityID());

    referencedEntities.put(entityID, initialize(entity));
  }

  /**
   * Runs the insert/update/select/delete tests for the given entityID
   * @param entityID the ID of the entity to test
   * @throws Exception in case of an exception
   */
  protected void testEntity(final String entityID) throws Exception {
    try {
      getDbConnectionProvider().getEntityDb().beginTransaction();
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
      getDbConnectionProvider().getEntityDb().endTransaction(false);
    }
  }

  /**
   * Tests inserting the given entity
   * @param testEntity the entity to test insert for
   * @return the same entity retrieved from the database after the insert
   * @throws Exception in case of an exception
   */
  protected Entity testInsert(final Entity testEntity) throws Exception {
    try {
      final List<Entity.Key> keys = getDbConnectionProvider().getEntityDb().insert(Arrays.asList(testEntity));
      try {
        return getDbConnectionProvider().getEntityDb().selectSingle(keys.get(0));
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

  /**
   * Tests selecting the given entity
   * @param testEntity the entity to test selecting
   * @throws Exception in case of an exception
   */
  protected void testSelect(final Entity testEntity) throws Exception {
    try {
      final Entity etmp = getDbConnectionProvider().getEntityDb().selectSingle(testEntity.getPrimaryKey());
      assertTrue("Entity of type " + testEntity.getEntityID() + " failed select comparison",
              testEntity.propertyValuesEqual(etmp));

      final List<Entity> entityByPrimaryKey = getDbConnectionProvider().getEntityDb().selectMany(
              Arrays.asList(testEntity.getPrimaryKey()));
      assertTrue("Entity of type " + testEntity.getEntityID() + " was not found when selecting by primary key",
              entityByPrimaryKey.size() == 1 && entityByPrimaryKey.get(0).equals(testEntity));
    }
    catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  /**
   * Test updating the given entity, if the entity is not modified this test does nothing
   * @param testEntity the entity to test updating
   * @throws Exception in case of an exception
   */
  protected void testUpdate(final Entity testEntity) throws Exception {
    try {
      modifyEntity(testEntity);
      if (!testEntity.isModified())
        return;

      getDbConnectionProvider().getEntityDb().update(Arrays.asList(testEntity));

      final Entity tmp = getDbConnectionProvider().getEntityDb().selectSingle(testEntity.getPrimaryKey());
      assertEquals("Primary keys of entity and its updated counterpart should be equal",
              testEntity.getPrimaryKey(), tmp.getPrimaryKey());
      for (final Property property : EntityRepository.getProperties(testEntity.getEntityID()).values()) {
        if (!property.isSelectOnly() && property.isUpdatable()) {
          final Object beforeUpdate = testEntity.getRawValue(property.getPropertyID());
          final Object afterUpdate = tmp.getRawValue(property.getPropertyID());
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

  /**
   * Test deleting the given entity
   * @param testEntity the entity to test deleting
   * @throws Exception in case of an exception
   */
  protected void testDelete(final Entity testEntity) throws Exception {
    try {
      getDbConnectionProvider().getEntityDb().delete(EntityUtil.getPrimaryKeys(Arrays.asList(testEntity)));

      boolean caught = false;
      try {
        getDbConnectionProvider().getEntityDb().selectSingle(testEntity.getPrimaryKey());
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
      final List<Entity> entities = getDbConnectionProvider().getEntityDb().selectMany(Arrays.asList(entity.getPrimaryKey()));
      if (entities.size() > 0)
        return entities.get(0);

      return getDbConnectionProvider().getEntityDb().selectSingle(
              getDbConnectionProvider().getEntityDb().insert(Arrays.asList(entity)).get(0));
    }
    catch (DbException e) {
      System.out.println(e.getStatement());
      e.printStackTrace();
      throw e;
    }
  }

  /**
   * Adds all entityIDs referenced via foreign keys by the entity identified by <code>entityID</code> to <code>container</code>
   * @param entityID the entityID
   * @param container the container
   * @return the container
   */
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

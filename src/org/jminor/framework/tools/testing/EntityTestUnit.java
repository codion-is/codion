/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.tools.testing;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.exception.RecordNotFoundException;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.ui.LoginPanel;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.db.provider.EntityConnectionProvider;
import org.jminor.framework.db.provider.EntityConnectionProviders;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;

import org.junit.After;
import org.junit.Before;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * A class for unit testing domain entities.
 */
public abstract class EntityTestUnit {

  private EntityConnection connection;
  private final Map<String, Entity> referencedEntities = new HashMap<String, Entity>();

  /**
   * Instantiates a new EntityTestUnit.
   */
  public EntityTestUnit() {
    loadDomainModel();
  }

  /**
   * Sets up the database connection
   * @throws CancelException in case the test is cancelled during setup
   */
  @Before
  public final void setUp() throws CancelException {
    connection = initializeConnectionProvider().getConnection();
    doSetUp();
  }

  /**
   * Tears down the database connection
   */
  @After
  public final void tearDown() {
    if (connection != null) {
      connection.disconnect();
    }
    doTearDown();
  }

  /**
   * Runs the insert/update/select/delete tests for the given entityID
   * @param entityID the ID of the entity to test
   * @throws org.jminor.common.db.exception.DatabaseException in case of an exception
   */
  public final void testEntity(final String entityID) throws DatabaseException {
    connection.beginTransaction();
    try {
      initializeReferencedEntities(entityID);
      Entity testEntity = null;
      if (!Entities.isReadOnly(entityID)) {
        testEntity = testInsert(Util.rejectNullValue(initializeTestEntity(entityID), "test entity"));
        testUpdate(testEntity);
      }
      testSelect(entityID, testEntity);
      if (!Entities.isReadOnly(entityID)) {
        testDelete(testEntity);
      }
    }
    finally {
      referencedEntities.clear();
      connection.rollbackTransaction();
    }
  }

  /**
   * Override to provide specific setup for this test
   */
  protected void doSetUp() {}

  /**
   * Override to provide specific tear down for this test
   */
  protected void doTearDown() {}

  /**
   * @return the EntityConnectionProvider instance this test case should use
   * @throws CancelException in case the login was cancelled
   */
  protected EntityConnectionProvider initializeConnectionProvider() throws CancelException {
    return EntityConnectionProviders.createConnectionProvider(getTestUser(), getClass().getName());
  }

  /**
   * Returns the database user to use when running the tests, this default implementation
   * prompts for the user/password information, usually overridden
   * @return the db user to use when running the test
   * @throws CancelException in case the user cancels the login
   */
  protected User getTestUser() throws CancelException {
    return LoginPanel.getUser(null, null);
  }

  /**
   * @return the EntityConnection instance used by this EntityTestUnit
   */
  protected final EntityConnection getConnection() {
    return connection;
  }

  /**
   * @param entityID the entityID of the the reference entity to retrieve
   * @return the entity mapped to the given entityID
   * @see #setReferenceEntity(String, org.jminor.framework.domain.Entity)
   */
  protected final Entity getReferenceEntity(final String entityID) {
    final Entity entity = referencedEntities.get(entityID);
    if (entity == null) {
      throw new IllegalArgumentException("No reference entity available of type " + entityID);
    }

    return entity;
  }

  /**
   * Maps the given reference entity to the given entityID
   * @param entityID the entityID
   * @param entity the reference entity to map to the given entityID
   * @throws org.jminor.common.db.exception.DatabaseException in case of an exception
   * @see #getReferenceEntity(String)
   */
  protected final void setReferenceEntity(final String entityID, final Entity entity) throws DatabaseException {
    if (!entity.is(entityID)) {
      throw new IllegalArgumentException("Reference entity type mismatch: " + entityID + " - " + entity.getEntityID());
    }

    referencedEntities.put(entityID, initialize(entity));
  }

  /**
   * This method should load the domain model, for example by instantiating the domain model
   * class or simply loading it by name
   */
  protected abstract void loadDomainModel();

  /**
   * This method should return an instance of the entity specified by <code>entityID</code>
   * @param entityID the entityID for which to initialize an entity instance
   * @return the entity instance to use for testing the entity type
   */
  protected Entity initializeTestEntity(final String entityID) {
    return EntityUtil.createRandomEntity(entityID, referencedEntities);
  }

  /**
   * Initializes a new Entity of the given type, by default this method creates a Entity filled with random values.
   * @param entityID the entity ID
   * @return a entity of the given type
   */
  protected Entity initializeReferenceEntity(final String entityID) {
    return EntityUtil.createRandomEntity(entityID, referencedEntities);
  }

  /**
   * This method should return <code>testEntity</code> in a modified state
   * @param testEntity the entity to modify
   */
  protected void modifyEntity(final Entity testEntity) {
    EntityUtil.randomize(testEntity, false, referencedEntities);
  }

  /**
   * Initializes the entities referenced by the entity identified by <code>entityID</code>
   * @param entityID the ID of the entity for which to initialize the referenced entities
   * @throws org.jminor.common.db.exception.DatabaseException in case of an exception
   * @see #initializeReferenceEntity(String) (String, org.jminor.framework.domain.Entity)
   */
  @SuppressWarnings({"UnusedDeclaration"})
  private void initializeReferencedEntities(final String entityID) throws DatabaseException {
    boolean referencesSelf = false;
    for (final Property.ForeignKeyProperty foreignKeyProperty : Entities.getForeignKeyProperties(entityID)) {
      final String referencedEntityID = foreignKeyProperty.getReferencedEntityID();
      if (referencedEntityID.equals(entityID)) {
        referencesSelf = true;
      }
      else {
        initializeReferencedEntities(referencedEntityID);
        if (!referencedEntities.containsKey(referencedEntityID)) {
          setReferenceEntity(referencedEntityID, initializeReferenceEntity(referencedEntityID));
        }
      }
    }
    //we initialize the self reference last, to insure that all other referenced entities have been initialized
    if (referencesSelf && !referencedEntities.containsKey(entityID)) {
      setReferenceEntity(entityID, initializeReferenceEntity(entityID));
    }
  }

  /**
   * Tests inserting the given entity
   * @param testEntity the entity to test insert for
   * @return the same entity retrieved from the database after the insert
   * @throws org.jminor.common.db.exception.DatabaseException in case of an exception
   */
  private Entity testInsert(final Entity testEntity) throws DatabaseException {
    final List<Entity.Key> keys = connection.insert(Arrays.asList(testEntity));
    try {
      return connection.selectSingle(keys.get(0));
    }
    catch (RecordNotFoundException e) {
      fail("Inserted entity of type " + testEntity.getEntityID() + " not returned by select after insert");
      throw e;
    }
  }

  /**
   * Tests selecting the given entity, if <code>testEntity</code> is null
   * then selecting many entities is tested.
   * @param entityID the entityID in case <code>testEntity</code> is null
   * @param testEntity the entity to test selecting
   * @throws org.jminor.common.db.exception.DatabaseException in case of an exception
   */
  private void testSelect(final String entityID, final Entity testEntity) throws DatabaseException {
    if (testEntity != null) {
      final Entity tmp = connection.selectSingle(testEntity.getPrimaryKey());
      assertTrue("Entity of type " + testEntity.getEntityID() + " failed equals comparison",
              testEntity.equals(tmp));
    }
    else {
      connection.selectMany(EntityCriteriaUtil.selectCriteria(entityID, 10));
    }
  }

  /**
   * Test updating the given entity, if the entity is not modified this test does nothing
   * @param testEntity the entity to test updating
   * @throws org.jminor.common.db.exception.DatabaseException in case of an exception
   */
  private void testUpdate(final Entity testEntity) throws DatabaseException {
    modifyEntity(testEntity);
    if (!testEntity.isModified()) {
      return;
    }

    connection.update(Arrays.asList(testEntity));

    final Entity tmp = connection.selectSingle(testEntity.getOriginalPrimaryKey());
    assertEquals("Primary keys of entity and its updated counterpart should be equal",
            testEntity.getPrimaryKey(), tmp.getPrimaryKey());
    for (final Property.ColumnProperty property : Entities.getColumnProperties(testEntity.getEntityID())) {
      if (!property.isReadOnly() && property.isUpdatable()) {
        final Object beforeUpdate = testEntity.getValue(property.getPropertyID());
        final Object afterUpdate = tmp.getValue(property.getPropertyID());
        assertTrue("Values of property " + property + " should be equal after update ["
                + beforeUpdate + (beforeUpdate != null ? (" (" + beforeUpdate.getClass() + ")") : "") + ", "
                + afterUpdate + (afterUpdate != null ? (" (" + afterUpdate.getClass() + ")") : "") + "]",
                Util.equal(beforeUpdate, afterUpdate));
      }
    }
  }

  /**
   * Test deleting the given entity
   * @param testEntity the entity to test deleting
   * @throws org.jminor.common.db.exception.DatabaseException in case of an exception
   */
  private void testDelete(final Entity testEntity) throws DatabaseException {
    connection.delete(EntityUtil.getPrimaryKeys(Arrays.asList(testEntity)));

    boolean caught = false;
    try {
      connection.selectSingle(testEntity.getPrimaryKey());
    }
    catch (DatabaseException e) {
      caught = true;
    }
    assertTrue("Entity of type " + testEntity.getEntityID() + " failed delete test", caught);
  }

  /**
   * Initializes the given entity, that is, performs an insert on it in case it doesn't
   * already exist in the database, returns the same entity
   * @param entity the entity to initialize
   * @return the entity
   * @throws org.jminor.common.db.exception.DatabaseException in case of an exception
   */
  private Entity initialize(final Entity entity) throws DatabaseException {
    final List<Entity> entities = connection.selectMany(Arrays.asList(entity.getPrimaryKey()));
    if (!entities.isEmpty()) {
      return entities.get(0);
    }

    return connection.selectSingle(connection.insert(Arrays.asList(entity)).get(0));
  }
}

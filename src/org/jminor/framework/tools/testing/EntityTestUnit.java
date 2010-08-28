/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.tools.testing;

import org.jminor.common.db.exception.DbException;
import org.jminor.common.db.exception.RecordNotFoundException;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.ui.LoginPanel;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityDb;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.db.provider.EntityDbProviderFactory;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class for unit testing domain entities.
 */
public abstract class EntityTestUnit {

  private EntityDbProvider entityDbProvider;
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
    entityDbProvider = initializeDbConnectionProvider();
    doSetUp();
  }

  /**
   * Tears down the database connection
   */
  @After
  public final void tearDown() {
    if (entityDbProvider != null) {
      entityDbProvider.disconnect();
    }
    doTearDown();
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
   * @return the EntityDbProvider instance this test case should use
   * @throws CancelException in case the login was cancelled
   */
  protected EntityDbProvider initializeDbConnectionProvider() throws CancelException {
    return EntityDbProviderFactory.createEntityDbProvider(getTestUser(), getClass().getSimpleName());
  }

  /**
   * Returns the database user to use when running the tests, this default implementation
   * prompts for the user/password information, usually overridden
   * @return the db user to use when running the test
   * @throws CancelException in case the user cancels the login
   */
  protected User getTestUser() throws CancelException {
    return LoginPanel.getUser(null, new User(Configuration.getDefaultUsername(getClass().getName()), null));
  }

  /**
   * @return the EntityDbProvider instance used by this EntityTestUnit
   */
  protected final EntityDbProvider getDbProvider() {
    return entityDbProvider;
  }

  /**
   * @return the EntityDb instance used by this EntityTestUnit
   */
  protected final EntityDb getEntityDb() {
    return entityDbProvider.getEntityDb();
  }

  /**
   * Instantiates an entity of the given type, initializing the foreign key values
   * according the reference entities set via <code>setReferenceEntity</code>
   * @param entityID the ID specifying the entity type to return
   * @return an initialized entity
   * @see #setReferenceEntity(String, org.jminor.framework.domain.Entity)
   */
  protected Entity createEntity(final String entityID) {
    final Entity entity = Entities.entityInstance(entityID);
    for (final Property.ForeignKeyProperty foreignKeyProperty : Entities.getForeignKeyProperties(entityID)) {
      final Entity referenceEntity = referencedEntities.get(foreignKeyProperty.getReferencedEntityID());
      if (referenceEntity != null) {
        entity.setValue(foreignKeyProperty.getPropertyID(), referenceEntity);
      }
    }

    return entity;
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
   * @throws DbException in case of an exception
   * @see #getReferenceEntity(String)
   */
  protected final void setReferenceEntity(final String entityID, final Entity entity) throws DbException {
    if (!entity.is(entityID)) {
      throw new IllegalArgumentException("Reference entity type mismatch: " + entityID + " - " + entity.getEntityID());
    }

    referencedEntities.put(entityID, initialize(entity));
  }

  /**
   * Runs the insert/update/select/delete tests for the given entityID
   * @param entityID the ID of the entity to test
   * @throws DbException in case of an exception
   */
  protected final void testEntity(final String entityID) throws DbException {
    try {
      getEntityDb().beginTransaction();
      initializeReferencedEntities(entityID, entityID);
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
      getEntityDb().rollbackTransaction();
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
   * This method should initialize instances of entities specified by the entityIDs found in the
   * <code>entityIDs</code> Collection and map them to their respective entityIDs via the setReferenceEntity method
   * @param testEntityID the ID of the entity being tested
   * @param entityID the ID of the entity for which to initialize the referenced entities
   * @throws DbException in case of an exception
   * @see #setReferenceEntity(String, org.jminor.framework.domain.Entity)
   */
  @SuppressWarnings({"UnusedDeclaration"})
  protected final void initializeReferencedEntities(final String testEntityID, final String entityID) throws DbException {
    for (final Property.ForeignKeyProperty fkProperty : Entities.getForeignKeyProperties(entityID)) {
      if (!fkProperty.getReferencedEntityID().equals(entityID)) {
        initializeReferencedEntities(testEntityID, fkProperty.getReferencedEntityID());
      }
      if (!referencedEntities.containsKey(fkProperty.getReferencedEntityID())) {
        setReferenceEntity(fkProperty.getReferencedEntityID(), initializeReferenceEntity(fkProperty.getReferencedEntityID()));
      }
    }
  }

  /**
   * Tests inserting the given entity
   * @param testEntity the entity to test insert for
   * @return the same entity retrieved from the database after the insert
   * @throws DbException in case of an exception
   */
  private Entity testInsert(final Entity testEntity) throws DbException {
    final List<Entity.Key> keys = getEntityDb().insert(Arrays.asList(testEntity));
    try {
      return getEntityDb().selectSingle(keys.get(0));
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
   * @throws DbException in case of an exception
   */
  private void testSelect(final String entityID, final Entity testEntity) throws DbException {
    if (testEntity != null) {
      final Entity tmp = getEntityDb().selectSingle(testEntity.getPrimaryKey());
      assertTrue("Entity of type " + testEntity.getEntityID() + " failed equals comparison",
              testEntity.equals(tmp));
    }
    else {
      getEntityDb().selectMany(EntityCriteriaUtil.selectCriteria(entityID, 10));
    }
  }

  /**
   * Test updating the given entity, if the entity is not modified this test does nothing
   * @param testEntity the entity to test updating
   * @throws DbException in case of an exception
   */
  private void testUpdate(final Entity testEntity) throws DbException {
    modifyEntity(testEntity);
    if (!testEntity.isModified()) {
      return;
    }

    getEntityDb().update(Arrays.asList(testEntity));

    final Entity tmp = getEntityDb().selectSingle(testEntity.getOriginalPrimaryKey());
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
   * @throws DbException in case of an exception
   */
  private void testDelete(final Entity testEntity) throws DbException {
    getEntityDb().delete(EntityUtil.getPrimaryKeys(Arrays.asList(testEntity)));

    boolean caught = false;
    try {
      getEntityDb().selectSingle(testEntity.getPrimaryKey());
    }
    catch (DbException e) {
      caught = true;
    }
    assertTrue("Entity of type " + testEntity.getEntityID() + " failed delete test", caught);
  }

  /**
   * Initializes the given entity, that is, performs an insert on it in case it doesn't
   * already exist in the database, returns the same entity
   * @param entity the entity to initialize
   * @return the entity
   * @throws DbException in case of an exception
   */
  private Entity initialize(final Entity entity) throws DbException {
    final List<Entity> entities = getEntityDb().selectMany(Arrays.asList(entity.getPrimaryKey()));
    if (!entities.isEmpty()) {
      return entities.get(0);
    }

    return getEntityDb().selectSingle(getEntityDb().insert(Arrays.asList(entity)).get(0));
  }
}

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
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class for unit testing domain entities.
 */
public abstract class EntityTestUnit {

  private EntityDbProvider entityDbProvider;
  private final Map<String, Entity> referencedEntities = new HashMap<String, Entity>();

  public EntityTestUnit() {
    loadDomainModel();
  }

  @Before
  public void setUp() throws Exception {
    entityDbProvider = initializeDbConnectionProvider();
  }

  @After
  public void tearDown() throws Exception {
    if (entityDbProvider != null) {
      entityDbProvider.disconnect();
    }
  }

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
  protected EntityDbProvider getDbProvider() {
    return entityDbProvider;
  }

  /**
   * @return the EntityDb instance used by this EntityTestUnit
   */
  protected EntityDb getEntityDb() {
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
    final Entity entity = new Entity(entityID);
    for (final Property.ForeignKeyProperty foreignKeyProperty : EntityRepository.getForeignKeyProperties(entityID)) {
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
  protected Entity getReferenceEntity(final String entityID) {
    final Entity entity = referencedEntities.get(entityID);
    if (entity == null) {
      throw new RuntimeException("No reference entity available of type " + entityID);
    }

    return entity;
  }

  /**
   * Maps the given reference entity to the given entityID
   * @param entityID the entityID
   * @param entity the reference entity to map to the given entityID
   * @throws Exception in case of an exception
   * @see #getReferenceEntity(String)
   */
  protected void setReferenceEntity(final String entityID, final Entity entity) throws Exception {
    if (!entity.is(entityID)) {
      throw new IllegalArgumentException("Reference entity type mismatch: " + entityID + " - " + entity.getEntityID());
    }

    referencedEntities.put(entityID, initialize(entity));
  }

  /**
   * Runs the insert/update/select/delete tests for the given entityID
   * @param entityID the ID of the entity to test
   * @throws Exception in case of an exception
   */
  protected void testEntity(final String entityID) throws Exception {
    try {
      getEntityDb().beginTransaction();
      initializeReferenceEntities(addAllReferencedEntityIDs(entityID, new ArrayList<String>()));
      Entity testEntity = null;
      if (!EntityRepository.isReadOnly(entityID)) {
        final Entity initialEntity = initializeTestEntity(entityID);
        if (initialEntity == null) {
          throw new Exception("No test entity provided " + entityID);
        }

        testEntity = testInsert(initialEntity);
        testUpdate(testEntity);
      }
      testSelect(entityID, testEntity);
      if (!EntityRepository.isReadOnly(entityID)) {
        testDelete(testEntity);
      }
    }
    finally {
      referencedEntities.clear();
      getEntityDb().rollbackTransaction();
    }
  }

  /**
   * Tests inserting the given entity
   * @param testEntity the entity to test insert for
   * @return the same entity retrieved from the database after the insert
   * @throws Exception in case of an exception
   */
  protected Entity testInsert(final Entity testEntity) throws Exception {
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
   * @throws Exception in case of an exception
   */
  protected void testSelect(final String entityID, final Entity testEntity) throws Exception {
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
   * @throws Exception in case of an exception
   */
  protected void testUpdate(final Entity testEntity) throws Exception {
    modifyEntity(testEntity);
    if (!testEntity.isModified()) {
      return;
    }

    getEntityDb().update(Arrays.asList(testEntity));

    final Entity tmp = getEntityDb().selectSingle(testEntity.getOriginalPrimaryKey());
    assertEquals("Primary keys of entity and its updated counterpart should be equal",
            testEntity.getPrimaryKey(), tmp.getPrimaryKey());
    for (final Property property : EntityRepository.getProperties(testEntity.getEntityID()).values()) {
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
   * @throws Exception in case of an exception
   */
  protected void testDelete(final Entity testEntity) throws Exception {
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
   * This method should load the domain model, for example by instantiating the domain model
   * class or simply loading it by name
   */
  protected abstract void loadDomainModel();

  /**
   * This method should return an instance of the entity specified by <code>entityID</code>
   * @param entityID the entityID for which to initialize an entity instance
   * @return an entity
   * @throws Exception in case of an exception
   */
  protected Entity initializeTestEntity(final String entityID) throws Exception {
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
   * @param referenceEntityIDs the IDs of the entities that should be initialized
   * @throws Exception in case of an exception
   * @see #setReferenceEntity(String, org.jminor.framework.domain.Entity)
   */
  @SuppressWarnings({"UnusedDeclaration"})
  protected void initializeReferenceEntities(final List<String> referenceEntityIDs) throws Exception {
    Collections.sort(referenceEntityIDs, new Comparator<String>() {
      public int compare(final String o1, final String o2) {
        if (superTreeContains(o1, o2)) {
          return 1;
        }
        else if (superTreeContains(o2, o1)) {
          return -1;
        }
        return 0;
      }
    });
    for (final String entityID : referenceEntityIDs) {
      setReferenceEntity(entityID, createReferenceEntity(entityID));
    }
  }

  /**
   * Initializes a new Entity of the given type, by default this method creates a Entity filled with random values.
   * @param entityID the entity ID
   * @return a entity of the given type
   * @throws Exception in case of an exception
   */
  protected Entity createReferenceEntity(final String entityID) throws Exception {
    return EntityUtil.createRandomEntity(entityID, referencedEntities);
  }

  /**
   * Initializes the given entity, that is, performs an insert on it in case it doesn't
   * already exist in the database, returns the same entity
   * @param entity the entity to initialize
   * @return the entity
   * @throws Exception in case of an exception
   */
  private Entity initialize(final Entity entity) throws Exception {
    final List<Entity> entities = getEntityDb().selectMany(Arrays.asList(entity.getPrimaryKey()));
    if (entities.size() > 0) {
      return entities.get(0);
    }

    return getEntityDb().selectSingle(getEntityDb().insert(Arrays.asList(entity)).get(0));
  }

  /**
   * Adds all entityIDs referenced via foreign keys by the entity identified by <code>entityID</code> to <code>container</code>
   * @param entityID the entityID
   * @param container the container
   * @return the container
   */
  private List<String> addAllReferencedEntityIDs(final String entityID, final List<String> container) {
    for (final Property.ForeignKeyProperty foreignKeyProperty : EntityRepository.getForeignKeyProperties(entityID)) {
      final String referenceEntityID = foreignKeyProperty.getReferencedEntityID();
      if (referenceEntityID != null && !container.contains(referenceEntityID)) {
        container.add(referenceEntityID);
        addAllReferencedEntityIDs(referenceEntityID, container);
      }
    }
    return container;
  }

  private static boolean superTreeContains(final String root, final String entityID) {
    Util.rejectNullValue(root, "root");
    Util.rejectNullValue(entityID, "entityID");
    if (root.equals(entityID)) {
      return true;
    }

    final Collection<Property.ForeignKeyProperty> foreignKeyProperties = EntityRepository.getForeignKeyProperties(root);
    if (foreignKeyProperties.size() == 0) {
      return false;
    }

    for (final Property.ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
      final String referencedEntityID = foreignKeyProperty.getReferencedEntityID();
      if (referencedEntityID.equals(root) || referencedEntityID.equals(entityID) || superTreeContains(referencedEntityID, entityID)) {
        return true;
      }
    }

    return false;
  }
}

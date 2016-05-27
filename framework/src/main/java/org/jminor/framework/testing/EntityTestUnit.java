/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.testing;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.exception.RecordNotFoundException;
import org.jminor.common.model.DateUtil;
import org.jminor.common.model.Item;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.ValueProvider;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.EntityConnectionProviders;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;

import org.junit.After;
import org.junit.Before;

import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * A class for unit testing domain entities.
 */
public abstract class EntityTestUnit {

  private static final int MININUM_RANDOM_NUMBER = -10000000;
  private static final int MAXIMUM_RANDOM_NUMBER = 10000000;
  private static final int MAXIMUM_RANDOM_STRING_LENGTH = 10;
  private static final int SELECT_FETCH_COUNT = 10;
  private static final Random RANDOM = new Random();
  private static final String ENTITY_PARAM = "entity";

  private EntityConnection connection;
  private final Map<String, Entity> referencedEntities = new HashMap<>();

  /**
   * Instantiates a new EntityTestUnit.
   */
  public EntityTestUnit() {
    loadDomainModel();
  }

  /**
   * Sets up the database connection
   */
  @Before
  public final void setUp() {
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
      initializeReferencedEntities(entityID, new HashSet<>());
      Entity testEntity = null;
      if (!Entities.isReadOnly(entityID)) {
        testEntity = testInsert(Objects.requireNonNull(initializeTestEntity(entityID), "test entity"));
        assertNotNull(testEntity.toString());
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
   * @param entityID the entity ID
   * @param referenceEntities entities referenced by the given entity ID
   * @return a Entity instance containing randomized values, based on the property definitions
   */
  public static Entity createRandomEntity(final String entityID, final Map<String, Entity> referenceEntities) {
    return createEntity(entityID, new ValueProvider<Property, Object>() {
      @Override
      public Object get(final Property property) {
        return getRandomValue(property, referenceEntities);
      }
    });
  }

  /**
   * @param entityID the entity ID
   * @param valueProvider the value provider
   * @return an Entity instance initialized with values provided by the given value provider
   */
  public static Entity createEntity(final String entityID, final ValueProvider<Property, Object> valueProvider) {
    final Entity entity = Entities.entity(entityID);
    populateEntity(entity, Entities.getColumnProperties(entityID, !Entities.isPrimaryKeyAutoGenerated(entityID),
            false, true), valueProvider);

    return entity;
  }

  /**
   * Randomizes the values in the given entity, note that if a reference entity is not provided
   * the respective foreign key value in not modified
   * @param entity the entity to randomize
   * @param includePrimaryKey if true then the primary key values are include
   * @param referenceEntities entities referenced by the given entity via foreign keys
   * @return the entity with randomized values
   */
  public static Entity randomize(final Entity entity, final boolean includePrimaryKey, final Map<String, Entity> referenceEntities) {
    Objects.requireNonNull(entity, ENTITY_PARAM);
    populateEntity(entity, Entities.getColumnProperties(entity.getEntityID(), includePrimaryKey, false, true),
            new ValueProvider<Property, Object>() {
              @Override
              public Object get(final Property property) {
                return getRandomValue(property, referenceEntities);
              }
            });

    return entity;
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
   */
  protected EntityConnectionProvider initializeConnectionProvider() {
    return EntityConnectionProviders.connectionProvider(getTestUser(), getClass().getName());
  }

  /**
   * Returns the database user to use when running the tests, this default implementation
   * returns a user based on the {@link User#UNITTEST_USERNAME_PROPERTY}
   * and {@link User#UNITTEST_PASSWORD_PROPERTY} properties.
   * @return the db user to use when running the test
   */
  protected User getTestUser() {
    return User.UNIT_TEST_USER;
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
    if (entity != null && !entity.is(entityID)) {
      throw new IllegalArgumentException("Reference entity type mismatch: " + entityID + " - " + entity.getEntityID());
    }

    referencedEntities.put(entityID, entity == null ? null : insertOrSelect(entity));
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
    return createRandomEntity(entityID, referencedEntities);
  }

  /**
   * Initializes a new Entity of the given type, by default this method creates a Entity filled with random values.
   * @param entityID the entity ID
   * @return a entity of the given type
   */
  protected Entity initializeReferenceEntity(final String entityID) {
    return createRandomEntity(entityID, referencedEntities);
  }

  /**
   * This method should return <code>testEntity</code> in a modified state
   * @param testEntity the entity to modify
   */
  protected void modifyEntity(final Entity testEntity) {
    randomize(testEntity, false, referencedEntities);
  }

  /**
   * Initializes the entities referenced by the entity identified by <code>entityID</code>
   * @param entityID the ID of the entity for which to initialize the referenced entities
   * @param visited the entityIDs already visited
   * @throws org.jminor.common.db.exception.DatabaseException in case of an exception
   * @see #initializeReferenceEntity(String) (String, org.jminor.framework.domain.Entity)
   */
  @SuppressWarnings({"UnusedDeclaration"})
  private void initializeReferencedEntities(final String entityID, final Collection<String> visited) throws DatabaseException {
    visited.add(entityID);
    final List<Property.ForeignKeyProperty> foreignKeyProperties = new ArrayList<>(Entities.getForeignKeyProperties(entityID));
    Collections.sort(foreignKeyProperties, new Comparator<Property.ForeignKeyProperty>() {
      //we initialize the self references last, to insure that all required reference entities have been initialized
      @Override//before trying to initialize an instance of this entity
      public int compare(final Property.ForeignKeyProperty o1, final Property.ForeignKeyProperty o2) {
        return o1.getReferencedEntityID().equals(entityID) ? 1 : 0;
      }
    });
    for (final Property.ForeignKeyProperty foreignKeyProperty : Entities.getForeignKeyProperties(entityID)) {
      final String referencedEntityID = foreignKeyProperty.getReferencedEntityID();
      if (!visited.contains(referencedEntityID)) {
        initializeReferencedEntities(referencedEntityID, visited);
      }
      if (!referencedEntities.containsKey(referencedEntityID)) {
        setReferenceEntity(referencedEntityID, initializeReferenceEntity(referencedEntityID));
      }
    }
  }

  /**
   * Tests inserting the given entity
   * @param testEntity the entity to test insert for
   * @return the same entity retrieved from the database after the insert
   * @throws org.jminor.common.db.exception.DatabaseException in case of an exception
   */
  private Entity testInsert(final Entity testEntity) throws DatabaseException {
    final List<Entity.Key> keys = connection.insert(Collections.singletonList(testEntity));
    try {
      return connection.selectSingle(keys.get(0));
    }
    catch (final RecordNotFoundException e) {
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
      final Entity tmp = connection.selectSingle(testEntity.getKey());
      assertTrue("Entity of type " + testEntity.getEntityID() + " failed equals comparison",
              testEntity.equals(tmp));
    }
    else {
      connection.selectMany(EntityCriteriaUtil.selectCriteria(entityID, SELECT_FETCH_COUNT));
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

    connection.update(Collections.singletonList(testEntity));

    final Entity tmp = connection.selectSingle(testEntity.getOriginalKey());
    assertEquals("Keys of entity and its updated counterpart should be equal",
            testEntity.getKey(), tmp.getKey());
    for (final Property.ColumnProperty property : Entities.getColumnProperties(testEntity.getEntityID())) {
      if (!property.isReadOnly() && property.isUpdatable()) {
        final Object beforeUpdate = testEntity.get(property.getPropertyID());
        final Object afterUpdate = tmp.get(property.getPropertyID());
        assertTrue("Values of property " + property + " should be equal after update ["
                + beforeUpdate + (beforeUpdate != null ? (" (" + beforeUpdate.getClass() + ")") : "") + ", "
                + afterUpdate + (afterUpdate != null ? (" (" + afterUpdate.getClass() + ")") : "") + "]",
                Objects.equals(beforeUpdate, afterUpdate));
      }
    }
  }

  /**
   * Test deleting the given entity
   * @param testEntity the entity to test deleting
   * @throws org.jminor.common.db.exception.DatabaseException in case of an exception
   */
  private void testDelete(final Entity testEntity) throws DatabaseException {
    connection.delete(EntityUtil.getKeys(Collections.singletonList(testEntity)));

    boolean caught = false;
    try {
      connection.selectSingle(testEntity.getKey());
    }
    catch (final DatabaseException e) {
      caught = true;
    }
    assertTrue("Entity of type " + testEntity.getEntityID() + " failed delete test", caught);
  }

  /**
   * Inserts or selects the given entity if it exists and returns the result
   * @param entity the entity to initialize
   * @return the entity
   * @throws DatabaseException in case of an exception
   */
  private Entity insertOrSelect(final Entity entity) throws DatabaseException {
    if (!entity.isKeyNull()) {
      final List<Entity> entities = connection.selectMany(Collections.singletonList(entity.getKey()));
      if (!entities.isEmpty()) {
        return entities.get(0);
      }
    }

    return connection.selectSingle(connection.insert(Collections.singletonList(entity)).get(0));
  }

  /**
   * @param property the property
   * @param referenceEntities entities referenced by the given property
   * @return a random value
   */
  private static Object getRandomValue(final Property property, final Map<String, Entity> referenceEntities) {
    Objects.requireNonNull(property, "property");
    if (property instanceof Property.ForeignKeyProperty) {
      return getReferenceEntity((Property.ForeignKeyProperty) property, referenceEntities);
    }
    if (property instanceof Property.ValueListProperty) {
      return getRandomListValue((Property.ValueListProperty) property);
    }
    switch (property.getType()) {
      case Types.BOOLEAN:
        return RANDOM.nextBoolean();
      case Types.CHAR:
        return (char) RANDOM.nextInt();
      case Types.DATE:
        return DateUtil.floorDate(getRandomDate());
      case Types.TIMESTAMP:
        return DateUtil.floorTimestamp(new Timestamp(getRandomDate().getTime()));
      case Types.TIME:
        return DateUtil.floorTime(getRandomDate());
      case Types.DOUBLE:
        return getRandomDouble(property);
      case Types.INTEGER:
        return getRandomInteger(property);
      case Types.BIGINT:
        return (long) getRandomInteger(property);
      case Types.VARCHAR:
        return getRandomString(property);
      default:
        return null;
    }
  }

  private static void populateEntity(final Entity entity, final Collection<Property.ColumnProperty> properties,
                                     final ValueProvider<Property, Object> valueProvider) {
    for (final Property.ColumnProperty property : properties) {
      if (!property.isForeignKeyProperty() && !property.isDenormalized()) {
        entity.put(property, valueProvider.get(property));
      }
    }
    for (final Property.ForeignKeyProperty property : Entities.getForeignKeyProperties(entity.getEntityID())) {
      final Object value = valueProvider.get(property);
      if (value != null) {
        entity.put(property, value);
      }
    }
  }

  private static String getRandomString(final Property property) {
    final int length = property.getMaxLength() < 0 ? MAXIMUM_RANDOM_STRING_LENGTH : property.getMaxLength();

    return Util.createRandomString(length, length);
  }

  private static Date getRandomDate() {
    final Calendar calendar = Calendar.getInstance();
    final long offset = calendar.getTimeInMillis();
    calendar.add(Calendar.YEAR, -1);
    final long end = System.currentTimeMillis();
    final long diff = end - offset + 1;

    return new Timestamp(offset + (long) (Math.random() * diff));
  }

  private static Object getReferenceEntity(final Property.ForeignKeyProperty property, final Map<String, Entity> referenceEntities) {
    final String referenceEntityID = property.getReferencedEntityID();

    return referenceEntities == null ? null : referenceEntities.get(referenceEntityID);
  }

  private static Object getRandomListValue(final Property.ValueListProperty property) {
    final List<Item> items = property.getValues();
    final Item item = items.get(RANDOM.nextInt(items.size()));

    return item.getItem();
  }

  private static int getRandomInteger(final Property property) {
    final int min = (int) (property.getMin() == null ? MININUM_RANDOM_NUMBER : property.getMin());
    final int max = (int) (property.getMax() == null ? MAXIMUM_RANDOM_NUMBER : property.getMax());

    return RANDOM.nextInt((max - min) + 1) + min;
  }

  private static double getRandomDouble(final Property property) {
    final double min = property.getMin() == null ? MININUM_RANDOM_NUMBER : property.getMin();
    final double max = property.getMax() == null ? MAXIMUM_RANDOM_NUMBER : property.getMax();

    return Util.roundDouble((RANDOM.nextDouble() * (max - min)) + min, property.getMaximumFractionDigits());
  }
}

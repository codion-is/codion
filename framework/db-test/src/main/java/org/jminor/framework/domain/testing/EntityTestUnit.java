/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.testing;

import org.jminor.common.Item;
import org.jminor.common.TextUtil;
import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.exception.RecordNotFoundException;
import org.jminor.common.db.valuemap.ValueProvider;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.EntityConnectionProviders;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A class for unit testing domain entities.
 */
public class EntityTestUnit {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  private static final int MININUM_RANDOM_NUMBER = -10000000;
  private static final int MAXIMUM_RANDOM_NUMBER = 10000000;
  private static final int MAXIMUM_RANDOM_STRING_LENGTH = 10;
  private static final int SELECT_FETCH_COUNT = 10;
  private static final Random RANDOM = new Random();
  private static final String ENTITY_PARAM = "entity";

  private final String domainClass;
  private final Map<String, Entity> referencedEntities = new HashMap<>();

  private EntityConnection connection;
  private Domain domain;
  private EntityConditions conditions;

  /**
   * Instantiates a new EntityTestUnit.
   * @param domainClass the name of the domain model class
   */
  public EntityTestUnit(final String domainClass) {
    this.domainClass = domainClass;
  }

  /**
   * @return the domain model
   */
  public final Domain getDomain() {
    if (domain == null) {
      domain = connection.getDomain();
    }

    return domain;
  }

  /**
   * @return the domain conditions
   */
  public final EntityConditions getConditions() {
    if (conditions == null) {
      conditions = new EntityConditions(getDomain());
    }

    return conditions;
  }

  /**
   * Sets up the database connection
   */
  @BeforeEach
  public final void setUp() {
    connection = initializeConnectionProvider().getConnection();
    doSetUp();
  }

  /**
   * Tears down the database connection
   */
  @AfterEach
  public final void tearDown() {
    if (connection != null) {
      connection.disconnect();
    }
    doTearDown();
  }

  /**
   * Runs the insert/update/select/delete tests for the given entityId
   * @param entityId the ID of the entity to test
   * @throws org.jminor.common.db.exception.DatabaseException in case of an exception
   */
  public final void testEntity(final String entityId) throws DatabaseException {
    connection.beginTransaction();
    try {
      initializeReferencedEntities(entityId, new HashSet<>());
      Entity testEntity = null;
      if (!getDomain().isReadOnly(entityId)) {
        testEntity = testInsert(Objects.requireNonNull(initializeTestEntity(entityId), "test entity"));
        assertNotNull(testEntity.toString());
        testUpdate(testEntity);
      }
      testSelect(entityId, testEntity);
      if (!getDomain().isReadOnly(entityId)) {
        testDelete(testEntity);
      }
    }
    finally {
      referencedEntities.clear();
      connection.rollbackTransaction();
    }
  }

  /**
   * @param domain the domain model
   * @param entityId the entity ID
   * @param referenceEntities entities referenced by the given entity ID
   * @return a Entity instance containing randomized values, based on the property definitions
   */
  public static Entity createRandomEntity(final Domain domain, final String entityId, final Map<String, Entity> referenceEntities) {
    return createEntity(domain, entityId, property -> getRandomValue(property, referenceEntities));
  }

  /**
   * @param domain the domain model
   * @param entityId the entity ID
   * @param valueProvider the value provider
   * @return an Entity instance initialized with values provided by the given value provider
   */
  public static Entity createEntity(final Domain domain, final String entityId, final ValueProvider<Property, Object> valueProvider) {
    final Entity entity = domain.entity(entityId);
    populateEntity(domain, entity, domain.getColumnProperties(entityId, !domain.isPrimaryKeyAutoGenerated(entityId),
            false, true), valueProvider);

    return entity;
  }

  /**
   * Randomizes the values in the given entity, note that if a reference entity is not provided
   * the respective foreign key value in not modified
   * @param domain the domain model
   * @param entity the entity to randomize
   * @param includePrimaryKey if true then the primary key values are include
   * @param referenceEntities entities referenced by the given entity via foreign keys
   * @return the entity with randomized values
   */
  public static Entity randomize(final Domain domain, final Entity entity, final boolean includePrimaryKey,
                                 final Map<String, Entity> referenceEntities) {
    Objects.requireNonNull(entity, ENTITY_PARAM);
    populateEntity(domain, entity, domain.getColumnProperties(entity.getEntityId(), includePrimaryKey, false, true),
            property -> getRandomValue(property, referenceEntities));

    return entity;
  }

  /**
   * Override to provide specific setup for this test
   */
  protected void doSetUp() {/*For overriding*/}

  /**
   * Override to provide specific tear down for this test
   */
  protected void doTearDown() {/*For overriding*/}

  /**
   * @return the EntityConnectionProvider instance this test case should use
   */
  protected EntityConnectionProvider initializeConnectionProvider() {
    return EntityConnectionProviders.connectionProvider().setDomainClassName(domainClass)
            .setClientTypeId(getClass().getName()).setUser(getTestUser());
  }

  /**
   * Returns the database user to use when running the tests, this default implementation returns
   * a user based on the "jminor.unittest.username" and "jminor.unittest.password" system properties.
   * @return the db user to use when running the test
   */
  protected User getTestUser() {
    return UNIT_TEST_USER;
  }

  /**
   * @return the EntityConnection instance used by this EntityTestUnit
   */
  protected final EntityConnection getConnection() {
    return connection;
  }

  /**
   * @param entityId the entityId of the the reference entity to retrieve
   * @return the entity mapped to the given entityId
   * @see #setReferenceEntity(String, org.jminor.framework.domain.Entity)
   */
  protected final Entity getReferenceEntity(final String entityId) {
    final Entity entity = referencedEntities.get(entityId);
    if (entity == null) {
      throw new IllegalArgumentException("No reference entity available of type " + entityId);
    }

    return entity;
  }

  /**
   * Maps the given reference entity to the given entityId
   * @param entityId the entityId
   * @param entity the reference entity to map to the given entityId
   * @throws org.jminor.common.db.exception.DatabaseException in case of an exception
   * @see #getReferenceEntity(String)
   */
  protected final void setReferenceEntity(final String entityId, final Entity entity) throws DatabaseException {
    if (entity != null && !entity.is(entityId)) {
      throw new IllegalArgumentException("Reference entity type mismatch: " + entityId + " - " + entity.getEntityId());
    }

    referencedEntities.put(entityId, entity == null ? null : insertOrSelect(entity));
  }

  /**
   * This method should return an instance of the entity specified by {@code entityId}
   * @param entityId the entityId for which to initialize an entity instance
   * @return the entity instance to use for testing the entity type
   */
  protected Entity initializeTestEntity(final String entityId) {
    return createRandomEntity(getDomain(), entityId, referencedEntities);
  }

  /**
   * Initializes a new Entity of the given type, by default this method creates a Entity filled with random values.
   * @param entityId the entity ID
   * @return a entity of the given type
   */
  protected Entity initializeReferenceEntity(final String entityId) {
    return createRandomEntity(getDomain(), entityId, referencedEntities);
  }

  /**
   * This method should return {@code testEntity} in a modified state
   * @param testEntity the entity to modify
   */
  protected void modifyEntity(final Entity testEntity) {
    randomize(getDomain(), testEntity, false, referencedEntities);
  }

  /**
   * Initializes the entities referenced by the entity identified by {@code entityId}
   * @param entityId the ID of the entity for which to initialize the referenced entities
   * @param visited the entityIds already visited
   * @throws org.jminor.common.db.exception.DatabaseException in case of an exception
   * @see #initializeReferenceEntity(String) (String, org.jminor.framework.domain.Entity)
   */
  @SuppressWarnings({"UnusedDeclaration"})
  private void initializeReferencedEntities(final String entityId, final Collection<String> visited) throws DatabaseException {
    visited.add(entityId);
    final List<Property.ForeignKeyProperty> foreignKeyProperties = new ArrayList<>(getDomain().getForeignKeyProperties(entityId));
    foreignKeyProperties.sort((o1, o2) -> o1.getForeignEntityId().equals(entityId) ? 1 : 0);
    for (final Property.ForeignKeyProperty foreignKeyProperty : getDomain().getForeignKeyProperties(entityId)) {
      final String foreignEntityId = foreignKeyProperty.getForeignEntityId();
      if (!visited.contains(foreignEntityId)) {
        initializeReferencedEntities(foreignEntityId, visited);
      }
      if (!referencedEntities.containsKey(foreignEntityId)) {
        setReferenceEntity(foreignEntityId, initializeReferenceEntity(foreignEntityId));
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
      fail("Inserted entity of type " + testEntity.getEntityId() + " not returned by select after insert");
      throw e;
    }
  }

  /**
   * Tests selecting the given entity, if {@code testEntity} is null
   * then selecting many entities is tested.
   * @param entityId the entityId in case {@code testEntity} is null
   * @param testEntity the entity to test selecting
   * @throws org.jminor.common.db.exception.DatabaseException in case of an exception
   */
  private void testSelect(final String entityId, final Entity testEntity) throws DatabaseException {
    if (testEntity != null) {
      final Entity tmp = connection.selectSingle(testEntity.getKey());
      assertEquals(testEntity, tmp, "Entity of type " + testEntity.getEntityId() + " failed equals comparison");
    }
    else {
      connection.selectMany(getConditions().selectCondition(entityId, SELECT_FETCH_COUNT));
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
    assertEquals(testEntity.getKey(), tmp.getKey());
    for (final Property.ColumnProperty property : getDomain().getColumnProperties(testEntity.getEntityId())) {
      if (!property.isReadOnly() && property.isUpdatable()) {
        final Object beforeUpdate = testEntity.get(property);
        final Object afterUpdate = tmp.get(property);
        assertEquals(beforeUpdate, afterUpdate, "Values of property " + property + " should be equal after update ["
                + beforeUpdate + (beforeUpdate != null ? (" (" + beforeUpdate.getClass() + ")") : "") + ", "
                + afterUpdate + (afterUpdate != null ? (" (" + afterUpdate.getClass() + ")") : "") + "]");
      }
    }
  }

  /**
   * Test deleting the given entity
   * @param testEntity the entity to test deleting
   * @throws org.jminor.common.db.exception.DatabaseException in case of an exception
   */
  private void testDelete(final Entity testEntity) throws DatabaseException {
    connection.delete(Entities.getKeys(Collections.singletonList(testEntity)));

    boolean caught = false;
    try {
      connection.selectSingle(testEntity.getKey());
    }
    catch (final DatabaseException e) {
      caught = true;
    }
    assertTrue(caught, "Entity of type " + testEntity.getEntityId() + " failed delete test");
  }

  /**
   * Inserts or selects the given entity if it exists and returns the result
   * @param entity the entity to initialize
   * @return the entity
   * @throws DatabaseException in case of an exception
   */
  private Entity insertOrSelect(final Entity entity) throws DatabaseException {
    if (!entity.isKeyNull()) {
      final List<Entity> selected = connection.selectMany(Collections.singletonList(entity.getKey()));
      if (!selected.isEmpty()) {
        return selected.get(0);
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
        return LocalDate.now();
      case Types.TIMESTAMP:
        return LocalDateTime.now();
      case Types.TIME:
        return LocalTime.now();
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

  private static void populateEntity(final Domain domain, final Entity entity, final Collection<Property.ColumnProperty> properties,
                                     final ValueProvider<Property, Object> valueProvider) {
    for (final Property.ColumnProperty property : properties) {
      if (!property.isForeignKeyProperty() && !property.isDenormalized()) {
        entity.put(property, valueProvider.get(property));
      }
    }
    for (final Property.ForeignKeyProperty property : domain.getForeignKeyProperties(entity.getEntityId())) {
      final Object value = valueProvider.get(property);
      if (value != null) {
        entity.put(property, value);
      }
    }
  }

  private static String getRandomString(final Property property) {
    final int length = property.getMaxLength() < 0 ? MAXIMUM_RANDOM_STRING_LENGTH : property.getMaxLength();

    return TextUtil.createRandomString(length, length);
  }

  private static Object getReferenceEntity(final Property.ForeignKeyProperty property, final Map<String, Entity> referenceEntities) {
    final String referenceEntityId = property.getForeignEntityId();

    return referenceEntities == null ? null : referenceEntities.get(referenceEntityId);
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

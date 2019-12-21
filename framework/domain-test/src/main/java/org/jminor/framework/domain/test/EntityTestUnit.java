/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.test;

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
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityDefinition;
import org.jminor.framework.domain.property.BlobProperty;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;
import org.jminor.framework.domain.property.ValueListProperty;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.jminor.framework.db.condition.Conditions.entitySelectCondition;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A class for unit testing a domain model.
 */
public class EntityTestUnit {

  private static final Logger LOG = LoggerFactory.getLogger(EntityTestUnit.class);

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

  private EntityConnection connection;
  private Domain domain;

  /**
   * Instantiates a new EntityTestUnit.
   * @param domainClass the name of the domain model class
   * @throws NullPointerException in case domainClass is null
   */
  public EntityTestUnit(final String domainClass) {
    this.domainClass = requireNonNull(domainClass, "domainClass");
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
  public final void test(final String entityId) throws DatabaseException {
    try {
      connection.beginTransaction();
      final Map<String, Entity> foreignKeyEntities = initializeReferencedEntities(entityId, new HashMap<>());
      Entity testEntity = null;
      final EntityDefinition entityDefinition = getDomain().getDefinition(entityId);
      if (!entityDefinition.isReadOnly()) {
        testEntity = testInsert(requireNonNull(initializeTestEntity(entityId, foreignKeyEntities), "test entity"));
        assertFalse(testEntity.getKey().isNull());
        testUpdate(testEntity, initializeReferencedEntities(entityId, foreignKeyEntities));
      }
      testSelect(entityId, testEntity);
      if (!entityDefinition.isReadOnly()) {
        testDelete(testEntity);
      }
    }
    finally {
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
    populateEntity(domain, entity, domain.getDefinition(entityId).getWritableColumnProperties(
            !domain.getDefinition(entityId).isKeyGenerated(), true), valueProvider);

    return entity;
  }

  /**
   * Randomizes the values in the given entity, note that if a foreign key entity is not provided
   * the respective foreign key value in not modified
   * @param domain the domain model
   * @param entity the entity to randomize
   * @param foreignKeyEntities the entities referenced via foreign keys
   */
  public static void randomize(final Domain domain, final Entity entity, final Map<String, Entity> foreignKeyEntities) {
    requireNonNull(entity, ENTITY_PARAM);
    populateEntity(domain, entity,
            domain.getDefinition(entity.getEntityId()).getWritableColumnProperties(false, true),
            property -> getRandomValue(property, foreignKeyEntities));
  }

  /**
   * @param property the property
   * @param referenceEntities entities referenced by the given property
   * @return a random value
   */
  public static Object getRandomValue(final Property property, final Map<String, Entity> referenceEntities) {
    requireNonNull(property, "property");
    if (property instanceof ForeignKeyProperty) {
      return getReferenceEntity((ForeignKeyProperty) property, referenceEntities);
    }
    if (property instanceof ValueListProperty) {
      return getRandomListValue((ValueListProperty) property);
    }
    switch (property.getType()) {
      case Types.BOOLEAN:
        return RANDOM.nextBoolean();
      case Types.CHAR:
        return (char) RANDOM.nextInt();
      case Types.DATE:
        return LocalDate.now();
      case Types.TIMESTAMP:
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
      case Types.TIME:
        return LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
      case Types.DOUBLE:
        return getRandomDouble(property);
      case Types.DECIMAL:
        return BigDecimal.valueOf(getRandomDouble(property));
      case Types.INTEGER:
        return getRandomInteger(property);
      case Types.BIGINT:
        return (long) getRandomInteger(property);
      case Types.VARCHAR:
        return getRandomString(property);
      case Types.BLOB: {
        if ((property instanceof BlobProperty) && ((BlobProperty) property).isEagerlyLoaded()) {
          return getRandomBlob(1024);
        }
        //fallthrough to null
      }
      default:
        return null;
    }
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
   * This method should return an instance of the entity specified by {@code entityId}
   * @param entityId the entityId for which to initialize an entity instance
   * @param foreignKeyEntities the entities referenced via foreign keys
   * @return the entity instance to use for testing the entity type
   */
  protected Entity initializeTestEntity(final String entityId, final Map<String, Entity> foreignKeyEntities) {
    return createRandomEntity(getDomain(), entityId, foreignKeyEntities);
  }

  /**
   * Initializes a new Entity of the given type, by default this method creates a Entity filled with random values.
   * @param entityId the entity ID
   * @param foreignKeyEntities the entities referenced via foreign keys
   * @return a entity of the given type
   */
  protected Entity initializeReferenceEntity(final String entityId, final Map<String, Entity> foreignKeyEntities) {
    return createRandomEntity(getDomain(), entityId, foreignKeyEntities);
  }

  /**
   * This method should return {@code testEntity} in a modified state
   * @param testEntity the entity to modify
   * @param foreignKeyEntities the entities referenced via foreign keys
   */
  protected void modifyEntity(final Entity testEntity, final Map<String, Entity> foreignKeyEntities) {
    randomize(getDomain(), testEntity, foreignKeyEntities);
  }

  /**
   * Initializes the entities referenced by the entity identified by {@code entityId}
   * @param entityId the ID of the entity for which to initialize the referenced entities
   * @param foreignKeyEntities foreign key entities already created
   * @throws org.jminor.common.db.exception.DatabaseException in case of an exception
   * @see #initializeReferenceEntity(String, Map)
   * @return the Entities to reference mapped to their respective foreign key propertyIds
   */
  private Map<String, Entity> initializeReferencedEntities(final String entityId, final Map<String, Entity> foreignKeyEntities)
          throws DatabaseException {
    for (final ForeignKeyProperty foreignKeyProperty : getDomain().getDefinition(entityId).getForeignKeyProperties()) {
      final String foreignEntityId = foreignKeyProperty.getForeignEntityId();
      if (!foreignKeyEntities.containsKey(foreignEntityId)) {
        if (!Objects.equals(entityId, foreignEntityId)) {
          foreignKeyEntities.put(foreignEntityId, null);//short circuit recursion, value replaced below
          initializeReferencedEntities(foreignEntityId, foreignKeyEntities);
        }
        final Entity referencedEntity = initializeReferenceEntity(foreignEntityId, foreignKeyEntities);
        if (referencedEntity != null) {
          foreignKeyEntities.put(foreignEntityId, insertOrSelect(referencedEntity));
        }
      }
    }

    return foreignKeyEntities;
  }

  /**
   * Tests inserting the given entity
   * @param testEntity the entity to test insert for
   * @return the same entity retrieved from the database after the insert
   * @throws org.jminor.common.db.exception.DatabaseException in case of an exception
   */
  private Entity testInsert(final Entity testEntity) throws DatabaseException {
    final List<Entity.Key> keys = connection.insert(singletonList(testEntity));
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
      assertEquals(testEntity, connection.selectSingle(testEntity.getKey()),
              "Entity of type " + testEntity.getEntityId() + " failed equals comparison");
    }
    else {
      connection.select(entitySelectCondition(entityId).setFetchCount(SELECT_FETCH_COUNT));
    }
  }

  /**
   * Test updating the given entity, if the entity is not modified this test does nothing
   * @param testEntity the entity to test updating
   * @param foreignKeyEntities the entities referenced via foreign keys
   * @throws org.jminor.common.db.exception.DatabaseException in case of an exception
   */
  private void testUpdate(final Entity testEntity, final Map<String, Entity> foreignKeyEntities) throws DatabaseException {
    modifyEntity(testEntity, foreignKeyEntities);
    if (!testEntity.isModified()) {
      return;
    }

    final Entity updated = connection.update(singletonList(testEntity)).get(0);
    assertEquals(testEntity.getKey(), updated.getKey());
    for (final ColumnProperty property : getDomain().getDefinition(testEntity.getEntityId()).getColumnProperties()) {
      if (!property.isReadOnly() && property.isUpdatable()) {
        final Object beforeUpdate = testEntity.get(property);
        final Object afterUpdate = updated.get(property);
        final String message = "Values of property " + property + " should be equal after update ["
                + beforeUpdate + (beforeUpdate != null ? (" (" + beforeUpdate.getClass() + ")") : "") + ", "
                + afterUpdate + (afterUpdate != null ? (" (" + afterUpdate.getClass() + ")") : "") + "]";
        if (property.isBigDecimal()) {//special case, scale is not necessarily the same, hence not equal
          assertTrue((afterUpdate == beforeUpdate) || (afterUpdate != null
                  && ((BigDecimal) afterUpdate).compareTo((BigDecimal) beforeUpdate) == 0));
        }
        else if (property.isBlob() && property instanceof BlobProperty && ((BlobProperty) property).isEagerlyLoaded()) {
          assertArrayEquals((byte[]) beforeUpdate, (byte[]) afterUpdate, message);
        }
        else {
          assertEquals(beforeUpdate, afterUpdate, message);
        }
      }
    }
  }

  /**
   * Test deleting the given entity
   * @param testEntity the entity to test deleting
   * @throws org.jminor.common.db.exception.DatabaseException in case of an exception
   */
  private void testDelete(final Entity testEntity) throws DatabaseException {
    assertEquals(1, connection.delete(Entities.getKeys(singletonList(testEntity))));

    boolean caught = false;
    try {
      connection.selectSingle(testEntity.getKey());
    }
    catch (final RecordNotFoundException e) {
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
    try {
      if (!entity.isKeyNull()) {
        final List<Entity> selected = connection.select(singletonList(entity.getKey()));
        if (!selected.isEmpty()) {
          return selected.get(0);
        }
      }

      return connection.selectSingle(connection.insert(singletonList(entity)).get(0));
    }
    catch (final DatabaseException e) {
      LOG.error("EntityTestUnit.insertOrSelect()", e);
      throw e;
    }
  }

  private static void populateEntity(final Domain domain, final Entity entity, final Collection<ColumnProperty> properties,
                                     final ValueProvider<Property, Object> valueProvider) {
    for (final ColumnProperty property : properties) {
      if (!property.isForeignKeyProperty() && !property.isDenormalized()) {
        entity.put(property, valueProvider.get(property));
      }
    }
    for (final ForeignKeyProperty property : domain.getDefinition(entity.getEntityId()).getForeignKeyProperties()) {
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

  private static byte[] getRandomBlob(final int numberOfBytes) {
    final byte[] bytes = new byte[numberOfBytes];
    RANDOM.nextBytes(bytes);

    return bytes;
  }

  private static Object getReferenceEntity(final ForeignKeyProperty property, final Map<String, Entity> referenceEntities) {
    return referenceEntities == null ? null : referenceEntities.get(property.getForeignEntityId());
  }

  private static Object getRandomListValue(final ValueListProperty property) {
    final List<Item> items = property.getValues();
    final Item item = items.get(RANDOM.nextInt(items.size()));

    return item.getValue();
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

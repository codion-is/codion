/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.test;

import is.codion.common.Configuration;
import is.codion.common.Text;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.RecordNotFoundException;
import is.codion.common.item.Item;
import is.codion.common.user.User;
import is.codion.common.value.PropertyValue;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.property.BlobProperty;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.ItemProperty;
import is.codion.framework.domain.property.Property;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;

import static is.codion.framework.db.condition.Conditions.condition;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A class for unit testing a domain model.
 */
public class EntityTestUnit {

  private static final Logger LOG = LoggerFactory.getLogger(EntityTestUnit.class);

  /**
   * Specifies the database user to use when running domain unit tests.
   */
  public static final PropertyValue<String> TEST_USER = Configuration.stringValue("codion.test.user", null);

  private static final int MININUM_RANDOM_NUMBER = -10000000;
  private static final int MAXIMUM_RANDOM_NUMBER = 10000000;
  private static final int MAXIMUM_RANDOM_STRING_LENGTH = 10;
  private static final int SELECT_FETCH_COUNT = 10;
  private static final Random RANDOM = new Random();

  private final String domainClass;
  private final User user;

  private EntityConnection connection;
  private Entities entities;

  /**
   * Instantiates a new EntityTestUnit.
   * The default database user is based on the {@link #TEST_USER} configuration value.
   * @param domainClass the name of the domain model class
   * @throws NullPointerException in case domainClass is null
   */
  public EntityTestUnit(final String domainClass) {
    this(domainClass, initializeDefaultUser());
  }

  /**
   * Instantiates a new EntityTestUnit.
   * @param domainClass the name of the domain model class
   * @param user the user to use when running the tests
   * @throws NullPointerException in case domainClass or user is null
   */
  public EntityTestUnit(final String domainClass, final User user) {
    this.domainClass = requireNonNull(domainClass, "domainClass");
    this.user = requireNonNull(user, "user");
  }

  /**
   * @return the domain entities
   */
  public final Entities getEntities() {
    if (entities == null) {
      entities = connection.getEntities();
    }

    return entities;
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
      connection.close();
    }
    doTearDown();
  }

  /**
   * Runs the insert/update/select/delete tests for the given entityType
   * @param entityType the type of the entity to test
   * @throws is.codion.common.db.exception.DatabaseException in case of an exception
   */
  public final void test(final EntityType<?> entityType) throws DatabaseException {
    connection.beginTransaction();
    try {
      final Map<EntityType<?>, Entity> foreignKeyEntities = initializeReferencedEntities(entityType, new HashMap<>());
      Entity testEntity = null;
      final EntityDefinition entityDefinition = getEntities().getDefinition(entityType);
      if (!entityDefinition.isReadOnly()) {
        testEntity = testInsert(requireNonNull(initializeTestEntity(entityType, foreignKeyEntities), "test entity"));
        assertTrue(testEntity.getPrimaryKey().isNotNull());
        testUpdate(testEntity, initializeReferencedEntities(entityType, foreignKeyEntities));
      }
      testSelect(entityType, testEntity);
      if (!entityDefinition.isReadOnly()) {
        testDelete(testEntity);
      }
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  /**
   * @param entities the domain model entities
   * @param entityType the entityType
   * @param referenceEntities entities referenced by the given entityType
   * @return a Entity instance containing randomized values, based on the property definitions
   */
  public static Entity createRandomEntity(final Entities entities, final EntityType<?> entityType,
                                          final Map<EntityType<?>, Entity> referenceEntities) {
    return createEntity(entities, entityType, property -> createRandomValue(property, referenceEntities));
  }

  /**
   * @param entities the domain model entities
   * @param entityType the entityType
   * @param valueProvider the value provider
   * @return an Entity instance initialized with values provided by the given value provider
   */
  public static Entity createEntity(final Entities entities, final EntityType<?> entityType, final Function<Property<?>, Object> valueProvider) {
    requireNonNull(entities);
    requireNonNull(entityType);
    final Entity entity = entities.entity(entityType);
    populateEntity(entities, entity, entities.getDefinition(entityType).getWritableColumnProperties(
            !entities.getDefinition(entityType).isKeyGenerated(), true), valueProvider);

    return entity;
  }

  /**
   * Randomizes the values in the given entity, note that if a foreign key entity is not provided
   * the respective foreign key value in not modified
   * @param entities the domain model entities
   * @param entity the entity to randomize
   * @param foreignKeyEntities the entities referenced via foreign keys
   */
  public static void randomize(final Entities entities, final Entity entity, final Map<EntityType<?>, Entity> foreignKeyEntities) {
    requireNonNull(entities);
    requireNonNull(entity);
    populateEntity(entities, entity,
            entities.getDefinition(entity.getEntityType()).getWritableColumnProperties(false, true),
            property -> createRandomValue(property, foreignKeyEntities));
  }

  /**
   * Creates a random value for the given property.
   * @param property the property
   * @param referenceEntities entities referenced by the given property
   * @return a random value
   */
  public static Object createRandomValue(final Property<?> property, final Map<EntityType<?>, Entity> referenceEntities) {
    requireNonNull(property, "property");
    if (property instanceof ForeignKeyProperty) {
      return getReferenceEntity((ForeignKeyProperty) property, referenceEntities);
    }
    if (property instanceof ItemProperty) {
      return getRandomItem((ItemProperty<?>) property);
    }
    final Attribute<?> attribute = property.getAttribute();
    if (attribute.isBoolean()) {
      return RANDOM.nextBoolean();
    }
    if (attribute.isCharacter()) {
      return (char) RANDOM.nextInt();
    }
    if (attribute.isLocalDate()) {
      return LocalDate.now();
    }
    if (attribute.isLocalDateTime()) {
      return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
    if (attribute.isOffsetDateTime()) {
      return OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
    if (attribute.isLocalTime()) {
      return LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
    if (attribute.isDouble()) {
      return getRandomDouble(property);
    }
    if (attribute.isBigDecimal()) {
      return BigDecimal.valueOf(getRandomDouble(property));
    }
    if (attribute.isInteger()) {
      return getRandomInteger(property);
    }
    if (attribute.isLong()) {
      return (long) getRandomInteger(property);
    }
    if (attribute.isString()) {
      return getRandomString(property);
    }
    if (attribute.isByteArray()) {
      return getRandomBlob(property);
    }

    return null;
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
    return EntityConnectionProvider.connectionProvider().setDomainClassName(domainClass)
            .setClientTypeId(getClass().getName()).setUser(user);
  }

  /**
   * @return the EntityConnection instance used by this EntityTestUnit
   */
  protected final EntityConnection getConnection() {
    return connection;
  }

  /**
   * This method should return an instance of the entity specified by {@code entityType}
   * @param entityType the entityType for which to initialize an entity instance
   * @param foreignKeyEntities the entities referenced via foreign keys
   * @return the entity instance to use for testing the entity type
   */
  protected Entity initializeTestEntity(final EntityType<?> entityType, final Map<EntityType<?>, Entity> foreignKeyEntities) {
    return createRandomEntity(getEntities(), entityType, foreignKeyEntities);
  }

  /**
   * Initializes a new Entity of the given type, by default this method creates a Entity filled with random values.
   * @param entityType the entityType
   * @param foreignKeyEntities the entities referenced via foreign keys
   * @return a entity of the given type
   * @throws DatabaseException in case of an exception
   */
  protected Entity initializeReferenceEntity(final EntityType<?> entityType, final Map<EntityType<?>, Entity> foreignKeyEntities)
          throws DatabaseException {
    return createRandomEntity(getEntities(), entityType, foreignKeyEntities);
  }

  /**
   * This method should return {@code testEntity} in a modified state
   * @param testEntity the entity to modify
   * @param foreignKeyEntities the entities referenced via foreign keys
   */
  protected void modifyEntity(final Entity testEntity, final Map<EntityType<?>, Entity> foreignKeyEntities) {
    randomize(getEntities(), testEntity, foreignKeyEntities);
  }

  /**
   * Initializes the entities referenced by the entity identified by {@code entityType}
   * @param entityType the type of the entity for which to initialize the referenced entities
   * @param foreignKeyEntities foreign key entities already created
   * @throws is.codion.common.db.exception.DatabaseException in case of an exception
   * @see #initializeReferenceEntity(String, Map)
   * @return the Entities to reference mapped to their respective entityTypes
   */
  private Map<EntityType<?>, Entity> initializeReferencedEntities(final EntityType<?> entityType,
                                                                  final Map<EntityType<?>, Entity> foreignKeyEntities)
          throws DatabaseException {
    final List<ForeignKey> foreignKeys = new ArrayList<>(getEntities().getDefinition(entityType).getForeignKeys());
    //we have to start with non-self-referential ones
    foreignKeys.sort((fk1, fk2) -> !fk1.getReferencedEntityType().equals(entityType) ? -1 : 1);
    for (final ForeignKey foreignKey : foreignKeys) {
      final EntityType<?> referencedEntityType = foreignKey.getReferencedEntityType();
      if (!foreignKeyEntities.containsKey(referencedEntityType)) {
        if (!Objects.equals(entityType, referencedEntityType)) {
          foreignKeyEntities.put(referencedEntityType, null);//short circuit recursion, value replaced below
          initializeReferencedEntities(referencedEntityType, foreignKeyEntities);
        }
        final Entity referencedEntity = initializeReferenceEntity(referencedEntityType, foreignKeyEntities);
        if (referencedEntity != null) {
          foreignKeyEntities.put(referencedEntityType, insertOrSelect(referencedEntity));
        }
      }
    }

    return foreignKeyEntities;
  }

  /**
   * Tests inserting the given entity
   * @param testEntity the entity to test insert for
   * @return the same entity retrieved from the database after the insert
   * @throws is.codion.common.db.exception.DatabaseException in case of an exception
   */
  private Entity testInsert(final Entity testEntity) throws DatabaseException {
    final Key key = connection.insert(testEntity);
    try {
      return connection.selectSingle(key);
    }
    catch (final RecordNotFoundException e) {
      fail("Inserted entity of type " + testEntity.getEntityType() + " not returned by select after insert");
      throw e;
    }
  }

  /**
   * Tests selecting the given entity, if {@code testEntity} is null
   * then selecting many entities is tested.
   * @param entityType the entityType in case {@code testEntity} is null
   * @param testEntity the entity to test selecting
   * @throws is.codion.common.db.exception.DatabaseException in case of an exception
   */
  private void testSelect(final EntityType<?> entityType, final Entity testEntity) throws DatabaseException {
    if (testEntity != null) {
      assertEquals(testEntity, connection.selectSingle(testEntity.getPrimaryKey()),
              "Entity of type " + testEntity.getEntityType() + " failed equals comparison");
    }
    else {
      connection.select(condition(entityType).toSelectCondition().fetchCount(SELECT_FETCH_COUNT));
    }
  }

  /**
   * Test updating the given entity, if the entity is not modified this test does nothing
   * @param testEntity the entity to test updating
   * @param foreignKeyEntities the entities referenced via foreign keys
   * @throws is.codion.common.db.exception.DatabaseException in case of an exception
   */
  private void testUpdate(final Entity testEntity, final Map<EntityType<?>, Entity> foreignKeyEntities) throws DatabaseException {
    modifyEntity(testEntity, foreignKeyEntities);
    if (!testEntity.isModified()) {
      return;
    }

    final Entity updated = connection.update(testEntity);
    assertEquals(testEntity.getPrimaryKey(), updated.getPrimaryKey());
    for (final ColumnProperty<?> property : getEntities().getDefinition(testEntity.getEntityType()).getColumnProperties()) {
      if (property.isUpdatable()) {
        final Object beforeUpdate = testEntity.get(property.getAttribute());
        final Object afterUpdate = updated.get(property.getAttribute());
        final String message = "Values of property " + property + " should be equal after update ["
                + beforeUpdate + (beforeUpdate != null ? (" (" + beforeUpdate.getClass() + ")") : "") + ", "
                + afterUpdate + (afterUpdate != null ? (" (" + afterUpdate.getClass() + ")") : "") + "]";
        if (property.getAttribute().isBigDecimal()) {//special case, scale is not necessarily the same, hence not equal
          assertTrue((afterUpdate == beforeUpdate) || (afterUpdate != null
                  && ((BigDecimal) afterUpdate).compareTo((BigDecimal) beforeUpdate) == 0));
        }
        else if (property.getAttribute().isByteArray() && property instanceof BlobProperty && ((BlobProperty) property).isEagerlyLoaded()) {
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
   * @throws is.codion.common.db.exception.DatabaseException in case of an exception
   */
  private void testDelete(final Entity testEntity) throws DatabaseException {
    assertEquals(1, connection.delete(Entity.getPrimaryKeys(singletonList(testEntity))));

    boolean caught = false;
    try {
      connection.selectSingle(testEntity.getPrimaryKey());
    }
    catch (final RecordNotFoundException e) {
      caught = true;
    }
    assertTrue(caught, "Entity of type " + testEntity.getEntityType() + " failed delete test");
  }

  /**
   * Inserts or selects the given entity if it exists and returns the result
   * @param entity the entity to initialize
   * @return the entity
   * @throws DatabaseException in case of an exception
   */
  private Entity insertOrSelect(final Entity entity) throws DatabaseException {
    try {
      if (entity.getPrimaryKey().isNotNull()) {
        final List<Entity> selected = connection.select(singletonList(entity.getPrimaryKey()));
        if (!selected.isEmpty()) {
          return selected.get(0);
        }
      }

      return connection.selectSingle(connection.insert(entity));
    }
    catch (final DatabaseException e) {
      LOG.error("EntityTestUnit.insertOrSelect()", e);
      throw e;
    }
  }

  private static User initializeDefaultUser() {
    return User.parseUser(TEST_USER.getOrThrow());
  }

  private static void populateEntity(final Entities entities, final Entity entity, final Collection<ColumnProperty<?>> properties,
                                     final Function<Property<?>, Object> valueProvider) {
    requireNonNull(valueProvider, "valueProvider");
    final EntityDefinition definition = entities.getDefinition(entity.getEntityType());
    for (@SuppressWarnings("rawtypes") final ColumnProperty property : properties) {
      if (!definition.isForeignKeyAttribute(property.getAttribute()) && !property.isDenormalized()) {
        entity.put(property.getAttribute(), valueProvider.apply(property));
      }
    }
    for (final ForeignKeyProperty property : entities.getDefinition(entity.getEntityType()).getForeignKeyProperties()) {
      final Entity value = (Entity) valueProvider.apply(property);
      if (value != null) {
        entity.put(property.getAttribute(), value);
      }
    }
  }

  private static String getRandomString(final Property<?> property) {
    final int length = property.getMaximumLength() < 0 ? MAXIMUM_RANDOM_STRING_LENGTH : property.getMaximumLength();

    return Text.randomString(length, length);
  }

  private static byte[] getRandomBlob(final Property<?> property) {
    if ((property instanceof BlobProperty) && ((BlobProperty) property).isEagerlyLoaded()) {
      return getRandomBlob(1024);
    }

    return null;
  }

  private static byte[] getRandomBlob(final int numberOfBytes) {
    final byte[] bytes = new byte[numberOfBytes];
    RANDOM.nextBytes(bytes);

    return bytes;
  }

  private static Object getReferenceEntity(final ForeignKeyProperty property, final Map<EntityType<?>, Entity> referenceEntities) {
    return referenceEntities == null ? null : referenceEntities.get(property.getReferencedEntityType());
  }

  private static <T> T getRandomItem(final ItemProperty<T> property) {
    final List<Item<T>> items = property.getValues();
    final Item<T> item = items.get(RANDOM.nextInt(items.size()));

    return item.getValue();
  }

  private static int getRandomInteger(final Property<?> property) {
    final int min = (int) (property.getMinimumValue() == null ? MININUM_RANDOM_NUMBER : property.getMinimumValue());
    final int max = (int) (property.getMaximumValue() == null ? MAXIMUM_RANDOM_NUMBER : property.getMaximumValue());

    return RANDOM.nextInt((max - min) + 1) + min;
  }

  private static double getRandomDouble(final Property<?> property) {
    final double min = property.getMinimumValue() == null ? MININUM_RANDOM_NUMBER : property.getMinimumValue();
    final double max = property.getMaximumValue() == null ? MAXIMUM_RANDOM_NUMBER : property.getMaximumValue();

    return RANDOM.nextDouble() * (max - min) + min;
  }
}

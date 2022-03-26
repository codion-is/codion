/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.test;

import is.codion.common.Configuration;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.RecordNotFoundException;
import is.codion.common.properties.PropertyValue;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.property.BlobProperty;
import is.codion.framework.domain.property.ColumnProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
  public static final PropertyValue<String> TEST_USER = Configuration.stringValue("codion.test.user");

  private static final int SELECT_FETCH_COUNT = 10;

  private final EntityConnectionProvider connectionProvider;

  /**
   * Instantiates a new EntityTestUnit.
   * The default database user is based on the {@link #TEST_USER} configuration value.
   * @param domainClass the name of the domain model class
   * @throws NullPointerException in case domainClass is null
   */
  public EntityTestUnit(String domainClass) {
    this(domainClass, initializeDefaultUser());
  }

  /**
   * Instantiates a new EntityTestUnit.
   * @param domainClass the name of the domain model class
   * @param user the user to use when running the tests
   * @throws NullPointerException in case domainClass or user is null
   */
  public EntityTestUnit(String domainClass, User user) {
    this.connectionProvider = EntityConnectionProvider.builder()
            .domainClassName(requireNonNull(domainClass, "domainClass"))
            .clientTypeId(getClass().getName())
            .user(requireNonNull(user, "user"))
            .build();
  }

  /**
   * @return the domain entities
   */
  public final Entities getEntities() {
    return connectionProvider.getEntities();
  }

  /**
   * Runs the insert/update/select/delete tests for the given entityType
   * @param entityType the type of the entity to test
   * @throws is.codion.common.db.exception.DatabaseException in case of an exception
   */
  public final void test(EntityType entityType) throws DatabaseException {
    EntityConnection connection = connectionProvider.getConnection();
    connection.beginTransaction();
    try {
      Map<EntityType, Entity> foreignKeyEntities = initializeReferencedEntities(entityType, new HashMap<>(), connection);
      Entity testEntity = null;
      EntityDefinition entityDefinition = getEntities().getDefinition(entityType);
      if (!entityDefinition.isReadOnly()) {
        testEntity = testInsert(requireNonNull(initializeTestEntity(entityType, foreignKeyEntities), "test entity"), connection);
        assertTrue(testEntity.getPrimaryKey().isNotNull());
        testUpdate(testEntity, initializeReferencedEntities(entityType, foreignKeyEntities, connection), connection, this);
      }
      testSelect(entityType, testEntity, connection);
      if (!entityDefinition.isReadOnly()) {
        testDelete(testEntity, connection);
      }
    }
    finally {
      connection.rollbackTransaction();
      if (connection != null) {
        connection.close();
      }
    }
  }

  /**
   * @return the EntityConnection instance used by this EntityTestUnit
   */
  protected final EntityConnection getConnection() {
    return connectionProvider.getConnection();
  }

  /**
   * This method should return an instance of the entity specified by {@code entityType}
   * @param entityType the entityType for which to initialize an entity instance
   * @param foreignKeyEntities the entities referenced via foreign keys
   * @return the entity instance to use for testing the entity type
   */
  protected Entity initializeTestEntity(EntityType entityType, Map<EntityType, Entity> foreignKeyEntities) {
    return EntityTestUtil.createRandomEntity(getEntities(), entityType, foreignKeyEntities);
  }

  /**
   * Initializes a new Entity of the given type, by default this method creates an Entity filled with random values.
   * @param entityType the entityType
   * @param foreignKeyEntities the entities referenced via foreign keys
   * @return an entity of the given type
   * @throws DatabaseException in case of an exception
   */
  protected Entity initializeReferenceEntity(EntityType entityType, Map<EntityType, Entity> foreignKeyEntities)
          throws DatabaseException {
    return EntityTestUtil.createRandomEntity(getEntities(), entityType, foreignKeyEntities);
  }

  /**
   * This method should return {@code testEntity} in a modified state
   * @param testEntity the entity to modify
   * @param foreignKeyEntities the entities referenced via foreign keys
   */
  protected void modifyEntity(Entity testEntity, Map<EntityType, Entity> foreignKeyEntities) {
    EntityTestUtil.randomize(getEntities(), testEntity, foreignKeyEntities);
  }

  /**
   * Initializes the entities referenced by the entity identified by {@code entityType}
   * @param entityType the type of the entity for which to initialize the referenced entities
   * @param foreignKeyEntities foreign key entities already created
   * @param connection the connection to use
   * @throws is.codion.common.db.exception.DatabaseException in case of an exception
   * @see #initializeReferenceEntity(EntityType, Map)
   * @return the Entities to reference mapped to their respective entityTypes
   */
  private Map<EntityType, Entity> initializeReferencedEntities(EntityType entityType,
                                                               Map<EntityType, Entity> foreignKeyEntities,
                                                               EntityConnection connection) throws DatabaseException {
    List<ForeignKey> foreignKeys = new ArrayList<>(getEntities().getDefinition(entityType).getForeignKeys());
    //we have to start with non-self-referential ones
    foreignKeys.sort((fk1, fk2) -> !fk1.getReferencedEntityType().equals(entityType) ? -1 : 1);
    for (ForeignKey foreignKey : foreignKeys) {
      EntityType referencedEntityType = foreignKey.getReferencedEntityType();
      if (!foreignKeyEntities.containsKey(referencedEntityType)) {
        if (!Objects.equals(entityType, referencedEntityType)) {
          foreignKeyEntities.put(referencedEntityType, null);//short circuit recursion, value replaced below
          initializeReferencedEntities(referencedEntityType, foreignKeyEntities, connection);
        }
        Entity referencedEntity = initializeReferenceEntity(referencedEntityType, foreignKeyEntities);
        if (referencedEntity != null) {
          foreignKeyEntities.put(referencedEntityType, insertOrSelect(referencedEntity, connection));
        }
      }
    }

    return foreignKeyEntities;
  }

  /**
   * Tests inserting the given entity
   * @param testEntity the entity to test insert for
   * @param connection the connection to use
   * @return the same entity retrieved from the database after the insert
   * @throws is.codion.common.db.exception.DatabaseException in case of an exception
   */
  private static Entity testInsert(Entity testEntity, EntityConnection connection) throws DatabaseException {
    Key key = connection.insert(testEntity);
    try {
      return connection.selectSingle(key);
    }
    catch (RecordNotFoundException e) {
      fail("Inserted entity of type " + testEntity.getEntityType() + " not returned by select after insert");
      throw e;
    }
  }

  /**
   * Tests selecting the given entity, if {@code testEntity} is null
   * then selecting many entities is tested.
   * @param entityType the entityType in case {@code testEntity} is null
   * @param testEntity the entity to test selecting
   * @param connection the connection to use
   * @throws is.codion.common.db.exception.DatabaseException in case of an exception
   */
  private static void testSelect(EntityType entityType, Entity testEntity,
                                 EntityConnection connection) throws DatabaseException {
    if (testEntity != null) {
      assertEquals(testEntity, connection.selectSingle(testEntity.getPrimaryKey()),
              "Entity of type " + testEntity.getEntityType() + " failed equals comparison");
    }
    else {
      connection.select(condition(entityType).toSelectCondition().limit(SELECT_FETCH_COUNT));
    }
  }

  /**
   * Test updating the given entity, if the entity is not modified this test does nothing
   * @param testEntity the entity to test updating
   * @param foreignKeyEntities the entities referenced via foreign keys
   * @param connection the connection to use
   * @param entityTestUnit the test unit instance, for modifying the entity
   * @throws is.codion.common.db.exception.DatabaseException in case of an exception
   */
  private static void testUpdate(Entity testEntity, Map<EntityType, Entity> foreignKeyEntities,
                                 EntityConnection connection, EntityTestUnit entityTestUnit) throws DatabaseException {
    entityTestUnit.modifyEntity(testEntity, foreignKeyEntities);
    if (!testEntity.isModified()) {
      return;
    }

    Entity updated = connection.update(testEntity);
    assertEquals(testEntity.getPrimaryKey(), updated.getPrimaryKey());
    for (ColumnProperty<?> property : testEntity.getDefinition().getColumnProperties()) {
      if (property.isUpdatable()) {
        Object beforeUpdate = testEntity.get(property.getAttribute());
        Object afterUpdate = updated.get(property.getAttribute());
        String message = "Values of property " + property + " should be equal after update ["
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
   * @param connection the connection to use
   * @throws is.codion.common.db.exception.DatabaseException in case of an exception
   */
  private static void testDelete(Entity testEntity, EntityConnection connection) throws DatabaseException {
    connection.delete(Entity.getPrimaryKeys(singletonList(testEntity)));
    boolean caught = false;
    try {
      connection.selectSingle(testEntity.getPrimaryKey());
    }
    catch (RecordNotFoundException e) {
      caught = true;
    }
    assertTrue(caught, "Entity of type " + testEntity.getEntityType() + " failed delete test");
  }

  /**
   * Inserts or selects the given entity if it exists and returns the result
   * @param entity the entity to initialize
   * @param connection the connection to use
   * @return the entity
   * @throws DatabaseException in case of an exception
   */
  private static Entity insertOrSelect(Entity entity, EntityConnection connection) throws DatabaseException {
    try {
      if (entity.getPrimaryKey().isNotNull()) {
        List<Entity> selected = connection.select(singletonList(entity.getPrimaryKey()));
        if (!selected.isEmpty()) {
          return selected.get(0);
        }
      }

      return connection.selectSingle(connection.insert(entity));
    }
    catch (DatabaseException e) {
      LOG.error("EntityTestUnit.insertOrSelect()", e);
      throw e;
    }
  }

  private static User initializeDefaultUser() {
    return User.parse(TEST_USER.getOrThrow());
  }
}

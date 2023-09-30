/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.test;

import is.codion.common.Configuration;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.RecordNotFoundException;
import is.codion.common.property.PropertyValue;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.BlobColumnDefinition;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

  private static final int SELECT_LIMIT = 10;

  private final EntityConnectionProvider connectionProvider;

  /**
   * Instantiates a new EntityTestUnit.
   * The default database user is based on the {@link #TEST_USER} configuration value.
   * @param domain the domain model
   * @throws NullPointerException in case domainClass is null
   */
  public EntityTestUnit(Domain domain) {
    this(domain, initializeDefaultUser());
  }

  /**
   * Instantiates a new EntityTestUnit.
   * @param domain the domain model
   * @param user the user to use when running the tests
   * @throws NullPointerException in case domainClass or user is null
   */
  public EntityTestUnit(Domain domain, User user) {
    this.connectionProvider = LocalEntityConnectionProvider.builder()
            .domain(requireNonNull(domain, "domain"))
            .clientTypeId(getClass().getName())
            .user(requireNonNull(user, "user"))
            .build();
  }

  /**
   * @return the domain entities
   */
  public final Entities entities() {
    return connectionProvider.entities();
  }

  /**
   * Runs the insert/update/select/delete tests for the given entityType
   * @param entityType the type of the entity to test
   * @throws DatabaseException in case of an exception
   */
  public final void test(EntityType entityType) throws DatabaseException {
    EntityConnection connection = connectionProvider.connection();
    connection.beginTransaction();
    try {
      Map<ForeignKey, Entity> foreignKeyEntities = initializeForeignKeyEntities(entityType, new HashMap<>(), connection);
      Entity testEntity = null;
      EntityDefinition entityDefinition = entities().definition(entityType);
      if (!entityDefinition.readOnly()) {
        testEntity = testInsert(requireNonNull(initializeTestEntity(entityType, foreignKeyEntities), "test entity"), connection);
        assertTrue(testEntity.primaryKey().isNotNull());
        testUpdate(testEntity, initializeForeignKeyEntities(entityType, foreignKeyEntities, connection), connection, this);
      }
      testSelect(entityType, testEntity, connection);
      if (!entityDefinition.readOnly()) {
        testDelete(testEntity, connection);
      }
    }
    finally {
      connection.rollbackTransaction();
      connection.close();
    }
  }

  /**
   * @return the EntityConnection instance used by this EntityTestUnit
   */
  protected final EntityConnection connection() {
    return connectionProvider.connection();
  }

  /**
   * This method returns the Entity instance on which to run the tests, by default this method creates an instance
   * filled with random values.
   * @param entityType the entityType for which to initialize an entity instance for testing
   * @param foreignKeyEntities the entities referenced via foreign keys
   * @return the entity instance to use for testing the entity type
   */
  protected Entity initializeTestEntity(EntityType entityType, Map<ForeignKey, Entity> foreignKeyEntities) {
    return EntityTestUtil.createRandomEntity(entities(), entityType, foreignKeyEntities);
  }

  /**
   * Initializes an Entity instance to reference via the given foreign key, by default this method creates an Entity
   * filled with random values. Subclasses can override and provide a hard coded instance or select one from the database.
   * Note that this default implementation returns null in case the referenced entity type is read-only.
   * @param foreignKey the foreign key referencing the entity
   * @param foreignKeyEntities the entities referenced via foreign keys
   * @return an entity for the given foreign key
   * @throws DatabaseException in case of an exception
   */
  protected Entity initializeForeignKeyEntity(ForeignKey foreignKey, Map<ForeignKey, Entity> foreignKeyEntities) throws DatabaseException {
    if (entities().definition(requireNonNull(foreignKey).referencedType()).readOnly()) {
      return null;
    }

    return EntityTestUtil.createRandomEntity(entities(), foreignKey.referencedType(), foreignKeyEntities);
  }

  /**
   * This method should return {@code testEntity} in a modified state
   * @param testEntity the entity to modify
   * @param foreignKeyEntities the entities referenced via foreign keys
   */
  protected void modifyEntity(Entity testEntity, Map<ForeignKey, Entity> foreignKeyEntities) {
    EntityTestUtil.randomize(entities(), testEntity, foreignKeyEntities);
  }

  /**
   * Initializes the entities referenced by the entity identified by {@code entityType}
   * @param entityType the type of the entity for which to initialize the referenced entities
   * @param foreignKeyEntities foreign key entities already created
   * @param connection the connection to use
   * @return the Entities to reference mapped to their respective foreign keys
   * @throws DatabaseException in case of an exception
   * @see #initializeForeignKeyEntity(ForeignKey, Map)
   */
  private Map<ForeignKey, Entity> initializeForeignKeyEntities(EntityType entityType,
                                                               Map<ForeignKey, Entity> foreignKeyEntities,
                                                               EntityConnection connection) throws DatabaseException {
    List<ForeignKey> foreignKeys = new ArrayList<>(entities().definition(entityType).foreignKeys().get());
    //we have to start with non-self-referential ones
    foreignKeys.sort((fk1, fk2) -> !fk1.referencedType().equals(entityType) ? -1 : 1);
    for (ForeignKey foreignKey : foreignKeys) {
      EntityType referencedEntityType = foreignKey.referencedType();
      if (!foreignKeyEntities.containsKey(foreignKey)) {
        if (!Objects.equals(entityType, referencedEntityType)) {
          foreignKeyEntities.put(foreignKey, null);//short circuit recursion, value replaced below
          initializeForeignKeyEntities(referencedEntityType, foreignKeyEntities, connection);
        }
        Entity referencedEntity = initializeForeignKeyEntity(foreignKey, foreignKeyEntities);
        if (referencedEntity != null) {
          foreignKeyEntities.put(foreignKey, insertOrSelect(referencedEntity, connection));
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
   * @throws DatabaseException in case of an exception
   */
  private static Entity testInsert(Entity testEntity, EntityConnection connection) throws DatabaseException {
    Entity.Key key = connection.insert(testEntity);
    try {
      return connection.select(key);
    }
    catch (RecordNotFoundException e) {
      fail("Inserted entity of type " + testEntity.entityType() + " not returned by select after insert");
      throw e;
    }
  }

  /**
   * Tests selecting the given entity, if {@code testEntity} is null
   * then selecting many entities is tested.
   * @param entityType the entityType in case {@code testEntity} is null
   * @param testEntity the entity to test selecting
   * @param connection the connection to use
   * @throws DatabaseException in case of an exception
   */
  private static void testSelect(EntityType entityType, Entity testEntity,
                                 EntityConnection connection) throws DatabaseException {
    if (testEntity != null) {
      assertEquals(testEntity, connection.select(testEntity.primaryKey()),
              "Entity of type " + testEntity.entityType() + " failed equals comparison");
    }
    else {
      connection.select(Select.all(entityType)
              .limit(SELECT_LIMIT)
              .build());
    }
  }

  /**
   * Test updating the given entity, if the entity is not modified this test does nothing
   * @param testEntity the entity to test updating
   * @param foreignKeyEntities the entities referenced via foreign keys
   * @param connection the connection to use
   * @param entityTestUnit the test unit instance, for modifying the entity
   * @throws DatabaseException in case of an exception
   */
  private static void testUpdate(Entity testEntity, Map<ForeignKey, Entity> foreignKeyEntities,
                                 EntityConnection connection, EntityTestUnit entityTestUnit) throws DatabaseException {
    entityTestUnit.modifyEntity(testEntity, foreignKeyEntities);
    if (!testEntity.modified()) {
      return;
    }

    Entity updatedEntity = connection.updateSelect(testEntity);
    assertEquals(testEntity.primaryKey(), updatedEntity.primaryKey());
    testEntity.definition().columns().definitions().stream()
            .filter(ColumnDefinition::updatable)
            .forEach(columnDefinition -> assertValueEqual(testEntity, updatedEntity, columnDefinition));
  }

  /**
   * Test deleting the given entity
   * @param testEntity the entity to test deleting
   * @param connection the connection to use
   * @throws DatabaseException in case of an exception
   */
  private static void testDelete(Entity testEntity, EntityConnection connection) throws DatabaseException {
    connection.delete(Entity.primaryKeys(singletonList(testEntity)));
    boolean caught = false;
    try {
      connection.select(testEntity.primaryKey());
    }
    catch (RecordNotFoundException e) {
      caught = true;
    }
    assertTrue(caught, "Entity of type " + testEntity.entityType() + " failed delete test");
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
      if (entity.primaryKey().isNotNull()) {
        Collection<Entity> selected = connection.select(singletonList(entity.primaryKey()));
        if (!selected.isEmpty()) {
          return selected.iterator().next();
        }
      }

      return connection.insertSelect(entity);
    }
    catch (DatabaseException e) {
      LOG.error("EntityTestUnit.insertOrSelect()", e);
      throw e;
    }
  }

  private static void assertValueEqual(Entity testEntity, Entity updated, ColumnDefinition<?> columnDefinition) {
    Object beforeUpdate = testEntity.get(columnDefinition.attribute());
    Object afterUpdate = updated.get(columnDefinition.attribute());
    String message = "Values of column " + columnDefinition + " should be equal after update ["
            + beforeUpdate + (beforeUpdate != null ? (" (" + beforeUpdate.getClass() + ")") : "") + ", "
            + afterUpdate + (afterUpdate != null ? (" (" + afterUpdate.getClass() + ")") : "") + "]";
    if (columnDefinition.attribute().type().isBigDecimal()) {//special case, scale is not necessarily the same, hence not equal
      assertTrue((afterUpdate == beforeUpdate) || (afterUpdate != null
              && ((BigDecimal) afterUpdate).compareTo((BigDecimal) beforeUpdate) == 0));
    }
    else if (columnDefinition.attribute().type().isByteArray() && columnDefinition instanceof BlobColumnDefinition && ((BlobColumnDefinition) columnDefinition).eagerlyLoaded()) {
      assertArrayEquals((byte[]) beforeUpdate, (byte[]) afterUpdate, message);
    }
    else {
      assertEquals(beforeUpdate, afterUpdate, message);
    }
  }

  private static User initializeDefaultUser() {
    return User.parse(TEST_USER.getOrThrow());
  }
}

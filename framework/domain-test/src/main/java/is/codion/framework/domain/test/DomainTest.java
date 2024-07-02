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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.test;

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
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A class for unit testing a domain model.
 */
public class DomainTest {

	/**
	 * Specifies the database user to use when running domain unit tests.
	 */
	public static final PropertyValue<String> TEST_USER = Configuration.stringValue("codion.test.user");

	private static final int SELECT_LIMIT = 10;

	private final EntityConnectionProvider connectionProvider;
	private final EntityFactory entityFactory;

	/**
	 * Instantiates a new DomainTest.
	 * The default database user is based on the {@link #TEST_USER} configuration value.
	 * @param domain the domain model
	 */
	public DomainTest(Domain domain) {
		this(domain, initializeDefaultUser());
	}

	/**
	 * Instantiates a new DomainTest.
	 * @param domain the domain model
	 * @param entityFactory the factory used to create test entities
	 */
	public DomainTest(Domain domain, EntityFactory entityFactory) {
		this(domain, entityFactory, initializeDefaultUser());
	}

	/**
	 * Instantiates a new DomainTest.
	 * @param domain the domain model
	 * @param user the user to use when running the tests
	 */
	public DomainTest(Domain domain, User user) {
		this(domain, new DefaultEntityFactory(domain.entities()), user);
	}

	/**
	 * Instantiates a new DomainTest.
	 * @param domain the domain model
	 * @param entityFactory the factory used to create test entities
	 * @param user the user to use when running the tests
	 */
	public DomainTest(Domain domain, EntityFactory entityFactory, User user) {
		this.connectionProvider = LocalEntityConnectionProvider.builder()
						.domain(requireNonNull(domain, "domain"))
						.clientTypeId(getClass().getName())
						.user(requireNonNull(user, "user"))
						.build();
		this.entityFactory = requireNonNull(entityFactory);
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
		connection.startTransaction();
		try {
			Map<ForeignKey, Entity> foreignKeyEntities = entityFactory.foreignKeyEntities(entityType, new HashMap<>(), connection);
			Entity entity = null;
			EntityDefinition entityDefinition = entities().definition(entityType);
			if (!entityDefinition.readOnly()) {
				entity = testInsert(requireNonNull(entityFactory.entity(entityType, foreignKeyEntities),
								"EntityFactory.entity() must return a non-null entity"), connection);
				assertTrue(entity.primaryKey().isNotNull());
				entityFactory.modify(entity, entityFactory.foreignKeyEntities(entityType, foreignKeyEntities, connection));
				testUpdate(entity, connection);
			}
			testSelect(entityType, entity, connection);
			if (!entityDefinition.readOnly()) {
				testDelete(entity, connection);
			}
		}
		finally {
			connection.rollbackTransaction();
			connection.close();
		}
	}

	/**
	 * Handles creating and modifying entities used for testing.
	 */
	public interface EntityFactory {

		/**
		 * This method returns the Entity instance on which to run the tests, by default this method creates an instance
		 * filled with random values.
		 * @param entityType the entityType for which to initialize an entity instance for testing
		 * @return the entity instance to use for testing the entity type
		 */
		Entity entity(EntityType entityType);

		/**
		 * This method returns the Entity instance on which to run the tests, by default this method creates an instance
		 * filled with random values.
		 * @param entityType the entityType for which to initialize an entity instance for testing
		 * @param foreignKeyEntities the entities referenced via foreign keys
		 * @return the entity instance to use for testing the entity type
		 */
		Entity entity(EntityType entityType, Map<ForeignKey, Entity> foreignKeyEntities);

		/**
		 * Initializes an Entity instance to reference via the given foreign key, by default this method creates an Entity
		 * filled with random values. Subclasses can override and provide a hard coded instance or select one from the database.
		 * Note that this default implementation returns null in case the referenced entity type is read-only.
		 * @param foreignKey the foreign key referencing the entity
		 * @param foreignKeyEntities the entities referenced via foreign keys
		 * @return an entity for the given foreign key
		 * @throws DatabaseException in case of an exception
		 */
		Entity foreignKeyEntity(ForeignKey foreignKey, Map<ForeignKey, Entity> foreignKeyEntities);

		/**
		 * This method should return {@code entity} in a modified state
		 * @param entity the entity to modify
		 */
		void modify(Entity entity);

		/**
		 * This method should return {@code entity} in a modified state
		 * @param entity the entity to modify
		 * @param foreignKeyEntities the entities referenced via foreign keys
		 */
		void modify(Entity entity, Map<ForeignKey, Entity> foreignKeyEntities);

		/**
		 * Initializes the entities referenced by the entity identified by {@code entityType}
		 * @param entityType the type of the entity for which to initialize the referenced entities
		 * @param foreignKeyEntities foreign key entities already created
		 * @param connection the connection to use
		 * @return the reference entities mapped to their respective foreign keys
		 * @throws DatabaseException in case of an exception
		 * @see #foreignKeyEntities(EntityType, Map, EntityConnection)
		 */
		Map<ForeignKey, Entity> foreignKeyEntities(EntityType entityType, Map<ForeignKey, Entity> foreignKeyEntities,
																							 EntityConnection connection) throws DatabaseException;
	}

	/**
	 * @return the EntityConnection instance used by this DomainTest
	 */
	protected final EntityConnection connection() {
		return connectionProvider.connection();
	}

	/**
	 * Tests inserting the given entity
	 * @param entity the entity to test insert for
	 * @param connection the connection to use
	 * @return the same entity retrieved from the database after the insert
	 * @throws DatabaseException in case of an exception
	 */
	private static Entity testInsert(Entity entity, EntityConnection connection) throws DatabaseException {
		try {
			Entity insertedEntity = connection.insertSelect(entity);
			assertEquals(entity.primaryKey(), insertedEntity.primaryKey());
			entity.definition().columns().definitions().stream()
							.filter(ColumnDefinition::insertable)
							.forEach(columnDefinition -> assertValueEqual(entity, insertedEntity, columnDefinition));

			return insertedEntity;
		}
		catch (RecordNotFoundException e) {
			fail("Inserted entity of type " + entity.entityType() + " not returned by select after insert");
			throw e;
		}
	}

	/**
	 * Tests selecting the given entity, if {@code entity} is null
	 * then selecting many entities is tested.
	 * @param entityType the entityType in case {@code entity} is null
	 * @param entity the entity to test selecting
	 * @param connection the connection to use
	 * @throws DatabaseException in case of an exception
	 */
	private static void testSelect(EntityType entityType, Entity entity,
																 EntityConnection connection) throws DatabaseException {
		if (entity != null) {
			assertEquals(entity, connection.select(entity.primaryKey()),
							"Entity of type " + entity.entityType() + " failed equals comparison");
		}
		else {
			connection.select(Select.all(entityType)
							.limit(SELECT_LIMIT)
							.build());
		}
	}

	/**
	 * Test updating the given entity, if the entity is not modified this test does nothing
	 * @param entity the entity to test updating
	 * @param foreignKeyEntities the entities referenced via foreign keys
	 * @param connection the connection to use
	 * @throws DatabaseException in case of an exception
	 */
	private static void testUpdate(Entity entity, EntityConnection connection) throws DatabaseException {
		if (!entity.modified()) {
			return;
		}

		Entity updatedEntity = connection.updateSelect(entity);
		assertEquals(entity.primaryKey(), updatedEntity.primaryKey());
		entity.definition().columns().definitions().stream()
						.filter(ColumnDefinition::updatable)
						.forEach(columnDefinition -> assertValueEqual(entity, updatedEntity, columnDefinition));
	}

	/**
	 * Test deleting the given entity
	 * @param entity the entity to test deleting
	 * @param connection the connection to use
	 * @throws DatabaseException in case of an exception
	 */
	private static void testDelete(Entity entity, EntityConnection connection) throws DatabaseException {
		connection.delete(Entity.primaryKeys(singletonList(entity)));
		boolean caught = false;
		try {
			connection.select(entity.primaryKey());
		}
		catch (RecordNotFoundException e) {
			caught = true;
		}
		assertTrue(caught, "Entity of type " + entity.entityType() + " failed delete test");
	}

	private static void assertValueEqual(Entity entity, Entity updated, ColumnDefinition<?> columnDefinition) {
		Object beforeUpdate = entity.get(columnDefinition.attribute());
		Object afterUpdate = updated.get(columnDefinition.attribute());
		String message = "Values of column " + columnDefinition + " should be equal after update ["
						+ beforeUpdate + (beforeUpdate != null ? (" (" + beforeUpdate.getClass() + ")") : "") + ", "
						+ afterUpdate + (afterUpdate != null ? (" (" + afterUpdate.getClass() + ")") : "") + "]";
		if (columnDefinition.attribute().type().isBigDecimal()) {//special case, scale is not necessarily the same, hence not equal
			assertTrue((afterUpdate == beforeUpdate) || (afterUpdate != null
							&& ((BigDecimal) afterUpdate).compareTo((BigDecimal) beforeUpdate) == 0));
		}
		else if (columnDefinition.attribute().type().isByteArray() && !columnDefinition.lazy()) {
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

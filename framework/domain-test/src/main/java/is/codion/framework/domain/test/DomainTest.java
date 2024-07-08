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
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.math.BigDecimal;
import java.util.Optional;

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
		this(domain, new DefaultEntityFactory(requireNonNull(domain).entities()), user);
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
			if (entities().definition(entityType).readOnly()) {
				testSelect(entityType, connection);
			}
			else {
				entityFactory.populateForeignKeys(entityType, connection);
				Entity entity = testInsert(entityFactory.entity(entityType), connection);
				entityFactory.modify(entity);
				testUpdate(entity, connection);
				testSelect(entity, connection);
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
		 * Initializes the Entity instance on which to run the tests, by default this method creates an instance filled with random values.
		 * @param entityType the entityType for which to initialize an entity instance for testing
		 * @return the entity instance to use for testing the entity type
		 */
		Entity entity(EntityType entityType);

		/**
		 * Initializes an Entity instance to reference via the given foreign key, by default this method returns an non-existing Entity
		 * filled with random values. The resulting entity is inserted if it does not already exist.
		 * Note that this default implementation returns an empty Optional in case the referenced entity type is read-only.
		 * @param foreignKey the foreign key referencing the entity
		 * @return an entity for the given foreign key or an empty Optional if none is required
		 * @throws DatabaseException in case of an exception
		 */
		Optional<Entity> entity(ForeignKey foreignKey);

		/**
		 * Modifies one or more values in {@code entity}, for the update test.
		 * If the entity is not modified, the update test will not be run.
		 * The default implementation populates the entity with random values.
		 * @param entity the entity to modify
		 */
		void modify(Entity entity);

		/**
		 * Initializes the entities referenced by the given {@code entityType} via foreign keys
		 * @param entityType the type of the entity for which to initialize the referenced entities
		 * @param connection the connection to use
		 * @throws DatabaseException in case of an exception
		 */
		void populateForeignKeys(EntityType entityType, EntityConnection connection) throws DatabaseException;
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
		if (entity == null) {
			throw new IllegalStateException("EntityFactory.entity() must return a non-null entity");
		}
		try {
			Entity insertedEntity = connection.insertSelect(entity);
			assertEquals(entity.primaryKey(), insertedEntity.primaryKey());
			assertTrue(entity.primaryKey().isNotNull());
			entity.definition().columns().definitions().stream()
							.filter(ColumnDefinition::insertable)
							.forEach(columnDefinition -> assertValueEqual(columnDefinition, entity, insertedEntity));

			return insertedEntity;
		}
		catch (RecordNotFoundException e) {
			fail("Inserted entity of type " + entity.entityType() + " not returned by select after insert");
			throw e;
		}
	}

	/**
	 * Tests selecting the given entity, if {@code entity} is null
	 * then selecting multiple entities of the given type is tested.
	 * @param entity the entity to test selecting
	 * @param connection the connection to use
	 * @throws DatabaseException in case of an exception
	 */
	private static void testSelect(Entity entity, EntityConnection connection) throws DatabaseException {
		assertEquals(entity, connection.select(entity.primaryKey()),
						"Entity of type " + entity.entityType() + " failed equals comparison");
	}

	/**
	 * Tests selecting multiple entities of the given type.
	 * @param entityType the entityType
	 * @param connection the connection to use
	 * @throws DatabaseException in case of an exception
	 */
	private static void testSelect(EntityType entityType, EntityConnection connection) throws DatabaseException {
		connection.select(Select.all(entityType)
						.limit(SELECT_LIMIT)
						.build());
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
						.forEach(columnDefinition -> assertValueEqual(columnDefinition, entity, updatedEntity));
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

	private static void assertValueEqual(ColumnDefinition<?> columnDefinition, Entity original, Entity updated) {
		Object originalValue = original.get(columnDefinition.attribute());
		Object updatedValue = updated.get(columnDefinition.attribute());
		String message = createMessage(columnDefinition.attribute(), originalValue, updatedValue);
		if (columnDefinition.attribute().type().isBigDecimal()) {//special case, scale is not necessarily the same, hence not equal
			assertTrue((updatedValue == originalValue) || (updatedValue != null
							&& ((BigDecimal) updatedValue).compareTo((BigDecimal) originalValue) == 0));
		}
		else if (columnDefinition.attribute().type().isByteArray() && !columnDefinition.lazy()) {
			assertArrayEquals((byte[]) originalValue, (byte[]) updatedValue, message);
		}
		else {
			assertEquals(originalValue, updatedValue, message);
		}
	}

	private static String createMessage(Column<?> column, Object originalValue, Object updatedValue) {
		return "Values of column " + column + " should be equal ["
						+ originalValue + (originalValue != null ? (" (" + originalValue.getClass() + ")") : "") + ", "
						+ updatedValue + (updatedValue != null ? (" (" + updatedValue.getClass() + ")") : "") + "]";
	}

	private static User initializeDefaultUser() {
		return User.parse(TEST_USER.getOrThrow());
	}
}

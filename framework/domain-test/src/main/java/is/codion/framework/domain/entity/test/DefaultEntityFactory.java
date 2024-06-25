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
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.test;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.item.Item;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;

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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.IntStream;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * Handles creating and manipulating Entity instances for testing purposes.
 */
public class DefaultEntityFactory implements EntityTestUnit.EntityFactory {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultEntityFactory.class);

	private static final int MININUM_RANDOM_NUMBER = -10_000;
	private static final int MAXIMUM_RANDOM_NUMBER = 10_000;
	private static final int MAXIMUM_RANDOM_STRING_LENGTH = 10;
	private static final String ALPHA_NUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final Random RANDOM = new Random();

	private final Entities entities;

	public DefaultEntityFactory(Entities entities) {
		this.entities = requireNonNull(entities);
	}

	@Override
	public Entity entity(EntityType entityType) {
		return entity(entityType, emptyMap());
	}

	@Override
	public Entity entity(EntityType entityType, Map<ForeignKey, Entity> foreignKeyEntities) {
		return randomEntity(entityType, foreignKeyEntities);
	}

	@Override
	public Entity foreignKeyEntity(ForeignKey foreignKey, Map<ForeignKey, Entity> foreignKeyEntities) {
		if (entities.definition(requireNonNull(foreignKey).referencedType()).readOnly()) {
			return null;
		}

		return randomEntity(foreignKey.referencedType(), foreignKeyEntities);
	}

	@Override
	public void modify(Entity entity) {
		modify(entity, null);
	}

	@Override
	public void modify(Entity entity, Map<ForeignKey, Entity> foreignKeyEntities) {
		randomize(entity, foreignKeyEntities);
	}

	@Override
	public Map<ForeignKey, Entity> foreignKeyEntities(EntityType entityType,
																										Map<ForeignKey, Entity> foreignKeyEntities,
																										EntityConnection connection) throws DatabaseException {
		List<ForeignKey> foreignKeys = new ArrayList<>(entities.definition(entityType).foreignKeys().get());
		//we have to start with non-self-referential ones
		foreignKeys.sort((fk1, fk2) -> !fk1.referencedType().equals(entityType) ? -1 : 1);
		for (ForeignKey foreignKey : foreignKeys) {
			EntityType referencedEntityType = foreignKey.referencedType();
			if (!foreignKeyEntities.containsKey(foreignKey)) {
				if (!Objects.equals(entityType, referencedEntityType)) {
					foreignKeyEntities.put(foreignKey, null);//short circuit recursion, value replaced below
					foreignKeyEntities(referencedEntityType, foreignKeyEntities, connection);
				}
				Entity referencedEntity = foreignKeyEntity(foreignKey, foreignKeyEntities);
				if (referencedEntity != null) {
					foreignKeyEntities.put(foreignKey, insertOrSelect(referencedEntity, connection));
				}
			}
		}

		return foreignKeyEntities;
	}

	/**
	 * @return the underlying {@link Entities} instance
	 */
	protected final Entities entities() {
		return entities;
	}

	private Entity randomEntity(EntityType entityType, Map<ForeignKey, Entity> foreignKeyEntities) {
		return createRandomEntity(entityType, foreignKeyEntities);
	}

	/**
	 * @param entities the domain model entities
	 * @param entityType the entityType
	 * @param referenceEntities entities referenced by the given foreign key
	 * @return an Entity instance containing randomized values, based on the attribute definitions
	 */
	private Entity createRandomEntity(EntityType entityType, Map<ForeignKey, Entity> referenceEntities) {
		return createEntity(entityType, definition -> createValue(definition, referenceEntities));
	}

	/**
	 * @param entities the domain model entities
	 * @param entityType the entityType
	 * @param valueProvider the value provider
	 * @return an Entity instance with insertable attributes populated with values provided by the given value provider
	 */
	private Entity createEntity(EntityType entityType, Function<Attribute<?>, Object> valueProvider) {
		requireNonNull(entityType);
		Entity entity = entities.entity(entityType);
		populateEntity(entity, insertableColumns(entities.definition(entityType)), valueProvider);

		return entity;
	}

	/**
	 * Randomizes updatable attribute values in the given entity, note that if a foreign key entity is not provided
	 * the respective foreign key value in not modified
	 * @param entities the domain model entities
	 * @param entity the entity to randomize
	 * @param foreignKeyEntities the entities referenced via foreign keys
	 */
	private void randomize(Entity entity, Map<ForeignKey, Entity> foreignKeyEntities) {
		requireNonNull(entity);
		populateEntity(entity,
						updatableColumns(entity.definition()),
						attributeDefinition -> createValue(attributeDefinition, foreignKeyEntities));
	}

	/**
	 * Creates a random value for the given attribute.
	 * @param attribute the attribute
	 * @param referenceEntities entities referenced by the given attribute
	 * @param <T> the attribute value type
	 * @return a random value
	 */
	protected <T> T createValue(Attribute<T> attribute, Map<ForeignKey, Entity> referenceEntities) {
		requireNonNull(attribute, "attribute");
		AttributeDefinition<T> attributeDefinition = entities.definition(attribute.entityType()).attributes().definition(attribute);
		try {
			if (attributeDefinition instanceof ForeignKeyDefinition) {
				return (T) referenceEntity(((ForeignKeyDefinition) attributeDefinition).attribute(), referenceEntities);
			}
			if (!attributeDefinition.items().isEmpty()) {
				return randomItem(attributeDefinition);
			}
			if (attribute.type().isBoolean()) {
				return (T) Boolean.valueOf(RANDOM.nextBoolean());
			}
			if (attribute.type().isCharacter()) {
				return (T) Character.valueOf((char) RANDOM.nextInt());
			}
			if (attribute.type().isLocalDate()) {
				return (T) LocalDate.now();
			}
			if (attribute.type().isLocalDateTime()) {
				return (T) LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
			}
			if (attribute.type().isOffsetDateTime()) {
				return (T) OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
			}
			if (attribute.type().isLocalTime()) {
				return (T) LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
			}
			if (attribute.type().isDouble()) {
				return (T) Double.valueOf(randomDouble(attributeDefinition));
			}
			if (attribute.type().isBigDecimal()) {
				return (T) BigDecimal.valueOf(randomDouble(attributeDefinition));
			}
			if (attribute.type().isInteger()) {
				return (T) Integer.valueOf(randomInteger(attributeDefinition));
			}
			if (attribute.type().isLong()) {
				return (T) Long.valueOf(randomLong(attributeDefinition));
			}
			if (attribute.type().isShort()) {
				return (T) Short.valueOf(randomShort(attributeDefinition));
			}
			if (attribute.type().isString()) {
				return (T) randomString(attributeDefinition);
			}
			if (attribute.type().isByteArray()) {
				return (T) randomBlob(attributeDefinition);
			}
			if (attribute.type().isEnum()) {
				return randomEnum(attribute);
			}

			return null;
		}
		catch (RuntimeException e) {
			LOG.error("Exception while creating random value for: {}", attributeDefinition.attribute(), e);
			throw e;
		}
	}

	private static void populateEntity(Entity entity, Collection<Column<?>> columns,
																		 Function<Attribute<?>, Object> valueProvider) {
		requireNonNull(valueProvider, "valueProvider");
		EntityDefinition definition = entity.definition();
		for (Column<?> column : columns) {
			if (!definition.foreignKeys().foreignKeyColumn(column)) {
				entity.put((Attribute<Object>) column, valueProvider.apply(column));
			}
		}
		for (ForeignKey foreignKey : entity.definition().foreignKeys().get()) {
			Entity value = (Entity) valueProvider.apply(foreignKey);
			if (value != null) {
				entity.put(foreignKey, value);
			}
		}
	}

	private static List<Column<?>> insertableColumns(EntityDefinition entityDefinition) {
		return entityDefinition.columns().definitions().stream()
						.filter(column -> column.insertable() && (!entityDefinition.primaryKey().generated() || !column.primaryKey()))
						.map(ColumnDefinition::attribute)
						.collect(toList());
	}

	private static List<Column<?>> updatableColumns(EntityDefinition entityDefinition) {
		return entityDefinition.columns().definitions().stream()
						.filter(column -> column.updatable() && !column.primaryKey())
						.map(ColumnDefinition::attribute)
						.collect(toList());
	}

	private static String randomString(AttributeDefinition<?> attributeDefinition) {
		int length = attributeDefinition.maximumLength() < 0 ? MAXIMUM_RANDOM_STRING_LENGTH : attributeDefinition.maximumLength();

		return IntStream.range(0, length)
						.mapToObj(i -> String.valueOf(ALPHA_NUMERIC.charAt(RANDOM.nextInt(ALPHA_NUMERIC.length()))))
						.collect(joining());
	}

	private static byte[] randomBlob(AttributeDefinition<?> attributeDefinition) {
		if (attributeDefinition.attribute().type().isByteArray() &&
						attributeDefinition instanceof ColumnDefinition &&
						!((ColumnDefinition<?>) attributeDefinition).lazy()) {
			return randomBlob(1024);
		}

		return null;
	}

	private static byte[] randomBlob(int numberOfBytes) {
		byte[] bytes = new byte[numberOfBytes];
		RANDOM.nextBytes(bytes);

		return bytes;
	}

	private static <T> T randomEnum(Attribute<?> attribute) {
		Object[] enumConstants = attribute.type().valueClass().getEnumConstants();

		return (T) enumConstants[RANDOM.nextInt(enumConstants.length)];
	}

	private static Entity referenceEntity(ForeignKey foreignKey, Map<ForeignKey, Entity> referenceEntities) {
		return referenceEntities == null ? null : referenceEntities.get(foreignKey);
	}

	private static <T> T randomItem(AttributeDefinition<T> attributeDefinition) {
		List<Item<T>> items = attributeDefinition.items();
		Item<T> item = items.get(RANDOM.nextInt(items.size()));

		return item.get();
	}

	private static int randomInteger(AttributeDefinition<?> attributeDefinition) {
		int min = attributeDefinition.minimumValue() == null ?
						MININUM_RANDOM_NUMBER : Math.max(attributeDefinition.minimumValue().intValue(), MININUM_RANDOM_NUMBER);
		int max = attributeDefinition.maximumValue() == null ?
						MAXIMUM_RANDOM_NUMBER : Math.min(attributeDefinition.maximumValue().intValue(), MAXIMUM_RANDOM_NUMBER);

		return RANDOM.nextInt((max - min) + 1) + min;
	}

	private static long randomLong(AttributeDefinition<?> attributeDefinition) {
		long min = attributeDefinition.minimumValue() == null ?
						MININUM_RANDOM_NUMBER : Math.max(attributeDefinition.minimumValue().longValue(), MININUM_RANDOM_NUMBER);
		long max = attributeDefinition.maximumValue() == null ?
						MAXIMUM_RANDOM_NUMBER : Math.min(attributeDefinition.maximumValue().longValue(), MAXIMUM_RANDOM_NUMBER);

		return RANDOM.nextLong() % (max - min) + min;
	}

	private static short randomShort(AttributeDefinition<?> attributeDefinition) {
		short min = attributeDefinition.minimumValue() == null ?
						MININUM_RANDOM_NUMBER : (short) Math.max(attributeDefinition.minimumValue().intValue(), MININUM_RANDOM_NUMBER);
		short max = attributeDefinition.maximumValue() == null ?
						MAXIMUM_RANDOM_NUMBER : (short) Math.min(attributeDefinition.maximumValue().intValue(), MAXIMUM_RANDOM_NUMBER);

		return (short) (RANDOM.nextInt((max - min) + 1) + min);
	}

	private static double randomDouble(AttributeDefinition<?> attributeDefinition) {
		double min = attributeDefinition.minimumValue() == null ?
						MININUM_RANDOM_NUMBER : Math.max(attributeDefinition.minimumValue().doubleValue(), MININUM_RANDOM_NUMBER);
		double max = attributeDefinition.maximumValue() == null ?
						MAXIMUM_RANDOM_NUMBER : Math.min(attributeDefinition.maximumValue().doubleValue(), MAXIMUM_RANDOM_NUMBER);

		return RANDOM.nextDouble() * (max - min) + min;
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
			throw new DatabaseException(e.getMessage() + ": " + entity);
		}
	}
}

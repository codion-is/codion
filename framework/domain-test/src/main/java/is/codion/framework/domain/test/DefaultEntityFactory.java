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
 * Copyright (c) 2021 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.test;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.utilities.item.Item;
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
import is.codion.framework.domain.entity.attribute.ValueAttributeDefinition;
import is.codion.framework.domain.test.DomainTest.EntityFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * Handles creating and manipulating Entity instances for testing purposes.
 */
public class DefaultEntityFactory implements EntityFactory {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultEntityFactory.class);

	private static final int MININUM_RANDOM_NUMBER = -10_000;
	private static final int MAXIMUM_RANDOM_NUMBER = 10_000;
	private static final int MAXIMUM_RANDOM_STRING_LENGTH = 10;
	private static final String ALPHA_NUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final Random RANDOM = new Random();

	private final EntityConnection connection;
	private final Entities entities;
	private final Map<ForeignKey, Entity> foreignKeyEntities = new HashMap<>();

	/**
	 * Instantiates a new {@link DefaultEntityFactory}
	 * @param connection the connection to use
	 */
	public DefaultEntityFactory(EntityConnection connection) {
		this.connection = requireNonNull(connection);
		this.entities = connection.entities();
	}

	@Override
	public Entity entity(EntityType entityType) {
		Entity entity = entities.entity(requireNonNull(entityType)).build();
		populate(entity, insertableColumns(entities.definition(entityType)));

		return entity;
	}

	@Override
	public Optional<Entity> entity(ForeignKey foreignKey) {
		if (entities.definition(requireNonNull(foreignKey).referencedType()).readOnly()) {
			return Optional.empty();
		}
		if (foreignKeyEntities.containsKey(foreignKey)) {
			return Optional.ofNullable(foreignKeyEntities.get(foreignKey));
		}

		foreignKeyEntities.put(foreignKey, null);// short curcuit recursion
		Entity entity = insertOrSelect(entity(foreignKey.referencedType()), connection);
		foreignKeyEntities.put(foreignKey, entity);

		return Optional.of(entity);
	}

	@Override
	public void modify(Entity entity) {
		populate(requireNonNull(entity), updatableColumns(entity.definition()));
	}

	/**
	 * @return the underlying {@link EntityConnection} instance
	 */
	protected final EntityConnection connection() {
		return connection;
	}

	/**
	 * @return the underlying {@link Entities} instance
	 */
	protected final Entities entities() {
		return entities;
	}

	/**
	 * Creates a value for the given attribute.
	 * @param attribute the attribute
	 * @param <T> the attribute value type
	 * @return a random value
	 * @throws DatabaseException in case of an exception
	 */
	protected <T> T value(Attribute<T> attribute) {
		requireNonNull(attribute);
		try {
			if (attribute instanceof ForeignKey) {
				return (T) entity((ForeignKey) attribute).orElse(null);
			}
			AttributeDefinition<T> definition = entities.definition(attribute.entityType()).attributes().definition(attribute);
			if (!(definition instanceof ValueAttributeDefinition<T>)) {
				return null;
			}
			ValueAttributeDefinition<T> attributeDefinition = (ValueAttributeDefinition<T>) definition;
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
				return (T) randomByteArray(attributeDefinition);
			}
			if (attribute.type().isEnum()) {
				return randomEnum(attribute);
			}
			if (attribute.type().valueClass().equals(UUID.class)) {
				return (T) UUID.randomUUID();
			}

			return null;
		}
		catch (Exception e) {
			LOG.error("Exception while fetching a value for: {}", attribute, e);
			if (e instanceof RuntimeException) {
				throw e;
			}

			throw new RuntimeException(e);
		}
	}

	private void populate(Entity entity, Collection<Column<?>> columns) {
		EntityDefinition definition = entity.definition();
		for (Column<?> column : columns) {
			if (!definition.foreignKeys().foreignKeyColumn(column)) {
				entity.set((Attribute<Object>) column, value(column));
			}
		}
		for (ForeignKey foreignKey : entity.definition().foreignKeys().get()) {
			Entity value = value(foreignKey);
			if (value != null) {
				entity.set(foreignKey, value);
			}
		}
	}

	private static List<Column<?>> insertableColumns(EntityDefinition entityDefinition) {
		return entityDefinition.columns().definitions().stream()
						.filter(column -> column.insertable() && (!column.generated() || !column.primaryKey()))
						.map(ColumnDefinition::attribute)
						.collect(toList());
	}

	private static List<Column<?>> updatableColumns(EntityDefinition entityDefinition) {
		return entityDefinition.columns().definitions().stream()
						.filter(column -> column.updatable() && !column.primaryKey())
						.map(ColumnDefinition::attribute)
						.collect(toList());
	}

	private static String randomString(ValueAttributeDefinition<?> attributeDefinition) {
		int length = attributeDefinition.maximumLength() < 0 ? MAXIMUM_RANDOM_STRING_LENGTH : attributeDefinition.maximumLength();

		return IntStream.range(0, length)
						.mapToObj(i -> String.valueOf(ALPHA_NUMERIC.charAt(RANDOM.nextInt(ALPHA_NUMERIC.length()))))
						.collect(joining());
	}

	private static byte[] randomByteArray(AttributeDefinition<?> attributeDefinition) {
		if (attributeDefinition.attribute().type().isByteArray() &&
						attributeDefinition instanceof ColumnDefinition &&
						((ColumnDefinition<?>) attributeDefinition).selected()) {
			return randomByteArray(1024);
		}

		return null;
	}

	private static byte[] randomByteArray(int numberOfBytes) {
		byte[] bytes = new byte[numberOfBytes];
		RANDOM.nextBytes(bytes);

		return bytes;
	}

	private static <T> T randomEnum(Attribute<?> attribute) {
		Object[] enumConstants = attribute.type().valueClass().getEnumConstants();

		return (T) enumConstants[RANDOM.nextInt(enumConstants.length)];
	}

	private static <T> T randomItem(ValueAttributeDefinition<T> attributeDefinition) {
		List<Item<T>> items = attributeDefinition.items();
		Item<T> item = items.get(RANDOM.nextInt(items.size()));

		return item.get();
	}

	private static int randomInteger(ValueAttributeDefinition<?> attributeDefinition) {
		int min = attributeDefinition.minimum()
						.map(minimum -> Math.max(minimum.intValue(), MININUM_RANDOM_NUMBER))
						.orElse(MININUM_RANDOM_NUMBER);
		int max = attributeDefinition.maximum()
						.map(maximum -> Math.min(maximum.intValue(), MAXIMUM_RANDOM_NUMBER))
						.orElse(MAXIMUM_RANDOM_NUMBER);

		return RANDOM.nextInt((max - min) + 1) + min;
	}

	private static long randomLong(ValueAttributeDefinition<?> attributeDefinition) {
		long min = attributeDefinition.minimum()
						.map(minimum -> Math.max(minimum.longValue(), MININUM_RANDOM_NUMBER))
						.orElse((long) MININUM_RANDOM_NUMBER);
		long max = attributeDefinition.maximum()
						.map(maximum -> Math.min(maximum.longValue(), MAXIMUM_RANDOM_NUMBER))
						.orElse((long) MAXIMUM_RANDOM_NUMBER);

		return RANDOM.nextLong() % (max - min) + min;
	}

	private static short randomShort(ValueAttributeDefinition<?> attributeDefinition) {
		short min = attributeDefinition.minimum()
						.map(minimum -> (short) Math.max(minimum.intValue(), MININUM_RANDOM_NUMBER))
						.orElse((short) MININUM_RANDOM_NUMBER);
		short max = attributeDefinition.maximum()
						.map(maximum -> (short) Math.min(maximum.intValue(), MAXIMUM_RANDOM_NUMBER))
						.orElse((short) MAXIMUM_RANDOM_NUMBER);

		return (short) (RANDOM.nextInt((max - min) + 1) + min);
	}

	private static double randomDouble(ValueAttributeDefinition<?> attributeDefinition) {
		double min = attributeDefinition.minimum()
						.map(minimum -> Math.max(minimum.doubleValue(), MININUM_RANDOM_NUMBER))
						.orElse((double) MININUM_RANDOM_NUMBER);
		double max = attributeDefinition.maximum()
						.map(maximum -> Math.min(maximum.doubleValue(), MAXIMUM_RANDOM_NUMBER))
						.orElse((double) MAXIMUM_RANDOM_NUMBER);

		return RANDOM.nextDouble() * (max - min) + min;
	}

	private static Entity insertOrSelect(Entity entity, EntityConnection connection) {
		if (!entity.primaryKey().isNull()) {
			Collection<Entity> selected = connection.select(singletonList(entity.primaryKey()));
			if (!selected.isEmpty()) {
				return selected.iterator().next();
			}
		}

		return connection.insertSelect(entity);
	}
}

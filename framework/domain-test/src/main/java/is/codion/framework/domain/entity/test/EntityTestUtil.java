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
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.test;

import is.codion.common.Text;
import is.codion.common.item.Item;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.BlobColumnDefinition;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.domain.entity.attribute.ItemColumnDefinition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * Utility methods for creating and manipulating Entity instances for testing purposes.
 */
public final class EntityTestUtil {

  private static final Logger LOG = LoggerFactory.getLogger(EntityTestUtil.class);

  private static final int MININUM_RANDOM_NUMBER = -10_000;
  private static final int MAXIMUM_RANDOM_NUMBER = 10_000;
  private static final int MAXIMUM_RANDOM_STRING_LENGTH = 10;
  private static final Random RANDOM = new Random();

  private EntityTestUtil() {}

  /**
   * @param entities the domain model entities
   * @param entityType the entityType
   * @param referenceEntities entities referenced by the given foreign key
   * @return an Entity instance containing randomized values, based on the attribute definitions
   */
  public static Entity createRandomEntity(Entities entities, EntityType entityType,
                                          Map<ForeignKey, Entity> referenceEntities) {
    return createEntity(entities, entityType, definition -> createRandomValue(definition, referenceEntities));
  }

  /**
   * @param entities the domain model entities
   * @param entityType the entityType
   * @param valueProvider the value provider
   * @return an Entity instance with insertable attributes populated with values provided by the given value provider
   */
  public static Entity createEntity(Entities entities, EntityType entityType, Function<AttributeDefinition<?>, Object> valueProvider) {
    requireNonNull(entities);
    requireNonNull(entityType);
    Entity entity = entities.entity(entityType);
    populateEntity(entity, insertableColumnDefinitions(entities.definition(entityType)), valueProvider);

    return entity;
  }

  /**
   * Randomizes updatable attribute values in the given entity, note that if a foreign key entity is not provided
   * the respective foreign key value in not modified
   * @param entities the domain model entities
   * @param entity the entity to randomize
   * @param foreignKeyEntities the entities referenced via foreign keys
   */
  public static void randomize(Entities entities, Entity entity, Map<ForeignKey, Entity> foreignKeyEntities) {
    requireNonNull(entities);
    requireNonNull(entity);
    populateEntity(entity,
            updatableColumnDefinitions(entity.definition()),
            attributeDefinition -> createRandomValue(attributeDefinition, foreignKeyEntities));
  }

  /**
   * Creates a random value for the given attribute.
   * @param attributeDefinition the attribute definition
   * @param referenceEntities entities referenced by the given attribute
   * @param <T> the attribute value type
   * @return a random value
   */
  public static <T> T createRandomValue(AttributeDefinition<T> attributeDefinition, Map<ForeignKey, Entity> referenceEntities) {
    requireNonNull(attributeDefinition, "attributeDefinition");
    try {
      if (attributeDefinition instanceof ForeignKeyDefinition) {
        return (T) referenceEntity(((ForeignKeyDefinition) attributeDefinition).attribute(), referenceEntities);
      }
      if (attributeDefinition instanceof ItemColumnDefinition) {
        return randomItem((ItemColumnDefinition<T>) attributeDefinition);
      }
      Attribute<?> attribute = attributeDefinition.attribute();
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
      LOG.error("Exception while creating random value for: " + attributeDefinition.attribute(), e);
      throw e;
    }
  }

  private static void populateEntity(Entity entity, Collection<ColumnDefinition<?>> columnDefinitions,
                                     Function<AttributeDefinition<?>, Object> valueProvider) {
    requireNonNull(valueProvider, "valueProvider");
    EntityDefinition definition = entity.definition();
    for (ColumnDefinition<?> columnDefinition : columnDefinitions) {
      if (!definition.foreignKeys().foreignKeyColumn(columnDefinition.attribute())) {
        entity.put((Attribute<Object>) columnDefinition.attribute(), valueProvider.apply(columnDefinition));
      }
    }
    for (ForeignKeyDefinition foreignKeyDefinition : entity.definition().foreignKeys().definitions()) {
      Entity value = (Entity) valueProvider.apply(foreignKeyDefinition);
      if (value != null) {
        entity.put(foreignKeyDefinition.attribute(), value);
      }
    }
  }

  private static List<ColumnDefinition<?>> insertableColumnDefinitions(EntityDefinition entityDefinition) {
    return entityDefinition.columns().definitions().stream()
            .filter(column -> column.insertable() && (!entityDefinition.primaryKey().generated() || !column.primaryKey()))
            .collect(toList());
  }

  private static List<ColumnDefinition<?>> updatableColumnDefinitions(EntityDefinition entityDefinition) {
    return entityDefinition.columns().definitions().stream()
            .filter(column -> column.updatable() && !column.primaryKey())
            .collect(toList());
  }

  private static String randomString(AttributeDefinition<?> attributeDefinition) {
    int length = attributeDefinition.maximumLength() < 0 ? MAXIMUM_RANDOM_STRING_LENGTH : attributeDefinition.maximumLength();

    return Text.randomString(length, length);
  }

  private static byte[] randomBlob(AttributeDefinition<?> attributeDefinition) {
    if ((attributeDefinition instanceof BlobColumnDefinition) && ((BlobColumnDefinition) attributeDefinition).eagerlyLoaded()) {
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

  private static <T> T randomItem(ItemColumnDefinition<T> columnDefinition) {
    List<Item<T>> items = columnDefinition.items();
    Item<T> item = items.get(RANDOM.nextInt(items.size()));

    return item.get();
  }

  private static int randomInteger(AttributeDefinition<?> attributeDefinition) {
    int min = attributeDefinition.minimumValue() == null ? MININUM_RANDOM_NUMBER : Math.max(attributeDefinition.minimumValue().intValue(), MININUM_RANDOM_NUMBER);
    int max = attributeDefinition.maximumValue() == null ? MAXIMUM_RANDOM_NUMBER : Math.min(attributeDefinition.maximumValue().intValue(), MAXIMUM_RANDOM_NUMBER);

    return RANDOM.nextInt((max - min) + 1) + min;
  }

  private static long randomLong(AttributeDefinition<?> attributeDefinition) {
    long min = attributeDefinition.minimumValue() == null ? MININUM_RANDOM_NUMBER : Math.max(attributeDefinition.minimumValue().longValue(), MININUM_RANDOM_NUMBER);
    long max = attributeDefinition.maximumValue() == null ? MAXIMUM_RANDOM_NUMBER : Math.min(attributeDefinition.maximumValue().longValue(), MAXIMUM_RANDOM_NUMBER);

    return RANDOM.nextLong() % (max - min) + min;
  }

  private static short randomShort(AttributeDefinition<?> attributeDefinition) {
    short min = attributeDefinition.minimumValue() == null ? MININUM_RANDOM_NUMBER : (short) Math.max(attributeDefinition.minimumValue().intValue(), MININUM_RANDOM_NUMBER);
    short max = attributeDefinition.maximumValue() == null ? MAXIMUM_RANDOM_NUMBER : (short) Math.min(attributeDefinition.maximumValue().intValue(), MAXIMUM_RANDOM_NUMBER);

    return (short) (RANDOM.nextInt((max - min) + 1) + min);
  }

  private static double randomDouble(AttributeDefinition<?> attributeDefinition) {
    double min = attributeDefinition.minimumValue() == null ? MININUM_RANDOM_NUMBER : Math.max(attributeDefinition.minimumValue().doubleValue(), MININUM_RANDOM_NUMBER);
    double max = attributeDefinition.maximumValue() == null ? MAXIMUM_RANDOM_NUMBER : Math.min(attributeDefinition.maximumValue().doubleValue(), MAXIMUM_RANDOM_NUMBER);

    return RANDOM.nextDouble() * (max - min) + min;
  }
}

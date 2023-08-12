/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.test;

import is.codion.common.Text;
import is.codion.common.item.Item;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.BlobProperty;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.ItemProperty;
import is.codion.framework.domain.property.Property;

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
   * @return an Entity instance containing randomized values, based on the property definitions
   */
  public static Entity createRandomEntity(Entities entities, EntityType entityType,
                                          Map<ForeignKey, Entity> referenceEntities) {
    return createEntity(entities, entityType, property -> createRandomValue(property, referenceEntities));
  }

  /**
   * @param entities the domain model entities
   * @param entityType the entityType
   * @param valueProvider the value provider
   * @return an Entity instance initialized with values provided by the given value provider
   */
  public static Entity createEntity(Entities entities, EntityType entityType, Function<Property<?>, Object> valueProvider) {
    requireNonNull(entities);
    requireNonNull(entityType);
    Entity entity = entities.entity(entityType);
    populateEntity(entity, entities.definition(entityType).writableColumnProperties(
            !entities.definition(entityType).isKeyGenerated(), true), valueProvider);

    return entity;
  }

  /**
   * Randomizes the values in the given entity, note that if a foreign key entity is not provided
   * the respective foreign key value in not modified
   * @param entities the domain model entities
   * @param entity the entity to randomize
   * @param foreignKeyEntities the entities referenced via foreign keys
   */
  public static void randomize(Entities entities, Entity entity, Map<ForeignKey, Entity> foreignKeyEntities) {
    requireNonNull(entities);
    requireNonNull(entity);
    populateEntity(entity,
            entity.definition().writableColumnProperties(false, true),
            property -> createRandomValue(property, foreignKeyEntities));
  }

  /**
   * Creates a random value for the given property.
   * @param property the property
   * @param referenceEntities entities referenced by the given property
   * @param <T> the property value type
   * @return a random value
   */
  public static <T> T createRandomValue(Property<T> property, Map<ForeignKey, Entity> referenceEntities) {
    requireNonNull(property, "property");
    try {
      if (property instanceof ForeignKeyProperty) {
        return (T) referenceEntity(((ForeignKeyProperty) property).attribute(), referenceEntities);
      }
      if (property instanceof ItemProperty) {
        return randomItem((ItemProperty<T>) property);
      }
      Attribute<?> attribute = property.attribute();
      if (attribute.isBoolean()) {
        return (T) Boolean.valueOf(RANDOM.nextBoolean());
      }
      if (attribute.isCharacter()) {
        return (T) Character.valueOf((char) RANDOM.nextInt());
      }
      if (attribute.isLocalDate()) {
        return (T) LocalDate.now();
      }
      if (attribute.isLocalDateTime()) {
        return (T) LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
      }
      if (attribute.isOffsetDateTime()) {
        return (T) OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
      }
      if (attribute.isLocalTime()) {
        return (T) LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
      }
      if (attribute.isDouble()) {
        return (T) Double.valueOf(randomDouble(property));
      }
      if (attribute.isBigDecimal()) {
        return (T) BigDecimal.valueOf(randomDouble(property));
      }
      if (attribute.isInteger()) {
        return (T) Integer.valueOf(randomInteger(property));
      }
      if (attribute.isLong()) {
        return (T) Long.valueOf(randomLong(property));
      }
      if (attribute.isShort()) {
        return (T) Short.valueOf(randomShort(property));
      }
      if (attribute.isString()) {
        return (T) randomString(property);
      }
      if (attribute.isByteArray()) {
        return (T) randomBlob(property);
      }
      if (attribute.isEnum()) {
        return randomEnum(attribute);
      }

      return null;
    }
    catch (RuntimeException e) {
      LOG.error("Exception while creating random value for: " + property.attribute(), e);
      throw e;
    }
  }

  private static void populateEntity(Entity entity, Collection<ColumnProperty<?>> properties,
                                     Function<Property<?>, Object> valueProvider) {
    requireNonNull(valueProvider, "valueProvider");
    EntityDefinition definition = entity.definition();
    for (ColumnProperty<?> property : properties) {
      if (!definition.isForeignKeyColumn(property.attribute())) {
        entity.put((Attribute<Object>) property.attribute(), valueProvider.apply(property));
      }
    }
    for (ForeignKeyProperty property : entity.definition().foreignKeyProperties()) {
      Entity value = (Entity) valueProvider.apply(property);
      if (value != null) {
        entity.put(property.attribute(), value);
      }
    }
  }

  private static String randomString(Property<?> property) {
    int length = property.maximumLength() < 0 ? MAXIMUM_RANDOM_STRING_LENGTH : property.maximumLength();

    return Text.randomString(length, length);
  }

  private static byte[] randomBlob(Property<?> property) {
    if ((property instanceof BlobProperty) && ((BlobProperty) property).isEagerlyLoaded()) {
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
    Object[] enumConstants = attribute.valueClass().getEnumConstants();

    return (T) enumConstants[RANDOM.nextInt(enumConstants.length)];
  }

  private static Entity referenceEntity(ForeignKey foreignKey, Map<ForeignKey, Entity> referenceEntities) {
    return referenceEntities == null ? null : referenceEntities.get(foreignKey);
  }

  private static <T> T randomItem(ItemProperty<T> property) {
    List<Item<T>> items = property.items();
    Item<T> item = items.get(RANDOM.nextInt(items.size()));

    return item.get();
  }

  private static int randomInteger(Property<?> property) {
    int min = property.minimumValue() == null ? MININUM_RANDOM_NUMBER : Math.max(property.minimumValue().intValue(), MININUM_RANDOM_NUMBER);
    int max = property.maximumValue() == null ? MAXIMUM_RANDOM_NUMBER : Math.min(property.maximumValue().intValue(), MAXIMUM_RANDOM_NUMBER);

    return RANDOM.nextInt((max - min) + 1) + min;
  }

  private static long randomLong(Property<?> property) {
    long min = property.minimumValue() == null ? MININUM_RANDOM_NUMBER : Math.max(property.minimumValue().longValue(), MININUM_RANDOM_NUMBER);
    long max = property.maximumValue() == null ? MAXIMUM_RANDOM_NUMBER : Math.min(property.maximumValue().longValue(), MAXIMUM_RANDOM_NUMBER);

    return RANDOM.nextLong() % (max - min) + min;
  }

  private static short randomShort(Property<?> property) {
    short min = property.minimumValue() == null ? MININUM_RANDOM_NUMBER : (short) Math.max(property.minimumValue().intValue(), MININUM_RANDOM_NUMBER);
    short max = property.maximumValue() == null ? MAXIMUM_RANDOM_NUMBER : (short) Math.min(property.maximumValue().intValue(), MAXIMUM_RANDOM_NUMBER);

    return (short) (RANDOM.nextInt((max - min) + 1) + min);
  }

  private static double randomDouble(Property<?> property) {
    double min = property.minimumValue() == null ? MININUM_RANDOM_NUMBER : Math.max(property.minimumValue().doubleValue(), MININUM_RANDOM_NUMBER);
    double max = property.maximumValue() == null ? MAXIMUM_RANDOM_NUMBER : Math.min(property.maximumValue().doubleValue(), MAXIMUM_RANDOM_NUMBER);

    return RANDOM.nextDouble() * (max - min) + min;
  }
}

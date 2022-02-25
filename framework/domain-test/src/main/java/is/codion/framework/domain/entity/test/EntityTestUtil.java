/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.test;

import is.codion.common.Text;
import is.codion.common.item.Item;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.property.BlobProperty;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.ItemProperty;
import is.codion.framework.domain.property.Property;

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

  private static final int MININUM_RANDOM_NUMBER = -10000000;
  private static final int MAXIMUM_RANDOM_NUMBER = 10000000;
  private static final int MAXIMUM_RANDOM_STRING_LENGTH = 10;
  private static final Random RANDOM = new Random();

  private EntityTestUtil() {}

  /**
   * @param entities the domain model entities
   * @param entityType the entityType
   * @param referenceEntities entities referenced by the given entityType
   * @return an Entity instance containing randomized values, based on the property definitions
   */
  public static Entity createRandomEntity(final Entities entities, final EntityType entityType,
                                          final Map<EntityType, Entity> referenceEntities) {
    return createEntity(entities, entityType, property -> createRandomValue(property, referenceEntities));
  }

  /**
   * @param entities the domain model entities
   * @param entityType the entityType
   * @param valueProvider the value provider
   * @return an Entity instance initialized with values provided by the given value provider
   */
  public static Entity createEntity(final Entities entities, final EntityType entityType, final Function<Property<?>, Object> valueProvider) {
    requireNonNull(entities);
    requireNonNull(entityType);
    Entity entity = entities.entity(entityType);
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
  public static void randomize(final Entities entities, final Entity entity, final Map<EntityType, Entity> foreignKeyEntities) {
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
   * @param <T> the property type
   * @return a random value
   */
  public static <T> T createRandomValue(final Property<T> property, final Map<EntityType, Entity> referenceEntities) {
    requireNonNull(property, "property");
    if (property instanceof ForeignKeyProperty) {
      return (T) getReferenceEntity((ForeignKeyProperty) property, referenceEntities);
    }
    if (property instanceof ItemProperty) {
      return getRandomItem((ItemProperty<T>) property);
    }
    Attribute<?> attribute = property.getAttribute();
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
      return (T) Double.valueOf(getRandomDouble(property));
    }
    if (attribute.isBigDecimal()) {
      return (T) BigDecimal.valueOf(getRandomDouble(property));
    }
    if (attribute.isInteger()) {
      return (T) Integer.valueOf(getRandomInteger(property));
    }
    if (attribute.isLong()) {
      return (T) Long.valueOf(getRandomInteger(property));
    }
    if (attribute.isString()) {
      return (T) getRandomString(property);
    }
    if (attribute.isByteArray()) {
      return (T) getRandomBlob(property);
    }

    return null;
  }

  private static void populateEntity(final Entities entities, final Entity entity, final Collection<ColumnProperty<?>> properties,
                                     final Function<Property<?>, Object> valueProvider) {
    requireNonNull(valueProvider, "valueProvider");
    EntityDefinition definition = entities.getDefinition(entity.getEntityType());
    for (@SuppressWarnings("rawtypes") final ColumnProperty property : properties) {
      if (!definition.isForeignKeyAttribute(property.getAttribute()) && !property.isDenormalized()) {
        entity.put(property.getAttribute(), valueProvider.apply(property));
      }
    }
    for (final ForeignKeyProperty property : entities.getDefinition(entity.getEntityType()).getForeignKeyProperties()) {
      Entity value = (Entity) valueProvider.apply(property);
      if (value != null) {
        entity.put(property.getAttribute(), value);
      }
    }
  }

  private static String getRandomString(final Property<?> property) {
    int length = property.getMaximumLength() < 0 ? MAXIMUM_RANDOM_STRING_LENGTH : property.getMaximumLength();

    return Text.randomString(length, length);
  }

  private static byte[] getRandomBlob(final Property<?> property) {
    if ((property instanceof BlobProperty) && ((BlobProperty) property).isEagerlyLoaded()) {
      return getRandomBlob(1024);
    }

    return null;
  }

  private static byte[] getRandomBlob(final int numberOfBytes) {
    byte[] bytes = new byte[numberOfBytes];
    RANDOM.nextBytes(bytes);

    return bytes;
  }

  private static Object getReferenceEntity(final ForeignKeyProperty property, final Map<EntityType, Entity> referenceEntities) {
    return referenceEntities == null ? null : referenceEntities.get(property.getReferencedEntityType());
  }

  private static <T> T getRandomItem(final ItemProperty<T> property) {
    List<Item<T>> items = property.getItems();
    Item<T> item = items.get(RANDOM.nextInt(items.size()));

    return item.getValue();
  }

  private static int getRandomInteger(final Property<?> property) {
    int min = (int) (property.getMinimumValue() == null ? MININUM_RANDOM_NUMBER : property.getMinimumValue());
    int max = (int) (property.getMaximumValue() == null ? MAXIMUM_RANDOM_NUMBER : property.getMaximumValue());

    return RANDOM.nextInt((max - min) + 1) + min;
  }

  private static double getRandomDouble(final Property<?> property) {
    double min = property.getMinimumValue() == null ? MININUM_RANDOM_NUMBER : property.getMinimumValue();
    double max = property.getMaximumValue() == null ? MAXIMUM_RANDOM_NUMBER : property.getMaximumValue();

    return RANDOM.nextDouble() * (max - min) + min;
  }
}

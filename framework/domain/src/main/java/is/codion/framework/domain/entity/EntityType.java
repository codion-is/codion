/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Defines a Entity type and serves as a Factory for {@link Attribute} instances associated with this entity type.
 * @param <T> the entity type
 */
public interface EntityType<T extends Entity> extends Serializable {

  /**
   * @return the name of the domain this entity type is associated with
   */
  String getDomainName();

  /**
   * @return the entity type name, unique within a domain.
   */
  String getName();

  /**
   * @return the entity type class
   */
  Class<T> getEntityClass();

  /**
   * Creates a new {@link Attribute}, associated with this EntityType.
   * @param name the attribute name
   * @param typeClass the class representing the attribute value type
   * @param <T> the attribute type
   * @return a new {@link Attribute}
   */
  <T> Attribute<T> attribute(String name, Class<T> typeClass);

  /**
   * Creates a new {@link Attribute} associated with this EntityType.
   * Use this when you don't have access to an actual Attribute instance, only its name
   * and EntityType, but need to access the value associated with it.
   * @param name the attribute name
   * @return a new {@link Attribute}
   */
  Attribute<Object> objectAttribute(String name);

  /**
   * Creates a new Long based attribute, associated with this EntityType.
   * @param name the attribute name.
   * @return a new Long based attribute.
   */
  Attribute<Long> longAttribute(String name);

  /**
   * Creates a new Integer based attribute, associated with this EntityType.
   * @param name the attribute name.
   * @return a new Integer based attribute.
   */
  Attribute<Integer> integerAttribute(String name);

  /**
   * Creates a new Double based attribute, associated with this EntityType.
   * @param name the attribute name.
   * @return a new Double based attribute.
   */
  Attribute<Double> doubleAttribute(String name);

  /**
   * Creates a new BigDecimal based attribute, associated with this EntityType.
   * @param name the attribute name.
   * @return a new BigDecimal based attribute.
   */
  Attribute<BigDecimal> bigDecimalAttribute(String name);

  /**
   * Creates a new LocalDate based attribute, associated with this EntityType.
   * @param name the attribute name.
   * @return a new LocalDate based attribute.
   */
  Attribute<LocalDate> localDateAttribute(String name);

  /**
   * Creates a new LocalTime based attribute, associated with this EntityType.
   * @param name the attribute name.
   * @return a new LocalTime based attribute.
   */
  Attribute<LocalTime> localTimeAttribute(String name);

  /**
   * Creates a new LocalDateTime based attribute, associated with this EntityType.
   * @param name the attribute name.
   * @return a new LocalDateTime based attribute.
   */
  Attribute<LocalDateTime> localDateTimeAttribute(String name);

  /**
   * Creates a new String based attribute, associated with this EntityType.
   * @param name the attribute name.
   * @return a new String based attribute.
   */
  Attribute<String> stringAttribute(String name);

  /**
   * Creates a new Boolean based attribute, associated with this EntityType.
   * @param name the attribute name.
   * @return a new Boolean based attribute.
   */
  Attribute<Boolean> booleanAttribute(String name);

  /**
   * Creates a new {@link Attribute}, associated with this EntityType.
   * @param name the attribute name
   * @return a new {@link Attribute}
   */
  Attribute<Entity> entityAttribute(String name);

  /**
   * Creates a new {@link Attribute}, associated with this EntityType.
   * @param name the attribute name
   * @return a new {@link Attribute}
   */
  Attribute<byte[]> blobAttribute(String name);

  /**
   * @param name the entity type name
   * @param domainName the name of the domain to associate this entity type with
   * @return a {@link EntityType} instance with the given name
   */
  static EntityType<Entity> entityType(final String name, final String domainName) {
    return new DefaultEntityType<>(domainName, name);
  }

  /**
   * @param name the entity type name
   * @param domainName the name of the domain to associate this entity type with
   * @param entityClass the entity representation class
   * @param <T> the entity representation type
   * @return a {@link EntityType} instance with the given name
   */
  static <T extends Entity> EntityType<T> entityType(final String name, final String domainName, final Class<T> entityClass) {
    return new DefaultEntityType<>(domainName, name, entityClass);
  }
}

/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Identity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * A factory for {@link Attribute} instances.
 */
public final class Attributes {

  private Attributes() {}

  /**
   * Creates a new {@link Attribute}.
   * @param name the attribute name
   * @param typeClass the class representing the attribute value type
   * @param entityId the id of the entity to associate this attribute with
   * @param <T> the attribute type
   * @return a new {@link Attribute}
   */
  public static <T> Attribute<T> attribute(final String name, final Class<T> typeClass, final Identity entityId) {
    return new DefaultAttribute<>(name, typeClass, entityId);
  }

  /**
   * Creates a new {@link Attribute} associated with the given entityId.
   * Use this when you don't have access to an actual Attribute instance, only its name
   * and entityId, but need to access the value associated with it.
   * @param name the attribute name
   * @param entityId the id of the entity to associate this attribute with
   * @return a new {@link Attribute}
   */
  public static Attribute<Object> attribute(final String name, final Identity entityId) {
    final DefaultAttribute<Object> attribute = new DefaultAttribute<>(name, Object.class, entityId);

    return attribute;
  }

  /**
   * Creates a new Long based attribute.
   * @param name the attribute name.
   * @param entityId the id of the entity to associate this attribute with
   * @return a new Long based attribute.
   */
  public static Attribute<Long> longAttribute(final String name, final Identity entityId) {
    return attribute(name, Long.class, entityId);
  }

  /**
   * Creates a new Integer based attribute.
   * @param name the attribute name.
   * @param entityId the id of the entity to associate this attribute with
   * @return a new Integer based attribute.
   */
  public static Attribute<Integer> integerAttribute(final String name, final Identity entityId) {
    return attribute(name, Integer.class, entityId);
  }

  /**
   * Creates a new Double based attribute.
   * @param name the attribute name.
   * @param entityId the id of the entity to associate this attribute with
   * @return a new Double based attribute.
   */
  public static Attribute<Double> doubleAttribute(final String name, final Identity entityId) {
    return attribute(name, Double.class, entityId);
  }

  /**
   * Creates a new BigDecimal based attribute.
   * @param name the attribute name.
   * @param entityId the id of the entity to associate this attribute with
   * @return a new BigDecimal based attribute.
   */
  public static Attribute<BigDecimal> bigDecimalAttribute(final String name, final Identity entityId) {
    return attribute(name, BigDecimal.class, entityId);
  }

  /**
   * Creates a new LocalDate based attribute.
   * @param name the attribute name.
   * @param entityId the id of the entity to associate this attribute with
   * @return a new LocalDate based attribute.
   */
  public static Attribute<LocalDate> localDateAttribute(final String name, final Identity entityId) {
    return attribute(name, LocalDate.class, entityId);
  }

  /**
   * Creates a new LocalTime based attribute.
   * @param name the attribute name.
   * @param entityId the id of the entity to associate this attribute with
   * @return a new LocalTime based attribute.
   */
  public static Attribute<LocalTime> localTimeAttribute(final String name, final Identity entityId) {
    return attribute(name, LocalTime.class, entityId);
  }

  /**
   * Creates a new LocalDateTime based attribute.
   * @param name the attribute name.
   * @param entityId the id of the entity to associate this attribute with
   * @return a new LocalDateTime based attribute.
   */
  public static Attribute<LocalDateTime> localDateTimeAttribute(final String name, final Identity entityId) {
    return attribute(name, LocalDateTime.class, entityId);
  }

  /**
   * Creates a new String based attribute.
   * @param name the attribute name.
   * @param entityId the id of the entity to associate this attribute with
   * @return a new String based attribute.
   */
  public static Attribute<String> stringAttribute(final String name, final Identity entityId) {
    return attribute(name, String.class, entityId);
  }

  /**
   * Creates a new Boolean based attribute.
   * @param name the attribute name.
   * @param entityId the id of the entity to associate this attribute with
   * @return a new Boolean based attribute.
   */
  public static Attribute<Boolean> booleanAttribute(final String name, final Identity entityId) {
    return attribute(name, Boolean.class, entityId);
  }

  /**
   * Creates a new {@link EntityAttribute}.
   * @param name the attribute name
   * @param entityId the id of the entity to associate this attribute with
   * @return a new {@link EntityAttribute}
   */
  public static EntityAttribute entityAttribute(final String name, final Identity entityId) {
    return new DefaultEntityAttribute(name, entityId);
  }

  /**
   * Creates a new {@link BlobAttribute}.
   * @param name the attribute name
   * @param entityId the id of the entity to associate this attribute with
   * @return a new {@link BlobAttribute}
   */
  public static BlobAttribute blobAttribute(final String name, final Identity entityId) {
    return new DefaultBlobAttribute(name, entityId);
  }
}

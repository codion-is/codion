/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * An identifier for entity types.
 * Factory for {@link Attributes} associated with this identity.
 */
public interface Identity extends Serializable {

  /**
   * @return the identity name, unique within a domain.
   */
  String getName();

  /**
   * Creates a new {@link Attribute}, associated with this Identity.
   * @param name the attribute name
   * @param typeClass the class representing the attribute value type
   * @param <T> the attribute type
   * @return a new {@link Attribute}
   */
  <T> Attribute<T> attribute(String name, Class<T> typeClass);


  /**
   * Creates a new Long based attribute, associated with this Identity.
   * @param name the attribute name.
   * @return a new Long based attribute.
   */
  Attribute<Long> longAttribute(String name);

  /**
   * Creates a new Integer based attribute, associated with this Identity.
   * @param name the attribute name.
   * @return a new Integer based attribute.
   */
  Attribute<Integer> integerAttribute(String name);

  /**
   * Creates a new Double based attribute, associated with this Identity.
   * @param name the attribute name.
   * @return a new Double based attribute.
   */
  Attribute<Double> doubleAttribute(String name);

  /**
   * Creates a new BigDecimal based attribute, associated with this Identity.
   * @param name the attribute name.
   * @return a new BigDecimal based attribute.
   */
  Attribute<BigDecimal> bigDecimalAttribute(String name);

  /**
   * Creates a new LocalDate based attribute, associated with this Identity.
   * @param name the attribute name.
   * @return a new LocalDate based attribute.
   */
  Attribute<LocalDate> localDateAttribute(String name);

  /**
   * Creates a new LocalTime based attribute, associated with this Identity.
   * @param name the attribute name.
   * @return a new LocalTime based attribute.
   */
  Attribute<LocalTime> localTimeAttribute(String name);

  /**
   * Creates a new LocalDateTime based attribute, associated with this Identity.
   * @param name the attribute name.
   * @return a new LocalDateTime based attribute.
   */
  Attribute<LocalDateTime> localDateTimeAttribute(String name);

  /**
   * Creates a new String based attribute, associated with this Identity.
   * @param name the attribute name.
   * @return a new String based attribute.
   */
  Attribute<String> stringAttribute(String name);

  /**
   * Creates a new Boolean based attribute, associated with this Identity.
   * @param name the attribute name.
   * @return a new Boolean based attribute.
   */
  Attribute<Boolean> booleanAttribute(String name);

  /**
   * Creates a new {@link EntityAttribute}, associated with this Identity.
   * @param name the attribute name
   * @return a new {@link EntityAttribute}
   */
  EntityAttribute entityAttribute(String name);

  /**
   * Creates a new {@link BlobAttribute}, associated with this Identity.
   * @param name the attribute name
   * @return a new {@link BlobAttribute}
   */
  BlobAttribute blobAttribute(String name);

  /**
   * @param name the identity name
   * @return a Identity instance with the given name
   */
  static Identity identity(final String name) {
    return new DefaultIdentity(name);
  }
}

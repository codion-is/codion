/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.Attributes;
import is.codion.framework.domain.property.BlobAttribute;
import is.codion.framework.domain.property.EntityAttribute;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static is.codion.common.Util.nullOrEmpty;

final class DefaultIdentity implements Identity {

  private static final long serialVersionUID = 1;

  private final String name;

  DefaultIdentity(final String name) {
    if (nullOrEmpty(name)) {
      throw new IllegalArgumentException("name must be a non-empty string");
    }
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public <T> Attribute<T> attribute(final String name, final Class<T> typeClass) {
    return Attributes.attribute(name, typeClass, this);
  }

  @Override
  public Attribute<Long> longAttribute(final String name) {
    return Attributes.attribute(name, Long.class, this);
  }

  @Override
  public Attribute<Integer> integerAttribute(final String name) {
    return Attributes.attribute(name, Integer.class, this);
  }

  @Override
  public Attribute<Double> doubleAttribute(final String name) {
    return Attributes.attribute(name, Double.class, this);
  }

  @Override
  public Attribute<BigDecimal> bigDecimalAttribute(final String name) {
    return Attributes.attribute(name, BigDecimal.class, this);
  }

  @Override
  public Attribute<LocalDate> localDateAttribute(final String name) {
    return Attributes.attribute(name, LocalDate.class, this);
  }

  @Override
  public Attribute<LocalTime> localTimeAttribute(final String name) {
    return Attributes.attribute(name, LocalTime.class, this);
  }

  @Override
  public Attribute<LocalDateTime> localDateTimeAttribute(final String name) {
    return Attributes.attribute(name, LocalDateTime.class, this);
  }

  @Override
  public Attribute<String> stringAttribute(final String name) {
    return Attributes.attribute(name, String.class, this);
  }

  @Override
  public Attribute<Boolean> booleanAttribute(final String name) {
    return Attributes.attribute(name, Boolean.class, this);
  }

  @Override
  public EntityAttribute entityAttribute(final String name) {
    return Attributes.entityAttribute(name, this);
  }

  @Override
  public BlobAttribute blobAttribute(final String name) {
    return Attributes.blobAttribute(name, this);
  }

  @Override
  public boolean equals(final Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }
    final DefaultIdentity that = (DefaultIdentity) object;

    return name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public String toString() {
    return name;
  }
}

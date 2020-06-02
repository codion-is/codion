/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.attribute.Attribute;
import is.codion.framework.domain.attribute.Attributes;
import is.codion.framework.domain.identity.Identities;
import is.codion.framework.domain.identity.Identity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

final class DefaultEntityId implements EntityId {

  private static final long serialVersionUID = 1;

  private final Identity identity;

  DefaultEntityId(final String name) {
    this.identity = Identities.identity(name);
  }

  @Override
  public String getName() {
    return identity.getName();
  }

  @Override
  public <T> Attribute<T> attribute(final String name, final Class<T> typeClass) {
    return Attributes.attribute(name, typeClass, this);
  }

  @Override
  public Attribute<Object> objectAttribute(final String name) {
    return attribute(name, Object.class);
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
  public Attribute<Entity> entityAttribute(final String name) {
    return Attributes.entityAttribute(name, this);
  }

  @Override
  public Attribute<byte[]> blobAttribute(final String name) {
    return Attributes.blobAttribute(name, this);
  }

  @Override
  public String toString() {
    return identity.toString();
  }

  @Override
  public int hashCode() {
    return identity.hashCode();
  }

  @Override
  public boolean equals(final Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }
    final DefaultEntityId that = (DefaultEntityId) object;

    return getName().equals(that.getName());
  }
}

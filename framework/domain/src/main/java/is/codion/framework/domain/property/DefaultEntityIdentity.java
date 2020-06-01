/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.EntityIdentity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

final class DefaultEntityIdentity extends DefaultIdentity implements EntityIdentity {

  private static final long serialVersionUID = 1;

  DefaultEntityIdentity(final String name) {
    super(name);
  }

  @Override
  public <T> Attribute<T> attribute(final String name, final Class<T> typeClass) {
    return new DefaultAttribute<>(name, typeClass, this);
  }

  @Override
  public Attribute<Object> objectAttribute(final String name) {
    return attribute(name, Object.class);
  }

  @Override
  public Attribute<Long> longAttribute(final String name) {
    return new DefaultAttribute<>(name, Long.class, this);
  }

  @Override
  public Attribute<Integer> integerAttribute(final String name) {
    return new DefaultAttribute<>(name, Integer.class, this);
  }

  @Override
  public Attribute<Double> doubleAttribute(final String name) {
    return new DefaultAttribute<>(name, Double.class, this);
  }

  @Override
  public Attribute<BigDecimal> bigDecimalAttribute(final String name) {
    return new DefaultAttribute<>(name, BigDecimal.class, this);
  }

  @Override
  public Attribute<LocalDate> localDateAttribute(final String name) {
    return new DefaultAttribute<>(name, LocalDate.class, this);
  }

  @Override
  public Attribute<LocalTime> localTimeAttribute(final String name) {
    return new DefaultAttribute<>(name, LocalTime.class, this);
  }

  @Override
  public Attribute<LocalDateTime> localDateTimeAttribute(final String name) {
    return new DefaultAttribute<>(name, LocalDateTime.class, this);
  }

  @Override
  public Attribute<String> stringAttribute(final String name) {
    return new DefaultAttribute<>(name, String.class, this);
  }

  @Override
  public Attribute<Boolean> booleanAttribute(final String name) {
    return new DefaultAttribute<>(name, Boolean.class, this);
  }

  @Override
  public EntityAttribute entityAttribute(final String name) {
    return new DefaultEntityAttribute(name, this);
  }

  @Override
  public BlobAttribute blobAttribute(final String name) {
    return new DefaultBlobAttribute(name, this);
  }

  @Override
  public boolean equals(final Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }
    final DefaultEntityIdentity that = (DefaultEntityIdentity) object;

    return getName().equals(that.getName());
  }
}

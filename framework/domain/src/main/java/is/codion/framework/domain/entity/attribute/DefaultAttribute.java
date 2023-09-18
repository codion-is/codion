/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.DefaultDerivedAttributeDefinition.DefaultDerivedAttributeDefinitionBuilder;
import is.codion.framework.domain.entity.attribute.DefaultTransientAttributeDefinition.DefaultTransientAttributeDefinitionBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.Objects;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static java.util.Objects.requireNonNull;

class DefaultAttribute<T> implements Attribute<T>, Serializable {

  private static final long serialVersionUID = 1;

  private final String name;
  private final DefaultType<T> type;
  private final int hashCode;

  DefaultAttribute(String name, Class<T> valueClass, EntityType entityType) {
    if (nullOrEmpty(name)) {
      throw new IllegalArgumentException("name must be a non-empty string");
    }
    requireNonNull(entityType, "entityType");
    requireNonNull(valueClass, "valueClass");
    this.name = name;
    this.type = new DefaultType<>(entityType, valueClass);
    this.hashCode = Objects.hash(name, entityType);
  }

  @Override
  public AttributeDefiner<T> define() {
    return new DefaultAttributeDefiner<>(this);
  }

  @Override
  public final Type<T> type() {
    return type;
  }

  @Override
  public final String name() {
    return name;
  }

  @Override
  public final EntityType entityType() {
    return type.entityType;
  }

  @Override
  public final boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof DefaultAttribute)) {
      return false;
    }
    DefaultAttribute<?> that = (DefaultAttribute<?>) object;

    return hashCode == that.hashCode && name.equals(that.name) && type.entityType.equals(that.type.entityType);
  }

  @Override
  public final int hashCode() {
    return hashCode;
  }

  @Override
  public final String toString() {
    return type.entityType.name() + "." + name;
  }

  private static final class DefaultType<T> implements Type<T>, Serializable {

    private static final long serialVersionUID = 1;

    private final EntityType entityType;
    private final Class<T> valueClass;

    private DefaultType(EntityType entityType, Class<T> valueClass) {
      this.entityType = entityType;
      this.valueClass = valueClass;
    }

    @Override
    public Class<T> valueClass() {
      return valueClass;
    }

    @Override
    public T validateType(T value) {
      if (value != null && valueClass != value.getClass() && !valueClass.isAssignableFrom(value.getClass())) {
        throw new IllegalArgumentException("Value of type " + valueClass +
                " expected for attribute " + this + " in entity " + entityType + ", got: " + value.getClass());
      }

      return value;
    }

    @Override
    public boolean isNumerical() {
      return Number.class.isAssignableFrom(valueClass);
    }

    @Override
    public boolean isTemporal() {
      return Temporal.class.isAssignableFrom(valueClass);
    }

    @Override
    public boolean isLocalDate() {
      return isType(LocalDate.class);
    }

    @Override
    public boolean isLocalDateTime() {
      return isType(LocalDateTime.class);
    }

    @Override
    public boolean isLocalTime() {
      return isType(LocalTime.class);
    }

    @Override
    public boolean isOffsetDateTime() {
      return isType(OffsetDateTime.class);
    }

    @Override
    public boolean isCharacter() {
      return isType(Character.class);
    }

    @Override
    public boolean isString() {
      return isType(String.class);
    }

    @Override
    public boolean isLong() {
      return isType(Long.class);
    }

    @Override
    public boolean isInteger() {
      return isType(Integer.class);
    }

    @Override
    public boolean isShort() {
      return isType(Short.class);
    }

    @Override
    public boolean isDouble() {
      return isType(Double.class);
    }

    @Override
    public boolean isBigDecimal() {
      return isType(BigDecimal.class);
    }

    @Override
    public boolean isDecimal() {
      return isDouble() || isBigDecimal();
    }

    @Override
    public boolean isBoolean() {
      return isType(Boolean.class);
    }

    @Override
    public boolean isByteArray() {
      return isType(byte[].class);
    }

    @Override
    public boolean isEnum() {
      return valueClass.isEnum();
    }

    @Override
    public boolean isEntity() {
      return isType(Entity.class);
    }

    private boolean isType(Class<?> valueClass) {
      return this.valueClass.equals(valueClass);
    }
  }

  protected static class DefaultAttributeDefiner<T> implements AttributeDefiner<T> {

    private final Attribute<T> attribute;

    protected DefaultAttributeDefiner(Attribute<T> attribute) {
      this.attribute = attribute;
    }

    @Override
    public final <B extends TransientAttributeDefinition.Builder<T, B>> TransientAttributeDefinition.Builder<T, B> attribute() {
      return new DefaultTransientAttributeDefinitionBuilder<>(attribute);
    }

    @Override
    public final <B extends AttributeDefinition.Builder<T, B>> AttributeDefinition.Builder<T, B> denormalized(Attribute<Entity> entityAttribute,
                                                                                                              Attribute<T> denormalizedAttribute) {
      return new DefaultDerivedAttributeDefinitionBuilder<>(attribute,
              new DenormalizedValueProvider<>(entityAttribute, denormalizedAttribute), entityAttribute);
    }

    @Override
    public final <B extends AttributeDefinition.Builder<T, B>> AttributeDefinition.Builder<T, B> derived(DerivedAttribute.Provider<T> valueProvider,
                                                                                                         Attribute<?>... sourceAttributes) {
      return new DefaultDerivedAttributeDefinitionBuilder<>(attribute, valueProvider, sourceAttributes);
    }
  }
}

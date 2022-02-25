/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import static is.codion.common.Util.nullOrEmpty;
import static is.codion.framework.domain.entity.ForeignKey.reference;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

final class DefaultEntityType implements EntityType, Serializable {

  private static final long serialVersionUID = 1;

  private final String domainName;
  private final String name;
  private final Class<? extends Entity> entityClass;
  private final String resourceBundleName;
  private final int hashCode;

  DefaultEntityType(final String domainName, final String name, final Class<? extends Entity> entityClass,
                    final String resourceBundleName) {
    if (nullOrEmpty(name)) {
      throw new IllegalArgumentException("name must be a non-empty string");
    }
    if (nullOrEmpty(domainName)) {
      throw new IllegalArgumentException("domainName must be a non-empty string");
    }
    this.domainName = domainName;
    this.name = name;
    this.entityClass = entityClass;
    if (resourceBundleName != null) {
      ResourceBundle.getBundle(resourceBundleName);
    }
    this.resourceBundleName = resourceBundleName;
    this.hashCode = Objects.hash(name, domainName);
  }

  @Override
  public String getDomainName() {
    return domainName;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public <T extends Entity> Class<T> getEntityClass() {
    return (Class<T>) (entityClass == null ? Entity.class : entityClass);
  }

  @Override
  public String getResourceBundleName() {
    return resourceBundleName;
  }

  @Override
  public <T> Attribute<T> attribute(final String name, final Class<T> typeClass) {
    return Attribute.attribute(this, name, typeClass);
  }

  @Override
  public Attribute<Long> longAttribute(final String name) {
    return attribute(name, Long.class);
  }

  @Override
  public Attribute<Integer> integerAttribute(final String name) {
    return attribute(name, Integer.class);
  }

  @Override
  public Attribute<Double> doubleAttribute(final String name) {
    return attribute(name, Double.class);
  }

  @Override
  public Attribute<BigDecimal> bigDecimalAttribute(final String name) {
    return attribute(name, BigDecimal.class);
  }

  @Override
  public Attribute<LocalDate> localDateAttribute(final String name) {
    return attribute(name, LocalDate.class);
  }

  @Override
  public Attribute<LocalTime> localTimeAttribute(final String name) {
    return attribute(name, LocalTime.class);
  }

  @Override
  public Attribute<LocalDateTime> localDateTimeAttribute(final String name) {
    return attribute(name, LocalDateTime.class);
  }

  @Override
  public Attribute<OffsetDateTime> offsetDateTimeAttribute(final String name) {
    return attribute(name, OffsetDateTime.class);
  }

  @Override
  public Attribute<String> stringAttribute(final String name) {
    return attribute(name, String.class);
  }

  @Override
  public Attribute<Character> characterAttribute(final String name) {
    return attribute(name, Character.class);
  }

  @Override
  public Attribute<Boolean> booleanAttribute(final String name) {
    return attribute(name, Boolean.class);
  }

  @Override
  public Attribute<Entity> entityAttribute(final String name) {
    return attribute(name, Entity.class);
  }

  @Override
  public Attribute<byte[]> byteArrayAttribute(final String name) {
    return attribute(name, byte[].class);
  }

  @Override
  public <A> ForeignKey foreignKey(final String name, final Attribute<A> attribute, final Attribute<A> referencedAttribute) {
    return foreignKey(name, singletonList(reference(attribute, referencedAttribute)));
  }

  @Override
  public <A, B> ForeignKey foreignKey(final String name,
                                      final Attribute<A> firstAttribute, final Attribute<A> firstReferencedAttribute,
                                      final Attribute<B> secondAttribute, final Attribute<B> secondReferencedAttribute) {
    return foreignKey(name, asList(
            reference(firstAttribute, firstReferencedAttribute),
            reference(secondAttribute, secondReferencedAttribute)));
  }

  @Override
  public <A, B, C> ForeignKey foreignKey(final String name,
                                         final Attribute<A> firstAttribute, final Attribute<A> firstReferencedAttribute,
                                         final Attribute<B> secondAttribute, final Attribute<B> secondReferencedAttribute,
                                         final Attribute<C> thirdAttribute, final Attribute<C> thirdReferencedAttribute) {
    return foreignKey(name, asList(
            reference(firstAttribute, firstReferencedAttribute),
            reference(secondAttribute, secondReferencedAttribute),
            reference(thirdAttribute, thirdReferencedAttribute)));
  }

  @Override
  public ForeignKey foreignKey(final String name, final List<ForeignKey.Reference<?>> references) {
    return ForeignKey.foreignKey(this, name, references);
  }

  @Override
  public ConditionType conditionType(final String name) {
    return ConditionType.conditionType(this, name);
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public boolean equals(final Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }
    DefaultEntityType that = (DefaultEntityType) object;

    return hashCode == that.hashCode && name.equals(that.name) && domainName.equals(that.domainName);
  }
}

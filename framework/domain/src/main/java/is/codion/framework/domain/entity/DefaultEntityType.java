/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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

import static is.codion.common.NullOrEmpty.nullOrEmpty;
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

  DefaultEntityType(String domainName, String name, Class<? extends Entity> entityClass,
                    String resourceBundleName) {
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
  public String domainName() {
    return domainName;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public <T extends Entity> Class<T> entityClass() {
    return (Class<T>) (entityClass == null ? Entity.class : entityClass);
  }

  @Override
  public String resourceBundleName() {
    return resourceBundleName;
  }

  @Override
  public <T> Attribute<T> attribute(String name, Class<T> valueClass) {
    return Attribute.attribute(this, name, valueClass);
  }

  @Override
  public Attribute<Long> longAttribute(String name) {
    return attribute(name, Long.class);
  }

  @Override
  public Attribute<Integer> integerAttribute(String name) {
    return attribute(name, Integer.class);
  }

  @Override
  public Attribute<Short> shortAttribute(String name) {
    return attribute(name, Short.class);
  }

  @Override
  public Attribute<Double> doubleAttribute(String name) {
    return attribute(name, Double.class);
  }

  @Override
  public Attribute<BigDecimal> bigDecimalAttribute(String name) {
    return attribute(name, BigDecimal.class);
  }

  @Override
  public Attribute<LocalDate> localDateAttribute(String name) {
    return attribute(name, LocalDate.class);
  }

  @Override
  public Attribute<LocalTime> localTimeAttribute(String name) {
    return attribute(name, LocalTime.class);
  }

  @Override
  public Attribute<LocalDateTime> localDateTimeAttribute(String name) {
    return attribute(name, LocalDateTime.class);
  }

  @Override
  public Attribute<OffsetDateTime> offsetDateTimeAttribute(String name) {
    return attribute(name, OffsetDateTime.class);
  }

  @Override
  public Attribute<String> stringAttribute(String name) {
    return attribute(name, String.class);
  }

  @Override
  public Attribute<Character> characterAttribute(String name) {
    return attribute(name, Character.class);
  }

  @Override
  public Attribute<Boolean> booleanAttribute(String name) {
    return attribute(name, Boolean.class);
  }

  @Override
  public Attribute<Entity> entityAttribute(String name) {
    return attribute(name, Entity.class);
  }

  @Override
  public Attribute<byte[]> byteArrayAttribute(String name) {
    return attribute(name, byte[].class);
  }

  @Override
  public <A> ForeignKey foreignKey(String name, Attribute<A> attribute, Attribute<A> referencedAttribute) {
    return foreignKey(name, singletonList(reference(attribute, referencedAttribute)));
  }

  @Override
  public <A, B> ForeignKey foreignKey(String name,
                                      Attribute<A> firstAttribute, Attribute<A> firstReferencedAttribute,
                                      Attribute<B> secondAttribute, Attribute<B> secondReferencedAttribute) {
    return foreignKey(name, asList(
            reference(firstAttribute, firstReferencedAttribute),
            reference(secondAttribute, secondReferencedAttribute)));
  }

  @Override
  public <A, B, C> ForeignKey foreignKey(String name,
                                         Attribute<A> firstAttribute, Attribute<A> firstReferencedAttribute,
                                         Attribute<B> secondAttribute, Attribute<B> secondReferencedAttribute,
                                         Attribute<C> thirdAttribute, Attribute<C> thirdReferencedAttribute) {
    return foreignKey(name, asList(
            reference(firstAttribute, firstReferencedAttribute),
            reference(secondAttribute, secondReferencedAttribute),
            reference(thirdAttribute, thirdReferencedAttribute)));
  }

  @Override
  public ForeignKey foreignKey(String name, List<ForeignKey.Reference<?>> references) {
    return ForeignKey.foreignKey(this, name, references);
  }

  @Override
  public CriteriaType criteriaType(String name) {
    return CriteriaType.criteriaType(this, name);
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
  public boolean equals(Object object) {
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

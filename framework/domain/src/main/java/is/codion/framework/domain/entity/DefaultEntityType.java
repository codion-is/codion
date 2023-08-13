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
  public <T> Column<T> column(String name, Class<T> valueClass) {
    return Column.column(this, name, valueClass);
  }

  @Override
  public Column<Long> longColumn(String name) {
    return column(name, Long.class);
  }

  @Override
  public Column<Integer> integerColumn(String name) {
    return column(name, Integer.class);
  }

  @Override
  public Column<Short> shortColumn(String name) {
    return column(name, Short.class);
  }

  @Override
  public Column<Double> doubleColumn(String name) {
    return column(name, Double.class);
  }

  @Override
  public Column<BigDecimal> bigDecimalColumn(String name) {
    return column(name, BigDecimal.class);
  }

  @Override
  public Column<LocalDate> localDateColumn(String name) {
    return column(name, LocalDate.class);
  }

  @Override
  public Column<LocalTime> localTimeColumn(String name) {
    return column(name, LocalTime.class);
  }

  @Override
  public Column<LocalDateTime> localDateTimeColumn(String name) {
    return column(name, LocalDateTime.class);
  }

  @Override
  public Column<OffsetDateTime> offsetDateTimeColumn(String name) {
    return column(name, OffsetDateTime.class);
  }

  @Override
  public Column<String> stringColumn(String name) {
    return column(name, String.class);
  }

  @Override
  public Column<Character> characterColumn(String name) {
    return column(name, Character.class);
  }

  @Override
  public Column<Boolean> booleanColumn(String name) {
    return column(name, Boolean.class);
  }

  @Override
  public Column<byte[]> byteArrayColumn(String name) {
    return column(name, byte[].class);
  }

  @Override
  public <A> ForeignKey foreignKey(String name, Column<A> column, Column<A> referencedColumn) {
    return foreignKey(name, singletonList(reference(column, referencedColumn)));
  }

  @Override
  public <A, B> ForeignKey foreignKey(String name,
                                      Column<A> firstColumn, Column<A> firstReferencedColumn,
                                      Column<B> secondColumn, Column<B> secondReferencedColumn) {
    return foreignKey(name, asList(
            reference(firstColumn, firstReferencedColumn),
            reference(secondColumn, secondReferencedColumn)));
  }

  @Override
  public <A, B, C> ForeignKey foreignKey(String name,
                                         Column<A> firstColumn, Column<A> firstReferencedColumn,
                                         Column<B> secondColumn, Column<B> secondReferencedColumn,
                                         Column<C> thirdColumn, Column<C> thirdReferencedColumn) {
    return foreignKey(name, asList(
            reference(firstColumn, firstReferencedColumn),
            reference(secondColumn, secondReferencedColumn),
            reference(thirdColumn, thirdReferencedColumn)));
  }

  @Override
  public ForeignKey foreignKey(String name, List<ForeignKey.Reference<?>> references) {
    return ForeignKey.foreignKey(this, name, references);
  }

  @Override
  public ConditionType conditionType(String name) {
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

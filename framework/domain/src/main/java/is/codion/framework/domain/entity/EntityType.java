/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Defines an Entity type and serves as a Factory for {@link Attribute} instances associated with this entity type.
 * A factory for {@link EntityType} instances.
 */
public interface EntityType {

  /**
   * @return the domain type this entity type is associated with
   */
  DomainType domainType();

  /**
   * @return the entity type name, unique within a domain.
   */
  String name();

  /**
   * @param <T> the entity class type
   * @return the entity type class
   */
  <T extends Entity> Class<T> entityClass();

  /**
   * @return the name of the resource bundle, containing captions for this entity type, if any
   */
  String resourceBundleName();

  /**
   * Creates a {@link EntityDefinition.Builder} instance based on the given attribute definition builders.
   * @param definitionBuilders builders for the attribute definitions comprising the entity
   * @return a {@link EntityDefinition.Builder} instance
   * @throws IllegalArgumentException in case {@code definitionBuilders} is empty
   * @throws IllegalArgumentException in case of a entityType mismatch
   */
  EntityDefinition.Builder define(List<AttributeDefinition.Builder<?, ?>> definitionBuilders);

  /**
   * Creates a {@link EntityDefinition.Builder} instance based on the given attribute definition builders.
   * @param definitionBuilders builders for the attribute definitions comprising the entity
   * @return a {@link EntityDefinition.Builder} instance
   * @throws IllegalArgumentException in case {@code definitionBuilders} is empty
   * @throws IllegalArgumentException in case of a entityType mismatch
   */
  EntityDefinition.Builder define(AttributeDefinition.Builder<?, ?>... definitionBuilders);

  /**
   * Creates a new {@link Attribute}, associated with this EntityType.
   * @param name the attribute name
   * @param valueClass the class representing the attribute value type
   * @param <T> the attribute type
   * @return a new {@link Attribute}
   */
  <T> Attribute<T> attribute(String name, Class<T> valueClass);

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
   * Creates a new Short based attribute, associated with this EntityType.
   * @param name the attribute name.
   * @return a new Short based attribute.
   */
  Attribute<Short> shortAttribute(String name);

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
   * Creates a new OffsetDateTime based attribute, associated with this EntityType.
   * @param name the attribute name.
   * @return a new OffsetDateTime based attribute.
   */
  Attribute<OffsetDateTime> offsetDateTimeAttribute(String name);

  /**
   * Creates a new String based attribute, associated with this EntityType.
   * @param name the attribute name.
   * @return a new String based attribute.
   */
  Attribute<String> stringAttribute(String name);

  /**
   * Creates a new Character based attribute, associated with this EntityType.
   * @param name the attribute name.
   * @return a new Character based attribute.
   */
  Attribute<Character> characterAttribute(String name);

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
  Attribute<byte[]> byteArrayAttribute(String name);

  /**
   * Creates a new {@link Column}, associated with this EntityType.
   * @param name the column name
   * @param valueClass the class representing the column value type
   * @param <T> the column type
   * @return a new {@link Column}
   */
  <T> Column<T> column(String name, Class<T> valueClass);

  /**
   * Creates a new Long based column, associated with this EntityType.
   * @param name the column name.
   * @return a new Long based column.
   */
  Column<Long> longColumn(String name);

  /**
   * Creates a new Integer based column, associated with this EntityType.
   * @param name the column name.
   * @return a new Integer based column.
   */
  Column<Integer> integerColumn(String name);

  /**
   * Creates a new Short based column, associated with this EntityType.
   * @param name the column name.
   * @return a new Short based column.
   */
  Column<Short> shortColumn(String name);

  /**
   * Creates a new Double based column, associated with this EntityType.
   * @param name the column name.
   * @return a new Double based column.
   */
  Column<Double> doubleColumn(String name);

  /**
   * Creates a new BigDecimal based column, associated with this EntityType.
   * @param name the column name.
   * @return a new BigDecimal based column.
   */
  Column<BigDecimal> bigDecimalColumn(String name);

  /**
   * Creates a new LocalDate based column, associated with this EntityType.
   * @param name the column name.
   * @return a new LocalDate based column.
   */
  Column<LocalDate> localDateColumn(String name);

  /**
   * Creates a new LocalTime based column, associated with this EntityType.
   * @param name the column name.
   * @return a new LocalTime based column.
   */
  Column<LocalTime> localTimeColumn(String name);

  /**
   * Creates a new LocalDateTime based column, associated with this EntityType.
   * @param name the column name.
   * @return a new LocalDateTime based column.
   */
  Column<LocalDateTime> localDateTimeColumn(String name);

  /**
   * Creates a new OffsetDateTime based column, associated with this EntityType.
   * @param name the column name.
   * @return a new OffsetDateTime based column.
   */
  Column<OffsetDateTime> offsetDateTimeColumn(String name);

  /**
   * Creates a new String based column, associated with this EntityType.
   * @param name the column name.
   * @return a new String based column.
   */
  Column<String> stringColumn(String name);

  /**
   * Creates a new Character based column, associated with this EntityType.
   * @param name the column name.
   * @return a new Character based column.
   */
  Column<Character> characterColumn(String name);

  /**
   * Creates a new Boolean based column, associated with this EntityType.
   * @param name the column name.
   * @return a new Boolean based column.
   */
  Column<Boolean> booleanColumn(String name);

  /**
   * Creates a new {@link Column}, associated with this EntityType.
   * @param name the column name
   * @return a new {@link Column}
   */
  Column<byte[]> byteArrayColumn(String name);

  /**
   * Creates a new {@link ForeignKey} based on the given attributes.
   * @param name the attribute name
   * @param column the column
   * @param referencedColumn the referenced column
   * @param <A> the attribute type
   * @return a new {@link ForeignKey}
   */
  <A> ForeignKey foreignKey(String name, Column<A> column, Column<A> referencedColumn);

  /**
   * Creates a new {@link ForeignKey} based on the given columns.
   * @param name the column name
   * @param firstColumn the first column
   * @param firstReferencedColumn the first referenced column
   * @param secondColumn the second column
   * @param secondReferencedColumn the second referenced column
   * @param <A> the first column type
   * @param <B> the second column type
   * @return a new {@link ForeignKey}
   */
  <A, B> ForeignKey foreignKey(String name,
                               Column<A> firstColumn, Column<A> firstReferencedColumn,
                               Column<B> secondColumn, Column<B> secondReferencedColumn);

  /**
   * Creates a new {@link ForeignKey} based on the given columns.
   * @param name the column name
   * @param firstColumn the first column
   * @param firstReferencedColumn the first referenced column
   * @param secondColumn the second column
   * @param secondReferencedColumn the third referenced column
   * @param thirdColumn the second column
   * @param thirdReferencedColumn the third referenced column
   * @param <A> the first column type
   * @param <B> the second column type
   * @param <C> the third column type
   * @return a new {@link ForeignKey}
   */
  <A, B, C> ForeignKey foreignKey(String name,
                                  Column<A> firstColumn, Column<A> firstReferencedColumn,
                                  Column<B> secondColumn, Column<B> secondReferencedColumn,
                                  Column<C> thirdColumn, Column<C> thirdReferencedColumn);

  /**
   * Creates a new {@link ForeignKey} based on the given references.
   * @param name the attribute name
   * @param references the references
   * @return a new {@link ForeignKey}
   * @see ForeignKey#reference(Column, Column)
   */
  ForeignKey foreignKey(String name, List<ForeignKey.Reference<?>> references);

  /**
   * Instantiates a new {@link ConditionType} for this entity type
   * @param name the name
   * @return a new condition type
   */
  ConditionType conditionType(String name);

  /**
   * Creates a new EntityType instance.
   * @param name the entity type name
   * @param domainType the domainType to associate this entity type with
   * @param entityClass the entity representation class
   * @param <T> the entity representation type
   * @return a {@link EntityType} instance with the given name
   */
  static <T extends Entity> EntityType entityType(String name, DomainType domainType,
                                                  Class<T> entityClass) {
    String bundleName = null;
    try {
      ResourceBundle.getBundle(entityClass.getName());
      bundleName = entityClass.getName();
    }
    catch (MissingResourceException e) {/* Non-existing bundle */}

    return new DefaultEntityType(domainType, name, entityClass, bundleName);
  }

  /**
   * Creates a new EntityType instance.
   * @param name the entity type name
   * @param domainType the domainType to associate this entity type with
   * @param resourceBundleName the name of a resource bundle to use for captions, if any
   * @return a {@link EntityType} instance with the given name
   */
  static EntityType entityType(String name, DomainType domainType,
                               String resourceBundleName) {
    return new DefaultEntityType(domainType, name, Entity.class, resourceBundleName);
  }

  /**
   * Creates a new EntityType instance.
   * @param name the entity type name
   * @param domainType the domainType to associate this entity type with
   * @param entityClass the entity representation class
   * @param resourceBundleName the name of a resource bundle to use for captions, if any
   * @param <T> the entity representation type
   * @return a {@link EntityType} instance with the given name
   */
  static <T extends Entity> EntityType entityType(String name, DomainType domainType,
                                                  Class<T> entityClass, String resourceBundleName) {
    return new DefaultEntityType(domainType, name, entityClass, resourceBundleName);
  }
}

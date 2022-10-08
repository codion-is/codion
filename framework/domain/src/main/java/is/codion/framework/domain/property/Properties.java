/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.item.Item;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.DefaultAuditProperty.DefaultAuditPropertyBuilder;
import is.codion.framework.domain.property.DefaultBlobProperty.DefaultBlobPropertyBuilder;
import is.codion.framework.domain.property.DefaultColumnProperty.DefaultColumnPropertyBuilder;
import is.codion.framework.domain.property.DefaultColumnProperty.DefaultSubqueryPropertyBuilder;
import is.codion.framework.domain.property.DefaultDenormalizedProperty.DefaultDenormalizedPropertyBuilder;
import is.codion.framework.domain.property.DefaultDerivedProperty.DefaultDerivedPropertyBuilder;
import is.codion.framework.domain.property.DefaultForeignKeyProperty.DefaultForeignKeyPropertyBuilder;
import is.codion.framework.domain.property.DefaultItemProperty.DefaultItemPropertyBuilder;
import is.codion.framework.domain.property.DefaultTransientProperty.DefaultTransientPropertyBuilder;

import java.text.Collator;
import java.time.temporal.Temporal;
import java.util.List;

import static is.codion.framework.domain.property.AuditProperty.AuditAction.INSERT;
import static is.codion.framework.domain.property.AuditProperty.AuditAction.UPDATE;
import static java.util.Objects.requireNonNull;

/**
 * A Property factory class.
 */
public final class Properties {

  private Properties() {}

  /**
   * Creates a new {@link ColumnProperty.Builder} instance.
   * @param attribute the attribute
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <T, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> columnProperty(Attribute<T> attribute) {
    return columnProperty(attribute, null);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance.
   * @param attribute the attribute
   * @param caption the property caption
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <T, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> columnProperty(Attribute<T> attribute, String caption) {
    return new DefaultColumnPropertyBuilder<>(attribute, caption);
  }

  /**
   * A convenience method for creating a new {@link ColumnProperty.Builder} instance,
   * with the primary key index set to 0.
   * @param attribute the attribute
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder} with primary key index 0
   */
  public static <T, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> primaryKeyProperty(Attribute<T> attribute) {
    return primaryKeyProperty(attribute, null);
  }

  /**
   * A convenience method for creating a new {@link ColumnProperty.Builder} instance,
   * with the primary key index set to 0.
   * @param attribute the attribute
   * @param caption the property caption
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder} with primary key index 0
   */
  public static <T, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> primaryKeyProperty(Attribute<T> attribute, String caption) {
    return (ColumnProperty.Builder<T, B>) columnProperty(attribute, caption).primaryKeyIndex(0);
  }

  /**
   * Instantiates a {@link ForeignKeyProperty.Builder} instance.
   * @param foreignKey the foreign key
   * @return a new {@link ForeignKeyProperty.Builder}
   */
  public static ForeignKeyProperty.Builder foreignKeyProperty(ForeignKey foreignKey) {
    return foreignKeyProperty(foreignKey, null);
  }

  /**
   * Instantiates a {@link ForeignKeyProperty.Builder} instance.
   * @param foreignKey the foreign key
   * @param caption the caption
   * @return a new {@link ForeignKeyProperty.Builder}
   */
  public static ForeignKeyProperty.Builder foreignKeyProperty(ForeignKey foreignKey, String caption) {
    return new DefaultForeignKeyPropertyBuilder(foreignKey, caption);
  }

  /**
   * Instantiates a {@link TransientProperty.Builder} instance, for displaying a value from an entity attribute.
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @param attribute the attribute
   * @param entityAttribute the entity attribute from which this property gets its value
   * @param denormalizedAttribute the property from the referenced entity, from which this property gets its value
   * @return a new {@link TransientProperty.Builder}
   */
  public static <T, B extends TransientProperty.Builder<T, B>> TransientProperty.Builder<T, B> denormalizedViewProperty(Attribute<T> attribute,
                                                                                                                        Attribute<Entity> entityAttribute,
                                                                                                                        Attribute<T> denormalizedAttribute) {
    return denormalizedViewProperty(attribute, null, entityAttribute, denormalizedAttribute);
  }

  /**
   * Instantiates a {@link TransientProperty.Builder} instance, for displaying a value from an entity attribute.
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @param attribute the attribute
   * @param caption the caption of this property
   * @param entityAttribute the entity attribute from which this property gets its value
   * @param denormalizedAttribute the property from the referenced entity, from which this property gets its value
   * @return a new {@link TransientProperty.Builder}
   */
  public static <T, B extends TransientProperty.Builder<T, B>> TransientProperty.Builder<T, B> denormalizedViewProperty(Attribute<T> attribute, String caption,
                                                                                                                        Attribute<Entity> entityAttribute,
                                                                                                                        Attribute<T> denormalizedAttribute) {
    DerivedProperty.Provider<T> valueProvider = sourceValues -> {
      Entity foreignKeyValue = sourceValues.get(entityAttribute);

      return foreignKeyValue == null ? null : foreignKeyValue.get(denormalizedAttribute);
    };

    return new DefaultDerivedPropertyBuilder<>(attribute, caption, valueProvider, entityAttribute);
  }

  /**
   * Instantiates a {@link TransientProperty.Builder} instance, which value is derived from one or more source attributes.
   * @param attribute the attribute
   * @param valueProvider a {@link DerivedProperty.Provider} instance responsible for deriving the value
   * @param sourceAttributes the attributes from which this property derives its value
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link TransientProperty.Builder}
   * @throws IllegalArgumentException in case no source properties are specified
   */
  public static <T, B extends TransientProperty.Builder<T, B>> TransientProperty.Builder<T, B> derivedProperty(Attribute<T> attribute,
                                                                                                               DerivedProperty.Provider<T> valueProvider,
                                                                                                               Attribute<?>... sourceAttributes) {
    return derivedProperty(attribute, null, valueProvider, sourceAttributes);
  }

  /**
   * Instantiates a {@link TransientProperty.Builder} instance, which value is derived from one or more source attributes.
   * @param attribute the attribute
   * @param caption the caption
   * @param valueProvider a {@link DerivedProperty.Provider} instance responsible for deriving the value
   * @param sourceAttributes the ids of the properties from which this property derives its value
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link TransientProperty.Builder}
   * @throws IllegalArgumentException in case no source properties are specified
   */
  public static <T, B extends TransientProperty.Builder<T, B>> TransientProperty.Builder<T, B> derivedProperty(Attribute<T> attribute, String caption,
                                                                                                               DerivedProperty.Provider<T> valueProvider,
                                                                                                               Attribute<?>... sourceAttributes) {
    return new DefaultDerivedPropertyBuilder<>(attribute, caption, valueProvider, sourceAttributes);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, which value should mirror the value from an entity attribute.
   * @param attribute the attribute to base this property on
   * @param entityAttribute the entity attribute owning the attribute which value to mirror
   * @param denormalizedAttribute the attribute from which this attribute should get its value
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <T, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> denormalizedProperty(Attribute<T> attribute,
                                                                                                              Attribute<Entity> entityAttribute,
                                                                                                              Attribute<T> denormalizedAttribute) {
    return denormalizedProperty(attribute, null, entityAttribute, denormalizedAttribute);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, which value should mirror the value from an entity attribute.
   * @param attribute the attribute to base this property on
   * @param caption the property caption
   * @param entityAttribute the entity attribute owning the attribute which value to mirror
   * @param denormalizedAttribute the attribute from which this attribute should get its value
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <T, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> denormalizedProperty(Attribute<T> attribute, String caption,
                                                                                                              Attribute<Entity> entityAttribute,
                                                                                                              Attribute<T> denormalizedAttribute) {
    return new DefaultDenormalizedPropertyBuilder<>(attribute, caption, entityAttribute, denormalizedAttribute);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, based on a subquery.
   * @param attribute the attribute
   * @param subquery the sql query
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <T, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> subqueryProperty(Attribute<T> attribute, String subquery) {
    return subqueryProperty(attribute, null, subquery);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, based on a subquery.
   * @param attribute the attribute
   * @param caption the property caption
   * @param subquery the sql query
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <T, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> subqueryProperty(Attribute<T> attribute, String caption,
                                                                                                          String subquery) {
    return new DefaultSubqueryPropertyBuilder<>(attribute, caption, subquery);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, based on the given items.
   * @param attribute the attribute
   * @param validItems the Items representing all the valid values for this property
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   * @throws IllegalArgumentException in case the valid item list contains duplicate values
   */
  public static <T, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> itemProperty(Attribute<T> attribute, List<Item<T>> validItems) {
    return itemProperty(attribute, null, validItems);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, based on the given items.
   * @param attribute the attribute
   * @param caption the property caption
   * @param validItems the Items representing all the valid values for this property
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   * @throws IllegalArgumentException in case the valid item list contains duplicate values
   */
  public static <T, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> itemProperty(Attribute<T> attribute, String caption,
                                                                                                      List<Item<T>> validItems) {
    return new DefaultItemPropertyBuilder<>(attribute, caption, validItems);
  }

  /**
   * Creates a new {@link TransientProperty.Builder} instance, which does not map to an underlying table column.
   * @param attribute the attribute
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link TransientProperty.Builder}
   */
  public static <T, B extends TransientProperty.Builder<T, B>> TransientProperty.Builder<T, B> transientProperty(Attribute<T> attribute) {
    return transientProperty(attribute, null);
  }

  /**
   * Creates a new {@link TransientProperty.Builder} instance, which does not map to an underlying table column.
   * @param attribute the attribute
   * @param caption the property caption
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link TransientProperty.Builder}
   */
  public static <T, B extends TransientProperty.Builder<T, B>> TransientProperty.Builder<T, B> transientProperty(Attribute<T> attribute, String caption) {
    return new DefaultTransientPropertyBuilder<>(attribute, caption);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance representing a Boolean value.
   * @param <C> the column type
   * @param <B> the builder type
   * @param attribute the attribute
   * @param columnClass the underlying column data type class
   * @param trueValue the value representing 'true' in the underlying column
   * @param falseValue the value representing 'false' in the underlying column
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <C, B extends ColumnProperty.Builder<Boolean, B>> ColumnProperty.Builder<Boolean, B> booleanProperty(Attribute<Boolean> attribute, Class<C> columnClass,
                                                                                                                     C trueValue, C falseValue) {
    return booleanProperty(attribute, null, columnClass, trueValue, falseValue);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance representing a Boolean value.
   * @param <C> the column type
   * @param <B> the builder type
   * @param columnClass the underlying column data type class
   * @param attribute the attribute
   * @param caption the property caption
   * @param trueValue the value representing 'true' in the underlying column
   * @param falseValue the value representing 'false' in the underlying column
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <C, B extends ColumnProperty.Builder<Boolean, B>> ColumnProperty.Builder<Boolean, B> booleanProperty(Attribute<Boolean> attribute, String caption,
                                                                                                                     Class<C> columnClass, C trueValue, C falseValue) {
    return (ColumnProperty.Builder<Boolean, B>) new DefaultColumnPropertyBuilder<>(attribute, caption)
            .columnClass(columnClass, booleanValueConverter(trueValue, falseValue));
  }

  /**
   * Creates a new {@link BlobProperty.Builder} instance.
   * @param attribute the attribute
   * @return a new {@link BlobProperty.Builder}
   */
  public static BlobProperty.Builder blobProperty(Attribute<byte[]> attribute) {
    return blobProperty(attribute, null);
  }

  /**
   * Creates a new {@link BlobProperty.Builder} instance.
   * @param attribute the attribute
   * @param caption the property caption
   * @return a new {@link BlobProperty.Builder}
   */
  public static BlobProperty.Builder blobProperty(Attribute<byte[]> attribute, String caption) {
    return new DefaultBlobPropertyBuilder(attribute, caption);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the time a record was inserted.
   * @param attribute the attribute
   * @param <T> the Temporal type to base this property on
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <T extends Temporal, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> auditInsertTimeProperty(Attribute<T> attribute) {
    return auditInsertTimeProperty(attribute, null);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the time a record was inserted.
   * @param attribute the attribute
   * @param caption the property caption
   * @param <T> the Temporal type to base this property on
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <T extends Temporal, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> auditInsertTimeProperty(Attribute<T> attribute, String caption) {
    return new DefaultAuditPropertyBuilder<>(attribute, caption, INSERT);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the time a record was updated.
   * @param attribute the attribute
   * @param <T> the Temporal type to base this property on
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <T extends Temporal, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> auditUpdateTimeProperty(Attribute<T> attribute) {
    return auditUpdateTimeProperty(attribute, null);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the time a record was updated.
   * @param attribute the attribute
   * @param caption the property caption
   * @param <T> the Temporal type to base this property on
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <T extends Temporal, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> auditUpdateTimeProperty(Attribute<T> attribute, String caption) {
    return new DefaultAuditPropertyBuilder<>(attribute, caption, UPDATE);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the username of the user who inserted a record.
   * @param attribute the attribute
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <B extends ColumnProperty.Builder<String, B>> ColumnProperty.Builder<String, B> auditInsertUserProperty(Attribute<String> attribute) {
    return auditInsertUserProperty(attribute, null);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the username of the user who inserted a record.
   * @param attribute the attribute
   * @param <B> the builder type
   * @param caption the property caption
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <B extends ColumnProperty.Builder<String, B>> ColumnProperty.Builder<String, B> auditInsertUserProperty(Attribute<String> attribute, String caption) {
    return new DefaultAuditPropertyBuilder<>(attribute, caption, INSERT);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the username of the user who updated a record.
   * @param attribute the attribute
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <B extends ColumnProperty.Builder<String, B>> ColumnProperty.Builder<String, B> auditUpdateUserProperty(Attribute<String> attribute) {
    return auditUpdateUserProperty(attribute, null);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the username of the user who updated a record.
   * @param attribute the attribute
   * @param caption the property caption
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <B extends ColumnProperty.Builder<String, B>> ColumnProperty.Builder<String, B> auditUpdateUserProperty(Attribute<String> attribute, String caption) {
    return new DefaultAuditPropertyBuilder<>(attribute, caption, UPDATE);
  }

  /**
   * Creates a new {@link ColumnProperty.ValueConverter} instance for converting a column value
   * representing a boolean value to and from an actual Boolean.
   * @param trueValue the value used to represent 'true' in the underlying database, may not be null
   * @param falseValue the value used to represent 'false' in the underlying database, may not be null
   * @param <T> the type of the value used to represent a boolean
   * @return a value converter for converting an underlying database representation
   * of a boolean value into an actual Boolean
   */
  public static <T> ColumnProperty.ValueConverter<Boolean, T> booleanValueConverter(T trueValue, T falseValue) {
    return new DefaultColumnProperty.BooleanValueConverter<>(trueValue, falseValue);
  }

  /**
   * Sorts the given properties by caption, or if that is not available, attribute name, ignoring case
   * @param properties the properties to sort
   * @return the sorted list
   */
  public static List<Property<?>> sort(List<Property<?>> properties) {
    requireNonNull(properties, "properties");
    Collator collator = Collator.getInstance();
    properties.sort((o1, o2) -> collator.compare(o1.toString().toLowerCase(), o2.toString().toLowerCase()));

    return properties;
  }
}

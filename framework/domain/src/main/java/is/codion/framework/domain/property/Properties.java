/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.item.Item;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;

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
  public static <T, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> columnProperty(final Attribute<T> attribute) {
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
  public static <T, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> columnProperty(final Attribute<T> attribute, final String caption) {
    return new DefaultColumnProperty<>(attribute, caption).builder();
  }

  /**
   * A convenience method for creating a new {@link ColumnProperty.Builder} instance,
   * with the primary key index set to 0.
   * @param attribute the attribute
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder} with primary key index 0
   */
  public static <T, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> primaryKeyProperty(final Attribute<T> attribute) {
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
  public static <T, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> primaryKeyProperty(final Attribute<T> attribute, final String caption) {
    return (ColumnProperty.Builder<T, B>) columnProperty(attribute, caption).primaryKeyIndex(0);
  }

  /**
   * Instantiates a {@link ForeignKeyProperty.Builder} instance.
   * @param foreignKey the foreign key
   * @return a new {@link ForeignKeyProperty.Builder}
   */
  public static ForeignKeyProperty.Builder foreignKeyProperty(final ForeignKey foreignKey) {
    return foreignKeyProperty(foreignKey, null);
  }

  /**
   * Instantiates a {@link ForeignKeyProperty.Builder} instance.
   * @param foreignKey the foreign key
   * @param caption the caption
   * @return a new {@link ForeignKeyProperty.Builder}
   */
  public static ForeignKeyProperty.Builder foreignKeyProperty(final ForeignKey foreignKey, final String caption) {
    return new DefaultForeignKeyProperty(foreignKey, caption).builder();
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
  public static <T, B extends TransientProperty.Builder<T, B>> TransientProperty.Builder<T, B> denormalizedViewProperty(final Attribute<T> attribute,
                                                                                                                        final Attribute<Entity> entityAttribute,
                                                                                                                        final Attribute<T> denormalizedAttribute) {
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
  public static <T, B extends TransientProperty.Builder<T, B>> TransientProperty.Builder<T, B> denormalizedViewProperty(final Attribute<T> attribute, final String caption,
                                                                                                                        final Attribute<Entity> entityAttribute,
                                                                                                                        final Attribute<T> denormalizedAttribute) {
    final DerivedProperty.Provider<T> valueProvider = sourceValues -> {
      final Entity foreignKeyValue = sourceValues.get(entityAttribute);

      return foreignKeyValue == null ? null : foreignKeyValue.get(denormalizedAttribute);
    };

    return new DefaultDerivedProperty<>(attribute, caption, valueProvider, entityAttribute).builder();
  }

  /**
   * Instantiates a {@link TransientProperty.Builder} instance, which value is derived from one or more linked attributes.
   * @param attribute the attribute
   * @param valueProvider a {@link DerivedProperty.Provider} instance responsible for deriving the value
   * @param linkedAttributes the attributes from which this property derives its value
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link TransientProperty.Builder}
   * @throws IllegalArgumentException in case no linked property ids are provided
   */
  public static <T, B extends TransientProperty.Builder<T, B>> TransientProperty.Builder<T, B> derivedProperty(final Attribute<T> attribute,
                                                                                                               final DerivedProperty.Provider<T> valueProvider,
                                                                                                               final Attribute<?>... linkedAttributes) {
    return derivedProperty(attribute, null, valueProvider, linkedAttributes);
  }

  /**
   * Instantiates a {@link TransientProperty.Builder} instance, which value is derived from one or more linked attributes.
   * @param attribute the attribute
   * @param caption the caption
   * @param valueProvider a {@link DerivedProperty.Provider} instance responsible for deriving the value
   * @param linkedAttributes the ids of the properties from which this property derives its value
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link TransientProperty.Builder}
   * @throws IllegalArgumentException in case no linked property ids are provided
   */
  public static <T, B extends TransientProperty.Builder<T, B>> TransientProperty.Builder<T, B> derivedProperty(final Attribute<T> attribute, final String caption,
                                                                                                               final DerivedProperty.Provider<T> valueProvider,
                                                                                                               final Attribute<?>... linkedAttributes) {
    return new DefaultDerivedProperty<>(attribute, caption, valueProvider, linkedAttributes).builder();
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
  public static <T, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> denormalizedProperty(final Attribute<T> attribute,
                                                                                                              final Attribute<Entity> entityAttribute,
                                                                                                              final Attribute<T> denormalizedAttribute) {
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
  public static <T, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> denormalizedProperty(final Attribute<T> attribute, final String caption,
                                                                                                              final Attribute<Entity> entityAttribute,
                                                                                                              final Attribute<T> denormalizedAttribute) {
    return new DefaultDenormalizedProperty<>(attribute, entityAttribute, denormalizedAttribute, caption).builder();
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, based on a subquery.
   * @param attribute the attribute
   * @param subquery the sql query
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <T, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> subqueryProperty(final Attribute<T> attribute, final String subquery) {
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
  public static <T, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> subqueryProperty(final Attribute<T> attribute, final String caption,
                                                                                                          final String subquery) {
    return new DefaultSubqueryProperty<>(attribute, caption, subquery).builder();
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, based on the given items.
   * @param attribute the attribute
   * @param validItems the Items representing all the valid values for this property
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <T, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> itemProperty(final Attribute<T> attribute, final List<Item<T>> validItems) {
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
   */
  public static <T, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> itemProperty(final Attribute<T> attribute, final String caption,
                                                                                                      final List<Item<T>> validItems) {
    return new DefaultItemProperty<>(attribute, caption, validItems).builder();
  }

  /**
   * Creates a new {@link TransientProperty.Builder} instance, which does not map to an underlying table column.
   * @param attribute the attribute
   * @param <T> the attribute value type
   * @param <B> the builder type
   * @return a new {@link TransientProperty.Builder}
   */
  public static <T, B extends TransientProperty.Builder<T, B>> TransientProperty.Builder<T, B> transientProperty(final Attribute<T> attribute) {
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
  public static <T, B extends TransientProperty.Builder<T, B>> TransientProperty.Builder<T, B> transientProperty(final Attribute<T> attribute, final String caption) {
    return new DefaultTransientProperty<>(attribute, caption).builder();
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
  public static <C, B extends ColumnProperty.Builder<Boolean, B>> ColumnProperty.Builder<Boolean, B> booleanProperty(final Attribute<Boolean> attribute, final Class<C> columnClass,
                                                                                                                     final C trueValue, final C falseValue) {
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
  public static <C, B extends ColumnProperty.Builder<Boolean, B>> ColumnProperty.Builder<Boolean, B> booleanProperty(final Attribute<Boolean> attribute, final String caption,
                                                                                                                     final Class<C> columnClass, final C trueValue, final C falseValue) {
    return (ColumnProperty.Builder<Boolean, B>) new DefaultColumnProperty<>(attribute, caption).builder()
            .columnClass(columnClass, booleanValueConverter(trueValue, falseValue));
  }

  /**
   * Creates a new {@link BlobProperty.Builder} instance.
   * @param attribute the attribute
   * @return a new {@link BlobProperty.Builder}
   */
  public static BlobProperty.Builder blobProperty(final Attribute<byte[]> attribute) {
    return blobProperty(attribute, null);
  }

  /**
   * Creates a new {@link BlobProperty.Builder} instance.
   * @param attribute the attribute
   * @param caption the property caption
   * @return a new {@link BlobProperty.Builder}
   */
  public static BlobProperty.Builder blobProperty(final Attribute<byte[]> attribute, final String caption) {
    return new DefaultBlobProperty(attribute, caption).builder();
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the time a record was inserted.
   * @param attribute the attribute
   * @param <T> the Temporal type to base this property on
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <T extends Temporal, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> auditInsertTimeProperty(final Attribute<T> attribute) {
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
  public static <T extends Temporal, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> auditInsertTimeProperty(final Attribute<T> attribute, final String caption) {
    return new DefaultAuditTimeProperty<>(attribute, INSERT, caption).builder();
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the time a record was updated.
   * @param attribute the attribute
   * @param <T> the Temporal type to base this property on
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <T extends Temporal, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> auditUpdateTimeProperty(final Attribute<T> attribute) {
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
  public static <T extends Temporal, B extends ColumnProperty.Builder<T, B>> ColumnProperty.Builder<T, B> auditUpdateTimeProperty(final Attribute<T> attribute, final String caption) {
    return new DefaultAuditTimeProperty<>(attribute, UPDATE, caption).builder();
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the username of the user who inserted a record.
   * @param attribute the attribute
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <B extends ColumnProperty.Builder<String, B>> ColumnProperty.Builder<String, B> auditInsertUserProperty(final Attribute<String> attribute) {
    return auditInsertUserProperty(attribute, null);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the username of the user who inserted a record.
   * @param attribute the attribute
   * @param <B> the builder type
   * @param caption the property caption
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <B extends ColumnProperty.Builder<String, B>> ColumnProperty.Builder<String, B> auditInsertUserProperty(final Attribute<String> attribute, final String caption) {
    return new DefaultAuditUserProperty(attribute, INSERT, caption).builder();
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the username of the user who updated a record.
   * @param attribute the attribute
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <B extends ColumnProperty.Builder<String, B>> ColumnProperty.Builder<String, B> auditUpdateUserProperty(final Attribute<String> attribute) {
    return auditUpdateUserProperty(attribute, null);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the username of the user who updated a record.
   * @param attribute the attribute
   * @param caption the property caption
   * @param <B> the builder type
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <B extends ColumnProperty.Builder<String, B>> ColumnProperty.Builder<String, B> auditUpdateUserProperty(final Attribute<String> attribute, final String caption) {
    return new DefaultAuditUserProperty(attribute, UPDATE, caption).builder();
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
  public static <T> ColumnProperty.ValueConverter<Boolean, T> booleanValueConverter(final T trueValue, final T falseValue) {
    return new DefaultColumnProperty.BooleanValueConverter<>(trueValue, falseValue);
  }

  /**
   * Sorts the given properties by caption, or if that is not available, attribute name, ignoring case
   * @param properties the properties to sort
   * @return the sorted list
   */
  public static List<Property<?>> sort(final List<Property<?>> properties) {
    requireNonNull(properties, "properties");
    final Collator collator = Collator.getInstance();
    properties.sort((o1, o2) -> collator.compare(o1.toString().toLowerCase(), o2.toString().toLowerCase()));

    return properties;
  }
}

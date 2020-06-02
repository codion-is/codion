/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.item.Item;
import is.codion.framework.domain.attribute.Attribute;
import is.codion.framework.domain.entity.Entity;

import java.text.Collator;
import java.time.LocalDateTime;
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
   * @param <T> the property type
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <T> ColumnProperty.Builder<T> columnProperty(final Attribute<T> attribute) {
    return columnProperty(attribute, null);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance.
   * @param attribute the attribute
   * @param caption the property caption
   * @param <T> the property type
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <T> ColumnProperty.Builder<T> columnProperty(final Attribute<T> attribute, final String caption) {
    return new DefaultColumnProperty<>(attribute, caption).builder();
  }

  /**
   * A convenience method for creating a new {@link ColumnProperty.Builder} instance,
   * with the primary key index set to 0.
   * @param attribute the attribute
   * @param <T> the property type
   * @return a new {@link ColumnProperty.Builder} with primary key index 0
   */
  public static <T> ColumnProperty.Builder<T> primaryKeyProperty(final Attribute<T> attribute) {
    return primaryKeyProperty(attribute, null);
  }

  /**
   * A convenience method for creating a new {@link ColumnProperty.Builder} instance,
   * with the primary key index set to 0.
   * @param attribute the attribute
   * @param caption the property caption
   * @param <T> the property type
   * @return a new {@link ColumnProperty.Builder} with primary key index 0
   */
  public static <T> ColumnProperty.Builder<T> primaryKeyProperty(final Attribute<T> attribute, final String caption) {
    return columnProperty(attribute, caption).primaryKeyIndex(0);
  }

  /**
   * Instantiates a {@link ForeignKeyProperty.Builder} instance.
   * @param attribute the attribute
   * @param caption the caption
   * @param foreignEntityId the id of the entity referenced by this foreign key
   * @param columnPropertyBuilder the {@link ColumnProperty.Builder} for the underlying
   * column property comprising this foreign key relation
   * @return a new {@link ForeignKeyProperty.Builder}
   */
  public static ForeignKeyProperty.Builder foreignKeyProperty(final Attribute<Entity> attribute,
                                                              final String caption, final Entity.Identity foreignEntityId,
                                                              final ColumnProperty.Builder<?> columnPropertyBuilder) {
    return new DefaultForeignKeyProperty(attribute, caption, foreignEntityId, columnPropertyBuilder).builder();
  }

  /**
   * Instantiates a {@link ForeignKeyProperty.Builder} instance.
   * @param attribute the attribute
   * @param caption the caption
   * @param foreignEntityId the id of the entity referenced by this foreign key
   * @param columnPropertyBuilders a List containing the {@link ColumnProperty.Builder}s for the underlying
   * column properties comprising this foreign key relation, in the same order as the column properties
   * they reference appear in the the referenced entities primary key
   * @return a new {@link ForeignKeyProperty.Builder}
   */
  public static ForeignKeyProperty.Builder foreignKeyProperty(final Attribute<Entity> attribute,
                                                              final String caption, final Entity.Identity foreignEntityId,
                                                              final List<ColumnProperty.Builder<?>> columnPropertyBuilders) {
    return new DefaultForeignKeyProperty(attribute, caption, foreignEntityId, columnPropertyBuilders).builder();
  }

  /**
   * Instantiates a {@link TransientProperty.Builder} instance, for displaying a value from a
   * entity referenced via a foreign key.
   * @param attribute the attribute
   * @param entityAttribute the id of the foreign key attribute from which this property gets its value
   * @param denormalizedAttribute the property from the referenced entity, from which this property gets its value
   * @param caption the caption of this property
   * @param <T> the property type
   * @return a new {@link TransientProperty.Builder}
   */
  public static <T> TransientProperty.Builder<T> denormalizedViewProperty(final Attribute<T> attribute,
                                                                          final Attribute<Entity> entityAttribute,
                                                                          final Attribute<T> denormalizedAttribute, final String caption) {
    final DerivedProperty.Provider<T> valueProvider = linkedValues -> {
      final Entity foreignKeyValue = (Entity) linkedValues.get(entityAttribute);

      return foreignKeyValue == null ? null : foreignKeyValue.get(denormalizedAttribute);
    };

    return new DefaultDerivedProperty<>(attribute, caption, valueProvider, entityAttribute).builder();
  }

  /**
   * Instantiates a {@link TransientProperty.Builder} instance, which value is derived from
   * or more linked property values.
   * @param attribute the attribute
   * @param caption the caption
   * @param valueProvider a {@link DerivedProperty.Provider} instance responsible for deriving the value
   * @param linkedAttributes the ids of the properties from which this property derives its value
   * @param <T> the property type
   * @return a new {@link TransientProperty.Builder}
   * @throws IllegalArgumentException in case no linked property ids are provided
   */
  public static <T> TransientProperty.Builder<T> derivedProperty(final Attribute<T> attribute, final String caption,
                                                                 final DerivedProperty.Provider<T> valueProvider,
                                                                 final Attribute<?>... linkedAttributes) {
    return new DefaultDerivedProperty<>(attribute, caption, valueProvider, linkedAttributes).builder();
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, which value should mirror the value from
   * a entity referenced by a foreign key.
   * @param attribute the attribute, in case of database properties this should be the underlying column name
   * @param entityAttribute the id of the foreign key reference which owns the attribute which value to mirror
   * @param denormalizedAttribute the attribute from which this property should get its value
   * @param <T> the property type
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <T> ColumnProperty.Builder<T> denormalizedProperty(final Attribute<T> attribute,
                                                                   final Attribute<Entity> entityAttribute,
                                                                   final Attribute<T> denormalizedAttribute) {
    return denormalizedProperty(attribute, entityAttribute, denormalizedAttribute, null);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, which value should mirror the value from
   * a entity referenced by a foreign key.
   * @param attribute the attribute, in case of database properties this should be the underlying column name
   * @param entityAttribute the id of the foreign key reference which owns the attribute which value to mirror
   * @param denormalizedAttribute the property from which this attribute should get its value
   * @param caption the property caption
   * @param <T> the property type
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <T> ColumnProperty.Builder<T> denormalizedProperty(final Attribute<T> attribute,
                                                                   final Attribute<Entity> entityAttribute,
                                                                   final Attribute<T> denormalizedAttribute, final String caption) {
    return new DefaultDenormalizedProperty<>(attribute, entityAttribute, denormalizedAttribute, caption).builder();
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, based on a sub-query.
   * @param attribute the attribute
   * @param caption the property caption
   * @param subquery the sql query
   * @param <T> the property type
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <T> ColumnProperty.Builder<T> subqueryProperty(final Attribute<T> attribute, final String caption,
                                                               final String subquery) {
    return new DefaultSubqueryProperty<>(attribute, caption, subquery).builder();
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, based on the given items.
   * @param attribute the attribute
   * @param caption the property caption
   * @param validItems the Items representing all the valid values for this property
   * @param <T> the property type
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <T> ColumnProperty.Builder<T> valueListProperty(final Attribute<T> attribute, final String caption,
                                                                final List<Item<T>> validItems) {
    return new DefaultValueListProperty<>(attribute, caption, validItems).builder();
  }

  /**
   * Creates a new {@link TransientProperty.Builder} instance, which does not map to an underlying table column.
   * @param attribute the attribute
   * @param <T> the attribute type
   * @return a new {@link TransientProperty.Builder}
   */
  public static <T> TransientProperty.Builder<T> transientProperty(final Attribute<T> attribute) {
    return transientProperty(attribute, null);
  }

  /**
   * Creates a new {@link TransientProperty.Builder} instance, which does not map to an underlying table column.
   * @param attribute the attribute
   * @param caption the property caption
   * @param <T> the attribute type
   * @return a new {@link TransientProperty.Builder}
   */
  public static <T> TransientProperty.Builder<T> transientProperty(final Attribute<T> attribute, final String caption) {
    return new DefaultTransientProperty<>(attribute, caption).builder();
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance representing a Boolean value.
   * @param attribute the attribute
   * @param columnType the sql data type of the underlying column
   * @param trueValue the value representing 'true' in the underlying column
   * @param falseValue the value representing 'false' in the underlying column
   * @return a new {@link ColumnProperty.Builder}
   */
  public static ColumnProperty.Builder<Boolean> booleanProperty(final Attribute<Boolean> attribute, final int columnType,
                                                                final Object trueValue, final Object falseValue) {
    return booleanProperty(attribute, columnType, null, trueValue, falseValue);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance representing a Boolean value.
   * @param attribute the attribute
   * @param columnType the sql data type of the underlying column
   * @param caption the property caption
   * @param trueValue the value representing 'true' in the underlying column
   * @param falseValue the value representing 'false' in the underlying column
   * @return a new {@link ColumnProperty.Builder}
   */
  public static ColumnProperty.Builder<Boolean> booleanProperty(final Attribute<Boolean> attribute, final int columnType, final String caption,
                                                                final Object trueValue, final Object falseValue) {
    return new DefaultColumnProperty<>(attribute, caption).builder()
            .columnType(columnType)
            .valueConverter(booleanValueConverter(trueValue, falseValue));
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
   * @return a new {@link ColumnProperty.Builder}
   */
  public static ColumnProperty.Builder<LocalDateTime> auditInsertTimeProperty(final Attribute<LocalDateTime> attribute) {
    return auditInsertTimeProperty(attribute, null);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the time a record was inserted.
   * @param attribute the attribute
   * @param caption the property caption
   * @return a new {@link ColumnProperty.Builder}
   */
  public static ColumnProperty.Builder<LocalDateTime> auditInsertTimeProperty(final Attribute<LocalDateTime> attribute, final String caption) {
    return new DefaultAuditTimeProperty(attribute, INSERT, caption).builder();
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the time a record was updated.
   * @param attribute the attribute
   * @return a new {@link ColumnProperty.Builder}
   */
  public static ColumnProperty.Builder<LocalDateTime> auditUpdateTimeProperty(final Attribute<LocalDateTime> attribute) {
    return auditUpdateTimeProperty(attribute, null);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the time a record was updated.
   * @param attribute the attribute
   * @param caption the property caption
   * @return a new {@link ColumnProperty.Builder}
   */
  public static ColumnProperty.Builder<LocalDateTime> auditUpdateTimeProperty(final Attribute<LocalDateTime> attribute, final String caption) {
    return new DefaultAuditTimeProperty(attribute, UPDATE, caption).builder();
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the username of the user who inserted a record.
   * @param attribute the attribute
   * @return a new {@link ColumnProperty.Builder}
   */
  public static ColumnProperty.Builder<String> auditInsertUserProperty(final Attribute<String> attribute) {
    return auditInsertUserProperty(attribute, null);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the username of the user who inserted a record.
   * @param attribute the attribute
   * @param caption the property caption
   * @return a new {@link ColumnProperty.Builder}
   */
  public static ColumnProperty.Builder<String> auditInsertUserProperty(final Attribute<String> attribute, final String caption) {
    return new DefaultAuditUserProperty(attribute, INSERT, caption).builder();
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the username of the user who updated a record.
   * @param attribute the attribute
   * @return a new {@link ColumnProperty.Builder}
   */
  public static ColumnProperty.Builder<String> auditUpdateUserProperty(final Attribute<String> attribute) {
    return auditUpdateUserProperty(attribute, null);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the username of the user who updated a record.
   * @param attribute the attribute
   * @param caption the property caption
   * @return a new {@link ColumnProperty.Builder}
   */
  public static ColumnProperty.Builder<String> auditUpdateUserProperty(final Attribute<String> attribute, final String caption) {
    return new DefaultAuditUserProperty(attribute, UPDATE, caption).builder();
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, for use in a foreign key,
   * mirroring a property which already exists as part of a different foreign key.
   * @param attribute the attribute of the mirrored property
   * @param <T> the attribute type
   * @return a new {@link ColumnProperty.Builder}
   */
  public static <T> ColumnProperty.Builder<T> mirrorProperty(final Attribute<T> attribute) {
    return new DefaultMirrorProperty<>(attribute).builder();
  }

  /**
   * Creates a new {@link ColumnProperty.ValueConverter} instance for converting a column value
   * representing a boolean value to and from an actual Boolean.
   * @param trueValue the value used to represent 'true' in the underlying database, can be null
   * @param falseValue the value used to represent 'false' in the underlying database, can be null
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

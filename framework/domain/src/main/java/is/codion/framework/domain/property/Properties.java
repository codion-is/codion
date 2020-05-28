/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.item.Item;
import is.codion.common.valuemap.ValueMap;

import java.sql.Types;
import java.text.Collator;
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
   * Creates a new {@link Attribute}.
   * @param attributeId the attributeId
   * @param <T> the attribute type
   * @return a new {@link Attribute}
   */
  public static <T> Attribute<T> attribute(final String attributeId) {
    return new DefaultAttribute<>(attributeId);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance.
   * @param  propertyId the propertyId
   * @param type the property sql data type
   * @return a new {@link ColumnProperty.Builder}
   */
  public static ColumnProperty.Builder columnProperty(final Attribute<?> propertyId, final int type) {
    return columnProperty(propertyId, type, null);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance.
   * @param  propertyId the propertyId
   * @param type the property sql data type
   * @param caption the property caption
   * @return a new {@link ColumnProperty.Builder}
   */
  public static ColumnProperty.Builder columnProperty(final Attribute<?> propertyId, final int type, final String caption) {
    return new DefaultColumnProperty(propertyId, type, caption).builder();
  }

  /**
   * A convenience method for creating a new {@link ColumnProperty.Builder} instance,
   * with the primary key index set to 0.
   * @param  propertyId the propertyId
   * @param type the property sql data type
   * @return a new {@link ColumnProperty.Builder} with primary key index 0
   */
  public static ColumnProperty.Builder primaryKeyProperty(final Attribute<?> propertyId, final int type) {
    return primaryKeyProperty(propertyId, type, null);
  }

  /**
   * A convenience method for creating a new {@link ColumnProperty.Builder} instance,
   * with the primary key index set to 0.
   * @param  propertyId the propertyId
   * @param type the property sql data type
   * @param caption the property caption
   * @return a new {@link ColumnProperty.Builder} with primary key index 0
   */
  public static ColumnProperty.Builder primaryKeyProperty(final Attribute<?> propertyId, final int type, final String caption) {
    return columnProperty(propertyId, type, caption).primaryKeyIndex(0);
  }

  /**
   * Instantiates a {@link ForeignKeyProperty.Builder} instance.
   * @param  propertyId the propertyId
   * @param caption the caption
   * @param foreignEntityId the id of the entity referenced by this foreign key
   * @param columnPropertyBuilder the {@link ColumnProperty.Builder} for the underlying
   * column property comprising this foreign key relation
   * @return a new {@link ForeignKeyProperty.Builder}
   */
  public static ForeignKeyProperty.Builder foreignKeyProperty(final Attribute<?> propertyId, final String caption,
                                                              final String foreignEntityId,
                                                              final ColumnProperty.Builder columnPropertyBuilder) {
    return new DefaultForeignKeyProperty(propertyId, caption, foreignEntityId, columnPropertyBuilder).builder();
  }

  /**
   * Instantiates a {@link ForeignKeyProperty.Builder} instance.
   * @param  propertyId the propertyId
   * @param caption the caption
   * @param foreignEntityId the id of the entity referenced by this foreign key
   * @param columnPropertyBuilders a List containing the {@link ColumnProperty.Builder}s for the underlying
   * column properties comprising this foreign key relation, in the same order as the column properties
   * they reference appear in the the referenced entities primary key
   * @return a new {@link ForeignKeyProperty.Builder}
   */
  public static ForeignKeyProperty.Builder foreignKeyProperty(final Attribute<?> propertyId, final String caption,
                                                              final String foreignEntityId,
                                                              final List<ColumnProperty.Builder> columnPropertyBuilders) {
    return new DefaultForeignKeyProperty(propertyId, caption, foreignEntityId, columnPropertyBuilders).builder();
  }

  /**
   * Instantiates a {@link TransientProperty.Builder} instance, for displaying a value from a
   * entity referenced via a foreign key.
   * @param propertyId the id of the property
   * @param foreignKeyPropertyId the id of the foreign key property from which this property gets its value
   * @param property the property from the referenced entity, from which this property gets its value
   * @param caption the caption of this property
   * @return a new {@link TransientProperty.Builder}
   */
  public static TransientProperty.Builder denormalizedViewProperty(final Attribute<?> propertyId, final Attribute<?> foreignKeyPropertyId,
                                                                   final Property property, final String caption) {
    final DerivedProperty.Provider valueProvider = linkedValues -> {
      final ValueMap foreignKeyValue = (ValueMap) linkedValues.get(foreignKeyPropertyId);

      return foreignKeyValue == null ? null : foreignKeyValue.get(property);
    };

    return new DefaultDerivedProperty(propertyId, property.getType(), caption, valueProvider, foreignKeyPropertyId).builder();
  }

  /**
   * Instantiates a {@link TransientProperty.Builder} instance, which value is derived from
   * or more linked property values.
   * @param  propertyId the propertyId
   * @param type the property sql data type
   * @param caption the caption
   * @param valueProvider a {@link DerivedProperty.Provider} instance responsible for deriving the value
   * @param linkedPropertyIds the ids of the properties from which this property derives its value
   * @return a new {@link TransientProperty.Builder}
   * @throws IllegalArgumentException in case no linked property ids are provided
   */
  public static TransientProperty.Builder derivedProperty(final Attribute<?> propertyId, final int type, final String caption,
                                                          final DerivedProperty.Provider valueProvider,
                                                          final Attribute<?>... linkedPropertyIds) {
    return new DefaultDerivedProperty(propertyId, type, caption, valueProvider, linkedPropertyIds).builder();
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, which value should mirror the value from
   * a entity referenced by a foreign key.
   * @param  propertyId the propertyId, in case of database properties this should be the underlying column name
   * @param foreignKeyPropertyId the id of the foreign key reference which owns the property which value to mirror
   * @param denormalizedProperty the property from which this property should get its value
   * @return a new {@link ColumnProperty.Builder}
   */
  public static ColumnProperty.Builder denormalizedProperty(final Attribute<?> propertyId, final Attribute<?> foreignKeyPropertyId,
                                                            final Property denormalizedProperty) {
    return denormalizedProperty(propertyId, foreignKeyPropertyId, denormalizedProperty, null);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, which value should mirror the value from
   * a entity referenced by a foreign key.
   * @param  propertyId the propertyId, in case of database properties this should be the underlying column name
   * @param foreignKeyPropertyId the id of the foreign key reference which owns the property which value to mirror
   * @param denormalizedProperty the property from which this property should get its value
   * @param caption the property caption
   * @return a new {@link ColumnProperty.Builder}
   */
  public static ColumnProperty.Builder denormalizedProperty(final Attribute<?> propertyId, final Attribute<?> foreignKeyPropertyId,
                                                            final Property denormalizedProperty, final String caption) {
    return new DefaultDenormalizedProperty(propertyId, foreignKeyPropertyId, denormalizedProperty, caption).builder();
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, based on a sub-query.
   * @param  propertyId the propertyId
   * @param type the property sql data type
   * @param caption the property caption
   * @param subquery the sql query
   * @return a new {@link ColumnProperty.Builder}
   */
  public static ColumnProperty.Builder subqueryProperty(final Attribute<?> propertyId, final int type, final String caption,
                                                        final String subquery) {
    return new DefaultSubqueryProperty(propertyId, type, caption, subquery).builder();
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, based on the given items.
   * @param  propertyId the propertyId
   * @param type the property sql data type
   * @param caption the property caption
   * @param validItems the Items representing all the valid values for this property
   * @return a new {@link ColumnProperty.Builder}
   */
  public static ColumnProperty.Builder valueListProperty(final Attribute<?> propertyId, final int type, final String caption,
                                                         final List<Item> validItems) {
    return new DefaultValueListProperty(propertyId, type, caption, validItems).builder();
  }

  /**
   * Creates a new {@link TransientProperty.Builder} instance, which does not map to an underlying table column.
   * @param  propertyId the propertyId
   * @param type the property sql data type
   * @return a new {@link TransientProperty.Builder}
   */
  public static TransientProperty.Builder transientProperty(final Attribute<?> propertyId, final int type) {
    return transientProperty(propertyId, type, null);
  }

  /**
   * Creates a new {@link TransientProperty.Builder} instance, which does not map to an underlying table column.
   * @param  propertyId the propertyId
   * @param type the property sql data type
   * @param caption the property caption
   * @return a new {@link TransientProperty.Builder}
   */
  public static TransientProperty.Builder transientProperty(final Attribute<?> propertyId, final int type,
                                                            final String caption) {
    return new DefaultTransientProperty(propertyId, type, caption).builder();
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance representing a Boolean value.
   * @param  propertyId the propertyId
   * @param columnType the sql data type of the underlying column
   * @param trueValue the value representing 'true' in the underlying column
   * @param falseValue the value representing 'false' in the underlying column
   * @return a new {@link ColumnProperty.Builder}
   */
  public static ColumnProperty.Builder booleanProperty(final Attribute<?> propertyId, final int columnType,
                                                       final Object trueValue, final Object falseValue) {
    return booleanProperty(propertyId, columnType, null, trueValue, falseValue);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance representing a Boolean value.
   * @param  propertyId the propertyId
   * @param columnType the sql data type of the underlying column
   * @param caption the property caption
   * @param trueValue the value representing 'true' in the underlying column
   * @param falseValue the value representing 'false' in the underlying column
   * @return a new {@link ColumnProperty.Builder}
   */
  public static ColumnProperty.Builder booleanProperty(final Attribute<?> propertyId, final int columnType, final String caption,
                                                       final Object trueValue, final Object falseValue) {
    return new DefaultColumnProperty(propertyId, Types.BOOLEAN, caption).builder()
            .columnType(columnType)
            .valueConverter(booleanValueConverter(trueValue, falseValue));
  }

  /**
   * Creates a new {@link BlobProperty.Builder} instance.
   * @param  propertyId the propertyId
   * @return a new {@link BlobProperty.Builder}
   */
  public static BlobProperty.Builder blobProperty(final Attribute<?> propertyId) {
    return blobProperty(propertyId, null);
  }

  /**
   * Creates a new {@link BlobProperty.Builder} instance.
   * @param  propertyId the propertyId
   * @param caption the property caption
   * @return a new {@link BlobProperty.Builder}
   */
  public static BlobProperty.Builder blobProperty(final Attribute<?> propertyId, final String caption) {
    return new DefaultBlobProperty(propertyId, caption).builder();
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the time a record was inserted.
   * @param  propertyId the propertyId
   * @return a new {@link ColumnProperty.Builder}
   */
  public static ColumnProperty.Builder auditInsertTimeProperty(final Attribute<?> propertyId) {
    return auditInsertTimeProperty(propertyId, null);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the time a record was inserted.
   * @param  propertyId the propertyId
   * @param caption the property caption
   * @return a new {@link ColumnProperty.Builder}
   */
  public static ColumnProperty.Builder auditInsertTimeProperty(final Attribute<?> propertyId, final String caption) {
    return new DefaultAuditTimeProperty(propertyId, INSERT, caption).builder();
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the time a record was updated.
   * @param  propertyId the propertyId
   * @return a new {@link ColumnProperty.Builder}
   */
  public static ColumnProperty.Builder auditUpdateTimeProperty(final Attribute<?> propertyId) {
    return auditUpdateTimeProperty(propertyId, null);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the time a record was updated.
   * @param  propertyId the propertyId
   * @param caption the property caption
   * @return a new {@link ColumnProperty.Builder}
   */
  public static ColumnProperty.Builder auditUpdateTimeProperty(final Attribute<?> propertyId, final String caption) {
    return new DefaultAuditTimeProperty(propertyId, UPDATE, caption).builder();
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the username of the user who inserted a record.
   * @param  propertyId the propertyId
   * @return a new {@link ColumnProperty.Builder}
   */
  public static ColumnProperty.Builder auditInsertUserProperty(final Attribute<?> propertyId) {
    return auditInsertUserProperty(propertyId, null);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the username of the user who inserted a record.
   * @param  propertyId the propertyId
   * @param caption the property caption
   * @return a new {@link ColumnProperty.Builder}
   */
  public static ColumnProperty.Builder auditInsertUserProperty(final Attribute<?> propertyId, final String caption) {
    return new DefaultAuditUserProperty(propertyId, INSERT, caption).builder();
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the username of the user who updated a record.
   * @param  propertyId the propertyId
   * @return a new {@link ColumnProperty.Builder}
   */
  public static ColumnProperty.Builder auditUpdateUserProperty(final Attribute<?> propertyId) {
    return auditUpdateUserProperty(propertyId, null);
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, representing the username of the user who updated a record.
   * @param  propertyId the propertyId
   * @param caption the property caption
   * @return a new {@link ColumnProperty.Builder}
   */
  public static ColumnProperty.Builder auditUpdateUserProperty(final Attribute<?> propertyId, final String caption) {
    return new DefaultAuditUserProperty(propertyId, UPDATE, caption).builder();
  }

  /**
   * Creates a new {@link ColumnProperty.Builder} instance, for use in a foreign key,
   * mirroring a property which already exists as part of a different foreign key.
   * @param  propertyId the propertyId of the mirrored property
   * @return a new {@link ColumnProperty.Builder}
   */
  public static ColumnProperty.Builder mirrorProperty(final Attribute<?> propertyId) {
    return new DefaultMirrorProperty(propertyId).builder();
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
   * Sorts the given properties by caption, or if that is not available, property id, ignoring case
   * @param properties the properties to sort
   * @return the sorted list
   */
  public static List<Property> sort(final List<Property> properties) {
    requireNonNull(properties, "properties");
    final Collator collator = Collator.getInstance();
    properties.sort((o1, o2) -> collator.compare(o1.toString().toLowerCase(), o2.toString().toLowerCase()));

    return properties;
  }
}

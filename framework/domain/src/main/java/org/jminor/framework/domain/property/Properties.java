/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

import org.jminor.common.Item;
import org.jminor.common.db.ValueConverter;
import org.jminor.common.db.valuemap.ValueMap;

import java.sql.Types;
import java.text.Collator;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.jminor.framework.domain.property.AuditProperty.AuditAction.INSERT;
import static org.jminor.framework.domain.property.AuditProperty.AuditAction.UPDATE;

/**
 * A Property factory class.
 */
public final class Properties {

  private Properties() {}

  /**
   * @param propertyId the property ID
   * @return a new column property
   */
  public static ColumnPropertyBuilder columnProperty(final String propertyId) {
    return columnProperty(propertyId, Types.INTEGER);
  }

  /**
   * @param propertyId the property ID
   * @param type the property type
   * @return a new column property
   */
  public static ColumnPropertyBuilder columnProperty(final String propertyId, final int type) {
    return columnProperty(propertyId, type, null);
  }

  /**
   * @param propertyId the property ID
   * @param type the property type
   * @param caption the caption
   * @return a new column property
   */
  public static ColumnPropertyBuilder columnProperty(final String propertyId, final int type, final String caption) {
    final DefaultColumnProperty property =
            new DefaultColumnProperty(propertyId, type, caption);

    return new DefaultColumnPropertyBuilder(property);
  }

  /**
   * @param propertyId the property ID
   * @return a new primary key property with index 0
   */
  public static ColumnPropertyBuilder primaryKeyProperty(final String propertyId) {
    return primaryKeyProperty(propertyId, Types.INTEGER);
  }

  /**
   * @param propertyId the property ID
   * @param type the property type
   * @return a new primary key property with index 0
   */
  public static ColumnPropertyBuilder primaryKeyProperty(final String propertyId, final int type) {
    return primaryKeyProperty(propertyId, type, null);
  }

  /**
   * @param propertyId the property ID
   * @param type the property type
   * @param caption the caption
   * @return a new primary key property with index 0
   */
  public static ColumnPropertyBuilder primaryKeyProperty(final String propertyId, final int type, final String caption) {
    return new DefaultColumnPropertyBuilder(new DefaultColumnProperty(propertyId, type, caption)).setPrimaryKeyIndex(0);
  }

  /**
   * Instantiates a {@link ForeignKeyProperty}
   * @param propertyId the property ID
   * @param caption the caption
   * @param foreignEntityId the ID of the entity referenced by this foreign key
   * @param columnPropertyBuilder the underlying column property comprising this foreign key
   * @return a new foreign key property
   */
  public static ForeignKeyPropertyBuilder foreignKeyProperty(final String propertyId, final String caption,
                                                             final String foreignEntityId,
                                                             final ColumnPropertyBuilder columnPropertyBuilder) {
    return new DefaultForeignKeyPropertyBuilder(new DefaultForeignKeyProperty(propertyId, caption,
            foreignEntityId, columnPropertyBuilder));
  }

  /**
   * Instantiates a {@link ForeignKeyProperty}
   * @param propertyId the property ID, note that this is not a column name
   * @param caption the property caption
   * @param foreignEntityId the ID of the entity referenced by this foreign key
   * @param columnProperties the underlying column properties comprising this foreign key
   * @return a new foreign key property
   */
  public static ForeignKeyPropertyBuilder foreignKeyProperty(final String propertyId, final String caption,
                                                             final String foreignEntityId,
                                                             final List<ColumnPropertyBuilder> columnProperties) {
    return new DefaultForeignKeyPropertyBuilder(new DefaultForeignKeyProperty(propertyId, caption,
            foreignEntityId, columnProperties));
  }

  /**
   * @param propertyId the ID of the property, this should not be a column name since this property does not
   * map to a table column
   * @param foreignKeyPropertyId the ID of the foreign key property from which entity value this property gets its value
   * @param property the property from which this property gets its value
   * @param caption the caption of this property
   * @return a new denormalized view property
   */
  public static TransientPropertyBuilder denormalizedViewProperty(final String propertyId, final String foreignKeyPropertyId,
                                                                  final Property property, final String caption) {
    final DerivedProperty.Provider valueProvider = linkedValues -> {
      final ValueMap foreignKeyValue = (ValueMap) linkedValues.get(foreignKeyPropertyId);

      return foreignKeyValue == null ? null : foreignKeyValue.get(property);
    };

    return new DefaultTransientPropertyBuilder(new DefaultDerivedProperty(propertyId, property.getType(),
            caption, valueProvider, foreignKeyPropertyId));
  }

  /**
   * @param propertyId the property ID
   * @param type the property type
   * @param caption the caption
   * @param valueProvider the object responsible for providing the derived value
   * @param linkedPropertyIds the IDs of the properties on whose value this property derives its value
   * @return a new derived property
   * @throws IllegalArgumentException in case no linked property IDs are provided
   */
  public static TransientPropertyBuilder derivedProperty(final String propertyId, final int type, final String caption,
                                                         final DerivedProperty.Provider valueProvider,
                                                         final String... linkedPropertyIds) {
    return new DefaultTransientPropertyBuilder(new DefaultDerivedProperty(propertyId, type, caption,
            valueProvider, linkedPropertyIds));
  }

  /**
   * @param propertyId the property ID, in case of database properties this should be the underlying column name
   * @param foreignKeyPropertyId the ID of the foreign key property which references the entity which owns
   * the denormalized property
   * @param denormalizedProperty the property from which this property should get its value
   * @return a new denormalized property
   */
  public static PropertyBuilder denormalizedProperty(final String propertyId, final String foreignKeyPropertyId,
                                                     final Property denormalizedProperty) {
    return denormalizedProperty(propertyId, foreignKeyPropertyId, denormalizedProperty, null);
  }

  /**
   * @param propertyId the property ID, in case of database properties this should be the underlying column name
   * @param foreignKeyPropertyId the ID of the foreign key property which references the entity which owns
   * the denormalized property
   * @param denormalizedProperty the property from which this property should get its value
   * @param caption the caption if this property
   * @return a new denormalized property
   */
  public static PropertyBuilder denormalizedProperty(final String propertyId, final String foreignKeyPropertyId,
                                                     final Property denormalizedProperty, final String caption) {
    return new DefaultPropertyBuilder(new DefaultDenormalizedProperty(propertyId, foreignKeyPropertyId,
            denormalizedProperty, caption));
  }

  /**
   * @param propertyId the property ID, since SubqueryProperties do not map to underlying table columns,
   * the property ID should not be column name, only be unique for this entity
   * @param type the data type of this property
   * @param caption the caption of this property
   * @param subquery the sql query
   * @return a new subquery property
   */
  public static ColumnPropertyBuilder subqueryProperty(final String propertyId, final int type, final String caption,
                                                       final String subquery) {
    return subqueryProperty(propertyId, type, caption, subquery, type);
  }

  /**
   * @param propertyId the property ID, since SubqueryProperties do not map to underlying table columns,
   * the property ID should not be column name, only be unique for this entity
   * @param type the data type of this property
   * @param caption the caption of this property
   * @param subquery the sql query
   * @param columnType the actual column type
   * @return a new subquery property
   */
  public static ColumnPropertyBuilder subqueryProperty(final String propertyId, final int type, final String caption,
                                                       final String subquery, final int columnType) {
    return new DefaultColumnPropertyBuilder(new DefaultSubqueryProperty(propertyId, type, caption, subquery, columnType));
  }

  /**
   * @param propertyId the property ID
   * @param type the data type of this property
   * @param caption the property caption
   * @param validItems all allowed values for this property
   * @return a new value list property
   */
  public static ColumnPropertyBuilder valueListProperty(final String propertyId, final int type, final String caption,
                                                        final List<Item> validItems) {
    return new DefaultColumnPropertyBuilder(new DefaultValueListProperty(propertyId, type, caption, validItems));
  }

  /**
   * @param propertyId the property ID, since TransientProperties do not map to underlying table columns,
   * the property ID should not be column name, only be unique for this entity
   * @param type the data type of this property
   * @return a new transient property
   */
  public static TransientPropertyBuilder transientProperty(final String propertyId, final int type) {
    return transientProperty(propertyId, type, null);
  }

  /**
   * @param propertyId the property ID, since TransientProperties do not map to underlying table columns,
   * the property ID should not be column name, only be unique for this entity
   * @param type the data type of this property
   * @param caption the caption of this property
   * @return a new transient property
   */
  public static TransientPropertyBuilder transientProperty(final String propertyId, final int type,
                                                           final String caption) {
    return new DefaultTransientPropertyBuilder(new DefaultTransientProperty(propertyId, type, caption));
  }

  /**
   * @param propertyId the property ID
   * @param columnType the data type of the underlying column
   * @param trueValue the value representing 'true' in the underlying column
   * @param falseValue the value representing 'false' in the underlying column
   * @return a new boolean property
   */
  public static ColumnPropertyBuilder booleanProperty(final String propertyId, final int columnType,
                                                      final Object trueValue, final Object falseValue) {
    return booleanProperty(propertyId, columnType, null, trueValue, falseValue);
  }

  /**
   * @param propertyId the property ID
   * @param columnType the data type of the underlying column
   * @param caption the caption of this property
   * @param trueValue the value representing 'true' in the underlying column
   * @param falseValue the value representing 'false' in the underlying column
   * @return a new boolean property
   */
  public static ColumnPropertyBuilder booleanProperty(final String propertyId, final int columnType,
                                                      final String caption,
                                                      final Object trueValue, final Object falseValue) {
    return new DefaultColumnPropertyBuilder(new DefaultColumnProperty(propertyId, Types.BOOLEAN, caption, columnType))
            .setValueConverter(booleanValueConverter(trueValue, falseValue));
  }

  /**
   * @param propertyId the property ID
   * @return a new audit insert time property
   */
  public static ColumnPropertyBuilder auditInsertTimeProperty(final String propertyId) {
    return auditInsertTimeProperty(propertyId, null);
  }

  /**
   * @param propertyId the property ID
   * @param caption the caption
   * @return a new audit insert time property
   */
  public static ColumnPropertyBuilder auditInsertTimeProperty(final String propertyId, final String caption) {
    return new DefaultColumnPropertyBuilder(new AuditProperty.DefaultAuditTimeProperty(propertyId, INSERT, caption));
  }

  /**
   * @param propertyId the property ID
   * @return a new audit update time property
   */
  public static ColumnPropertyBuilder auditUpdateTimeProperty(final String propertyId) {
    return auditUpdateTimeProperty(propertyId, null);
  }

  /**
   * @param propertyId the property ID
   * @param caption the caption
   * @return a new audit update time property
   */
  public static ColumnPropertyBuilder auditUpdateTimeProperty(final String propertyId, final String caption) {
    return new DefaultColumnPropertyBuilder(new AuditProperty.DefaultAuditTimeProperty(propertyId, UPDATE, caption));
  }

  /**
   * @param propertyId the property ID
   * @return a new audit insert user property
   */
  public static ColumnPropertyBuilder auditInsertUserProperty(final String propertyId) {
    return auditInsertUserProperty(propertyId, null);
  }

  /**
   * @param propertyId the property ID
   * @param caption the caption
   * @return a new audit insert user property
   */
  public static ColumnPropertyBuilder auditInsertUserProperty(final String propertyId, final String caption) {
    return new DefaultColumnPropertyBuilder(new AuditProperty.DefaultAuditUserProperty(propertyId, INSERT, caption));
  }

  /**
   * @param propertyId the property ID
   * @return a new audit update user property
   */
  public static ColumnPropertyBuilder auditUpdateUserProperty(final String propertyId) {
    return auditUpdateUserProperty(propertyId, null);
  }

  /**
   * @param propertyId the property ID
   * @param caption the caption
   * @return a new audit update user property
   */
  public static ColumnPropertyBuilder auditUpdateUserProperty(final String propertyId, final String caption) {
    return new DefaultColumnPropertyBuilder(new AuditProperty.DefaultAuditUserProperty(propertyId, UPDATE, caption));
  }

  /**
   * @param propertyId the property ID
   * @return a new mirror property
   */
  public static ColumnPropertyBuilder mirrorProperty(final String propertyId) {
    return new DefaultColumnPropertyBuilder(new DefaultMirrorProperty(propertyId));
  }

  /**
   * @param trueValue the value used to represent 'true' in the underlying database, can be null
   * @param falseValue the value used to represent 'false' in the underlying database, can be null
   * @param <T> the type of the value used to represent a boolean
   * @return a value converter which converts an underlying database representation
   * of a boolean value into an actual Boolean
   */
  public static <T> ValueConverter<Boolean, T> booleanValueConverter(final T trueValue, final T falseValue) {
    return new DefaultProperty.BooleanValueConverter(trueValue, falseValue);
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

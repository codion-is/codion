/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.Item;

import java.sql.Types;
import java.util.List;

/**
 * A Property factory class.
 */
public final class Properties {

  private Properties() {}

  /**
   * @param propertyID the property ID
   * @return a new column property
   */
  public static Property.ColumnProperty columnProperty(final String propertyID) {
    return columnProperty(propertyID, Types.INTEGER);
  }

  /**
   * @param propertyID the property ID
   * @param type the property type
   * @return a new column property
   */
  public static Property.ColumnProperty columnProperty(final String propertyID, final int type) {
    return columnProperty(propertyID, type, null);
  }

  /**
   * @param propertyID the property ID
   * @param type the property type
   * @param caption the caption
   * @return a new column property
   */
  public static Property.ColumnProperty columnProperty(final String propertyID,final int type, final String caption) {
    return new DefaultProperty.DefaultColumnProperty(propertyID, type, caption);
  }

  /**
   * @param propertyID the property ID
   * @return a new primary key property
   */
  public static Property.PrimaryKeyProperty primaryKeyProperty(final String propertyID) {
    return primaryKeyProperty(propertyID, Types.INTEGER);
  }

  /**
   * @param propertyID the property ID
   * @param type the property type
   * @return a new primary key property
   */
  public static Property.PrimaryKeyProperty primaryKeyProperty(final String propertyID, final int type) {
    return primaryKeyProperty(propertyID, type, null);
  }

  /**
   * @param propertyID the property ID
   * @param type the property type
   * @param caption the caption
   * @return a new primary key property
   */
  public static Property.PrimaryKeyProperty primaryKeyProperty(final String propertyID, final int type, final String caption) {
    return new DefaultProperty.DefaultPrimaryKeyProperty(propertyID, type, caption);
  }

  /**
   * @param propertyID the property ID
   * @param caption the caption
   * @param referencedEntityID the ID of the referenced entity
   * @param referenceProperty the actual reference property
   * @return a new foreign key property
   */
  public static Property.ForeignKeyProperty foreignKeyProperty(final String propertyID, final String caption,
                                                               final String referencedEntityID,
                                                               final Property.ColumnProperty referenceProperty) {
    return new DefaultProperty.DefaultForeignKeyProperty(propertyID, caption, referencedEntityID, referenceProperty);
  }

  /**
   * @param propertyID the property ID, since EntityProperties are meta properties, the property ID should not
   * be a underlying table column, it must only be unique for this entity
   * @param caption the property caption
   * @param referencedEntityID the ID of the referenced entity type
   * @param referenceProperties the actual column properties involved in the reference
   * @param referencedPropertyIDs the IDs of the properties referenced, in the same order as the reference properties
   * @return a new foreign key proeprty
   */
  public static Property.ForeignKeyProperty foreignKeyProperty(final String propertyID, final String caption,
                                                               final String referencedEntityID,
                                                               final Property.ColumnProperty[] referenceProperties,
                                                               final String[] referencedPropertyIDs) {
    return new DefaultProperty.DefaultForeignKeyProperty(propertyID, caption, referencedEntityID, referenceProperties, referencedPropertyIDs);
  }

  /**
   * @param propertyID the ID of the property, this should not be a column name since this property does not
   * map to a table column
   * @param foreignKeyPropertyID the ID of the foreign key property from which entity value this property gets its value
   * @param property the property from which this property gets its value
   * @param caption the caption of this property
   * @return a new denormalized view property
   */
  public static Property.DenormalizedViewProperty denormalizedViewProperty(final String propertyID, final String foreignKeyPropertyID,
                                                                           final Property property, final String caption) {
    return new DefaultProperty.DefaultDenormalizedViewProperty(propertyID, foreignKeyPropertyID, property, caption);
  }

  /**
   * @param propertyID the property ID
   * @param type the property type
   * @param caption the caption
   * @param valueProvider the object responsible for providing the derived value
   * @param linkedPropertyIDs the IDs of the properties on whose value this property derives its value
   * @return a new derived property
   * @throws IllegalArgumentException in case no linked property IDs are provided
   */
  public static Property.DerivedProperty derivedProperty(final String propertyID, final int type, final String caption,
                                                         final Property.DerivedProperty.Provider valueProvider,
                                                         final String... linkedPropertyIDs) {
    return new DefaultProperty.DefaultDerivedProperty(propertyID, type, caption, valueProvider, linkedPropertyIDs);
  }

  /**
   * @param propertyID the property ID, in case of database properties this should be the underlying column name
   * @param foreignKeyPropertyID the ID of the foreign key property which references the entity which owns
   * the denormalized property
   * @param denormalizedProperty the property from which this property should get its value
   * @return a new denormalized property
   */
  public static Property denormalizedProperty(final String propertyID, final String foreignKeyPropertyID,
                                              final Property denormalizedProperty) {
    return denormalizedProperty(propertyID, foreignKeyPropertyID, denormalizedProperty, null);
  }

  /**
   * @param propertyID the property ID, in case of database properties this should be the underlying column name
   * @param foreignKeyPropertyID the ID of the foreign key property which references the entity which owns
   * the denormalized property
   * @param denormalizedProperty the property from which this property should get its value
   * @param caption the caption if this property
   * @return a new denormalized property
   */
  public static Property denormalizedProperty(final String propertyID, final String foreignKeyPropertyID,
                                              final Property denormalizedProperty, final String caption) {
    return new DefaultProperty.DefaultDenormalizedProperty(propertyID, foreignKeyPropertyID, denormalizedProperty, caption);
  }

  /**
   * @param propertyID the property ID, since SubqueryProperties do not map to underlying table columns,
   * the property ID should not be column name, only be unique for this entity
   * @param type the data type of this property
   * @param caption the caption of this property
   * @param subquery the sql query
   * @return a new subquery property
   */
  public static Property.SubqueryProperty subqueryProperty(final String propertyID, final int type, final String caption,
                                                           final String subquery) {
    return subqueryProperty(propertyID, type, caption, subquery, type);
  }

  /**
   * @param propertyID the property ID, since SubqueryProperties do not map to underlying table columns,
   * the property ID should not be column name, only be unique for this entity
   * @param type the data type of this property
   * @param caption the caption of this property
   * @param subquery the sql query
   * @param columnType the actual column type
   * @return a new subquery property
   */
  public static Property.SubqueryProperty subqueryProperty(final String propertyID, final int type, final String caption,
                                                           final String subquery, final int columnType) {
    return new DefaultProperty.DefaultSubqueryProperty(propertyID, type, caption, subquery, columnType);
  }

  /**
   * @param propertyID the property ID
   * @param type the data type of this property
   * @param caption the property caption
   * @param values the values to base this property on
   * @param <T> the value type
   * @return a new value list property
   */
  public static <T> Property.ValueListProperty valueListProperty(final String propertyID, final int type, final String caption,
                                                                 final List<Item<T>> values) {
    return new DefaultProperty.DefaultValueListProperty<T>(propertyID, type, caption, values);
  }

  /**
   * @param propertyID the property ID, since TransientProperties do not map to underlying table columns,
   * the property ID should not be column name, only be unique for this entity
   * @param type the data type of this property
   * @return a new transient property
   */
  public static Property.TransientProperty transientProperty(final String propertyID, final int type) {
    return transientProperty(propertyID, type, null);
  }

  /**
   * @param propertyID the property ID, since TransientProperties do not map to underlying table columns,
   * the property ID should not be column name, only be unique for this entity
   * @param type the data type of this property
   * @param caption the caption of this property
   * @return a new transient property
   */
  public static Property.TransientProperty transientProperty(final String propertyID, final int type, final String caption) {
    return new DefaultProperty.DefautTransientProperty(propertyID, type, caption);
  }

  /**
   * @param propertyID the property ID
   * @param columnType the data type of the underlying column
   * @param caption the caption of this property
   * @return a new boolean property
   */
  public static Property.ColumnProperty booleanProperty(final String propertyID, final int columnType, final String caption) {
    return new DefaultProperty.DefaultColumnProperty(propertyID, Types.BOOLEAN, caption, columnType)
            .setValueConverter(booleanValueConverter());
  }

  /**
   * @param propertyID the property ID
   * @param columnType the data type of the underlying column
   * @param caption the caption of this property
   * @param trueValue the value representing 'true' in the underlying column
   * @param falseValue the value representing 'false' in the underlying column
   * @return a new boolean property
   */
  public static Property.ColumnProperty booleanProperty(final String propertyID, final int columnType, final String caption,
                                                        final Object trueValue, final Object falseValue) {
    return new DefaultProperty.DefaultColumnProperty(propertyID, Types.BOOLEAN, caption, columnType)
            .setValueConverter(booleanValueConverter(trueValue, falseValue));
  }

  /**
   * @param propertyID the property ID
   * @return a new audit insert time property
   */
  public static Property.AuditTimeProperty auditInsertTimeProperty(final String propertyID) {
    return auditInsertTimeProperty(propertyID, null);
  }

  /**
   * @param propertyID the property ID
   * @param caption the caption
   * @return a new audit insert time property
   */
  public static Property.AuditTimeProperty auditInsertTimeProperty(final String propertyID, final String caption) {
    return new DefaultProperty.DefaultAuditTimeProperty(propertyID, Property.AuditProperty.AuditAction.INSERT, caption);
  }

  /**
   * @param propertyID the property ID
   * @return a new audit update time property
   */
  public static Property.AuditTimeProperty auditUpdateTimeProperty(final String propertyID) {
    return auditUpdateTimeProperty(propertyID, null);
  }

  /**
   * @param propertyID the property ID
   * @param caption the caption
   * @return a new audit update time property
   */
  public static Property.AuditTimeProperty auditUpdateTimeProperty(final String propertyID, final String caption) {
    return new DefaultProperty.DefaultAuditTimeProperty(propertyID, Property.AuditProperty.AuditAction.UPDATE, caption);
  }

  /**
   * @param propertyID the property ID
   * @return a new audit insert user property
   */
  public static Property.AuditUserProperty auditInsertUserProperty(final String propertyID) {
    return auditInsertUserProperty(propertyID, null);
  }

  /**
   * @param propertyID the property ID
   * @param caption the caption
   * @return a new audit insert user property
   */
  public static Property.AuditUserProperty auditInsertUserProperty(final String propertyID, final String caption) {
    return new DefaultProperty.DefaultAuditUserProperty(propertyID, Property.AuditProperty.AuditAction.INSERT, caption);
  }

  /**
   * @param propertyID the property ID
   * @return a new audit update user property
   */
  public static Property.AuditUserProperty auditUpdateUserProperty(final String propertyID) {
    return auditUpdateUserProperty(propertyID, null);
  }

  /**
   * @param propertyID the property ID
   * @param caption the caption
   * @return a new audit update user property
   */
  public static Property.AuditUserProperty auditUpdateUserProperty(final String propertyID, final String caption) {
    return new DefaultProperty.DefaultAuditUserProperty(propertyID, Property.AuditProperty.AuditAction.UPDATE, caption);
  }

  /**
   * @param propertyID the property ID
   * @return a new mirror property
   */
  public static Property.MirrorProperty mirrorProperty(final String propertyID) {
    return new DefaultProperty.DefaultMirrorProperty(propertyID);
  }
  /**
   * @return a value converter which converts an underlying database representation
   * of a boolean value into an actual Boolean
   * @see org.jminor.framework.Configuration#SQL_BOOLEAN_VALUE_TRUE
   * @see org.jminor.framework.Configuration#SQL_BOOLEAN_VALUE_FALSE
   */
  public static Property.ColumnProperty.ValueConverter booleanValueConverter() {
    return new DefaultProperty.BooleanValueConverter();
  }

  /**
   * @param trueValue the value used to represent 'true' in the underlying database
   * @param falseValue the value used to represent 'false' in the underlying database
   * @return a value converter which converts an underlying database representation
   * of a boolean value into an actual Boolean
   */
  public static Property.ColumnProperty.ValueConverter booleanValueConverter(final Object trueValue, final Object falseValue) {
    return new DefaultProperty.BooleanValueConverter(trueValue, falseValue);
  }
}

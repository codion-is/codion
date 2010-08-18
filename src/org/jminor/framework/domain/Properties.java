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
    return new PropertyImpl.ColumnPropertyImpl(propertyID, type, caption);
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
    return new PropertyImpl.PrimaryKeyPropertyImpl(propertyID, type, caption);
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
    return new PropertyImpl.ForeignKeyPropertyImpl(propertyID, caption, referencedEntityID, referenceProperty);
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
    return new PropertyImpl.ForeignKeyPropertyImpl(propertyID, caption, referencedEntityID, referenceProperties, referencedPropertyIDs);
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
    return new PropertyImpl.DenormalizedViewPropertyImpl(propertyID, foreignKeyPropertyID, property, caption);
  }

  /**
   * @param propertyID the property ID
   * @param type the property type
   * @param caption the caption
   * @return a new derived property
   */
  public static Property.DerivedProperty derivedProperty(final String propertyID, final int type, final String caption) {
    return new PropertyImpl.DerivedPropertyImpl(propertyID, type, caption);
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
    return new PropertyImpl.DenormalizedPropertyImpl(propertyID, foreignKeyPropertyID, denormalizedProperty, caption);
  }

  /**
   * @param propertyID the property ID, since SubqueryProperties do not map to underlying table columns,
   * the property ID should not be column name, only be unique for this entity
   * @param type the data type of this property
   * @param caption the caption of this property
   * @param subquery the sql query
   * @return a new subquery property
   */
  public static Property.SubqueryProperty subqueryProperty(final String propertyID, final int type, final String caption, final String subquery) {
    return new PropertyImpl.SubqueryPropertyImpl(propertyID, type, caption, subquery);
  }

  /**
   * @param propertyID the property ID
   * @param type the data type of this property
   * @param caption the property caption
   * @param values the values to base this property on
   * @return a new value list property
   */
  public static Property.ValueListProperty valueListProperty(final String propertyID, final int type, final String caption,
                                                             final List<Item<Object>> values) {
    return new PropertyImpl.ValueListPropertyImpl(propertyID, type, caption, values);
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
    return new PropertyImpl.TransientPropertyImpl(propertyID, type, caption);
  }

  /**
   * @param propertyID the property ID, in case of database properties this should be the underlying column name
   * @param caption the caption of this property
   * @return a new boolean property
   */
  public static Property.BooleanProperty booleanProperty(final String propertyID, final String caption) {
    return new PropertyImpl.BooleanPropertyImpl(propertyID, caption);
  }

  /**
   * @param propertyID the property ID, in case of database properties this should be the underlying column name
   * @param columnType the data type of the underlying column
   * @param caption the caption of this property
   * @return a new boolean property
   */
  public static Property.BooleanProperty booleanProperty(final String propertyID, final int columnType, final String caption) {
    return new PropertyImpl.BooleanPropertyImpl(propertyID, columnType, caption);
  }

  /**
   * @param propertyID the property ID, in case of database properties this should be the underlying column name
   * @param columnType the data type of the underlying column
   * @param caption the caption of this property
   * @param trueValue the Object value representing 'true' in the underlying column
   * @param falseValue the Object value representing 'false' in the underlying column
   * @return a new boolean property
   */
  public static Property.BooleanProperty booleanProperty(final String propertyID, final int columnType, final String caption,
                                                         final Object trueValue, final Object falseValue) {
    return new PropertyImpl.BooleanPropertyImpl(propertyID, columnType, caption, trueValue, falseValue);
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
    return new PropertyImpl.AuditTimePropertyImpl(propertyID, Property.AuditProperty.AuditAction.INSERT, caption);
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
    return new PropertyImpl.AuditTimePropertyImpl(propertyID, Property.AuditProperty.AuditAction.UPDATE, caption);
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
    return new PropertyImpl.AuditUserPropertyImpl(propertyID, Property.AuditProperty.AuditAction.INSERT, caption);
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
    return new PropertyImpl.AuditUserPropertyImpl(propertyID, Property.AuditProperty.AuditAction.UPDATE, caption);
  }

  /**
   * @param propertyID the property ID
   * @return a new mirror property
   */
  public static Property.MirrorProperty mirrorProperty(final String propertyID) {
    return new PropertyImpl.MirrorPropertyImpl(propertyID);
  }
}

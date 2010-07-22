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

  public static Property.ColumnProperty columnProperty(final String propertyID) {
    return columnProperty(propertyID, Types.INTEGER);
  }

  public static Property.ColumnProperty columnProperty(final String propertyID, final int type) {
    return columnProperty(propertyID, type, null);
  }

  public static Property.ColumnProperty columnProperty(final String propertyID,final int type, final String caption) {
    return new PropertyImpl.ColumnPropertyImpl(propertyID, type, caption);
  }

  public static Property.PrimaryKeyProperty primaryKeyProperty(final String propertyID) {
    return primaryKeyProperty(propertyID, Types.INTEGER);
  }

  public static Property.PrimaryKeyProperty primaryKeyProperty(final String propertyID, final int type) {
    return primaryKeyProperty(propertyID, type, null);
  }

  public static Property.PrimaryKeyProperty primaryKeyProperty(final String propertyID, final int type, final String caption) {
    return new PropertyImpl.PrimaryKeyPropertyImpl(propertyID, type, caption);
  }

  public static Property.ForeignKeyProperty foreignKeyProperty(final String propertyID, final String caption,
                                                               final String referencedEntityID,
                                                               final Property.ColumnProperty referenceProperty) {
    return new PropertyImpl.ForeignKeyPropertyImpl(propertyID, caption, referencedEntityID, referenceProperty);
  }

  public static Property.ForeignKeyProperty foreignKeyProperty(final String propertyID, final String caption,
                                                               final String referencedEntityID,
                                                               final Property.ColumnProperty[] referenceProperties,
                                                               final String[] referencedPropertyIDs) {
    return new PropertyImpl.ForeignKeyPropertyImpl(propertyID, caption, referencedEntityID, referenceProperties, referencedPropertyIDs);
  }

  public static Property.DenormalizedViewProperty denormalizedViewProperty(final String propertyID, final String foreignKeyPropertyID,
                                                                           final Property property, final String caption) {
    return new PropertyImpl.DenormalizedViewPropertyImpl(propertyID, foreignKeyPropertyID, property, caption);
  }

  public static Property.DerivedProperty derivedProperty(final String propertyID, final int type, final String caption) {
    return new PropertyImpl.DerivedPropertyImpl(propertyID, type, caption);
  }

  public static Property denormalizedProperty(final String propertyID, final String foreignKeyPropertyID, final Property property) {
    return denormalizedProperty(propertyID, foreignKeyPropertyID, property, null);
  }

  public static Property denormalizedProperty(final String propertyID, final String foreignKeyPropertyID, final Property property, final String caption) {
    return new PropertyImpl.DenormalizedPropertyImpl(propertyID, foreignKeyPropertyID, property, caption);
  }

  public static Property.SubqueryProperty subqueryProperty(final String propertyID, final int type, final String caption, final String subquery) {
    return new PropertyImpl.SubqueryPropertyImpl(propertyID, type, caption, subquery);
  }

  public static Property.ValueListProperty valueListProperty(final String propertyID, final int type, final String caption,
                                                             final List<Item<Object>> values) {
    return new PropertyImpl.ValueListPropertyImpl(propertyID, type, caption, values);
  }

  public static Property.TransientProperty transientProperty(final String propertyID, final int type) {
    return transientProperty(propertyID, type, null);
  }

  public static Property.TransientProperty transientProperty(final String propertyID, final int type, final String caption) {
    return new PropertyImpl.TransientPropertyImpl(propertyID, type, caption);
  }

  public static Property.BooleanProperty booleanProperty(final String propertyID, final String caption) {
    return new PropertyImpl.BooleanPropertyImpl(propertyID, caption);
  }

  public static Property.BooleanProperty booleanProperty(final String propertyID, final int columnType, final String caption) {
    return new PropertyImpl.BooleanPropertyImpl(propertyID, columnType, caption);
  }

  public static Property.BooleanProperty booleanProperty(final String propertyID, final int columnType, final String caption,
                               final Object trueValue, final Object falseValue) {
    return new PropertyImpl.BooleanPropertyImpl(propertyID, columnType, caption, trueValue, falseValue);
  }

  public static Property.AuditTimeProperty auditInsertTimeProperty(final String propertyID, final String caption) {
    return new PropertyImpl.AuditTimePropertyImpl(propertyID, Property.AuditProperty.AuditAction.INSERT, caption);
  }

  public static Property.AuditTimeProperty auditUpdateTimeProperty(final String propertyID, final String caption) {
    return new PropertyImpl.AuditTimePropertyImpl(propertyID, Property.AuditProperty.AuditAction.UPDATE, caption);
  }

  public static Property.AuditUserProperty auditInsertUserProperty(final String propertyID, final String caption) {
    return new PropertyImpl.AuditUserPropertyImpl(propertyID, Property.AuditProperty.AuditAction.INSERT, caption);
  }

  public static Property.AuditUserProperty auditUpdateUserProperty(final String propertyID, final String caption) {
    return new PropertyImpl.AuditUserPropertyImpl(propertyID, Property.AuditProperty.AuditAction.UPDATE, caption);
  }

  public static Property.MirrorProperty mirrorProperty(final String propertyID) {
    return new PropertyImpl.MirrorPropertyImpl(propertyID);
  }
}

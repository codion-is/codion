/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import java.sql.Types;

/**
 * User: Björn Darri
 * Date: 18.7.2010
 * Time: 21:53:53
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

  public static Property denormalizedProperty(final String propertyID, final String foreignKeyPropertyID, final Property property, final String caption) {
    return new PropertyImpl.DenormalizedPropertyImpl(propertyID, foreignKeyPropertyID, property, caption);
  }

  public static Property.SubqueryProperty subqueryProperty(final String propertyID, final int type, final String caption, final String subquery) {
    return new PropertyImpl.SubqueryPropertyImpl(propertyID, type, caption, subquery);
  }
}

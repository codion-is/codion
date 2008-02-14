/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import java.awt.Color;

/**
 * Acts as a proxy for retrieving values from Entity objects, allowing for plugged
 * in entity specific functionality
 * @see EntityRepository#setDefaultEntityProxy
 * @see EntityRepository#addEntityProxy
 */
public class EntityProxy {

  public Object getValue(final Entity entity, final Property property) {
    final String propertyID = property.propertyID;
    if (property instanceof Property.DenormalizedViewProperty)
      throw new IllegalArgumentException("EntityProxy.getValue does not handle denormalized view properties (Property.DenormalizedViewProperty)");
    else if (property instanceof Property.PrimaryKeyProperty)
      return entity.getPrimaryKey().getValue(propertyID);
    else if (entity.hasValue(propertyID))
      return entity.getRawValue(propertyID);
    else
      return property.getDefaultValue();
  }

  public int compareTo(final Entity entity, final Entity entityToCompare) {
    return entity.toString().compareTo(entityToCompare.toString());
  }

  public String toString(final Entity entity) {
    return entity.getEntityID() + ": " + entity.getPrimaryKey().toString();
  }

  public String getValueAsUserString(final Entity entity, final Property property) {
    return entity.isValueNull(property.propertyID) ? "" : getValueAsString(entity, property);
  }

  public String getValueAsString(final Entity entity, final Property property) {
    return entity.isValueNull(property.propertyID) ? "" : getValue(entity, property).toString();
  }

  public Object getTableValue(final Entity entity, final Property property) {
    return getValue(entity, property);
  }

  @SuppressWarnings({"UnusedDeclaration"})
  public Color getBackgroundColor(final Entity entity) {
    return null;
  }
}

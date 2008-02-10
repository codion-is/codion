/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import java.awt.Color;
import java.sql.Timestamp;
import java.util.Date;

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
      return getDenormalizedValue(entity, (Property.DenormalizedViewProperty) property);
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

  public String getStringValue(final Entity entity, final Property property) {
    return (String) getValue(entity, property);
  }

  public String getStringValue(final Entity entity, final String propertyID) {
    return getStringValue(entity, EntityRepository.get().getProperty(entity.getEntityID(), propertyID));
  }

  public String getValueAsString(final Entity entity, final String propertyID) {
    return getValueAsString(entity, EntityRepository.get().getProperty(entity.getEntityID(), propertyID));
  }

  public Entity getEntityValue(final Entity entity, final Property property) {
    return (Entity) entity.getValue(property);
  }

  public Timestamp getDateValue(final Entity entity, final Property property) {
    final Object value = entity.getValue(property);
    if (value == null)
      return null;

    if (value.getClass().equals(Date.class))
      return new Timestamp(((Date)value).getTime());

    return (Timestamp) value;
  }

  public Integer getIntValue(final Entity entity, final Property property) {
    return (Integer) entity.getValue(property);
  }

  public Type.Boolean getBooleanValue(final Entity entity, final Property property) {
    final Type.Boolean value = (Type.Boolean) entity.getValue(property);

    return value == null ? Type.Boolean.NULL : value;
  }

  public char getCharValue(final Entity entity, final Property property) {
    return (Character) entity.getValue(property);
  }

  public Double getDoubleValue(final Entity entity, final Property property) {
    return (Double) entity.getValue(property);
  }

  public String getValueAsUserString(final Entity entity, final Property property) {
    return entity.isValueNull(property.propertyID) ? "" : getValueAsString(entity, property);
  }

  public String getValueAsString(final Entity entity, final Property property) {
    return entity.isValueNull(property.propertyID) ? "" : getValue(entity, property).toString();
  }

  public Object getTableValue(final Entity entity, final Property property) {
    return entity.isValueNull(property.propertyID) ? null : getValue(entity, property);
  }

  @SuppressWarnings({"UnusedDeclaration"})
  public Color getBackgroundColor(final Entity entity) {
    return null;
  }

  /**
   * Allows for overriding the null definition of an Entity,
   * by default an Entity is null if its primary key is null
   * @param entity the entity
   * @return true if the given Entity instance represents a null value
   */
  public boolean isNull(final Entity entity) {
    if (entity.getPrimaryKey() == null)
      throw new RuntimeException("Can only tell if entity is null if it has a primary key");

    return entity.getPrimaryKey().isNull();
  }

  private Object getDenormalizedValue(final Entity entity, final Property.DenormalizedViewProperty denormalizedViewProperty) {
    final Property.EntityProperty ownerProperty =
            EntityRepository.get().getEntityProperty(entity.getEntityID(), denormalizedViewProperty.ownerEntityID);
    final Entity valueOwner = entity.getEntityValue(ownerProperty.propertyID);

    return valueOwner != null ? valueOwner.getValue(denormalizedViewProperty.denormalizedPropertyName) : null;
  }
}

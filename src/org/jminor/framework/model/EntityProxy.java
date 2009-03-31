/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import java.awt.Color;
import java.util.HashMap;

/**
 * Acts as a proxy for retrieving values from Entity objects, allowing for plugged
 * in entity specific functionality, such as providing toString() and compareTo() implementations
 */
public class EntityProxy {

  private static HashMap<String, EntityProxy> entityProxies;
  private static EntityProxy defaultEntityProxy = new EntityProxy();

  /**
   * @param proxy sets the default EntityProxy instance used if no entity specific one is specified
   */
  public static void setDefaultEntityProxy(final EntityProxy proxy) {
    defaultEntityProxy = proxy;
  }

  public static void addEntityProxy(final String entityID, final EntityProxy entityProxy) {
    if (entityProxies == null)
      entityProxies = new HashMap<String, EntityProxy>();

    entityProxies.put(entityID, entityProxy);
  }

  public static EntityProxy getEntityProxy(final String entityID) {
    if (entityProxies != null && entityProxies.containsKey(entityID))
      return entityProxies.get(entityID);

    return defaultEntityProxy;
  }

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

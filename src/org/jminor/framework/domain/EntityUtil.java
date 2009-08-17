/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A static utility class
 */
public class EntityUtil {

  private EntityUtil() {}

  /**
   * @param entities the entities
   * @return a List of entities that have been modified
   */
  public static List<Entity> getModifiedEntities(final List<Entity> entities) {
    final List<Entity> ret = new ArrayList<Entity>();
    for (final Entity entity : entities)
      if (entity.isModified())
        ret.add(entity);

    return ret;
  }

  public static boolean activeDependencies(final Map<String, List<Entity>> entities) {
    for (final List<Entity> ents : entities.values())
      if (ents.size() > 0)
        return true;

    return false;
  }

  public static Map<EntityKey, Entity> hashByPrimaryKey(final List<Entity> entities) {
    final Map<EntityKey, Entity> ret = new HashMap<EntityKey, Entity>();
    for (final Entity entity : entities)
      ret.put(entity.getPrimaryKey(), entity);

    return ret;
  }

  /**
   * @param entities the entities
   * @return a List containing the primary keys of the given entities
   */
  public static List<EntityKey> getPrimaryKeys(final Collection<Entity> entities) {
    final List<EntityKey> ret = new ArrayList<EntityKey>(entities.size());
    for (final Entity entity : entities)
      ret.add(entity.getPrimaryKey());

    return ret;
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @return an array containing the values of the property with the given ID from the given entities,
   * null values are included
   */
  public static Object[] getPropertyValues(final String propertyID, final List<Entity> entities) {
    return getPropertyValues(propertyID, entities, true);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @param includeNullValues if true then null values are included
   * @return an array containing the values of the property with the given ID from the given entities
   */
  public static Object[] getPropertyValues(final String propertyID, final List<Entity> entities,
                                           final boolean includeNullValues) {
    final List<Object> ret = new ArrayList<Object>(entities.size());
    for (final Entity entity : entities) {
      if (includeNullValues)
        ret.add(entity.getValue(propertyID));
      else if (!entity.isValueNull(propertyID))
        ret.add(entity.getValue(propertyID));
    }

    return ret.toArray();
  }

  /**
   * Returns a Collection containing the distinct values of <code>propertyID</code> from the given entities
   * @param entities the entities from which to retrieve the values
   * @param propertyID the ID of the property for which to retrieve the values
   * @return a Collection containing the distinct property values
   */
  public static Collection<Object> getPropertyValues(final List<Entity> entities, final String propertyID) {
    final Set<Object> ret = new HashSet<Object>();
    for (final Entity entity : entities)
      ret.add(entity.getValue(propertyID));

    return ret;
  }

  /**
   * Sets the value of the property with ID <code>propertyID</code> to <code>value</code>
   * in the given entities
   * @param propertyID the ID of the property for which to set the value
   * @param value the value
   * @param entities the entities for which to set the value
   * @return the old values in the same order as the entities were recieved
   */
  public static Object[] setPropertyValue(final String propertyID, final Object value,
                                          final List<Entity> entities) {
    final Object[] oldValues = getPropertyValues(propertyID, entities);
    for (final Entity entity : entities)
      entity.setValue(propertyID, value);

    return oldValues;
  }

  public static Object[] setPropertyValue(final String propertyID, final Object[] values,
                                          final List<Entity> entities) {
    final Object[] oldValues = getPropertyValues(propertyID, entities);
    for (int i = 0; i < entities.size(); i++)
      entities.get(i).setValue(propertyID, values[i]);

    return oldValues;
  }

  /**
   * Returns a Map containing the given entities hashed by the value of the property with ID <code>propertyID</code>
   * @param entities the entities to map by property value
   * @param propertyID the ID of the property which value should be used for mapping
   * @return a Map of entities hashed by property value
   */
  public static Map<Object, List<Entity>> hashByPropertyValue(final List<Entity> entities, final String propertyID) {
    final Map<Object, List<Entity>> ret = new HashMap<Object, List<Entity>>(entities.size());
    for (final Entity entity : entities) {
      final Object key = entity.getValue(propertyID);
      if (ret.containsKey(key))
        ret.get(key).add(entity);
      else {
        final List<Entity> list = new ArrayList<Entity>();
        list.add(entity);
        ret.put(key, list);
      }
    }

    return ret;
  }

  /**
   * Returns a Map containing the given entities hashed by their entityIDs
   * @param entities the entities to map by entityID
   * @return a Map of entities hashed by entityID
   */
  public static Map<String, Collection<Entity>> hashByEntityID(final Collection<Entity> entities) {
    final Map<String, Collection<Entity>> ret = new HashMap<String, Collection<Entity>>(entities.size());
    for (final Entity entity : entities) {
      final String entityID = entity.getEntityID();
      if (ret.containsKey(entityID))
        ret.get(entityID).add(entity);
      else {
        final List<Entity> list = new ArrayList<Entity>();
        list.add(entity);
        ret.put(entityID, list);
      }
    }

    return ret;
  }
}

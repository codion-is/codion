/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.model.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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
    for (final Entity entity : entities) {
      if (entity.isModified())
        ret.add(entity);
    }

    return ret;
  }

  /**
   * Performes a basic data validation of <code>value</code>, checking if the
   * <code>value</code> data type is consistent with the data type of this
   * property, returns the value
   * @param value the value to validate
   * @param property the property
   * @return the value
   * @throws IllegalArgumentException when the value is not of the same type as the propertyValue
   */
  public static Object validateValue(final Property property, final Object value) throws IllegalArgumentException {
    final Type propertyType = property.propertyType;
    if (value == null)
      return value;

    final String propertyID = property.propertyID;
    switch (propertyType) {
      case INT : {
        if (!(value instanceof Integer))
          throw new IllegalArgumentException("Integer value expected for property: " + propertyID + " (" + value.getClass() + ")");
        return value;
      }
      case DOUBLE : {
        if (!(value instanceof Double))
          throw new IllegalArgumentException("Double value expected for property: " + propertyID + " (" + value.getClass() + ")");
        return value;
      }
      case BOOLEAN : {
        if (!(value instanceof Type.Boolean))
          throw new IllegalArgumentException("Boolean value expected for property: " + propertyID + " (" + value.getClass() + ")");
        return value;
      }
      case LONG_DATE :
      case SHORT_DATE : {
        if (!(value instanceof Date))
          throw new IllegalArgumentException("Date value expected for property: " + propertyID + " (" + value.getClass() + ")");
        return value;
      }
      case ENTITY : {
        if (!(value instanceof Entity) && !(value instanceof EntityKey))
          throw new IllegalArgumentException("Entity or EntityKey value expected for property: " + propertyID + "(" + value.getClass() + ")");
        return value;
      }
      case CHAR : {
        if (!(value instanceof Character))
          throw new IllegalArgumentException("Character value expected for property: " + propertyID + "(" + value.getClass() + ")");
        return value;
      }
      case STRING : {
        if (!(value instanceof String))
          throw new IllegalArgumentException("String value expected for propertyValue: " + propertyID + "(" + value.getClass() + ")");
        return value;
      }
    }

    throw new IllegalArgumentException("Unknown type " + propertyType);
  }

  public static Class getValueClass(final Type type, final Object value) {
    if (type == Type.INT)
      return Integer.class;
    if (type == Type.DOUBLE)
      return Double.class;
    if (type == Type.BOOLEAN)
      return Type.Boolean.class;
    if (type == Type.SHORT_DATE || type == Type.LONG_DATE)
      return Date.class;
    if (type == Type.CHAR)
      return Character.class;
    if (type == Type.ENTITY)
      return Entity.class;

    return value == null ? Object.class : value.getClass();
  }

  public static boolean activeDependencies(final Map<String, List<Entity>> entities) {
    for (final List<Entity> ents : entities.values()) {
      if (ents.size() > 0)
        return true;
    }

    return false;
  }

  public static boolean equal(final Type type, final Object one, final Object two) {
    final boolean oneNull = isValueNull(type, one);
    final boolean twoNull = isValueNull(type, two);

    return oneNull && twoNull || !(oneNull ^ twoNull) && one.equals(two);
  }

  public static HashMap<EntityKey, Entity> hashByPrimaryKey(final List<Entity> entities) {
    final HashMap<EntityKey, Entity> ret = new HashMap<EntityKey, Entity>();
    for (final Entity entity : entities)
      ret.put(entity.getPrimaryKey(), entity);

    return ret;
  }

  public static String getValueString(final Property property, final Object value) {
    final boolean valueIsNull = isValueNull(property.propertyType, value);
    final StringBuffer ret = new StringBuffer("[").append(valueIsNull ? (value == null ? "null" : "null value") : value).append("]");
    if (value instanceof Entity)
      ret.append(" PK{").append(((Entity)value).getPrimaryKey()).append("}");

    return ret.toString();
  }

  /**
   * Returns true if <code>value</code> represents a null value for the given property type
   * @param propertyType the property type
   * @param value the value to check
   * @return true if <code>value</code> represents null
   */
  public static boolean isValueNull(final Type propertyType, final Object value) {
    if (value == null)
      return true;

    switch (propertyType) {
      case CHAR :
        if (value instanceof String)
          return ((String)value).length() == 0;
      case BOOLEAN :
        return value == Type.Boolean.NULL;
      case STRING :
        return value.equals("");
      case ENTITY :
        return value instanceof Entity ? ((Entity) value).isNull() : ((EntityKey) value).isNull();
      default :
        return false;
    }
  }

  /**
   * Returns a copy of the given value.
   * If the value is an entity it is deep copied.
   * @param value the value to copy
   * @return a copy of <code>value</code>
   */
  public static Object copyPropertyValue(final Object value) {
    return value instanceof Entity ? ((Entity)value).getCopy() : value;
  }

  public static String getPropertyChangeDebugString(final String entityID, final Property property,
                                                    final Object oldValue, final Object newValue,
                                                    final boolean isInitialization) {
    final StringBuffer ret = new StringBuffer();
    if (isInitialization)
      ret.append("INIT ");
    else
      ret.append("SET").append(Util.equal(oldValue, newValue) ? " == " : " <> ");
    ret.append(entityID).append(" -> ").append(property).append("; ");
    if (!isInitialization) {
      if (oldValue != null)
        ret.append(oldValue.getClass().getSimpleName()).append(" ");
      ret.append(getValueString(property, oldValue));
    }
    if (!isInitialization)
      ret.append(" : ");
    if (newValue != null)
      ret.append(newValue.getClass().getSimpleName()).append(" ");
    ret.append(getValueString(property, newValue));

    return ret.toString();
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
  public static Object[] getPropertyValue(final String propertyID, final List<Entity> entities) {
    return getPropertyValue(propertyID, entities, true);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @param includeNullValues if true then null values are included
   * @return an array containing the values of the property with the given ID from the given entities
   */
  public static Object[] getPropertyValue(final String propertyID, final List<Entity> entities,
                                          final boolean includeNullValues) {
    final List<Object> ret = new ArrayList<Object>(entities.size());
    for (Entity entity : entities) {
      if (includeNullValues)
        ret.add(entity.getValue(propertyID));
      else if (!entity.isValueNull(propertyID))
        ret.add(entity.getValue(propertyID));
    }

    return ret.toArray();
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
    final Object[] oldValues = getPropertyValue(propertyID, entities);
    for (final Entity entity : entities)
      entity.setValue(propertyID, value);

    return oldValues;
  }

  public static Object[] setPropertyValue(final String propertyID, final Object[] values,
                                          final List<Entity> entities) {
    final Object[] oldValues = getPropertyValue(propertyID, entities);
    for (int i = 0; i < entities.size(); i++)
      entities.get(i).setValue(propertyID, values[i]);

    return oldValues;
  }

  public static void printPropertyValues(final Entity entity) {
    final Collection<Property> properties = EntityRepository.get().getProperties(entity.getEntityID(), true);
    System.out.println("*********************[" + entity + "]***********************");
    for (final Property property : properties) {
      final Object value = entity.getValue(property.propertyID);
      System.out.println(property + " = " + getValueString(property, value));
    }
    System.out.println("********************************************");
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
}

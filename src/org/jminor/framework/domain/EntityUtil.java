/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.db.Database;
import org.jminor.common.db.IdSource;
import org.jminor.framework.Configuration;

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

  public static Map<Entity.Key, Entity> hashByPrimaryKey(final List<Entity> entities) {
    final Map<Entity.Key, Entity> ret = new HashMap<Entity.Key, Entity>();
    for (final Entity entity : entities)
      ret.put(entity.getPrimaryKey(), entity);

    return ret;
  }

  /**
   * @param entities the entities
   * @return a List containing the primary keys of the given entities
   */
  public static List<Entity.Key> getPrimaryKeys(final Collection<Entity> entities) {
    final List<Entity.Key> ret = new ArrayList<Entity.Key>(entities.size());
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

  /**
   * Returns a SQL string version of the given value
   * @param property the property
   * @param value the value
   * @return a SQL string version of value
   */
  public static String getSQLStringValue(final Property property, final Object value) {
    if (Entity.isValueNull(property.getPropertyType(), value))
      return "null";

    switch (property.getPropertyType()) {
      case INT:
      case DOUBLE:
        return value.toString();//localize?
      case TIMESTAMP:
      case DATE:
        if (!(value instanceof Date))
          throw new IllegalArgumentException("Date value expected for property: " + property + ", got: " + value.getClass());
        return Database.get().getSQLDateString((Date) value, property.getPropertyType() == Type.TIMESTAMP);
      case CHAR:
        return "'" + value + "'";
      case STRING:
        if (!(value instanceof String))
          throw new IllegalArgumentException("String value expected for property: " + property + ", got: " + value.getClass());
        return "'" + sqlEscapeString((String) value) + "'";
      case BOOLEAN:
        if (!(value instanceof Boolean))
          throw new IllegalArgumentException("Boolean value expected for property: " + property + ", got: " + value.getClass());
        return getBooleanSQLString(property, (Boolean) value);
      case ENTITY:
        return value instanceof Entity ? getSQLStringValue(property, ((Entity)value).getPrimaryKey().getFirstKeyValue())
                : getSQLStringValue(((Entity.Key)value).getFirstKeyProperty(), ((Entity.Key)value).getFirstKeyValue());
      default:
        throw new IllegalArgumentException("Undefined property type: " + property.getPropertyType());
    }
  }

  public static String sqlEscapeString(final String val) {
    return val.replaceAll("'", "''");
  }

  public static String getBooleanSQLString(final Property property, final Boolean value) {
    if (property instanceof Property.BooleanProperty)
      return ((Property.BooleanProperty) property).toSQLString(value);
    else {
      if (value == null)
        return Configuration.getValue(Configuration.SQL_BOOLEAN_VALUE_NULL) + "";
      else if (value)
        return Configuration.getValue(Configuration.SQL_BOOLEAN_VALUE_TRUE) + "";
      else
        return Configuration.getValue(Configuration.SQL_BOOLEAN_VALUE_FALSE) + "";
    }
  }

  /**
   * Constructs a where condition based on the given primary key
   * @param entityKey the EntityKey instance
   * @return a where clause using this EntityKey instance,
   * e.g. " where (idCol = 42)" or in case of multi column key " where (idCol1 = 42) and (idCol2 = 24)"
   */
  public static String getWhereCondition(final Entity.Key entityKey) {
    return getWhereCondition(entityKey.getProperties(), new ValueProvider() {
      public Object getValue(final String propertyID) {
        return entityKey.getValue(propertyID);
      }
    });
  }

  /**
   * Constructs a where condition based on the primary key of the given entity, using the
   * original property values. This method should be used when updating an entity in case
   * a primary key property value has changed, hence using the original value.
   * @param entity the Entity instance
   * @return a where clause specifying this entity instance,
   * e.g. " where (idCol = 42)" or in case of multi column key " where (idCol1 = 42) and (idCol2 = 24)"
   */
  public static String getWhereCondition(final Entity entity) {
    return getWhereCondition(entity.getPrimaryKey().getProperties(), new ValueProvider() {
      public Object getValue(final String propertyID) {
        return entity.getOriginalValue(propertyID);
      }
    });
  }

  /**
   * Constructs a where condition based on the given primary key properties and the values provide by <code>valueProvider</code>
   * @param properties the properties to use when constructing the condition
   * @param valueProvider the value provider
   * @return a where clause according to the given properties and the values provided by <code>valueProvider</code>,
   * e.g. " where (idCol = 42)" or in case of multiple properties " where (idCol1 = 42) and (idCol2 = 24)"
   */
  public static String getWhereCondition(final List<Property.PrimaryKeyProperty> properties, final ValueProvider valueProvider) {
    final StringBuilder ret = new StringBuilder(" where (");
    int i = 0;
    for (final Property.PrimaryKeyProperty property : properties) {
      ret.append(getQueryString(property.getPropertyID(), getSQLStringValue(property, valueProvider.getValue(property.getPropertyID()))));
      if (i++ < properties.size() - 1)
        ret.append(" and ");
    }

    return ret.append(")").toString();
  }

  /**
   * @param columnName the columnName
   * @param sqlStringValue the sql string value
   * @return a query comparison string, e.g. "columnName = sqlStringValue"
   * or "columnName is null" in case sqlStringValue is 'null'
   */
  public static String getQueryString(final String columnName, final String sqlStringValue) {
    return new StringBuilder(columnName).append(sqlStringValue.toUpperCase().equals("NULL") ?
            " is " : " = ").append(sqlStringValue).toString();
  }

  /**
   * Returns the insert properties for this entity, leaving out properties with null values
   * @param entity the entity
   * @return the properties used to insert the given entity type
   */
  public static List<Property> getInsertProperties(final Entity entity) {
    final List<Property> ret = new ArrayList<Property>();
    for (final Property property : EntityRepository.getDatabaseProperties(entity.getEntityID(),
            EntityRepository.getIdSource(entity.getEntityID()) != IdSource.AUTO_INCREMENT, false, true)) {
      if (!(property instanceof Property.ForeignKeyProperty) && !entity.isValueNull(property.getPropertyID()))
        ret.add(property);
    }

    return ret;
  }

  /**
   * @param entity the Entity instance
   * @return the properties used to update this entity, modified properties that is
   */
  public static Collection<Property> getUpdateProperties(final Entity entity) {
    final List<Property> ret = new ArrayList<Property>();
    for (final Property property : EntityRepository.getDatabaseProperties(entity.getEntityID(), true, false, false))
      if (entity.isModified(property.getPropertyID()) && !(property instanceof Property.ForeignKeyProperty))
        ret.add(property);

    return ret;
  }

  public interface ValueProvider {
    public Object getValue(final String propertyID);
  }
}

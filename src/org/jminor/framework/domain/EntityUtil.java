/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.db.IdSource;
import org.jminor.common.db.dbms.Database;
import org.jminor.framework.Configuration;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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

  private static final SimpleDateFormat jsonDateFormat = new SimpleDateFormat("yyyy-MM-dd");
  private static final SimpleDateFormat jsonTimestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

  private EntityUtil() {}

  public static List<Entity> parseJSONString(final String jsonString) throws JSONException, ParseException {
    if (jsonString == null || jsonString.length() == 0)
      return new ArrayList<Entity>(0);

    final JSONObject jsonObject = new JSONObject(jsonString);
    final List<Entity> entities = new ArrayList<Entity>();
    for (int i = 0; i < jsonObject.names().length(); i++) {
      final JSONObject entityObject = jsonObject.getJSONObject(jsonObject.names().get(i).toString());
      final Map<String, Object> propertyValueMap = new HashMap<String, Object>();
      Map<String, Object> originalValueMap = null;
      final String entityID = entityObject.getString("entityID");
      if (!EntityRepository.isDefined(entityID))
        throw new RuntimeException("Undifined entity type found in JSON file: '" + entityID + "'");

      final JSONObject propertyValues = entityObject.getJSONObject("propertyValues");
      for (int j = 0; j < propertyValues.names().length(); j++) {
        final String propertyID = propertyValues.names().get(j).toString();
        propertyValueMap.put(propertyID, parseJSONValue(entityID, propertyID, propertyValues));
      }
      if (!entityObject.isNull("originalValues")) {
        originalValueMap = new HashMap<String, Object>();
        final JSONObject originalValues = entityObject.getJSONObject("originalValues");
        for (int j = 0; j < originalValues.names().length(); j++) {
          final String propertyID = originalValues.names().get(j).toString();
          originalValueMap.put(propertyID, parseJSONValue(entityID, propertyID, originalValues));
        }
      }
      entities.add(Entity.initializeEntity(entityID, propertyValueMap, originalValueMap));
    }

    return entities;
  }

  public static String getJSONString(final Collection<Entity> entities) throws JSONException {
    return getJSONString(entities, 0);
  }

  public static String getJSONString(final Collection<Entity> entities, final int indentFactor) throws JSONException {
    return indentFactor > 0 ? getJSONEntity(entities).toString(indentFactor) : getJSONEntity(entities).toString();
  }

  public static JSONObject getJSONEntity(final Collection<Entity> entities) throws JSONException {
    final JSONObject jsonEntities = new JSONObject();
    for (final Entity entity : entities)
      jsonEntities.put(entity.getEntityID() + " PK[" + entity.getPrimaryKey() + "]", toJSONObject(entity));

    return jsonEntities;
  }

  /**
   * @param entities the entities
   * @return a List of entities that have been modified
   */
  public static List<Entity> getModifiedEntities(final Collection<Entity> entities) {
    final List<Entity> modifiedEntities = new ArrayList<Entity>();
    for (final Entity entity : entities)
      if (entity.isModified())
        modifiedEntities.add(entity);

    return modifiedEntities;
  }

  public static Map<Entity.Key, Entity> hashByPrimaryKey(final List<Entity> entities) {
    final Map<Entity.Key, Entity> entityMap = new HashMap<Entity.Key, Entity>();
    for (final Entity entity : entities)
      entityMap.put(entity.getPrimaryKey(), entity);

    return entityMap;
  }

  /**
   * @param entities the entities
   * @return a List containing the primary keys of the given entities
   */
  public static List<Entity.Key> getPrimaryKeys(final Collection<Entity> entities) {
    final List<Entity.Key> keys = new ArrayList<Entity.Key>(entities.size());
    for (final Entity entity : entities)
      keys.add(entity.getPrimaryKey());

    return keys;
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @return a List containing the values of the property with the given ID from the given entities,
   * null values are included
   */
  public static List<Object> getPropertyValues(final String propertyID, final List<Entity> entities) {
    return getPropertyValues(propertyID, entities, true);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @param includeNullValues if true then null values are included
   * @return a List containing the values of the property with the given ID from the given entities
   */
  public static List<Object> getPropertyValues(final String propertyID, final List<Entity> entities,
                                               final boolean includeNullValues) {
    final List<Object> values = new ArrayList<Object>(entities.size());
    for (final Entity entity : entities) {
      if (includeNullValues)
        values.add(entity.getValue(propertyID));
      else if (!entity.isValueNull(propertyID))
        values.add(entity.getValue(propertyID));
    }

    return values;
  }

  /**
   * Returns a Collection containing the distinct values of <code>propertyID</code> from the given entities.
   * If the <code>entities</code> list is null an empty Collection is returned.
   * @param entities the entities from which to retrieve the values
   * @param propertyID the ID of the property for which to retrieve the values
   * @return a Collection containing the distinct property values
   */
  public static Collection<Object> getDistinctPropertyValues(final List<Entity> entities, final String propertyID) {
    final Set<Object> values = new HashSet<Object>();
    if (entities == null)
      return values;
    for (final Entity entity : entities)
      values.add(entity.getValue(propertyID));

    return values;
  }

  /**
   * Sets the value of the property with ID <code>propertyID</code> to <code>value</code>
   * in the given entities
   * @param propertyID the ID of the property for which to set the value
   * @param value the value
   * @param entities the entities for which to set the value
   * @return the old property values mapped to their respective primary key
   */
  public static Map<Entity.Key, Object> setPropertyValue(final String propertyID, final Object value,
                                                         final List<Entity> entities) {
    final Map<Entity.Key, Object> oldValues = new HashMap<Entity.Key, Object>(entities.size());
    for (final Entity entity : entities)
      oldValues.put(entity.getPrimaryKey(), entity.setValue(propertyID, value));

    return oldValues;
  }

  /**
   * Returns a Map containing the given entities hashed by the value of the property with ID <code>propertyID</code>
   * @param entities the entities to map by property value
   * @param propertyID the ID of the property which value should be used for mapping
   * @return a Map of entities hashed by property value
   */
  public static Map<Object, Collection<Entity>> hashByPropertyValue(final List<Entity> entities, final String propertyID) {
    final Map<Object, Collection<Entity>> entityMap = new HashMap<Object, Collection<Entity>>(entities.size());
    for (final Entity entity : entities) {
      final Object key = entity.getValue(propertyID);
      if (entityMap.containsKey(key))
        entityMap.get(key).add(entity);
      else {
        final Collection<Entity> list = new ArrayList<Entity>();
        list.add(entity);
        entityMap.put(key, list);
      }
    }

    return entityMap;
  }

  /**
   * Returns a Map containing the given entities hashed by their entityIDs
   * @param entities the entities to map by entityID
   * @return a Map of entities hashed by entityID
   */
  public static Map<String, Collection<Entity>> hashByEntityID(final Collection<Entity> entities) {
    final Map<String, Collection<Entity>> entityMap = new HashMap<String, Collection<Entity>>(entities.size());
    for (final Entity entity : entities) {
      final String entityID = entity.getEntityID();
      if (entityMap.containsKey(entityID))
        entityMap.get(entityID).add(entity);
      else {
        final Collection<Entity> list = new ArrayList<Entity>();
        list.add(entity);
        entityMap.put(entityID, list);
      }
    }

    return entityMap;
  }

  /**
   * Returns a SQL string version of the given value
   * @param database the Database instance
   * @param property the property
   * @param value the value
   * @return a SQL string version of value
   */
  public static String getSQLStringValue(final Database database, final Property property, final Object value) {
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
        return database.getSQLDateString((Date) value, property.getPropertyType() == Type.TIMESTAMP);
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
        return value instanceof Entity ? getSQLStringValue(database, property, ((Entity)value).getPrimaryKey().getFirstKeyValue())
                : getSQLStringValue(database, ((Entity.Key)value).getFirstKeyProperty(), ((Entity.Key)value).getFirstKeyValue());
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
   * @param database the Database instance
   * @param entityKey the EntityKey instance
   * @return a where clause using this EntityKey instance, without the 'where' keyword
   * e.g. "(idCol = 42)" or in case of multiple column key "(idCol1 = 42) and (idCol2 = 24)"
   */
  public static String getWhereCondition(final Database database, final Entity.Key entityKey) {
    return getWhereCondition(database, entityKey.getProperties(), new ValueProvider() {
      public Object getValue(final String propertyID) {
        return entityKey.getValue(propertyID);
      }
    });
  }

  /**
   * Constructs a where condition based on the primary key of the given entity, using the
   * original property values. This method should be used when updating an entity in case
   * a primary key property value has changed, hence using the original value.
   * @param database the Database instance
   * @param entity the Entity instance
   * @return a where clause specifying this entity instance, without the 'where' keyword
   * e.g. "(idCol = 42)" or in case of multiple column key "(idCol1 = 42) and (idCol2 = 24)"
   */
  public static String getWhereCondition(final Database database, final Entity entity) {
    return getWhereCondition(database, entity.getPrimaryKey().getProperties(), new ValueProvider() {
      public Object getValue(final String propertyID) {
        return entity.getOriginalValue(propertyID);
      }
    });
  }

  /**
   * Constructs a where condition based on the given primary key properties and the values provide by <code>valueProvider</code>
   * @param database the Database instance
   * @param properties the properties to use when constructing the condition
   * @param valueProvider the value provider
   * @return a where clause according to the given properties and the values provided by <code>valueProvider</code>,
   * without the 'where' keyword
   * e.g. "(idCol = 42)" or in case of multiple properties "(idCol1 = 42) and (idCol2 = 24)"
   */
  public static String getWhereCondition(final Database database, final List<Property.PrimaryKeyProperty> properties,
                                         final ValueProvider valueProvider) {
    final StringBuilder stringBuilder = new StringBuilder("(");
    int i = 0;
    for (final Property.PrimaryKeyProperty property : properties) {
      stringBuilder.append(getQueryString(property.getPropertyID(),
              getSQLStringValue(database, property, valueProvider.getValue(property.getPropertyID()))));
      if (i++ < properties.size() - 1)
        stringBuilder.append(" and ");
    }

    return stringBuilder.append(")").toString();
  }

  /**
   * @param columnName the columnName
   * @param sqlStringValue the sql string value
   * @return a query comparison string, e.g. "columnName = sqlStringValue"
   * or "columnName is null" in case sqlStringValue is 'null'
   */
  public static String getQueryString(final String columnName, final String sqlStringValue) {
    return new StringBuilder(columnName).append(sqlStringValue.equalsIgnoreCase("null") ?
            " is " : " = ").append(sqlStringValue).toString();
  }

  /**
   * Returns the properties used when inserting an instance of this entity, leaving out properties with null values
   * @param entity the entity
   * @return the properties used to insert the given entity type
   */
  public static Collection<Property> getInsertProperties(final Entity entity) {
    final Collection<Property> properties = new ArrayList<Property>();
    for (final Property property : EntityRepository.getDatabaseProperties(entity.getEntityID(),
            EntityRepository.getIdSource(entity.getEntityID()) != IdSource.AUTO_INCREMENT, false, true)) {
      if (!(property instanceof Property.ForeignKeyProperty) && !entity.isValueNull(property.getPropertyID()))
        properties.add(property);
    }

    return properties;
  }

  /**
   * @param entity the Entity instance
   * @return the properties used to update this entity, modified properties that is
   */
  public static Collection<Property> getUpdateProperties(final Entity entity) {
    final List<Property> properties = new ArrayList<Property>();
    for (final Property property : EntityRepository.getDatabaseProperties(entity.getEntityID(), true, false, false))
      if (entity.isModified(property.getPropertyID()) && !(property instanceof Property.ForeignKeyProperty))
        properties.add(property);

    return properties;
  }

  public static List<Entity> copyEntities(final List<Entity> entities) {
    final List<Entity> copies = new ArrayList<Entity>(entities.size());
    for (final Entity entity : entities)
      copies.add(entity.getCopy());

    return copies;
  }

  private static Object parseJSONValue(final String entityID, final String propertyID, final JSONObject propertyValues) throws JSONException, ParseException {
    final Property property = EntityRepository.getProperty(entityID, propertyID);
    if (propertyValues.isNull(propertyID))
      return null;

    switch (property.getPropertyType()) {
      case ENTITY:
        return parseJSONString(propertyValues.getString(propertyID)).get(0);
      case STRING:
        return propertyValues.getString(propertyID);
      case BOOLEAN:
        return propertyValues.getBoolean(propertyID);
      case DATE:
        return jsonDateFormat.parse(propertyValues.getString(propertyID));
      case TIMESTAMP:
        return jsonTimestampFormat.parse(propertyValues.getString(propertyID));
      case DOUBLE:
        return propertyValues.getDouble(propertyID);
      case INT:
        return propertyValues.getInt(propertyID);
    }

    return propertyValues.getString(propertyID);
  }

  private static JSONObject toJSONObject(final Entity entity) throws JSONException {
    final JSONObject jsonEntity = new JSONObject();
    jsonEntity.put("entityID", entity.getEntityID());
    jsonEntity.put("propertyValues", getPropertyValuesJSONObject(entity));
    if (entity.isModified())
      jsonEntity.put("originalValues", getOriginalValuesJSONObject(entity));

    return jsonEntity;
  }

  private static JSONObject getPropertyValuesJSONObject(final Entity entity) throws JSONException {
    final JSONObject propertyValues = new JSONObject();
    for (final Property property : EntityRepository.getDatabaseProperties(entity.getEntityID(), true, true, true))
      propertyValues.put(property.getPropertyID(), getJSONValue(entity, property));

    return propertyValues;
  }

  private static Object getJSONValue(final Entity entity, final Property property) throws JSONException {
    if (entity.isValueNull(property.getPropertyID()))
      return JSONObject.NULL;
    if (property instanceof Property.ForeignKeyProperty)
      return getJSONEntity(Arrays.asList(entity.getEntityValue(property.getPropertyID())));
    if (property.getPropertyType() == Type.DATE || property.getPropertyType() == Type.TIMESTAMP)
      return entity.getFormattedDate(property.getPropertyID(), property.getPropertyType() == Type.DATE ? jsonDateFormat : jsonTimestampFormat);

    return entity.getValue(property.getPropertyID());
  }

  private static JSONObject getOriginalValuesJSONObject(final Entity entity) throws JSONException {
    final JSONObject originalValues = new JSONObject();
    for (final Property property : EntityRepository.getDatabaseProperties(entity.getEntityID(), true, true, true)) {
      if (entity.isModified(property.getPropertyID()))
        originalValues.put(property.getPropertyID(), getJSONOriginalValue(entity, property));
    }

    return originalValues;
  }

  private static Object getJSONOriginalValue(final Entity entity, final Property property) throws JSONException {
    if (Entity.isValueNull(property.getPropertyType(), entity.getOriginalValue(property.getPropertyID())))
      return JSONObject.NULL;
    if (property instanceof Property.ForeignKeyProperty)
      return getJSONEntity(Arrays.asList((Entity) entity.getOriginalValue(property.getPropertyID())));
    if (property.getPropertyType() == Type.DATE || property.getPropertyType() == Type.TIMESTAMP) {
      final Date date = (Date) entity.getOriginalValue(property.getPropertyID());
      return property.getPropertyType() == Type.DATE ? jsonDateFormat.format(date) : jsonTimestampFormat.format(date);
    }

    return entity.getOriginalValue(property.getPropertyID());
  }

  private interface ValueProvider {
    public Object getValue(final String propertyID);
  }
}

/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.Util;
import org.jminor.common.model.ValueProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
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
import java.util.Random;
import java.util.Set;

/**
 * A static utility class.
 */
public class EntityUtil {

  private static final SimpleDateFormat jsonDateFormat = new SimpleDateFormat("yyyy-MM-dd");
  private static final SimpleDateFormat jsonTimestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
  public static final Random random = new Random();

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
      entities.add(Entity.initialize(entityID, propertyValueMap, originalValueMap));
    }

    return entities;
  }

  public static String getJSONString(final Collection<Entity> entities) throws JSONException {
    return getJSONString(entities, 0);
  }

  public static String getJSONString(final Collection<Entity> entities, final int indentFactor) throws JSONException {
    return indentFactor > 0 ? getJSONObject(entities).toString(indentFactor) : getJSONObject(entities).toString();
  }

  public static JSONObject getJSONObject(final Collection<Entity> entities) throws JSONException {
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
      return getJSONObject(Arrays.asList(entity.getEntityValue(property.getPropertyID())));
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
      return getJSONObject(Arrays.asList((Entity) entity.getOriginalValue(property.getPropertyID())));
    if (property.getPropertyType() == Type.DATE || property.getPropertyType() == Type.TIMESTAMP) {
      final Date date = (Date) entity.getOriginalValue(property.getPropertyID());
      return property.getPropertyType() == Type.DATE ? jsonDateFormat.format(date) : jsonTimestampFormat.format(date);
    }

    return entity.getOriginalValue(property.getPropertyID());
  }

  public static Entity createRandomEntity(final String entityID, final Map<String, Entity> referenceEntities) {
    return createEntity(entityID, new ValueProvider<Property, Object>() {
      public Object getValue(final Property key) {
        return getRandomValue(key, referenceEntities);
      }
    });
  }

  public static Entity createEntity(final String entityID, final ValueProvider<Property, Object> valueProvider) {
    final Entity entity = new Entity(entityID);
    final boolean autoGenID = EntityRepository.isPrimaryKeyAutoGenerated(entityID);
    for (final Property property : EntityRepository.getDatabaseProperties(entityID, !autoGenID, false, true)) {
      if (!property.hasParentProperty())
        entity.setValue(property, valueProvider.getValue(property));
    }

    return entity;
  }

  public static Object getRandomValue(final Property property, final Map<String, Entity> referenceEntities) {
    if (property instanceof Property.ForeignKeyProperty) {
      final String referenceEntityID = ((Property.ForeignKeyProperty) property).getReferencedEntityID();
      return referenceEntities == null ? null : referenceEntities.get(referenceEntityID);
    }
    else if (!property.hasParentProperty()) {
      switch (property.getPropertyType()) {
        case BOOLEAN:
          return random.nextBoolean();
        case CHAR:
          return (char) random.nextInt();
        case DATE:
          return new Date();
        case DOUBLE:
          double min = property.getMin() == null ? -10000000 : property.getMin();
          double max = property.getMax() == null ? 10000000 : property.getMax();
          //Min + (int)(Math.random() * ((Max - Min) + 1))
          return min + (random.nextDouble() * ((max - min) + 1));
        case INT: {
          min = property.getMin() == null ? -10000000 : property.getMin();
          max = property.getMax() == null ? 10000000 : property.getMax();

          return (int) (min + (random.nextDouble() * ((max - min) + 1)));
        }
        case STRING:
          return Util.createRandomString(1, property.getMaxLength());
        case TIMESTAMP:
          return new Timestamp(System.currentTimeMillis());
      }
    }

    return null;
  }
}

/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.DateUtil;
import org.jminor.common.model.Item;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.ValueProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.text.Collator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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
    return getJSONString(entities, false);
  }

  public static String getJSONString(final Collection<Entity> entities, final boolean includeForeignKeys) throws JSONException {
    return getJSONString(entities, includeForeignKeys, 0);
  }

  public static String getJSONString(final Collection<Entity> entities, final boolean includeForeignKeys,
                                     final int indentFactor) throws JSONException {
    return indentFactor > 0 ? getJSONObject(entities, includeForeignKeys).toString(indentFactor) :
            getJSONObject(entities, includeForeignKeys).toString();
  }

  public static JSONObject getJSONObject(final Collection<Entity> entities, final boolean includeForeignKeys) throws JSONException {
    final JSONObject jsonEntities = new JSONObject();
    for (final Entity entity : entities)
      jsonEntities.put(entity.getEntityID() + " PK[" + entity.getPrimaryKey() + "]", toJSONObject(entity, includeForeignKeys));

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

  public static List<Object> getOriginalPropertyValues(final List<Entity.Key> keys) {
    final List<Object> list = new ArrayList<Object>(keys.size());
    for (final Entity.Key key : keys)
      list.add(key.getOriginalValue(key.getFirstKeyProperty().getPropertyID()));

    return list;
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
    if (entities == null || entities.size() == 0)
      return new ArrayList<Object>(0);

    return getPropertyValues(entities.get(0).getProperty(propertyID), entities, includeNullValues);
  }

  /**
   * @param property the the property for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @return a List containing the values of the property with the given ID from the given entities,
   * null values are included
   */
  public static List<Object> getPropertyValues(final Property property, final List<Entity> entities) {
    return getPropertyValues(property, entities, true);
  }

  /**
   * @param property the the property for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @param includeNullValues if true then null values are included
   * @return a List containing the values of the property with the given ID from the given entities
   */
  public static List<Object> getPropertyValues(final Property property, final List<Entity> entities,
                                               final boolean includeNullValues) {
    final List<Object> values = new ArrayList<Object>(entities.size());
    for (final Entity entity : entities) {
      if (includeNullValues)
        values.add(entity.getValue(property));
      else if (!entity.isValueNull(property))
        values.add(entity.getValue(property));
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
   * Returns a Map containing the given entity keys hashed by their entityIDs
   * @param keys the entity keys to map by entityID
   * @return a Map of entity keys hashed by entityID
   */
  public static Map<String, Collection<Entity.Key>> hashKeysByEntityID(final Collection<Entity.Key> keys) {
    final Map<String, Collection<Entity.Key>> entityMap = new HashMap<String, Collection<Entity.Key>>(keys.size());
    for (final Entity.Key key : keys) {
      final String entityID = key.getEntityID();
      if (entityMap.containsKey(entityID))
        entityMap.get(entityID).add(key);
      else {
        final Collection<Entity.Key> list = new ArrayList<Entity.Key>();
        list.add(key);
        entityMap.put(entityID, list);
      }
    }

    return entityMap;
  }

  public static List<Property> getProperties(final String entityID, final Collection<String> propertyIDs) {
    if (propertyIDs == null || propertyIDs.size() == 0)
      return new ArrayList<Property>(0);

    final List<Property> properties = new ArrayList<Property>(propertyIDs.size());
    for (final String propertyID : propertyIDs)
      properties.add(EntityRepository.getProperty(entityID, propertyID));

    return properties;
  }

  public static List<Property> getSortedProperties(final String entityID, final Collection<String> propertyIDs) {
    return sort(getProperties(entityID, propertyIDs));
  }

  public static List<Property> sort(final List<Property> properties) {
    final Collator collator = Collator.getInstance();
    Collections.sort(properties, new Comparator<Property>() {
      public int compare(final Property propertyOne, final Property propertyTwo) {
        return collator.compare(propertyOne.toString(), propertyTwo.toString());
      }
    });

    return properties;
  }

  public static List<Property> getUpdateProperties(final String entityID) {
    final List<Property> properties = EntityRepository.getDatabaseProperties(entityID, true, false, false);
    final ListIterator<Property> iterator = properties.listIterator();
    while(iterator.hasNext()) {
      final Property property = iterator.next();
      if (property.hasParentProperty() || property.isDenormalized()||
              (property instanceof Property.PrimaryKeyProperty && EntityRepository.getIdSource(entityID).isAutoGenerated()))
        iterator.remove();
    }
    Collections.sort(properties, new Comparator<Property>() {
      public int compare(final Property propertyOne, final Property propertyTwo) {
        return propertyOne.toString().toLowerCase().compareTo(propertyTwo.toString().toLowerCase());
      }
    });

    return properties;
  }

  public static List<Entity> copyEntities(final List<Entity> entities) {
    final List<Entity> copies = new ArrayList<Entity>(entities.size());
    for (final Entity entity : entities)
      copies.add((Entity) entity.getCopy());

    return copies;
  }

  private static Object parseJSONValue(final String entityID, final String propertyID, final JSONObject propertyValues) throws JSONException, ParseException {
    final Property property = EntityRepository.getProperty(entityID, propertyID);
    if (propertyValues.isNull(propertyID))
      return null;

    if (property.isReference())
      return parseJSONString(propertyValues.getString(propertyID)).get(0);
    else if (property.isString())
      return propertyValues.getString(propertyID);
    else if (property.isBoolean())
      return propertyValues.getBoolean(propertyID);
    else if (property.isDate())
      return jsonDateFormat.parse(propertyValues.getString(propertyID));
    else if (property.isTimestamp())
      return jsonTimestampFormat.parse(propertyValues.getString(propertyID));
    else if (property.isDouble())
      return propertyValues.getDouble(propertyID);
    else if (property.isInteger())
      return propertyValues.getInt(propertyID);

    return propertyValues.getString(propertyID);
  }

  private static JSONObject toJSONObject(final Entity entity, final boolean includeForeignKeys) throws JSONException {
    final JSONObject jsonEntity = new JSONObject();
    jsonEntity.put("entityID", entity.getEntityID());
    jsonEntity.put("propertyValues", getPropertyValuesJSONObject(entity, includeForeignKeys));
    if (entity.isModified())
      jsonEntity.put("originalValues", getOriginalValuesJSONObject(entity));

    return jsonEntity;
  }

  private static JSONObject getPropertyValuesJSONObject(final Entity entity, final boolean includeForeignKeys) throws JSONException {
    final JSONObject propertyValues = new JSONObject();
    for (final Property property : EntityRepository.getDatabaseProperties(entity.getEntityID(), true, true, true)) {
      if (!(property instanceof Property.ForeignKeyProperty) || includeForeignKeys)
        propertyValues.put(property.getPropertyID(), getJSONValue(entity, property, includeForeignKeys));
    }

    return propertyValues;
  }

  private static Object getJSONValue(final Entity entity, final Property property, final boolean includeForeignKeys) throws JSONException {
    if (entity.isValueNull(property.getPropertyID()))
      return JSONObject.NULL;
    if (property instanceof Property.ForeignKeyProperty)
      return getJSONObject(Arrays.asList(entity.getEntityValue(property.getPropertyID())), includeForeignKeys);
    if (property.isTime())
      return entity.getFormattedValue(property.getPropertyID(), property.isDate() ? jsonDateFormat : jsonTimestampFormat);

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
    final Object originalValue = entity.getOriginalValue(property.getPropertyID());
    if (originalValue == null)
      return JSONObject.NULL;
    if (property instanceof Property.ForeignKeyProperty)
      return getJSONObject(Arrays.asList((Entity) originalValue), false);
    if (property.isTime()) {
      final Date date = (Date) originalValue;
      return property.isDate() ? jsonDateFormat.format(date) : jsonTimestampFormat.format(date);
    }

    return originalValue;
  }

  public static Entity createRandomEntity(final String entityID, final Map<String, Entity> referenceEntities) {
    return createEntity(entityID, new ValueProvider<Property, Object>() {
      public Object getValue(final Property property) {
        return getRandomValue(property, referenceEntities);
      }
    });
  }

  public static Entity createEntity(final String entityID, final ValueProvider<Property, Object> valueProvider) {
    final Entity entity = new Entity(entityID);
    final boolean autoGenID = EntityRepository.isPrimaryKeyAutoGenerated(entityID);
    for (final Property property : EntityRepository.getDatabaseProperties(entityID, !autoGenID, false, true)) {
      if (!property.hasParentProperty() && !property.isDenormalized())
        entity.setValue(property, valueProvider.getValue(property));
    }

    return entity;
  }

  public static Entity randomize(final Entity entity, final boolean includePrimaryKey, final Map<String, Entity> referenceEntities) {
    for (final Property property : EntityRepository.getDatabaseProperties(entity.getEntityID(), includePrimaryKey, false, true)) {
      if (property instanceof Property.ForeignKeyProperty) {
        final Property.ForeignKeyProperty foreignKeyProperty = (Property.ForeignKeyProperty) property;
        if (referenceEntities != null && referenceEntities.containsKey(foreignKeyProperty.getReferencedEntityID()))
          entity.setValue(property, getRandomValue(property, referenceEntities));
      }
      else if (!property.hasParentProperty() && !property.isDenormalized())
        entity.setValue(property, getRandomValue(property, referenceEntities));
    }

    return entity;
  }

  public static Object getRandomValue(final Property property, final Map<String, Entity> referenceEntities) {
    if (property instanceof Property.ForeignKeyProperty) {
      final String referenceEntityID = ((Property.ForeignKeyProperty) property).getReferencedEntityID();
      return referenceEntities == null ? null : referenceEntities.get(referenceEntityID);
    }
    else {
      if (property instanceof Property.ValueListProperty) {
        final List<Item<Object>> items = ((Property.ValueListProperty) property).getValues();
        final Item item = items.get(random.nextInt(items.size()));

        return item.getItem();
      }
      else if (property.isBoolean())
        return random.nextBoolean();
      else if (property.isCharacter())
        return (char) random.nextInt();
      else if (property.isDate())
        return DateUtil.floorDate(new Date());
      else if (property.isTimestamp())
        return DateUtil.floorTimestamp(new Timestamp(System.currentTimeMillis()));
      else if (property.isDouble()) {
        double min = property.getMin() == null ? -10000000 : property.getMin();
        double max = property.getMax() == null ? 10000000 : property.getMax();
        //Min + (int)(Math.random() * ((Max - Min) + 1))
        final double ret = min + (random.nextDouble() * ((max - min) + 1));
        if (property.getMaximumFractionDigits() > 0)
          return Util.roundDouble(ret, property.getMaximumFractionDigits());
        else
          return ret;
      }
      else if (property.isInteger()) {
        double min = property.getMin() == null ? -10000000 : property.getMin();
        double max = property.getMax() == null ? 10000000 : property.getMax();

        return (int) (min + (random.nextDouble() * ((max - min) + 1)));
      }
      else if (property.isString()) {
        final int minLength = property.isNullable() ? 0 : 1;
        return Util.createRandomString(minLength, property.getMaxLength() < 0 ? 10 : property.getMaxLength());
      }
    }

    return null;
  }
}

/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.json;

import org.jminor.common.DateFormats;
import org.jminor.common.Serializer;
import org.jminor.common.Util;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class responsible for serializing Entity related objects to and from the JSON format.
 */
public final class EntityJSONParser implements Serializer<Entity> {

  private static final String ENTITY_ID = "entityID";
  private static final String VALUES = "values";
  private static final String ORIGINAL_VALUES = "originalValues";
  private static final String JSON_TIME_FORMAT = "HH:mm";
  private static final String JSON_DATE_FORMAT = "yyyy-MM-dd";
  private static final String JSON_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm";

  private final Entities entities;

  private boolean includeForeignKeyValues = false;
  private boolean includeNullValues = true;
  private boolean includeReadOnlyValues = true;
  private int indentation = -1;

  /**
   * @param entities the underlying domain entities
   */
  public EntityJSONParser(final Entities entities) {
    this.entities = entities;
  }

  /**
   * @return true if the foreign key graph should be included in serialized entities
   */
  public boolean isIncludeForeignKeyValues() {
    return includeForeignKeyValues;
  }

  /**
   * @param includeForeignKeyValues if true then the foreign key graph is included in serialized entities
   * @return this {@link EntityJSONParser} instance
   */
  public EntityJSONParser setIncludeForeignKeyValues(final boolean includeForeignKeyValues) {
    this.includeForeignKeyValues = includeForeignKeyValues;
    return this;
  }

  /**
   * @return true if null values should be included in exported entities
   */
  public boolean isIncludeNullValues() {
    return includeNullValues;
  }

  /**
   * @param includeNullValues true if null values should be included in exported entities
   * @return this {@link EntityJSONParser} instance
   */
  public EntityJSONParser setIncludeNullValues(final boolean includeNullValues) {
    this.includeNullValues = includeNullValues;
    return this;
  }

  /**
   * @return true if read only values should be included in exported entities
   */
  public boolean isIncludeReadOnlyValues() {
    return includeReadOnlyValues;
  }

  /**
   * @param includeReadOnlyValues true if read only values should be included in exported entities
   * @return this {@link EntityJSONParser} instance
   */
  public EntityJSONParser setIncludeReadOnlyValues(final boolean includeReadOnlyValues) {
    this.includeReadOnlyValues = includeReadOnlyValues;
    return this;
  }

  /**
   * @return the indentation to use when serializing entities
   */
  public int getIndentation() {
    return indentation;
  }

  /**
   * Sets the indendation used when exporting to JSON format, -1 means non-human readable, whereas &gt;= 0
   * means human readable with the given indentation.
   * @param indentation if &gt;= 0 then the serialized form will be human readable with the given indentation
   * @return this {@link EntityJSONParser} instance
   */
  public EntityJSONParser setIndentation(final int indentation) {
    this.indentation = indentation;
    return this;
  }

  /**
   * Serializes the given Entity instances into a JSON string array
   * @param entities the entities
   * @return a JSON string representation of the given entities
   * @throws SerializeException in case of an exception
   */
  @Override
  public String serialize(final List<Entity> entities) throws SerializeException {
    try {
      if (Util.nullOrEmpty(entities)) {
        return "";
      }

      final DateFormat jsonTimeFormat = DateFormats.getDateFormat(JSON_TIME_FORMAT);
      final DateFormat jsonDateFormat = DateFormats.getDateFormat(JSON_DATE_FORMAT);
      final DateFormat jsonTimestampFormat = DateFormats.getDateFormat(JSON_TIMESTAMP_FORMAT);
      final JSONArray jsonArray = new JSONArray();
      for (final Entity entity : entities) {
        jsonArray.put(serializeEntity(entity, jsonTimeFormat, jsonDateFormat, jsonTimestampFormat));
      }

      return indentation < 0 ? jsonArray.toString() : jsonArray.toString(indentation);
    }
    catch (final JSONException e) {
      throw new SerializeException(e.getMessage(), e);
    }
  }

  /**
   * Deserializes the given JSON string into a list of Entity instances
   * @param jsonString the JSON string to parse
   * @return a List containing the Entity instances represented by the given JSON string
   * @throws SerializeException in case of an exception
   */
  @Override
  public List<Entity> deserialize(final String jsonString) throws SerializeException {
    try {
      return deserializeEntities(jsonString);
    }
    catch (final Exception e) {
      throw new SerializeException(e.getMessage(), e);
    }
  }

  /**
   * Serializes the given Entity.Key instances into a JSON string array
   * @param keys the keys
   * @return a JSON string representation of the given entity keys
   * @throws JSONException in case of an exception
   */
  public String serializeKeys(final Collection<Entity.Key> keys) throws JSONException {
    if (Util.nullOrEmpty(keys)) {
      return "";
    }

    final DateFormat jsonTimeFormat = DateFormats.getDateFormat(JSON_TIME_FORMAT);
    final DateFormat jsonDateFormat = DateFormats.getDateFormat(JSON_DATE_FORMAT);
    final DateFormat jsonTimestampFormat = DateFormats.getDateFormat(JSON_TIMESTAMP_FORMAT);
    final JSONArray jsonArray = new JSONArray();
    for (final Entity.Key key : keys) {
      jsonArray.put(serializeKey(key, jsonTimeFormat, jsonDateFormat, jsonTimestampFormat));
    }

    return jsonArray.toString();
  }

  /**
   * Serializes the given entity
   * @param entity the Entity to serialize
   * @return the entity as a serialized string
   * @throws JSONException in case of an exception
   */
  public String serializeEntity(final Entity entity) throws JSONException {
    return serializeEntity(entity, DateFormats.getDateFormat(JSON_TIME_FORMAT),
            DateFormats.getDateFormat(JSON_DATE_FORMAT), DateFormats.getDateFormat(JSON_TIMESTAMP_FORMAT)).toString();
  }

  /**
   * Serializes the given key
   * @param key the key
   * @return a JSON serialized representation of the key
   * @throws JSONException in case of an exception
   */
  public String serializeKey(final Entity.Key key) throws JSONException {
    return serializeKey(key, DateFormats.getDateFormat(JSON_TIME_FORMAT), DateFormats.getDateFormat(JSON_DATE_FORMAT),
            DateFormats.getDateFormat(JSON_TIMESTAMP_FORMAT)).toString();
  }

  /**
   * Deserializes the given JSON string into a list of Entity instances
   * @param jsonString the JSON string to parse
   * @return a List containing the Entity instances represented by the given JSON string
   * @throws ParseException in case of an exception
   * @throws JSONException in case of an exception
   */
  public List<Entity> deserializeEntities(final String jsonString) throws JSONException, ParseException {
    if (Util.nullOrEmpty(jsonString)) {
      return Collections.emptyList();
    }

    final DateFormat jsonTimeFormat = DateFormats.getDateFormat(JSON_TIME_FORMAT);
    final DateFormat jsonDateFormat = DateFormats.getDateFormat(JSON_DATE_FORMAT);
    final DateFormat jsonTimestampFormat = DateFormats.getDateFormat(JSON_TIMESTAMP_FORMAT);
    final JSONArray jsonArray = new JSONArray(jsonString);
    final List<Entity> parsedEntities = new ArrayList<>();
    for (int i = 0; i < jsonArray.length(); i++) {
      parsedEntities.add(parseEntity(jsonArray.getJSONObject(i), jsonTimeFormat, jsonDateFormat, jsonTimestampFormat));
    }

    return parsedEntities;
  }

  /**
   * Deserializes the given JSON string into a list of Entity.Key instances
   * @param jsonString the JSON string to parse
   * @return a List containing the Entity.Key instances represented by the given JSON string
   * @throws ParseException in case of an exception
   * @throws JSONException in case of an exception
   */
  public List<Entity.Key> deserializeKeys(final String jsonString) throws JSONException, ParseException {
    if (Util.nullOrEmpty(jsonString)) {
      return Collections.emptyList();
    }

    final DateFormat jsonTimeFormat = DateFormats.getDateFormat(JSON_TIME_FORMAT);
    final DateFormat jsonDateFormat = DateFormats.getDateFormat(JSON_DATE_FORMAT);
    final DateFormat jsonTimestampFormat = DateFormats.getDateFormat(JSON_TIMESTAMP_FORMAT);
    final JSONArray jsonArray = new JSONArray(jsonString);
    final List<Entity.Key> keys = new ArrayList<>();
    for (int i = 0; i < jsonArray.length(); i++) {
      keys.add(parseKey(jsonArray.getJSONObject(i), jsonTimeFormat, jsonDateFormat, jsonTimestampFormat));
    }

    return keys;
  }

  /**
   * Parses an Entity instance from the given JSON object string
   * @param entityObject the JSON object string representing the entity
   * @return the Entity represented by the given JSON object
   * @throws ParseException in case of an exception
   * @throws JSONException in case of an exception
   */
  public Entity parseEntity(final String entityObject) throws JSONException, ParseException {
    return parseEntity(new JSONObject(entityObject), DateFormats.getDateFormat(JSON_TIME_FORMAT),
            DateFormats.getDateFormat(JSON_DATE_FORMAT), DateFormats.getDateFormat(JSON_TIMESTAMP_FORMAT));
  }

  /**
   * Parses an Entity.Key instance from the given JSON object string
   * @param keyObject the JSON object string representing the entity
   * @return the Entity.Key represented by the given JSON object
   * @throws ParseException in case of an exception
   * @throws JSONException in case of an exception
   */
  public Entity.Key parseKey(final String keyObject) throws JSONException, ParseException {
    return parseKey(new JSONObject(keyObject), DateFormats.getDateFormat(JSON_TIME_FORMAT),
            DateFormats.getDateFormat(JSON_DATE_FORMAT), DateFormats.getDateFormat(JSON_TIMESTAMP_FORMAT));
  }

  /**
   * Parses an Entity.Key instance from the given JSON object
   * @param keyObject the JSON object representing the entity key
   * @param jsonTimeFormat the format to use when parsing time
   * @param jsonDateFormat the format to use when parsing a date
   * @param jsonTimestampFormat the format to use when parsing a timestamp
   * @return the Entity.Key represented by the given JSON object
   * @throws IllegalArgumentException in case of an undefined entity
   * @throws ParseException in case of an exception
   * @throws JSONException in case of an exception
   */
  public Entity.Key parseKey(final JSONObject keyObject, final DateFormat jsonTimeFormat,
                                    final DateFormat jsonDateFormat, final DateFormat jsonTimestampFormat)
          throws JSONException, ParseException {
    final String entityID = keyObject.getString(ENTITY_ID);
    if (!entities.isDefined(entityID)) {
      throw new IllegalArgumentException("Undefined entity found in JSON string: '" + entityID + "'");
    }

    final Entity.Key key = entities.key(entityID);
    final JSONObject propertyValues = keyObject.getJSONObject(VALUES);
    for (int j = 0; j < propertyValues.names().length(); j++) {
      final String propertyID = propertyValues.names().get(j).toString();
      key.put(propertyID, parseValue(entities.getProperty(entityID, propertyID), propertyValues,
              jsonTimeFormat, jsonDateFormat, jsonTimestampFormat));
    }

    return key;
  }

  /**
   * Fetches the value of the given property from the given JSONObject
   * @param property the property
   * @param propertyValues the JSONObject containing the value
   * @return the value for the given property
   * @throws JSONException in case of an exception
   * @throws ParseException in case of an exception
   */
  public Object parseValue(final Property property, final JSONObject propertyValues)
          throws JSONException, ParseException {
    return parseValue(property, propertyValues, DateFormats.getDateFormat(JSON_TIME_FORMAT),
            DateFormats.getDateFormat(JSON_DATE_FORMAT), DateFormats.getDateFormat(JSON_TIMESTAMP_FORMAT));
  }

  private JSONObject serializeEntity(final Entity entity,
                                     final DateFormat jsonTimeFormat, final DateFormat jsonDateFormat,
                                     final DateFormat jsonTimestampFormat) throws JSONException {
    final JSONObject jsonEntity = new JSONObject();
    jsonEntity.put(ENTITY_ID, entity.getEntityID());
    jsonEntity.put(VALUES, serializeValues(entity, jsonTimeFormat, jsonDateFormat, jsonTimestampFormat));
    if (entity.isModified()) {
      jsonEntity.put(ORIGINAL_VALUES, serializeOriginalValues(entity, jsonTimeFormat, jsonDateFormat, jsonTimestampFormat));
    }

    return jsonEntity;
  }

  private JSONObject serializeKey(final Entity.Key key, final DateFormat jsonTimeFormat,
                                  final DateFormat jsonDateFormat, final DateFormat jsonTimestampFormat) throws JSONException {
    final JSONObject jsonKey = new JSONObject();
    jsonKey.put(ENTITY_ID, key.getEntityID());
    jsonKey.put(VALUES, serializeValues(key, jsonTimeFormat, jsonDateFormat, jsonTimestampFormat));

    return jsonKey;
  }

  private JSONObject serializeValues(final Entity entity, final DateFormat jsonTimeFormat,
                                     final DateFormat jsonDateFormat, final DateFormat jsonTimestampFormat) throws JSONException {
    final JSONObject propertyValues = new JSONObject();
    for (final Property property : entity.keySet()) {
      if (include(property, entity)) {
        propertyValues.put(property.getPropertyID(),
                serializeValue(entity.get(property), property, jsonTimeFormat, jsonDateFormat, jsonTimestampFormat));
      }
    }

    return propertyValues;
  }

  private JSONObject serializeValues(final Entity.Key key, final DateFormat jsonTimeFormat,
                                     final DateFormat jsonDateFormat, final DateFormat jsonTimestampFormat) throws JSONException {
    final JSONObject propertyValues = new JSONObject();
    for (final Property.ColumnProperty property : entities.getPrimaryKeyProperties(key.getEntityID())) {
      propertyValues.put(property.getPropertyID(), serializeValue(key.get(property), property,
              jsonTimeFormat, jsonDateFormat, jsonTimestampFormat));
    }

    return propertyValues;
  }

  private JSONObject serializeOriginalValues(final Entity entity, final DateFormat jsonTimeFormat,
                                             final DateFormat jsonDateFormat, final DateFormat jsonTimestampFormat) throws JSONException {
    final JSONObject originalValues = new JSONObject();
    for (final Property property : entities.getProperties(entity.getEntityID())) {
      if (entity.isModified(property.getPropertyID()) && (!(property instanceof Property.ForeignKeyProperty) || includeForeignKeyValues)) {
        originalValues.put(property.getPropertyID(),
                serializeValue(entity.getOriginal(property.getPropertyID()), property,
                        jsonTimeFormat, jsonDateFormat, jsonTimestampFormat));
      }
    }

    return originalValues;
  }

  private Object serializeValue(final Object value, final Property property,
                                final DateFormat jsonTimeFormat, final DateFormat jsonDateFormat,
                                final DateFormat jsonTimestampFormat) throws JSONException {
    if (value == null) {
      return JSONObject.NULL;
    }
    if (property instanceof Property.ForeignKeyProperty) {
      return serializeEntity((Entity) value, jsonTimeFormat, jsonDateFormat, jsonTimestampFormat);
    }
    if (property.isTime()) {
      final Time time = (Time) value;
      return jsonTimeFormat.format(new Date(time.getTime()));
    }
    if (property.isDateOrTime()) {
      final Date date = (Date) value;
      return property.isDate() ? jsonDateFormat.format(date) : jsonTimestampFormat.format(date);
    }

    return value;
  }

  private boolean include(final Property property, final Entity entity) {
    if (property instanceof Property.DerivedProperty || property instanceof Property.DenormalizedViewProperty) {
      return false;
    }
    if (!includeForeignKeyValues && property instanceof Property.ForeignKeyProperty) {
      return false;
    }
    if (!includeReadOnlyValues && property.isReadOnly()) {
      return false;
    }
    if (!includeNullValues && entity.isValueNull(property)) {
      return false;
    }

    return true;
  }

  /**
   * Parses an Entity instance from the given JSON object
   * @param entityObject the JSON object representing the entity
   * @param jsonDateFormat the format to use when parsing dates
   * @param jsonTimestampFormat the format to use when parsing timestamps
   * @return the Entity represented by the given JSON object
   * @throws IllegalArgumentException in case of an undefined entity
   * @throws ParseException in case of an exception
   * @throws JSONException in case of an exception
   */
  private Entity parseEntity(final JSONObject entityObject, final DateFormat jsonTimeFormat,
                                    final DateFormat jsonDateFormat, final DateFormat jsonTimestampFormat) throws JSONException, ParseException {
    final Map<Property, Object> propertyValueMap = new HashMap<>();
    final String entityID = entityObject.getString(ENTITY_ID);
    if (!entities.isDefined(entityID)) {
      throw new IllegalArgumentException("Undefined entity found in JSON string: '" + entityID + "'");
    }

    final JSONObject propertyValues = entityObject.getJSONObject(VALUES);
    for (int j = 0; j < propertyValues.names().length(); j++) {
      final String propertyID = propertyValues.names().get(j).toString();
      propertyValueMap.put(entities.getProperty(entityID, propertyID),
              parseValue(entities.getProperty(entityID, propertyID), propertyValues, jsonTimeFormat, jsonDateFormat, jsonTimestampFormat));
    }
    Map<Property, Object> originalValueMap = null;
    if (!entityObject.isNull(ORIGINAL_VALUES)) {
      originalValueMap = new HashMap<>();
      final JSONObject originalValues = entityObject.getJSONObject(ORIGINAL_VALUES);
      for (int j = 0; j < originalValues.names().length(); j++) {
        final String propertyID = originalValues.names().get(j).toString();
        originalValueMap.put(entities.getProperty(entityID, propertyID),
                parseValue(entities.getProperty(entityID, propertyID), originalValues, jsonTimeFormat, jsonDateFormat, jsonTimestampFormat));
      }
    }

    return entities.entity(entityID, propertyValueMap, originalValueMap);
  }

  /**
   * Fetches the value of the given property from the given JSONObject
   * @param property the property
   * @param jsonDateFormat the format to use when parsing dates
   * @param jsonTimestampFormat the format to use when parsing timestamps
   * @param propertyValues the JSONObject containing the value
   * @return the value for the given property
   * @throws JSONException in case of an exception
   * @throws ParseException in case of an exception
   */
  private Object parseValue(final Property property, final JSONObject propertyValues,
                                   final DateFormat jsonTimeFormat, final DateFormat jsonDateFormat,
                                   final DateFormat jsonTimestampFormat) throws JSONException, ParseException {
    if (propertyValues.isNull(property.getPropertyID())) {
      return null;
    }
    if (property.isString()) {
      return propertyValues.getString(property.getPropertyID());
    }
    else if (property.isBoolean()) {
      return propertyValues.getBoolean(property.getPropertyID());
    }
    else if (property.isTime()) {
      return jsonTimeFormat.parse(propertyValues.getString(property.getPropertyID()));
    }
    else if (property.isDate()) {
      return jsonDateFormat.parse(propertyValues.getString(property.getPropertyID()));
    }
    else if (property.isTimestamp()) {
      return new Timestamp(jsonTimestampFormat.parse(propertyValues.getString(property.getPropertyID())).getTime());
    }
    else if (property.isDouble()) {
      return propertyValues.getDouble(property.getPropertyID());
    }
    else if (property.isInteger()) {
      return propertyValues.getInt(property.getPropertyID());
    }
    else if (property instanceof Property.ForeignKeyProperty) {
      return parseEntity(propertyValues.getJSONObject(property.getPropertyID()), jsonTimeFormat, jsonDateFormat, jsonTimestampFormat);
    }

    return propertyValues.getString(property.getPropertyID());
  }
}

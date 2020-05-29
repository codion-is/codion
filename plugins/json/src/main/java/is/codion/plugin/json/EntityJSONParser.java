/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.json;

import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.DerivedProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static is.codion.common.Util.nullOrEmpty;
import static is.codion.framework.domain.property.Properties.attribute;
import static java.util.Collections.emptyList;

/**
 * A class responsible for serializing Entity related objects to and from the JSON format.
 * Note that this class is not thread safe.
 */
public final class EntityJSONParser {

  private static final String ENTITY_ID = "entityId";
  private static final String VALUES = "values";
  private static final String ORIGINAL_VALUES = "originalValues";
  private static final String JSON_TIME_FORMAT = "HH:mm";
  private static final String JSON_DATE_FORMAT = "yyyy-MM-dd";
  private static final String JSON_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm";

  private final DateTimeFormatter jsonTimeFormat = DateTimeFormatter.ofPattern(JSON_TIME_FORMAT);
  private final DateTimeFormatter jsonDateFormat = DateTimeFormatter.ofPattern(JSON_DATE_FORMAT);
  private final DateTimeFormatter jsonTimestampFormat = DateTimeFormatter.ofPattern(JSON_TIMESTAMP_FORMAT);

  private final Entities entities;

  private boolean includeForeignKeyValues = false;
  private boolean includeNullValues = true;
  private int indentation = -1;

  /**
   * @param entities the domain model entities
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
   */
  public String serialize(final List<Entity> entities) {
    if (nullOrEmpty(entities)) {
      return "";
    }

    final JSONArray jsonArray = new JSONArray();
    for (final Entity entity : entities) {
      jsonArray.put(toJSONObject(entity));
    }

    return indentation < 0 ? jsonArray.toString() : jsonArray.toString(indentation);
  }

  /**
   * Deserializes the given JSON string into a list of Entity instances
   * @param jsonString the JSON string to parse
   * @return a List containing the Entity instances represented by the given JSON string
   */
  public List<Entity> deserialize(final String jsonString) {
    return deserializeEntities(jsonString);
  }

  /**
   * Serializes the given Entity.Key instances into a JSON string array
   * @param keys the keys
   * @return a JSON string representation of the given entity keys
   */
  public String serializeKeys(final Collection<Entity.Key> keys) {
    if (nullOrEmpty(keys)) {
      return "";
    }

    final JSONArray jsonArray = new JSONArray();
    for (final Entity.Key key : keys) {
      jsonArray.put(toJSONObject(key));
    }

    return jsonArray.toString();
  }

  /**
   * Serializes the given entity
   * @param entity the Entity to serialize
   * @return the entity as a serialized string
   */
  public String serializeEntity(final Entity entity) {
    return toJSONObject(entity).toString();
  }

  /**
   * Serializes the given key
   * @param key the key
   * @return a JSON serialized representation of the key
   */
  public String serializeKey(final Entity.Key key) {
    return toJSONObject(key).toString();
  }

  /**
   * Serializes the given property value
   * @param value the value
   * @param property the property
   * @return the value as a string
   */
  public Object serializeValue(final Object value, final Property property) {
    if (value == null) {
      return JSONObject.NULL;
    }
    if (property instanceof ForeignKeyProperty) {
      return toJSONObject((Entity) value);
    }
    if (property.isBigDecimal()) {
      return value.toString();
    }
    if (property.isTime()) {
      final LocalTime time = (LocalTime) value;
      return jsonTimeFormat.format(time);
    }
    if (property.isDate()) {
      final LocalDate date = (LocalDate) value;
      return jsonDateFormat.format(date);
    }
    if (property.isTimestamp()) {
      final LocalDateTime dateTime = (LocalDateTime) value;
      return jsonTimestampFormat.format(dateTime);
    }

    return value;
  }

  /**
   * Deserializes the given JSON string into a list of Entity instances
   * @param jsonString the JSON string to parse
   * @return a List containing the Entity instances represented by the given JSON string
   */
  public List<Entity> deserializeEntities(final String jsonString) {
    if (nullOrEmpty(jsonString)) {
      return emptyList();
    }

    final JSONArray jsonArray = new JSONArray(jsonString);
    final List<Entity> parsedEntities = new ArrayList<>();
    for (int i = 0; i < jsonArray.length(); i++) {
      parsedEntities.add(parseEntity(jsonArray.getJSONObject(i)));
    }

    return parsedEntities;
  }

  /**
   * Deserializes the given JSON string into a list of Entity.Key instances
   * @param jsonString the JSON string to parse
   * @return a List containing the Entity.Key instances represented by the given JSON string
   */
  public List<Entity.Key> deserializeKeys(final String jsonString) {
    if (nullOrEmpty(jsonString)) {
      return emptyList();
    }

    final JSONArray jsonArray = new JSONArray(jsonString);
    final List<Entity.Key> keys = new ArrayList<>();
    for (int i = 0; i < jsonArray.length(); i++) {
      keys.add(parseKey(jsonArray.getJSONObject(i)));
    }

    return keys;
  }

  /**
   * Parses an Entity instance from the given JSON object string
   * @param entityObject the JSON object string representing the entity
   * @return the Entity represented by the given JSON object
   */
  public Entity parseEntity(final String entityObject) {
    return parseEntity(new JSONObject(entityObject));
  }

  /**
   * Parses an Entity.Key instance from the given JSON object string
   * @param keyObject the JSON object string representing the entity
   * @return the Entity.Key represented by the given JSON object
   */
  public Entity.Key parseKey(final String keyObject) {
    return parseKey(new JSONObject(keyObject));
  }

  /**
   * Parses an Entity.Key instance from the given JSON object
   * @param keyObject the JSON object representing the entity key
   * @return the Entity.Key represented by the given JSON object
   * @throws IllegalArgumentException in case of an undefined entity
   */
  public Entity.Key parseKey(final JSONObject keyObject) {
    final String entityId = keyObject.getString(ENTITY_ID);
    final Entity.Key key = entities.key(entityId);
    final EntityDefinition definition = entities.getDefinition(entityId);
    final JSONObject propertyValues = keyObject.getJSONObject(VALUES);
    for (int j = 0; j < propertyValues.names().length(); j++) {
      final Attribute<Object> propertyId = attribute(propertyValues.names().get(j).toString());
      key.put(propertyId, parseValue(definition.getProperty(propertyId), propertyValues));
    }

    return key;
  }

  /**
   * Fetches the value of the given property from the given JSONObject
   * @param property the property
   * @param propertyValues the JSONObject containing the value
   * @return the value for the given property
   */
  public Object parseValue(final Property property, final JSONObject propertyValues) {
    if (propertyValues.isNull(property.getPropertyId().getId())) {
      return null;
    }
    if (property.isString()) {
      return propertyValues.getString(property.getPropertyId().getId());
    }
    else if (property.isBoolean()) {
      return propertyValues.getBoolean(property.getPropertyId().getId());
    }
    else if (property.isTime()) {
      return LocalTime.parse(propertyValues.getString(property.getPropertyId().getId()), jsonTimeFormat);
    }
    else if (property.isDate()) {
      return LocalDate.parse(propertyValues.getString(property.getPropertyId().getId()), jsonDateFormat);
    }
    else if (property.isTimestamp()) {
      return LocalDateTime.parse(propertyValues.getString(property.getPropertyId().getId()), jsonTimestampFormat);
    }
    else if (property.isDouble()) {
      return propertyValues.getDouble(property.getPropertyId().getId());
    }
    else if (property.isInteger()) {
      return propertyValues.getInt(property.getPropertyId().getId());
    }
    else if (property.isBigDecimal()) {
      return propertyValues.getBigDecimal(property.getPropertyId().getId());
    }
    else if (property instanceof ForeignKeyProperty) {
      return parseEntity(propertyValues.getJSONObject(property.getPropertyId().getId()));
    }

    return propertyValues.getString(property.getPropertyId().getId());
  }

  private JSONObject toJSONObject(final Entity entity) {
    final JSONObject jsonEntity = new JSONObject();
    jsonEntity.put(ENTITY_ID, entity.getEntityId());
    jsonEntity.put(VALUES, serializeValues(entity));
    if (entity.isModified()) {
      jsonEntity.put(ORIGINAL_VALUES, serializeOriginalValues(entity));
    }

    return jsonEntity;
  }

  private JSONObject toJSONObject(final Entity.Key key) {
    final JSONObject jsonKey = new JSONObject();
    jsonKey.put(ENTITY_ID, key.getEntityId());
    jsonKey.put(VALUES, serializeValues(key));

    return jsonKey;
  }

  private JSONObject serializeValues(final Entity entity) {
    final JSONObject propertyValues = new JSONObject();
    for (final Property property : entity.keySet()) {
      if (include(property, entity)) {
        propertyValues.put(property.getPropertyId().getId(), serializeValue(entity.get(property), property));
      }
    }

    return propertyValues;
  }

  private JSONObject serializeValues(final Entity.Key key) {
    final JSONObject propertyValues = new JSONObject();
    for (final ColumnProperty property : entities.getDefinition(key.getEntityId()).getPrimaryKeyProperties()) {
      propertyValues.put(property.getPropertyId().getId(), serializeValue(key.get(property), property));
    }

    return propertyValues;
  }

  private JSONObject serializeOriginalValues(final Entity entity) {
    final JSONObject originalValues = new JSONObject();
    for (final Property property : entities.getDefinition(entity.getEntityId()).getProperties()) {
      if (entity.isModified(property.getPropertyId()) && (!(property instanceof ForeignKeyProperty) || includeForeignKeyValues)) {
        originalValues.put(property.getPropertyId().getId(),
                serializeValue(entity.getOriginal(property.getPropertyId()), property));
      }
    }

    return originalValues;
  }

  private boolean include(final Property property, final Entity entity) {
    if (property instanceof DerivedProperty) {
      return false;
    }
    if (!includeForeignKeyValues && property instanceof ForeignKeyProperty) {
      return false;
    }
    if (!includeNullValues && entity.isNull(property)) {
      return false;
    }

    return true;
  }

  /**
   * Parses an Entity instance from the given JSON object
   * @param entityObject the JSON object representing the entity
   * @return the Entity represented by the given JSON object
   * @throws IllegalArgumentException in case of an undefined entity
   */
  private Entity parseEntity(final JSONObject entityObject) {
    final String entityId = entityObject.getString(ENTITY_ID);

    return entities.getDefinition(entityId).entity(parseValues(entityObject, entityId, VALUES),
            entityObject.isNull(ORIGINAL_VALUES) ? null : parseValues(entityObject, entityId, ORIGINAL_VALUES));
  }

  private Map<Property, Object> parseValues(final JSONObject entityObject, final String entityId, final String valuesKey) {
    final Map<Property, Object> valueMap = new HashMap<>();
    final JSONObject propertyValues = entityObject.getJSONObject(valuesKey);
    for (int j = 0; j < propertyValues.names().length(); j++) {
      final Attribute<Object> propertyId = attribute(propertyValues.names().get(j).toString());
      final EntityDefinition entityDefinition = entities.getDefinition(entityId);
      valueMap.put(entityDefinition.getProperty(propertyId),
              parseValue(entityDefinition.getProperty(propertyId), propertyValues));
    }

    return valueMap;
  }
}

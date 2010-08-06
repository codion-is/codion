/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.json;

import org.jminor.common.model.DateUtil;
import org.jminor.common.model.Deserializer;
import org.jminor.common.model.Serializer;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EntityJSONParser implements Serializer<Entity>, Deserializer<Entity> {

  private int indentFactor = 2;

  private static final ThreadLocal<DateFormat> jsonDateFormat = DateUtil.getThreadLocalDateFormat("yyyy-MM-dd");
  private static final ThreadLocal<DateFormat> jsonTimestampFormat = DateUtil.getThreadLocalDateFormat("yyyy-MM-dd HH:mm");

  public int getIndentFactor() {
    return indentFactor;
  }

  public void setIndentFactor(final int indentFactor) {
    this.indentFactor = indentFactor;
  }

  public String serialize(final List<Entity> values) throws SerializeException {
    try {
      return getJSONString(values, false, indentFactor);
    }
    catch (JSONException e) {
      throw new SerializeException(e.getMessage(), e);
    }
  }

  public List<Entity> deserialize(final String values) throws DeserializeException {
    try {
      return parseJSONString(values);
    }
    catch (Exception e) {
      throw new DeserializeException(e.getMessage(), e);
    }
  }

  public static List<Entity> parseJSONString(final String jsonString) throws JSONException, ParseException {
    if (jsonString == null || jsonString.length() == 0) {
      return Collections.emptyList();
    }

    final JSONObject jsonObject = new JSONObject(jsonString);
    final List<Entity> entities = new ArrayList<Entity>();
    for (int i = 0; i < jsonObject.names().length(); i++) {
      final JSONObject entityObject = jsonObject.getJSONObject(jsonObject.names().get(i).toString());
      final Map<String, Object> propertyValueMap = new HashMap<String, Object>();
      final String entityID = entityObject.getString("entityID");
      if (!Entities.isDefined(entityID)) {
        throw new RuntimeException("Undifined entity type found in JSON file: '" + entityID + "'");
      }

      final JSONObject propertyValues = entityObject.getJSONObject("propertyValues");
      for (int j = 0; j < propertyValues.names().length(); j++) {
        final String propertyID = propertyValues.names().get(j).toString();
        propertyValueMap.put(propertyID, parseJSONValue(entityID, propertyID, propertyValues));
      }
      Map<String, Object> originalValueMap = null;
      if (!entityObject.isNull("originalValues")) {
        originalValueMap = new HashMap<String, Object>();
        final JSONObject originalValues = entityObject.getJSONObject("originalValues");
        for (int j = 0; j < originalValues.names().length(); j++) {
          final String propertyID = originalValues.names().get(j).toString();
          originalValueMap.put(propertyID, parseJSONValue(entityID, propertyID, originalValues));
        }
      }
      entities.add(Entities.entityInstance(entityID, propertyValueMap, originalValueMap));
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
    final JSONObject jsonObject = getJSONObject(entities, includeForeignKeys);
    if (indentFactor > 0) {
      return jsonObject.toString(indentFactor);
    }

    return jsonObject.toString();
  }

  public static JSONObject getJSONObject(final Collection<Entity> entities, final boolean includeForeignKeys) throws JSONException {
    final JSONObject jsonEntities = new JSONObject();
    for (final Entity entity : entities) {
      jsonEntities.put(entity.getEntityID() + " PK[" + entity.getPrimaryKey() + "]", toJSONObject(entity, includeForeignKeys));
    }

    return jsonEntities;
  }

  private static Object parseJSONValue(final String entityID, final String propertyID, final JSONObject propertyValues) throws JSONException, ParseException {
    final Property property = Entities.getProperty(entityID, propertyID);
    if (propertyValues.isNull(propertyID)) {
      return null;
    }

    if (property.isReference()) {
      return parseJSONString(propertyValues.getString(propertyID)).get(0);
    }
    else if (property.isString()) {
      return propertyValues.getString(propertyID);
    }
    else if (property.isBoolean()) {
      return propertyValues.getBoolean(propertyID);
    }
    else if (property.isDate()) {
      return jsonDateFormat.get().parse(propertyValues.getString(propertyID));
    }
    else if (property.isTimestamp()) {
      return jsonTimestampFormat.get().parse(propertyValues.getString(propertyID));
    }
    else if (property.isDouble()) {
      return propertyValues.getDouble(propertyID);
    }
    else if (property.isInteger()) {
      return propertyValues.getInt(propertyID);
    }

    return propertyValues.getString(propertyID);
  }

  private static JSONObject toJSONObject(final Entity entity, final boolean includeForeignKeys) throws JSONException {
    final JSONObject jsonEntity = new JSONObject();
    jsonEntity.put("entityID", entity.getEntityID());
    jsonEntity.put("propertyValues", getPropertyValuesJSONObject(entity, includeForeignKeys));
    if (entity.isModified()) {
      jsonEntity.put("originalValues", getOriginalValuesJSONObject(entity, includeForeignKeys));
    }

    return jsonEntity;
  }

  private static JSONObject getPropertyValuesJSONObject(final Entity entity, final boolean includeForeignKeys) throws JSONException {
    final JSONObject propertyValues = new JSONObject();
    for (final Property property : Entities.getColumnProperties(entity.getEntityID(), true, true, true, includeForeignKeys)) {
      propertyValues.put(property.getPropertyID(), getJSONValue(entity, property, includeForeignKeys));
    }

    return propertyValues;
  }

  private static Object getJSONValue(final Entity entity, final Property property, final boolean includeForeignKeys) throws JSONException {
    if (entity.isValueNull(property.getPropertyID())) {
      return JSONObject.NULL;
    }
    if (property instanceof Property.ForeignKeyProperty) {
      return getJSONObject(Arrays.asList(entity.getForeignKeyValue(property.getPropertyID())), includeForeignKeys);
    }
    if (property.isTime()) {
      return entity.getFormattedValue(property.getPropertyID(), property.isDate() ? jsonDateFormat.get() : jsonTimestampFormat.get());
    }

    return entity.getValue(property.getPropertyID());
  }

  private static JSONObject getOriginalValuesJSONObject(final Entity entity, final boolean includeForeignKeys) throws JSONException {
    final JSONObject originalValues = new JSONObject();
    for (final Property property : Entities.getColumnProperties(entity.getEntityID(), true, true, true, includeForeignKeys)) {
      if (entity.isModified(property.getPropertyID())) {
        originalValues.put(property.getPropertyID(), getJSONOriginalValue(entity, property));
      }
    }

    return originalValues;
  }

  private static Object getJSONOriginalValue(final Entity entity, final Property property) throws JSONException {
    final Object originalValue = entity.getOriginalValue(property.getPropertyID());
    if (originalValue == null) {
      return JSONObject.NULL;
    }
    if (property instanceof Property.ForeignKeyProperty) {
      return getJSONObject(Arrays.asList((Entity) originalValue), false);
    }
    if (property.isTime()) {
      final Date date = (Date) originalValue;
      return property.isDate() ? jsonDateFormat.get().format(date) : jsonTimestampFormat.get().format(date);
    }

    return originalValue;
  }
}

/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json;

import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

final class EntityDeserializer extends StdDeserializer<Entity> {

  private final Domain domain;
  private final EntityObjectMapper mapper;

  EntityDeserializer(final Domain domain, final EntityObjectMapper mapper) {
    super(Entity.class);
    this.domain = domain;
    this.mapper = mapper;
  }

  @Override
  public Entity deserialize(final JsonParser parser, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
    final ObjectCodec codec = parser.getCodec();
    final JsonNode node = codec.readTree(parser);

    final String entityId = node.get("entityId").asText();
    final Entity.Definition definition = domain.getDefinition(entityId);

    final JsonNode values = node.get("values");
    final Map<Property, Object> valueMap = new HashMap<>();
    final Iterator<Map.Entry<String, JsonNode>> fields = values.fields();
    while (fields.hasNext()) {
      final Map.Entry<String, JsonNode> field = fields.next();
      final Property property = definition.getProperty(field.getKey());
      valueMap.put(property, parseValue(property, field.getValue()));
    }
    final JsonNode originalValues = node.get("originalValues");
    final Map<Property, Object> originalValueMap;
    if (originalValues != null) {
      originalValueMap = new HashMap<>();
      final Iterator<Map.Entry<String, JsonNode>> originalFields = originalValues.fields();
      while (originalFields.hasNext()) {
        final Map.Entry<String, JsonNode> field = originalFields.next();
        final Property property = definition.getProperty(field.getKey());
        originalValueMap.put(property, parseValue(property, field.getValue()));
      }
    }
    else {
      originalValueMap = null;
    }

    return domain.entity(entityId, valueMap, originalValueMap);
  }

  /**
   * Fetches the value of the given property from the given JSONObject
   * @param property the property
   * @param propertyValues the JSONObject containing the value
   * @return the value for the given property
   */
  public Object parseValue(final Property property, final JsonNode jsonNode) throws JsonProcessingException {
    if (jsonNode.isNull()) {
      return null;
    }
    if (property.isString()) {
      return jsonNode.asText();
    }
    else if (property.isBoolean()) {
      return jsonNode.asBoolean();
    }
    else if (property.isTime()) {
      return LocalTime.parse(jsonNode.asText());
    }
    else if (property.isDate()) {
      return LocalDate.parse(jsonNode.asText(property.getPropertyId()));
    }
    else if (property.isTimestamp()) {
      return LocalDateTime.parse(jsonNode.asText(property.getPropertyId()));
    }
    else if (property.isDouble()) {
      return jsonNode.asDouble();
    }
    else if (property.isInteger()) {
      return jsonNode.asInt();
    }
    else if (property.isBigDecimal()) {
      return new BigDecimal(jsonNode.asText());
    }
    else if (property.isBlob()) {
      return Base64.getDecoder().decode((String) jsonNode.asText());
    }
    else if (property instanceof ForeignKeyProperty) {
      final String content = jsonNode.toString();
      return mapper.readValue(content, Entity.class);
    }

    return jsonNode.asText();
  }
}

/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json.domain;

import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
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

public final class EntityDeserializer extends StdDeserializer<Entity> {

  private final Domain domain;
  private final EntityObjectMapper mapper;

  EntityDeserializer(final Domain domain, final EntityObjectMapper mapper) {
    super(Entity.class);
    this.domain = domain;
    this.mapper = mapper;
  }

  @Override
  public Entity deserialize(final JsonParser parser, final DeserializationContext ctxt)
          throws IOException, JsonProcessingException {
    final JsonNode entityNode = parser.getCodec().readTree(parser);

    final String entityId = entityNode.get("entityId").asText();
    final Entity.Definition definition = domain.getDefinition(entityId);

    return domain.entity(entityId, getValueMap(entityNode, definition),
            getOriginalValueMap(entityNode, definition));
  }

  public static Object parseValue(final EntityObjectMapper mapper, final Property property, final JsonNode jsonNode) throws JsonProcessingException {
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
      return LocalDate.parse(jsonNode.asText());
    }
    else if (property.isTimestamp()) {
      return LocalDateTime.parse(jsonNode.asText());
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
      return mapper.readValue(jsonNode.toString(), Entity.class);
    }

    return jsonNode.asText();
  }

  private Map<Property, Object> getValueMap(final JsonNode node, final Entity.Definition definition)
          throws JsonProcessingException {
    final JsonNode values = node.get("values");
    final Map<Property, Object> valueMap = new HashMap<>();
    final Iterator<Map.Entry<String, JsonNode>> fields = values.fields();
    while (fields.hasNext()) {
      final Map.Entry<String, JsonNode> field = fields.next();
      final Property property = definition.getProperty(field.getKey());
      valueMap.put(property, parseValue(property, field.getValue()));
    }

    return valueMap;
  }

  private Map<Property, Object> getOriginalValueMap(final JsonNode node, final Entity.Definition definition)
          throws JsonProcessingException {
    final JsonNode originalValues = node.get("originalValues");
    if (originalValues != null) {
      final Map<Property, Object> originalValueMap = new HashMap<>();
      final Iterator<Map.Entry<String, JsonNode>> originalFields = originalValues.fields();
      while (originalFields.hasNext()) {
        final Map.Entry<String, JsonNode> field = originalFields.next();
        final Property property = definition.getProperty(field.getKey());
        originalValueMap.put(property, parseValue(property, field.getValue()));
      }

      return originalValueMap;
    }

    return null;
  }

  /**
   * Fetches the value of the given property from the given JsonNode
   * @param property the property
   * @param jsonNode the node containing the value
   * @return the value for the given property
   * @throws JsonProcessingException in case of an error
   */
  private Object parseValue(final Property property, final JsonNode jsonNode) throws JsonProcessingException {
    return parseValue(mapper, property, jsonNode);
  }
}

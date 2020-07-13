/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.domain;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.property.Property;

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

  private static final long serialVersionUID = 1;

  private final Entities entities;
  private final EntityObjectMapper mapper;

  EntityDeserializer(final Entities entities, final EntityObjectMapper mapper) {
    super(Entity.class);
    this.entities = entities;
    this.mapper = mapper;
  }

  @Override
  public Entity deserialize(final JsonParser parser, final DeserializationContext ctxt) throws IOException {
    final JsonNode entityNode = parser.getCodec().readTree(parser);

    final EntityType<?> entityType = entities.getDomainType().entityType(entityNode.get("entityType").asText());
    final EntityDefinition definition = entities.getDefinition(entityType);

    return definition.entity(getValueMap(entityNode, definition), getOriginalValueMap(entityNode, definition));
  }

  public static Object parseValue(final EntityObjectMapper mapper, final Attribute<?> attribute, final JsonNode jsonNode)
          throws JsonProcessingException {
    if (jsonNode.isNull()) {
      return null;
    }
    if (attribute.isString()) {
      return jsonNode.asText();
    }
    else if (attribute.isBoolean()) {
      return jsonNode.asBoolean();
    }
    else if (attribute.isLocalTime()) {
      return LocalTime.parse(jsonNode.asText());
    }
    else if (attribute.isLocalDate()) {
      return LocalDate.parse(jsonNode.asText());
    }
    else if (attribute.isLocalDateTime()) {
      return LocalDateTime.parse(jsonNode.asText());
    }
    else if (attribute.isDouble()) {
      return jsonNode.asDouble();
    }
    else if (attribute.isInteger()) {
      return jsonNode.asInt();
    }
    else if (attribute.isLong()) {
      return jsonNode.asLong();
    }
    else if (attribute.isBigDecimal()) {
      return new BigDecimal(jsonNode.asText());
    }
    else if (attribute.isByteArray()) {
      return Base64.getDecoder().decode(jsonNode.asText());
    }
    else if (attribute.isEntity()) {
      return mapper.readValue(jsonNode.toString(), Entity.class);
    }

    return jsonNode.asText();
  }

  private Map<Attribute<?>, Object> getValueMap(final JsonNode node, final EntityDefinition definition)
          throws JsonProcessingException {
    return getPropertyValueMap(definition, node.get("values"));
  }

  private Map<Attribute<?>, Object> getOriginalValueMap(final JsonNode node, final EntityDefinition definition)
          throws JsonProcessingException {
    final JsonNode originalValues = node.get("originalValues");
    if (originalValues != null) {
      return getPropertyValueMap(definition, originalValues);
    }

    return null;
  }

  private Map<Attribute<?>, Object> getPropertyValueMap(final EntityDefinition definition, final JsonNode values) throws JsonProcessingException {
    final Map<Attribute<?>, Object> valueMap = new HashMap<>();
    final Iterator<Map.Entry<String, JsonNode>> fields = values.fields();
    while (fields.hasNext()) {
      final Map.Entry<String, JsonNode> field = fields.next();
      final Property<?> property = definition.getProperty(definition.getEntityType().objectAttribute(field.getKey()));
      valueMap.put(property.getAttribute(), parseValue(property.getAttribute(), field.getValue()));
    }

    return valueMap;
  }

  /**
   * Fetches the value of the given attribute from the given JsonNode
   * @param attribute the attribute
   * @param jsonNode the node containing the value
   * @return the value for the given attribute
   * @throws JsonProcessingException in case of an error
   */
  private Object parseValue(final Attribute<?> attribute, final JsonNode jsonNode) throws JsonProcessingException {
    return parseValue(mapper, attribute, jsonNode);
  }
}

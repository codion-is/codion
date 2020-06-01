/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.domain;

import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.Identity;
import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.EntityAttribute;
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

import static is.codion.framework.domain.property.Attributes.attribute;

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

    final Identity entityId = Identity.identity(entityNode.get("entityId").asText());
    final EntityDefinition definition = entities.getDefinition(entityId);

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
    else if (attribute.isTime()) {
      return LocalTime.parse(jsonNode.asText());
    }
    else if (attribute.isDate()) {
      return LocalDate.parse(jsonNode.asText());
    }
    else if (attribute.isTimestamp()) {
      return LocalDateTime.parse(jsonNode.asText());
    }
    else if (attribute.isDouble()) {
      return jsonNode.asDouble();
    }
    else if (attribute.isInteger()) {
      return jsonNode.asInt();
    }
    else if (attribute.isBigDecimal()) {
      return new BigDecimal(jsonNode.asText());
    }
    else if (attribute.isBlob()) {
      return Base64.getDecoder().decode(jsonNode.asText());
    }
    else if (attribute instanceof EntityAttribute) {
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
      final Property<?> property = definition.getProperty(attribute(field.getKey(), definition.getEntityId()));
      valueMap.put(property.getAttribute(), parseValue(property.getAttribute(), field.getValue()));
    }

    return valueMap;
  }

  /**
   * Fetches the value of the given property from the given JsonNode
   * @param property the property
   * @param jsonNode the node containing the value
   * @return the value for the given property
   * @throws JsonProcessingException in case of an error
   */
  private Object parseValue(final Attribute<?> property, final JsonNode jsonNode) throws JsonProcessingException {
    return parseValue(mapper, property, jsonNode);
  }
}

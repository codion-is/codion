/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.domain;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.property.Property;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EntityDeserializer extends StdDeserializer<Entity> {

  private static final long serialVersionUID = 1;

  private final Entities entities;
  private final EntityObjectMapper entityObjectMapper;
  private final Map<String, EntityDefinition> definitions = new ConcurrentHashMap<>();

  EntityDeserializer(Entities entities, EntityObjectMapper entityObjectMapper) {
    super(Entity.class);
    this.entities = entities;
    this.entityObjectMapper = entityObjectMapper;
  }

  @Override
  public Entity deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
    JsonNode entityNode = parser.getCodec().readTree(parser);
    EntityDefinition definition = definitions.computeIfAbsent(entityNode.get("entityType").asText(), entities::getDefinition);

    return definition.entity(getValueMap(entityNode, definition), getOriginalValueMap(entityNode, definition));
  }

  private Map<Attribute<?>, Object> getValueMap(JsonNode node, EntityDefinition definition)
          throws JsonProcessingException {
    return getPropertyValueMap(definition, node.get("values"));
  }

  private Map<Attribute<?>, Object> getOriginalValueMap(JsonNode node, EntityDefinition definition)
          throws JsonProcessingException {
    JsonNode originalValues = node.get("originalValues");
    if (originalValues != null) {
      return getPropertyValueMap(definition, originalValues);
    }

    return null;
  }

  private Map<Attribute<?>, Object> getPropertyValueMap(EntityDefinition definition, JsonNode values) throws JsonProcessingException {
    Map<Attribute<?>, Object> valueMap = new HashMap<>();
    Iterator<Map.Entry<String, JsonNode>> fields = values.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      Property<?> property = definition.getProperty(definition.getAttribute(field.getKey()));
      valueMap.put(property.getAttribute(), entityObjectMapper.readValue(field.getValue().toString(), property.getAttribute().getValueClass()));
    }

    return valueMap;
  }
}

/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.json.domain;

import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;

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
    EntityDefinition definition = definitions.computeIfAbsent(entityNode.get("entityType").asText(), entities::definition);

    Entity entity = definition.entity(valueMap(entityNode, definition), originalValueMap(entityNode, definition));
    JsonNode immutable = entityNode.get("immutable");
    if (immutable != null && immutable.booleanValue()) {
      entity = entity.immutable();
    }

    return entity;
  }

  private Map<Attribute<?>, Object> valueMap(JsonNode node, EntityDefinition definition)
          throws JsonProcessingException {
    return attributeValueMap(definition, node.get("values"));
  }

  private Map<Attribute<?>, Object> originalValueMap(JsonNode node, EntityDefinition definition)
          throws JsonProcessingException {
    JsonNode originalValues = node.get("originalValues");
    if (originalValues != null) {
      return attributeValueMap(definition, originalValues);
    }

    return null;
  }

  private Map<Attribute<?>, Object> attributeValueMap(EntityDefinition definition, JsonNode values) throws JsonProcessingException {
    Map<Attribute<?>, Object> valueMap = new HashMap<>();
    Iterator<Map.Entry<String, JsonNode>> fields = values.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      AttributeDefinition<?> attributeDefinition = definition.attributeDefinition(definition.attribute(field.getKey()));
      valueMap.put(attributeDefinition.attribute(), entityObjectMapper.readValue(field.getValue().toString(), attributeDefinition.attribute().type().valueClass()));
    }

    return valueMap;
  }
}

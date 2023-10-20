/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.domain;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class EntityKeyDeserializer extends StdDeserializer<Entity.Key> {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;
  private final Map<String, EntityDefinition> definitions = new ConcurrentHashMap<>();

  EntityKeyDeserializer(EntityObjectMapper entityObjectMapper) {
    super(Entity.Key.class);
    this.entityObjectMapper = entityObjectMapper;
  }

  @Override
  public Entity.Key deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
    ObjectCodec codec = parser.getCodec();
    JsonNode node = codec.readTree(parser);
    EntityDefinition definition = definitions.computeIfAbsent(node.get("entityType").asText(), entityObjectMapper.entities()::definition);
    JsonNode values = node.get("values");
    Entity.Key.Builder builder = entityObjectMapper.entities().keyBuilder(definition.entityType());
    Iterator<Map.Entry<String, JsonNode>> fields = values.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      ColumnDefinition<Object> columnDefinition = definition.columns().definition((Column<Object>) definition.attributes().get(field.getKey()));
      builder.with(columnDefinition.attribute(), entityObjectMapper.readValue(field.getValue().toString(), columnDefinition.attribute().type().valueClass()));
    }

    return builder.build();
  }
}

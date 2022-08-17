/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.domain;

import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.property.ColumnProperty;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class EntityKeyDeserializer extends StdDeserializer<Key> {

  private static final long serialVersionUID = 1;

  private final Entities entities;
  private final EntityObjectMapper entityObjectMapper;
  private final Map<String, EntityDefinition> definitions = new ConcurrentHashMap<>();

  EntityKeyDeserializer(Entities entities, EntityObjectMapper entityObjectMapper) {
    super(Key.class);
    this.entities = entities;
    this.entityObjectMapper = entityObjectMapper;
  }

  @Override
  public Key deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
    ObjectCodec codec = parser.getCodec();
    JsonNode node = codec.readTree(parser);
    EntityDefinition definition = definitions.computeIfAbsent(node.get("entityType").asText(), entities::definition);
    JsonNode values = node.get("values");
    Key.Builder builder = entities.keyBuilder(definition.type());
    Iterator<Map.Entry<String, JsonNode>> fields = values.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      ColumnProperty<Object> property = definition.columnProperty(definition.attribute(field.getKey()));
      builder.with(property.attribute(), entityObjectMapper.readValue(field.getValue().toString(), property.attribute().valueClass()));
    }

    return builder.build();
  }
}

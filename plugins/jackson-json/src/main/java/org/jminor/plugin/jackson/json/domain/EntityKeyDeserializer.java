/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json.domain;

import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.ColumnProperty;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

final class EntityKeyDeserializer extends StdDeserializer<Entity.Key> {

  private final Domain domain;
  private final EntityObjectMapper entityObjectMapper;

  EntityKeyDeserializer(final Domain domain, final EntityObjectMapper entityObjectMapper) {
    super(Entity.Key.class);
    this.domain = domain;
    this.entityObjectMapper = entityObjectMapper;
  }

  @Override
  public Entity.Key deserialize(final JsonParser parser, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
    final ObjectCodec codec = parser.getCodec();
    final JsonNode node = codec.readTree(parser);
    final String entityId = node.get("entityId").asText();
    final Entity.Definition definition = domain.getDefinition(entityId);
    final JsonNode values = node.get("values");
    final Entity.Key key = domain.key(entityId);
    final Iterator<Map.Entry<String, JsonNode>> fields = values.fields();
    while (fields.hasNext()) {
      final Map.Entry<String, JsonNode> field = fields.next();
      final ColumnProperty property = definition.getColumnProperty(field.getKey());
      key.put(property, EntityDeserializer.parseValue(entityObjectMapper, property, field.getValue()));
    }

    return key;
  }
}

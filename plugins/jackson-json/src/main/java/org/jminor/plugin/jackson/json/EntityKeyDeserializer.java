/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json;

import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Map;

final class EntityKeyDeserializer extends StdDeserializer<Entity.Key> {

  private final Domain domain;
  private final ObjectMapper mapper = new ObjectMapper();

  EntityKeyDeserializer(final Domain domain) {
    super(Entity.Key.class);
    this.domain = domain;
  }

  @Override
  public Entity.Key deserialize(final JsonParser parser, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
    final ObjectCodec codec = parser.getCodec();
    final JsonNode node = codec.readTree(parser);

    final String entityId = node.get("entityId").asText();
    final Entity.Definition definition = domain.getDefinition(entityId);

    final JsonNode values = node.get("values");
    final Map<String, Object> valueMap = mapper.convertValue(values, Map.class);
    final Entity.Key key = domain.key(entityId);
    valueMap.forEach((propertyId, value) -> key.put(definition.getColumnProperty(propertyId), value));

    return key;
  }
}

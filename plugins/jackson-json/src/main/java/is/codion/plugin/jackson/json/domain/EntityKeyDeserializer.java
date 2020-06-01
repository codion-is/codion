/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.domain;

import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityIdentity;
import is.codion.framework.domain.property.ColumnProperty;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

final class EntityKeyDeserializer extends StdDeserializer<Entity.Key> {

  private static final long serialVersionUID = 1;

  private final Entities entities;
  private final EntityObjectMapper entityObjectMapper;

  EntityKeyDeserializer(final Entities entities, final EntityObjectMapper entityObjectMapper) {
    super(Entity.Key.class);
    this.entities = entities;
    this.entityObjectMapper = entityObjectMapper;
  }

  @Override
  public Entity.Key deserialize(final JsonParser parser, final DeserializationContext ctxt) throws IOException {
    final ObjectCodec codec = parser.getCodec();
    final JsonNode node = codec.readTree(parser);
    final EntityIdentity entityId = Entities.entityIdentity(node.get("entityId").asText());
    final EntityDefinition definition = entities.getDefinition(entityId);
    final JsonNode values = node.get("values");
    final Entity.Key key = entities.key(entityId);
    final Iterator<Map.Entry<String, JsonNode>> fields = values.fields();
    while (fields.hasNext()) {
      final Map.Entry<String, JsonNode> field = fields.next();
      final ColumnProperty<Object> property = definition.getColumnProperty(definition.getEntityId().objectAttribute(field.getKey()));
      key.put(property.getAttribute(), EntityDeserializer.parseValue(entityObjectMapper, property.getAttribute(), field.getValue()));
    }

    return key;
  }
}

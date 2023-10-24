/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.EntityConnection.Count;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

final class CountDeserializer extends StdDeserializer<Count> {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  CountDeserializer(EntityObjectMapper entityObjectMapper) {
    super(Count.class);
    this.entityObjectMapper = entityObjectMapper;
  }

  @Override
  public Count deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
    JsonNode jsonNode = parser.getCodec().readTree(parser);
    EntityType entityType = entityObjectMapper.entities().domainType().entityType(jsonNode.get("entityType").asText());
    EntityDefinition definition = entityObjectMapper.entities().definition(entityType);
    JsonNode whereNode = jsonNode.get("where");
    Condition where = entityObjectMapper.deserializeCondition(definition, whereNode);
    JsonNode havingNode = jsonNode.get("having");
    Condition having = entityObjectMapper.deserializeCondition(definition, havingNode);

    return Count.builder(where)
            .having(having)
            .build();
  }
}

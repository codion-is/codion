/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.db.condition.EntityCondition;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityType;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

final class EntityConditionDeserializer extends StdDeserializer<EntityCondition> {

  private static final long serialVersionUID = 1;

  private final ConditionDeserializer conditionDeserializer;
  private final Entities entities;

  EntityConditionDeserializer(final EntityObjectMapper entityObjectMapper) {
    super(EntityCondition.class);
    this.conditionDeserializer = new ConditionDeserializer(entityObjectMapper);
    this.entities = entityObjectMapper.getEntities();
  }

  @Override
  public EntityCondition deserialize(final JsonParser parser, final DeserializationContext ctxt) throws IOException {
    final JsonNode entityConditionNode = parser.getCodec().readTree(parser);
    final EntityType entityType = entities.getDomainType().entityType(entityConditionNode.get("entityType").asText());
    final JsonNode conditionNode = entityConditionNode.get("condition");

    final Condition condition = conditionDeserializer.deserialize(entities.getDefinition(entityType), conditionNode);

    return Conditions.condition(entityType, condition);
  }
}

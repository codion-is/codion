/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json.db;

import org.jminor.framework.db.condition.Condition;
import org.jminor.framework.db.condition.Conditions;
import org.jminor.framework.db.condition.EntityCondition;
import org.jminor.framework.domain.EntityDefinition;
import org.jminor.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

final class EntityConditionDeserializer extends StdDeserializer<EntityCondition> {

  private static final long serialVersionUID = 1;

  private final ConditionDeserializer conditionDeserializer;
  private final EntityDefinition.Provider definitionProvider;

  EntityConditionDeserializer(final EntityObjectMapper entityObjectMapper) {
    super(EntityCondition.class);
    this.conditionDeserializer = new ConditionDeserializer(entityObjectMapper);
    this.definitionProvider = entityObjectMapper.getDomain();
  }

  @Override
  public EntityCondition deserialize(final JsonParser parser, final DeserializationContext ctxt) throws IOException {
    final JsonNode entityConditionNode = parser.getCodec().readTree(parser);
    final String entityId = entityConditionNode.get("entityId").asText();
    final JsonNode conditionNode = entityConditionNode.get("condition");

    final Condition condition = conditionDeserializer.deserialize(
            definitionProvider.getDefinition(entityId), conditionNode);

    return Conditions.entityCondition(entityId, condition);
  }
}

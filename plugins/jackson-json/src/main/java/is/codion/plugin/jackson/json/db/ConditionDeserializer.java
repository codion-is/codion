/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

final class ConditionDeserializer extends StdDeserializer<Condition> {

  private static final long serialVersionUID = 1;

  private final Entities entities;

  private final AttributeConditionDeserializer attributeConditionDeserializer;
  private final ConditionCombinationDeserializer conditionCombinationDeserializer;
  private final CustomConditionDeserializer customConditionDeserializer;

  ConditionDeserializer(final EntityObjectMapper entityObjectMapper) {
    super(Condition.class);
    this.entities = entityObjectMapper.getEntities();
    this.attributeConditionDeserializer = new AttributeConditionDeserializer(entityObjectMapper);
    this.conditionCombinationDeserializer = new ConditionCombinationDeserializer(this);
    this.customConditionDeserializer = new CustomConditionDeserializer(entityObjectMapper);
  }

  @Override
  public Condition deserialize(final JsonParser parser, final DeserializationContext ctxt) throws IOException {
    final JsonNode entityConditionNode = parser.getCodec().readTree(parser);
    final EntityType entityType = entities.getDomainType().entityType(entityConditionNode.get("entityType").asText());
    final JsonNode conditionNode = entityConditionNode.get("condition");

    return deserialize(entities.getDefinition(entityType), conditionNode);
  }

  Condition deserialize(final EntityDefinition definition, final JsonNode conditionNode) throws IOException {
    final JsonNode type = conditionNode.get("type");
    final String typeString = type.asText();
    if ("combination".equals(typeString)) {
      return conditionCombinationDeserializer.deserialize(definition, conditionNode);
    }
    else if ("attribute".equals(typeString)) {
      return attributeConditionDeserializer.deserialize(definition, conditionNode);
    }
    else if ("custom".equals(typeString)) {
      return customConditionDeserializer.deserialize(definition, conditionNode);
    }
    else if ("empty".equals(typeString)) {
      return Conditions.condition(definition.getEntityType());
    }

    throw new IllegalArgumentException("Unknown condition type: " + type);
  }
}

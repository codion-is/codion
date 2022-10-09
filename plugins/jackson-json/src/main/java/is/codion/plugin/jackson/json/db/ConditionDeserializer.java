/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.framework.db.condition.Condition;
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

  ConditionDeserializer(EntityObjectMapper entityObjectMapper) {
    super(Condition.class);
    this.entities = entityObjectMapper.entities();
    this.attributeConditionDeserializer = new AttributeConditionDeserializer(entityObjectMapper);
    this.conditionCombinationDeserializer = new ConditionCombinationDeserializer(this);
    this.customConditionDeserializer = new CustomConditionDeserializer(entityObjectMapper);
  }

  @Override
  public Condition deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
    JsonNode entityConditionNode = parser.getCodec().readTree(parser);
    EntityType entityType = entities.domainType().entityType(entityConditionNode.get("entityType").asText());
    JsonNode conditionNode = entityConditionNode.get("condition");

    return deserialize(entities.definition(entityType), conditionNode);
  }

  Condition deserialize(EntityDefinition definition, JsonNode conditionNode) throws IOException {
    JsonNode type = conditionNode.get("type");
    String typeString = type.asText();
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
      return Condition.condition(definition.type());
    }

    throw new IllegalArgumentException("Unknown condition type: " + type);
  }
}

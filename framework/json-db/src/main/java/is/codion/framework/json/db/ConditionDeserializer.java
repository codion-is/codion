/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.condition.Condition;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

import static is.codion.framework.db.condition.Condition.where;

public class ConditionDeserializer extends StdDeserializer<Condition> {

  private static final long serialVersionUID = 1;

  private final CriteriaDeserializer criteriaDeserializer;
  private final Entities entities;

  ConditionDeserializer(EntityObjectMapper entityObjectMapper) {
    super(Condition.class);
    this.criteriaDeserializer = new CriteriaDeserializer(entityObjectMapper);
    this.entities = entityObjectMapper.entities();
  }

  @Override
  public Condition deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
    JsonNode jsonNode = parser.getCodec().readTree(parser);
    EntityType entityType = entities.domainType().entityType(jsonNode.get("entityType").asText());
    EntityDefinition definition = entities.definition(entityType);
    JsonNode criteriaNode = jsonNode.get("criteria");

    return where(criteriaDeserializer.deserialize(definition, criteriaNode));
  }
}

/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.condition.Criteria;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

import static java.util.Objects.requireNonNull;

final class CriteriaDeserializer extends StdDeserializer<Criteria> {

  private static final long serialVersionUID = 1;

  final EntityObjectMapper entityObjectMapper;
  final Entities entities;

  private final AttributeCriteriaDeserializer attributeCriteriaDeserializer;
  private final CriteriaCombinationDeserializer criteriaCombinationDeserializer;
  private final CustomCriteriaDeserializer customCriteriaDeserializer;

  CriteriaDeserializer(EntityObjectMapper entityObjectMapper) {
    super(Criteria.class);
    this.entityObjectMapper = requireNonNull(entityObjectMapper);
    this.attributeCriteriaDeserializer = new AttributeCriteriaDeserializer(entityObjectMapper);
    this.criteriaCombinationDeserializer = new CriteriaCombinationDeserializer(this);
    this.customCriteriaDeserializer = new CustomCriteriaDeserializer(entityObjectMapper);
    this.entities = entityObjectMapper.entities();
  }

  @Override
  public Criteria deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
    JsonNode conditionNode = parser.getCodec().readTree(parser);
    EntityType entityType = entities.domainType().entityType(conditionNode.get("entityType").asText());
    JsonNode criteriaNode = conditionNode.get("criteria");

    return deserialize(entities.definition(entityType), criteriaNode);
  }

  Criteria deserialize(EntityDefinition definition, JsonNode criteriaNode) throws IOException {
    JsonNode type = criteriaNode.get("type");
    String typeString = type.asText();
    if ("combination".equals(typeString)) {
      return criteriaCombinationDeserializer.deserialize(definition, criteriaNode);
    }
    else if ("attribute".equals(typeString)) {
      return attributeCriteriaDeserializer.deserialize(definition, criteriaNode);
    }
    else if ("custom".equals(typeString)) {
      return customCriteriaDeserializer.deserialize(definition, criteriaNode);
    }
    else if ("all".equals(typeString)) {
      return Criteria.all(definition.type());
    }

    throw new IllegalArgumentException("Unknown condition type: " + type);
  }
}

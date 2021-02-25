/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.common.db.Operator;
import is.codion.framework.db.condition.AttributeCondition;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.property.Property;
import is.codion.plugin.jackson.json.domain.EntityDeserializer;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

final class AttributeConditionDeserializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  AttributeConditionDeserializer(final EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = entityObjectMapper;
  }

  <T> AttributeCondition<T> deserialize(final EntityDefinition definition, final JsonNode conditionNode) throws IOException {
    final String attributeName = conditionNode.get("attribute").asText();
    final Property<T> property = definition.getProperty(definition.getAttribute(attributeName));
    final JsonNode valuesNode = conditionNode.get("values");
    final List<T> values = new ArrayList<>();
    for (final JsonNode valueNode : valuesNode) {
      if (valueNode.has("entityType")) {
        values.add((T) entityObjectMapper.readValue(valueNode.toString(), Entity.class));
      }
      else {
        values.add((T) EntityDeserializer.parseValue(entityObjectMapper, property.getAttribute(), valueNode));
      }
    }
    final AttributeCondition.Builder<T> builder = Conditions.condition(property.getAttribute());
    switch (Operator.valueOf(conditionNode.get("operator").asText())) {
      case EQUAL:
        return builder.equalTo(values);
      case NOT_EQUAL:
        return builder.notEqualTo(values);
      case LESS_THAN:
        return builder.lessThan(values.get(0));
      case LESS_THAN_OR_EQUAL:
        return builder.lessThanOrEqualTo(values.get(0));
      case GREATER_THAN:
        return builder.greaterThan(values.get(0));
      case GREATER_THAN_OR_EQUAL:
        return builder.greaterThanOrEqualTo(values.get(0));
      case BETWEEN_EXCLUSIVE:
        return builder.betweenExclusive(values.get(0), values.get(1));
      case BETWEEN:
        return builder.between(values.get(0), values.get(1));
      case NOT_BETWEEN_EXCLUSIVE:
        return builder.notBetweenExclusive(values.get(0), values.get(1));
      case NOT_BETWEEN:
        return builder.notBetween(values.get(0), values.get(1));
      default:
        throw new IllegalArgumentException("Unknown operator: " + Operator.valueOf(conditionNode.get("operator").asText()));
    }
  }
}

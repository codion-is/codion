/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.common.Operator;
import is.codion.framework.db.condition.AttributeCondition;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.property.Property;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class AttributeConditionDeserializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  AttributeConditionDeserializer(EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = entityObjectMapper;
  }

  <T> AttributeCondition<T> deserialize(EntityDefinition definition, JsonNode conditionNode) throws IOException {
    String attributeName = conditionNode.get("attribute").asText();
    Property<T> property = definition.property(definition.attribute(attributeName));
    boolean caseSensitive = conditionNode.get("caseSensitive").asBoolean();
    JsonNode valuesNode = conditionNode.get("values");
    List<T> values = new ArrayList<>();
    for (JsonNode valueNode : valuesNode) {
      values.add(entityObjectMapper.readValue(valueNode.toString(), property.attribute().valueClass()));
    }
    AttributeCondition.Builder<T> builder = Condition.where(property.attribute());
    switch (Operator.valueOf(conditionNode.get("operator").asText())) {
      case EQUAL:
        if (values.isEmpty()) {
          return builder.isNull();
        }
        if (caseSensitive) {
          return builder.in(values);
        }

        return (AttributeCondition<T>) builder.inIgnoreCase((Collection<String>) values);
      case NOT_EQUAL:
        if (values.isEmpty()) {
          return builder.isNotNull();
        }
        if (caseSensitive) {
          return builder.notIn(values);
        }

        return (AttributeCondition<T>) builder.notInIgnoreCase((Collection<String>) values);
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

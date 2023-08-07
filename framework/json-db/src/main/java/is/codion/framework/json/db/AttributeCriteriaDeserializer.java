/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.common.Operator;
import is.codion.framework.db.condition.AttributeCriteria;
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

import static java.util.Objects.requireNonNull;

final class AttributeCriteriaDeserializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  AttributeCriteriaDeserializer(EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = requireNonNull(entityObjectMapper);
  }

  <T> AttributeCriteria<T> deserialize(EntityDefinition definition, JsonNode conditionNode) throws IOException {
    String attributeName = conditionNode.get("attribute").asText();
    Property<T> property = definition.property(definition.attribute(attributeName));
    boolean caseSensitive = conditionNode.get("caseSensitive").asBoolean();
    JsonNode valuesNode = conditionNode.get("values");
    List<T> values = new ArrayList<>();
    for (JsonNode valueNode : valuesNode) {
      values.add(entityObjectMapper.readValue(valueNode.toString(), property.attribute().valueClass()));
    }
    AttributeCriteria.Builder<T> builder = Condition.attribute(property.attribute());
    switch (Operator.valueOf(conditionNode.get("operator").asText())) {
      case EQUAL:
        return equalAttributeCondition(values, builder, caseSensitive);
      case NOT_EQUAL:
        return notEqualAttributeCondition(values, builder, caseSensitive);
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

  private static <T> AttributeCriteria<T> equalAttributeCondition(List<T> values, AttributeCriteria.Builder<T> builder,
                                                                  boolean caseSensitive) {
    if (values.isEmpty()) {
      return builder.isNull();
    }
    if (caseSensitive) {
      return caseSensitiveEqualAttributeCondition(values, builder);
    }

    return caseInsitiveEqualAttributeCondition(values, builder);
  }

  private static <T> AttributeCriteria<T> notEqualAttributeCondition(List<T> values, AttributeCriteria.Builder<T> builder,
                                                                     boolean caseSensitive) {
    if (values.isEmpty()) {
      return builder.isNotNull();
    }
    if (caseSensitive) {
      return caseSensitiveNotEqualAttributeCondition(values, builder);
    }

    return caseInsensitiveNotEqualAttributeCondition(values, builder);
  }

  private static <T> AttributeCriteria<T> caseSensitiveEqualAttributeCondition(List<T> values, AttributeCriteria.Builder<T> builder) {
    if (values.size() == 1) {
      return builder.equalTo(values.iterator().next());
    }

    return builder.in(values);
  }

  private static <T> AttributeCriteria<T> caseSensitiveNotEqualAttributeCondition(List<T> values, AttributeCriteria.Builder<T> builder) {
    if (values.size() == 1) {
      return builder.notEqualTo(values.iterator().next());
    }

    return builder.notIn(values);
  }

  private static <T> AttributeCriteria<T> caseInsitiveEqualAttributeCondition(List<T> values, AttributeCriteria.Builder<T> builder) {
    if (values.size() == 1) {
      return (AttributeCriteria<T>) builder.equalToIgnoreCase((String) values.iterator().next());
    }

    return (AttributeCriteria<T>) builder.inIgnoreCase((Collection<String>) values);
  }

  private static <T> AttributeCriteria<T> caseInsensitiveNotEqualAttributeCondition(List<T> values, AttributeCriteria.Builder<T> builder) {
    if (values.size() == 1) {
      return (AttributeCriteria<T>) builder.notEqualToIgnoreCase((String) values.iterator().next());
    }

    return (AttributeCriteria<T>) builder.notInIgnoreCase((Collection<String>) values);
  }
}

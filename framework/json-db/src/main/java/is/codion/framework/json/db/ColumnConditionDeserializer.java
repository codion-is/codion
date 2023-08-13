/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.common.Operator;
import is.codion.framework.db.condition.ColumnCondition;
import is.codion.framework.domain.entity.Column;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static is.codion.framework.db.condition.Condition.column;
import static java.util.Objects.requireNonNull;

final class ColumnConditionDeserializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  ColumnConditionDeserializer(EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = requireNonNull(entityObjectMapper);
  }

  <T> ColumnCondition<T> deserialize(EntityDefinition definition, JsonNode conditionNode) throws IOException {
    String columnName = conditionNode.get("column").asText();
    ColumnProperty<T> property = definition.columnProperty((Column<T>) definition.attribute(columnName));
    boolean caseSensitive = conditionNode.get("caseSensitive").asBoolean();
    JsonNode valuesNode = conditionNode.get("values");
    List<T> values = new ArrayList<>();
    for (JsonNode valueNode : valuesNode) {
      values.add(entityObjectMapper.readValue(valueNode.toString(), property.attribute().valueClass()));
    }
    ColumnCondition.Builder<T> builder = column(property.attribute());
    switch (Operator.valueOf(conditionNode.get("operator").asText())) {
      case EQUAL:
        return equalColumnCondition(values, builder, caseSensitive);
      case NOT_EQUAL:
        return notEqualColumnCondition(values, builder, caseSensitive);
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

  private static <T> ColumnCondition<T> equalColumnCondition(List<T> values, ColumnCondition.Builder<T> builder,
                                                             boolean caseSensitive) {
    if (values.isEmpty()) {
      return builder.isNull();
    }
    if (caseSensitive) {
      return caseSensitiveEqualColumnCondition(values, builder);
    }

    return caseInsitiveEqualColumnCondition(values, builder);
  }

  private static <T> ColumnCondition<T> notEqualColumnCondition(List<T> values, ColumnCondition.Builder<T> builder,
                                                                boolean caseSensitive) {
    if (values.isEmpty()) {
      return builder.isNotNull();
    }
    if (caseSensitive) {
      return caseSensitiveNotEqualColumnCondition(values, builder);
    }

    return caseInsensitiveNotEqualColumnCondition(values, builder);
  }

  private static <T> ColumnCondition<T> caseSensitiveEqualColumnCondition(List<T> values, ColumnCondition.Builder<T> builder) {
    if (values.size() == 1) {
      return builder.equalTo(values.iterator().next());
    }

    return builder.in(values);
  }

  private static <T> ColumnCondition<T> caseSensitiveNotEqualColumnCondition(List<T> values, ColumnCondition.Builder<T> builder) {
    if (values.size() == 1) {
      return builder.notEqualTo(values.iterator().next());
    }

    return builder.notIn(values);
  }

  private static <T> ColumnCondition<T> caseInsitiveEqualColumnCondition(List<T> values, ColumnCondition.Builder<T> builder) {
    if (values.size() == 1) {
      return (ColumnCondition<T>) builder.equalToIgnoreCase((String) values.iterator().next());
    }

    return (ColumnCondition<T>) builder.inIgnoreCase((Collection<String>) values);
  }

  private static <T> ColumnCondition<T> caseInsensitiveNotEqualColumnCondition(List<T> values, ColumnCondition.Builder<T> builder) {
    if (values.size() == 1) {
      return (ColumnCondition<T>) builder.notEqualToIgnoreCase((String) values.iterator().next());
    }

    return (ColumnCondition<T>) builder.notInIgnoreCase((Collection<String>) values);
  }
}

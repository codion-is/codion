/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.CustomCondition;
import is.codion.framework.domain.entity.Column;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.property.Property;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class CustomConditionDeserializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  CustomConditionDeserializer(EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = requireNonNull(entityObjectMapper);
  }

  CustomCondition deserialize(EntityDefinition definition, JsonNode conditionNode) throws IOException {
    String conditionTypeName = conditionNode.get("conditionType").asText();
    JsonNode columnsNode = conditionNode.get("columns");
    List<Column<?>> columns = Arrays.stream(entityObjectMapper.readValue(columnsNode.toString(), String[].class))
            .map(definition::attribute)
            .map(attribute -> (Column<?>) attribute)
            .collect(toList());
    JsonNode valuesNode = conditionNode.get("values");
    List<Object> values = new ArrayList<>();
    int attributeIndex = 0;
    for (JsonNode valueNode : valuesNode) {
      Property<?> property = definition.property(columns.get(attributeIndex++));
      values.add(entityObjectMapper.readValue(valueNode.toString(), property.attribute().valueClass()));
    }

    return Condition.customCondition(definition.type().conditionType(conditionTypeName), columns, values);
  }
}

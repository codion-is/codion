/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.framework.db.condition.Conditions;
import is.codion.framework.db.condition.CustomCondition;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.property.Property;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

final class CustomConditionDeserializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  CustomConditionDeserializer(EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = entityObjectMapper;
  }

  CustomCondition deserialize(EntityDefinition definition, JsonNode conditionNode) throws IOException {
    String conditionTypeName = conditionNode.get("conditionTypeName").asText();
    JsonNode attributesNode = conditionNode.get("attributes");
    List<String> attributeNames = Arrays.asList(entityObjectMapper.readValue(attributesNode.toString(), String[].class));
    List<Attribute<?>> attributes = attributeNames.stream()
            .map(definition::getAttribute)
            .collect(toList());
    JsonNode valuesNode = conditionNode.get("values");
    List<Object> values = new ArrayList<>();
    int attributeIndex = 0;
    for (JsonNode valueNode : valuesNode) {
      Property<?> property = definition.getProperty(attributes.get(attributeIndex++));
      values.add(entityObjectMapper.readValue(valueNode.toString(), property.attribute().valueClass()));
    }

    return Conditions.customCondition(definition.getEntityType().conditionType(conditionTypeName), attributes, values);
  }
}

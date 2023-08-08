/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.CustomCriteria;
import is.codion.framework.domain.entity.Attribute;
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

final class CustomCriteriaDeserializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  CustomCriteriaDeserializer(EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = requireNonNull(entityObjectMapper);
  }

  CustomCriteria deserialize(EntityDefinition definition, JsonNode conditionNode) throws IOException {
    String criteriaTypeName = conditionNode.get("criteriaType").asText();
    JsonNode attributesNode = conditionNode.get("attributes");
    List<Attribute<?>> attributes = Arrays.stream(entityObjectMapper.readValue(attributesNode.toString(), String[].class))
            .map(definition::attribute)
            .collect(toList());
    JsonNode valuesNode = conditionNode.get("values");
    List<Object> values = new ArrayList<>();
    int attributeIndex = 0;
    for (JsonNode valueNode : valuesNode) {
      Property<?> property = definition.property(attributes.get(attributeIndex++));
      values.add(entityObjectMapper.readValue(valueNode.toString(), property.attribute().valueClass()));
    }

    return Condition.customCriteria(definition.type().criteriaType(criteriaTypeName), attributes, values);
  }
}

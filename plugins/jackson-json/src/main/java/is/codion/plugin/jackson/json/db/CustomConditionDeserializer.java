/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.framework.db.condition.Conditions;
import is.codion.framework.db.condition.CustomCondition;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.property.Property;
import is.codion.plugin.jackson.json.domain.EntityDeserializer;
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

  CustomConditionDeserializer(final EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = entityObjectMapper;
  }

  CustomCondition deserialize(final EntityDefinition definition, final JsonNode conditionNode) throws IOException {
    final String conditionTypeName = conditionNode.get("conditionTypeName").asText();
    final JsonNode attributesNode = conditionNode.get("attributes");
    final List<String> attributeNames = Arrays.asList(entityObjectMapper.readValue(attributesNode.toString(), String[].class));
    final List<Attribute<?>> attributes = attributeNames.stream()
            .map(definition::getAttribute)
            .collect(toList());
    final JsonNode valuesNode = conditionNode.get("values");
    final List<Object> values = new ArrayList<>();
    int attributeIndex = 0;
    for (final JsonNode valueNode : valuesNode) {
      final Property<?> property = definition.getProperty(attributes.get(attributeIndex++));
      if (valueNode.isNull()) {
        values.add(null);
      }
      else if (valueNode.has("entityType")) {
        values.add(entityObjectMapper.readValue(valueNode.toString(), Key.class));
      }
      else {
        values.add(EntityDeserializer.parseValue(entityObjectMapper, property.getAttribute(), valueNode));
      }
    }

    return Conditions.customCondition(definition.getEntityType().conditionType(conditionTypeName), attributes, values);
  }
}

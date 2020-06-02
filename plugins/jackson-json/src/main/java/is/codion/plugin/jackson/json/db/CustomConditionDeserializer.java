/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.framework.db.condition.Conditions;
import is.codion.framework.db.condition.CustomCondition;
import is.codion.framework.domain.attribute.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.property.Property;
import is.codion.plugin.jackson.json.domain.EntityDeserializer;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

final class CustomConditionDeserializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  CustomConditionDeserializer(final EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = entityObjectMapper;
  }

  CustomCondition deserialize(final EntityDefinition definition, final JsonNode conditionNode) throws IOException {
    final String conditionId = conditionNode.get("conditionId").asText();
    final JsonNode attributesNode = conditionNode.get("attributes");
    final List<String> attributeNames = Arrays.asList(entityObjectMapper.readValue(attributesNode.toString(), String[].class));
    final List<Attribute<?>> attributes = attributeNames.stream().map(name -> definition.getEntityType().objectAttribute(name)).collect(Collectors.toList());
    final JsonNode valuesNode = conditionNode.get("values");
    final List<Object> values = new ArrayList<>();
    int attributeIndex = 0;
    for (final JsonNode valueNode : valuesNode) {
      final Property<?> property = definition.getProperty(attributes.get(attributeIndex++));
      if (valueNode.isNull()) {
        values.add(null);
      }
      else if (valueNode.has("entityType")) {
        values.add(entityObjectMapper.readValue(valueNode.toString(), Entity.Key.class));
      }
      else {
        values.add(EntityDeserializer.parseValue(entityObjectMapper, property.getAttribute(), valueNode));
      }
    }

    return Conditions.customCondition(conditionId, attributes, values);
  }
}

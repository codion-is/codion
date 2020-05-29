/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.framework.db.condition.Conditions;
import is.codion.framework.db.condition.CustomCondition;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.Properties;
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
    final JsonNode propertyIdsNode = conditionNode.get("propertyIds");
    final List<String> attributeIds = Arrays.asList(entityObjectMapper.readValue(propertyIdsNode.toString(), String[].class));
    final List<Attribute<?>> attributes = attributeIds.stream().map(Properties::attribute).collect(Collectors.toList());
    final JsonNode valuesNode = conditionNode.get("values");
    final List values = new ArrayList();
    int propertyIdIndex = 0;
    for (final JsonNode valueNode : valuesNode) {
      final Property property = definition.getProperty(attributes.get(propertyIdIndex++));
      if (valueNode.isNull()) {
        values.add(null);
      }
      else if (valueNode.has("entityId")) {
        values.add(entityObjectMapper.readValue(valueNode.toString(), Entity.Key.class));
      }
      else {
        values.add(EntityDeserializer.parseValue(entityObjectMapper, property, valueNode));
      }
    }

    return Conditions.customCondition(conditionId, attributes, values);
  }
}

/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json.db;

import org.jminor.framework.db.condition.Conditions;
import org.jminor.framework.db.condition.CustomCondition;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityDefinition;
import org.jminor.framework.domain.property.Property;
import org.jminor.plugin.jackson.json.domain.EntityDeserializer;
import org.jminor.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class CustomConditionDeserializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  CustomConditionDeserializer(final EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = entityObjectMapper;
  }

  CustomCondition deserialize(final EntityDefinition definition, final JsonNode conditionNode) throws IOException {
    final String conditionId = conditionNode.get("conditionId").asText();
    final JsonNode propertyIdsNode = conditionNode.get("propertyIds");
    final List<String> propertyIds = Arrays.asList(entityObjectMapper.readValue(propertyIdsNode.toString(), String[].class));
    final JsonNode valuesNode = conditionNode.get("values");
    final List values = new ArrayList();
    int propertyIdIndex = 0;
    for (final JsonNode valueNode : valuesNode) {
      final Property property = definition.getProperty(propertyIds.get(propertyIdIndex++));
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

    return Conditions.customCondition(conditionId, propertyIds, values);
  }
}

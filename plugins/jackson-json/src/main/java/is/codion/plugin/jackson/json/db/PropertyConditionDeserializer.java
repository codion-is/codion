/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.plugin.jackson.json.db;

import dev.codion.common.db.Operator;
import dev.codion.framework.db.condition.Conditions;
import dev.codion.framework.db.condition.PropertyCondition;
import dev.codion.framework.domain.entity.Entity;
import dev.codion.framework.domain.entity.EntityDefinition;
import dev.codion.framework.domain.property.Property;
import dev.codion.plugin.jackson.json.domain.EntityDeserializer;
import dev.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

final class PropertyConditionDeserializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  PropertyConditionDeserializer(final EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = entityObjectMapper;
  }

  PropertyCondition deserialize(final EntityDefinition definition, final JsonNode conditionNode) throws IOException {
    final Property property = definition.getProperty(conditionNode.get("propertyId").asText());
    final JsonNode valuesNode = conditionNode.get("values");
    final List values = new ArrayList();
    for (final JsonNode valueNode : valuesNode) {
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
    final boolean nullCondition = values.isEmpty();

    return Conditions.propertyCondition(conditionNode.get("propertyId").asText(),
            Operator.valueOf(conditionNode.get("operator").asText()), nullCondition ? null : values);
  }
}

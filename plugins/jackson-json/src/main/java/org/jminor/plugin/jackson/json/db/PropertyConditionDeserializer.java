/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json.db;

import org.jminor.common.db.ConditionType;
import org.jminor.framework.db.condition.Conditions;
import org.jminor.framework.db.condition.PropertyCondition;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.Property;
import org.jminor.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class PropertyConditionDeserializer {

  private final EntityObjectMapper entityObjectMapper;

  public PropertyConditionDeserializer(final EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = entityObjectMapper;
  }

  public PropertyCondition deserialize(final Entity.Definition definition, final JsonNode conditionNode,
                                       final DeserializationContext ctxt) throws IOException, JsonProcessingException {
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
        values.add(entityObjectMapper.getEntityDeserializer().parseValue(property, valueNode));
      }
    }

    return Conditions.propertyCondition(conditionNode.get("propertyId").asText(),
            ConditionType.valueOf(conditionNode.get("conditionType").asText()), values);
  }
}

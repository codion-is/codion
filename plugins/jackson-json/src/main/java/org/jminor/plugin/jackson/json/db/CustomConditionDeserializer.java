/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json.db;

import org.jminor.framework.db.condition.Conditions;
import org.jminor.framework.db.condition.CustomCondition;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.Property;
import org.jminor.plugin.jackson.json.domain.EntityDeserializer;
import org.jminor.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class CustomConditionDeserializer {

  private static final TypeReference<List<String>> STRING_LIST_TYPE_REF = new TypeReference<List<String>>() {};
  private final EntityObjectMapper entityObjectMapper;

  public CustomConditionDeserializer(final EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = entityObjectMapper;
  }

  public CustomCondition deserialize(final Entity.Definition definition, final JsonNode conditionNode)
          throws IOException, JsonProcessingException {
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

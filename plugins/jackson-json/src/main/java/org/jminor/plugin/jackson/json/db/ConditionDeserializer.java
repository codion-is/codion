/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json.db;

import org.jminor.framework.db.condition.Condition;
import org.jminor.framework.domain.EntityDefinition;
import org.jminor.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.Serializable;

final class ConditionDeserializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final PropertyConditionDeserializer propertyConditionDeserializer;
  private final ConditionSetDeserializer conditionSetDeserializer;
  private final CustomConditionDeserializer customConditionDeserializer;

  ConditionDeserializer(final EntityObjectMapper entityObjectMapper) {
    this.propertyConditionDeserializer = new PropertyConditionDeserializer(entityObjectMapper);
    this.conditionSetDeserializer = new ConditionSetDeserializer(this);
    this.customConditionDeserializer = new CustomConditionDeserializer(entityObjectMapper);
  }

  Condition deserialize(final EntityDefinition definition, final JsonNode conditionNode) throws IOException {
    final JsonNode type = conditionNode.get("type");
    final String typeString = type.asText();
    if ("set".equals(typeString)) {
      return conditionSetDeserializer.deserialize(definition, conditionNode);
    }
    else if ("property".equals(typeString)) {
      return propertyConditionDeserializer.deserialize(definition, conditionNode);
    }
    else if ("custom".equals(typeString)) {
      return customConditionDeserializer.deserialize(definition, conditionNode);
    }

    throw new IllegalArgumentException("Unknown condition type: " + type);
  }
}

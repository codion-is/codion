/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.framework.db.condition.Condition;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.Serializable;

final class ConditionDeserializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final PropertyConditionDeserializer propertyConditionDeserializer;
  private final ConditionCombinationDeserializer conditionCombinationDeserializer;
  private final CustomConditionDeserializer customConditionDeserializer;

  ConditionDeserializer(final EntityObjectMapper entityObjectMapper) {
    this.propertyConditionDeserializer = new PropertyConditionDeserializer(entityObjectMapper);
    this.conditionCombinationDeserializer = new ConditionCombinationDeserializer(this);
    this.customConditionDeserializer = new CustomConditionDeserializer(entityObjectMapper);
  }

  Condition deserialize(final EntityDefinition definition, final JsonNode conditionNode) throws IOException {
    final JsonNode type = conditionNode.get("type");
    final String typeString = type.asText();
    if ("combination".equals(typeString)) {
      return conditionCombinationDeserializer.deserialize(definition, conditionNode);
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

/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json.db;

import org.jminor.framework.db.condition.Condition;
import org.jminor.framework.domain.Entity;
import org.jminor.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

final class ConditionDeserializer {

  private final ConditionSetDeserializer conditionSetDeserializer;
  private final PropertyConditionDeserializer propertyConditionDeserializer;

  public ConditionDeserializer(final EntityObjectMapper entityObjectMapper) {
    this.conditionSetDeserializer = new ConditionSetDeserializer(this);
    this.propertyConditionDeserializer = new PropertyConditionDeserializer(entityObjectMapper);
  }

  public Condition deserialize(final Entity.Definition definition, final JsonNode conditionNode, final DeserializationContext ctxt)
          throws IOException, JsonProcessingException {
    final JsonNode type = conditionNode.get("type");
    final String typeString = type.asText();
    if (typeString.equals("set")) {
      return conditionSetDeserializer.deserialize(definition, conditionNode, ctxt);
    }
    else if (typeString.equals("property")) {
      return propertyConditionDeserializer.deserialize(definition, conditionNode, ctxt);
    }

    throw new IllegalArgumentException("Unknown condition type: " + type);
  }
}

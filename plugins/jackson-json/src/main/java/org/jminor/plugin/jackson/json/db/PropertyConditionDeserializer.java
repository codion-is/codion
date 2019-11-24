/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json.db;

import org.jminor.common.db.ConditionType;
import org.jminor.framework.db.condition.Conditions;
import org.jminor.framework.db.condition.PropertyCondition;
import org.jminor.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.List;

final class PropertyConditionDeserializer extends StdDeserializer<PropertyCondition> {

  private final EntityObjectMapper entityObjectMapper;

  public PropertyConditionDeserializer(final EntityObjectMapper entityObjectMapper) {
    super(PropertyCondition.class);
    this.entityObjectMapper = entityObjectMapper;
  }

  @Override
  public PropertyCondition deserialize(final JsonParser parser, final DeserializationContext ctxt)
          throws IOException, JsonProcessingException {
    final JsonNode conditionNode = parser.getCodec().readTree(parser);
    final JsonNode valueNode = conditionNode.get("values");
    final List values = entityObjectMapper.readValue(valueNode.toString(), List.class);

    return Conditions.propertyCondition(conditionNode.get("propertyId").asText(),
            ConditionType.valueOf(conditionNode.get("conditionType").asText()), values);
  }
}

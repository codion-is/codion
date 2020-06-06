/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.common.db.Operator;
import is.codion.framework.db.condition.AttributeCondition;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.property.Property;
import is.codion.plugin.jackson.json.domain.EntityDeserializer;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

final class AttributeConditionDeserializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  AttributeConditionDeserializer(final EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = entityObjectMapper;
  }

  AttributeCondition deserialize(final EntityDefinition definition, final JsonNode conditionNode) throws IOException {
    final String attributeName = conditionNode.get("attribute").asText();
    final Property<?> property = definition.getProperty(definition.getEntityType().objectAttribute(attributeName));
    final JsonNode valuesNode = conditionNode.get("values");
    final List<Object> values = new ArrayList<>();
    for (final JsonNode valueNode : valuesNode) {
      if (valueNode.isNull()) {
        values.add(null);
      }
      else if (valueNode.has("entityType")) {
        values.add(entityObjectMapper.readValue(valueNode.toString(), Key.class));
      }
      else {
        values.add(EntityDeserializer.parseValue(entityObjectMapper, property.getAttribute(), valueNode));
      }
    }
    final boolean nullCondition = values.isEmpty();

    return Conditions.attributeCondition(definition.getEntityType().objectAttribute(conditionNode.get("attribute").asText()),
            Operator.valueOf(conditionNode.get("operator").asText()), nullCondition ? null : values);
  }
}

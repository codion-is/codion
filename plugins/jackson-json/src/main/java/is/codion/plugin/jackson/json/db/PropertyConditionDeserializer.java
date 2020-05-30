/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.common.db.Operator;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.db.condition.PropertyCondition;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.property.Property;
import is.codion.plugin.jackson.json.domain.EntityDeserializer;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import static is.codion.framework.domain.property.Properties.attribute;

final class PropertyConditionDeserializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  PropertyConditionDeserializer(final EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = entityObjectMapper;
  }

  PropertyCondition deserialize(final EntityDefinition definition, final JsonNode conditionNode) throws IOException {
    final String attributeName = conditionNode.get("attribute").asText();
    final Property property = definition.getProperty(attribute(attributeName, Types.OTHER));
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

    return Conditions.propertyCondition(attribute(conditionNode.get("attribute").asText(), Types.OTHER),
            Operator.valueOf(conditionNode.get("operator").asText()), nullCondition ? null : values);
  }
}

/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json.db;

import org.jminor.common.Conjunction;
import org.jminor.framework.db.condition.Condition;
import org.jminor.framework.db.condition.Conditions;
import org.jminor.framework.domain.entity.EntityDefinition;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class ConditionCombinationDeserializer {

  private final ConditionDeserializer conditionDeserializer;

  ConditionCombinationDeserializer(final ConditionDeserializer conditionDeserializer) {
    this.conditionDeserializer = conditionDeserializer;
  }

  Condition.Combination deserialize(final EntityDefinition definition, final JsonNode jsonNode) throws IOException {
    final Conjunction conjunction = Conjunction.valueOf(jsonNode.get("conjunction").asText());
    final JsonNode conditionsNode = jsonNode.get("conditions");
    final List<Condition> conditions = new ArrayList<>(conditionsNode.size());
    for (final JsonNode conditionNode : conditionsNode) {
      conditions.add(conditionDeserializer.deserialize(definition, conditionNode));
    }

    return Conditions.combination(conjunction, conditions);
  }
}

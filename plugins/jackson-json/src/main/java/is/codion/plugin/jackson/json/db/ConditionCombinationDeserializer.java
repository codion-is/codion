/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.common.Conjunction;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.domain.entity.EntityDefinition;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

final class ConditionCombinationDeserializer {

  private final ConditionDeserializer conditionDeserializer;

  ConditionCombinationDeserializer(ConditionDeserializer conditionDeserializer) {
    this.conditionDeserializer = conditionDeserializer;
  }

  Condition.Combination deserialize(EntityDefinition definition, JsonNode jsonNode) throws IOException {
    Conjunction conjunction = Conjunction.valueOf(jsonNode.get("conjunction").asText());
    JsonNode conditionsNode = jsonNode.get("conditions");
    Collection<Condition> conditions = new ArrayList<>(conditionsNode.size());
    for (JsonNode conditionNode : conditionsNode) {
      conditions.add(conditionDeserializer.deserialize(definition, conditionNode));
    }

    return Conditions.combination(conjunction, conditions);
  }
}

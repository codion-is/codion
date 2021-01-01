/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.common.Conjunction;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.domain.entity.EntityDefinition;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

final class ConditionCombinationDeserializer {

  private final ConditionDeserializer conditionDeserializer;

  ConditionCombinationDeserializer(final ConditionDeserializer conditionDeserializer) {
    this.conditionDeserializer = conditionDeserializer;
  }

  Condition.Combination deserialize(final EntityDefinition definition, final JsonNode jsonNode) throws IOException {
    final Conjunction conjunction = Conjunction.valueOf(jsonNode.get("conjunction").asText());
    final JsonNode conditionsNode = jsonNode.get("conditions");
    final Condition.Combination combination = Conditions.combination(conjunction);
    for (final JsonNode conditionNode : conditionsNode) {
      combination.add(conditionDeserializer.deserialize(definition, conditionNode));
    }

    return combination;
  }
}

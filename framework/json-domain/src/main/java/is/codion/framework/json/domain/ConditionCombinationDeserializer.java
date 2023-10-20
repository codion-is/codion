/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.domain;

import is.codion.common.Conjunction;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Condition;
import is.codion.framework.domain.entity.attribute.Condition.Combination;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import static java.util.Objects.requireNonNull;

final class ConditionCombinationDeserializer {

  private final ConditionDeserializer conditionDeserializer;

  ConditionCombinationDeserializer(ConditionDeserializer conditionDeserializer) {
    this.conditionDeserializer = requireNonNull(conditionDeserializer);
  }

  Combination deserialize(EntityDefinition definition, JsonNode jsonNode) throws IOException {
    Conjunction conjunction = Conjunction.valueOf(jsonNode.get("conjunction").asText());
    JsonNode conditionsNode = jsonNode.get("conditions");
    Collection<Condition> conditions = new ArrayList<>(conditionsNode.size());
    for (JsonNode conditionNode : conditionsNode) {
      conditions.add(conditionDeserializer.deserialize(definition, conditionNode));
    }

    return Condition.combination(conjunction, conditions);
  }
}

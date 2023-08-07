/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.common.Conjunction;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.Criteria;
import is.codion.framework.db.condition.Criteria.Combination;
import is.codion.framework.domain.entity.EntityDefinition;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import static java.util.Objects.requireNonNull;

final class CriteriaCombinationDeserializer {

  private final CriteriaDeserializer criteriaDeserializer;

  CriteriaCombinationDeserializer(CriteriaDeserializer criteriaDeserializer) {
    this.criteriaDeserializer = requireNonNull(criteriaDeserializer);
  }

  Combination deserialize(EntityDefinition definition, JsonNode jsonNode) throws IOException {
    Conjunction conjunction = Conjunction.valueOf(jsonNode.get("conjunction").asText());
    JsonNode criteriaNode = jsonNode.get("criteria");
    Collection<Criteria> criteria = new ArrayList<>(criteriaNode.size());
    for (JsonNode conditionNode : criteriaNode) {
      criteria.add(criteriaDeserializer.deserialize(definition, conditionNode));
    }

    return Condition.combination(conjunction, criteria);
  }
}

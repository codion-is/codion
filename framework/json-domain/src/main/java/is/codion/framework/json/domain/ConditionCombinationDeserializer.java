/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.json.domain;

import is.codion.common.Conjunction;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.domain.entity.condition.Condition.Combination;

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

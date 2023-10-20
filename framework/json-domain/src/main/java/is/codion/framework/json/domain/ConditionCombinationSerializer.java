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

import is.codion.framework.domain.entity.attribute.ColumnCondition;
import is.codion.framework.domain.entity.attribute.Condition;
import is.codion.framework.domain.entity.attribute.Condition.Combination;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.io.Serializable;

import static java.util.Objects.requireNonNull;

final class ConditionCombinationSerializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final ColumnConditionSerializer columnConditionSerializer;

  ConditionCombinationSerializer(ColumnConditionSerializer columnConditionSerializer) {
    this.columnConditionSerializer = requireNonNull(columnConditionSerializer);
  }

  void serialize(Combination combination, JsonGenerator generator) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("type", "combination");
    generator.writeStringField("conjunction", combination.conjunction().name());
    generator.writeArrayFieldStart("conditions");
    for (Condition condition : combination.conditions()) {
      if (condition instanceof Combination) {
        serialize((Combination) condition, generator);
      }
      else if (condition instanceof ColumnCondition) {
        columnConditionSerializer.serialize((ColumnCondition<?>) condition, generator);
      }
    }
    generator.writeEndArray();
    generator.writeEndObject();
  }
}

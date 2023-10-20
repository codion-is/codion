/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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

/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.framework.db.condition.AttributeCondition;
import is.codion.framework.db.condition.Condition;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.io.Serializable;

final class ConditionCombinationSerializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final AttributeConditionSerializer attributeConditionSerializer;

  ConditionCombinationSerializer(AttributeConditionSerializer attributeConditionSerializer) {
    this.attributeConditionSerializer = attributeConditionSerializer;
  }

  void serialize(Condition.Combination combination, JsonGenerator generator) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("type", "combination");
    generator.writeStringField("conjunction", combination.conjunction().name());
    generator.writeArrayFieldStart("conditions");
    for (Condition condition : combination.conditions()) {
      if (condition instanceof Condition.Combination) {
        serialize((Condition.Combination) condition, generator);
      }
      else if (condition instanceof AttributeCondition) {
        attributeConditionSerializer.serialize((AttributeCondition<?>) condition, generator);
      }
    }
    generator.writeEndArray();
    generator.writeEndObject();
  }
}

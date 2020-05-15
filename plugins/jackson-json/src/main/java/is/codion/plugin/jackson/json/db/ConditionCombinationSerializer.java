/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.plugin.jackson.json.db;

import dev.codion.framework.db.condition.Condition;
import dev.codion.framework.db.condition.PropertyCondition;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

final class ConditionCombinationSerializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final PropertyConditionSerializer propertyConditionSerializer;

  ConditionCombinationSerializer(final PropertyConditionSerializer propertyConditionSerializer) {
    this.propertyConditionSerializer = propertyConditionSerializer;
  }

  void serialize(final Condition.Combination combination, final JsonGenerator generator) throws IOException {
    generator.writeStartObject();
    generator.writeObjectField("type", "combination");
    generator.writeObjectField("conjunction", combination.getConjunction().toString());
    generator.writeArrayFieldStart("conditions");
    final List<Condition> conditions = combination.getConditions();
    for (final Condition condition : conditions) {
      if (condition instanceof Condition.Combination) {
        serialize((Condition.Combination) condition, generator);
      }
      else if (condition instanceof PropertyCondition) {
        propertyConditionSerializer.serialize((PropertyCondition) condition, generator);
      }
    }
    generator.writeEndArray();
    generator.writeEndObject();
  }
}

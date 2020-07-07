/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.framework.db.condition.AttributeCondition;
import is.codion.framework.db.condition.Condition;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

final class ConditionCombinationSerializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final AttributeConditionSerializer attributeConditionSerializer;

  ConditionCombinationSerializer(final AttributeConditionSerializer attributeConditionSerializer) {
    this.attributeConditionSerializer = attributeConditionSerializer;
  }

  void serialize(final Condition.Combination combination, final JsonGenerator generator) throws IOException {
    generator.writeStartObject();
    generator.writeObjectField("type", "combination");
    generator.writeObjectField("conjunction", combination.getConjunction().name());
    generator.writeArrayFieldStart("conditions");
    final List<Condition> conditions = combination.getConditions();
    for (final Condition condition : conditions) {
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

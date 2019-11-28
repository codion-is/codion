/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json.db;

import org.jminor.framework.db.condition.Condition;
import org.jminor.framework.db.condition.PropertyCondition;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

final class ConditionSetSerializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final PropertyConditionSerializer propertyConditionSerializer;

  ConditionSetSerializer(final PropertyConditionSerializer propertyConditionSerializer) {
    this.propertyConditionSerializer = propertyConditionSerializer;
  }

  void serialize(final Condition.Set set, final JsonGenerator generator) throws IOException {
    generator.writeStartObject();
    generator.writeObjectField("type", "set");
    generator.writeObjectField("conjunction", set.getConjunction().toString());
    generator.writeArrayFieldStart("conditions");
    final List<Condition> conditions = set.getConditions();
    for (final Condition condition : conditions) {
      if (condition instanceof Condition.Set) {
        serialize((Condition.Set) condition, generator);
      }
      else if (condition instanceof PropertyCondition) {
        propertyConditionSerializer.serialize((PropertyCondition) condition, generator);
      }
    }
    generator.writeEndArray();
    generator.writeEndObject();
  }
}

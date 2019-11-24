/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json.db;

import org.jminor.framework.db.condition.Condition;
import org.jminor.framework.db.condition.PropertyCondition;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.List;

final class ConditionSetSerializer {

  private final PropertyConditionSerializer propertyConditionSerializer;

  public ConditionSetSerializer(final PropertyConditionSerializer propertyConditionSerializer) {
    this.propertyConditionSerializer = propertyConditionSerializer;
  }

  public void serialize(final Condition.Set set, final JsonGenerator generator, final SerializerProvider provider)
          throws IOException {
    generator.writeStartObject();
    generator.writeObjectField("type", "set");
    generator.writeObjectField("conjunction", set.getConjunction().toString());
    generator.writeArrayFieldStart("conditions");
    final List<Condition> conditions = set.getConditions();
    for (final Condition condition : conditions) {
      if (condition instanceof Condition.Set) {
        serialize((Condition.Set) condition, generator, provider);
      }
      else if (condition instanceof PropertyCondition) {
        propertyConditionSerializer.serialize((PropertyCondition) condition, generator, provider);
      }
    }
    generator.writeEndArray();
    generator.writeEndObject();
  }
}

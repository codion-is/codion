/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json.db;

import org.jminor.framework.db.condition.Condition;
import org.jminor.framework.db.condition.PropertyCondition;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

final class ConditionSerializer {

  private final PropertyConditionSerializer propertyConditionSerializer;
  private final ConditionSetSerializer conditionSetSerializer;

  public ConditionSerializer(final PropertyConditionSerializer propertyConditionSerializer) {
    this.propertyConditionSerializer = propertyConditionSerializer;
    this.conditionSetSerializer = new ConditionSetSerializer(propertyConditionSerializer);
  }

  public void serialize(final Condition condition, final JsonGenerator generator, final SerializerProvider provider) throws IOException {
    if (condition instanceof Condition.Set) {
      final Condition.Set set = (Condition.Set) condition;
      conditionSetSerializer.serialize(set, generator, provider);
    }
    else if (condition instanceof PropertyCondition) {
      final PropertyCondition propertyCondition = (PropertyCondition) condition;
      propertyConditionSerializer.serialize(propertyCondition, generator, provider);
    }
    else {
      throw new IllegalArgumentException("Unknown Condition type: " + condition.getClass());
    }
  }
}

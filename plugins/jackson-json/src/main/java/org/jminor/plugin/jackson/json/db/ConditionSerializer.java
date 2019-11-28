/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json.db;

import org.jminor.framework.db.condition.Condition;
import org.jminor.framework.db.condition.CustomCondition;
import org.jminor.framework.db.condition.PropertyCondition;
import org.jminor.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.io.Serializable;

final class ConditionSerializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final PropertyConditionSerializer propertyConditionSerializer;
  private final ConditionSetSerializer conditionSetSerializer;
  private final CustomConditionSerializer customConditionSerializer;

  ConditionSerializer(final PropertyConditionSerializer propertyConditionSerializer,
                      final EntityObjectMapper entityObjectMapper) {
    this.propertyConditionSerializer = propertyConditionSerializer;
    this.conditionSetSerializer = new ConditionSetSerializer(propertyConditionSerializer);
    this.customConditionSerializer = new CustomConditionSerializer(entityObjectMapper);
  }

  void serialize(final Condition condition, final JsonGenerator generator) throws IOException {
    if (condition instanceof Condition.Set) {
      final Condition.Set set = (Condition.Set) condition;
      conditionSetSerializer.serialize(set, generator);
    }
    else if (condition instanceof PropertyCondition) {
      final PropertyCondition propertyCondition = (PropertyCondition) condition;
      propertyConditionSerializer.serialize(propertyCondition, generator);
    }
    else if (condition instanceof CustomCondition) {
      final CustomCondition customCondition = (CustomCondition) condition;
      customConditionSerializer.serialize(customCondition, generator);
    }
    else {
      throw new IllegalArgumentException("Unknown Condition type: " + condition.getClass());
    }
  }
}

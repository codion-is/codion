/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.CustomCondition;
import is.codion.framework.db.condition.PropertyCondition;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.io.Serializable;

final class ConditionSerializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final PropertyConditionSerializer propertyConditionSerializer;
  private final ConditionCombinationSerializer conditionCombinationSerializer;
  private final CustomConditionSerializer customConditionSerializer;

  ConditionSerializer(final PropertyConditionSerializer propertyConditionSerializer,
                      final EntityObjectMapper entityObjectMapper) {
    this.propertyConditionSerializer = propertyConditionSerializer;
    this.conditionCombinationSerializer = new ConditionCombinationSerializer(propertyConditionSerializer);
    this.customConditionSerializer = new CustomConditionSerializer(entityObjectMapper);
  }

  void serialize(final Condition condition, final JsonGenerator generator) throws IOException {
    if (condition instanceof Condition.Combination) {
      final Condition.Combination combination = (Condition.Combination) condition;
      conditionCombinationSerializer.serialize(combination, generator);
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

/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.framework.db.condition.AttributeCondition;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.CustomCondition;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.io.Serializable;

final class ConditionSerializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final AttributeConditionSerializer attributeConditionSerializer;
  private final ConditionCombinationSerializer conditionCombinationSerializer;
  private final CustomConditionSerializer customConditionSerializer;

  ConditionSerializer(final AttributeConditionSerializer attributeConditionSerializer,
                      final EntityObjectMapper entityObjectMapper) {
    this.attributeConditionSerializer = attributeConditionSerializer;
    this.conditionCombinationSerializer = new ConditionCombinationSerializer(attributeConditionSerializer);
    this.customConditionSerializer = new CustomConditionSerializer(entityObjectMapper);
  }

  void serialize(final Condition condition, final JsonGenerator generator) throws IOException {
    if (condition instanceof Condition.Combination) {
      final Condition.Combination combination = (Condition.Combination) condition;
      conditionCombinationSerializer.serialize(combination, generator);
    }
    else if (condition instanceof AttributeCondition) {
      final AttributeCondition attributeCondition = (AttributeCondition) condition;
      attributeConditionSerializer.serialize(attributeCondition, generator);
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

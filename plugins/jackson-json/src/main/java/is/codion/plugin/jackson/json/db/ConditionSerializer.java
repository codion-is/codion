/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.framework.db.condition.AttributeCondition;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.CustomCondition;
import is.codion.framework.domain.entity.Entities;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

final class ConditionSerializer extends StdSerializer<Condition> {

  private static final long serialVersionUID = 1;

  private final AttributeConditionSerializer attributeConditionSerializer;
  private final ConditionCombinationSerializer conditionCombinationSerializer;
  private final CustomConditionSerializer customConditionSerializer;
  private final Entities entities;

  public ConditionSerializer(final EntityObjectMapper entityObjectMapper) {
    super(Condition.class);
    this.attributeConditionSerializer = new AttributeConditionSerializer(entityObjectMapper);
    this.conditionCombinationSerializer = new ConditionCombinationSerializer(attributeConditionSerializer);
    this.customConditionSerializer = new CustomConditionSerializer(entityObjectMapper);
    this.entities = entityObjectMapper.getEntities();
  }

  @Override
  public void serialize(final Condition condition, final JsonGenerator generator,
                        final SerializerProvider provider) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("entityType", condition.getEntityType().getName());
    generator.writeFieldName("condition");
    serialize(condition, generator);
    generator.writeEndObject();
  }

  void serialize(final Condition condition, final JsonGenerator generator) throws IOException {
    if (condition instanceof Condition.Combination) {
      Condition.Combination combination = (Condition.Combination) condition;
      conditionCombinationSerializer.serialize(combination, generator);
    }
    else if (condition instanceof AttributeCondition) {
      AttributeCondition<?> attributeCondition = (AttributeCondition<?>) condition;
      attributeConditionSerializer.serialize(attributeCondition, generator);
    }
    else if (condition instanceof CustomCondition) {
      CustomCondition customCondition = (CustomCondition) condition;
      customConditionSerializer.serialize(customCondition, generator);
    }
    else if (condition.getConditionString(entities.getDefinition(condition.getEntityType())).isEmpty()) {
      generator.writeStartObject();
      generator.writeStringField("type", "empty");
      generator.writeEndObject();
    }
    else {
      throw new IllegalArgumentException("Unknown Condition type: " + condition.getClass());
    }
  }
}

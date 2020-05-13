/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.plugin.jackson.json.db;

import dev.codion.framework.db.condition.EntityCondition;
import dev.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

final class EntityConditionSerializer extends StdSerializer<EntityCondition> {

  private static final long serialVersionUID = 1;

  private final ConditionSerializer conditionSerializer;

  public EntityConditionSerializer(final EntityObjectMapper entityObjectMapper) {
    super(EntityCondition.class);
    this.conditionSerializer = new ConditionSerializer(new PropertyConditionSerializer(entityObjectMapper), entityObjectMapper);
  }

  @Override
  public void serialize(final EntityCondition condition, final JsonGenerator generator,
                        final SerializerProvider provider) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("entityId", condition.getEntityId());
    generator.writeFieldName("condition");
    conditionSerializer.serialize(condition.getCondition(), generator);
    generator.writeEndObject();
  }
}

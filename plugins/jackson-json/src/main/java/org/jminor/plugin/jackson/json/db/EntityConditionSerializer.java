/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json.db;

import org.jminor.framework.db.condition.EntityCondition;
import org.jminor.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

final class EntityConditionSerializer extends StdSerializer<EntityCondition> {

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
    conditionSerializer.serialize(condition.getCondition(), generator, provider);
    generator.writeEndObject();
  }
}

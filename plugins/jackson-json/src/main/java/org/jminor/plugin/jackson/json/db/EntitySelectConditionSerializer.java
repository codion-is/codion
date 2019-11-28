/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json.db;

import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

final class EntitySelectConditionSerializer extends StdSerializer<EntitySelectCondition> {

  private static final long serialVersionUID = 1;

  private final ConditionSerializer conditionSerializer;

  EntitySelectConditionSerializer(final EntityObjectMapper entityObjectMapper) {
    super(EntitySelectCondition.class);
    this.conditionSerializer = new ConditionSerializer(new PropertyConditionSerializer(entityObjectMapper), entityObjectMapper);
  }

  @Override
  public void serialize(final EntitySelectCondition condition, final JsonGenerator generator,
                        final SerializerProvider provider) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("entityId", condition.getEntityId());
    generator.writeFieldName("condition");
    conditionSerializer.serialize(condition.getCondition(), generator);
    generator.writeFieldName("orderBy");
    generator.writeObject(condition.getOrderBy());
    generator.writeFieldName("limit");
    generator.writeObject(condition.getLimit());
    generator.writeFieldName("offset");
    generator.writeObject(condition.getOffset());
    generator.writeFieldName("forUpdate");
    generator.writeObject(condition.isForUpdate());
    generator.writeFieldName("fetchCount");
    generator.writeObject(condition.getFetchCount());
    generator.writeEndObject();
  }
}

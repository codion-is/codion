/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.framework.db.condition.Condition;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

final class EntityConditionSerializer extends StdSerializer<Condition> {

  private static final long serialVersionUID = 1;

  private final ConditionSerializer conditionSerializer;

  public EntityConditionSerializer(final EntityObjectMapper entityObjectMapper) {
    super(Condition.class);
    this.conditionSerializer = new ConditionSerializer(new AttributeConditionSerializer(entityObjectMapper), entityObjectMapper);
  }

  @Override
  public void serialize(final Condition condition, final JsonGenerator generator,
                        final SerializerProvider provider) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("entityType", condition.getEntityType().getName());
    generator.writeFieldName("condition");
    conditionSerializer.serialize(condition, generator);
    generator.writeEndObject();
  }
}

/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Map;

final class UpdateSerializer extends StdSerializer<Update> {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;
  private final ConditionSerializer conditionSerializer;

  UpdateSerializer(EntityObjectMapper entityObjectMapper) {
    super(Update.class);
    this.conditionSerializer = new ConditionSerializer(entityObjectMapper);
    this.entityObjectMapper = entityObjectMapper;
  }

  @Override
  public void serialize(Update update, JsonGenerator generator,
                        SerializerProvider provider) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("entityType", update.condition().entityType().name());
    generator.writeFieldName("condition");
    conditionSerializer.serialize(update.condition(), generator);
    generator.writeFieldName("values");
    generator.writeStartObject();
    for (Map.Entry<Column<?>, Object> columnValue : update.columnValues().entrySet()) {
      generator.writeFieldName(columnValue.getKey().name());
      entityObjectMapper.writeValue(generator, columnValue.getValue());
    }
    generator.writeEndObject();
    generator.writeEndObject();
  }
}

/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.EntityConnection.Count;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

final class CountSerializer extends StdSerializer<Count> {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  CountSerializer(EntityObjectMapper entityObjectMapper) {
    super(Count.class);
    this.entityObjectMapper = entityObjectMapper;
  }

  @Override
  public void serialize(Count count, JsonGenerator generator, SerializerProvider provider) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("entityType", count.where().entityType().name());
    generator.writeFieldName("where");
    entityObjectMapper.serializeCondition(count.where(), generator);
    generator.writeFieldName("having");
    entityObjectMapper.serializeCondition(count.having(), generator);
    generator.writeEndObject();
  }
}

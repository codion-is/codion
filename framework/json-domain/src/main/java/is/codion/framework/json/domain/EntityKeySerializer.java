/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.domain;

import is.codion.framework.domain.entity.Column;
import is.codion.framework.domain.entity.Key;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

import static java.util.Objects.requireNonNull;

final class EntityKeySerializer extends StdSerializer<Key> {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  EntityKeySerializer(EntityObjectMapper entityObjectMapper) {
    super(Key.class);
    this.entityObjectMapper = entityObjectMapper;
  }

  @Override
  public void serialize(Key key, JsonGenerator generator, SerializerProvider provider) throws IOException {
    requireNonNull(key, "key");
    generator.writeStartObject();
    generator.writeStringField("entityType", key.type().name());
    generator.writeFieldName("values");
    writeValues(key, generator);
    generator.writeEndObject();
  }

  private void writeValues(Key key, JsonGenerator generator) throws IOException {
    generator.writeStartObject();
    for (Column<?> column : key.columns()) {
      generator.writeFieldName(column.name());
      entityObjectMapper.writeValue(generator, key.get(column));
    }
    generator.writeEndObject();
  }
}

/*
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.domain;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Column;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

import static java.util.Objects.requireNonNull;

final class EntityKeySerializer extends StdSerializer<Entity.Key> {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  EntityKeySerializer(EntityObjectMapper entityObjectMapper) {
    super(Entity.Key.class);
    this.entityObjectMapper = entityObjectMapper;
  }

  @Override
  public void serialize(Entity.Key key, JsonGenerator generator, SerializerProvider provider) throws IOException {
    requireNonNull(key, "key");
    generator.writeStartObject();
    generator.writeStringField("entityType", key.entityType().name());
    generator.writeFieldName("values");
    writeValues(key, generator);
    generator.writeEndObject();
  }

  private void writeValues(Entity.Key key, JsonGenerator generator) throws IOException {
    generator.writeStartObject();
    for (Column<?> column : key.columns()) {
      generator.writeFieldName(column.name());
      entityObjectMapper.writeValue(generator, key.get(column));
    }
    generator.writeEndObject();
  }
}

/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.domain;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Key;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

import static java.util.Objects.requireNonNull;

final class EntityKeySerializer extends StdSerializer<Key> {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  EntityKeySerializer(final EntityObjectMapper entityObjectMapper) {
    super(Key.class);
    this.entityObjectMapper = entityObjectMapper;
  }

  @Override
  public void serialize(final Key key, final JsonGenerator generator, final SerializerProvider provider) throws IOException {
    requireNonNull(key, "key");
    generator.writeStartObject();
    generator.writeStringField("entityType", key.getEntityType().getName());
    generator.writeFieldName("values");
    writeValues(key, generator);
    generator.writeEndObject();
  }

  private void writeValues(final Key key, final JsonGenerator generator) throws IOException {
    generator.writeStartObject();
    for (final Attribute<?> attribute : key.getAttributes()) {
      generator.writeFieldName(attribute.getName());
      entityObjectMapper.writeValue(generator, key.get(attribute));
    }
    generator.writeEndObject();
  }
}

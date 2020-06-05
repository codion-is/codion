/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.domain;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Key;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
    entityObjectMapper.writeValue(generator, getValueMap(key));
    generator.writeEndObject();
  }

  private static Map<String, Object> getValueMap(final Key key) {
    final Map<String, Object> valueMap = new HashMap<>();
    for (final Attribute<?> attribute : key.getAttributes()) {
      valueMap.put(attribute.getName(), key.get(attribute));
    }

    return valueMap;
  }
}

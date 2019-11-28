/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json.domain;

import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.ColumnProperty;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

final class EntityKeySerializer extends StdSerializer<Entity.Key> {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  EntityKeySerializer(final EntityObjectMapper entityObjectMapper) {
    super(Entity.Key.class);
    this.entityObjectMapper = entityObjectMapper;
  }

  @Override
  public void serialize(final Entity.Key key, final JsonGenerator generator, final SerializerProvider provider) throws IOException {
    requireNonNull(key, "key");
    generator.writeStartObject();
    generator.writeStringField("entityId", key.getEntityId());
    generator.writeFieldName("values");
    entityObjectMapper.writeValue(generator, getValueMap(key));
    generator.writeEndObject();
  }

  private static Map<String, Object> getValueMap(final Entity.Key key) {
    final Map<String, Object> valueMap = new HashMap<>();
    for (final ColumnProperty property : key.keySet()) {
      valueMap.put(property.getPropertyId(), key.get(property));
    }

    return valueMap;
  }
}

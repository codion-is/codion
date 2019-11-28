/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json.domain;

import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

final class EntitySerializer extends StdSerializer<Entity> {

  private static final long serialVersionUID = 1;

  private boolean includeForeignKeyValues = false;
  private boolean includeNullValues = true;
  private boolean includeReadOnlyValues = true;

  private final EntityObjectMapper mapper;

  EntitySerializer(final EntityObjectMapper mapper) {
    super(Entity.class);
    this.mapper = mapper;
  }

  @Override
  public void serialize(final Entity entity, final JsonGenerator generator, final SerializerProvider provider)
          throws IOException {
    requireNonNull(entity, "entity");
    generator.writeStartObject();
    generator.writeFieldName("entityId");
    mapper.writeValue(generator, entity.getEntityId());
    generator.writeFieldName("values");
    mapper.writeValue(generator, getValueMap(entity));
    if (entity.isModified()) {
      generator.writeFieldName("originalValues");
      mapper.writeValue(generator, getOriginalValueMap(entity));
    }
    generator.writeEndObject();
  }

  void setIncludeForeignKeyValues(final boolean includeForeignKeyValues) {
    this.includeForeignKeyValues = includeForeignKeyValues;
  }

  void setIncludeNullValues(final boolean includeNullValues) {
    this.includeNullValues = includeNullValues;
  }

  void setIncludeReadOnlyValues(final boolean includeReadOnlyValues) {
    this.includeReadOnlyValues = includeReadOnlyValues;
  }

  private Map<String, Object> getValueMap(final Entity entity) {
    final Map<String, Object> valueMap = new HashMap<>();
    for (final Property property : entity.keySet()) {
      if (include(property, entity)) {
        valueMap.put(property.getPropertyId(), entity.get(property));
      }
    }

    return valueMap;
  }

  private Map<String, Object> getOriginalValueMap(final Entity entity) {
    final Map<String, Object> valueMap = new HashMap<>();
    for (final Property property : entity.originalKeySet()) {
      if (include(property, entity)) {
        valueMap.put(property.getPropertyId(), entity.getOriginal(property));
      }
    }

    return valueMap;
  }

  private boolean include(final Property property, final Entity entity) {
    if (!includeForeignKeyValues && property instanceof ForeignKeyProperty) {
      return false;
    }
    if (!includeReadOnlyValues && property.isReadOnly()) {
      return false;
    }
    if (!includeNullValues && entity.isNull(property)) {
      return false;
    }

    return true;
  }
}

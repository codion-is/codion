/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.domain;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;

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
    mapper.writeValue(generator, entity.getEntityId().getName());
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

  private Map<String, Object> getValueMap(final Entity entity) {
    final Map<String, Object> valueMap = new HashMap<>();
    final EntityDefinition definition = mapper.getEntities().getDefinition(entity.getEntityId());
    for (final Attribute<?> attribute : entity.keySet()) {
      final Property<?> property = definition.getProperty(attribute);
      if (include(property, entity)) {
        valueMap.put(property.getAttribute().getName(), entity.get(property.getAttribute()));
      }
    }

    return valueMap;
  }

  private Map<String, Object> getOriginalValueMap(final Entity entity) {
    final Map<String, Object> valueMap = new HashMap<>();
    final EntityDefinition definition = mapper.getEntities().getDefinition(entity.getEntityId());
    for (final Attribute<?> attribute : entity.originalKeySet()) {
      final Property<?> property = definition.getProperty(attribute);
      if (include(property, entity)) {
        valueMap.put(property.getAttribute().getName(), entity.getOriginal(property.getAttribute()));
      }
    }

    return valueMap;
  }

  private boolean include(final Property<?> property, final Entity entity) {
    if (!includeForeignKeyValues && property instanceof ForeignKeyProperty) {
      return false;
    }
    if (!includeNullValues && entity.isNull(property.getAttribute())) {
      return false;
    }

    return true;
  }
}

/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.domain;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

final class EntitySerializer extends StdSerializer<Entity> {

  private static final long serialVersionUID = 1;

  private boolean includeForeignKeyValues = true;
  private boolean includeNullValues = true;

  private final EntityObjectMapper mapper;

  EntitySerializer(final EntityObjectMapper mapper) {
    super(Entity.class);
    this.mapper = mapper;
  }

  @Override
  public void serialize(final Entity entity, final JsonGenerator generator, final SerializerProvider provider) throws IOException {
    requireNonNull(entity, "entity");
    generator.writeStartObject();
    generator.writeStringField("entityType", entity.getEntityType().getName());
    generator.writeFieldName("values");
    writeValues(entity, generator, entity.entrySet());
    if (entity.isModified()) {
      generator.writeFieldName("originalValues");
      writeValues(entity, generator, entity.originalEntrySet());
    }
    generator.writeEndObject();
  }

  void setIncludeForeignKeyValues(final boolean includeForeignKeyValues) {
    this.includeForeignKeyValues = includeForeignKeyValues;
  }

  void setIncludeNullValues(final boolean includeNullValues) {
    this.includeNullValues = includeNullValues;
  }

  private void writeValues(final Entity entity, final JsonGenerator generator, final Set<Map.Entry<Attribute<?>, Object>> entrySet) throws IOException {
    generator.writeStartObject();
    final EntityDefinition definition = mapper.getEntities().getDefinition(entity.getEntityType());
    for (final Map.Entry<Attribute<?>, Object> entry : entrySet) {
      final Property<?> property = definition.getProperty(entry.getKey());
      if (include(property, entity)) {
        generator.writeFieldName(property.getAttribute().getName());
        mapper.writeValue(generator, entry.getValue());
      }
    }
    generator.writeEndObject();
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

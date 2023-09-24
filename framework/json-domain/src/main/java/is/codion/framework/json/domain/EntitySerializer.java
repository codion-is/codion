/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.json.domain;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;

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

  EntitySerializer(EntityObjectMapper mapper) {
    super(Entity.class);
    this.mapper = mapper;
  }

  @Override
  public void serialize(Entity entity, JsonGenerator generator, SerializerProvider provider) throws IOException {
    requireNonNull(entity, "entity");
    generator.writeStartObject();
    generator.writeStringField("entityType", entity.entityType().name());
    generator.writeFieldName("values");
    writeValues(entity, generator, entity.entrySet());
    if (entity.isModified()) {
      generator.writeFieldName("originalValues");
      writeValues(entity, generator, entity.originalEntrySet());
    }
    if (entity.isImmutable()) {
      generator.writeBooleanField("immutable", Boolean.TRUE);
    }
    generator.writeEndObject();
  }

  void setIncludeForeignKeyValues(boolean includeForeignKeyValues) {
    this.includeForeignKeyValues = includeForeignKeyValues;
  }

  void setIncludeNullValues(boolean includeNullValues) {
    this.includeNullValues = includeNullValues;
  }

  private void writeValues(Entity entity, JsonGenerator generator, Set<Map.Entry<Attribute<?>, Object>> entrySet) throws IOException {
    generator.writeStartObject();
    EntityDefinition definition = entity.entityDefinition();
    for (Map.Entry<Attribute<?>, Object> entry : entrySet) {
      AttributeDefinition<?> attributeDefinition = definition.attributeDefinition(entry.getKey());
      if (include(attributeDefinition, entity)) {
        generator.writeFieldName(attributeDefinition.attribute().name());
        mapper.writeValue(generator, entry.getValue());
      }
    }
    generator.writeEndObject();
  }

  private boolean include(AttributeDefinition<?> attributeDefinition, Entity entity) {
    if (!includeForeignKeyValues && attributeDefinition instanceof ForeignKeyDefinition) {
      return false;
    }
    if (!includeNullValues && entity.isNull(attributeDefinition.attribute())) {
      return false;
    }

    return true;
  }
}

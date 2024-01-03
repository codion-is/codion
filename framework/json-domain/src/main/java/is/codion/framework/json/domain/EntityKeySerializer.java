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
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson.
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

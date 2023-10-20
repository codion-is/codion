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
package is.codion.framework.json.db;

import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Map;

final class UpdateSerializer extends StdSerializer<Update> {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  UpdateSerializer(EntityObjectMapper entityObjectMapper) {
    super(Update.class);
    this.entityObjectMapper = entityObjectMapper;
  }

  @Override
  public void serialize(Update update, JsonGenerator generator,
                        SerializerProvider provider) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("entityType", update.where().entityType().name());
    generator.writeFieldName("condition");
    entityObjectMapper.serializeCondition(update.where(), generator);
    generator.writeFieldName("values");
    generator.writeStartObject();
    for (Map.Entry<Column<?>, Object> columnValue : update.columnValues().entrySet()) {
      generator.writeFieldName(columnValue.getKey().name());
      entityObjectMapper.writeValue(generator, columnValue.getValue());
    }
    generator.writeEndObject();
    generator.writeEndObject();
  }
}

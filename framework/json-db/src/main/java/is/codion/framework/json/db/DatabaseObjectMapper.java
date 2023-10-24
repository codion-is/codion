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

import is.codion.framework.db.EntityConnection.Count;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.domain.entity.attribute.Condition;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import static java.util.Objects.requireNonNull;

/**
 * ObjectMapper implementation for {@link Select} and {@link Update}.
 * For instances use the {@link #databaseObjectMapper(EntityObjectMapper)} factory method.
 */
public final class DatabaseObjectMapper extends ObjectMapper {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  private DatabaseObjectMapper(EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = requireNonNull(entityObjectMapper);
    SimpleModule module = new SimpleModule();
    module.addSerializer(Condition.class, entityObjectMapper.conditionSerializer());
    module.addDeserializer(Condition.class, entityObjectMapper.conditionDeserializer());
    module.addSerializer(Select.class, new SelectSerializer(entityObjectMapper));
    module.addDeserializer(Select.class, new SelectDeserializer(entityObjectMapper));
    module.addSerializer(Update.class, new UpdateSerializer(entityObjectMapper));
    module.addDeserializer(Update.class, new UpdateDeserializer(entityObjectMapper));
    module.addSerializer(Count.class, new CountSerializer(entityObjectMapper));
    module.addDeserializer(Count.class, new CountDeserializer(entityObjectMapper));
    registerModule(module);
  }

  /**
   * @return the {@link EntityObjectMapper} this {@link DatabaseObjectMapper} uses.
   */
  public EntityObjectMapper entityObjectMapper() {
    return entityObjectMapper;
  }

  /**
   * Instantiates a new {@link DatabaseObjectMapper}
   * @param entityObjectMapper a {@link EntityObjectMapper}
   * @return a new {@link DatabaseObjectMapper} instance
   */
  public static DatabaseObjectMapper databaseObjectMapper(EntityObjectMapper entityObjectMapper) {
    return new DatabaseObjectMapper(entityObjectMapper);
  }
}

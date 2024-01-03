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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.json.domain;

import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityDefinition;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class EntityTypeKeyDeserializer extends KeyDeserializer {

  private final Map<String, EntityDefinition> definitions = new ConcurrentHashMap<>();
  private final Entities entities;

  EntityTypeKeyDeserializer(Entities entities) {
    this.entities = entities;
  }

  @Override
  public Object deserializeKey(String key, DeserializationContext ctxt) {
    return definitions.computeIfAbsent(key, entities::definition).entityType();
  }
}

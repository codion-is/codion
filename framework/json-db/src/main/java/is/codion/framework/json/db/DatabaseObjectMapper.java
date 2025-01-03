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
 * Copyright (c) 2019 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.EntityConnection.Count;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.Serial;

import static java.util.Objects.requireNonNull;

/**
 * ObjectMapper implementation for {@link Select} and {@link Update}.
 * For instances use the {@link #databaseObjectMapper(EntityObjectMapper)} factory method.
 */
public final class DatabaseObjectMapper extends ObjectMapper {

	@Serial
	private static final long serialVersionUID = 1;

	private DatabaseObjectMapper(EntityObjectMapper entityObjectMapper) {
		registerModule(requireNonNull(entityObjectMapper).module());
		SimpleModule module = new SimpleModule();
		module.addSerializer(Select.class, new SelectSerializer(entityObjectMapper));
		module.addDeserializer(Select.class, new SelectDeserializer(entityObjectMapper));
		module.addSerializer(Update.class, new UpdateSerializer(entityObjectMapper));
		module.addDeserializer(Update.class, new UpdateDeserializer(entityObjectMapper));
		module.addSerializer(Count.class, new CountSerializer(entityObjectMapper));
		module.addDeserializer(Count.class, new CountDeserializer(entityObjectMapper));
		registerModule(module);
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

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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.EntityConnection.Savepoint;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.io.Serial;
import java.util.UUID;

final class SavepointDeserializer extends StdDeserializer<Savepoint> {

	@Serial
	private static final long serialVersionUID = 1;

	SavepointDeserializer() {
		super(Savepoint.class);
	}

	@Override
	public Savepoint deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
		JsonNode jsonNode = parser.getCodec().readTree(parser);

		return Savepoint.savepoint(UUID.fromString(jsonNode.get("id").asText()));
	}
}

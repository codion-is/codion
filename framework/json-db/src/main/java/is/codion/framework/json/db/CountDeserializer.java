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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.EntityConnection.Count;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.io.Serial;

final class CountDeserializer extends StdDeserializer<Count> {

	@Serial
	private static final long serialVersionUID = 1;

	private final EntityObjectMapper entityObjectMapper;

	CountDeserializer(EntityObjectMapper entityObjectMapper) {
		super(Count.class);
		this.entityObjectMapper = entityObjectMapper;
	}

	@Override
	public Count deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
		JsonNode jsonNode = parser.getCodec().readTree(parser);
		EntityType entityType = entityObjectMapper.entities().domainType().entityType(jsonNode.get("entityType").asText());
		EntityDefinition definition = entityObjectMapper.entities().definition(entityType);
		JsonNode whereNode = jsonNode.get("where");
		Condition where = entityObjectMapper.deserializeCondition(definition, whereNode);
		JsonNode havingNode = jsonNode.get("having");
		Condition having = entityObjectMapper.deserializeCondition(definition, havingNode);

		return Count.builder(where)
						.having(having)
						.build();
	}
}

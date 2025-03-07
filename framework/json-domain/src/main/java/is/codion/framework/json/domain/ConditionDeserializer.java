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
package is.codion.framework.json.domain;

import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.condition.Condition;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.io.Serial;

import static java.util.Objects.requireNonNull;

final class ConditionDeserializer extends StdDeserializer<Condition> {

	@Serial
	private static final long serialVersionUID = 1;

	final EntityObjectMapper entityObjectMapper;
	final Entities entities;

	private final ColumnConditionDeserializer columnConditionDeserializer;
	private final ConditionCombinationDeserializer conditionCombinationDeserializer;
	private final CustomConditionDeserializer customConditionDeserializer;

	ConditionDeserializer(EntityObjectMapper entityObjectMapper) {
		super(Condition.class);
		this.entityObjectMapper = requireNonNull(entityObjectMapper);
		this.columnConditionDeserializer = new ColumnConditionDeserializer(entityObjectMapper);
		this.conditionCombinationDeserializer = new ConditionCombinationDeserializer(this);
		this.customConditionDeserializer = new CustomConditionDeserializer(entityObjectMapper);
		this.entities = entityObjectMapper.entities();
	}

	@Override
	public Condition deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
		JsonNode node = parser.getCodec().readTree(parser);
		EntityType entityType = entities.domainType().entityType(node.get("entityType").asText());
		JsonNode conditionNode = node.get("condition");

		return deserialize(entities.definition(entityType), conditionNode);
	}

	Condition deserialize(EntityDefinition definition, JsonNode conditionNode) throws IOException {
		JsonNode type = conditionNode.get("type");
		String typeString = type.asText();
		if ("combination".equals(typeString)) {
			return conditionCombinationDeserializer.deserialize(definition, conditionNode);
		}
		else if ("column".equals(typeString)) {
			return columnConditionDeserializer.deserialize(definition, conditionNode);
		}
		else if ("custom".equals(typeString)) {
			return customConditionDeserializer.deserialize(definition, conditionNode);
		}
		else if ("all".equals(typeString)) {
			return Condition.all(definition.type());
		}

		throw new IllegalArgumentException("Unknown condition type: " + type);
	}
}

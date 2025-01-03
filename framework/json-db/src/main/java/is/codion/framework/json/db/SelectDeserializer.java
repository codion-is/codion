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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.OrderBy.NullOrder;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

final class SelectDeserializer extends StdDeserializer<Select> {

	@Serial
	private static final long serialVersionUID = 1;

	private final EntityObjectMapper entityObjectMapper;

	SelectDeserializer(EntityObjectMapper entityObjectMapper) {
		super(Select.class);
		this.entityObjectMapper = entityObjectMapper;
	}

	@Override
	public Select deserialize(JsonParser parser, DeserializationContext ctxt)
					throws IOException {
		JsonNode jsonNode = parser.getCodec().readTree(parser);
		EntityType entityType = entityObjectMapper.entities().domainType().entityType(jsonNode.get("entityType").asText());
		EntityDefinition definition = entityObjectMapper.entities().definition(entityType);
		JsonNode whereConditionNode = jsonNode.get("where");
		Condition whereCondition = entityObjectMapper.deserializeCondition(definition, whereConditionNode);

		Select.Builder selectBuilder = Select.where(whereCondition);
		JsonNode havingCondition = jsonNode.get("having");
		if (havingCondition != null) {
			selectBuilder.having(entityObjectMapper.deserializeCondition(definition, havingCondition));
		}
		JsonNode orderBy = jsonNode.get("orderBy");
		if (orderBy != null && !orderBy.isNull()) {
			selectBuilder.orderBy(deserializeOrderBy(definition, orderBy));
		}
		JsonNode limit = jsonNode.get("limit");
		if (limit != null && !limit.isNull()) {
			selectBuilder.limit(limit.asInt());
		}
		JsonNode offset = jsonNode.get("offset");
		if (offset != null && !offset.isNull()) {
			selectBuilder.offset(offset.asInt());
		}
		JsonNode forUpdate = jsonNode.get("forUpdate");
		if (forUpdate != null && !forUpdate.isNull() && forUpdate.asBoolean()) {
			selectBuilder.forUpdate();
		}
		JsonNode fetchDepth = jsonNode.get("fetchDepth");
		if (fetchDepth != null && !fetchDepth.isNull()) {
			selectBuilder.fetchDepth(fetchDepth.asInt());
		}
		JsonNode fkFetchDepth = jsonNode.get("fkFetchDepth");
		if (fkFetchDepth != null && !fkFetchDepth.isNull()) {
			for (ForeignKey foreignKey : definition.foreignKeys().get()) {
				JsonNode fetchDepthNode = fkFetchDepth.get(foreignKey.name());
				if (fetchDepthNode != null) {
					selectBuilder.fetchDepth(foreignKey, fetchDepthNode.asInt());
				}
			}
		}
		JsonNode attributes = jsonNode.get("attributes");
		if (attributes != null && !attributes.isNull()) {
			selectBuilder.attributes(deserializeAttributes(definition, attributes));
		}
		JsonNode queryTimeout = jsonNode.get("queryTimeout");
		if (queryTimeout != null && !queryTimeout.isNull()) {
			selectBuilder.queryTimeout(queryTimeout.asInt());
		}

		return selectBuilder.build();
	}

	private static OrderBy deserializeOrderBy(EntityDefinition definition, JsonNode jsonNode) {
		if (jsonNode.isEmpty()) {
			return null;
		}

		OrderBy.Builder builder = OrderBy.builder();
		for (JsonNode node : jsonNode) {
			String[] split = node.asText().split(":");
			Column<?> column = (Column<?>) definition.attributes().get(split[0]);
			String order = split[1];
			NullOrder nullOrder = NullOrder.valueOf(split[2]);
			boolean ignoreCase = Boolean.parseBoolean(split[3]);
			if ("asc".equals(order)) {
				if (ignoreCase) {
					builder.ascendingIgnoreCase(nullOrder, (Column<String>) column);
				}
				else {
					builder.ascending(nullOrder, column);
				}
			}
			else {
				if (ignoreCase) {
					builder.descendingIgnoreCase(nullOrder, (Column<String>) column);
				}
				else {
					builder.descending(nullOrder, column);
				}
			}
		}

		return builder.build();
	}

	private static List<Attribute<?>> deserializeAttributes(EntityDefinition definition, JsonNode jsonNode) {
		List<Attribute<?>> attributes = new ArrayList<>(jsonNode.size());
		for (JsonNode node : jsonNode) {
			attributes.add(definition.attributes().get(node.asText()));
		}

		return attributes;
	}
}

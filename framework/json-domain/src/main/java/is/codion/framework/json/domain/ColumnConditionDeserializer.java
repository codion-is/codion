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

import is.codion.common.utilities.Operator;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.condition.ColumnCondition;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class ColumnConditionDeserializer implements Serializable {

	@Serial
	private static final long serialVersionUID = 1;

	private final EntityObjectMapper entityObjectMapper;

	ColumnConditionDeserializer(EntityObjectMapper entityObjectMapper) {
		this.entityObjectMapper = requireNonNull(entityObjectMapper);
	}

	<T> ColumnCondition<T> deserialize(EntityDefinition definition, JsonNode conditionNode) {
		String columnName = conditionNode.get("column").asText();
		ColumnDefinition<T> columnDefinition = definition.columns().definition((Column<T>) definition.attributes().getOrThrow(columnName));
		boolean wildcard = conditionNode.get("wildcard").asBoolean();
		boolean caseSensitive = conditionNode.get("caseSensitive").asBoolean();
		JsonNode valuesNode = conditionNode.get("values");
		List<T> values = new ArrayList<>();
		for (JsonNode valueNode : valuesNode) {
			values.add(entityObjectMapper.convertValue(valueNode, columnDefinition.attribute().type().valueClass()));
		}
		Column<T> column = columnDefinition.attribute();
		switch (Operator.valueOf(conditionNode.get("operator").asText())) {
			case EQUAL:
				return equalColumnCondition(values, column, caseSensitive, wildcard);
			case NOT_EQUAL:
				return notEqualColumnCondition(values, column, caseSensitive, wildcard);
			case LESS_THAN:
				return column.lessThan(values.get(0));
			case LESS_THAN_OR_EQUAL:
				return column.lessThanOrEqualTo(values.get(0));
			case GREATER_THAN:
				return column.greaterThan(values.get(0));
			case GREATER_THAN_OR_EQUAL:
				return column.greaterThanOrEqualTo(values.get(0));
			case BETWEEN_EXCLUSIVE:
				return column.betweenExclusive(values.get(0), values.get(1));
			case BETWEEN:
				return column.between(values.get(0), values.get(1));
			case NOT_BETWEEN_EXCLUSIVE:
				return column.notBetweenExclusive(values.get(0), values.get(1));
			case NOT_BETWEEN:
				return column.notBetween(values.get(0), values.get(1));
			case IN:
				return inCondition(values, column, caseSensitive);
			case NOT_IN:
				return notInCondition(values, column, caseSensitive);
			default:
				throw new IllegalArgumentException("Unknown operator: " + Operator.valueOf(conditionNode.get("operator").asText()));
		}
	}

	private static <T> ColumnCondition<T> equalColumnCondition(List<T> values, Column<T> column,
																														 boolean caseSensitive, boolean wildcard) {
		if (values.isEmpty()) {
			return column.isNull();
		}
		if (caseSensitive) {
			return caseSensitiveEqualColumnCondition(values, column, wildcard);
		}

		return caseInsitiveEqualColumnCondition(values, column, wildcard);
	}

	private static <T> ColumnCondition<T> notEqualColumnCondition(List<T> values, Column<T> column,
																																boolean caseSensitive, boolean wildcard) {
		if (values.isEmpty()) {
			return column.isNotNull();
		}
		if (caseSensitive) {
			return caseSensitiveNotEqualColumnCondition(values, column, wildcard);
		}

		return caseInsensitiveNotEqualColumnCondition(values, column, wildcard);
	}

	private static <T> ColumnCondition<T> caseSensitiveEqualColumnCondition(List<T> values, Column<T> column, boolean wildcard) {
		if (values.size() == 1) {
			return wildcard ?
							(ColumnCondition<T>) column.like((String) values.iterator().next()) :
							column.equalTo(values.iterator().next());
		}

		return column.in(values);
	}

	private static <T> ColumnCondition<T> caseSensitiveNotEqualColumnCondition(List<T> values, Column<T> column, boolean wildcard) {
		if (values.size() == 1) {
			return wildcard ?
							(ColumnCondition<T>) column.notLike((String) values.iterator().next()) :
							column.notEqualTo(values.iterator().next());
		}

		return column.notIn(values);
	}

	private static <T> ColumnCondition<T> caseInsitiveEqualColumnCondition(List<T> values, Column<T> column, boolean wildcard) {
		if (values.size() == 1) {
			return wildcard ?
							(ColumnCondition<T>) column.likeIgnoreCase((String) values.iterator().next()) :
							(ColumnCondition<T>) column.equalToIgnoreCase((String) values.iterator().next());
		}

		return (ColumnCondition<T>) column.inIgnoreCase((Collection<String>) values);
	}

	private static <T> ColumnCondition<T> caseInsensitiveNotEqualColumnCondition(List<T> values, Column<T> column, boolean wildcard) {
		if (values.size() == 1) {
			return wildcard ?
							(ColumnCondition<T>) column.notLikeIgnoreCase((String) values.iterator().next()) :
							(ColumnCondition<T>) column.notEqualToIgnoreCase((String) values.iterator().next());
		}

		return (ColumnCondition<T>) column.notInIgnoreCase((Collection<String>) values);
	}

	private static <T> ColumnCondition<T> inCondition(Collection<T> values, Column<T> column, boolean caseSensitive) {
		if (column.type().isString()) {
			Column<String> stringColumn = (Column<String>) column;
			Collection<String> inValues = (Collection<String>) values;

			return (ColumnCondition<T>) (caseSensitive ?
							stringColumn.in(inValues) :
							stringColumn.inIgnoreCase(inValues));
		}

		return column.in(values);
	}

	private static <T> ColumnCondition<T> notInCondition(Collection<T> values, Column<T> column, boolean caseSensitive) {
		if (column.type().isString()) {
			Column<String> stringColumn = (Column<String>) column;
			Collection<String> inValues = (Collection<String>) values;

			return (ColumnCondition<T>) (caseSensitive ?
							stringColumn.notIn(inValues) :
							stringColumn.notInIgnoreCase(inValues));
		}

		return column.notIn(values);
	}
}

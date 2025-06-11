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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.local;

import is.codion.common.db.database.Database;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityQueries;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.condition.Condition;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import static is.codion.framework.db.local.Queries.*;
import static is.codion.framework.domain.entity.condition.Condition.key;
import static java.lang.String.valueOf;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class DefaultEntityQueries implements EntityQueries {

	private static final DecimalFormat NUMBER_FORMAT = (DecimalFormat) NumberFormat.getInstance();

	private static final String PLACEHOLDER = "?";

	static {
		NUMBER_FORMAT.setGroupingUsed(false);
		DecimalFormatSymbols symbols = NUMBER_FORMAT.getDecimalFormatSymbols();
		symbols.setDecimalSeparator('.');
	}

	private final Entities entities;
	private final SelectQueries selectQueries;

	DefaultEntityQueries(Database database, Entities entities) {
		this.entities = requireNonNull(entities);
		this.selectQueries = new SelectQueries(requireNonNull(database));
	}

	@Override
	public String select(Select select) {
		return populateParameters(selectQueries.builder(entities.definition(requireNonNull(select).where().entityType())).select(select).build(),
						parameterValues(select));
	}

	@Override
	public String insert(Entity entity) {
		KeyGenerator keyGenerator = requireNonNull(entity).definition().primaryKey().generator();
		List<ColumnDefinition<?>> columnDefinitions =
						writableColumnDefinitions(entity.definition(), keyGenerator.inserted(), true).stream()
										.filter(columnDefinition -> entity.contains(columnDefinition.attribute()))
										.collect(toList());

		return populateParameters(insertQuery(entity.definition().table(), columnDefinitions),
						parameterValues(entity, columnDefinitions));
	}

	@Override
	public String update(Entity entity) {
		Condition condition = key(requireNonNull(entity).originalPrimaryKey());
		List<ColumnDefinition<?>> columnDefinitions =
						writableColumnDefinitions(entity.definition(), true, false).stream()
										.filter(columnDefinition -> entity.modified(columnDefinition.attribute()))
										.collect(toList());

		return populateParameters(updateQuery(entity.definition().table(), columnDefinitions, condition.toString(entity.definition())),
						parameterValues(entity, columnDefinitions, condition));
	}

	private static String populateParameters(String query, List<?> parameters) {
		StringBuilder builder = new StringBuilder(query);
		ListIterator<?> iterator = parameters.listIterator(parameters.size());
		while (iterator.hasPrevious()) {
			String string = valueOf(iterator.previous());
			int index = builder.lastIndexOf(PLACEHOLDER);
			if (index != -1) {
				builder.replace(index, index + 1, string);
			}
		}

		return builder.toString();
	}

	private static List<?> parameterValues(Entity entity, List<ColumnDefinition<?>> columnDefinitions) {
		return parameterValues(entity, columnDefinitions, null);
	}

	private static List<?> parameterValues(Entity entity, List<ColumnDefinition<?>> columnDefinitions, Condition condition) {
		List<Object> values = columnDefinitions.stream()
						.map(columnDefinition -> entity.get(columnDefinition.attribute()))
						.collect(Collectors.toCollection(ArrayList::new));
		if (condition != null) {
			values.addAll(condition.values());
		}

		return values.stream()
						.map(DefaultEntityQueries::addSingleQuotes)
						.map(DefaultEntityQueries::formatDecimal)
						.collect(toList());
	}

	private static List<?> parameterValues(Select select) {
		List<Object> values = new ArrayList<>(select.where().values());
		values.addAll(select.having().values());

		return values.stream()
						.map(DefaultEntityQueries::addSingleQuotes)
						.map(DefaultEntityQueries::formatDecimal)
						.collect(toList());
	}

	private static Object addSingleQuotes(Object value) {
		if (value instanceof String || value instanceof Temporal) {
			return "'" + value + "'";
		}

		return value;
	}

	private static Object formatDecimal(Object value) {
		if (value instanceof Number) {
			return NUMBER_FORMAT.format(value);
		}

		return value;
	}
}

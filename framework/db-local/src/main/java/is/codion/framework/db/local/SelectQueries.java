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
 * Copyright (c) 2021 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.local;

import is.codion.common.db.database.Database;
import is.codion.framework.db.EntityConnection.Count;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.domain.entity.query.SelectQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static is.codion.common.Text.nullOrEmpty;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

final class SelectQueries {

	private final Database database;
	private final Map<EntityType, List<ColumnDefinition<?>>> defaultSelectColumnsCache = new ConcurrentHashMap<>();
	private final Map<EntityType, String> defaultColumnsClauseCache = new ConcurrentHashMap<>();
	private final Map<EntityType, String> groupByClauseCache = new ConcurrentHashMap<>();

	SelectQueries(Database database) {
		this.database = database;
	}

	Builder builder(EntityDefinition definition) {
		return new Builder(definition);
	}

	final class Builder {

		private static final String SELECT = "SELECT ";
		private static final String FROM = "FROM ";
		private static final String WHERE = "WHERE ";
		private static final String AND = "AND ";
		private static final String GROUP_BY = "GROUP BY ";
		private static final String HAVING = "HAVING ";
		private static final String ORDER_BY = "ORDER BY ";
		private static final String NEWLINE = "\n";
		private static final String NULL_FIRST = " NULLS FIRST";
		private static final String NULL_LAST = " NULLS LAST";
		private static final String COUNT = "COUNT(*)";

		private final EntityDefinition definition;

		private final List<String> where = new ArrayList<>(1);

		private List<ColumnDefinition<?>> selectedColums = Collections.emptyList();

		private String columns;
		private String from;
		private String orderBy;
		private boolean forUpdate;
		private String groupBy;
		private String having;
		private Integer limit;
		private Integer offset;

		private boolean columnsClauseFromSelectQuery = false;

		private Builder(EntityDefinition definition) {
			this.definition = definition;
		}

		List<ColumnDefinition<?>> selectedColumns() {
			return selectedColums;
		}

		Builder select(Select select) {
			return select(select, true);
		}

		Builder select(Select select, boolean useWhereClause) {
			entitySelectQuery();
			if (!columnsClauseFromSelectQuery) {
				setColumns(select);
			}
			//default from clause is handled by from()
			if (useWhereClause) {
				where(select.where());
			}
			if (groupBy == null) {
				groupBy(groupByClause());
			}
			havingCondition(select.having());
			select.orderBy().ifPresent(this::setOrderBy);
			forUpdate(select.forUpdate());
			select.limit().ifPresent(this::limit);
			select.offset().ifPresent(this::offset);

			return this;
		}

		Builder entitySelectQuery() {
			definition.selectQuery().ifPresent(this::fromSelectQuery);
			return this;
		}

		Builder columns(String columns) {
			this.columns = columns;
			return this;
		}

		Builder count(Count count) {
			return columns(COUNT).from("(" + builder(definition)
							.select(Select.where(count.where())
											.having(count.having())
											.attributes(definition.primaryKey().columns())
											.build())
							.build() + ")" + (database.subqueryRequiresAlias() ? " AS row_count" : ""));
		}

		Builder from(String from) {
			this.from = from;
			return this;
		}

		Builder where(Condition condition) {
			String conditionString = condition.toString(definition);
			if (!conditionString.isEmpty()) {
				where(conditionString);
			}
			return this;
		}

		Builder where(String where) {
			if (!nullOrEmpty(where)) {
				this.where.add(where);
			}
			return this;
		}

		Builder orderBy(String orderBy) {
			this.orderBy = orderBy;
			return this;
		}

		Builder forUpdate(boolean forUpdate) {
			this.forUpdate = forUpdate;
			return this;
		}

		Builder groupBy(String groupBy) {
			this.groupBy = groupBy;
			return this;
		}

		Builder having(String having) {
			this.having = having;
			return this;
		}

		Builder limit(int limit) {
			this.limit = limit;
			return this;
		}

		Builder offset(int offset) {
			this.offset = offset;
			return this;
		}

		String build() {
			StringBuilder builder = new StringBuilder()
							.append(SELECT).append(columns).append(NEWLINE)
							.append(FROM).append(from());
			if (!where.isEmpty()) {
				builder.append(NEWLINE).append(WHERE).append(where.get(0));
				if (where.size() > 1) {
					for (int i = 1; i < where.size(); i++) {
						builder.append(NEWLINE).append(AND).append(where.get(i));
					}
				}
			}
			if (groupBy != null && !groupBy.isEmpty()) {
				builder.append(NEWLINE).append(GROUP_BY).append(groupBy);
			}
			if (having != null) {
				builder.append(NEWLINE).append(HAVING).append(having);
			}
			if (orderBy != null) {
				builder.append(NEWLINE).append(ORDER_BY).append(orderBy);
			}
			String limitOffsetClause = database.limitOffsetClause(limit, offset);
			if (!limitOffsetClause.isEmpty()) {
				builder.append(NEWLINE).append(limitOffsetClause);
			}
			if (forUpdate) {
				String forUpdateClause = database.selectForUpdateClause();
				if (!nullOrEmpty(forUpdateClause)) {
					builder.append(NEWLINE).append(forUpdateClause);
				}
			}

			return builder.toString();
		}

		private void setColumns(Select select) {
			Collection<Attribute<?>> attributes = select.attributes();
			if (attributes.isEmpty()) {
				this.selectedColums = defaultSelectColumns();
				columns(defaultColumnsClause());
			}
			else {
				this.selectedColums = columnsToSelect(attributes);
				columns(columnsClause(selectedColums));
			}
		}

		private void fromSelectQuery(SelectQuery selectQuery) {
			if (selectQuery.columns() != null) {
				columns(selectQuery.columns());
				selectedColums = defaultSelectColumns();
				columnsClauseFromSelectQuery = true;
			}
			else {
				columns(defaultColumnsClause());
			}
			from(selectQuery.from());
			where(selectQuery.where());
			orderBy(selectQuery.orderBy());
			groupBy(selectQuery.groupBy());
			having(selectQuery.having());
		}

		private String from() {
			if (from == null) {
				return forUpdate ? definition.tableName() : definition.selectTableName();
			}

			return from;
		}

		private List<ColumnDefinition<?>> columnsToSelect(Collection<Attribute<?>> selectAttributes) {
			Set<ColumnDefinition<?>> columnsToSelect = new HashSet<>(definition.primaryKey().definitions());
			selectAttributes.forEach(attribute -> {
				if (attribute instanceof ForeignKey) {
					((ForeignKey) attribute).references().forEach(reference ->
									columnsToSelect.add(definition.columns().definition(reference.column())));
				}
				else if (attribute instanceof Column) {
					columnsToSelect.add(definition.columns().definition((Column<?>) attribute));
				}
			});

			return new ArrayList<>(columnsToSelect);
		}

		private List<ColumnDefinition<?>> defaultSelectColumns() {
			return defaultSelectColumnsCache.computeIfAbsent(definition.type(), entityType ->
							definition.columns().definitions().stream()
											.filter(ColumnDefinition::selected)
											.collect(toList()));
		}

		private String defaultColumnsClause() {
			return defaultColumnsClauseCache.computeIfAbsent(definition.type(), type -> columnsClause(defaultSelectColumns()));
		}

		private String groupByClause() {
			return groupByClauseCache.computeIfAbsent(definition.type(), type ->
							definition.columns().definitions().stream()
											.filter(ColumnDefinition::groupBy)
											.map(ColumnDefinition::expression)
											.collect(joining(", ")));
		}

		private String columnsClause(List<ColumnDefinition<?>> columnDefinitions) {
			StringBuilder stringBuilder = new StringBuilder();
			for (int i = 0; i < columnDefinitions.size(); i++) {
				ColumnDefinition<?> columnDefinition = columnDefinitions.get(i);
				String columnName = columnDefinition.name();
				String columnExpression = columnDefinition.expression();
				stringBuilder.append(columnExpression);
				if (!columnName.equals(columnExpression)) {
					stringBuilder.append(" AS ").append(columnName);
				}
				if (i < columnDefinitions.size() - 1) {
					stringBuilder.append(", ");
				}
			}

			return stringBuilder.toString();
		}

		private void havingCondition(Condition condition) {
			String conditionString = condition.toString(definition);
			if (!conditionString.isEmpty()) {
				having(having == null ? conditionString : "(" + having + ") AND (" + conditionString + ")");
			}
		}

		private void setOrderBy(OrderBy orderBy) {
			orderBy(createOrderByClause(orderBy));
		}

		private String createOrderByClause(OrderBy orderBy) {
			List<OrderBy.OrderByColumn> orderByColumns = orderBy.orderByColumns();
			if (orderByColumns.size() == 1) {
				return columnOrderByClause(definition, orderByColumns.get(0));
			}

			return orderByColumns.stream()
							.map(orderByColumn -> columnOrderByClause(definition, orderByColumn))
							.collect(joining(", "));
		}

		private String columnOrderByClause(EntityDefinition entityDefinition, OrderBy.OrderByColumn orderByColumn) {
			String columnExpression = entityDefinition.columns().definition(orderByColumn.column()).expression();

			return orderByColumn.ignoreCase() ? "UPPER(" + columnExpression + ")" : columnExpression +
							(orderByColumn.ascending() ? "" : " DESC") +
							nullOrderString(orderByColumn.nullOrder());
		}

		private String nullOrderString(OrderBy.NullOrder nullOrder) {
			switch (nullOrder) {
				case NULLS_FIRST:
					return NULL_FIRST;
				case NULLS_LAST:
					return NULL_LAST;
				default:
					return "";
			}
		}
	}
}

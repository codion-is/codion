/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.Util;
import is.codion.common.db.database.Database;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.query.SelectQuery;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.SubqueryProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

final class SelectQueries {

  private final Database database;
  private final Map<EntityType, List<ColumnProperty<?>>> selectablePropertiesCache = new HashMap<>();
  private final Map<EntityType, String> allColumnsClauseCache = new HashMap<>();

  SelectQueries(final Database database) {
    this.database = database;
  }

  Builder builder(final EntityDefinition definition) {
    return new Builder(definition);
  }

  final class Builder {

    private static final String SELECT = "select ";
    private static final String FROM = "from ";
    private static final String WHERE = "where ";
    private static final String AND = "and ";
    private static final String GROUP_BY = "group by ";
    private static final String HAVING = "having ";
    private static final String ORDER_BY = "order by ";
    private static final String LIMIT = "limit ";
    private static final String OFFSET = "offset ";
    private static final String NEWLINE = "\n";

    private final EntityDefinition definition;

    private final List<String> where = new ArrayList<>(1);

    private List<ColumnProperty<?>> selectedProperties = Collections.emptyList();

    private String columns;
    private String from;
    private String orderBy;
    private boolean forUpdate;
    private String groupBy;
    private String having;
    private Integer limit;
    private Integer offset;

    private boolean columnsClauseFromSelectQuery = false;

    private Builder(final EntityDefinition definition) {
      this.definition = definition;
    }

    List<ColumnProperty<?>> getSelectedProperties() {
      return selectedProperties;
    }

    Builder selectCondition(final SelectCondition condition) {
      entitySelectQuery();
      if (!columnsClauseFromSelectQuery) {
        setColumns(condition);
      }
      where(condition);
      groupBy(definition.getGroupByClause());
      having(definition.getHavingClause());
      forUpdate(condition.isForUpdate());
      if (condition.getOrderBy() != null) {
        orderBy(getOrderByClause(condition.getOrderBy()));
      }
      if (condition.getLimit() >= 0) {
        limit(condition.getLimit());
        if (condition.getOffset() >= 0) {
          offset(condition.getOffset());
        }
      }

      return this;
    }

    Builder entitySelectQuery() {
      final SelectQuery selectQuery = definition.getSelectQuery();
      if (selectQuery != null) {
        if (selectQuery.getColumns() != null) {
          columns(selectQuery.getColumns());
          selectedProperties = getSelectableProperties();
          columnsClauseFromSelectQuery = true;
        }
        else {
          columns(getAllColumnsClause());
        }
        from(selectQuery.getFrom());
        where(selectQuery.getWhere());
        orderBy(selectQuery.getOrderBy() == null ? getOrderByClause(definition.getOrderBy()) : selectQuery.getOrderBy());
      }

      return this;
    }

    Builder columns(final String columns) {
      this.columns = columns;
      return this;
    }

    Builder subquery(final String subquery) {
      return from("(" + subquery + ")" + (database.subqueryRequiresAlias() ? " as row_count" : ""));
    }

    Builder from(final String from) {
      this.from = from;
      return this;
    }

    Builder where(final Condition condition) {
      final String conditionString = condition.getConditionString(definition);
      if (!conditionString.isEmpty()) {
        where(conditionString);
      }
      return this;
    }

    Builder where(final String where) {
      if (!Util.nullOrEmpty(where)) {
        this.where.add(where);
      }
      return this;
    }

    Builder orderBy(final String orderBy) {
      this.orderBy = orderBy;
      return this;
    }

    Builder forUpdate(final boolean forUpdate) {
      this.forUpdate = forUpdate;
      return this;
    }

    Builder groupBy(final String groupBy) {
      this.groupBy = groupBy;
      return this;
    }

    Builder having(final String having) {
      this.having = having;
      return this;
    }

    Builder limit(final int limit) {
      this.limit = limit;
      return this;
    }

    Builder offset(final int offset) {
      this.offset = offset;
      return this;
    }

    String build() {
      final StringBuilder builder = new StringBuilder()
              .append(SELECT).append(columns).append(NEWLINE)
              .append(FROM).append(getFrom());
      if (!where.isEmpty()) {
        builder.append(NEWLINE).append(WHERE).append(where.get(0));
        if (where.size() > 1) {
          for (int i = 1; i < where.size(); i++) {
            builder.append(NEWLINE).append(AND).append(where.get(i));
          }
        }
      }
      if (groupBy != null) {
        builder.append(NEWLINE).append(GROUP_BY).append(groupBy);
      }
      if (having != null) {
        builder.append(NEWLINE).append(HAVING).append(having);
      }
      if (orderBy != null) {
        builder.append(NEWLINE).append(ORDER_BY).append(orderBy);
      }
      if (limit != null) {
        builder.append(NEWLINE).append(LIMIT).append(limit);
      }
      if (offset != null) {
        builder.append(NEWLINE).append(OFFSET).append(offset);
      }
      if (forUpdate) {
        final String forUpdateClause = database.getSelectForUpdateClause();
        if (!nullOrEmpty(forUpdateClause)) {
          builder.append(NEWLINE).append(forUpdateClause);
        }
      }

      return builder.toString();
    }

    private void setColumns(final SelectCondition condition) {
      final Collection<Attribute<?>> selectAttributes = condition.getSelectAttributes();
      if (selectAttributes.isEmpty()) {
        this.selectedProperties = getSelectableProperties();
        columns(getAllColumnsClause());
      }
      else {
        this.selectedProperties = getPropertiesToSelect(selectAttributes);
        columns(getColumnsClause(selectedProperties));
      }
    }

    private String getFrom() {
      return from == null ? forUpdate ? definition.getTableName() : definition.getSelectTableName() : from;
    }

    private List<ColumnProperty<?>> getPropertiesToSelect(final Collection<Attribute<?>> selectAttributes) {
      final Set<Attribute<?>> attributesToSelect = new HashSet<>(definition.getPrimaryKeyAttributes());
      selectAttributes.forEach(attribute -> {
        if (attribute instanceof ForeignKey) {
          ((ForeignKey) attribute).getReferences().forEach(reference -> attributesToSelect.add(reference.getAttribute()));
        }
        else {
          attributesToSelect.add(attribute);
        }
      });

      return attributesToSelect.stream()
              .map(definition::getColumnProperty)
              .collect(toList());
    }

    private List<ColumnProperty<?>> getSelectableProperties() {
      return selectablePropertiesCache.computeIfAbsent(definition.getEntityType(), entityType ->
              definition.getColumnProperties().stream()
                      .filter(property -> !definition.getLazyLoadedBlobProperties().contains(property))
                      .filter(ColumnProperty::isSelectable)
                      .collect(toList()));
    }

    private String getAllColumnsClause() {
      return allColumnsClauseCache.computeIfAbsent(definition.getEntityType(), type -> getColumnsClause(getSelectableProperties()));
    }

    private String getColumnsClause(final List<ColumnProperty<?>> columnProperties) {
      final StringBuilder stringBuilder = new StringBuilder();
      for (int i = 0; i < columnProperties.size(); i++) {
        final ColumnProperty<?> property = columnProperties.get(i);
        final String columnName = property.getColumnName();
        if (property instanceof SubqueryProperty) {
          stringBuilder.append("(").append(((SubqueryProperty<?>) property).getSubquery()).append(") as ").append(columnName);
        }
        else {
          final String columnExpression = property.getColumnExpression();
          stringBuilder.append(columnExpression);
          if (!columnName.equals(columnExpression)) {
            stringBuilder.append(" as ").append(columnName);
          }
        }

        if (i < columnProperties.size() - 1) {
          stringBuilder.append(", ");
        }
      }

      return stringBuilder.toString();
    }

    private String getOrderByClause(final OrderBy orderBy) {
      if (orderBy == null) {
        return null;
      }
      final List<OrderBy.OrderByAttribute> orderByAttributes = orderBy.getOrderByAttributes();
      if (orderByAttributes.isEmpty()) {
        throw new IllegalArgumentException("An order by clause must contain at least a single attribute");
      }
      if (orderByAttributes.size() == 1) {
        return getColumnOrderByClause(definition, orderByAttributes.get(0));
      }

      return orderByAttributes.stream()
              .map(orderByAttribute -> getColumnOrderByClause(definition, orderByAttribute))
              .collect(joining(", "));
    }

    private String getColumnOrderByClause(final EntityDefinition entityDefinition, final OrderBy.OrderByAttribute orderByAttribute) {
      return entityDefinition.getColumnProperty(orderByAttribute.getAttribute()).getColumnExpression() + (orderByAttribute.isAscending() ? "" : " desc");
    }
  }
}

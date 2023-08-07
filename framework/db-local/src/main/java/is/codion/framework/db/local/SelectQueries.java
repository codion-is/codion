/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.db.database.Database;
import is.codion.framework.db.condition.Criteria;
import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.query.SelectQuery;
import is.codion.framework.domain.property.ColumnProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

final class SelectQueries {

  private final Database database;
  private final Map<EntityType, List<ColumnProperty<?>>> selectablePropertiesCache = new ConcurrentHashMap<>();
  private final Map<EntityType, String> allColumnsClauseCache = new ConcurrentHashMap<>();

  SelectQueries(Database database) {
    this.database = database;
  }

  Builder builder(EntityDefinition definition) {
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
    private static final String NEWLINE = "\n";
    private static final String NULL_FIRST = " nulls first";
    private static final String NULL_LAST = " nulls last";

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

    private Builder(EntityDefinition definition) {
      this.definition = definition;
    }

    List<ColumnProperty<?>> selectedProperties() {
      return selectedProperties;
    }

    Builder selectCondition(SelectCondition condition) {
      return selectCondition(condition, true);
    }

    Builder selectCondition(SelectCondition condition, boolean setWhereClause) {
      entitySelectQuery();
      if (!columnsClauseFromSelectQuery) {
        setColumns(condition);
      }
      //default from clause is handled by from()
      if (setWhereClause) {
        where(condition.criteria());
      }
      if (groupBy == null) {
        groupBy(definition.groupByClause());
      }
      condition.orderBy().ifPresent(this::setOrderBy);
      forUpdate(condition.forUpdate());
      if (condition.limit() >= 0) {
        limit(condition.limit());
        if (condition.offset() >= 0) {
          offset(condition.offset());
        }
      }

      return this;
    }

    Builder entitySelectQuery() {
      SelectQuery selectQuery = definition.selectQuery();
      if (selectQuery != null) {
        if (selectQuery.columns() != null) {
          columns(selectQuery.columns());
          selectedProperties = selectableProperties();
          columnsClauseFromSelectQuery = true;
        }
        else {
          columns(allColumnsClause());
        }
        from(selectQuery.from());
        where(selectQuery.where());
        orderBy(selectQuery.orderBy());
        groupBy(selectQuery.groupBy());
        having(selectQuery.having());
      }

      return this;
    }

    Builder columns(String columns) {
      this.columns = columns;
      return this;
    }

    Builder subquery(String subquery) {
      return from("(" + subquery + ")" + (database.subqueryRequiresAlias() ? " as row_count" : ""));
    }

    Builder from(String from) {
      this.from = from;
      return this;
    }

    Builder where(Criteria criteria) {
      String conditionString = criteria.toString(definition);
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
      if (groupBy != null) {
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

    private void setColumns(SelectCondition condition) {
      Collection<Attribute<?>> selectAttributes = condition.selectAttributes();
      if (selectAttributes.isEmpty()) {
        this.selectedProperties = selectableProperties();
        columns(allColumnsClause());
      }
      else {
        this.selectedProperties = propertiesToSelect(selectAttributes);
        columns(columnsClause(selectedProperties));
      }
    }

    private String from() {
      return from == null ? forUpdate ? definition.tableName() : definition.selectTableName() : from;
    }

    private List<ColumnProperty<?>> propertiesToSelect(Collection<Attribute<?>> selectAttributes) {
      Set<ColumnProperty<?>> propertiesToSelect = new HashSet<>(definition.primaryKeyProperties());
      selectAttributes.forEach(attribute -> {
        if (attribute instanceof ForeignKey) {
          ((ForeignKey) attribute).references().forEach(reference ->
                  propertiesToSelect.add(definition.columnProperty(reference.attribute())));
        }
        else {
          propertiesToSelect.add(definition.columnProperty(attribute));
        }
      });

      return new ArrayList<>(propertiesToSelect);
    }

    private List<ColumnProperty<?>> selectableProperties() {
      return selectablePropertiesCache.computeIfAbsent(definition.type(), entityType ->
              definition.columnProperties().stream()
                      .filter(property -> !definition.lazyLoadedBlobProperties().contains(property))
                      .filter(ColumnProperty::isSelectable)
                      .collect(toList()));
    }

    private String allColumnsClause() {
      return allColumnsClauseCache.computeIfAbsent(definition.type(), type -> columnsClause(selectableProperties()));
    }

    private String columnsClause(List<ColumnProperty<?>> columnProperties) {
      StringBuilder stringBuilder = new StringBuilder();
      for (int i = 0; i < columnProperties.size(); i++) {
        ColumnProperty<?> property = columnProperties.get(i);
        String columnName = property.columnName();
        String columnExpression = property.columnExpression();
        stringBuilder.append(columnExpression);
        if (!columnName.equals(columnExpression)) {
          stringBuilder.append(" as ").append(columnName);
        }

        if (i < columnProperties.size() - 1) {
          stringBuilder.append(", ");
        }
      }

      return stringBuilder.toString();
    }

    private void setOrderBy(OrderBy orderBy) {
      orderBy(createOrderByClause(orderBy));
    }

    private String createOrderByClause(OrderBy orderBy) {
      List<OrderBy.OrderByAttribute> orderByAttributes = orderBy.orderByAttributes();
      if (orderByAttributes.isEmpty()) {
        throw new IllegalArgumentException("An order by clause must contain at least a single attribute");
      }
      if (orderByAttributes.size() == 1) {
        return columnOrderByClause(definition, orderByAttributes.get(0));
      }

      return orderByAttributes.stream()
              .map(orderByAttribute -> columnOrderByClause(definition, orderByAttribute))
              .collect(joining(", "));
    }

    private String columnOrderByClause(EntityDefinition entityDefinition, OrderBy.OrderByAttribute orderByAttribute) {
      return entityDefinition.columnProperty(orderByAttribute.attribute()).columnExpression() +
              (orderByAttribute.isAscending() ? "" : " desc") +
              nullOrderString(orderByAttribute.nullOrder());
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

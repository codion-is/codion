/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.Util;
import is.codion.common.db.database.Database;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.query.SelectQuery;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.SubqueryProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static is.codion.common.Util.nullOrEmpty;

final class Queries {

  static final String WHERE_SPACE_PREFIX_POSTFIX = " where ";

  private Queries() {}

  /**
   * @param tableName the table name
   * @param insertProperties the properties used to insert the given entity type
   * @return a query for inserting
   */
  static String insertQuery(final String tableName, final List<ColumnProperty<?>> insertProperties) {
    final StringBuilder queryBuilder = new StringBuilder("insert ").append("into ").append(tableName).append("(");
    final StringBuilder columnValues = new StringBuilder(") values(");
    for (int i = 0; i < insertProperties.size(); i++) {
      queryBuilder.append(insertProperties.get(i).getColumnName());
      columnValues.append("?");
      if (i < insertProperties.size() - 1) {
        queryBuilder.append(", ");
        columnValues.append(", ");
      }
    }

    return queryBuilder.append(columnValues).append(")").toString();
  }

  /**
   * @param tableName the table name
   * @param updateProperties the properties being updated
   * @param conditionString the condition string, without the WHERE keyword
   * @return a query for updating
   */
  static String updateQuery(final String tableName, final List<ColumnProperty<?>> updateProperties,
                            final String conditionString) {
    final StringBuilder queryBuilder = new StringBuilder("update ").append(tableName).append(" set ");
    for (int i = 0; i < updateProperties.size(); i++) {
      queryBuilder.append(updateProperties.get(i).getColumnName()).append(" = ?");
      if (i < updateProperties.size() - 1) {
        queryBuilder.append(", ");
      }
    }

    return queryBuilder.append(WHERE_SPACE_PREFIX_POSTFIX).append(conditionString).toString();
  }

  /**
   * @param tableName the table name
   * @param conditionString the condition string
   * @return a query for deleting the entities specified by the given condition
   */
  static String deleteQuery(final String tableName, final String conditionString) {
    return "delete from " + tableName + (conditionString.isEmpty() ? "" : WHERE_SPACE_PREFIX_POSTFIX + conditionString);
  }

  static String selectQuery(final String columnsClause, final Condition condition,
                            final EntityDefinition entityDefinition, final Database database) {
    final SelectQueryBuilder queryBuilder = new SelectQueryBuilder(entityDefinition, database);
    final SelectQuery selectQuery = entityDefinition.getSelectQuery();
    if (selectQuery != null) {
      final String selectQueryColumnsClause = selectQuery.getColumnsClause();
      queryBuilder.columns(selectQueryColumnsClause == null ? columnsClause : selectQueryColumnsClause);
      queryBuilder.from(selectQuery.getFromClause());
      queryBuilder.where(selectQuery.getWhereClause());
      queryBuilder.orderBy(selectQuery.getOrderByClause());
    }
    else {
      queryBuilder.columns(columnsClause);
    }
    queryBuilder.where(condition);
    queryBuilder.groupBy(entityDefinition.getGroupByClause())
            .having(entityDefinition.getHavingClause());
    if (condition instanceof SelectCondition) {
      final SelectCondition selectCondition = (SelectCondition) condition;
      queryBuilder.forUpdate(selectCondition.isForUpdate());
      if (queryBuilder.orderBy == null) {
        queryBuilder.orderBy(getOrderByClause(selectCondition.getOrderBy(), entityDefinition));
      }
      if (selectCondition.getLimit() >= 0) {
        queryBuilder.limit(selectCondition.getLimit());
        if (selectCondition.getOffset() >= 0) {
          queryBuilder.offset(selectCondition.getOffset());
        }
      }
    }

    return queryBuilder.build();
  }

  static String columnsClause(final List<ColumnProperty<?>> columnProperties) {
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

  /**
   * Returns an order by clause based on the given {@link OrderBy}.
   * @param entityDefinition the entity definition
   * @return a order by string
   */
  static String getOrderByClause(final OrderBy orderBy, final EntityDefinition entityDefinition) {
    if (orderBy == null) {
      return null;
    }
    final List<OrderBy.OrderByAttribute> orderByAttributes = orderBy.getOrderByAttributes();
    if (orderByAttributes.isEmpty()) {
      throw new IllegalArgumentException("An order by clause must contain at least a single attribute");
    }
    if (orderByAttributes.size() == 1) {
      return getColumnOrderByClause(entityDefinition, orderByAttributes.get(0));
    }

    return orderByAttributes.stream()
            .map(orderByAttribute -> getColumnOrderByClause(entityDefinition, orderByAttribute))
            .collect(Collectors.joining(", "));
  }

  private static String getColumnOrderByClause(final EntityDefinition entityDefinition, final OrderBy.OrderByAttribute orderByAttribute) {
    return entityDefinition.getColumnProperty(orderByAttribute.getAttribute()).getColumnExpression() + (orderByAttribute.isAscending() ? "" : " desc");
  }

  static final class SelectQueryBuilder {

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
    private final Database database;

    private final List<String> where = new ArrayList<>(1);

    private String columns;
    private String from;
    private String orderBy;
    private boolean forUpdate;
    private String groupBy;
    private String having;
    private Integer limit;
    private Integer offset;

    SelectQueryBuilder(final EntityDefinition definition, final Database database) {
      this.definition = definition;
      this.database = database;
    }

    SelectQueryBuilder columns(final String columns) {
      this.columns = columns;
      return this;
    }

    SelectQueryBuilder from(final String from) {
      this.from = from;
      return this;
    }

    SelectQueryBuilder where(final Condition condition) {
      final String conditionString = condition.getConditionString(definition);
      if (!conditionString.isEmpty()) {
        where(conditionString);
      }
      return this;
    }

    SelectQueryBuilder where(final String where) {
      if (!Util.nullOrEmpty(where)) {
        this.where.add(where);
      }
      return this;
    }

    SelectQueryBuilder orderBy(final String orderBy) {
      this.orderBy = orderBy;
      return this;
    }

    SelectQueryBuilder forUpdate(final boolean forUpdate) {
      this.forUpdate = forUpdate;
      return this;
    }

    SelectQueryBuilder groupBy(final String groupBy) {
      this.groupBy = groupBy;
      return this;
    }

    SelectQueryBuilder having(final String having) {
      this.having = having;
      return this;
    }

    SelectQueryBuilder limit(final int limit) {
      this.limit = limit;
      return this;
    }

    SelectQueryBuilder offset(final int offset) {
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

    private String getFrom() {
      return from == null ? forUpdate ? definition.getTableName() : definition.getSelectTableName() : from;
    }
  }
}

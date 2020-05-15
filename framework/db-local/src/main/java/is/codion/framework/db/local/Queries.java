/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.db.database.Database;
import is.codion.framework.db.condition.EntityCondition;
import is.codion.framework.db.condition.EntitySelectCondition;
import is.codion.framework.db.condition.WhereCondition;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.SubqueryProperty;

import java.util.ArrayList;
import java.util.List;

import static is.codion.common.Util.nullOrEmpty;

final class Queries {

  static final String WHERE_SPACE_PREFIX_POSTFIX = " where ";

  private Queries() {}

  /**
   * @param tableName the table name
   * @param insertProperties the properties used to insert the given entity type
   * @return a query for inserting
   */
  static String insertQuery(final String tableName, final List<ColumnProperty> insertProperties) {
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
   * @param whereClause the where clause, without the WHERE keyword
   * @return a query for updating
   */
  static String updateQuery(final String tableName, final List<ColumnProperty> updateProperties,
                            final String whereClause) {
    final StringBuilder queryBuilder = new StringBuilder("update ").append(tableName).append(" set ");
    for (int i = 0; i < updateProperties.size(); i++) {
      queryBuilder.append(updateProperties.get(i).getColumnName()).append(" = ?");
      if (i < updateProperties.size() - 1) {
        queryBuilder.append(", ");
      }
    }

    return queryBuilder.append(WHERE_SPACE_PREFIX_POSTFIX).append(whereClause).toString();
  }

  /**
   * @param tableName the table name
   * @param whereClause the where clause
   * @return a query for deleting the entities specified by the given condition
   */
  static String deleteQuery(final String tableName, final String whereClause) {
    return "delete from " + tableName + (whereClause.isEmpty() ? "" : WHERE_SPACE_PREFIX_POSTFIX + whereClause);
  }

  /**
   * Generates a sql select query with the given parameters
   * @param tableName the name of the table from which to select
   * @param columnsClause the columns to select, example: "col1, col2"
   * @return the generated sql query
   */
  static String selectQuery(final String tableName, final String columnsClause) {
    return selectQuery(tableName, columnsClause, null, null);
  }

  /**
   * Generates a sql select query with the given parameters
   * @param tableName the name of the table from which to select
   * @param columnsClause the columns to select, example: "col1, col2"
   * @param whereClause the where condition without the WHERE keyword
   * @param orderByClause a string specifying the columns 'ORDER BY' clause,
   * "col1, col2" as input results in the following order by clause "order by col1, col2"
   * @return the generated sql query
   */
  static String selectQuery(final String tableName, final String columnsClause, final String whereClause,
                            final String orderByClause) {
    final StringBuilder queryBuilder = new StringBuilder("select ").append(columnsClause).append(" from ").append(tableName);
    if (!nullOrEmpty(whereClause)) {
      queryBuilder.append(WHERE_SPACE_PREFIX_POSTFIX).append(whereClause);
    }
    if (!nullOrEmpty(orderByClause)) {
      queryBuilder.append(" order by ").append(orderByClause);
    }

    return queryBuilder.toString();
  }

  static String selectQuery(final String columnsClause, final EntityCondition entityCondition,
                            final WhereCondition whereCondition, final EntityDefinition entityDefinition,
                            final Database database) {
    final boolean isForUpdate = entityCondition instanceof EntitySelectCondition &&
            ((EntitySelectCondition) entityCondition).isForUpdate();
    boolean containsWhereClause = false;
    String selectQuery = entityDefinition.getSelectQuery();
    if (selectQuery == null) {
      selectQuery = selectQuery(isForUpdate ? entityDefinition.getTableName() : entityDefinition.getSelectTableName(), columnsClause);
    }
    else {
      containsWhereClause = entityDefinition.selectQueryContainsWhereClause();
    }

    final StringBuilder queryBuilder = new StringBuilder(selectQuery);
    final String whereClause = whereCondition.getWhereClause();
    if (whereClause.length() > 0) {
      queryBuilder.append(containsWhereClause ? " and " : Queries.WHERE_SPACE_PREFIX_POSTFIX).append(whereClause);
    }
    if (isForUpdate) {
      addForUpdate(queryBuilder, database);
    }
    else {
      addGroupHavingOrderByAndLimitClauses(queryBuilder, entityCondition, entityDefinition);
    }

    return queryBuilder.toString();
  }

  static String columnsClause(final List<ColumnProperty> columnProperties) {
    final StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < columnProperties.size(); i++) {
      final ColumnProperty property = columnProperties.get(i);
      if (property instanceof SubqueryProperty) {
        stringBuilder.append("(").append(((SubqueryProperty) property).getSubQuery())
                .append(") as ").append(property.getColumnName());
      }
      else {
        stringBuilder.append(property.getColumnName());
      }

      if (i < columnProperties.size() - 1) {
        stringBuilder.append(", ");
      }
    }

    return stringBuilder.toString();
  }

  /**
   * Returns a order by clause based on the given {@link OrderBy}.
   * @param entityDefinition the entity definition
   * @return a order by string
   */
  static String getOrderByClause(final OrderBy orderBy, final EntityDefinition entityDefinition) {
    final List<OrderBy.OrderByProperty> orderByProperties = orderBy.getOrderByProperties();
    if (orderByProperties.isEmpty()) {
      throw new IllegalArgumentException("An order by clause must contain at least a single property");
    }
    final String columnsClause;
    if (orderByProperties.size() == 1) {
      final OrderBy.OrderByProperty orderByProperty = orderByProperties.get(0);
      columnsClause = getColumnOrderByClause(entityDefinition, orderByProperty);
    }
    else {
      final List<String> orderByColumnClauses = new ArrayList<>(orderByProperties.size());
      for (final OrderBy.OrderByProperty property : orderByProperties) {
        orderByColumnClauses.add(getColumnOrderByClause(entityDefinition, property));
      }
      columnsClause = String.join(", ", orderByColumnClauses);
    }

    return "order by " + columnsClause;
  }

  private static String getColumnOrderByClause(final EntityDefinition entityDefinition, final OrderBy.OrderByProperty property) {
    return entityDefinition.getColumnProperty(property.getPropertyId()).getColumnName() + (property.isAscending() ? "" : " desc");
  }

  private static void addForUpdate(final StringBuilder queryBuilder, final Database database) {
    final Database.SelectForUpdateSupport selectForUpdateSupport = database.getSelectForUpdateSupport();
    if (!selectForUpdateSupport.equals(Database.SelectForUpdateSupport.NONE)) {
      queryBuilder.append(" for update");
      if (selectForUpdateSupport.equals(Database.SelectForUpdateSupport.FOR_UPDATE_NOWAIT)) {
        queryBuilder.append(" nowait");
      }
    }
  }

  private static void addGroupHavingOrderByAndLimitClauses(final StringBuilder queryBuilder,
                                                           final EntityCondition condition,
                                                           final EntityDefinition entityDefinition) {
    final String groupByClause = entityDefinition.getGroupByClause();
    if (groupByClause != null) {
      queryBuilder.append(" group by ").append(groupByClause);
    }
    final String havingClause = entityDefinition.getHavingClause();
    if (havingClause != null) {
      queryBuilder.append(" having ").append(havingClause);
    }
    if (condition instanceof EntitySelectCondition) {
      final EntitySelectCondition selectCondition = (EntitySelectCondition) condition;
      final OrderBy orderBy = selectCondition.getOrderBy();
      if (orderBy != null) {
        queryBuilder.append(" ").append(getOrderByClause(orderBy, entityDefinition));
      }
      if (selectCondition.getLimit() > 0) {
        queryBuilder.append(" limit ").append(selectCondition.getLimit());
        if (selectCondition.getOffset() > 0) {
          queryBuilder.append(" offset ").append(selectCondition.getOffset());
        }
      }
    }
  }
}

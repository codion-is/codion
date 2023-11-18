/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.framework.domain.entity.attribute.ColumnDefinition;

import java.util.List;

final class Queries {

  private static final String WHERE_SPACE_POSTFIX = "WHERE ";
  private static final String NEWLINE = "\n";

  private Queries() {}

  /**
   * @param tableName the table name
   * @param columnDefinitions the column definitions used to insert the given entity type
   * @return a query for inserting
   */
  static String insertQuery(String tableName, List<ColumnDefinition<?>> columnDefinitions) {
    StringBuilder queryBuilder = new StringBuilder("INSERT ").append("INTO ").append(tableName).append("(");
    StringBuilder columnValues = new StringBuilder(")").append(NEWLINE).append("VALUES(");
    for (int i = 0; i < columnDefinitions.size(); i++) {
      queryBuilder.append(columnDefinitions.get(i).columnName());
      columnValues.append("?");
      if (i < columnDefinitions.size() - 1) {
        queryBuilder.append(", ");
        columnValues.append(", ");
      }
    }

    return queryBuilder.append(columnValues).append(")").toString();
  }

  /**
   * @param tableName the table name
   * @param columnDefinitions the column definitions being updated
   * @param conditionString the condition string, without the WHERE keyword
   * @return a query for updating
   */
  static String updateQuery(String tableName, List<ColumnDefinition<?>> columnDefinitions,
                            String conditionString) {
    StringBuilder queryBuilder = new StringBuilder("UPDATE ").append(tableName).append(NEWLINE).append("SET ");
    for (int i = 0; i < columnDefinitions.size(); i++) {
      queryBuilder.append(columnDefinitions.get(i).columnName()).append(" = ?");
      if (i < columnDefinitions.size() - 1) {
        queryBuilder.append(", ");
      }
    }

    return queryBuilder.append(NEWLINE).append(WHERE_SPACE_POSTFIX).append(conditionString).toString();
  }

  /**
   * @param tableName the table name
   * @param conditionString the condition string
   * @return a query for deleting the entities specified by the given condition
   */
  static String deleteQuery(String tableName, String conditionString) {
    return "DELETE FROM " + tableName + (conditionString.isEmpty() ? "" : NEWLINE + WHERE_SPACE_POSTFIX + conditionString);
  }
}

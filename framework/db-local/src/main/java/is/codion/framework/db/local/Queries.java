/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.framework.domain.property.ColumnProperty;

import java.util.List;

final class Queries {

  private static final String WHERE_SPACE_POSTFIX = "where ";
  private static final String NEWLINE = "\n";

  private Queries() {}

  /**
   * @param tableName the table name
   * @param insertProperties the properties used to insert the given entity type
   * @return a query for inserting
   */
  static String insertQuery(String tableName, List<ColumnProperty<?>> insertProperties) {
    StringBuilder queryBuilder = new StringBuilder("insert ").append("into ").append(tableName).append("(");
    StringBuilder columnValues = new StringBuilder(")").append(NEWLINE).append("values(");
    for (int i = 0; i < insertProperties.size(); i++) {
      queryBuilder.append(insertProperties.get(i).columnName());
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
  static String updateQuery(String tableName, List<ColumnProperty<?>> updateProperties,
                            String conditionString) {
    StringBuilder queryBuilder = new StringBuilder("update ").append(tableName).append(NEWLINE).append("set ");
    for (int i = 0; i < updateProperties.size(); i++) {
      queryBuilder.append(updateProperties.get(i).columnName()).append(" = ?");
      if (i < updateProperties.size() - 1) {
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
    return "delete from " + tableName + (conditionString.isEmpty() ? "" : NEWLINE + WHERE_SPACE_POSTFIX + conditionString);
  }
}

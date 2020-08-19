/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.metadata;

import is.codion.common.db.result.ResultPacker;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;

final class ColumnPacker implements ResultPacker<Column> {

  private final String tableName;
  private final Collection<PrimaryKeyColumn> primaryKeyColumns;
  private final Collection<ForeignKeyColumn> foreignKeyColumns;

  ColumnPacker(final String tableName, final Collection<PrimaryKeyColumn> primaryKeyColumns,
               final Collection<ForeignKeyColumn> foreignKeyColumns) {
    this.tableName = tableName;
    this.primaryKeyColumns = primaryKeyColumns;
    this.foreignKeyColumns = foreignKeyColumns;
  }

  @Override
  public Column fetch(final ResultSet resultSet) throws SQLException {
    final int dataType = resultSet.getInt("DATA_TYPE");
    int decimalDigits = resultSet.getInt("DECIMAL_DIGITS");
    if (resultSet.wasNull()) {
      decimalDigits = -1;
    }
    final Class<?> typeClass = translateTypeName(dataType, decimalDigits);
    if (typeClass != null) {
      final String tableName = resultSet.getString("TABLE_NAME");
      final String columnName = resultSet.getString("COLUMN_NAME");

      return new Column(columnName, typeClass,
              resultSet.getInt("COLUMN_SIZE"), decimalDigits, resultSet.getInt("NULLABLE"),
              resultSet.getObject("COLUMN_DEF") != null, resultSet.getString("REMARKS"),
              getPrimaryKeyColumnIndex(columnName));
    }

    return null;
  }

  private int getPrimaryKeyColumnIndex(final String columnName) {
    return primaryKeyColumns.stream().filter(primaryKeyColumn ->
            columnName.equals(primaryKeyColumn.getColumnName())).findFirst().map(PrimaryKeyColumn::getKeySeq).orElse(-1);
  }

  private ForeignKeyColumn getForeignKeyColumn(final String tableName, final String columnName) {
    return foreignKeyColumns.stream().filter(foreignKeyColumn ->
            foreignKeyColumn.getFkTableName().equals(tableName)
                    && foreignKeyColumn.getFkColumnName().equals(columnName)).findFirst().orElse(null);
  }

  private static Class<?> translateTypeName(final int sqlType, final int decimalDigits) {
    switch (sqlType) {
      case Types.BIGINT:
        return Long.class;
      case Types.INTEGER:
      case Types.ROWID:
      case Types.SMALLINT:
        return Integer.class;
      case Types.CHAR:
        return Character.class;
      case Types.DATE:
        return LocalDate.class;
      case Types.DECIMAL:
      case Types.DOUBLE:
      case Types.FLOAT:
      case Types.REAL:
      case Types.NUMERIC:
        return decimalDigits == 0 ? Integer.class : Double.class;
      case Types.TIME:
        return LocalTime.class;
      case Types.TIMESTAMP:
        return LocalDateTime.class;
      case Types.LONGVARCHAR:
      case Types.VARCHAR:
        return String.class;
      case Types.BLOB:
        return byte[].class;
      case Types.BOOLEAN:
        return Boolean.class;
      default://unsupported data type
        return null;
    }
  }
}

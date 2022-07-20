/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
import java.util.List;

final class ColumnPacker implements ResultPacker<Column> {

  private final Collection<PrimaryKeyColumn> primaryKeyColumns;
  private final List<ForeignKeyColumn> foreignKeyColumns;

  ColumnPacker(Collection<PrimaryKeyColumn> primaryKeyColumns, List<ForeignKeyColumn> foreignKeyColumns) {
    this.primaryKeyColumns = primaryKeyColumns;
    this.foreignKeyColumns = foreignKeyColumns;
  }

  @Override
  public Column fetch(ResultSet resultSet) throws SQLException {
    int dataType = resultSet.getInt("DATA_TYPE");
    int decimalDigits = resultSet.getInt("DECIMAL_DIGITS");
    if (resultSet.wasNull()) {
      decimalDigits = -1;
    }
    Class<?> typeClass = getTypeClass(dataType, decimalDigits);
    if (typeClass != null) {
      String columnName = resultSet.getString("COLUMN_NAME");
      try {
        return new Column(columnName, typeClass,
                resultSet.getInt("ORDINAL_POSITION"),
                resultSet.getInt("COLUMN_SIZE"), decimalDigits,
                resultSet.getInt("NULLABLE"),
                resultSet.getString("COLUMN_DEF"),
                resultSet.getString("REMARKS"),
                getPrimaryKeyColumnIndex(columnName),
                isForeignKeyColumn(columnName));
      }
      catch (SQLException e) {
        System.err.println("Exception fetching column: " + columnName + ", " + e.getMessage());
        return null;
      }
    }

    return null;
  }

  private int getPrimaryKeyColumnIndex(String columnName) {
    return primaryKeyColumns.stream()
            .filter(primaryKeyColumn -> columnName.equals(primaryKeyColumn.getColumnName()))
            .findFirst()
            .map(PrimaryKeyColumn::getIndex)
            .orElse(-1);
  }

  private boolean isForeignKeyColumn(String columnName) {
    return foreignKeyColumns.stream()
            .anyMatch(foreignKeyColumn -> foreignKeyColumn.getFkColumnName().equals(columnName));
  }

  private static Class<?> getTypeClass(int sqlType, int decimalDigits) {
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
      case Types.BIT:
      case Types.BOOLEAN:
        return Boolean.class;
      default://unsupported data type
        return null;
    }
  }
}

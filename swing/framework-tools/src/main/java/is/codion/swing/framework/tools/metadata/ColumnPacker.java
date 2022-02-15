/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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

  ColumnPacker(final Collection<PrimaryKeyColumn> primaryKeyColumns, final List<ForeignKeyColumn> foreignKeyColumns) {
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
      final String columnName = resultSet.getString("COLUMN_NAME");

      return new Column(columnName, typeClass,
              resultSet.getInt("ORDINAL_POSITION"), resultSet.getInt("COLUMN_SIZE"), decimalDigits,
              resultSet.getInt("NULLABLE"), resultSet.getObject("COLUMN_DEF") != null,
              resultSet.getString("REMARKS"), getPrimaryKeyColumnIndex(columnName), isForeignKeyColumn(columnName));
    }

    return null;
  }

  private int getPrimaryKeyColumnIndex(final String columnName) {
    return primaryKeyColumns.stream()
            .filter(primaryKeyColumn -> columnName.equals(primaryKeyColumn.getColumnName()))
            .findFirst()
            .map(PrimaryKeyColumn::getIndex)
            .orElse(-1);
  }

  private boolean isForeignKeyColumn(final String columnName) {
    return foreignKeyColumns.stream()
            .anyMatch(foreignKeyColumn -> foreignKeyColumn.getFkColumnName().equals(columnName));
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
      case Types.BIT:
      case Types.BOOLEAN:
        return Boolean.class;
      default://unsupported data type
        return null;
    }
  }
}

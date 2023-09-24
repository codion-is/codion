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
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model.tools.metadata;

import is.codion.common.db.result.ResultPacker;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.Collection;
import java.util.List;

final class ColumnPacker implements ResultPacker<MetadataColumn> {

  private static final String YES = "YES";

  private final Collection<PrimaryKeyColumn> primaryKeyColumns;
  private final List<ForeignKeyColumn> foreignKeyColumns;

  ColumnPacker(Collection<PrimaryKeyColumn> primaryKeyColumns, List<ForeignKeyColumn> foreignKeyColumns) {
    this.primaryKeyColumns = primaryKeyColumns;
    this.foreignKeyColumns = foreignKeyColumns;
  }

  @Override
  public MetadataColumn get(ResultSet resultSet) throws SQLException {
    int dataType = resultSet.getInt("DATA_TYPE");
    int decimalDigits = resultSet.getInt("DECIMAL_DIGITS");
    if (resultSet.wasNull()) {
      decimalDigits = -1;
    }
    Class<?> columnClass = columnClass(dataType, decimalDigits);
    if (columnClass != null) {
      String columnName = resultSet.getString("COLUMN_NAME");
      try {
        return new MetadataColumn(columnName, columnClass,
                resultSet.getInt("ORDINAL_POSITION"),
                resultSet.getInt("COLUMN_SIZE"), decimalDigits,
                resultSet.getInt("NULLABLE"),
                resultSet.getString("COLUMN_DEF"),
                resultSet.getString("REMARKS"),
                primaryKeyColumnIndex(columnName),
                isForeignKeyColumn(columnName),
                YES.equals(resultSet.getString("IS_AUTOINCREMENT")),
                YES.equals(resultSet.getString("IS_GENERATEDCOLUMN")));
      }
      catch (SQLException e) {
        System.err.println("Exception fetching column: " + columnName + ", " + e.getMessage());
        return null;
      }
    }

    return null;
  }

  private int primaryKeyColumnIndex(String columnName) {
    return primaryKeyColumns.stream()
            .filter(primaryKeyColumn -> columnName.equals(primaryKeyColumn.columnName()))
            .findFirst()
            .map(PrimaryKeyColumn::index)
            .orElse(-1);
  }

  private boolean isForeignKeyColumn(String columnName) {
    return foreignKeyColumns.stream()
            .anyMatch(foreignKeyColumn -> foreignKeyColumn.fkColumnName().equals(columnName));
  }

  private static Class<?> columnClass(int sqlType, int decimalDigits) {
    switch (sqlType) {
      case Types.BIGINT:
        return Long.class;
      case Types.INTEGER:
      case Types.ROWID:
        return Integer.class;
      case Types.SMALLINT:
        return Short.class;
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
      case Types.TIME_WITH_TIMEZONE:
        return OffsetTime.class;
      case Types.TIMESTAMP:
        return LocalDateTime.class;
      case Types.TIMESTAMP_WITH_TIMEZONE:
        return OffsetDateTime.class;
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

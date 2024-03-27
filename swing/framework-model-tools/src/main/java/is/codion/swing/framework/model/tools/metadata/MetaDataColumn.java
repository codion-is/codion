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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
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

import static java.util.Objects.requireNonNull;

/**
 * A database metadata column.
 */
public final class MetaDataColumn {

	private final String columnName;
	private final int dataType;
	private final String typeName;
	private final Class<?> columnClass;
	private final int position;
	private final int columnSize;
	private final int decimalDigits;
	private final int nullable;
	private final String defaultValue;
	private final String comment;
	private final int primaryKeyIndex;
	private final boolean foreignKeyColumn;
	private final boolean autoIncrement;
	private final boolean generated;

	MetaDataColumn(String columnName, int dataType, String typeName, Class<?> columnClass, int position, int columnSize,
								 int decimalDigits, int nullable, String defaultValue, String comment,
								 int primaryKeyIndex, boolean foreignKeyColumn, boolean autoIncrement, boolean generated) {
		this.columnName = requireNonNull(columnName);
		this.columnClass = requireNonNull(columnClass);
		this.dataType = dataType;
		this.typeName = typeName;
		this.position = position;
		this.columnSize = columnSize;
		this.decimalDigits = decimalDigits;
		this.nullable = nullable;
		this.defaultValue = defaultValue;
		this.comment = comment == null ? null : comment.trim().replace("\"", "\\\"");
		this.primaryKeyIndex = primaryKeyIndex;
		this.foreignKeyColumn = foreignKeyColumn;
		this.autoIncrement = autoIncrement;
		this.generated = generated;
	}

	public String columnName() {
		return columnName;
	}

	public int dataType() {
		return dataType;
	}

	public String typeName() {
		return typeName;
	}

	public int position() {
		return position;
	}

	public boolean primaryKeyColumn() {
		return primaryKeyIndex != -1;
	}

	public int primaryKeyIndex() {
		return primaryKeyIndex;
	}

	public boolean foreignKeyColumn() {
		return foreignKeyColumn;
	}

	public Class<?> columnClass() {
		return columnClass;
	}

	public String defaultValue() {
		return defaultValue;
	}

	public int nullable() {
		return nullable;
	}

	public int columnSize() {
		return columnSize;
	}

	public int decimalDigits() {
		return decimalDigits;
	}

	public String comment() {
		return comment;
	}

	public boolean autoIncrement() {
		return autoIncrement;
	}

	public boolean generated() {
		return generated;
	}

	@Override
	public String toString() {
		return columnName;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		MetaDataColumn column = (MetaDataColumn) object;

		return columnName.equals(column.columnName);
	}

	@Override
	public int hashCode() {
		return columnName.hashCode();
	}

	static final class ColumnPacker implements ResultPacker<MetaDataColumn> {

		private static final String YES = "YES";

		private final Collection<MetaDataPrimaryKeyColumn> primaryKeyColumns;
		private final List<MetaDataForeignKeyColumn> foreignKeyColumns;

		ColumnPacker(Collection<MetaDataPrimaryKeyColumn> primaryKeyColumns, List<MetaDataForeignKeyColumn> foreignKeyColumns) {
			this.primaryKeyColumns = primaryKeyColumns;
			this.foreignKeyColumns = foreignKeyColumns;
		}

		@Override
		public MetaDataColumn get(ResultSet resultSet) throws SQLException {
			int dataType = resultSet.getInt("DATA_TYPE");
			int decimalDigits = resultSet.getInt("DECIMAL_DIGITS");
			if (resultSet.wasNull()) {
				decimalDigits = -1;
			}
			Class<?> columnClass = columnClass(dataType, decimalDigits);
			String columnName = resultSet.getString("COLUMN_NAME");
			String typeName = resultSet.getString("TYPE_NAME");
			try {
				return new MetaDataColumn(columnName, dataType, typeName, columnClass,
								resultSet.getInt("ORDINAL_POSITION"),
								resultSet.getInt("COLUMN_SIZE"), decimalDigits,
								resultSet.getInt("NULLABLE"),
								resultSet.getString("COLUMN_DEF"),
								resultSet.getString("REMARKS"),
								primaryKeyColumnIndex(columnName),
								foreignKeyColumn(columnName),
								YES.equals(resultSet.getString("IS_AUTOINCREMENT")),
								YES.equals(resultSet.getString("IS_GENERATEDCOLUMN")));
			}
			catch (SQLException e) {
				System.err.println("Exception fetching column: " + columnName + ", " + e.getMessage());
				throw e;
			}
		}

		private int primaryKeyColumnIndex(String columnName) {
			return primaryKeyColumns.stream()
							.filter(primaryKeyColumn -> columnName.equals(primaryKeyColumn.columnName()))
							.findFirst()
							.map(MetaDataPrimaryKeyColumn::index)
							.orElse(-1);
		}

		private boolean foreignKeyColumn(String columnName) {
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
				default:
					return Object.class;
			}
		}
	}
}

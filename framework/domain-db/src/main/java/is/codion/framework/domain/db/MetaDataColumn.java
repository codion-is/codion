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
 * Copyright (c) 2020 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.db;

import is.codion.common.db.result.ResultPacker;

import java.math.BigDecimal;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

final class MetaDataColumn {

	// Types some drivers report via a vendor type code, or a less specific standard one, keyed by the normalized type name
	private static final Map<String, Class<?>> TYPE_NAMES = new HashMap<>();

	static {
		TYPE_NAMES.put("TIMESTAMPTZ", OffsetDateTime.class);// PostgreSQL, reported as TIMESTAMP
		TYPE_NAMES.put("TIMETZ", OffsetTime.class);// PostgreSQL, reported as TIME
		TYPE_NAMES.put("TIMESTAMP WITH TIME ZONE", OffsetDateTime.class);// Oracle, a vendor type code
		TYPE_NAMES.put("DATETIMEOFFSET", OffsetDateTime.class);// SQL Server, a vendor type code
		TYPE_NAMES.put("UUID", UUID.class);// H2 and HSQLDB report BINARY, PostgreSQL OTHER
		TYPE_NAMES.put("BINARY_FLOAT", Double.class);// Oracle, a vendor type code
		TYPE_NAMES.put("BINARY_DOUBLE", Double.class);// Oracle, a vendor type code
	}

	private final String name;
	private final int dataType;
	private final String typeName;
	private final Class<?> type;
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

	private MetaDataColumn(String name, int dataType, String typeName, Class<?> type, int position, int columnSize,
												 int decimalDigits, int nullable, String defaultValue, String comment,
												 int primaryKeyIndex, boolean foreignKeyColumn, boolean autoIncrement, boolean generated) {
		this.name = requireNonNull(name);
		this.type = requireNonNull(type);
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

	String name() {
		return name;
	}

	int dataType() {
		return dataType;
	}

	String typeName() {
		return typeName;
	}

	int position() {
		return position;
	}

	boolean primaryKeyColumn() {
		return primaryKeyIndex != -1;
	}

	int primaryKeyIndex() {
		return primaryKeyIndex;
	}

	boolean foreignKeyColumn() {
		return foreignKeyColumn;
	}

	Class<?> type() {
		return type;
	}

	String defaultValue() {
		return defaultValue;
	}

	int nullable() {
		return nullable;
	}

	int columnSize() {
		return columnSize;
	}

	int decimalDigits() {
		return decimalDigits;
	}

	String comment() {
		return comment;
	}

	boolean autoIncrement() {
		return autoIncrement;
	}

	boolean generated() {
		return generated;
	}

	@Override
	public String toString() {
		return name();
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

		return name.equals(column.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	/**
	 * @param sqlType the {@link Types} code reported by the driver
	 * @param typeName the database specific type name
	 * @param columnSize the column size, the precision of a numeric column
	 * @param decimalDigits the decimal digits, the scale of a numeric column, -1 if unknown
	 * @param declaredTypes true if the type is based on the declared type name instead of the type code, which
	 * for SQLite is derived from the type name by the driver, without the Java type in mind
	 * @return the Java type for the given column
	 */
	static Class<?> columnType(int sqlType, String typeName, int columnSize, int decimalDigits, boolean declaredTypes) {
		String name = typeName == null ? "" : typeName.toUpperCase(Locale.ROOT)
						.replaceAll("\\(.*?\\)", "")
						.replaceAll("\\s+", " ")
						.trim();
		if (declaredTypes) {
			return columnType(declaredType(name, sqlType), columnSize, decimalDigits);
		}
		Class<?> type = TYPE_NAMES.get(name);

		return type == null ? columnType(sqlType, columnSize, decimalDigits) : type;
	}

	private static Class<?> columnType(int sqlType, int columnSize, int decimalDigits) {
		switch (sqlType) {
			case Types.BIGINT:
				return Long.class;
			case Types.INTEGER:
				return Integer.class;
			case Types.SMALLINT:
			case Types.TINYINT:
				return Short.class;
			case Types.DECIMAL:
			case Types.NUMERIC:
				return numericType(columnSize, decimalDigits);
			case Types.DOUBLE:
			case Types.FLOAT:
			case Types.REAL:
				return Double.class;
			case Types.CHAR:
			case Types.NCHAR:
				return columnSize == 1 ? Character.class : String.class;
			case Types.VARCHAR:
			case Types.NVARCHAR:
			case Types.LONGVARCHAR:
			case Types.LONGNVARCHAR:
			case Types.CLOB:
			case Types.NCLOB:
				return String.class;
			case Types.DATE:
				return LocalDate.class;
			case Types.TIME:
				return LocalTime.class;
			case Types.TIME_WITH_TIMEZONE:
				return OffsetTime.class;
			case Types.TIMESTAMP:
				return LocalDateTime.class;
			case Types.TIMESTAMP_WITH_TIMEZONE:
				return OffsetDateTime.class;
			case Types.BINARY:
			case Types.VARBINARY:
			case Types.LONGVARBINARY:
			case Types.BLOB:
				return byte[].class;
			case Types.BIT:
			case Types.BOOLEAN:
				return Boolean.class;
			default:
				return Object.class;
		}
	}

	// A whole number is an Integer or a Long depending on its precision, which may exceed that of a Long,
	// a decimal number, or one of unknown scale, a BigDecimal, the exact type
	private static Class<?> numericType(int precision, int scale) {
		if (scale == 0 && precision > 0) {
			return precision <= 9 ? Integer.class : Long.class;
		}

		return BigDecimal.class;
	}

	private static int declaredType(String typeName, int sqlType) {
		switch (typeName) {
			case "INT":
				return Types.INTEGER;
			case "DATETIME":
				return Types.TIMESTAMP;
			case "TEXT":
				return Types.VARCHAR;
			default:
				try {
					return JDBCType.valueOf(typeName).getVendorTypeNumber();
				}
				catch (IllegalArgumentException e) {
					return sqlType;
				}
		}
	}

	static final class ColumnPacker implements ResultPacker<MetaDataColumn> {

		private static final String YES = "YES";
		private static final String IS_GENERATEDCOLUMN = "IS_GENERATEDCOLUMN";

		private final Collection<MetaDataPrimaryKeyColumn> primaryKeyColumns;
		private final List<MetaDataForeignKeyColumn> foreignKeyColumns;
		private final boolean declaredTypes;

		private Boolean generatedColumnReported;

		ColumnPacker(Collection<MetaDataPrimaryKeyColumn> primaryKeyColumns, List<MetaDataForeignKeyColumn> foreignKeyColumns,
								 boolean declaredTypes) {
			this.primaryKeyColumns = primaryKeyColumns;
			this.foreignKeyColumns = foreignKeyColumns;
			this.declaredTypes = declaredTypes;
		}

		@Override
		public MetaDataColumn get(ResultSet resultSet) throws SQLException {
			int dataType = resultSet.getInt("DATA_TYPE");
			int decimalDigits = resultSet.getInt("DECIMAL_DIGITS");
			if (resultSet.wasNull()) {
				decimalDigits = -1;
			}
			int columnSize = resultSet.getInt("COLUMN_SIZE");
			String columnName = resultSet.getString("COLUMN_NAME");
			String typeName = resultSet.getString("TYPE_NAME");
			Class<?> columnType = columnType(dataType, typeName, columnSize, decimalDigits, declaredTypes);
			try {
				return new MetaDataColumn(columnName, dataType, typeName, columnType,
								resultSet.getInt("ORDINAL_POSITION"),
								columnSize, decimalDigits,
								resultSet.getInt("NULLABLE"),
								resultSet.getString("COLUMN_DEF"),
								resultSet.getString("REMARKS"),
								primaryKeyColumnIndex(columnName),
								foreignKeyColumn(columnName),
								YES.equals(resultSet.getString("IS_AUTOINCREMENT")),
								generated(resultSet));
			}
			catch (SQLException e) {
				System.err.println("Exception fetching column: " + columnName + ", " + e.getMessage());
				throw e;
			}
		}

		// IS_GENERATEDCOLUMN was added in JDBC 4.1, older drivers do not report it, mssql-jdbc 6.4 for one
		private boolean generated(ResultSet resultSet) throws SQLException {
			if (generatedColumnReported == null) {
				generatedColumnReported = reported(resultSet.getMetaData(), IS_GENERATEDCOLUMN);
			}

			return generatedColumnReported && YES.equals(resultSet.getString(IS_GENERATEDCOLUMN));
		}

		private static boolean reported(ResultSetMetaData metaData, String columnName) throws SQLException {
			for (int i = 1; i <= metaData.getColumnCount(); i++) {
				if (columnName.equalsIgnoreCase(metaData.getColumnLabel(i))) {
					return true;
				}
			}

			return false;
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
	}
}

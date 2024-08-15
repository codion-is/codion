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
package is.codion.framework.domain.db;

import is.codion.common.db.result.ResultPacker;
import is.codion.framework.domain.db.MetaDataColumn.ColumnPacker;
import is.codion.framework.domain.db.MetaDataForeignKeyColumn.ForeignKeyColumnPacker;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

final class MetaDataTable {

	private final MetaDataSchema schema;
	private final String tableName;
	private final String tableType;
	private final String comment;
	private final List<MetaDataForeignKeyColumn> foreignKeyColumns;
	private final Map<String, MetaDataColumn> columns = new LinkedHashMap<>();
	private final List<MetaDataForeignKeyConstraint> foreignKeys = new ArrayList<>();

	private MetaDataTable(MetaDataSchema schema, String tableName, String tableType, String comment,
												List<MetaDataColumn> columns, List<MetaDataForeignKeyColumn> foreignKeyColumns) {
		this.schema = requireNonNull(schema);
		this.tableName = requireNonNull(tableName);
		this.tableType = requireNonNull(tableType);
		this.comment = comment == null ? null : comment.trim().replace("\"", "\\\"");
		this.foreignKeyColumns = requireNonNull(foreignKeyColumns);
		requireNonNull(columns).forEach(column -> this.columns.put(column.columnName(), column));
	}

	String tableName() {
		return tableName;
	}

	MetaDataSchema schema() {
		return schema;
	}

	String tableType() {
		return tableType;
	}

	String comment() {
		return comment;
	}

	List<MetaDataColumn> columns() {
		return unmodifiableList(new ArrayList<>(columns.values()));
	}

	Collection<String> referencedSchemaNames() {
		return foreignKeyColumns.stream()
						.filter(this::referencesExternalSchema)
						.map(MetaDataForeignKeyColumn::pkSchemaName)
						.collect(toSet());
	}

	Collection<MetaDataForeignKeyConstraint> foreignKeys() {
		return unmodifiableCollection(foreignKeys);
	}

	@Override
	public String toString() {
		return schema.name() + "." + tableName;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}

		MetaDataTable table = (MetaDataTable) object;

		return Objects.equals(schema, table.schema()) && Objects.equals(tableName, table.tableName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(schema, tableName);
	}

	void resolveForeignKeys(Map<String, MetaDataSchema> schemas) {
		for (MetaDataForeignKeyColumn foreignKeyColumn : foreignKeyColumns) {
			MetaDataTable referencedTable = referencedTable(foreignKeyColumn, schemas);
			MetaDataForeignKeyConstraint foreignKeyConstraint;
			if (foreignKeyColumn.keySeq() == 1) {//new key
				foreignKeyConstraint = new MetaDataForeignKeyConstraint(referencedTable);
				foreignKeys.add(foreignKeyConstraint);
			}
			else {//add to previous
				foreignKeyConstraint = foreignKeys.get(foreignKeys.size() - 1);
			}
			foreignKeyConstraint.addReference(columns.get(foreignKeyColumn.fkColumnName()),
							referencedTable.columns.get(foreignKeyColumn.pkColumnName()));
		}
	}

	private boolean referencesExternalSchema(MetaDataForeignKeyColumn foreignKeyColumn) {
		return !foreignKeyColumn.pkSchemaName().equals(schema.name());
	}

	private static MetaDataTable referencedTable(MetaDataForeignKeyColumn foreignKeyColumn, Map<String, MetaDataSchema> schemas) {
		MetaDataTable referencedTable = schemas.get(foreignKeyColumn.pkSchemaName()).tables().get(foreignKeyColumn.pkTableName());
		if (referencedTable == null) {
			throw new IllegalStateException("Referenced table not found: " + foreignKeyColumn.pkSchemaName() + "." + foreignKeyColumn.pkTableName());
		}

		return referencedTable;
	}

	static final class TablePacker implements ResultPacker<MetaDataTable> {

		private final MetaDataSchema schema;
		private final DatabaseMetaData metaData;
		private final String catalog;

		TablePacker(MetaDataSchema schema, DatabaseMetaData metaData, String catalog) {
			this.schema = schema;
			this.metaData = metaData;
			this.catalog = catalog;
		}

		@Override
		public MetaDataTable get(ResultSet resultSet) throws SQLException {
			String tableName = resultSet.getString("TABLE_NAME");
			String remarks = resultSet.getString("REMARKS");
			String tableType = resultSet.getString("TABLE_TYPE");
			List<MetaDataPrimaryKeyColumn> primaryKeyColumns = primaryKeyColumns(schema, metaData, catalog, tableName);
			List<MetaDataForeignKeyColumn> foreignKeyColumns = foreignKeyColumns(schema, metaData, catalog, tableName);
			List<MetaDataColumn> columns = columns(schema, metaData, catalog, tableName, primaryKeyColumns, foreignKeyColumns);

			return new MetaDataTable(schema, tableName, tableType, remarks, columns, foreignKeyColumns);
		}

		private static List<MetaDataPrimaryKeyColumn> primaryKeyColumns(MetaDataSchema schema, DatabaseMetaData metaData,
																																		String catalog, String tableName) throws SQLException {
			try (ResultSet resultSet = metaData.getPrimaryKeys(catalog, schema.name(), tableName)) {
				return new MetaDataPrimaryKeyColumn.PrimaryKeyColumnPacker().pack(resultSet);
			}
		}

		private static List<MetaDataForeignKeyColumn> foreignKeyColumns(MetaDataSchema schema, DatabaseMetaData metaData,
																																		String catalog, String tableName) throws SQLException {
			try (ResultSet resultSet = metaData.getImportedKeys(catalog, schema.name(), tableName)) {
				return new ForeignKeyColumnPacker().pack(resultSet);
			}
		}

		private static List<MetaDataColumn> columns(MetaDataSchema schema, DatabaseMetaData metaData, String catalog,
																								String tableName, List<MetaDataPrimaryKeyColumn> primaryKeyColumns,
																								List<MetaDataForeignKeyColumn> foreignKeyColumns) throws SQLException {
			try (ResultSet resultSet = metaData.getColumns(catalog, schema.name(), tableName, null)) {
				return new ColumnPacker(primaryKeyColumns, foreignKeyColumns).pack(resultSet);
			}
		}
	}
}

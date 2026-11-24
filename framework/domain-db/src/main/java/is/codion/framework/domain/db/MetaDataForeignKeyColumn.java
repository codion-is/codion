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

import java.sql.ResultSet;
import java.sql.SQLException;

final class MetaDataForeignKeyColumn {

	private final String pkSchemaName;
	private final String pkTableName;
	private final String pkColumnName;
	private final String fkTableName;
	private final String fkSchemaName;
	private final String fkColumnName;
	private final int keySeq;

	private MetaDataForeignKeyColumn(String pkSchemaName, String pkTableName, String pkColumnName,
																	 String fkTableName, String fkSchemaName, String fkColumnName,
																	 int keySeq) {
		this.pkSchemaName = pkSchemaName;
		this.pkTableName = pkTableName;
		this.pkColumnName = pkColumnName;
		this.fkTableName = fkTableName;
		this.fkSchemaName = fkSchemaName;
		this.fkColumnName = fkColumnName;
		this.keySeq = keySeq;
	}

	String pkSchemaName() {
		return pkSchemaName;
	}

	String pkTableName() {
		return pkTableName;
	}

	String pkColumnName() {
		return pkColumnName;
	}

	String fkTableName() {
		return fkTableName;
	}

	String fkSchemaName() {
		return fkSchemaName;
	}

	String fkColumnName() {
		return fkColumnName;
	}

	int keySeq() {
		return keySeq;
	}

	static final class ForeignKeyColumnPacker implements ResultPacker<MetaDataForeignKeyColumn> {

		private final MetaDataSchema schema;

		/**
		 * @param schema the schema of the table whose foreign keys are packed
		 */
		ForeignKeyColumnPacker(MetaDataSchema schema) {
			this.schema = schema;
		}

		@Override
		public MetaDataForeignKeyColumn get(ResultSet resultSet) throws SQLException {
			return new MetaDataForeignKeyColumn(schemaName(resultSet, "PKTABLE_SCHEM", "PKTABLE_CAT"),
							resultSet.getString("PKTABLE_NAME"),
							resultSet.getString("PKCOLUMN_NAME"),
							resultSet.getString("FKTABLE_NAME"),
							schemaName(resultSet, "FKTABLE_SCHEM", "FKTABLE_CAT"),
							resultSet.getString("FKCOLUMN_NAME"),
							resultSet.getInt("KEY_SEQ"));
		}

		// the catalog for a database reporting catalogs instead of schemas, MySQL and MariaDB, and the schema of the
		// table itself for a database reporting neither, SQLite, a foreign key there referencing a table in its own
		private String schemaName(ResultSet resultSet, String schemaColumn, String catalogColumn) throws SQLException {
			String schemaName = resultSet.getString(schemaColumn);
			if (schemaName == null) {
				schemaName = resultSet.getString(catalogColumn);
			}

			return schemaName == null ? schema.name() : schemaName;
		}
	}
}

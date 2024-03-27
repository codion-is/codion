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

public final class MetaDataForeignKeyColumn {

	private final String pkSchemaName;
	private final String pkTableName;
	private final String pkColumnName;
	private final String fkTableName;
	private final String fkSchemaName;
	private final String fkColumnName;
	private final int keySeq;

	MetaDataForeignKeyColumn(String pkSchemaName, String pkTableName, String pkColumnName,
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

	public String pkSchemaName() {
		return pkSchemaName;
	}

	public String pkTableName() {
		return pkTableName;
	}

	public String pkColumnName() {
		return pkColumnName;
	}

	public String fkTableName() {
		return fkTableName;
	}

	public String fkSchemaName() {
		return fkSchemaName;
	}

	public String fkColumnName() {
		return fkColumnName;
	}

	public int keySeq() {
		return keySeq;
	}

	static final class ForeignKeyColumnPacker implements ResultPacker<MetaDataForeignKeyColumn> {

		@Override
		public MetaDataForeignKeyColumn get(ResultSet resultSet) throws SQLException {
			String pktableSchem = resultSet.getString("PKTABLE_SCHEM");
			if (pktableSchem == null) {
				pktableSchem = resultSet.getString("PKTABLE_CAT");
			}
			String fktableSchem = resultSet.getString("FKTABLE_SCHEM");
			if (fktableSchem == null) {
				fktableSchem = resultSet.getString("FKTABLE_CAT");
			}
			return new MetaDataForeignKeyColumn(pktableSchem,
							resultSet.getString("PKTABLE_NAME"),
							resultSet.getString("PKCOLUMN_NAME"),
							resultSet.getString("FKTABLE_NAME"),
							fktableSchem,
							resultSet.getString("FKCOLUMN_NAME"),
							resultSet.getInt("KEY_SEQ"));
		}
	}
}

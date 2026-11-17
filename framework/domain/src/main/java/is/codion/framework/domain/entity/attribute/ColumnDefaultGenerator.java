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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Column.Generator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

final class ColumnDefaultGenerator<T> implements Generator<T> {

	ColumnDefaultGenerator() {}

	// never written, the column's default always making the value
	@Override
	public boolean inserted() {
		return false;
	}

	@Override
	public boolean generatedKeys() {
		return true;
	}

	// a key read back from the generated keys, the insert unable to select the row without it, a value which is not
	// a key coming back with the row selected after the insert
	@Override
	public void afterInsert(Entity entity, Column<T> column, Database database, Statement statement) throws SQLException {
		ColumnDefinition<T> columnDefinition = entity.definition().columns().definition(column);
		if (columnDefinition.primaryKey()) {
			try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
				if (!generatedKeys.next()) {
					throw new SQLException("Column default generator returned no generated keys", DatabaseException.SQL_STATE_NO_DATA);
				}
				entity.remove(column);
				entity.set(column, columnDefinition.get(generatedKeys, database));
			}
		}
	}
}

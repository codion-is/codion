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
 * Copyright (c) 2021 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Column.Generator.Identity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

final class IdentityGenerator<T> implements Identity<T> {

	IdentityGenerator() {}

	@Override
	public boolean inserted() {
		return false;
	}

	@Override
	public boolean generatedKeys() {
		return true;
	}

	@Override
	public void afterInsert(Entity entity, Column<T> column, Database database, Statement statement) throws SQLException {
		try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
			if (!generatedKeys.next()) {
				throw new SQLException("Identity key generator returned no generated keys", DatabaseException.SQL_STATE_NO_DATA);
			}
			ColumnDefinition<T> columnDefinition = entity.definition().columns().definition(column);
			entity.remove(columnDefinition.attribute());
			// must fetch value by column name, since some databases (PostgreSQL for example), return all columns, not just generated ones
			entity.set(columnDefinition.attribute(), columnDefinition.get(generatedKeys));
		}
	}
}

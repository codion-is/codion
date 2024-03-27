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
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

final class IdentityKeyGenerator implements KeyGenerator {

	static final KeyGenerator INSTANCE = new IdentityKeyGenerator();

	private IdentityKeyGenerator() {}

	@Override
	public boolean inserted() {
		return false;
	}

	@Override
	public boolean returnGeneratedKeys() {
		return true;
	}

	@Override
	public void afterInsert(Entity entity, DatabaseConnection connection, Statement insertStatement) throws SQLException {
		try (ResultSet generatedKeys = insertStatement.getGeneratedKeys()) {
			if (!generatedKeys.next()) {
				throw new SQLException("Identity key generator returned no generated keys", DatabaseConnection.SQL_STATE_NO_DATA);
			}
			ColumnDefinition<Object> column = (ColumnDefinition<Object>) entity.definition().primaryKey().definitions().get(0);
			// must fetch value by column name, since some databases (PostgreSQL for example), return all columns, not just generated ones
			entity.put(column.attribute(), column.prepareValue(generatedKeys.getObject(column.name())));
		}
	}
}

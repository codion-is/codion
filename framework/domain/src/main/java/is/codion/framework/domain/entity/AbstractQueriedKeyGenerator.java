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
package is.codion.framework.domain.entity;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.database.Database;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static is.codion.common.db.connection.DatabaseConnection.SQL_STATE_NO_DATA;

abstract class AbstractQueriedKeyGenerator implements KeyGenerator {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractQueriedKeyGenerator.class);

	protected final void selectAndPopulate(Entity entity, DatabaseConnection databaseConnection) throws SQLException {
		Connection connection = databaseConnection.getConnection();
		String query = query(databaseConnection.database());
		if (query == null) {
			throw new IllegalStateException("Queried key generator returned no query");
		}
		try (PreparedStatement statement = connection.prepareStatement(query);
				 ResultSet resultSet = statement.executeQuery()) {
			if (!resultSet.next()) {
				throw new SQLException("No rows returned when querying for a key value", SQL_STATE_NO_DATA);
			}
			ColumnDefinition<Object> column = (ColumnDefinition<Object>) entity.definition().primaryKey().definitions().get(0);
			entity.remove(column.attribute());
			entity.set(column.attribute(), column.get(resultSet, 1));
		}
		catch (SQLException e) {
			LOG.error("Exception during selectAndPopulate {}", e.getMessage(), e);
			throw e;
		}
		finally {
			databaseConnection.database().queryCounter().select();
		}
	}

	protected abstract String query(Database database);
}

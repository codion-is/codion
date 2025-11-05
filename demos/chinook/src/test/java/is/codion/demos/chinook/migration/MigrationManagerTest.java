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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.migration;

import is.codion.common.db.database.Database;
import is.codion.common.utilities.user.User;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MigrationManagerTest {

	@Test
	void testMigration() throws Exception {
		// This test verifies that migrations can be loaded and would execute
		// In a real test environment, you'd use an in-memory database

		// Create a mock database that would normally be injected
		Database database = Database.instance();
		try (Connection connection = database.createConnection(User.parse("scott:tiger"))) {
			// Create the schema
			try (Statement stmt = connection.createStatement()) {
				stmt.execute("CREATE SCHEMA IF NOT EXISTS CHINOOK");
			}

			// Run migrations
			MigrationManager.migrate(database);

			// Verify migration table was created
			try (Statement statement = connection.createStatement();
					 ResultSet resultSet = statement.executeQuery(
									 "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES " +
													 "WHERE TABLE_SCHEMA = 'CHINOOK' AND TABLE_NAME = 'SCHEMA_MIGRATION'")) {
				assertTrue(resultSet.next());
				assertTrue(resultSet.getInt(1) > 0, "Migration table should exist");
			}
		}
	}
}
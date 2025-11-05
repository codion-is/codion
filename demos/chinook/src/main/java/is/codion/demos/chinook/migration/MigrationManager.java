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
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.utilities.property.PropertyValue;
import is.codion.common.utilities.user.User;
import is.codion.demos.chinook.migration.MigrationDomain.Migration;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static is.codion.common.utilities.Configuration.booleanValue;

/**
 * A simple database migration manager for the Chinook demo.
 * This demonstrates how Codion's Domain.configure(Database) method
 * can be used for database migrations without external dependencies.
 */
public final class MigrationManager {

	// In JPMS, we need to list resources explicitly
	private static final String[] MIGRATION_FILES = new String[] {
					"V1__Initial_schema.sql",
					"V2__Add_track_play_count.sql",
					"V3__Add_preferences.sql"
	};

	public static final PropertyValue<Boolean> MIGRATION_ENABLED =
					booleanValue("chinook.migration.enabled", true);

	private static final MigrationDomain DOMAIN = new MigrationDomain();

	private MigrationManager() {}

	public static void migrate(Database database) throws DatabaseException {
		if (!MIGRATION_ENABLED.get()) {
			System.out.println("[MigrationManager] Database migration disabled");
			return;
		}

		try (Connection connection = database.createConnection(User.parse("scott:tiger"))) {
			List<Migration> pendingMigrations = DOMAIN.pendingMigrations(connection, MIGRATION_FILES);
			if (pendingMigrations.isEmpty()) {
				System.out.println("[MigrationManager] Database is up to date");
				return;
			}

			System.out.println("[MigrationManager] Found " + pendingMigrations.size() + " pending migration(s)");
			for (Migration migration : pendingMigrations) {
				DOMAIN.applyMigration(connection, database, migration);
			}

			connection.commit();
			System.out.println("[MigrationManager] All migrations completed successfully");
		}
		catch (SQLException e) {
			throw new DatabaseException(e, "Migration failed");
		}
		catch (IOException e) {
			throw new DatabaseException("Migration failed: " + e.getMessage());
		}
	}
}
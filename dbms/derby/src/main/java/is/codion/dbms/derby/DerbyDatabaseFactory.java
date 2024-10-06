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
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.dbms.derby;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;

import java.sql.DriverManager;
import java.sql.SQLException;

import static java.util.Objects.requireNonNull;

/**
 * Provides derby database implementations
 */
public final class DerbyDatabaseFactory implements DatabaseFactory {

	private static final String DRIVER_PACKAGE = "org.apache.derby.jdbc";
	private static final String SHUTDOWN_ERROR_CODE = "08006";

	@Override
	public boolean driverCompatible(String driverClassName) {
		return requireNonNull(driverClassName, "driverClassName").startsWith(DRIVER_PACKAGE);
	}

	@Override
	public Database create(String url) {
		return new DerbyDatabase(url);
	}

	/**
	 * Shuts down the given database instance, assuming it is embedded
	 * @param database the database to shutdown
	 */
	public static void shutdown(Database database) {
		requireNonNull(database);
		try {
			DriverManager.getConnection(database.url() + ";shutdown=true").close();
		}
		catch (SQLException e) {
			if (!e.getSQLState().equals(SHUTDOWN_ERROR_CODE)) {//08006 is expected on Derby shutdown
				System.err.println("Embedded Derby database did not successfully shut down: " + e.getMessage());
			}
		}
	}
}

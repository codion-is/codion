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
package is.codion.common.db.database;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import static java.util.Objects.requireNonNull;
import static java.util.stream.StreamSupport.stream;

/**
 * Provides {@link Database} implementations
 * @see #instance()
 * @see #instance(String)
 * @see #create(String)
 */
public interface DatabaseFactory {

	/**
	 * @param driverClassName the driver class name
	 * @return true if this database factory is compatible with the given driver
	 */
	boolean driverCompatible(String driverClassName);

	/**
	 * @param url the jdbc url
	 * @return a new {@link Database} implementation based on the given jdbc url.
	 */
	Database create(String url);

	/**
	 * @return a {@link DatabaseFactory} implementation for {@link Database#DATABASE_URL}
	 * @throws IllegalStateException in case {@link Database#DATABASE_URL} ('codion.db.url') is not specified.
	 * @throws SQLException in case loading of the database driver failed
	 * @throws IllegalArgumentException in case no implementation exists for the configured jdbc url
	 */
	static DatabaseFactory instance() throws SQLException {
		return instance(Database.DATABASE_URL.getOrThrow("codion.db.url must be specified before discovering DatabaseFactories"));
	}

	/**
	 * @param url the jdbc url
	 * @return a {@link DatabaseFactory} implementation for the given jdbc url
	 * @throws SQLException in case loading of database driver failed
	 * @throws IllegalArgumentException in case no implementation exists for the given jdbc url
	 */
	static DatabaseFactory instance(String url) throws SQLException {
		String driver = driverClassName(url);
		try {
			return stream(ServiceLoader.load(DatabaseFactory.class).spliterator(), false)
							.filter(factory -> factory.driverCompatible(driver))
							.findFirst()
							.orElseThrow(() -> new IllegalArgumentException("No DatabaseFactory implementation available for driver: " + driver));
		}
		catch (ServiceConfigurationError e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			}
			throw new RuntimeException(cause);
		}
	}

	/**
	 * @param url the jdbc url
	 * @return the database driver class name according to jdbc url
	 * @throws SQLException in case loading of database driver failed
	 */
	static String driverClassName(String url) throws SQLException {
		return DriverManager.getDriver(requireNonNull(url)).getClass().getName();
	}
}

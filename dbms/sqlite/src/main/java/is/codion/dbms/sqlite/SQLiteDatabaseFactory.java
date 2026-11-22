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
 * Copyright (c) 2019 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.dbms.sqlite;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.utilities.property.PropertyValue;

import static is.codion.common.utilities.Configuration.stringValue;
import static java.util.Objects.requireNonNull;

/**
 * Provides sqlite database implementations
 */
public final class SQLiteDatabaseFactory implements DatabaseFactory {

	/**
	 * The schemas attached to every connection, a comma separated list of names, each a database file of its own beside
	 * the main one, named after it and the schema, {@code fieldbook-sync.db} for the schema {@code sync} of
	 * {@code fieldbook.db}, created where it does not exist. Tables are then named by schema, {@code sync.row_state} for
	 * example, as on a database with schemas.
	 * <p>
	 * A schema is attached with the main database's journal mode and synchronous setting. A transaction writing to
	 * several files is atomic across them in a rollback journal mode only, not in WAL mode.
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: null, no schemas attached
	 * </ul>
	 */
	public static final PropertyValue<String> SCHEMAS = stringValue("is.codion.dbms.sqlite.SQLiteDatabaseFactory.schemas");

	private static final String DRIVER_PACKAGE = "org.sqlite";

	@Override
	public boolean supports(String driverClassName) {
		return requireNonNull(driverClassName).startsWith(DRIVER_PACKAGE);
	}

	@Override
	public Database create(String url) {
		return new SQLiteDatabase(url, SCHEMAS.get());
	}
}

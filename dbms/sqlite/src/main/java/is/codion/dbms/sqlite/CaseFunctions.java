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
package is.codion.dbms.sqlite;

import org.sqlite.Function;
import org.sqlite.SQLiteConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

/**
 * {@code upper()} and {@code lower()} as Java has them, registered on a connection in place of SQLite's own, which
 * change the letters of ASCII alone, so that a case-insensitive condition, {@code upper()} on both of its sides, finds
 * {@code Þingvellir} by {@code þing%}, as on other databases. Apart from {@link SQLiteDatabase}, which then needs the
 * driver's classes only where it configures a connection.
 */
final class CaseFunctions {

	private static final String UPPER = "upper";
	private static final String LOWER = "lower";

	private CaseFunctions() {}

	/**
	 * Registers the functions on the given connection, the driver's own or one wrapping it, any other left as it is.
	 * @param connection the connection
	 * @throws SQLException in case a function can not be registered
	 */
	static void register(Connection connection) throws SQLException {
		if (connection.isWrapperFor(SQLiteConnection.class)) {
			SQLiteConnection sqliteConnection = connection.unwrap(SQLiteConnection.class);
			// deterministic, as SQLite's own are, so usable where those are
			Function.create(sqliteConnection, UPPER, new Upper(), 1, Function.FLAG_DETERMINISTIC);
			Function.create(sqliteConnection, LOWER, new Lower(), 1, Function.FLAG_DETERMINISTIC);
		}
	}

	// The argument as text, as SQLite's own take it, null for null, the case by the root locale, whatever the default
	private static final class Upper extends Function {

		@Override
		protected void xFunc() throws SQLException {
			String text = value_text(0);
			if (text == null) {
				result();
			}
			else {
				result(text.toUpperCase(Locale.ROOT));
			}
		}
	}

	private static final class Lower extends Function {

		@Override
		protected void xFunc() throws SQLException {
			String text = value_text(0);
			if (text == null) {
				result();
			}
			else {
				result(text.toLowerCase(Locale.ROOT));
			}
		}
	}
}

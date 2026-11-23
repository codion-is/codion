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
package is.codion.dbms.sqlite.modulepath;

import is.codion.common.db.database.Database;
import is.codion.dbms.sqlite.SQLiteDatabaseFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Run by {@code SQLiteModulePathTest} in a JVM of its own, the dialect on the module path: connects to a database in
 * memory and prints where the dialect and the driver were loaded from, and what {@code upper()} and {@code LIKE} make
 * of a letter beyond ASCII and one within it, or that the connection was refused, naming the driver's module. In a package of its own, the dialect's being its module's.
 */
public final class ModulePathConnection {

	private ModulePathConnection() {}

	public static void main(String[] arguments) throws Exception {
		System.out.println("dialect: " + (SQLiteDatabaseFactory.class.getModule().isNamed() ? SQLiteDatabaseFactory.class.getModule().getName() : "unnamed"));
		Module driver = Class.forName("org.sqlite.JDBC").getModule();
		System.out.println("driver: " + (driver.isNamed() ? driver.getName() : "unnamed"));
		Database database = new SQLiteDatabaseFactory().create("jdbc:sqlite::memory:");
		for (int i = 0; i < 2; i++) {
			try (Connection connection = database.createConnection(); Statement statement = connection.createStatement();
					 ResultSet resultSet = statement.executeQuery("select upper('þórsmörk'), 'Heath' like 'h%'")) {
				resultSet.next();
				System.out.println("upper: " + resultSet.getString(1));
				System.out.println("like: " + resultSet.getString(2));
			}
			catch (IllegalStateException e) {
				System.out.println("refused: " + e.getMessage().contains("org.xerial.sqlitejdbc"));
			}
		}
	}
}

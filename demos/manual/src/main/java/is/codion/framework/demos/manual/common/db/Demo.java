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
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.manual.common.db;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.dbms.h2.H2DatabaseFactory;

import java.sql.SQLException;

public final class Demo {

  private void databaseFromSystemProperty() {
    // tag::systemProperty[]
    System.setProperty("codion.db.url", "jdbc:h2:mem:h2db");

    Database database = Database.instance();
    // end::systemProperty[]
  }

  private void databaseFromConfiguration() {
    // tag::configuration[]
    Database.DATABASE_URL.set("jdbc:h2:mem:h2db");

    Database database = Database.instance();
    // end::configuration[]
  }

  private void databaseFromUrl() throws SQLException {
    // tag::url[]
    String url = "jdbc:h2:mem:h2db";

    DatabaseFactory databaseFactory = DatabaseFactory.instance(url);

    Database database = databaseFactory.createDatabase(url);
    // end::url[]
  }

  private void databaseFactory() {
    // tag::factory[]
    String url = "jdbc:h2:mem:h2db";

    H2DatabaseFactory databaseFactory = new H2DatabaseFactory();

    Database database = databaseFactory.createDatabase(url);
    // end::factory[]
  }

  private void connection() throws DatabaseException {
    // tag::connection[]
    Database.DATABASE_URL.set("jdbc:h2:mem:h2db");

    Database database = Database.instance();

    User user = User.parse("scott:tiger");

    java.sql.Connection connection = database.createConnection(user);
    // end::connection[]
  }

  private void databaseConnection() throws DatabaseException {
    // tag::databaseConnection[]
    Database.DATABASE_URL.set("jdbc:h2:mem:h2db");

    Database database = Database.instance();

    User user = User.parse("scott:tiger");

    DatabaseConnection databaseConnection =
            DatabaseConnection.databaseConnection(database, user);

    java.sql.Connection connection = databaseConnection.getConnection();
    // end::databaseConnection[]
  }
}

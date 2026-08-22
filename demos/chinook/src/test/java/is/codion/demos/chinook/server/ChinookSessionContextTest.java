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
package is.codion.demos.chinook.server;

import is.codion.common.db.database.ClientInfo;
import is.codion.common.db.database.Database;
import is.codion.common.utilities.user.User;
import is.codion.demos.chinook.domain.api.Chinook;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class ChinookSessionContextTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final ClientInfo CLIENT_INFO = new ClientInfo("peter", Chinook.DOMAIN.name(), "localhost");

	@Test
	void prepareAndRelease() throws Exception {
		ChinookSessionContext sessionContext = new ChinookSessionContext();
		//the connection the server hands the context is one authenticated as the shared database user,
		//which is the whole reason the real one has to be told to the database separately
		try (Connection connection = Database.instance().createConnection(UNIT_TEST_USER)) {
			assertNull(chinookUser(connection));

			sessionContext.prepare(CLIENT_INFO, connection);
			assertEquals("peter", chinookUser(connection));

			sessionContext.release(CLIENT_INFO, connection);
			assertNull(chinookUser(connection));
		}
	}

	private static String chinookUser(Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement();
				 ResultSet resultSet = statement.executeQuery("SELECT @chinook_user")) {
			resultSet.next();

			return resultSet.getString(1);
		}
	}
}

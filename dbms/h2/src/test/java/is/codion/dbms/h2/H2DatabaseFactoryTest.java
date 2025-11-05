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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.dbms.h2;

import is.codion.common.utilities.user.User;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

public final class H2DatabaseFactoryTest {

	@Test
	void scriptRunner() throws SQLException {
		H2Database database = new H2Database("jdbc:h2:mem:test");
		User user = User.user("sa");

		H2DatabaseFactory.scriptRunner(database.url())
						.user(user)
						.charset(StandardCharsets.UTF_8)
						.run("src" + File.separator + "test" + File.separator + "resources" + File.separator + "create_schema.sql");

		Connection connection = database.createConnection(user);

		connection.createStatement().executeQuery("select id from test.test_table").close();
	}
}

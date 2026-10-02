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
package is.codion.demos.chinook.javadoc;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.utilities.user.User;
import is.codion.demos.chinook.domain.ChinookImpl;
import is.codion.demos.chinook.domain.api.Chinook.Customer;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.condition.Condition;

import java.sql.SQLException;
import java.util.List;

import static is.codion.framework.db.local.LocalEntityConnection.localEntityConnection;

/**
 * The {@link LocalEntityConnection} javadoc snippets, each the region of the same name.
 */
final class LocalEntityConnectionSnippets {

	void usage() throws SQLException {
		Domain domain = new ChinookImpl(); // @start region=usage
		String url = "jdbc:h2:file:/path/to/database";
		Database database = DatabaseFactory.instance(url).create(url);
		User user = User.parse("scott:tiger");

		try (EntityConnection connection = localEntityConnection(database, domain, user)) {
			List<Entity> customers = connection.select(Condition.all(Customer.TYPE));
		} // @end
	}
}

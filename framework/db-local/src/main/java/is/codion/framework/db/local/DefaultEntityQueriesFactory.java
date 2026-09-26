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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.local;

import is.codion.common.db.database.Database;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityQueries;

import static java.util.Objects.requireNonNull;

/**
 * The default {@link EntityQueries.Factory} implementation, providing {@link DefaultEntityQueries} instances.
 */
public final class DefaultEntityQueriesFactory implements EntityQueries.Factory {

	/**
	 * @param connection the connection, a {@link LocalEntityConnection} providing its own database,
	 * any other the one returned by {@link Database#instance()}
	 * @return a new {@link EntityQueries} instance
	 */
	@Override
	public EntityQueries create(EntityConnection connection) {
		Database database = requireNonNull(connection) instanceof LocalEntityConnection ?
						((LocalEntityConnection) connection).database() :
						Database.instance();

		return new DefaultEntityQueries(database, connection.entities());
	}
}

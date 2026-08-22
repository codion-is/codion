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
import is.codion.common.db.database.SessionContext;
import is.codion.demos.chinook.domain.api.Chinook;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * <p>Tells the database who is actually doing the work.
 * <p>{@link ChinookAuthenticator} authenticates against the CHINOOK.USERS table and then swaps in a single
 * shared database user, so everything every client does arrives at the database as that one user. Anything
 * the database would like to do with the real identity - an audit trigger stamping who changed a price, a
 * view showing a sales representative only their own customers - has nothing to go on.
 * <p>This puts it back, as the {@code @chinook_user} session variable, which SQL can read.
 */
public final class ChinookSessionContext implements SessionContext {

	private static final String SET_USER = "SET @chinook_user = ?";
	private static final String CLEAR_USER = "SET @chinook_user = NULL";

	/**
	 * Applied to Chinook clients only, the users it names being the ones in CHINOOK.USERS.
	 */
	@Override
	public Optional<String> clientType() {
		return Optional.of(Chinook.DOMAIN.name());
	}

	@Override
	public void prepare(ClientInfo clientInfo, Connection connection) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(SET_USER)) {
			statement.setString(1, clientInfo.user());
			statement.execute();
		}
	}

	/**
	 * Cleared rather than left standing, the connection going back to a pool it shares with
	 * every other client.
	 */
	@Override
	public void release(ClientInfo clientInfo, Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.execute(CLEAR_USER);
		}
	}
}

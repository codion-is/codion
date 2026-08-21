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
package is.codion.framework.db.local;

import is.codion.common.db.exception.DatabaseException;

import org.jspecify.annotations.Nullable;

import java.sql.Connection;

/**
 * <p>The server facing side of a {@link LocalEntityConnection}: one whose underlying JDBC connection is
 * attached and detached by its owner, the way the server runs a client's connection on one borrowed from
 * a connection pool for the duration of an invocation, or replaces one which has gone bad, in place.
 * <p>Connections created via {@link LocalEntityConnection#localEntityConnection} implement this,
 * reached by casting:
 * {@snippet :
 * ((ConnectionHolder) entityConnection).attach(connectionPool.connection(user));
 *}
 * A self-managing connection, see {@link LocalEntityConnection#builder()}, does not implement this,
 * its underlying connection being its own to manage.
 */
public interface ConnectionHolder {

	/**
	 * <p>Attaches the given JDBC connection, which is used 'as is': no validation or transaction checking
	 * is performed and auto-commit is assumed to be disabled. The domain does get to configure it, see
	 * {@link is.codion.framework.domain.Domain#configure(Connection)}.
	 * <p>Note that this does not reset the transaction state; the caller is responsible for ensuring no
	 * transaction is considered open when swapping the underlying connection.
	 * @param connection the connection to attach
	 */
	void attach(Connection connection);

	/**
	 * <p>Detaches and returns the underlying JDBC connection, leaving this connection without one -
	 * methods requiring it throw {@link DatabaseException} until one is attached.
	 * @return the detached connection, or null in case none was attached
	 */
	@Nullable Connection detach();
}

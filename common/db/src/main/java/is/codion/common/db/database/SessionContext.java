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
package is.codion.common.db.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * <p>Applies session state to the connection a client is about to use, and removes it again afterwards.
 * <p>The case this exists for is a server whose clients do not log in to the database, an authenticator
 * having swapped in a shared database user: the database then sees one user doing everyone's work, and
 * anything depending on who the work is really for - an audit trigger, a row level security policy - has
 * nothing to go on. Setting the relevant session state on the connection is what gives it something.
 * <p>Discovered via {@link ServiceLoader}, so an implementation is registered in
 * {@code META-INF/services/is.codion.common.db.database.SessionContext}, and applied by the Codion server.
 * All registered instances are applied, shared ones first, and removed in reverse.
 * {@snippet :
 * public final class AuditContext implements SessionContext {
 *
 *   @Override
 *   public void prepare(ClientInfo clientInfo, Connection connection) throws SQLException {
 *     try (CallableStatement statement = connection.prepareCall("{call set_audit_user(?)}")) {
 *       statement.setString(1, clientInfo.user());
 *       statement.execute();
 *     }
 *   }
 *
 *   @Override
 *   public void release(ClientInfo clientInfo, Connection connection) throws SQLException {
 *     try (CallableStatement statement = connection.prepareCall("{call clear_audit_user()}")) {
 *       statement.execute();
 *     }
 *   }
 * }
 *}
 * <p>The session is the database's, not Codion's: with a connection pool the connection is a single
 * database session handed to one client after another, and this is the context swapped in and out around
 * each use of it. Hence the name, which the databases share - {@code sp_set_session_context} on SQL Server,
 * an application context read through {@code SYS_CONTEXT} on Oracle, a session level {@code SET} read
 * through {@code current_setting} on PostgreSQL.
 * <p>This interface lives here, rather than with the server which applies it, so that implementing one
 * costs an application nothing it does not already have: a client carrying a domain model already has this
 * module, whereas the server module brings the whole server with it.
 * <p>Note the cost of implementing it: with a connection pool this runs on every connection check out, that
 * being the only point at which the connection is known to belong to this client, so whatever it does is
 * paid for by every database call the client makes. State which is the same for every client belongs on the
 * pool, not here.
 * <p>For the identity of the client alone - who, which application, which host - see
 * {@link Database#clientInfo(Connection, ClientInfo)}, which the server applies on its own, more cheaply,
 * and before any of these. Reach for this interface for what that cannot express.
 * @see Database#clientInfo(Connection, ClientInfo)
 */
public interface SessionContext {

	/**
	 * Returns the client type for which to use this {@link SessionContext}.
	 * If none is specified, this {@link SessionContext} is shared between all client types.
	 * <p>Unlike an authenticator, which selects a single one per client type, any number of contexts may
	 * name the same client type and all of them are applied. Shared contexts are applied first, so that a
	 * client type specific one sits on top of the general setup and, the removal running in reverse, comes
	 * off again before it.
	 * @return the String identifying the client type for which to use this context or an empty optional in case this context should be shared
	 * @see ClientInfo#clientType()
	 */
	default Optional<String> clientType() {
		return Optional.empty();
	}

	/**
	 * <p>Called once the connection has been made ready for the given client and before it is used: on every
	 * check out where the connection comes from a pool, once per connection otherwise.
	 * <p>Throwing aborts the operation the client requested, and any contexts already applied are removed
	 * before the exception propagates. That is deliberate, and unlike
	 * {@link Database#clientInfo(Connection, ClientInfo)}, which is a label and may be skipped: a connection
	 * whose session state could not be applied is not the connection the application asked for, and running
	 * a query on it may return rows the user should not see.
	 * @param clientInfo identifies the client the connection is being prepared for
	 * @param connection the connection
	 * @throws SQLException in case the state could not be applied
	 */
	void prepare(ClientInfo clientInfo, Connection connection) throws SQLException;

	/**
	 * <p>Called before the connection is released, in reverse of the order the contexts were applied in:
	 * on every return to the pool where the connection came from one, when the client disconnects otherwise.
	 * <p>Always called for a context whose {@link #prepare(ClientInfo, Connection)} was called, so that an
	 * implementation cannot leave state behind by forgetting a path. Throwing does not abort anything - the
	 * client's operation has already finished - but the connection is then considered to be in an unknown
	 * state and is discarded rather than handed to the next client.
	 * @param clientInfo identifies the client the connection was prepared for
	 * @param connection the connection
	 * @throws SQLException in case the state could not be removed
	 */
	void release(ClientInfo clientInfo, Connection connection) throws SQLException;
}

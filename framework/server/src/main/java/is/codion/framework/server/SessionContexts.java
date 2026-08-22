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
package is.codion.framework.server;

import is.codion.common.db.database.ClientInfo;
import is.codion.common.db.database.SessionContext;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.utilities.exceptions.Exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

/**
 * Applies and removes the registered {@link SessionContext} instances, keeping the pairing correct so that
 * an implementation can not be left half applied.
 */
final class SessionContexts {

	private static final Logger LOG = LoggerFactory.getLogger(SessionContexts.class);

	/**
	 * Loaded once, the lookup being far more expensive than the connection check out it would
	 * otherwise sit in. {@link EntityServer} touches this on startup, so that a context which can not
	 * be loaded fails the server start, with the cause in the log, rather than every connect after it.
	 */
	private static final List<SessionContext> CONTEXTS = load();

	private final ClientInfo clientInfo;
	private final List<SessionContext> contexts;

	/**
	 * The number of contexts applied to the connection currently in hand, so that a failure part way
	 * through unwinds exactly what it applied, and no more.
	 */
	private int applied = 0;
	/**
	 * Set when the unwinding of a failed {@link #prepare(Connection)} did not remove everything it
	 * had applied, so that the release which follows reports the connection unclean, as it would
	 * have had the failure happened on the way out.
	 */
	private boolean unclean = false;

	SessionContexts(ClientInfo clientInfo, List<SessionContext> contexts) {
		this.clientInfo = clientInfo;
		this.contexts = contexts;
	}

	boolean empty() {
		return contexts.isEmpty();
	}

	/**
	 * Applies each context in turn. Should one throw, those already applied are removed before the failure
	 * propagates, leaving the connection as it was found - the client's operation fails, which is the point,
	 * a connection whose session state is unknown being worse than no connection at all.
	 * @param connection the connection
	 */
	void prepare(Connection connection) {
		for (SessionContext context : contexts) {
			try {
				context.prepare(clientInfo, connection);
				applied++;
			}
			catch (Exception e) {
				LOG.error("Exception while applying session context {} for {}", context.getClass().getName(), clientInfo, e);
				unclean = !release(connection);

				throw e instanceof SQLException ? new DatabaseException((SQLException) e) : Exceptions.runtime(e);
			}
		}
	}

	/**
	 * Removes the applied contexts in reverse, continuing past a failure so that one bad context does not
	 * strand the rest.
	 * @param connection the connection
	 * @return true if every context was removed cleanly, false if the connection should be discarded
	 */
	boolean release(Connection connection) {
		boolean clean = !unclean;
		unclean = false;
		while (applied > 0) {
			SessionContext context = contexts.get(--applied);
			try {
				context.release(clientInfo, connection);
			}
			catch (Exception e) {
				clean = false;
				LOG.error("Exception while removing session context {} for {}, the connection is no longer reusable",
								context.getClass().getName(), clientInfo, e);
			}
		}

		return clean;
	}

	/**
	 * Forgets what was applied, without removing anything: for a connection which has gone bad, whose
	 * session state died with it, so that the count does not carry over to its replacement.
	 */
	void reset() {
		applied = 0;
		unclean = false;
	}

	/**
	 * @return all registered contexts
	 */
	static List<SessionContext> contexts() {
		return CONTEXTS;
	}

	/**
	 * @param clientType the client type
	 * @return the contexts applying to the given client type, the shared ones first, so that one
	 * specific to the client type is applied on top of the general setup and, the removal running
	 * in reverse, comes off again before it
	 */
	static List<SessionContext> contexts(String clientType) {
		if (CONTEXTS.isEmpty()) {
			return emptyList();
		}

		return Stream.concat(
										CONTEXTS.stream()
														.filter(context -> !context.clientType().isPresent()),
										CONTEXTS.stream()
														.filter(context -> context.clientType().filter(clientType::equals).isPresent()))
						.collect(toList());
	}

	private static List<SessionContext> load() {
		try {
			List<SessionContext> contexts =
							stream(ServiceLoader.load(SessionContext.class).spliterator(), false).collect(toList());
			contexts.forEach(context -> LOG.info("Server loading session context '{}', clientType '{}'",
							context.getClass().getName(), context.clientType().orElse("<shared>")));

			return unmodifiableList(contexts);
		}
		catch (ServiceConfigurationError e) {
			throw Exceptions.runtime(e, ServiceConfigurationError.class);
		}
	}
}

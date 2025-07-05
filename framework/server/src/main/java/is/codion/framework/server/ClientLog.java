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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.server;

import is.codion.common.logging.MethodTrace;

import java.util.List;
import java.util.UUID;

/**
 * Encapsulates a collection of server access log entries.
 */
public interface ClientLog {

	/**
	 * @return the method trace list
	 */
	List<MethodTrace> entries();

	/**
	 * @return the UUID identifying this logs client
	 */
	UUID clientId();

	/**
	 * Instantiates a new {@link ClientLog} instance.
	 * @param clientId the id of the client this log represents
	 * @param entries the log entries
	 * @return a new {@link ClientLog} instance
	 */
	static ClientLog clientLog(UUID clientId, List<MethodTrace> entries) {
		return new DefaultClientLog(clientId, entries);
	}
}

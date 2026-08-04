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
package is.codion.framework.db.rmi;

import is.codion.framework.db.AbstractEntityConnection;
import is.codion.framework.db.EntityConnection;

/**
 * An {@link EntityConnection} based on RMI, adapting a {@link ServerEntityConnection} running on the server.
 * @see #builder()
 */
public interface RemoteEntityConnection extends EntityConnection {

	/**
	 * <p>Instantiates a builder for a self-managing {@link RemoteEntityConnection}, one which connects on
	 * demand and reconnects when the underlying connection has gone bad, serving for the lifetime of a client.
	 * @return a new builder
	 * @see AbstractEntityConnection
	 */
	static Builder builder() {
		return new DefaultRemoteEntityConnectionBuilder();
	}

	/**
	 * Builds a self-managing {@link RemoteEntityConnection}.
	 * @see RemoteEntityConnection#builder()
	 */
	interface Builder extends EntityConnection.Builder<RemoteEntityConnection, Builder> {

		/**
		 * @param hostname the server hostname
		 * @return this builder instance
		 */
		Builder hostname(String hostname);

		/**
		 * @param port the server port
		 * @return this builder instance
		 */
		Builder port(int port);

		/**
		 * @param registryPort the rmi registry port
		 * @return this builder instance
		 */
		Builder registryPort(int registryPort);

		/**
		 * @param serverNamePrefix the name prefix to use when looking up the server
		 * @return this builder instance
		 */
		Builder namePrefix(String serverNamePrefix);
	}
}

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
 * Copyright (c) 2022 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.rmi;

import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.framework.db.AbstractEntityConnection.AbstractBuilder;
import is.codion.framework.db.EntityConnection;

import static java.util.Objects.requireNonNull;

/**
 * Builds a self-managing {@link RemoteEntityConnection}.
 * @see RemoteEntityConnection#builder()
 */
public final class DefaultRemoteEntityConnectionBuilder
				extends AbstractBuilder<RemoteEntityConnection, RemoteEntityConnection.Builder>
				implements RemoteEntityConnection.Builder {

	String hostname = Clients.SERVER_HOSTNAME.get();
	int port = ServerConfiguration.SERVER_PORT.getOrThrow();
	int registryPort = ServerConfiguration.REGISTRY_PORT.getOrThrow();
	String namePrefix = ServerConfiguration.SERVER_NAME_PREFIX.get();

	/**
	 * Instantiates a new {@link DefaultRemoteEntityConnectionBuilder}
	 */
	public DefaultRemoteEntityConnectionBuilder() {
		super(EntityConnection.CONNECTION_TYPE_REMOTE);
	}

	@Override
	public RemoteEntityConnection.Builder hostname(String hostname) {
		this.hostname = requireNonNull(hostname);
		return this;
	}

	@Override
	public RemoteEntityConnection.Builder port(int port) {
		this.port = port;
		return this;
	}

	@Override
	public RemoteEntityConnection.Builder registryPort(int registryPort) {
		this.registryPort = registryPort;
		return this;
	}

	@Override
	public RemoteEntityConnection.Builder namePrefix(String namePrefix) {
		this.namePrefix = requireNonNull(namePrefix);
		return this;
	}

	@Override
	protected RemoteEntityConnection createConnection() {
		return new ManagedRemoteEntityConnection(this);
	}
}

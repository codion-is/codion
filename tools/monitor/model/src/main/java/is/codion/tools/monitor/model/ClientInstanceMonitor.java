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
package is.codion.tools.monitor.model;

import is.codion.common.logging.MethodTrace;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.state.State;
import is.codion.framework.server.EntityServerAdmin;

import java.rmi.RemoteException;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A ClientInstanceMonitor
 */
public final class ClientInstanceMonitor {

	private final RemoteClient remoteClient;
	private final EntityServerAdmin server;
	private final State loggingEnabled;

	/**
	 * Instantiates a new {@link ClientInstanceMonitor}, monitoring the given client
	 * @param server the server being monitored
	 * @param remoteClient the client info
	 * @throws RemoteException in case of an exception
	 */
	public ClientInstanceMonitor(EntityServerAdmin server, RemoteClient remoteClient) throws RemoteException {
		this.remoteClient = requireNonNull(remoteClient);
		this.server = requireNonNull(server);
		this.loggingEnabled = State.state(server.isTracingEnabled(remoteClient.clientId()));
		bindEvents();
	}

	/**
	 * @return the {@link RemoteClient}
	 */
	public RemoteClient remoteClient() {
		return remoteClient;
	}

	/**
	 * @return the {@link State} for controlling whether logging is enabled
	 */
	public State loggingEnabled() {
		return loggingEnabled;
	}

	public List<MethodTrace> methodTraces() {
		try {
			return server.methodTraces(remoteClient.clientId());
		}
		catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return remoteClient.toString();
	}

	/**
	 * @param status true if method tracing should be enabled, false otherwise
	 */
	private void setLoggingEnabled(boolean status) {
		try {
			server.setTracingEnabled(remoteClient.clientId(), status);
		}
		catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	private void bindEvents() {
		loggingEnabled.addConsumer(this::setLoggingEnabled);
	}
}

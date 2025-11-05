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

import is.codion.common.reactive.state.State;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.utilities.logging.MethodTrace;
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
	private final State tracingEnabled;
	private final State traceToFileEnabled;

	/**
	 * Instantiates a new {@link ClientInstanceMonitor}, monitoring the given client
	 * @param server the server being monitored
	 * @param remoteClient the client info
	 * @throws RemoteException in case of an exception
	 */
	public ClientInstanceMonitor(EntityServerAdmin server, RemoteClient remoteClient) throws RemoteException {
		this.remoteClient = requireNonNull(remoteClient);
		this.server = requireNonNull(server);
		this.tracingEnabled = State.builder()
						.value(server.isTracingEnabled(remoteClient.clientId()))
						.consumer(this::setTracingEnabled)
						.build();
		this.traceToFileEnabled = State.builder()
						.value(server.isTraceToFile(remoteClient.clientId()))
						.consumer(this::setTraceToFile)
						.build();
	}

	/**
	 * @return the {@link RemoteClient}
	 */
	public RemoteClient remoteClient() {
		return remoteClient;
	}

	/**
	 * @return the {@link State} for controlling whether method tracing is enabled
	 */
	public State tracingEnabled() {
		return tracingEnabled;
	}

	/**
	 * @return the {@link State} for controlling whether method traces are written to file
	 */
	public State traceToFileEnabled() {
		return traceToFileEnabled;
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

	private void setTracingEnabled(boolean status) {
		try {
			server.setTracingEnabled(remoteClient.clientId(), status);
		}
		catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	private void setTraceToFile(boolean traceToFile) {
		try {
			server.setTraceToFile(remoteClient.clientId(), traceToFile);
		}
		catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}
}

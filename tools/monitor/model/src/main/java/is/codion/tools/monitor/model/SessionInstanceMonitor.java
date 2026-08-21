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
 * Copyright (c) 2008 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.tools.monitor.model;

import is.codion.common.reactive.state.State;
import is.codion.common.rmi.server.RemoteSession;
import is.codion.common.utilities.logging.MethodTrace;
import is.codion.framework.server.EntityServerAdmin;

import java.rmi.RemoteException;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A SessionInstanceMonitor
 */
public final class SessionInstanceMonitor {

	private final RemoteSession session;
	private final EntityServerAdmin server;
	private final State tracingEnabled;
	private final State traceToFileEnabled;

	/**
	 * Instantiates a new {@link SessionInstanceMonitor}, monitoring the given session
	 * @param server the server being monitored
	 * @param session the session info
	 * @throws RemoteException in case of an exception
	 */
	public SessionInstanceMonitor(EntityServerAdmin server, RemoteSession session) throws RemoteException {
		this.session = requireNonNull(session);
		this.server = requireNonNull(server);
		this.tracingEnabled = State.builder()
						.value(server.tracingEnabled(session.id()))
						.consumer(this::tracingEnabled)
						.build();
		this.traceToFileEnabled = State.builder()
						.value(server.traceToFile(session.id()))
						.consumer(this::traceToFile)
						.build();
	}

	/**
	 * @return the {@link RemoteSession}
	 */
	public RemoteSession session() {
		return session;
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
			return server.methodTraces(session.id());
		}
		catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return session.toString();
	}

	private void tracingEnabled(boolean status) {
		try {
			server.tracingEnabled(session.id(), status);
		}
		catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	private void traceToFile(boolean traceToFile) {
		try {
			server.traceToFile(session.id(), traceToFile);
		}
		catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}
}

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

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.rmi.RemoteException;
import java.text.NumberFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

/**
 * A ClientInstanceMonitor
 */
public final class ClientInstanceMonitor {

	private static final NumberFormat MICROSECOND_FORMAT = NumberFormat.getIntegerInstance();

	private final RemoteClient remoteClient;
	private final EntityServerAdmin server;
	private final State loggingEnabled;
	private final StyledDocument logDocument = new DefaultStyledDocument();
	private final DefaultMutableTreeNode logRootNode = new DefaultMutableTreeNode();
	private final DefaultTreeModel logTreeModel = new DefaultTreeModel(logRootNode);

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

	/**
	 * Refreshes the log document and tree model with the most recent method traces from the server
	 * @throws RemoteException in case of an exception
	 */
	public void refreshLog() throws RemoteException {
		List<MethodTrace> methodTraces = server.methodTraces(remoteClient.clientId());
		try {
			logDocument.remove(0, logDocument.getLength());
			logRootNode.removeAllChildren();
			StringBuilder logBuilder = new StringBuilder();
			for (MethodTrace trace : methodTraces) {
				trace.appendTo(logBuilder);
				DefaultMutableTreeNode traceNode = new DefaultMutableTreeNode(traceString(trace));
				addChildTraces(traceNode, trace.children());
				logRootNode.add(traceNode);
			}
			logDocument.insertString(0, logBuilder.toString(), null);
			logTreeModel.setRoot(logRootNode);
		}
		catch (BadLocationException e) {
			throw new RuntimeException(e);
		}
	}

	public Document logDocument() {
		return logDocument;
	}

	/**
	 * @return the TreeModel for displaying the log in a Tree view
	 */
	public DefaultTreeModel logTreeModel() {
		return logTreeModel;
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

	private static void addChildTraces(DefaultMutableTreeNode traceNode, List<MethodTrace> childTraces) {
		for (MethodTrace trace : childTraces) {
			DefaultMutableTreeNode subEntry = new DefaultMutableTreeNode(traceString(trace));
			addChildTraces(subEntry, trace.children());
			traceNode.add(subEntry);
		}
	}

	private static String traceString(MethodTrace trace) {
		StringBuilder builder = new StringBuilder(trace.method()).append(" [")
						.append(MICROSECOND_FORMAT.format(TimeUnit.NANOSECONDS.toMicros(trace.duration())))
						.append(" μs").append("]");
		String enterMessage = trace.message();
		if (enterMessage != null) {
			builder.append(": ").append(enterMessage.replace('\n', ' '));
		}

		return builder.toString();
	}
}

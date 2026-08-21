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

import is.codion.common.model.component.table.FilterTableModel.TableColumns;
import is.codion.common.rmi.server.RemoteSession;
import is.codion.framework.server.EntityServerAdmin;
import is.codion.swing.common.model.component.table.SwingFilterTableModel;

import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * A SessionMonitor
 */
public final class SessionMonitor {

	private final EntityServerAdmin server;

	private final SwingFilterTableModel<RemoteSession, String> sessionTableModel =
					SwingFilterTableModel.builder()
									.columns(new RemoteSessionColumns())
									.items(new RemoteSessionItems())
									.build();

	/**
	 * Instantiates a new {@link SessionMonitor}
	 * @param server the server being monitored
	 */
	public SessionMonitor(EntityServerAdmin server) {
		this.server = requireNonNull(server);
		refresh();
	}

	/**
	 * Refreshes the session info from the server
	 */
	public void refresh() {
		sessionTableModel.items().refresh();
	}

	/**
	 * @return the TableModel for displaying the sessions
	 */
	public SwingFilterTableModel<RemoteSession, String> sessionTableModel() {
		return sessionTableModel;
	}

	public EntityServerAdmin server() {
		return server;
	}

	private final class RemoteSessionItems implements Supplier<Collection<RemoteSession>> {

		@Override
		public Collection<RemoteSession> get() {
			try {
				return server.sessions();
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static final class RemoteSessionColumns implements TableColumns<RemoteSession, String> {

		public static final String USER = "User";
		public static final String CLIENT_HOST = "Host";
		public static final String CLIENT_TYPE = "Type";
		public static final String CLIENT_VERSION = "Version";
		public static final String CODION_VERSION = "Framework version";
		public static final String CONNECTION_ID = "Id";
		public static final String LOCALE = "Locale";
		public static final String TIMEZONE = "Timezone";
		public static final String CREATION_TIME = "Created";

		private static final List<String> IDENTIFIERS = unmodifiableList(asList(
						USER, CLIENT_HOST, CLIENT_TYPE, CLIENT_VERSION, CODION_VERSION, CONNECTION_ID, LOCALE, TIMEZONE, CREATION_TIME
		));

		@Override
		public List<String> identifiers() {
			return IDENTIFIERS;
		}

		@Override
		public Class<?> columnClass(String identifier) {
			if (identifier.equals(CREATION_TIME)) {
				return LocalDateTime.class;
			}

			return String.class;
		}

		@Override
		public Object value(RemoteSession row, String identifier) {
			switch (identifier) {
				case USER:
					return row.request().user().username();
				case CLIENT_HOST:
					return row.clientHost();
				case CLIENT_TYPE:
					return row.request().clientType();
				case CLIENT_VERSION:
					return row.request().version()
									.map(Object::toString)
									.orElse(null);
				case CODION_VERSION:
					return row.request().frameworkVersion().toString();
				case CONNECTION_ID:
					return row.id().toString();
				case LOCALE:
					return row.request().locale().toString();
				case TIMEZONE:
					return row.request().timeZone().toString();
				case CREATION_TIME:
					return row.creationTime();
				default:
					throw new IllegalArgumentException("Unknown column");
			}
		}
	}
}
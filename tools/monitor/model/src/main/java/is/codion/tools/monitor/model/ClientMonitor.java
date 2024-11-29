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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.tools.monitor.model;

import is.codion.common.rmi.server.RemoteClient;
import is.codion.framework.server.EntityServerAdmin;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.model.component.table.FilterTableModel.Columns;

import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * A ClientMonitor
 */
public final class ClientMonitor {

	private final EntityServerAdmin server;

	private final FilterTableModel<RemoteClient, RemoteClientColumns.Id> clientInstanceTableModel =
					FilterTableModel.builder(new RemoteClientColumns())
									.supplier(new RemoteClientItems())
									.build();

	/**
	 * Instantiates a new {@link ClientMonitor}
	 * @param server the server being monitored
	 */
	public ClientMonitor(EntityServerAdmin server) {
		this.server = requireNonNull(server);
		refresh();
	}

	/**
	 * Refreshes the client info from the server
	 */
	public void refresh() {
		clientInstanceTableModel.refresh();
	}

	/**
	 * @return the TableModel for displaying the client instances
	 */
	public FilterTableModel<RemoteClient, RemoteClientColumns.Id> clientInstanceTableModel() {
		return clientInstanceTableModel;
	}

	public EntityServerAdmin server() {
		return server;
	}

	private final class RemoteClientItems implements Supplier<Collection<RemoteClient>> {

		@Override
		public Collection<RemoteClient> get() {
			try {
				return server.clients();
			}
			catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static final class RemoteClientColumns implements Columns<RemoteClient, RemoteClientColumns.Id> {

		public enum Id {
			USER,
			CLIENT_HOST,
			CLIENT_TYPE,
			CLIENT_VERSION,
			CODION_VERSION,
			CLIENT_ID,
			LOCALE,
			TIMEZONE,
			CREATION_TIME
		}

		private static final List<Id> IDENTIFIERS = unmodifiableList(asList(Id.values()));

		@Override
		public List<Id> identifiers() {
			return IDENTIFIERS;
		}

		@Override
		public Class<?> columnClass(Id identifier) {
			if (identifier.equals(Id.CREATION_TIME)) {
				return LocalDateTime.class;
			}

			return String.class;
		}

		@Override
		public Object value(RemoteClient row, Id identifier) {
			switch (identifier) {
				case USER:
					return row.user().username();
				case CLIENT_HOST:
					return row.clientHost();
				case CLIENT_TYPE:
					return row.clientType();
				case CLIENT_VERSION:
					return row.clientVersion()
									.map(Object::toString)
									.orElse(null);
				case CODION_VERSION:
					return row.frameworkVersion().toString();
				case CLIENT_ID:
					return row.clientId().toString();
				case LOCALE:
					return row.clientLocale().toString();
				case TIMEZONE:
					return row.clientTimeZone().toString();
				case CREATION_TIME:
					return row.creationTime();
				default:
					throw new IllegalArgumentException("Unknown column");
			}
		}
	}
}
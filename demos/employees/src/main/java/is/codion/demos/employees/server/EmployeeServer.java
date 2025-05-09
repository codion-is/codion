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
 * Copyright (c) 2004 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.employees.server;

import is.codion.common.db.database.Database;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.demos.employees.domain.Employees;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.server.AbstractRemoteEntityConnection;
import is.codion.framework.server.EntityServer;
import is.codion.framework.server.EntityServerConfiguration;

import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.Collection;

import static is.codion.demos.employees.domain.Employees.Employee;
import static is.codion.framework.domain.entity.condition.Condition.all;

public final class EmployeeServer extends EntityServer {

	private final Domain domain = new Employees();

	public EmployeeServer(EntityServerConfiguration configuration) throws RemoteException {
		super(configuration);
	}

	@Override
	protected AbstractRemoteEntityConnection createRemoteConnection(Database database,
																																	RemoteClient remoteClient, int port,
																																	RMIClientSocketFactory clientSocketFactory,
																																	RMIServerSocketFactory serverSocketFactory)
					throws RemoteException {
		return new DefaultEmployeeService(domain, database, remoteClient, port);
	}

	static final class DefaultEmployeeService extends AbstractRemoteEntityConnection implements EmployeeService {

		private DefaultEmployeeService(Domain domain, Database database, RemoteClient remoteClient, int port)
						throws RemoteException {
			super(domain, database, remoteClient, port, null, null);
		}

		@Override
		public Collection<Entity> employees() throws RemoteException {
			synchronized (connectionProxy) {
				return connectionProxy.select(all(Employee.TYPE));
			}
		}
	}
}

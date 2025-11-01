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
 * Copyright (c) 2017 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.server;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.ReportType;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Count;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.db.rmi.RemoteEntityConnection;
import is.codion.framework.db.rmi.RemoteEntityResultIterator;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.condition.Condition;

import java.io.Serial;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A default RemoteEntityConnection implementation.
 */
final class DefaultRemoteEntityConnection extends AbstractRemoteEntityConnection implements RemoteEntityConnection {

	@Serial
	private static final long serialVersionUID = 1;

	/**
	 * Instantiates a new DefaultRemoteEntityConnection and exports it on the given port number
	 * @param domain the domain model
	 * @param database the underlying database
	 * @param remoteClient the client requesting the connection
	 * @param port the port to use when exporting this remote connection
	 * @throws RemoteException in case of an exception
	 * @throws DatabaseException in case a database connection can not be established, for example
	 * if a wrong username or password is provided
	 */
	DefaultRemoteEntityConnection(Domain domain, Database database, RemoteClient remoteClient,
																int port) throws RemoteException {
		this(domain, database, remoteClient, port, null, null);
	}

	/**
	 * Instantiates a new DefaultRemoteEntityConnection and exports it on the given port number
	 * @param domain the domain model
	 * @param database the underlying database
	 * @param remoteClient the client requesting the connection
	 * @param port the port to use when exporting this remote connection
	 * @param clientSocketFactory the client socket factory to use, null for default
	 * @param serverSocketFactory the server socket factory to use, null for default
	 * @throws RemoteException in case of an exception
	 * @throws DatabaseException in case a database connection can not be established, for example
	 * if a wrong username or password is provided
	 */
	DefaultRemoteEntityConnection(Domain domain, Database database, RemoteClient remoteClient,
																int port, RMIClientSocketFactory clientSocketFactory,
																RMIServerSocketFactory serverSocketFactory)
					throws RemoteException {
		super(domain, database, remoteClient, port, clientSocketFactory, serverSocketFactory);
	}

	@Override
	public Entities entities() {
		synchronized (connectionProxy) {
			return connectionProxy.entities();
		}
	}

	@Override
	public int count(Count count) {
		synchronized (connectionProxy) {
			return connectionProxy.count(count);
		}
	}

	@Override
	public <T, R, P> R report(ReportType<T, R, P> reportType, P parameter) {
		synchronized (connectionProxy) {
			return connectionProxy.report(reportType, parameter);
		}
	}

	@Override
	public <C extends EntityConnection, T> void execute(ProcedureType<C, T> procedureType) {
		execute(procedureType, null);
	}

	@Override
	public <C extends EntityConnection, T> void execute(ProcedureType<C, T> procedureType, T parameter) {
		synchronized (connectionProxy) {
			connectionProxy.execute(procedureType, parameter);
		}
	}

	@Override
	public <C extends EntityConnection, T, R> R execute(FunctionType<C, T, R> functionType) {
		return execute(functionType, null);
	}

	@Override
	public <C extends EntityConnection, T, R> R execute(FunctionType<C, T, R> functionType, T parameter) {
		synchronized (connectionProxy) {
			return connectionProxy.execute(functionType, parameter);
		}
	}

	@Override
	public void startTransaction() {
		synchronized (connectionProxy) {
			connectionProxy.startTransaction();
		}
	}

	@Override
	public void commitTransaction() {
		synchronized (connectionProxy) {
			connectionProxy.commitTransaction();
		}
	}

	@Override
	public void queryCache(boolean queryCache) {
		synchronized (connectionProxy) {
			connectionProxy.queryCache(queryCache);
		}
	}

	@Override
	public boolean queryCache() {
		synchronized (connectionProxy) {
			return connectionProxy.queryCache();
		}
	}

	@Override
	public void rollbackTransaction() {
		synchronized (connectionProxy) {
			connectionProxy.rollbackTransaction();
		}
	}

	@Override
	public boolean transactionOpen() {
		synchronized (connectionProxy) {
			return connectionProxy.transactionOpen();
		}
	}

	@Override
	public Entity.Key insert(Entity entity) {
		synchronized (connectionProxy) {
			return connectionProxy.insert(entity);
		}
	}

	@Override
	public Entity insertSelect(Entity entity) {
		synchronized (connectionProxy) {
			return connectionProxy.insertSelect(entity);
		}
	}

	@Override
	public Collection<Entity.Key> insert(Collection<Entity> entities) {
		synchronized (connectionProxy) {
			return connectionProxy.insert(entities);
		}
	}

	@Override
	public Collection<Entity> insertSelect(Collection<Entity> entities) {
		synchronized (connectionProxy) {
			return connectionProxy.insertSelect(entities);
		}
	}

	@Override
	public void update(Entity entity) {
		synchronized (connectionProxy) {
			connectionProxy.update(entity);
		}
	}

	@Override
	public Entity updateSelect(Entity entity) {
		synchronized (connectionProxy) {
			return connectionProxy.updateSelect(entity);
		}
	}

	@Override
	public void update(Collection<Entity> entities) {
		synchronized (connectionProxy) {
			connectionProxy.update(entities);
		}
	}

	@Override
	public Collection<Entity> updateSelect(Collection<Entity> entities) {
		synchronized (connectionProxy) {
			return connectionProxy.updateSelect(entities);
		}
	}

	@Override
	public int update(Update update) {
		synchronized (connectionProxy) {
			return connectionProxy.update(update);
		}
	}

	@Override
	public void delete(Entity.Key key) {
		synchronized (connectionProxy) {
			connectionProxy.delete(key);
		}
	}

	@Override
	public void delete(Collection<Entity.Key> keys) {
		synchronized (connectionProxy) {
			connectionProxy.delete(keys);
		}
	}

	@Override
	public int delete(Condition condition) {
		synchronized (connectionProxy) {
			return connectionProxy.delete(condition);
		}
	}

	@Override
	public <T> List<T> select(Column<T> column) {
		synchronized (connectionProxy) {
			return connectionProxy.select(column);
		}
	}

	@Override
	public <T> List<T> select(Column<T> column, Condition condition) {
		synchronized (connectionProxy) {
			return connectionProxy.select(column, condition);
		}
	}

	@Override
	public <T> List<T> select(Column<T> column, Select select) {
		synchronized (connectionProxy) {
			return connectionProxy.select(column, select);
		}
	}

	@Override
	public Entity select(Entity.Key key) {
		synchronized (connectionProxy) {
			return connectionProxy.select(key);
		}
	}

	@Override
	public Entity selectSingle(Condition condition) {
		synchronized (connectionProxy) {
			return connectionProxy.selectSingle(condition);
		}
	}

	@Override
	public Entity selectSingle(Select select) {
		synchronized (connectionProxy) {
			return connectionProxy.selectSingle(select);
		}
	}

	@Override
	public Collection<Entity> select(Collection<Entity.Key> keys) {
		synchronized (connectionProxy) {
			return connectionProxy.select(keys);
		}
	}

	@Override
	public List<Entity> select(Condition condition) {
		synchronized (connectionProxy) {
			return connectionProxy.select(condition);
		}
	}

	@Override
	public List<Entity> select(Select select) {
		synchronized (connectionProxy) {
			return connectionProxy.select(select);
		}
	}

	@Override
	public Map<EntityType, Collection<Entity>> dependencies(Collection<Entity> entities) {
		synchronized (connectionProxy) {
			return connectionProxy.dependencies(entities);
		}
	}

	@Override
	public RemoteEntityResultIterator iterator(Condition condition) throws RemoteException {
		synchronized (connectionProxy) {
			return remoteIterator(connectionProxy.iterator(condition));
		}
	}

	@Override
	public RemoteEntityResultIterator iterator(Select select) throws RemoteException {
		synchronized (connectionProxy) {
			return remoteIterator(connectionProxy.iterator(select));
		}
	}
}
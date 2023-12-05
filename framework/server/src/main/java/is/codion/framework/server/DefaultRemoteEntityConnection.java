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
 * Copyright (c) 2017 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.server;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.ReportException;
import is.codion.common.db.report.ReportType;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Count;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.db.rmi.RemoteEntityConnection;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.condition.Condition;

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
                                int port) throws DatabaseException, RemoteException {
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
          throws DatabaseException, RemoteException {
    super(domain, database, remoteClient, port, clientSocketFactory, serverSocketFactory);
  }

  @Override
  public Entities entities() {
    synchronized (connectionProxy) {
      return connectionProxy.entities();
    }
  }

  @Override
  public int count(Count count) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.count(count);
    }
  }

  @Override
  public <T, R, P> R report(ReportType<T, R, P> reportType, P reportParameters) throws ReportException, DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.report(reportType, reportParameters);
    }
  }

  @Override
  public <C extends EntityConnection, T> void execute(ProcedureType<C, T> procedureType) throws DatabaseException {
    execute(procedureType, null);
  }

  @Override
  public <C extends EntityConnection, T> void execute(ProcedureType<C, T> procedureType, T argument) throws DatabaseException {
    synchronized (connectionProxy) {
      connectionProxy.execute(procedureType, argument);
    }
  }

  @Override
  public <C extends EntityConnection, T, R> R execute(FunctionType<C, T, R> functionType) throws DatabaseException {
    return execute(functionType, null);
  }

  @Override
  public <C extends EntityConnection, T, R> R execute(FunctionType<C, T, R> functionType, T argument) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.execute(functionType, argument);
    }
  }

  @Override
  public void beginTransaction() {
    synchronized (connectionProxy) {
      connectionProxy.beginTransaction();
    }
  }

  @Override
  public void commitTransaction() {
    synchronized (connectionProxy) {
      connectionProxy.commitTransaction();
    }
  }

  @Override
  public void setQueryCacheEnabled(boolean queryCacheEnabled) {
    synchronized (connectionProxy) {
      connectionProxy.setQueryCacheEnabled(queryCacheEnabled);
    }
  }

  @Override
  public boolean isQueryCacheEnabled() {
    synchronized (connectionProxy) {
      return connectionProxy.isQueryCacheEnabled();
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
  public Entity.Key insert(Entity entity) throws RemoteException, DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.insert(entity);
    }
  }

  @Override
  public Entity insertSelect(Entity entity) throws RemoteException, DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.insertSelect(entity);
    }
  }

  @Override
  public Collection<Entity.Key> insert(Collection<? extends Entity> entities) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.insert(entities);
    }
  }

  @Override
  public Collection<Entity> insertSelect(Collection<? extends Entity> entities) throws RemoteException, DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.insertSelect(entities);
    }
  }

  @Override
  public void update(Entity entity) throws RemoteException, DatabaseException {
    synchronized (connectionProxy) {
      connectionProxy.update(entity);
    }
  }

  @Override
  public Entity updateSelect(Entity entity) throws RemoteException, DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.updateSelect(entity);
    }
  }

  @Override
  public void update(Collection<? extends Entity> entities) throws RemoteException, DatabaseException {
    synchronized (connectionProxy) {
      connectionProxy.update(entities);
    }
  }

  @Override
  public Collection<Entity> updateSelect(Collection<? extends Entity> entities) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.updateSelect(entities);
    }
  }

  @Override
  public int update(Update update) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.update(update);
    }
  }

  @Override
  public void delete(Entity.Key key) throws RemoteException, DatabaseException {
    synchronized (connectionProxy) {
      connectionProxy.delete(key);
    }
  }

  @Override
  public void delete(Collection<Entity.Key> keys) throws DatabaseException {
    synchronized (connectionProxy) {
      connectionProxy.delete(keys);
    }
  }

  @Override
  public int delete(Condition condition) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.delete(condition);
    }
  }

  @Override
  public <T> List<T> select(Column<T> column) throws RemoteException, DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.select(column);
    }
  }

  @Override
  public <T> List<T> select(Column<T> column, Condition condition) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.select(column, condition);
    }
  }

  @Override
  public <T> List<T> select(Column<T> column, Select select) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.select(column, select);
    }
  }

  @Override
  public Entity select(Entity.Key key) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.select(key);
    }
  }

  @Override
  public Entity selectSingle(Condition condition) throws RemoteException, DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.selectSingle(condition);
    }
  }

  @Override
  public Entity selectSingle(Select select) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.selectSingle(select);
    }
  }

  @Override
  public Collection<Entity> select(Collection<Entity.Key> keys) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.select(keys);
    }
  }

  @Override
  public List<Entity> select(Condition condition) throws RemoteException, DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.select(condition);
    }
  }

  @Override
  public List<Entity> select(Select select) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.select(select);
    }
  }

  @Override
  public Map<EntityType, Collection<Entity>> dependencies(Collection<? extends Entity> entities) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.dependencies(entities);
    }
  }
}
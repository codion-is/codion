/*
 * Copyright (c) 2017 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
import is.codion.framework.db.condition.Select;
import is.codion.framework.db.condition.Update;
import is.codion.framework.db.criteria.Criteria;
import is.codion.framework.db.rmi.RemoteEntityConnection;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Column;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.Key;

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
  public int rowCount(Criteria criteria) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.rowCount(criteria);
    }
  }

  @Override
  public <T, R, P> R fillReport(ReportType<T, R, P> reportType, P reportParameters) throws ReportException, DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.fillReport(reportType, reportParameters);
    }
  }

  @Override
  public <C extends EntityConnection, T> void executeProcedure(ProcedureType<C, T> procedureType) throws DatabaseException {
    executeProcedure(procedureType, null);
  }

  @Override
  public <C extends EntityConnection, T> void executeProcedure(ProcedureType<C, T> procedureType, T argument) throws DatabaseException {
    synchronized (connectionProxy) {
      connectionProxy.executeProcedure(procedureType, argument);
    }
  }

  @Override
  public <C extends EntityConnection, T, R> R executeFunction(FunctionType<C, T, R> functionType) throws DatabaseException {
    return executeFunction(functionType, null);
  }

  @Override
  public <C extends EntityConnection, T, R> R executeFunction(FunctionType<C, T, R> functionType, T argument) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.executeFunction(functionType, argument);
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
  public boolean isTransactionOpen() {
    synchronized (connectionProxy) {
      return connectionProxy.isTransactionOpen();
    }
  }

  @Override
  public Key insert(Entity entity) throws RemoteException, DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.insert(entity);
    }
  }

  @Override
  public Collection<Key> insert(Collection<? extends Entity> entities) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.insert(entities);
    }
  }

  @Override
  public Entity update(Entity entity) throws RemoteException, DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.update(entity);
    }
  }

  @Override
  public Collection<Entity> update(Collection<? extends Entity> entities) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.update(entities);
    }
  }

  @Override
  public int update(Update update) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.update(update);
    }
  }

  @Override
  public void delete(Key entityKey) throws RemoteException, DatabaseException {
    synchronized (connectionProxy) {
      connectionProxy.delete(entityKey);
    }
  }

  @Override
  public void delete(Collection<Key> entityKeys) throws DatabaseException {
    synchronized (connectionProxy) {
      connectionProxy.delete(entityKeys);
    }
  }

  @Override
  public int delete(Criteria criteria) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.delete(criteria);
    }
  }

  @Override
  public <T> List<T> select(Column<T> column) throws RemoteException, DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.select(column);
    }
  }

  @Override
  public <T> List<T> select(Column<T> column, Criteria criteria) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.select(column, criteria);
    }
  }

  @Override
  public <T> List<T> select(Column<T> column, Select select) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.select(column, select);
    }
  }

  @Override
  public Entity select(Key key) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.select(key);
    }
  }

  @Override
  public Entity selectSingle(Criteria criteria) throws RemoteException, DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.selectSingle(criteria);
    }
  }

  @Override
  public Entity selectSingle(Select select) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.selectSingle(select);
    }
  }

  @Override
  public Collection<Entity> select(Collection<Key> keys) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.select(keys);
    }
  }

  @Override
  public List<Entity> select(Criteria criteria) throws RemoteException, DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.select(criteria);
    }
  }

  @Override
  public List<Entity> select(Select select) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.select(select);
    }
  }

  @Override
  public Map<EntityType, Collection<Entity>> selectDependencies(Collection<? extends Entity> entities) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.selectDependencies(entities);
    }
  }

  @Override
  public void writeBlob(Key primaryKey, Column<byte[]> blobColumn, byte[] blobData) throws DatabaseException {
    synchronized (connectionProxy) {
      connectionProxy.writeBlob(primaryKey, blobColumn, blobData);
    }
  }

  @Override
  public byte[] readBlob(Key primaryKey, Column<byte[]> blobColumn) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.readBlob(primaryKey, blobColumn);
    }
  }
}
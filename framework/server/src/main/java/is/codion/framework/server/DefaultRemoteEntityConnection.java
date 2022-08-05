/*
 * Copyright (c) 2017 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.UpdateCondition;
import is.codion.framework.db.rmi.RemoteEntityConnection;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Attribute;
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
  public Entities getEntities() {
    synchronized (connectionProxy) {
      return connectionProxy.entities();
    }
  }

  @Override
  public int rowCount(Condition condition) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.rowCount(condition);
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
  public List<Key> insert(List<? extends Entity> entities) throws DatabaseException {
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
  public List<Entity> update(List<? extends Entity> entities) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.update(entities);
    }
  }

  @Override
  public int update(UpdateCondition condition) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.update(condition);
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
  public int delete(Condition condition) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.delete(condition);
    }
  }

  @Override
  public <T> List<T> select(Attribute<T> attribute) throws RemoteException, DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.select(attribute);
    }
  }

  @Override
  public <T> List<T> select(Attribute<T> attribute, Condition condition) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.select(attribute, condition);
    }
  }

  @Override
  public <T> Entity selectSingle(Attribute<T> attribute, T value) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.selectSingle(attribute, value);
    }
  }

  @Override
  public Entity select(Key key) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.select(key);
    }
  }

  @Override
  public Entity selectSingle(Condition condition) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.selectSingle(condition);
    }
  }

  @Override
  public List<Entity> select(Collection<Key> keys) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.select(keys);
    }
  }

  @Override
  public List<Entity> select(Condition condition) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.select(condition);
    }
  }

  @Override
  public <T> List<Entity> select(Attribute<T> attribute, T value) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.select(attribute, value);
    }
  }

  @Override
  public <T> List<Entity> select(Attribute<T> attribute, Collection<T> values) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.select(attribute, values);
    }
  }

  @Override
  public Map<EntityType, Collection<Entity>> selectDependencies(Collection<? extends Entity> entities) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.selectDependencies(entities);
    }
  }

  @Override
  public void writeBlob(Key primaryKey, Attribute<byte[]> blobAttribute, byte[] blobData) throws DatabaseException {
    synchronized (connectionProxy) {
      connectionProxy.writeBlob(primaryKey, blobAttribute, blobData);
    }
  }

  @Override
  public byte[] readBlob(Key primaryKey, Attribute<byte[]> blobAttribute) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.readBlob(primaryKey, blobAttribute);
    }
  }
}
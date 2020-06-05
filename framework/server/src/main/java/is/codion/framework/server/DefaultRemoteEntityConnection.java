/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.server;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.reports.ReportException;
import is.codion.common.db.reports.ReportWrapper;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.condition.EntityCondition;
import is.codion.framework.db.condition.EntitySelectCondition;
import is.codion.framework.db.condition.EntityUpdateCondition;
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
  DefaultRemoteEntityConnection(final Domain domain, final Database database, final RemoteClient remoteClient,
                                final int port) throws DatabaseException, RemoteException {
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
  DefaultRemoteEntityConnection(final Domain domain, final Database database, final RemoteClient remoteClient,
                                final int port, final RMIClientSocketFactory clientSocketFactory,
                                final RMIServerSocketFactory serverSocketFactory)
          throws DatabaseException, RemoteException {
    super(domain, database, remoteClient, port, clientSocketFactory, serverSocketFactory);
  }

  @Override
  public Entities getEntities() {
    synchronized (connectionProxy) {
      return connectionProxy.getEntities();
    }
  }

  @Override
  public int selectRowCount(final EntityCondition condition) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.selectRowCount(condition);
    }
  }

  @Override
  public <T, R, P> R fillReport(final ReportWrapper<T, R, P> reportWrapper, final P reportParameters) throws ReportException, DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.fillReport(reportWrapper, reportParameters);
    }
  }

  @Override
  public <T> void executeProcedure(final ProcedureType<EntityConnection, T> procedureType, final T... arguments) throws DatabaseException {
    synchronized (connectionProxy) {
      connectionProxy.executeProcedure(procedureType, arguments);
    }
  }

  @Override
  public <T, R> R executeFunction(final FunctionType<EntityConnection, T, R> functionType, final T... arguments) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.executeFunction(functionType, arguments);
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
  public Key insert(final Entity entity) throws RemoteException, DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.insert(entity);
    }
  }

  @Override
  public List<Key> insert(final List<Entity> entities) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.insert(entities);
    }
  }

  @Override
  public Entity update(final Entity entity) throws RemoteException, DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.update(entity);
    }
  }

  @Override
  public List<Entity> update(final List<Entity> entities) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.update(entities);
    }
  }

  @Override
  public int update(final EntityUpdateCondition condition) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.update(condition);
    }
  }

  @Override
  public boolean delete(final Key entityKey) throws RemoteException, DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.delete(entityKey);
    }
  }

  @Override
  public int delete(final List<Key> entityKeys) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.delete(entityKeys);
    }
  }

  @Override
  public int delete(final EntityCondition condition) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.delete(condition);
    }
  }

  @Override
  public <T> List<T> selectValues(final Attribute<T> attribute, final EntityCondition condition) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.selectValues(attribute, condition);
    }
  }

  @Override
  public <T> Entity selectSingle(final EntityType entityType, final Attribute<T> attribute, final T value) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.selectSingle(entityType, attribute, value);
    }
  }

  @Override
  public Entity selectSingle(final Key key) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.selectSingle(key);
    }
  }

  @Override
  public Entity selectSingle(final EntitySelectCondition condition) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.selectSingle(condition);
    }
  }

  @Override
  public List<Entity> select(final List<Key> keys) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.select(keys);
    }
  }

  @Override
  public List<Entity> select(final EntitySelectCondition condition) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.select(condition);
    }
  }

  @Override
  public <T> List<Entity> select(final EntityType entityType, final Attribute<T> attribute,
                                 final T value) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.select(entityType, attribute, value);
    }
  }

  @Override
  public <T> List<Entity> select(final EntityType entityType, final Attribute<T> attribute,
                                 final Collection<T> values) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.select(entityType, attribute, values);
    }
  }

  @Override
  public Map<EntityType, Collection<Entity>> selectDependencies(final Collection<Entity> entities) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.selectDependencies(entities);
    }
  }

  @Override
  public void writeBlob(final Key primaryKey, final Attribute<byte[]> blobAttribute, final byte[] blobData) throws DatabaseException {
    synchronized (connectionProxy) {
      connectionProxy.writeBlob(primaryKey, blobAttribute, blobData);
    }
  }

  @Override
  public byte[] readBlob(final Key primaryKey, final Attribute<byte[]> blobAttribute) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.readBlob(primaryKey, blobAttribute);
    }
  }
}
/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.database.Database;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.reports.ReportException;
import org.jminor.common.db.reports.ReportWrapper;
import org.jminor.common.rmi.server.RemoteClient;
import org.jminor.framework.db.condition.EntityCondition;
import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.framework.db.condition.EntityUpdateCondition;
import org.jminor.framework.db.rmi.RemoteEntityConnection;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.entity.Entities;
import org.jminor.framework.domain.entity.Entity;

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
  public void executeProcedure(final String procedureId, final Object... arguments) throws DatabaseException {
    synchronized (connectionProxy) {
      connectionProxy.executeProcedure(procedureId, arguments);
    }
  }

  @Override
  public <T> T executeFunction(final String functionId, final Object... arguments) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.executeFunction(functionId, arguments);
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
  public Entity.Key insert(final Entity entity) throws RemoteException, DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.insert(entity);
    }
  }

  @Override
  public List<Entity.Key> insert(final List<Entity> entities) throws DatabaseException {
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
  public boolean delete(final Entity.Key entityKey) throws RemoteException, DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.delete(entityKey);
    }
  }

  @Override
  public int delete(final List<Entity.Key> entityKeys) throws DatabaseException {
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
  public <T> List<T> selectValues(final String propertyId, final EntityCondition condition) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.selectValues(propertyId, condition);
    }
  }

  @Override
  public Entity selectSingle(final String entityId, final String propertyId, final Object value) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.selectSingle(entityId, propertyId, value);
    }
  }

  @Override
  public Entity selectSingle(final Entity.Key key) throws DatabaseException {
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
  public List<Entity> select(final List<Entity.Key> keys) throws DatabaseException {
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
  public List<Entity> select(final String entityId, final String propertyId,
                             final Object... values) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.select(entityId, propertyId, values);
    }
  }

  @Override
  public Map<String, Collection<Entity>> selectDependencies(final Collection<Entity> entities) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.selectDependencies(entities);
    }
  }

  @Override
  public void writeBlob(final Entity.Key primaryKey, final String blobPropertyId, final byte[] blobData) throws DatabaseException {
    synchronized (connectionProxy) {
      connectionProxy.writeBlob(primaryKey, blobPropertyId, blobData);
    }
  }

  @Override
  public byte[] readBlob(final Entity.Key primaryKey, final String blobPropertyId) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.readBlob(primaryKey, blobPropertyId);
    }
  }
}
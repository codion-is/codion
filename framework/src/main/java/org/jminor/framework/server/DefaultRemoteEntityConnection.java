/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.Database;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.reports.ReportException;
import org.jminor.common.db.reports.ReportResult;
import org.jminor.common.db.reports.ReportWrapper;
import org.jminor.common.server.RemoteClient;
import org.jminor.framework.db.RemoteEntityConnection;
import org.jminor.framework.db.condition.EntityCondition;
import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.framework.domain.Entity;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * An implementation of the RemoteEntityConnection interface, provides the logging of service calls
 * and database connection pooling.
 */
final class DefaultRemoteEntityConnection extends AbstractRemoteEntityConnection implements RemoteEntityConnection {

  /**
   * Instantiates a new DefaultRemoteEntityConnection and exports it on the given port number
   * @param database defines the underlying database
   * @param remoteClient the client requesting the connection
   * @param port the port to use when exporting this remote connection
   * @param loggingEnabled specifies whether or not method logging is enabled
   * @param sslEnabled specifies whether or not ssl should be enabled
   * @throws RemoteException in case of an exception
   * @throws DatabaseException in case a database connection can not be established, for example
   * if a wrong username or password is provided
   */
  DefaultRemoteEntityConnection(final Database database, final RemoteClient remoteClient, final int port,
                                final boolean loggingEnabled, final boolean sslEnabled)
          throws DatabaseException, RemoteException {
    this(null, database, remoteClient, port, loggingEnabled, sslEnabled);
  }

  /**
   * Instantiates a new DefaultRemoteEntityConnection and exports it on the given port number
   * @param connectionPool the connection pool to use, if none is provided a local connection is established
   * @param database defines the underlying database
   * @param remoteClient the client requesting the connection
   * @param port the port to use when exporting this remote connection
   * @param loggingEnabled specifies whether or not method logging is enabled
   * @param sslEnabled specifies whether or not ssl should be enabled
   * @throws RemoteException in case of an exception
   * @throws DatabaseException in case a database connection can not be established, for example
   * if a wrong username or password is provided
   */
  DefaultRemoteEntityConnection(final ConnectionPool connectionPool, final Database database, final RemoteClient remoteClient,
                                final int port, final boolean loggingEnabled, final boolean sslEnabled)
          throws DatabaseException, RemoteException {
    super(connectionPool, database, remoteClient, port, loggingEnabled, sslEnabled);
  }

  /** {@inheritDoc} */
  @Override
  public int selectRowCount(final EntityCondition condition) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.selectRowCount(condition);
    }
  }

  /** {@inheritDoc} */
  @Override
  public ReportResult fillReport(final ReportWrapper reportWrapper) throws ReportException, DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.fillReport(reportWrapper);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void executeProcedure(final String procedureID, final Object... arguments) throws DatabaseException {
    synchronized (connectionProxy) {
      connectionProxy.executeProcedure(procedureID, arguments);
    }
  }

  /** {@inheritDoc} */
  @Override
  public List executeFunction(final String functionID, final Object... arguments) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.executeFunction(functionID, arguments);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void beginTransaction() {
    synchronized (connectionProxy) {
      connectionProxy.beginTransaction();
    }
  }

  /** {@inheritDoc} */
  @Override
  public void commitTransaction() {
    synchronized (connectionProxy) {
      connectionProxy.commitTransaction();
    }
  }

  /** {@inheritDoc} */
  @Override
  public void rollbackTransaction() {
    synchronized (connectionProxy) {
      connectionProxy.rollbackTransaction();
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean isTransactionOpen() {
    synchronized (connectionProxy) {
      return connectionProxy.isTransactionOpen();
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity.Key> insert(final List<Entity> entities) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.insert(entities);
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> update(final List<Entity> entities) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.update(entities);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void delete(final List<Entity.Key> entityKeys) throws DatabaseException {
    synchronized (connectionProxy) {
      connectionProxy.delete(entityKeys);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void delete(final EntityCondition condition) throws DatabaseException {
    synchronized (connectionProxy) {
      connectionProxy.delete(condition);
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<Object> selectValues(final String propertyID, final EntityCondition condition) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.selectValues(propertyID, condition);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Entity selectSingle(final String entityID, final String propertyID, final Object value) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.selectSingle(entityID, propertyID, value);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Entity selectSingle(final Entity.Key key) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.selectSingle(key);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Entity selectSingle(final EntitySelectCondition condition) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.selectSingle(condition);
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> selectMany(final List<Entity.Key> keys) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.selectMany(keys);
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> selectMany(final EntitySelectCondition condition) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.selectMany(condition);
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> selectMany(final String entityID, final String propertyID,
                                 final Object... values) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.selectMany(entityID, propertyID, values);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Map<String, Collection<Entity>> selectDependentEntities(final Collection<Entity> entities) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.selectDependentEntities(entities);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void writeBlob(final Entity.Key primaryKey, final String blobPropertyID, final byte[] blobData) throws DatabaseException {
    synchronized (connectionProxy) {
      connectionProxy.writeBlob(primaryKey, blobPropertyID, blobData);
    }
  }

  /** {@inheritDoc} */
  @Override
  public byte[] readBlob(final Entity.Key primaryKey, final String blobPropertyID) throws DatabaseException {
    synchronized (connectionProxy) {
      return connectionProxy.readBlob(primaryKey, blobPropertyID);
    }
  }
}
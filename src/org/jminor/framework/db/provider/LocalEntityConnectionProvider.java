/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.provider;

import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.model.tools.MethodLogger;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

/**
 * A class responsible for managing a local EntityConnection.
 */
public final class LocalEntityConnectionProvider extends AbstractEntityConnectionProvider {

  private static final Logger LOG = LoggerFactory.getLogger(LocalEntityConnectionProvider.class);
  private static final boolean SCHEDULE_VALIDITY_CHECK = Configuration.getBooleanValue(Configuration.CONNECTION_SCHEDULE_VALIDATION);

  /**
   * The underlying database implementation
   */
  private final Database database;

  private final Properties connectionProperties = new Properties();

  /**
   * Instantiates a new LocalEntityConnectionProvider
   * @param user the user
   */
  public LocalEntityConnectionProvider(final User user) {
    this(user, Databases.createInstance());
  }

  /**
   * Instantiates a new LocalEntityConnectionProvider
   * @param user the user
   * @param database the Database implementation
   */
  public LocalEntityConnectionProvider(final User user, final Database database) {
    this(user, database, SCHEDULE_VALIDITY_CHECK);
  }

  /**
   * Instantiates a new LocalEntityConnectionProvider
   * @param user the user
   * @param database the Database implementation
   * @param scheduleValidityCheck if true then a periodic validity check is performed on the connection
   */
  public LocalEntityConnectionProvider(final User user, final Database database, final boolean scheduleValidityCheck) {
    super(user, scheduleValidityCheck);
    Util.rejectNullValue(database, "database");
    this.database = database;
    this.connectionProperties.put(Database.USER_PROPERTY, user.getUsername());
    this.connectionProperties.put(Database.PASSWORD_PROPERTY, user.getPassword());
  }

  /**
   * @return the service identifier (sid) of the underlying database or the hostname if sid is not specified
   */
  @Override
  public String getDescription() {
    final String sid = database.getSid();
    if (sid == null) {
      return database.getHost();
    }

    return sid;
  }

  /** {@inheritDoc} */
  @Override
  public String getHostName() {
    return database.getHost();
  }

  /** {@inheritDoc} */
  @Override
  public void disconnect() {
    if (getConnectionInternal() != null && getConnectionInternal().isValid()) {
      getConnectionInternal().disconnect();
      final Database database = getConnectionInternal().getDatabaseConnection().getDatabase();
      if (database.isEmbedded() && Configuration.getBooleanValue(Configuration.SHUTDOWN_EMBEDDED_DB_ON_DISCONNECT)) {
        database.shutdownEmbedded(connectionProperties);
      }
      setConnection(null);
    }
  }

  /** {@inheritDoc} */
  @Override
  protected EntityConnection connect() {
    try {
      LOG.debug("Initializing connection for {}", getUser());
      return Util.initializeProxy(EntityConnection.class, new LocalConnectionHandler(EntityConnections.createConnection(database, getUser())));
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  protected boolean isConnectionValid() {
    return isConnected() && getConnectionInternal().isValid();
  }

  private static final class LocalConnectionHandler implements InvocationHandler {
    private final EntityConnection connection;
    private final MethodLogger methodLogger = EntityConnections.createLogger();

    private LocalConnectionHandler(final EntityConnection connection) {
      this.connection = connection;
      this.connection.setMethodLogger(methodLogger);
      this.methodLogger.setEnabled(true);
    }

    @Override
    public synchronized Object invoke(final Object proxy, final Method method, final Object[] args) throws Exception {
      final String methodName = method.getName();
      if (methodName.equals(IS_CONNECTED)) {
        return connection.isConnected();
      }
      if (methodName.equals(IS_VALID)) {
        return connection.isValid();
      }

      Exception exception = null;
      try {
        methodLogger.logAccess(methodName, args);

        return method.invoke(connection, args);
      }
      catch (Exception e) {
        exception = Util.unwrapAndLog(e, InvocationTargetException.class, LOG);
        throw exception;
      }
      finally {
        final MethodLogger.Entry entry = methodLogger.logExit(methodName, exception);
        if (methodLogger.isEnabled()) {
          final StringBuilder messageBuilder = new StringBuilder(connection.getUser().toString()).append("\n");
          MethodLogger.appendLogEntry(messageBuilder, entry, 0);
          LOG.info(messageBuilder.toString());
        }
      }
    }
  }
}
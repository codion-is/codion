/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.Configuration;
import org.jminor.common.MethodLogger;
import org.jminor.common.Util;
import org.jminor.common.Value;
import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.framework.db.AbstractEntityConnectionProvider;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.domain.Entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Properties;

/**
 * A class responsible for managing a local EntityConnection.
 */
public final class LocalEntityConnectionProvider extends AbstractEntityConnectionProvider<LocalEntityConnection> {

  private static final Logger LOG = LoggerFactory.getLogger(LocalEntityConnectionProvider.class);

  /**
   * Specifies whether or not an embedded database is shut down when disconnected from<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final Value<Boolean> SHUTDOWN_EMBEDDED_DB_ON_DISCONNECT = Configuration.booleanValue("jminor.db.shutdownEmbeddedOnDisconnect", false);

  /**
   * The underlying database implementation
   */
  private final Database database;

  /**
   * Instantiates a new LocalEntityConnectionProvider
   */
  public LocalEntityConnectionProvider() {
    this(Databases.getInstance());
  }

  /**
   * Instantiates a new LocalEntityConnectionProvider
   * @param database the Database instance to base this connection provider on
   */
  public LocalEntityConnectionProvider(final Database database) {
    this.database = Objects.requireNonNull(database, "database");
  }

  /** {@inheritDoc} */
  @Override
  public EntityConnection.Type getConnectionType() {
    return EntityConnection.Type.LOCAL;
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
  public String getServerHostName() {
    return database.getHost();
  }

  /** {@inheritDoc} */
  @Override
  protected LocalEntityConnection connect() {
    try {
      LOG.debug("Initializing connection for {}", getUser());
      final Entities domain = (Entities) Class.forName(getDomainClassName()).getConstructor().newInstance();

      return Util.initializeProxy(LocalEntityConnection.class, new LocalConnectionHandler(domain,
              LocalEntityConnections.createConnection(domain, database, getUser())));
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  protected void disconnect(final LocalEntityConnection connection) {
    connection.disconnect();
    if (database.isEmbedded() && SHUTDOWN_EMBEDDED_DB_ON_DISCONNECT.get()) {
      final Properties connectionProperties = new Properties();
      connectionProperties.put(Database.USER_PROPERTY, getUser().getUsername());
      connectionProperties.put(Database.PASSWORD_PROPERTY, String.valueOf(getUser().getPassword()));
      database.shutdownEmbedded(connectionProperties);
    }
  }

  private static final class LocalConnectionHandler implements InvocationHandler {
    private final LocalEntityConnection connection;
    private final MethodLogger methodLogger;

    private LocalConnectionHandler(final Entities domain, final LocalEntityConnection connection) {
      this.connection = connection;
      this.methodLogger = LocalEntityConnections.createLogger(domain);
      this.connection.setMethodLogger(methodLogger);
      this.methodLogger.setEnabled(true);
    }

    @Override
    public synchronized Object invoke(final Object proxy, final Method method, final Object[] args) throws Exception {
      final String methodName = method.getName();
      if (methodName.equals(IS_CONNECTED)) {
        return connection.isConnected();
      }

      Exception exception = null;
      try {
        methodLogger.logAccess(methodName, args);

        return method.invoke(connection, args);
      }
      catch (final InvocationTargetException e) {
        LOG.error(e.getMessage(), e);
        exception = e.getCause() instanceof Exception ? (Exception) e.getCause() : e;
        throw exception;
      }
      catch (final Exception e) {
        exception = e;
        LOG.error(e.getMessage(), e);
        throw exception;
      }
      finally {
        if (methodLogger.isEnabled()) {
          final MethodLogger.Entry entry = methodLogger.logExit(methodName, exception);
          final StringBuilder messageBuilder = new StringBuilder(connection.getUser().toString()).append("\n");
          MethodLogger.appendLogEntry(messageBuilder, entry, 0);
          LOG.info(messageBuilder.toString());
        }
      }
    }
  }
}
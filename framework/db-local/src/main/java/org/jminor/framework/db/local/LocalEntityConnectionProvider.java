/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.Configuration;
import org.jminor.common.MethodLogger;
import org.jminor.common.Util;
import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.value.PropertyValue;
import org.jminor.framework.db.AbstractEntityConnectionProvider;
import org.jminor.framework.domain.Domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * A class responsible for managing a local EntityConnection.
 */
public final class LocalEntityConnectionProvider extends AbstractEntityConnectionProvider<LocalEntityConnection> {

  /**
   * Specifies whether method logging is enabled by default on local connections.<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  private static final PropertyValue<Boolean> METHOD_LOGGER_ENABLED = Configuration.booleanValue("jminor.db.local.methodLoggerEnabled", false);

  private static final Logger LOG = LoggerFactory.getLogger(LocalEntityConnectionProvider.class);

  /**
   * Specifies whether or not an embedded database is shut down when disconnected from<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> SHUTDOWN_EMBEDDED_DB_ON_DISCONNECT = Configuration.booleanValue("jminor.db.shutdownEmbeddedOnDisconnect", false);

  /**
   * The underlying database implementation
   */
  private Database database;

  /**
   * Instantiates a new LocalEntityConnectionProvider
   */
  public LocalEntityConnectionProvider() {}

  /**
   * Instantiates a new LocalEntityConnectionProvider
   * @param database the Database instance to base this connection provider on
   */
  public LocalEntityConnectionProvider(final Database database) {
    this.database = requireNonNull(database, "database");
  }

  /** {@inheritDoc} */
  @Override
  public String getConnectionType() {
    return CONNECTION_TYPE_LOCAL;
  }

  /**
   * @return the service identifier (sid) of the underlying database or the hostname if sid is not specified
   */
  @Override
  public String getDescription() {
    final String sid = getDatabase().getSid();
    if (sid == null) {
      return getDatabase().getHost();
    }

    return sid;
  }

  /** {@inheritDoc} */
  @Override
  public String getServerHostName() {
    return getDatabase().getHost();
  }

  /** {@inheritDoc} */
  @Override
  protected LocalEntityConnection connect() {
    try {
      LOG.debug("Initializing connection for {}", getUser());
      final Domain domain = (Domain) Class.forName(getDomainClassName()).getConstructor().newInstance();

      return Util.initializeProxy(LocalEntityConnection.class, new LocalConnectionHandler(domain,
              LocalEntityConnections.createConnection(domain, getDatabase(), getUser())));
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  protected void disconnect(final LocalEntityConnection connection) {
    connection.disconnect();
    if (database != null && database.isEmbedded() && SHUTDOWN_EMBEDDED_DB_ON_DISCONNECT.get()) {
      final Properties connectionProperties = new Properties();
      connectionProperties.put(Database.USER_PROPERTY, getUser().getUsername());
      connectionProperties.put(Database.PASSWORD_PROPERTY, String.valueOf(getUser().getPassword()));
      database.shutdownEmbedded(connectionProperties);
    }
  }

  private Database getDatabase() {
    if (database == null) {
      database = Databases.getInstance();
    }

    return database;
  }

  private static final class LocalConnectionHandler implements InvocationHandler {
    private final LocalEntityConnection connection;
    private final MethodLogger methodLogger;

    private LocalConnectionHandler(final Domain domain, final LocalEntityConnection connection) {
      this.connection = connection;
      this.methodLogger = LocalEntityConnections.createLogger(domain);
      this.connection.setMethodLogger(methodLogger);
      this.methodLogger.setEnabled(METHOD_LOGGER_ENABLED.get());
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
        }
      }
    }
  }
}
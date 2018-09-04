/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.Configuration;
import org.jminor.common.MethodLogger;
import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.common.Value;
import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.framework.db.AbstractEntityConnectionProvider;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnectionProvider;
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
public final class LocalEntityConnectionProvider extends AbstractEntityConnectionProvider {

  private static final Logger LOG = LoggerFactory.getLogger(LocalEntityConnectionProvider.class);

  /**
   * Specifies whether or not an embedded database is shut down when disconnected from<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final Value<Boolean> SHUTDOWN_EMBEDDED_DB_ON_DISCONNECT = Configuration.booleanValue("jminor.db.shutdownEmbeddedOnDisconnect", false);

  /**
   * The underlying domain entities
   */
  private final Entities domain;

  /**
   * The underlying database implementation
   */
  private final Database database;

  private final Properties connectionProperties = new Properties();

  /**
   * Instantiates a new LocalEntityConnectionProvider
   * @param domain the domain model entities
   * @param user the user
   */
  public LocalEntityConnectionProvider(final Entities domain, final User user) {
    this(domain, user, Databases.getInstance());
  }

  /**
   * Instantiates a new LocalEntityConnectionProvider
   * @param domain the domain model entities
   * @param user the user
   * @param database the Database implementation
   */
  public LocalEntityConnectionProvider(final Entities domain, final User user, final Database database) {
    this(domain, user, database, EntityConnectionProvider.CONNECTION_SCHEDULE_VALIDATION.get());
  }

  /**
   * Instantiates a new LocalEntityConnectionProvider
   * @param domain the domain model entities
   * @param user the user
   * @param database the Database implementation
   * @param scheduleValidityCheck if true then a periodic validity check is performed on the connection
   */
  public LocalEntityConnectionProvider(final Entities domain, final User user, final Database database,
                                       final boolean scheduleValidityCheck) {
    super(user, scheduleValidityCheck);
    this.domain = Objects.requireNonNull(domain, "domain");
    this.database = Objects.requireNonNull(database, "database");
    this.connectionProperties.put(Database.USER_PROPERTY, user.getUsername());
    this.connectionProperties.put(Database.PASSWORD_PROPERTY, String.valueOf(user.getPassword()));
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
  protected EntityConnection connect() {
    try {
      LOG.debug("Initializing connection for {}", getUser());
      return Util.initializeProxy(EntityConnection.class, new LocalConnectionHandler(domain,
              LocalEntityConnections.createConnection(domain, database, getUser())));
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  protected void disconnect(final EntityConnection connection) {
    connection.disconnect();
    if (database.isEmbedded() && SHUTDOWN_EMBEDDED_DB_ON_DISCONNECT.get()) {
      database.shutdownEmbedded(connectionProperties);
    }
  }

  private static final class LocalConnectionHandler implements InvocationHandler {
    private final EntityConnection connection;
    private final MethodLogger methodLogger;

    private LocalConnectionHandler(final Entities domain, final EntityConnection connection) {
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
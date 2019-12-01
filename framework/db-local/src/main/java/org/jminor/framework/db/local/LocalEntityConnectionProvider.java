/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.Configuration;
import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.value.PropertyValue;
import org.jminor.framework.db.AbstractEntityConnectionProvider;
import org.jminor.framework.domain.Domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * A class responsible for managing a local EntityConnection.
 */
public final class LocalEntityConnectionProvider extends AbstractEntityConnectionProvider<LocalEntityConnection> {

  private static final Logger LOG = LoggerFactory.getLogger(LocalEntityConnectionProvider.class);

  /**
   * Specifies whether an embedded database is shut down when disconnected from<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> SHUTDOWN_EMBEDDED_DB_ON_DISCONNECT = Configuration.booleanValue("jminor.db.shutdownEmbeddedOnDisconnect", false);

  /**
   * The underlying domain model
   */
  private Domain domain;

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
      return LocalEntityConnections.createConnection(initializeDomain(), getDatabase(), getUser());
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

  private Domain initializeDomain() throws Exception {
    if (domain == null) {
      domain = (Domain) Class.forName(getDomainClassName()).getConstructor().newInstance();
    }

    return domain;
  }
}
/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.db.local;

import dev.codion.common.Configuration;
import dev.codion.common.db.database.Database;
import dev.codion.common.db.database.Databases;
import dev.codion.common.value.PropertyValue;
import dev.codion.framework.db.AbstractEntityConnectionProvider;
import dev.codion.framework.db.EntityConnection;
import dev.codion.framework.domain.Domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * A class responsible for managing a local EntityConnection.
 */
public final class LocalEntityConnectionProvider extends AbstractEntityConnectionProvider {

  private static final Logger LOG = LoggerFactory.getLogger(LocalEntityConnectionProvider.class);

  /**
   * Specifies whether an embedded database is shut down when disconnected from<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> SHUTDOWN_EMBEDDED_DB_ON_DISCONNECT = Configuration.booleanValue("codion.db.shutdownEmbeddedOnDisconnect", false);

  private final boolean shutdownDatabaseOnDisconnect = SHUTDOWN_EMBEDDED_DB_ON_DISCONNECT.get();

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

  @Override
  public String getConnectionType() {
    return CONNECTION_TYPE_LOCAL;
  }

  /**
   * @return the name of the underlying database
   */
  @Override
  public String getDescription() {
    return database.getName().toUpperCase();
  }

  /**
   * @return the underlying domain model
   */
  public Domain getDomain() {
    try {
      return initializeDomain();
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

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

  @Override
  protected void disconnect(final EntityConnection connection) {
    connection.disconnect();
    if (database != null && shutdownDatabaseOnDisconnect) {
      database.shutdownEmbedded();
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
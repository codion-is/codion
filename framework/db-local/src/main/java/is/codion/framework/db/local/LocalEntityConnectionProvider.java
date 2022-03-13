/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.Configuration;
import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.properties.PropertyValue;
import is.codion.framework.db.AbstractEntityConnectionProvider;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.Domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static is.codion.framework.db.local.LocalEntityConnection.localEntityConnection;
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
  public static final PropertyValue<Boolean> SHUTDOWN_EMBEDDED_DB_ON_DISCONNECT =
          Configuration.booleanValue("codion.db.shutdownEmbeddedOnDisconnect", false);

  private final boolean shutdownDatabaseOnDisconnect = SHUTDOWN_EMBEDDED_DB_ON_DISCONNECT.get();

  /**
   * The underlying domain model
   */
  private Domain domain;

  /**
   * The underlying database implementation
   */
  private Database database;

  private int queryTimeout = LocalEntityConnection.QUERY_TIMEOUT_SECONDS.get();

  /**
   * Instantiates a new LocalEntityConnectionProvider
   */
  public LocalEntityConnectionProvider() {}

  /**
   * Instantiates a new LocalEntityConnectionProvider
   * @param database the Database instance to base this connection provider on
   */
  public LocalEntityConnectionProvider(Database database) {
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
    return getDatabase().getName().toUpperCase();
  }

  /**
   * @return the underlying domain model
   */
  public Domain getDomain() {
    if (domain == null) {
      String domainClassName = getDomainClassName();
      Optional<Domain> optionalDomain = Domain.getInstanceByClassName(domainClassName);
      if (optionalDomain.isPresent()) {
        domain = optionalDomain.get();
      }
      else {
        LOG.debug("Domain of type " + domainClassName + " not found in services");
      }
      try {
        if (domain == null) {
          domain = (Domain) Class.forName(domainClassName).getConstructor().newInstance();
        }
      }
      catch (Exception e) {
        LOG.error("Error when instantiating Domain of type " + domainClassName);
        throw new RuntimeException(e);
      }
    }

    return domain;
  }

  /**
   * @return the underlying {@link Database} instance
   */
  public Database getDatabase() {
    if (database == null) {
      database = DatabaseFactory.getDatabase();
    }

    return database;
  }

  /**
   * @return the query timeout being used
   */
  public int getQueryTimeout() {
    return queryTimeout;
  }

  /**
   * Note that calling this method closes the underlying connection, if one has been established.
   * @param queryTimeout the query timeout in seconds
   * @return this LocalEntityConnectionProvider instance
   */
  public LocalEntityConnectionProvider setQueryTimeout(int queryTimeout) {
    close();
    this.queryTimeout = queryTimeout;
    return this;
  }

  @Override
  protected LocalEntityConnection connect() {
    try {
      LOG.debug("Initializing connection for {}", getUser());
      return localEntityConnection(getDomain(), getDatabase(), getUser())
              .setQueryTimeout(queryTimeout);
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void close(EntityConnection connection) {
    connection.close();
    if (database != null && shutdownDatabaseOnDisconnect) {
      database.shutdownEmbedded();
    }
  }
}
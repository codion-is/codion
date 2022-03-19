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
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.Domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static is.codion.framework.db.local.LocalEntityConnection.localEntityConnection;
import static java.util.Objects.requireNonNull;

/**
 * A class responsible for managing a local EntityConnection.
 * @see LocalEntityConnectionProvider#builder()
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

  private final Domain domain;
  private final Database database;
  private final int defaultQueryTimeout;

  private LocalEntityConnectionProvider(DefaultBuilder builder) {
    super(builder);
    this.domain = initializeDomain(getDomainClassName());
    this.database = builder.database == null ? DatabaseFactory.getDatabase() : builder.database;
    this.defaultQueryTimeout = builder.defaultQueryTimeout;
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
    return domain;
  }

  /**
   * @return the underlying {@link Database} instance
   */
  public Database getDatabase() {
    return database;
  }

  /**
   * @return the default query timeout being used
   */
  public int getDefaultQueryTimeout() {
    return defaultQueryTimeout;
  }

  /**
   * Instantiates a new builder instance.
   * @return a new builder
   */
  public static Builder builder() {
    return new DefaultBuilder();
  }

  @Override
  protected LocalEntityConnection connect() {
    try {
      LOG.debug("Initializing connection for {}", getUser());
      return localEntityConnection(getDomain(), getDatabase(), getUser())
              .setDefaultQueryTimeout(defaultQueryTimeout);
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void close(EntityConnection connection) {
    connection.close();
    if (shutdownDatabaseOnDisconnect) {
      database.shutdownEmbedded();
    }
  }

  private static Domain initializeDomain(String domainClassName) {
    Optional<Domain> optionalDomain = Domain.getInstanceByClassName(domainClassName);
    if (optionalDomain.isPresent()) {
      return optionalDomain.get();
    }
    else {
      LOG.debug("Domain of type " + domainClassName + " not found in services");
    }
    try {
      return (Domain) Class.forName(domainClassName).getConstructor().newInstance();
    }
    catch (Exception e) {
      LOG.error("Error when instantiating Domain of type " + domainClassName);
      throw new RuntimeException(e);
    }
  }

  /**
   * Builds a {@link LocalEntityConnectionProvider}.
   */
  public interface Builder extends EntityConnectionProvider.Builder<Builder, LocalEntityConnectionProvider> {

    /**
     * @param database the database instance to use
     * @return this builder instance
     */
    Builder database(Database database);

    /**
     * @param defaultQueryTimeout the default query timeout
     * @return this builder instance
     */
    Builder defaultQueryTimeout(int defaultQueryTimeout);
  }

  public static final class DefaultBuilder extends AbstractBuilder<Builder, LocalEntityConnectionProvider> implements Builder {

    private Database database;
    private int defaultQueryTimeout = LocalEntityConnection.QUERY_TIMEOUT_SECONDS.get();

    public DefaultBuilder() {
      super(EntityConnectionProvider.CONNECTION_TYPE_LOCAL);
    }

    @Override
    public Builder database(Database database) {
      this.database = requireNonNull(database);
      return this;
    }

    @Override
    public Builder defaultQueryTimeout(int defaultQueryTimeout) {
      this.defaultQueryTimeout = defaultQueryTimeout;
      return this;
    }

    @Override
    public LocalEntityConnectionProvider build() {
      return new LocalEntityConnectionProvider(this);
    }
  }
}